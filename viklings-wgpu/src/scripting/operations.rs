use std::sync::Mutex;
use deno_core::op2;
use log::{debug, info};
use crate::ecs::{EntityManager, Position, Render, Controllable, Hitbox};
use crate::ecs::{RenderProps, RenderShape, ControllableProps, HitboxProps, InputType, BoundsType};
use crate::physics::{CollisionSystem, CollisionEventData};
use crate::graphics::{UISystem, TextOptions};
use crate::timing::{GameClock, GameTimer, FrameLimiter};
use crate::stats::StatsCollector;

// Global state for triangle position (accessible from TypeScript)
static TRIANGLE_POSITION: Mutex<[f32; 2]> = Mutex::new([0.0, 0.0]);

// Global ECS instance
static ENTITY_MANAGER: Mutex<Option<EntityManager>> = Mutex::new(None);

// Global collision system
static COLLISION_SYSTEM: Mutex<Option<CollisionSystem>> = Mutex::new(None);

// Global UI system
static UI_SYSTEM: Mutex<Option<UISystem>> = Mutex::new(None);

// Global game clock
static GAME_CLOCK: Mutex<Option<GameClock>> = Mutex::new(None);

// Global game timer
static GAME_TIMER: Mutex<Option<GameTimer>> = Mutex::new(None);

// Global stats collector
static STATS_COLLECTOR: Mutex<Option<StatsCollector>> = Mutex::new(None);

// Global frame limiter
static FRAME_LIMITER: Mutex<Option<FrameLimiter>> = Mutex::new(None);

// TypeScript op to move triangle to specific position
#[op2(fast)]
pub fn op_move_triangle_to(x: f32, y: f32) -> bool {
    // Convert screen coordinates (800x600) to normalized device coordinates (-1.0 to 1.0)
    let ndc_x = (x / 400.0) - 1.0;  // Convert 0-800 to -1.0 to 1.0
    let ndc_y = 1.0 - (y / 300.0); // Convert 0-600 to 1.0 to -1.0 (flip Y)
    
    if let Ok(mut pos) = TRIANGLE_POSITION.lock() {
        pos[0] = ndc_x;
        pos[1] = ndc_y;
        debug!("Triangle moved to screen coords ({}, {}) -> NDC ({:.2}, {:.2})", x, y, ndc_x, ndc_y);
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
        info!("ECS initialized");
    } else {
        success = false;
    }
    
    if let Ok(mut collision_system) = COLLISION_SYSTEM.lock() {
        *collision_system = Some(CollisionSystem::new());
        info!("Collision system initialized");
    } else {
        success = false;
    }
    
    if let Ok(mut ui_system) = UI_SYSTEM.lock() {
        *ui_system = Some(UISystem::new());
        info!("UI system initialized");
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
            debug!("Created entity {}", entity_id);
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
            debug!("Added position ({}, {}) to entity {}", x, y, entity_id);
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
            debug!("Added render component to entity {} ({}x{}, color: {:?})", 
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
            debug!("Added controllable component to entity {} (speed: {}, input: {})", 
                    entity_id, props.movement_speed, props.input_type);
            return true;
        }
    }
    false
}

