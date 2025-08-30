#!/bin/bash

# TOOL-001: Screenshot Tool for Agent Visual Feedback
# Cross-platform screenshot utility for capturing application windows

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCREENSHOTS_DIR="${SCRIPT_DIR}/../screenshots"
DEFAULT_FILENAME="screenshot-$(date +%Y%m%d-%H%M%S)"

# Ensure screenshots directory exists
mkdir -p "$SCREENSHOTS_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Function to detect the operating system
detect_platform() {
    case "$(uname -s)" in
        Darwin*) echo "macos" ;;
        Linux*)  echo "linux" ;;
        CYGWIN*|MINGW*|MSYS*) echo "windows" ;;
        *) echo "unknown" ;;
    esac
}

# Function to show usage
show_usage() {
    cat << EOF
TOOL-001: Screenshot Tool for Agent Visual Feedback

USAGE:
    $0 [WINDOW_NAME] [FILENAME] [OPTIONS]

ARGUMENTS:
    WINDOW_NAME    Name or title of the window to capture (optional)
    FILENAME       Custom filename without extension (optional)
    
OPTIONS:
    --fullscreen   Capture entire screen instead of specific window
    --help         Show this help message
    --list         List available windows (macOS only)
    --interactive  Interactive window selection (macOS only)

EXAMPLES:
    $0                                    # Interactive selection
    $0 "viklings-wgpu"                    # Capture window by name
    $0 "viklings-wgpu" triangle-test      # Custom filename
    $0 --fullscreen                       # Full screen capture
    $0 --list                            # List available windows

OUTPUT:
    Screenshots are saved to: $SCREENSHOTS_DIR/
    Default format: PNG with timestamp
EOF
}

# Function to list windows (macOS only)
list_windows_macos() {
    log_info "Available windows:"
    osascript << 'EOF'
tell application "System Events"
    set windowList to {}
    repeat with proc in application processes
        try
            set procName to name of proc
            set windowCount to count of windows of proc
            if windowCount > 0 then
                repeat with win in windows of proc
                    try
                        set winTitle to title of win
                        if winTitle is not "" then
                            set end of windowList to procName & " - " & winTitle
                        end if
                    end try
                end repeat
            end if
        end try
    end repeat
    
    repeat with winInfo in windowList
        log winInfo
    end repeat
end tell
EOF
}

