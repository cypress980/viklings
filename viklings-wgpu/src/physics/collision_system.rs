use serde::Serialize;
use crate::ecs::{EntityManager, EntityId, Position, Hitbox};
use super::aabb::AABB;

// Collision event data
#[derive(Debug, Clone)]
pub struct CollisionEvent {
    pub entity_a: EntityId,
    pub entity_b: EntityId,
    pub timestamp: f64,
}

#[derive(Serialize)]
pub struct CollisionEventData {
    pub entity_a: u32,
    pub entity_b: u32,
    pub timestamp: f64,
}

// Collision Detection System
pub struct CollisionSystem {
    pub events: Vec<CollisionEvent>,
}

impl CollisionSystem {
    pub fn new() -> Self {
        Self {
            events: Vec::new(),
        }
    }

    pub fn update(&mut self, entity_manager: &EntityManager) {
        self.events.clear();
        
        let colliders = entity_manager.get_entities_with_hitbox();
        
        // Check all pairs for collision
        for i in 0..colliders.len() {
            for j in (i + 1)..colliders.len() {
                let (id_a, pos_a, hitbox_a) = &colliders[i];
                let (id_b, pos_b, hitbox_b) = &colliders[j];
                
                if hitbox_a.active && hitbox_b.active && self.check_collision(pos_a, hitbox_a, pos_b, hitbox_b) {
                    self.events.push(CollisionEvent {
                        entity_a: *id_a,
                        entity_b: *id_b,
                        timestamp: std::time::SystemTime::now()
                            .duration_since(std::time::UNIX_EPOCH)
                            .unwrap()
                            .as_secs_f64(),
                    });
                }
            }
        }
    }
    
    pub fn check_collision(&self, pos_a: &Position, hitbox_a: &Hitbox, pos_b: &Position, hitbox_b: &Hitbox) -> bool {
        let bounds_a = self.get_aabb_bounds(pos_a, hitbox_a);
        let bounds_b = self.get_aabb_bounds(pos_b, hitbox_b);
        
        // AABB overlap test - use <= for inclusive bounds to handle zero-size hitboxes
        bounds_a.left <= bounds_b.right &&
        bounds_a.right >= bounds_b.left &&
        bounds_a.top <= bounds_b.bottom &&
        bounds_a.bottom >= bounds_b.top
    }
    
    pub fn get_aabb_bounds(&self, pos: &Position, hitbox: &Hitbox) -> AABB {
        let center_x = pos.x + hitbox.offset[0];
        let center_y = pos.y + hitbox.offset[1];
        let half_width = hitbox.size[0] / 2.0;
        let half_height = hitbox.size[1] / 2.0;
        
        AABB {
            left: center_x - half_width,
            right: center_x + half_width,
            top: center_y - half_height,
            bottom: center_y + half_height,
        }
    }
    
    pub fn get_events(&self) -> &Vec<CollisionEvent> {
        &self.events
    }

    pub fn get_serializable_events(&self) -> Vec<CollisionEventData> {
        self.events
            .iter()
            .map(|event| CollisionEventData {
                entity_a: event.entity_a,
                entity_b: event.entity_b,
                timestamp: event.timestamp,
            })
            .collect()
    }
}