// Remove controllable component
#[op2(fast)]
pub fn op_remove_controllable(entity_id: u32) -> bool {
    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            manager.remove_controllable(entity_id);
            debug!("Removed controllable component from entity {}", entity_id);
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
            debug!("Added hitbox to entity {} ({}x{}, offset: ({}, {}), layer: {})", 
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

// UI system operations
#[op2]
pub fn op_show_text(#[string] text: String, x: f32, y: f32, #[serde] options: Option<TextOptions>) -> u32 {
    if let Ok(mut ui_system_opt) = UI_SYSTEM.lock() {
        if let Some(ref mut ui_system) = *ui_system_opt {
            return ui_system.show_text(text, x, y, options);
        }
    }
    0
}

#[op2(fast)]
pub fn op_hide_text(element_id: u32) -> bool {
    if let Ok(mut ui_system_opt) = UI_SYSTEM.lock() {
        if let Some(ref mut ui_system) = *ui_system_opt {
            return ui_system.hide_text(element_id);
        }
    }
    false
}

#[op2(fast)]
pub fn op_clear_ui() -> bool {
    if let Ok(mut ui_system_opt) = UI_SYSTEM.lock() {
        if let Some(ref mut ui_system) = *ui_system_opt {
            return ui_system.clear_ui();
        }
    }
    false
}

pub fn with_ui_system_mut<T, F>(f: F) -> Option<T>
where
    F: FnOnce(&mut UISystem) -> T,
{
    if let Ok(mut ui_system_opt) = UI_SYSTEM.lock() {
        if let Some(ref mut ui_system) = *ui_system_opt {
            return Some(f(ui_system));
        }
    }
    None
}

pub fn with_game_clock_mut<T, F>(f: F) -> Option<T>
where
    F: FnOnce(&mut GameClock) -> T,
{
    if let Ok(mut game_clock_opt) = GAME_CLOCK.lock() {
        if let Some(ref mut game_clock) = *game_clock_opt {
            return Some(f(game_clock));
        }
    }
    None
}

pub fn with_game_timer<T, F>(f: F) -> Option<T>
where
    F: FnOnce(&GameTimer) -> T,
{
    if let Ok(game_timer_opt) = GAME_TIMER.lock() {
        if let Some(ref game_timer) = *game_timer_opt {
            return Some(f(game_timer));
        }
    }
    None
}

// TypeScript operation to schedule an event after a certain number of seconds
#[op2(fast)]
pub fn op_schedule_event_at_seconds(seconds: f64, #[string] callback_name: String) -> u32 {
    with_game_clock_mut(|game_clock| {
        game_clock.schedule_event_at_seconds(seconds, callback_name)
    }).unwrap_or(0)
}

// TypeScript operation to schedule an event after a certain number of milliseconds
#[op2(fast)]
pub fn op_schedule_event_at_millis(#[bigint] millis: u64, #[string] callback_name: String) -> u32 {
    with_game_clock_mut(|game_clock| {
        game_clock.schedule_event_at_millis(millis, callback_name)
    }).unwrap_or(0)
}

// TypeScript operation to schedule a repeating event
#[op2(fast)]
pub fn op_schedule_repeating_event(#[bigint] start_time_millis: u64, #[bigint] interval_millis: u64, #[string] callback_name: String) -> u32 {
    with_game_clock_mut(|game_clock| {
        game_clock.schedule_repeating_event(start_time_millis, interval_millis, callback_name)
    }).unwrap_or(0)
}

// TypeScript operation to schedule a repeating event starting now
#[op2(fast)]
pub fn op_schedule_repeating_event_now(#[bigint] interval_millis: u64, #[string] callback_name: String) -> u32 {
    let result = with_game_clock_mut(|game_clock| {
        let event_id = game_clock.schedule_repeating_event_now(interval_millis, callback_name.clone());
        debug!("Scheduled event '{}' with interval {} ms, got ID: {}", callback_name, interval_millis, event_id);
        event_id
    });
    
    match result {
        Some(id) => id,
        None => {
            debug!("Failed to schedule event - game clock not available");
            0
        }
    }
}

// TypeScript operation to cancel a scheduled event
#[op2(fast)]
pub fn op_cancel_event(event_id: u32) -> bool {
    with_game_clock_mut(|game_clock| {
        game_clock.cancel_event(event_id)
    }).unwrap_or(false)
}

// TypeScript operation to get current game time in seconds
#[op2(fast)]
pub fn op_get_game_time_seconds() -> f64 {
    with_game_timer(|game_timer| {
        game_timer.get_game_time()
    }).unwrap_or(0.0)
}

// TypeScript operation to get current game time in milliseconds
#[op2(fast)]
#[bigint] pub fn op_get_game_time_millis() -> u64 {
    with_game_timer(|game_timer| {
        game_timer.get_game_time_millis()
    }).unwrap_or(0)
}

// Initialize game clock and timer
pub fn init_game_clock_and_timer() {
    if let Ok(mut game_clock_opt) = GAME_CLOCK.lock() {
        *game_clock_opt = Some(GameClock::new());
    }
    
    if let Ok(mut game_timer_opt) = GAME_TIMER.lock() {
        let mut timer = GameTimer::new();
        timer.init();
        *game_timer_opt = Some(timer);
    }
}

// Initialize stats collector
pub fn init_stats_collector() {
    if let Ok(mut stats_opt) = STATS_COLLECTOR.lock() {
        *stats_opt = Some(StatsCollector::new());
    }
}

// Initialize frame limiter
pub fn init_frame_limiter(target_fps: f32) {
    if let Ok(mut frame_limiter_opt) = FRAME_LIMITER.lock() {
        *frame_limiter_opt = Some(FrameLimiter::new(target_fps));
    }
}

// Update stats (called from main app)
pub fn update_stats() {
    if let Ok(mut stats_opt) = STATS_COLLECTOR.lock() {
        if let Some(ref mut stats) = *stats_opt {
            stats.record_frame();
        }
    }
}

// TypeScript operation to get current FPS
#[op2(fast)]
pub fn op_get_fps() -> f32 {
    if let Ok(stats_opt) = STATS_COLLECTOR.lock() {
        if let Some(ref stats) = *stats_opt {
            return stats.get_avg_fps();
        }
    }
    0.0
}

// TypeScript operation to get instant FPS
#[op2(fast)]
pub fn op_get_instant_fps() -> f32 {
    if let Ok(stats_opt) = STATS_COLLECTOR.lock() {
        if let Some(ref stats) = *stats_opt {
            return stats.get_instant_fps();
        }
    }
    0.0
}

// TypeScript operation to get total frames
#[op2(fast)]
#[bigint] pub fn op_get_total_frames() -> u64 {
    if let Ok(stats_opt) = STATS_COLLECTOR.lock() {
        if let Some(ref stats) = *stats_opt {
            return stats.get_stats().total_frames;
        }
    }
    0
}

