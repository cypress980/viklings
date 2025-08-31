use std::sync::Mutex;
use deno_core::op2;
use crate::ecs::{EntityManager, Position, Render, Controllable, Hitbox};
use crate::ecs::{RenderProps, RenderShape, ControllableProps, HitboxProps, InputType, BoundsType};
use crate::physics::{CollisionSystem, CollisionEventData};

// Global state for triangle position (accessible from TypeScript)
static TRIANGLE_POSITION: Mutex<[f32; 2]> = Mutex::new([0.0, 0.0]);

// Global ECS instance
static ENTITY_MANAGER: Mutex<Option<EntityManager>> = Mutex::new(None);

// Global collision system
static COLLISION_SYSTEM: Mutex<Option<CollisionSystem>> = Mutex::new(None);

// TypeScript op to move triangle to specific position
#[op2(fast)]
pub fn op_move_triangle_to(x: f32, y: f32) -> bool {
    // Convert screen coordinates (800x600) to normalized device coordinates (-1.0 to 1.0)
    let ndc_x = (x / 400.0) - 1.0;  // Convert 0-800 to -1.0 to 1.0
    let ndc_y = 1.0 - (y / 300.0); // Convert 0-600 to 1.0 to -1.0 (flip Y)
    
    if let Ok(mut pos) = TRIANGLE_POSITION.lock() {
        pos[0] = ndc_x;
        pos[1] = ndc_y;
        println!("Triangle moved to screen coords ({}, {}) -> NDC ({:.2}, {:.2})", x, y, ndc_x, ndc_y);
        true
    } else {
        false
    }
}

// Initialize ECS
#[op2(fast)]
pub fn op_init_ecs() -> bool {
    // Initialize ECS and collision system
    let mut success = true;
    
    if let Ok(mut manager) = ENTITY_MANAGER.lock() {
        *manager = Some(EntityManager::new());
        println!("ECS initialized");
    } else {
        success = false;
    }
    
    if let Ok(mut collision_system) = COLLISION_SYSTEM.lock() {
        *collision_system = Some(CollisionSystem::new());
        println!("Collision system initialized");
    } else {
        success = false;
    }
    
    success
}

// Create entity
#[op2(fast)]
pub fn op_create_entity() -> u32 {
    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            let entity_id = manager.create_entity();
            println!("Created entity {}", entity_id);
            return entity_id;
        }
    }
    0 // Return 0 as invalid entity ID
}

// Add position component
#[op2(fast)]
pub fn op_add_position(entity_id: u32, x: f32, y: f32) -> bool {
    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            manager.add_position(entity_id, Position { x, y });
            println!("Added position ({}, {}) to entity {}", x, y, entity_id);
            return true;
        }
    }
    false
}

// Add render component - simplified for now with serde
#[op2]
pub fn op_add_render(entity_id: u32, #[serde] props: RenderProps) -> bool {
    let shape = match props.shape.as_str() {
        "square" => RenderShape::Square,
        "circle" => RenderShape::Circle,
        _ => RenderShape::Square,
    };

    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            manager.add_render(entity_id, Render {
                width: props.width,
                height: props.height,
                color: props.color,
                shape,
            });
            println!("Added render component to entity {} ({}x{}, color: {:?})", 
                    entity_id, props.width, props.height, props.color);
            return true;
        }
    }
    false
}

// Add controllable component
#[op2]
pub fn op_add_controllable(entity_id: u32, #[serde] props: ControllableProps) -> bool {
    let input_type = match props.input_type.as_str() {
        "arrow_keys" => InputType::ArrowKeys,
        "wasd" => InputType::WASD,
        _ => InputType::ArrowKeys,
    };

    let bounds = match props.bounds.as_str() {
        "screen" => BoundsType::Screen,
        "none" => BoundsType::None,
        _ => BoundsType::Screen,
    };

    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            manager.add_controllable(entity_id, Controllable {
                movement_speed: props.movement_speed,
                input_type,
                bounds,
            });
            println!("Added controllable component to entity {} (speed: {}, input: {})", 
                    entity_id, props.movement_speed, props.input_type);
            return true;
        }
    }
    false
}

#[op2]
pub fn op_add_hitbox(entity_id: u32, #[serde] props: HitboxProps) -> bool {
    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            manager.add_hitbox(entity_id, Hitbox {
                size: [props.width, props.height],
                offset: [props.offset_x.unwrap_or(0.0), props.offset_y.unwrap_or(0.0)],
                layer: props.layer.unwrap_or(0),
                active: true,
            });
            println!("Added hitbox to entity {} ({}x{}, offset: ({}, {}), layer: {})", 
                entity_id, props.width, props.height, 
                props.offset_x.unwrap_or(0.0), props.offset_y.unwrap_or(0.0), 
                props.layer.unwrap_or(0));
            return true;
        }
    }
    
    false
}

#[op2]
#[serde]
pub fn op_poll_collision_events() -> Vec<CollisionEventData> {
    if let Ok(mut collision_system_opt) = COLLISION_SYSTEM.lock() {
        if let Some(ref mut collision_system) = *collision_system_opt {
            let events = collision_system.get_serializable_events();
            
            // Clear events after polling (events are only consumed once)
            collision_system.events.clear();
            
            return events;
        }
    }
    Vec::new()
}

#[op2(fast)]
pub fn op_set_entity_color(entity_id: u32, r: f32, g: f32, b: f32) -> bool {
    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            return manager.set_entity_color(entity_id, [r, g, b]);
        }
    }
    false
}

#[op2(fast)]
pub fn op_set_entity_position(entity_id: u32, x: f32, y: f32) -> bool {
    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            if let Some(position) = manager.get_position_mut(entity_id) {
                position.x = x;
                position.y = y;
                return true;
            }
        }
    }
    false
}

// Getter functions for other systems to access global state
pub fn with_entity_manager<T, F>(f: F) -> Option<T>
where
    F: FnOnce(&EntityManager) -> T,
{
    if let Ok(manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref manager) = *manager_opt {
            return Some(f(manager));
        }
    }
    None
}

pub fn with_entity_manager_mut<T, F>(f: F) -> Option<T>
where
    F: FnOnce(&mut EntityManager) -> T,
{
    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            return Some(f(manager));
        }
    }
    None
}

pub fn with_collision_system_mut<T, F>(f: F) -> Option<T>
where
    F: FnOnce(&mut CollisionSystem) -> T,
{
    if let Ok(mut collision_system_opt) = COLLISION_SYSTEM.lock() {
        if let Some(ref mut collision_system) = *collision_system_opt {
            return Some(f(collision_system));
        }
    }
    None
}