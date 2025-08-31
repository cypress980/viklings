use std::time::Instant;
use super::components::*;
use super::entity_manager::{EntityManager, EntityId};

// Movement system for controllable entities
pub struct MovementSystem {
    last_frame_time: Instant,
}

impl MovementSystem {
    pub fn new() -> Self {
        Self {
            last_frame_time: Instant::now(),
        }
    }

    pub fn update(&mut self, entity_manager: &mut EntityManager, input_state: &InputState) {
        let current_time = Instant::now();
        let delta_time = current_time.duration_since(self.last_frame_time).as_secs_f32();
        self.last_frame_time = current_time;

        // Collect entity IDs and controllable data first
        let controllable_data: Vec<(EntityId, Controllable)> = entity_manager.controllables()
            .iter()
            .map(|(&id, controllable)| (id, controllable.clone()))
            .collect();

        for (entity_id, controllable) in controllable_data {
            if let Some(position) = entity_manager.get_position_mut(entity_id) {
                let old_pos = (position.x, position.y);
                let speed = controllable.movement_speed * delta_time;
                
                // Process input based on controllable input type
                match controllable.input_type {
                    InputType::ArrowKeys => {
                        if input_state.up { position.y -= speed; }
                        if input_state.down { position.y += speed; }
                        if input_state.left { position.x -= speed; }
                        if input_state.right { position.x += speed; }
                    }
                    InputType::WASD => {
                        // TODO: Implement WASD controls when needed
                    }
                }
                
                let new_pos = (position.x, position.y);
                if old_pos != new_pos {
                    println!("🏃 Entity {} moved from {:?} to {:?} (speed={:.1}, delta={:.3})", 
                            entity_id, old_pos, new_pos, controllable.movement_speed, delta_time);
                }
                
                // Apply bounds
                match controllable.bounds {
                    BoundsType::Screen => {
                        // Keep entity within screen bounds (assuming 800x600 window)
                        position.x = position.x.clamp(0.0, 800.0);
                        position.y = position.y.clamp(0.0, 600.0);
                    }
                    BoundsType::None => {
                        // No bounds checking
                    }
                }
            }
        }
    }
}