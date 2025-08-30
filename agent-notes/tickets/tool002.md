# TOOL-002: Enhanced Application Window Capture

## Description
Improve the existing screenshot tool (TOOL-001) to reliably capture specific application windows without falling back to fullscreen or interactive selection. The current tool has issues accurately identifying and capturing target application windows.

## Problem Statement
The current screenshot tool from TOOL-001 has reliability issues:
- Window detection by application name frequently fails
- Window title matching is unreliable for applications without descriptive titles
- Falls back to interactive selection, defeating automation purposes
- Cannot reliably capture specific applications like game windows or terminal applications
- AppleScript-based window detection is fragile and inconsistent

## Root Cause Analysis
After analyzing the current implementation:

### Current Detection Methods (Problematic)
1. **Application Name Lookup**: Uses AppleScript to find applications, but many applications don't expose windows properly
2. **Window Title Matching**: Searches for windows containing title text, but game windows often have generic or changing titles
3. **Interactive Fallback**: Requires human interaction, breaking agent automation

### Technical Issues Identified
- AppleScript window detection fails for OpenGL/game applications
- No support for process-based window identification
- Limited to foreground/visible windows only
- No validation of capture success beyond file existence

## Solution Overview
Enhance the screenshot tool with multiple robust window detection strategies:

1. **Process-Based Detection**: Capture by process ID or executable name
2. **System API Integration**: Use native macOS window server APIs
3. **Enhanced Validation**: Verify captured content matches expectations
4. **Multi-Strategy Fallback**: Try multiple detection methods before giving up
5. **Application-Specific Handling**: Special handling for game engines and graphics applications

## Requirements

### 1. Enhanced Window Detection
- **Process ID Support**: Capture windows by specific process ID
- **Executable Name Matching**: Find windows by executable file name (e.g., `viklings-wgpu`)
- **Window Property Search**: Search by window class, role, or other properties
- **Background Window Support**: Capture minimized or background windows

### 2. Validation and Quality Assurance  
- **Content Validation**: Verify captured image is not blank or corrupted
- **Size Verification**: Confirm captured window has reasonable dimensions
- **Color Analysis**: Basic sanity checks on captured content
- **Error Reporting**: Detailed failure reasons for debugging

### 3. Application-Specific Support
- **Graphics Applications**: Special handling for OpenGL/Vulkan/WGPU applications
- **Game Engine Support**: Optimized for game development tools
- **Terminal Applications**: Support for CLI tools and development environments
- **IDE Integration**: Support for capturing editor windows and debug outputs

## Technical Approach

### Enhanced macOS Implementation
```bash
# Multi-strategy window detection
capture_by_process_id() {
    local pid="$1"
    local output_path="$2"
    
    # Get window ID from process ID using system APIs
    local window_id
    window_id=$(osascript -e "
        tell application \"System Events\"
            repeat with proc in application processes
                if unix id of proc is $pid then
                    try
                        return id of window 1 of proc
                    end try
                end if
            end repeat
        end tell
    ")
    
    if [[ -n "$window_id" ]]; then
        screencapture -l"$window_id" "$output_path"
        return $?
    fi
    return 1
}

capture_by_executable() {
    local exec_name="$1"
    local output_path="$2"
    
    # Find process by executable name
    local pid
    pid=$(pgrep -f "$exec_name" | head -1)
    
    if [[ -n "$pid" ]]; then
        capture_by_process_id "$pid" "$output_path"
        return $?
    fi
    return 1
}

# Enhanced window search with multiple criteria
find_window_comprehensive() {
    local search_term="$1"
    
    # Strategy 1: Direct application name
    # Strategy 2: Process executable matching  
    # Strategy 3: Window title contains search term
    # Strategy 4: Window class or role matching
    
    # Return best match window ID
}
```

### Advanced Validation
```bash
validate_screenshot() {
    local image_path="$1"
    
    # Check file exists and has reasonable size
    if [[ ! -f "$image_path" ]]; then
        return 1
    fi
    
    local file_size
    file_size=$(stat -f%z "$image_path" 2>/dev/null || stat -c%s "$image_path")
    
    # Reject tiny files (likely empty or corrupt)
    if [[ "$file_size" -lt 1000 ]]; then
        log_error "Screenshot too small (${file_size} bytes), likely empty"
        return 1
    fi
    
    # Use ImageMagick or similar to check image properties
    if command -v identify >/dev/null 2>&1; then
        local dimensions
        dimensions=$(identify -format "%wx%h" "$image_path" 2>/dev/null)
        
        if [[ "$dimensions" =~ ^[0-9]+x[0-9]+$ ]]; then
            local width height
            width=${dimensions%x*}
            height=${dimensions#*x}
            
            # Reject unreasonably small captures
            if [[ "$width" -lt 100 || "$height" -lt 100 ]]; then
                log_error "Screenshot dimensions too small: ${dimensions}"
                return 1
            fi
            
            log_info "Screenshot validated: ${dimensions} pixels"
            return 0
        fi
    fi
    
    # Basic validation passed
    return 0
}
```

