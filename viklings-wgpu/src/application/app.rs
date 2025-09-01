use std::sync::Arc;
use std::time::{Instant, Duration};
use std::process::Command;
use winit::{
    application::ApplicationHandler,
    event::{WindowEvent, ElementState, KeyEvent},
    event_loop::{ActiveEventLoop, EventLoop},
    window::{Window, WindowId},
    keyboard::{KeyCode, PhysicalKey},
};
use log::{debug, info, error};

use crate::graphics::GraphicsEngine;
use crate::scripting::{ScriptingEngine, operations};
use crate::ecs::{InputState, systems::MovementSystem};
use crate::timing::{GameTimer, GameClock, FrameLimiter};
// Removed unused CollisionSystem import - it's accessed through operations module
use super::timer::Timer;

pub struct App {
    window: Option<Arc<Window>>,
    graphics_engine: Option<GraphicsEngine>,
    scripting_engine: Option<ScriptingEngine>,
    movement_system: Option<MovementSystem>,
    input_state: InputState,
    timer: Timer, // Keep old timer for compatibility
    game_timer: GameTimer,
    game_clock: GameClock,
    frame_limiter: FrameLimiter,
    last_frame_time: Instant,
}

impl App {
    pub fn new() -> Self {
        Self {
            window: None,
            graphics_engine: None,
            scripting_engine: None,
            movement_system: None,
            input_state: InputState::default(),
            timer: Timer::new(),
            game_timer: GameTimer::new(),
            game_clock: GameClock::new(),
            frame_limiter: FrameLimiter::new(60.0), // 60 FPS target
            last_frame_time: Instant::now(),
        }
    }

    pub fn run() {
        let event_loop = EventLoop::new().unwrap();
        let mut app = App::new();
        
        event_loop.run_app(&mut app).unwrap();
    }
}

impl ApplicationHandler for App {
    fn resumed(&mut self, event_loop: &ActiveEventLoop) {
        let window = Arc::new(
            event_loop
                .create_window(
                    winit::window::Window::default_attributes()
                        .with_title("Viklings wgpu Triangle")
                        .with_inner_size(winit::dpi::LogicalSize::new(800, 600))
                )
                .unwrap(),
        );

        let graphics_engine = pollster::block_on(GraphicsEngine::new(window.clone()));
        
        // Initialize timing systems FIRST
        self.game_timer.init();
        
        // Initialize global game clock and timer for TypeScript access BEFORE scripting
        operations::init_game_clock_and_timer();
        
        // Initialize scripting engine
        let mut scripting_engine = ScriptingEngine::new();
        
        // Load square game with automated test
        if let Err(e) = scripting_engine.load_game_test() {
            error!("Failed to load game test: {}", e);
        }
        
        // Initialize movement system
        let movement_system = MovementSystem::new();
        
        // Schedule some example events
        self.game_clock.schedule_event_at_seconds(3.0, "show_fps".to_string());
        self.game_clock.schedule_repeating_event_now(1000, "fps_report".to_string());
        
        self.window = Some(window);
        self.graphics_engine = Some(graphics_engine);
        self.scripting_engine = Some(scripting_engine);
        self.movement_system = Some(movement_system);
    }

    fn window_event(&mut self, event_loop: &ActiveEventLoop, _window_id: WindowId, event: WindowEvent) {
        match event {
            WindowEvent::CloseRequested => {
                info!("Window close requested. Shutting down...");
                event_loop.exit();
            }
            WindowEvent::Resized(physical_size) => {
                if let Some(ref mut graphics_engine) = self.graphics_engine {
                    graphics_engine.resize(physical_size);
                }
            }
            WindowEvent::KeyboardInput {
                event: KeyEvent {
                    state: key_state,
                    physical_key: PhysicalKey::Code(key_code),
                    ..
                },
                ..
            } => {
                let pressed = key_state == ElementState::Pressed;
                self.handle_input(key_code, pressed);
            }
            WindowEvent::RedrawRequested => {
                self.render();
                if let Some(ref window) = self.window {
                    window.request_redraw();
                }
            }
            _ => {}
        }
    }

    fn about_to_wait(&mut self, _event_loop: &ActiveEventLoop) {
        // Check if we should timeout and exit
        let elapsed = self.timer.elapsed();
        
        // Take a screenshot after 3 seconds to capture initial state
        if elapsed > Duration::from_secs(3) && !self.timer.screenshot_taken {
            debug!("Taking screenshot after 3 seconds (initial separated state)...");
            let _ = Command::new("../agent-notes/agent-tools/screenshot.sh")
                .args(["--fullscreen", "collision-test-initial-state"])
                .output();
            self.timer.screenshot_taken = true;
        }
        
        // Keep running indefinitely for normal usage
        // Tests should handle their own process management
        
        if let Some(ref window) = self.window {
            window.request_redraw();
        }
    }
}

