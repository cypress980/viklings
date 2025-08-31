// AABB (Axis-Aligned Bounding Box) for collision detection
#[derive(Debug, Clone)]
pub struct AABB {
    pub left: f32,
    pub right: f32,
    pub top: f32,
    pub bottom: f32,
}