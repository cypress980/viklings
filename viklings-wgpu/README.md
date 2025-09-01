# Viklings WGPU Game Engine

A simple 2D game engine built with Rust and WGPU, featuring entity-component-system architecture and TypeScript scripting.

## Build and Run

```bash
# Run the engine
cargo run

# Build optimized version
cargo build --release
cargo run --release
```

## Features

- Entity-Component-System (ECS) architecture
- WGPU-based rendering with instanced drawing
- TypeScript scripting via Deno
- Collision detection system
- UI text rendering
- Input handling (arrow keys, R to reset)

## Controls

- Arrow keys: Move player
- R: Reset game

The engine includes a complete square game demo where you move a blue square into a green goal to win.