// TypeScript operation to initialize game engine with target FPS
#[op2(fast)]
pub fn op_init_engine(target_fps: f32) -> bool {
    // Initialize frame limiter with specified FPS
    if let Ok(mut frame_limiter_opt) = FRAME_LIMITER.lock() {
        *frame_limiter_opt = Some(FrameLimiter::new(target_fps));
    }
    
    debug!("Game engine initialized with target FPS: {}", target_fps);
    true
}

// TypeScript operation to set target FPS
#[op2(fast)]
pub fn op_set_target_fps(fps: f32) -> bool {
    if let Ok(mut frame_limiter_opt) = FRAME_LIMITER.lock() {
        *frame_limiter_opt = Some(FrameLimiter::new(fps));
        debug!("Target FPS changed to: {}", fps);
        return true;
    }
    false
}

// TypeScript operation to get target FPS
#[op2(fast)]
pub fn op_get_target_fps() -> f32 {
    if let Ok(frame_limiter_opt) = FRAME_LIMITER.lock() {
        if let Some(ref frame_limiter) = *frame_limiter_opt {
            return frame_limiter.get_target_fps();
        }
    }
    60.0 // Default fallback
}

// Get frame limiter for app use
pub fn with_frame_limiter<T, F>(f: F) -> Option<T>
where
    F: FnOnce(&FrameLimiter) -> T,
{
    if let Ok(frame_limiter_opt) = FRAME_LIMITER.lock() {
        if let Some(ref frame_limiter) = *frame_limiter_opt {
            return Some(f(frame_limiter));
        }
    }
    None
}

// TypeScript operation to get engine uptime
#[op2(fast)]
pub fn op_get_uptime() -> f32 {
    if let Ok(stats_opt) = STATS_COLLECTOR.lock() {
        if let Some(ref stats) = *stats_opt {
            return stats.get_uptime();
        }
    }
    0.0
}