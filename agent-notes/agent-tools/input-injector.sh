#!/bin/bash

# TOOL: Input Injector for DEV-002
# Sends keyboard input to specified applications programmatically

set -eo pipefail

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

# macOS key codes for arrow keys (handled in case statement)

# Function to show usage
show_usage() {
    cat << EOF
Input Injector - DEV-002 Keyboard Input Automation

USAGE:
    $0 APPLICATION_NAME KEY [OPTIONS]

ARGUMENTS:
    APPLICATION_NAME    Name of the target application process
    KEY                 Key to send (up, down, left, right, space, enter, escape)
    
OPTIONS:
    --help             Show this help message
    --list-apps        List running applications
    --delay SECONDS    Delay after sending key (default: 0.1)
    --activate         Activate application before sending key

EXAMPLES:
    $0 "viklings-wgpu" up              # Send up arrow to triangle app
    $0 "viklings-wgpu" right --delay 0.5   # Send right arrow with delay
    $0 "Terminal" space --activate      # Activate Terminal then send space

SUPPORTED KEYS:
    up, down, left, right, space, enter, escape

OUTPUT:
    Reports success/failure of keystroke injection
EOF
}

# Function to list running applications
list_applications() {
    log_info "Running applications:"
    osascript << 'EOF'
tell application "System Events"
    repeat with proc in application processes
        try
            if background only of proc is false then
                log name of proc
            end if
        end try
    end repeat
end tell
EOF
}

# Function to check if application is running
check_application() {
    local app_name="$1"
    
    osascript -e "
        tell application \"System Events\"
            repeat with proc in application processes
                if name of proc contains \"$app_name\" then
                    return \"true\"
                end if
            end repeat
        end tell
        return \"false\"
    " 2>/dev/null
}

# Function to activate application
activate_application() {
    local app_name="$1"
    
    log_info "Activating application: $app_name"
    osascript -e "
        tell application \"System Events\"
            repeat with proc in application processes
                if name of proc contains \"$app_name\" then
                    set frontmost of proc to true
                    return \"activated\"
                end if
            end repeat
        end tell
        return \"not_found\"
    " 2>/dev/null
}

# Function to send keystroke
send_keystroke() {
    local app_name="$1"
    local key_name="$2"
    local activate="${3:-false}"
    
    # Check if key is supported
    local key_code=""
    case "$key_name" in
        "up") key_code=126 ;;
        "down") key_code=125 ;;
        "left") key_code=123 ;;
        "right") key_code=124 ;;
        "space") key_code=49 ;;
        "enter") key_code=36 ;;
        "escape") key_code=53 ;;
        *)
            log_error "Unsupported key: $key_name"
            log_error "Supported keys: up, down, left, right, space, enter, escape"
            return 1
            ;;
    esac
    
    # Check if application is running
    if [[ "$(check_application "$app_name")" != "true" ]]; then
        log_error "Application '$app_name' is not running"
        return 1
    fi
    
    # Activate application if requested
    if [[ "$activate" == "true" ]]; then
        local result
        result=$(activate_application "$app_name")
        if [[ "$result" != "activated" ]]; then
            log_warning "Could not activate application '$app_name'"
        fi
        # Small delay for activation
        sleep 0.2
    fi
    
    # Send keystroke
    log_info "Sending '$key_name' key (code: $key_code) to '$app_name'"
    
    local success
    success=$(osascript -e "
        tell application \"System Events\"
            repeat with proc in application processes
                if name of proc contains \"$app_name\" then
                    tell proc to key code $key_code
                    return \"success\"
                end if
            end repeat
        end tell
        return \"failed\"
    " 2>/dev/null)
    
    if [[ "$success" == "success" ]]; then
        log_success "Successfully sent '$key_name' key to '$app_name'"
        return 0
    else
        log_error "Failed to send keystroke to '$app_name'"
        return 1
    fi
}

# Main function
main() {
    local app_name=""
    local key_name=""
    local delay="0.1"
    local activate="false"
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --help|-h)
                show_usage
                exit 0
                ;;
            --list-apps)
                list_applications
                exit 0
                ;;
            --delay)
                delay="$2"
                shift 2
                ;;
            --activate)
                activate="true"
                shift
                ;;
            -*)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                if [[ -z "$app_name" ]]; then
                    app_name="$1"
                elif [[ -z "$key_name" ]]; then
                    key_name="$1"
                else
                    log_error "Too many arguments"
                    show_usage
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    # Check required arguments
    if [[ -z "$app_name" || -z "$key_name" ]]; then
        log_error "Missing required arguments"
        show_usage
        exit 1
    fi
    
    # Send keystroke
    if send_keystroke "$app_name" "$key_name" "$activate"; then
        # Add delay after successful keystroke
        if (( $(echo "$delay > 0" | bc -l) )); then
            log_info "Waiting ${delay}s for input processing..."
            sleep "$delay"
        fi
        exit 0
    else
        exit 1
    fi
}

# Run main function with all arguments
main "$@"