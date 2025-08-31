use serde::Deserialize;

// ========== ECS COMPONENTS ==========

#[derive(Debug, Clone)]
pub struct Position {
    pub x: f32,
    pub y: f32,
}

#[derive(Debug, Clone)]
pub struct Render {
    pub width: f32,
    pub height: f32,
    pub color: [f32; 3], // RGB
    pub shape: RenderShape,
}

#[derive(Debug, Clone)]
pub enum RenderShape {
    Square,
    Circle,
}

#[derive(Debug, Clone)]
pub struct Controllable {
    pub movement_speed: f32, // pixels per second
    pub input_type: InputType,
    pub bounds: BoundsType,
}

#[derive(Debug, Clone)]
pub struct Hitbox {
    pub size: [f32; 2],      // width, height of collision box
    pub offset: [f32; 2],    // offset from entity position (for centered hitboxes)
    pub layer: u32,          // collision layer (for filtering what collides)
    pub active: bool,        // can be disabled temporarily
}

#[derive(Debug, Clone)]
pub enum InputType {
    ArrowKeys,
    WASD,
}

#[derive(Debug, Clone)]
pub enum BoundsType {
    Screen,
    None,
}

// Input state tracking
#[derive(Debug, Default)]
pub struct InputState {
    pub up: bool,
    pub down: bool,
    pub left: bool,
    pub right: bool,
}

// ========== TYPESCRIPT INTEGRATION STRUCTS ==========

#[derive(Deserialize)]
pub struct RenderProps {
    pub width: f32,
    pub height: f32,
    pub color: [f32; 3],
    pub shape: String,
}

#[derive(Deserialize)]
pub struct ControllableProps {
    pub movement_speed: f32,
    pub input_type: String,
    pub bounds: String,
}

#[derive(Deserialize)]
pub struct HitboxProps {
    pub width: f32,
    pub height: f32,
    pub offset_x: Option<f32>,
    pub offset_y: Option<f32>,
    pub layer: Option<u32>,
}