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

use crate::graphics::GraphicsEngine;
use crate::scripting::{ScriptingEngine, operations};
use crate::ecs::{InputState, systems::MovementSystem};
// Removed unused CollisionSystem import - it's accessed through operations module
use super::timer::Timer;

pub struct App {
    window: Option<Arc<Window>>,
    graphics_engine: Option<GraphicsEngine>,
    scripting_engine: Option<ScriptingEngine>,
    movement_system: Option<MovementSystem>,
    input_state: InputState,
    timer: Timer,
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
        
        // Initialize scripting engine
        let mut scripting_engine = ScriptingEngine::new();
        
        // Load collision test script
        if let Err(e) = scripting_engine.load_collision_test() {
            eprintln!("Failed to load collision test: {}", e);
        }
        
        // Initialize movement system
        let movement_system = MovementSystem::new();
        
        self.window = Some(window);
        self.graphics_engine = Some(graphics_engine);
        self.scripting_engine = Some(scripting_engine);
        self.movement_system = Some(movement_system);
    }

    fn window_event(&mut self, event_loop: &ActiveEventLoop, _window_id: WindowId, event: WindowEvent) {
        match event {
            WindowEvent::CloseRequested => {
                println!("Window close requested. Shutting down...");
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

    fn about_to_wait(&mut self, event_loop: &ActiveEventLoop) {
        // Check if we should timeout and exit
        let elapsed = self.timer.elapsed();
        
        // Take a screenshot after 3 seconds to capture initial state
        if elapsed > Duration::from_secs(3) && !self.timer.screenshot_taken {
            println!("Taking screenshot after 3 seconds (initial separated state)...");
            let _ = Command::new("../agent-notes/agent-tools/screenshot.sh")
                .args(["--fullscreen", "collision-test-initial-state"])
                .output();
            self.timer.screenshot_taken = true;
        }
        
        // Exit after 20 seconds for interactive testing
        if elapsed > Duration::from_secs(20) {
            println!("Auto-exiting after 20 seconds (allows time for collision testing)");
            event_loop.exit();
            return;
        }
        
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
                if pressed { println!("⬆️ Arrow UP pressed"); }
                true
            }
            KeyCode::ArrowDown => {
                self.input_state.down = pressed;
                if pressed { println!("⬇️ Arrow DOWN pressed"); }
                true
            }
            KeyCode::ArrowLeft => {
                self.input_state.left = pressed;
                if pressed { println!("⬅️ Arrow LEFT pressed"); }
                true
            }
            KeyCode::ArrowRight => {
                self.input_state.right = pressed;
                if pressed { println!("➡️ Arrow RIGHT pressed"); }
                true
            }
            _ => false
        }
    }

    fn update(&mut self) {
        // Update movement system
        if let Some(ref mut movement_system) = self.movement_system {
            operations::with_entity_manager_mut(|entity_manager| {
                movement_system.update(entity_manager, &self.input_state);
            });
        }
        
        // Update collision system
        operations::with_collision_system_mut(|collision_system| {
            operations::with_entity_manager(|entity_manager| {
                collision_system.update(entity_manager);
            });
        });
        
        // Call TypeScript collision checking function
        if let Some(ref mut scripting_engine) = self.scripting_engine {
            if let Err(e) = scripting_engine.call_collision_check() {
                eprintln!("Collision check error: {}", e);
            }
        }
    }

    fn render(&mut self) {
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
                        eprintln!("Out of memory!");
                        return;
                    }
                    Err(e) => eprintln!("Render error: {:?}", e),
                }
            }
        }

        // Update FPS counter
        self.timer.increment_frame();
        let elapsed = self.timer.elapsed();
        if elapsed.as_secs() >= 1 {
            let fps = self.timer.fps();
            println!("FPS: {:.1}", fps);
            self.timer.reset();
        }
    }
}