## Implementation Plan

### Phase 1: Enhanced Window Detection (Priority: High)
- [ ] Implement process-based window capture
- [ ] Add executable name matching
- [ ] Create comprehensive window search function
- [ ] Test with viklings-wgpu and other graphics applications

### Phase 2: Validation and Quality (Priority: High)  
- [ ] Implement screenshot content validation
- [ ] Add dimension and size checks
- [ ] Create detailed error reporting
- [ ] Test validation with known good/bad captures

### Phase 3: Application-Specific Support (Priority: Medium)
- [ ] Add special handling for game engines (WGPU, OpenGL)
- [ ] Implement background window capture
- [ ] Create application profiles for common development tools
- [ ] Test with various development environments

### Phase 4: Cross-Platform Enhancement (Priority: Low)
- [ ] Enhance Linux window detection with xwininfo/xprop
- [ ] Improve Windows PowerShell-based capture
- [ ] Unify interface across platforms
- [ ] Comprehensive cross-platform testing

## Success Criteria

### Functional Requirements
- [ ] **95% Success Rate**: Capture specific application windows 95% of the time without fallback
- [ ] **Process Support**: Successfully capture by process ID and executable name
- [ ] **Validation**: Reject empty, corrupt, or invalid captures
- [ ] **Game Engine Support**: Reliably capture WGPU, OpenGL, and Vulkan applications

### Quality Requirements
- [ ] **No False Positives**: Never report success for failed/empty captures
- [ ] **Clear Error Messages**: Specific failure reasons for troubleshooting
- [ ] **Performance**: Capture within 2 seconds for active windows
- [ ] **Automation-Ready**: No human interaction required for standard use cases

### Agent Integration Requirements
- [ ] **Command Line Interface**: Works seamlessly in agent workflows
- [ ] **Predictable Output**: Consistent behavior for scripting
- [ ] **Error Handling**: Machine-readable success/failure status
- [ ] **Documentation**: Clear usage examples for different capture scenarios

## Testing Strategy

### Target Applications for Testing
1. **viklings-wgpu**: Primary target graphics application
2. **Terminal.app**: CLI development tool
3. **VSCode**: Development environment
4. **Chrome/Safari**: Web development browser
5. **System Preferences**: Standard macOS application

### Test Scenarios
1. **Active Window Capture**: Application in foreground
2. **Background Window Capture**: Application running but not focused
3. **Multiple Windows**: Application with multiple windows open
4. **Minimized Window**: Application minimized to dock
5. **Fullscreen Application**: Application in fullscreen mode

### Validation Tests
1. **Content Verification**: Captured image contains expected application content
2. **Dimension Verification**: Image dimensions match window size
3. **Quality Verification**: Image is not blank, corrupted, or distorted
4. **Performance Verification**: Capture completes within time limits

## Expected Challenges & Solutions

### Challenge: Graphics Application Compatibility
- **Problem**: OpenGL/WGPU applications may not expose windows correctly
- **Solution**: Use lower-level system APIs and process-based detection
- **Fallback**: Coordinate-based capture using window position

### Challenge: Window State Variations
- **Problem**: Windows can be minimized, background, or fullscreen
- **Solution**: Detect window state and use appropriate capture method
- **Workaround**: Temporarily bring window to foreground for capture

### Challenge: Permission and Security
- **Problem**: macOS security restrictions on window capture
- **Solution**: Clear documentation for permission setup
- **Alternative**: Provide diagnostic tools to test permissions

## Deliverables

### 1. Enhanced Screenshot Tool
- Updated `agent-tools/screenshot.sh` with new detection methods
- Backward compatibility with existing usage patterns
- New command-line options for advanced features

### 2. Validation Framework
- Content validation functions
- Quality assurance checks  
- Detailed error reporting and diagnostics

### 3. Testing and Documentation
- Comprehensive testing with target applications
- Usage examples for different scenarios
- Troubleshooting guide for common issues

### 4. Agent Integration
- Validated workflow integration
- Performance benchmarks
- Success rate metrics with different applications

## Time Estimate
4-6 hours for complete implementation, testing, and validation

## Priority
**Critical** - Essential for reliable agent-assisted graphics development

## Dependencies
- Existing TOOL-001 implementation
- Target applications for testing (viklings-wgpu, etc.)
- macOS system permissions for window capture
- Optional: ImageMagick for advanced image validation

## Success Metrics

### Before (TOOL-001 Current State)
- Unreliable application window capture
- Frequent fallback to interactive selection
- No validation of capture quality
- Limited automation capability

### After (TOOL-002 Target State)  
- **95%+ success rate** for specific application capture
- **Zero interactive fallbacks** for standard use cases
- **Validated captures** with quality assurance
- **Full automation support** for agent workflows

## Created By
Development Agent (TOOL-002)

## Created Date
2025-08-29