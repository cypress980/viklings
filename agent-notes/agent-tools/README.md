# Agent Tools

This directory contains utilities to assist agents in development and testing tasks.

## Tools Available

### 📸 screenshot.sh - Visual Feedback Tool

**Purpose**: Capture screenshots of application windows for visual verification during development.

**Location**: `./screenshot.sh`

**Usage Examples**:
```bash
# Interactive window selection (recommended for first use)
./screenshot.sh

# Capture specific application window
./screenshot.sh "viklings-wgpu"

# Capture with custom filename
./screenshot.sh "viklings-wgpu" triangle-test-1

# Full screen capture
./screenshot.sh --fullscreen

# List available windows (macOS only)
./screenshot.sh --list

# Show help
./screenshot.sh --help
```

**Output**: Screenshots saved to `../screenshots/` as PNG files

**Platform Support**:
- ✅ **macOS**: Full window detection and capture
- ✅ **Linux**: Supports gnome-screenshot, scrot, ImageMagick
- ✅ **Windows**: PowerShell-based screen capture

## Agent Development Workflow

### Visual Verification Process

1. **Start Application**: Launch graphics application (e.g., wgpu triangle renderer)
2. **Capture Screenshot**: Use screenshot tool to capture window
3. **Visual Analysis**: Read and analyze the captured image
4. **Iterate**: Make changes and capture again for comparison

### Example Workflow with wgpu Triangle

```bash
# Terminal 1: Start triangle renderer
cd viklings-wgpu
cargo run &

# Terminal 2: Capture screenshot
cd agent-notes/agent-tools  
./screenshot.sh "viklings-wgpu" triangle-initial

# Make code changes, then capture again
./screenshot.sh "viklings-wgpu" triangle-modified

# Compare results by reading both image files
```

## Tool Requirements

### macOS
- Built-in `screencapture` command (no additional dependencies)
- Screen recording permissions may need to be granted

### Linux
- One of the following screenshot tools:
  - `gnome-screenshot` (GNOME desktop)
  - `scrot` (lightweight, install with: `sudo apt install scrot`)
  - `import` (ImageMagick, install with: `sudo apt install imagemagick`)

### Windows  
- PowerShell (built-in on modern Windows)
- .NET Framework (usually pre-installed)

## Troubleshooting

### Permission Issues (macOS)
If you get permission errors:
1. Go to System Preferences > Security & Privacy > Privacy
2. Select "Screen Recording" in the left sidebar
3. Add your terminal application (Terminal.app, iTerm2, etc.)
4. Restart the terminal and try again

### Missing Screenshot Tools (Linux)
```bash
# Install common screenshot tools
sudo apt update
sudo apt install gnome-screenshot scrot imagemagick

# Or on Red Hat/CentOS
sudo yum install gnome-screenshot scrot ImageMagick
```

### Window Not Found
- Try using `--list` to see available windows (macOS)
- Use interactive mode (no arguments) to manually select
- Check that the application is actually running and visible
- Try capturing by partial window title match

## File Organization

```
agent-notes/
├── agent-tools/
│   ├── screenshot.sh      # Cross-platform screenshot tool
│   └── README.md         # This file
└── screenshots/
    ├── triangle-test-1.png    # Captured screenshots
    ├── 2025-08-26-151030.png  # Timestamped captures
    └── README.md             # Screenshot index
```

## Future Tool Ideas

- **diff-screenshots.sh**: Compare two screenshots for visual changes
- **record-video.sh**: Record video of application for animation testing
- **color-picker.sh**: Extract color values from specific screenshot coordinates
- **window-manager.sh**: Programmatically position and resize windows for testing