# Function to capture window on macOS
screenshot_macos() {
    local window_name="$1"
    local filename="$2"
    local output_path="${SCREENSHOTS_DIR}/${filename}.png"
    
    if [[ "$window_name" == "--fullscreen" ]]; then
        log_info "Capturing full screen..."
        screencapture "$output_path"
        return $?
    elif [[ "$window_name" == "--interactive" ]]; then
        log_info "Interactive window selection (click on window)..."
        screencapture -i "$output_path"
        return $?
    elif [[ -n "$window_name" ]]; then
        log_info "Attempting to capture window: '$window_name'"
        
        # Try to find window by application name first
        local window_id
        window_id=$(osascript -e "
            try
                tell application \"$window_name\"
                    if it is running then
                        return id of window 1
                    end if
                end tell
            on error
                return \"\"
            end try
        " 2>/dev/null)
        
        if [[ -n "$window_id" && "$window_id" != "" ]]; then
            log_info "Found window by application name, capturing..."
            screencapture -l"$window_id" "$output_path"
            return $?
        else
            # Try to find by window title or process name
            window_id=$(osascript -e "
                tell application \"System Events\"
                    repeat with proc in application processes
                        try
                            -- Check if process name contains search term
                            if name of proc contains \"$window_name\" then
                                if (count of windows of proc) > 0 then
                                    return id of window 1 of proc
                                end if
                            end if
                            -- Also check window titles
                            repeat with win in windows of proc
                                if title of win contains \"$window_name\" then
                                    return id of win
                                end if
                            end repeat
                        end try
                    end repeat
                end tell
                return \"\"
            " 2>/dev/null)
            
            if [[ -n "$window_id" && "$window_id" != "" ]]; then
                log_info "Found window by title match, capturing..."
                screencapture -l"$window_id" "$output_path"
                return $?
            else
                # Try coordinate-based capture for applications that don't support window IDs
                log_info "Trying coordinate-based capture for '$window_name'..."
                local coords
                coords=$(osascript -e "
                    tell application \"System Events\"
                        repeat with proc in application processes
                            try
                                if name of proc contains \"$window_name\" then
                                    if (count of windows of proc) > 0 then
                                        set win to window 1 of proc
                                        set {x, y} to position of win
                                        set {w, h} to size of win
                                        return x & \" \" & y & \" \" & w & \" \" & h
                                    end if
                                end if
                            end try
                        end repeat
                    end tell
                    return \"\"
                " 2>/dev/null)
                
                if [[ -n "$coords" && "$coords" != "" ]]; then
                    log_info "Found window coordinates: $coords, capturing..."
                    # Convert space-separated to comma-separated for screencapture
                    local rect_coords
                    rect_coords=$(echo "$coords" | tr ' ' ',')
                    screencapture -R "$rect_coords" "$output_path"
                    return $?
                else
                    log_error "Could not find window '$window_name'. Available windows:"
                    list_windows_macos 2>/dev/null | head -10
                    return 1
                fi
            fi
        fi
    else
        log_info "No window specified, using interactive selection..."
        screencapture -i "$output_path"
        return $?
    fi
}

# Function to capture window on Linux
screenshot_linux() {
    local window_name="$1"
    local filename="$2"
    local output_path="${SCREENSHOTS_DIR}/${filename}.png"
    
    # Check for available screenshot tools
    if command -v gnome-screenshot >/dev/null 2>&1; then
        if [[ "$window_name" == "--fullscreen" ]]; then
            gnome-screenshot -f "$output_path"
        else
            gnome-screenshot -w -f "$output_path"
        fi
    elif command -v scrot >/dev/null 2>&1; then
        if [[ "$window_name" == "--fullscreen" ]]; then
            scrot "$output_path"
        else
            scrot -s "$output_path"
        fi
    elif command -v import >/dev/null 2>&1; then
        # ImageMagick import
        if [[ "$window_name" == "--fullscreen" ]]; then
            import -window root "$output_path"
        else
            import "$output_path"
        fi
    else
        log_error "No screenshot tool found. Please install gnome-screenshot, scrot, or ImageMagick"
        return 1
    fi
}

# Function to capture window on Windows
screenshot_windows() {
    local window_name="$1"
    local filename="$2"
    local output_path="${SCREENSHOTS_DIR}/${filename}.png"
    
    # Use PowerShell to take screenshot
    powershell -Command "
        Add-Type -AssemblyName System.Windows.Forms
        Add-Type -AssemblyName System.Drawing
        \$bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
        \$bitmap = New-Object System.Drawing.Bitmap \$bounds.Width, \$bounds.Height
        \$graphics = [System.Drawing.Graphics]::FromImage(\$bitmap)
        \$graphics.CopyFromScreen(\$bounds.X, \$bounds.Y, 0, 0, \$bounds.Size)
        \$bitmap.Save('$output_path', [System.Drawing.Imaging.ImageFormat]::Png)
        \$graphics.Dispose()
        \$bitmap.Dispose()
    "
}

# Main screenshot function
take_screenshot() {
    local window_name="${1:-}"
    local filename="${2:-$DEFAULT_FILENAME}"
    local platform
    platform=$(detect_platform)
    
    log_info "Platform detected: $platform"
    log_info "Output directory: $SCREENSHOTS_DIR"
    log_info "Filename: ${filename}.png"
    
    case "$platform" in
        "macos")
            screenshot_macos "$window_name" "$filename"
            ;;
        "linux")
            screenshot_linux "$window_name" "$filename"
            ;;
        "windows")
            screenshot_windows "$window_name" "$filename"
            ;;
        *)
            log_error "Unsupported platform: $platform"
            return 1
            ;;
    esac
    
    local result=$?
    local output_path="${SCREENSHOTS_DIR}/${filename}.png"
    
    if [[ $result -eq 0 && -f "$output_path" ]]; then
        local file_size
        file_size=$(ls -lh "$output_path" | awk '{print $5}')
        log_success "Screenshot saved: $output_path ($file_size)"
        
        # Show some basic info about the image
        if command -v file >/dev/null 2>&1; then
            log_info "$(file "$output_path")"
        fi
        
        return 0
    else
        log_error "Failed to capture screenshot"
        return 1
    fi
}

# Parse command line arguments
main() {
    local window_name=""
    local filename="$DEFAULT_FILENAME"
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --help|-h)
                show_usage
                exit 0
                ;;
            --list)
                if [[ "$(detect_platform)" == "macos" ]]; then
                    list_windows_macos
                else
                    log_error "--list option only supported on macOS"
                    exit 1
                fi
                exit 0
                ;;
            --fullscreen)
                window_name="--fullscreen"
                shift
                ;;
            --interactive)
                window_name="--interactive"
                shift
                ;;
            -*)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                if [[ -z "$window_name" && "$window_name" != "--fullscreen" && "$window_name" != "--interactive" ]]; then
                    window_name="$1"
                elif [[ "$filename" == "$DEFAULT_FILENAME" ]]; then
                    filename="$1"
                else
                    log_error "Too many arguments"
                    show_usage
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    take_screenshot "$window_name" "$filename"
}

# Run main function with all arguments
main "$@"