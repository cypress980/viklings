# Viklings Rust+bgfx Prototype

**Status**: Phase 1 Research Complete  
**Date**: August 26, 2025  
**Ticket**: ARCH-001

## Overview

This prototype validates the Rust+bgfx technology stack as outlined in the [Vision: Next-Generation Game Engine Architecture](../agent-notes/AGENT_README.md#vision-next-generation-game-engine-architecture). The goal was to create a minimal triangle renderer using bgfx-rs.

## Quick Start

```bash
# Install Rust if needed
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Build and run research prototype  
cargo run
```

## Research Findings

### ✅ Successfully Validated

1. **Rust Toolchain**: Rust 1.89.0 installed and working perfectly on Apple Silicon
2. **Dependency Resolution**: bgfx-rs 0.17.0 and 214 total crates resolved successfully
3. **Build System**: Cargo handles complex dependency graph including native libraries
4. **Cross-Platform Support**: Proper platform detection and native library selection

### ⚠️ Challenges Discovered

#### bgfx-rs API Complexity
- **API Structure**: Uses `bgfx_rs::static_lib::*` module, not root-level exports
- **Documentation Gap**: Limited Rust examples compared to extensive C++ bgfx documentation  
- **API Differences**: Rust bindings differ significantly from C++ examples and tutorials

#### Shader Compilation Pipeline
- **External Tools Required**: bgfx shaders must be pre-compiled with bgfx command-line tools
- **Build Integration**: No simple way to integrate shader compilation into Cargo build
- **Platform Variants**: Shaders need compilation for each target platform/API

#### Window System Integration  
- **Platform-Specific Code**: Window handle extraction requires different code for macOS/Windows/Linux
- **API Evolution**: winit 0.30 has different APIs than older examples
- **Handle Compatibility**: Raw window handle types need careful mapping to bgfx expectations

## Technical Assessment

### Performance Characteristics
- **Binary Size**: ~2.5MB for minimal prototype (significant due to bgfx native library)
- **Build Time**: 6.72s for initial build, <0.1s for incremental changes
- **Memory Usage**: Not measured (blocked on successful initialization)
- **Startup Time**: Immediate (current prototype doesn't initialize graphics)

### Development Experience  
- **Compile-Test Cycle**: Fast once dependencies resolved
- **Error Messages**: Good Rust compiler errors, but bgfx errors are cryptic
- **IDE Support**: Excellent with rust-analyzer
- **Documentation**: Rust docs good for type signatures, missing usage examples

### Dependency Analysis
- **Total Crates**: 214 (includes transitive dependencies)
- **Key Dependencies**:
  - `bgfx-rs 0.17.0` - Main graphics API bindings
  - `winit 0.30` - Cross-platform windowing
  - `bytemuck 1.0` - Safe type casting for vertex data
- **Native Libraries**: bgfx C++ library compiled for each platform
- **Build Tools**: Requires C++ compiler toolchain

## Comparison with Java/LWJGL

| Aspect | Java/LWJGL | Rust/bgfx |
|--------|------------|-----------|
| **Setup Complexity** | Medium | High |
| **Documentation** | Excellent | Limited (Rust) |
| **Build Tools** | Maven (simple) | Cargo + C++ tools |
| **Performance** | Good (JIT) | Excellent (native) |
| **Memory Safety** | GC pauses | Zero-cost abstractions |
| **Cross-Platform** | JVM handles | Manual platform code |
| **Shader Pipeline** | OpenGL GLSL | bgfx cross-compilation |

## Next Steps & Recommendations

### Immediate Actions (Phase 2)
1. **Study bgfx-rs Examples**: Find and analyze working bgfx-rs projects
2. **Set Up Shader Tools**: Install bgfx shader compiler and integrate with build
3. **Complete Window Integration**: Fix platform-specific window handle code
4. **Simple Triangle**: Get basic colored triangle rendering

### Alternative Path: wgpu-rs
Consider switching to `wgpu-rs` instead of bgfx-rs:

**Advantages of wgpu-rs:**
- Native Rust implementation (not C++ bindings)
- Better Rust documentation and examples  
- WebGPU standard compliance
- Active Rust gamedev community support
- Easier shader pipeline with WGSL or SPIR-V

**Disadvantages of wgpu-rs:**
- Newer API (less mature than bgfx)
- Different from current Java/OpenGL knowledge
- May have different performance characteristics

### Architecture Decision

Based on this research, I recommend:

1. **Continue with bgfx** if native performance and cross-platform consistency are critical
2. **Switch to wgpu-rs** if development velocity and Rust ecosystem integration are priorities
3. **Evaluate godot-rust** as an alternative that provides complete game engine

## Success Criteria Analysis

| Criterion | Status | Notes |
|-----------|--------|-------|
| Rust project compiles successfully | ✅ | Complete |
| bgfx initializes without errors | ⚠️ | Blocked on API integration |
| Window opens and displays properly | ⚠️ | Window creation successful, bgfx binding pending |
| Triangle renders with solid color | ❌ | Blocked on shader compilation |
| Maintains stable 60+ FPS | ❓ | Cannot measure without working renderer |
| Clean shutdown without memory leaks | ⚠️ | Rust memory safety helps, bgfx cleanup needs testing |
| Cross-platform build process documented | ✅ | Via Cargo + platform profiles |

## Conclusion

The Rust+bgfx technology stack is **technically viable** but requires **significant additional research and setup** compared to Java/LWJGL. The core challenge is the complexity of bgfx integration rather than Rust itself.

**Recommendation**: Proceed with Phase 2 focusing on shader pipeline setup, but also prepare a wgpu-rs alternative prototype for comparison.

**Time Investment**: Estimated additional 8-16 hours needed for complete bgfx triangle renderer vs 2-4 hours for equivalent wgpu-rs implementation.

---

*This research was conducted as Phase 1 of ARCH-001 ticket for the Viklings next-generation engine architecture evaluation.*