impl App {
    fn handle_input(&mut self, key_code: KeyCode, pressed: bool) -> bool {
        match key_code {
            KeyCode::ArrowUp => {
                self.input_state.up = pressed;
                if pressed { debug!("Arrow UP pressed"); }
                true
            }
            KeyCode::ArrowDown => {
                self.input_state.down = pressed;
                if pressed { debug!("Arrow DOWN pressed"); }
                true
            }
            KeyCode::ArrowLeft => {
                self.input_state.left = pressed;
                if pressed { debug!("Arrow LEFT pressed"); }
                true
            }
            KeyCode::ArrowRight => {
                self.input_state.right = pressed;
                if pressed { debug!("Arrow RIGHT pressed"); }
                true
            }
            KeyCode::KeyR => {
                if pressed {
                    debug!("Reset key pressed");
                    // Call reset game function
                    if let Some(ref mut scripting_engine) = self.scripting_engine {
                        if let Err(e) = scripting_engine.execute_script("<reset>", "resetGame();".to_string()) {
                            error!("Reset game error: {}", e);
                        }
                    }
                }
                true
            }
            _ => false
        }
    }

    fn update(&mut self) {
        // Get delta time from game timer
        let delta_time = self.game_timer.get_elapsed_time();
        let current_time_millis = self.game_timer.get_game_time_millis();
        
        // Process scheduled events
        let fired_events = self.game_clock.update(current_time_millis);
        for event in fired_events {
            self.handle_game_event(&event);
        }
        
        // Update global game clock for TypeScript events  
        let global_fired_events = operations::with_game_clock_mut(|game_clock| {
            game_clock.update(current_time_millis)
        }).unwrap_or_default();
        
        // Handle global events from TypeScript
        for event in global_fired_events {
            self.handle_game_event(&event);
        }
        
        // Update movement system with proper delta time
        if let Some(ref mut movement_system) = self.movement_system {
            operations::with_entity_manager_mut(|entity_manager| {
                movement_system.update(entity_manager, &self.input_state, delta_time);
            });
        }
        
        // Update collision system
        operations::with_collision_system_mut(|collision_system| {
            operations::with_entity_manager(|entity_manager| {
                collision_system.update(entity_manager);
            });
        });
        
        // Call TypeScript game update function
        if let Some(ref mut scripting_engine) = self.scripting_engine {
            if let Err(e) = scripting_engine.call_game_update() {
                error!("Game update error: {}", e);
            }
        }
    }

    fn render(&mut self) {
        // Apply frame rate limiting
        self.frame_limiter.sync(self.last_frame_time);
        self.last_frame_time = std::time::Instant::now();
        
        // Update all systems
        self.update();
        
        // Render using graphics engine
        if let Some(ref mut graphics_engine) = self.graphics_engine {
            let render_result = operations::with_entity_manager(|entity_manager| {
                graphics_engine.render(entity_manager)
            });
            
            if let Some(result) = render_result {
                match result {
                    Ok(_) => {}
                    Err(wgpu::SurfaceError::Lost) => graphics_engine.resize(graphics_engine.size),
                    Err(wgpu::SurfaceError::OutOfMemory) => {
                        error!("Out of memory!");
                        return;
                    }
                    Err(e) => error!("Render error: {:?}", e),
                }
            }
        }

        // Render UI elements (console output for now)
        operations::with_ui_system_mut(|ui_system| {
            ui_system.render();
        });

        // Update FPS counter
        self.timer.increment_frame();
        let elapsed = self.timer.elapsed();
        if elapsed.as_secs() >= 1 {
            let fps = self.timer.fps();
            debug!("FPS: {:.1}", fps);
            self.timer.reset();
        }
    }

    fn handle_game_event(&mut self, event: &crate::timing::GameEvent) {
        use log::{debug, info};
        
        match event.callback_name.as_str() {
            "show_fps" => {
                let fps = self.timer.fps();
                info!("Current FPS: {:.1}", fps);
            }
            "fps_report" => {
                let fps = self.timer.fps();
                debug!("FPS Report: {:.1}", fps);
            }
            _ => {
                // Forward unknown events to scripting engine
                if let Some(ref mut scripting_engine) = self.scripting_engine {
                    let script = format!("if (typeof {} === 'function') {{ {}(); }}", event.callback_name, event.callback_name);
                    if let Err(e) = scripting_engine.execute_script("game_event", script) {
                        debug!("Game event callback error: {}", e);
                    }
                }
            }
        }
    }
}