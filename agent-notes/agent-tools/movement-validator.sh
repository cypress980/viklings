#!/bin/bash

# TOOL: Movement Validator for DEV-002
# Tests triangle movement with input injection and screenshot validation

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

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCREENSHOTS_DIR="${SCRIPT_DIR}/../screenshots"
APP_NAME="viklings-wgpu"

# Function to show usage
show_usage() {
    cat << EOF
Movement Validator - DEV-002 Input and Visual Testing

USAGE:
    $0 DIRECTION [OPTIONS]

ARGUMENTS:
    DIRECTION          Direction to test (up, down, left, right)
    
OPTIONS:
    --help            Show this help message
    --count N         Number of keystrokes to send (default: 5)
    --delay SECONDS   Delay between keystrokes (default: 0.1)
    --baseline        Take baseline screenshot only
    --compare         Compare two existing screenshots

EXAMPLES:
    $0 right                     # Test 5 right movements
    $0 up --count 10            # Test 10 up movements
    $0 --baseline               # Take baseline screenshot
    $0 --compare baseline.png after.png  # Compare screenshots

OUTPUT:
    Screenshots and validation results in: $SCREENSHOTS_DIR/
EOF
}

# Function to ensure application is running
check_application() {
    local running
    running=$(osascript -e "
        tell application \"System Events\"
            repeat with proc in application processes
                if name of proc contains \"$APP_NAME\" then
                    return \"true\"
                end if
            end repeat
        end tell
        return \"false\"
    " 2>/dev/null)
    
    if [[ "$running" != "true" ]]; then
        log_error "Application '$APP_NAME' is not running"
        log_error "Please start the application first: cd viklings-wgpu && cargo run"
        return 1
    fi
    
    log_info "Application '$APP_NAME' is running"
    return 0
}

# Function to take screenshot with retry logic
take_screenshot() {
    local filename="$1"
    local max_retries=3
    local retry=0
    
    while [[ $retry -lt $max_retries ]]; do
        log_info "Taking screenshot: $filename (attempt $((retry + 1))/$max_retries)"
        
        # Try different approaches to capture the window
        if "$SCRIPT_DIR/screenshot.sh" "$APP_NAME" "$filename"; then
            local screenshot_path="$SCREENSHOTS_DIR/${filename}.png"
            if [[ -f "$screenshot_path" ]]; then
                local file_size
                file_size=$(stat -f%z "$screenshot_path" 2>/dev/null || stat -c%s "$screenshot_path")
                
                # Basic validation: file exists and isn't too small
                if [[ "$file_size" -gt 10000 ]]; then
                    log_success "Screenshot captured successfully: $filename"
                    return 0
                else
                    log_warning "Screenshot too small ($file_size bytes), retrying..."
                fi
            fi
        fi
        
        retry=$((retry + 1))
        if [[ $retry -lt $max_retries ]]; then
            sleep 1
        fi
    done
    
    log_error "Failed to capture screenshot after $max_retries attempts"
    return 1
}

# Function to send multiple keystrokes
send_movements() {
    local direction="$1"
    local count="$2"
    local delay="$3"
    
    log_info "Sending $count '$direction' movements with ${delay}s delay"
    
    for ((i=1; i<=count; i++)); do
        log_info "Movement $i/$count: $direction"
        if "$SCRIPT_DIR/input-injector.sh" "$APP_NAME" "$direction" --delay "$delay"; then
            log_info "Successfully sent '$direction' keystroke $i/$count"
        else
            log_error "Failed to send keystroke $i/$count"
            return 1
        fi
    done
    
    log_success "Completed $count '$direction' movements"
    return 0
}

# Function to compare screenshots (basic file size comparison)
compare_screenshots() {
    local before_file="$1"
    local after_file="$2"
    
    local before_path="$SCREENSHOTS_DIR/${before_file}"
    local after_path="$SCREENSHOTS_DIR/${after_file}"
    
    # Check if files exist
    if [[ ! -f "$before_path" ]]; then
        log_error "Before screenshot not found: $before_path"
        return 1
    fi
    
    if [[ ! -f "$after_path" ]]; then
        log_error "After screenshot not found: $after_path"
        return 1
    fi
    
    # Basic comparison using file sizes
    local before_size after_size
    before_size=$(stat -f%z "$before_path" 2>/dev/null || stat -c%s "$before_path")
    after_size=$(stat -f%z "$after_path" 2>/dev/null || stat -c%s "$after_path")
    
    log_info "Before screenshot: ${before_file} (${before_size} bytes)"
    log_info "After screenshot: ${after_file} (${after_size} bytes)"
    
    # Calculate size difference percentage
    local size_diff
    if [[ "$before_size" -gt 0 ]]; then
        size_diff=$(( (after_size - before_size) * 100 / before_size ))
        if [[ "${size_diff#-}" -gt 5 ]]; then  # Absolute value > 5%
            log_success "Significant change detected: ${size_diff}% file size difference"
            return 0
        else
            log_warning "Minimal change detected: ${size_diff}% file size difference"
            return 2  # Ambiguous result
        fi
    else
        log_error "Before screenshot has zero size"
        return 1
    fi
}

# Function to run complete movement test
test_movement() {
    local direction="$1"
    local count="$2"
    local delay="$3"
    
    log_info "=== Starting Movement Test: $direction ==="
    
    # Check if application is running
    if ! check_application; then
        return 1
    fi
    
    # Take baseline screenshot
    local baseline_name="baseline-${direction}-$(date +%H%M%S)"
    if ! take_screenshot "$baseline_name"; then
        log_error "Failed to capture baseline screenshot"
        return 1
    fi
    
    # Wait a moment for stability
    sleep 0.5
    
    # Send movements
    if ! send_movements "$direction" "$count" "$delay"; then
        log_error "Failed to send movements"
        return 1
    fi
    
    # Wait for rendering to complete
    sleep 0.5
    
    # Take after screenshot
    local after_name="after-${direction}-$(date +%H%M%S)"
    if ! take_screenshot "$after_name"; then
        log_error "Failed to capture after screenshot"
        return 1
    fi
    
    # Compare screenshots
    log_info "=== Validating Movement Results ==="
    if compare_screenshots "${baseline_name}.png" "${after_name}.png"; then
        log_success "✅ Movement test PASSED for direction '$direction'"
        log_success "Screenshots: $baseline_name.png -> $after_name.png"
        return 0
    else
        local result=$?
        if [[ $result -eq 2 ]]; then
            log_warning "⚠️  Movement test INCONCLUSIVE for direction '$direction'"
        else
            log_error "❌ Movement test FAILED for direction '$direction'"
        fi
        log_info "Screenshots: $baseline_name.png -> $after_name.png"
        return $result
    fi
}

# Main function
main() {
    local direction=""
    local count="5"
    local delay="0.1"
    local baseline_only=false
    local compare_mode=false
    local compare_file1=""
    local compare_file2=""
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --help|-h)
                show_usage
                exit 0
                ;;
            --count)
                count="$2"
                shift 2
                ;;
            --delay)
                delay="$2"
                shift 2
                ;;
            --baseline)
                baseline_only=true
                shift
                ;;
            --compare)
                compare_mode=true
                compare_file1="$2"
                compare_file2="$3"
                shift 3
                ;;
            -*)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            up|down|left|right)
                direction="$1"
                shift
                ;;
            *)
                log_error "Unknown argument: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Handle different modes
    if [[ "$baseline_only" == "true" ]]; then
        log_info "Taking baseline screenshot only"
        if check_application && take_screenshot "baseline-$(date +%H%M%S)"; then
            log_success "Baseline screenshot completed"
            exit 0
        else
            log_error "Failed to capture baseline"
            exit 1
        fi
    elif [[ "$compare_mode" == "true" ]]; then
        if [[ -z "$compare_file1" || -z "$compare_file2" ]]; then
            log_error "Compare mode requires two filenames"
            show_usage
            exit 1
        fi
        log_info "Comparing screenshots: $compare_file1 vs $compare_file2"
        compare_screenshots "$compare_file1" "$compare_file2"
        exit $?
    elif [[ -n "$direction" ]]; then
        # Run movement test
        test_movement "$direction" "$count" "$delay"
        exit $?
    else
        log_error "No direction specified"
        show_usage
        exit 1
    fi
}

# Ensure screenshots directory exists
mkdir -p "$SCREENSHOTS_DIR"

# Run main function with all arguments
main "$@"