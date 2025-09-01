pub mod ecs;
pub mod graphics;
pub mod physics;
pub mod scripting;
pub mod application;
pub mod timing;

// Re-export key types for convenience
pub use ecs::{EntityManager, EntityId};
pub use graphics::GraphicsEngine;
pub use physics::CollisionSystem;