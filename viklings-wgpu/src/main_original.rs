use std::sync::{Arc, Mutex};
use std::collections::HashMap;
use wgpu::util::DeviceExt;
use winit::{
    application::ApplicationHandler,
    event::{WindowEvent, ElementState, KeyEvent},
    event_loop::{ActiveEventLoop, EventLoop},
    window::{Window, WindowId},
    keyboard::{KeyCode, PhysicalKey},
};
use std::time::{Instant, Duration};
use deno_core::{JsRuntime, RuntimeOptions, op2};
use serde::{Deserialize, Serialize};
use std::process::Command;

// Global state for triangle position (accessible from TypeScript)
static TRIANGLE_POSITION: Mutex<[f32; 2]> = Mutex::new([0.0, 0.0]);

// ========== ENTITY COMPONENT SYSTEM ==========

pub type EntityId = u32;

// ECS Components
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

// Instance data for WGPU instanced rendering
#[repr(C)]
#[derive(Copy, Clone, Debug, bytemuck::Pod, bytemuck::Zeroable)]
pub struct InstanceData {
    pub position: [f32; 2],    // Entity world position
    pub scale: [f32; 2],       // Entity size/scale  
    pub color: [f32; 4],       // RGBA color
}

impl InstanceData {
    const ATTRIBS: [wgpu::VertexAttribute; 3] = [
        wgpu::VertexAttribute {
            offset: 0,
            shader_location: 2, // After position (0) and color (1)
            format: wgpu::VertexFormat::Float32x2,
        },
        wgpu::VertexAttribute {
            offset: std::mem::size_of::<[f32; 2]>() as wgpu::BufferAddress,
            shader_location: 3,
            format: wgpu::VertexFormat::Float32x2,
        },
        wgpu::VertexAttribute {
            offset: (std::mem::size_of::<[f32; 2]>() * 2) as wgpu::BufferAddress,
            shader_location: 4,
            format: wgpu::VertexFormat::Float32x4,
        },
    ];
    
    // Create vertex buffer layout for instanced rendering
    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<InstanceData>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Instance, // Key difference!
            attributes: &Self::ATTRIBS,
        }
    }
    
    // Debug helper to print instance data
    #[cfg(debug_assertions)]
    pub fn debug_print(&self, index: usize) {
        println!("Instance {}: pos={:?}, scale={:?}, color={:?}", 
                 index, self.position, self.scale, self.color);
    }
}

// Entity Manager - core of our ECS
pub struct EntityManager {
    next_entity_id: EntityId,
    entities: Vec<EntityId>,
    positions: HashMap<EntityId, Position>,
    renders: HashMap<EntityId, Render>,
    controllables: HashMap<EntityId, Controllable>,
    hitboxes: HashMap<EntityId, Hitbox>,
}

impl EntityManager {
    pub fn new() -> Self {
        Self {
            next_entity_id: 0,
            entities: Vec::new(),
            positions: HashMap::new(),
            renders: HashMap::new(),
            controllables: HashMap::new(),
            hitboxes: HashMap::new(),
        }
    }

    pub fn create_entity(&mut self) -> EntityId {
        let id = self.next_entity_id;
        self.next_entity_id += 1;
        self.entities.push(id);
        id
    }

    pub fn add_position(&mut self, entity: EntityId, position: Position) {
        self.positions.insert(entity, position);
    }

    pub fn add_render(&mut self, entity: EntityId, render: Render) {
        self.renders.insert(entity, render);
    }

    pub fn add_controllable(&mut self, entity: EntityId, controllable: Controllable) {
        self.controllables.insert(entity, controllable);
    }

    pub fn add_hitbox(&mut self, entity: EntityId, hitbox: Hitbox) {
        self.hitboxes.insert(entity, hitbox);
    }

    pub fn get_position_mut(&mut self, entity: EntityId) -> Option<&mut Position> {
        self.positions.get_mut(&entity)
    }

    pub fn get_entities_with_render(&self) -> Vec<(EntityId, &Position, &Render)> {
        self.renders.keys()
            .filter_map(|&entity_id| {
                self.positions.get(&entity_id).map(|pos| (entity_id, pos, &self.renders[&entity_id]))
            })
            .collect()
    }

    pub fn get_controllable_entities(&self) -> Vec<(EntityId, &Position, &Controllable)> {
        self.controllables.keys()
            .filter_map(|&entity_id| {
                self.positions.get(&entity_id).map(|pos| (entity_id, pos, &self.controllables[&entity_id]))
            })
            .collect()
    }

    pub fn get_entities_with_hitbox(&self) -> Vec<(EntityId, &Position, &Hitbox)> {
        self.hitboxes.keys()
            .filter_map(|&entity_id| {
                self.positions.get(&entity_id).map(|pos| (entity_id, pos, &self.hitboxes[&entity_id]))
            })
            .collect()
    }
    
    pub fn set_entity_color(&mut self, entity: EntityId, color: [f32; 3]) -> bool {
        if let Some(render) = self.renders.get_mut(&entity) {
            render.color = color;
            true
        } else {
            false
        }
    }
}

// Collision event data
#[derive(Debug, Clone)]
pub struct CollisionEvent {
    pub entity_a: EntityId,
    pub entity_b: EntityId,
    pub timestamp: f64,
}

// AABB (Axis-Aligned Bounding Box) for collision detection
#[derive(Debug, Clone)]
pub struct AABB {
    pub left: f32,
    pub right: f32,
    pub top: f32,
    pub bottom: f32,
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
    
    fn check_collision(&self, pos_a: &Position, hitbox_a: &Hitbox, pos_b: &Position, hitbox_b: &Hitbox) -> bool {
        let bounds_a = self.get_aabb_bounds(pos_a, hitbox_a);
        let bounds_b = self.get_aabb_bounds(pos_b, hitbox_b);
        
        // AABB overlap test - use <= for inclusive bounds to handle zero-size hitboxes
        bounds_a.left <= bounds_b.right &&
        bounds_a.right >= bounds_b.left &&
        bounds_a.top <= bounds_b.bottom &&
        bounds_a.bottom >= bounds_b.top
    }
    
    fn get_aabb_bounds(&self, pos: &Position, hitbox: &Hitbox) -> AABB {
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
}

// Global ECS instance
static ENTITY_MANAGER: Mutex<Option<EntityManager>> = Mutex::new(None);

// Global collision system
static COLLISION_SYSTEM: Mutex<Option<CollisionSystem>> = Mutex::new(None);

// TypeScript op to move triangle to specific position
#[op2(fast)]
fn op_move_triangle_to(x: f32, y: f32) -> bool {
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

// ========== TYPESCRIPT ECS OPS ==========

// Initialize ECS
#[op2(fast)]
fn op_init_ecs() -> bool {
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
fn op_create_entity() -> u32 {
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
fn op_add_position(entity_id: u32, x: f32, y: f32) -> bool {
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
#[derive(Deserialize)]
struct RenderProps {
    width: f32,
    height: f32,
    color: [f32; 3],
    shape: String,
}

#[op2]
fn op_add_render(entity_id: u32, #[serde] props: RenderProps) -> bool {
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
#[derive(Deserialize)]
struct ControllableProps {
    movement_speed: f32,
    input_type: String,
    bounds: String,
}

#[op2]
fn op_add_controllable(entity_id: u32, #[serde] props: ControllableProps) -> bool {
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

#[derive(Deserialize)]
struct HitboxProps {
    width: f32,
    height: f32,
    offset_x: Option<f32>,
    offset_y: Option<f32>,
    layer: Option<u32>,
}

#[derive(Serialize)]
struct CollisionEventData {
    entity_a: u32,
    entity_b: u32,
    timestamp: f64,
}

#[op2]
fn op_add_hitbox(entity_id: u32, #[serde] props: HitboxProps) -> bool {
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
fn op_poll_collision_events() -> Vec<CollisionEventData> {
    if let Ok(mut collision_system_opt) = COLLISION_SYSTEM.lock() {
        if let Some(ref mut collision_system) = *collision_system_opt {
            let events: Vec<CollisionEventData> = collision_system.events
                .iter()
                .map(|event| CollisionEventData {
                    entity_a: event.entity_a,
                    entity_b: event.entity_b,
                    timestamp: event.timestamp,
                })
                .collect();
            
            // Clear events after polling (events are only consumed once)
            collision_system.events.clear();
            
            return events;
        }
    }
    Vec::new()
}

#[op2(fast)]
fn op_set_entity_color(entity_id: u32, r: f32, g: f32, b: f32) -> bool {
    if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
        if let Some(ref mut manager) = *manager_opt {
            return manager.set_entity_color(entity_id, [r, g, b]);
        }
    }
    false
}

#[op2(fast)]
fn op_set_entity_position(entity_id: u32, x: f32, y: f32) -> bool {
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

// Extension containing our ops
deno_core::extension!(
    triangle_ops,
    ops = [
        op_move_triangle_to,
        op_init_ecs,
        op_create_entity,
        op_add_position,
        op_add_render,
        op_add_controllable,
        op_add_hitbox,
        op_poll_collision_events,
        op_set_entity_color,
        op_set_entity_position
    ]
);

#[repr(C)]
#[derive(Copy, Clone, Debug, bytemuck::Pod, bytemuck::Zeroable)]
struct Uniforms {
    offset: [f32; 2],
    _padding: [f32; 2],
}

impl Uniforms {
    fn new() -> Self {
        Self {
            offset: [0.0, 0.0],
            _padding: [0.0, 0.0],
        }
    }
}

struct State {
    surface: wgpu::Surface<'static>,
    device: wgpu::Device,
    queue: wgpu::Queue,
    config: wgpu::SurfaceConfiguration,
    size: winit::dpi::PhysicalSize<u32>,
    render_pipeline: wgpu::RenderPipeline,
    vertex_buffer: wgpu::Buffer,
    instance_buffer: wgpu::Buffer,
    uniform_buffer: wgpu::Buffer,
    uniform_bind_group: wgpu::BindGroup,
    triangle_position: [f32; 2],
    movement_speed: f32,
    start_time: Instant,
    frame_count: u64,
    js_runtime: Option<JsRuntime>,
    input_state: InputState,
    last_frame_time: Instant,
    // Cached render state to avoid mutex contention
    cached_entity_position: Option<[f32; 2]>,
}

#[repr(C)]
#[derive(Copy, Clone, Debug, bytemuck::Pod, bytemuck::Zeroable)]
struct Vertex {
    position: [f32; 3],
    color: [f32; 3],
}

impl Vertex {
    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Vertex>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &[
                wgpu::VertexAttribute {
                    offset: 0,
                    shader_location: 0,
                    format: wgpu::VertexFormat::Float32x3,
                },
                wgpu::VertexAttribute {
                    offset: std::mem::size_of::<[f32; 3]>() as wgpu::BufferAddress,
                    shader_location: 1,
                    format: wgpu::VertexFormat::Float32x3,
                },
            ],
        }
    }
}

const VERTICES: &[Vertex] = &[
    // Large blue square made of two triangles (6 vertices) - much bigger for visibility
    // First triangle (top-left, bottom-left, top-right)
    Vertex { position: [-0.15, 0.15, 0.0], color: [0.0, 0.0, 1.0] },
    Vertex { position: [-0.15, -0.15, 0.0], color: [0.0, 0.0, 1.0] },
    Vertex { position: [0.15, 0.15, 0.0], color: [0.0, 0.0, 1.0] },
    
    // Second triangle (bottom-left, bottom-right, top-right)
    Vertex { position: [-0.15, -0.15, 0.0], color: [0.0, 0.0, 1.0] },
    Vertex { position: [0.15, -0.15, 0.0], color: [0.0, 0.0, 1.0] },
    Vertex { position: [0.15, 0.15, 0.0], color: [0.0, 0.0, 1.0] },
];

impl State {
    async fn new(window: Arc<Window>) -> Self {
        let size = window.inner_size();

        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::all(),
            ..Default::default()
        });

        let surface = instance.create_surface(window.clone()).unwrap();

        let adapter = instance.request_adapter(
            &wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::default(),
                compatible_surface: Some(&surface),
                force_fallback_adapter: false,
            },
        ).await.unwrap();

        let (device, queue) = adapter.request_device(
            &wgpu::DeviceDescriptor {
                required_features: wgpu::Features::empty(),
                required_limits: wgpu::Limits::default(),
                label: None,
            },
            None,
        ).await.unwrap();

        let surface_caps = surface.get_capabilities(&adapter);
        let surface_format = surface_caps.formats.iter()
            .copied()
            .find(|f| f.is_srgb())
            .unwrap_or(surface_caps.formats[0]);
        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format: surface_format,
            width: size.width,
            height: size.height,
            present_mode: surface_caps.present_modes[0],
            alpha_mode: surface_caps.alpha_modes[0],
            view_formats: vec![],
            desired_maximum_frame_latency: 2,
        };
        surface.configure(&device, &config);

        let shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Shader"),
            source: wgpu::ShaderSource::Wgsl(include_str!("shader.wgsl").into()),
        });

        // Create uniform buffer for triangle position
        let uniforms = Uniforms::new();
        let uniform_buffer = device.create_buffer_init(
            &wgpu::util::BufferInitDescriptor {
                label: Some("Uniform Buffer"),
                contents: bytemuck::cast_slice(&[uniforms]),
                usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
            }
        );

        // Create bind group layout for uniforms
        let uniform_bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            entries: &[
                wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::VERTEX,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                }
            ],
            label: Some("uniform_bind_group_layout"),
        });

        // Create bind group
        let uniform_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            layout: &uniform_bind_group_layout,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: uniform_buffer.as_entire_binding(),
                }
            ],
            label: Some("uniform_bind_group"),
        });

        let render_pipeline_layout = device.create_pipeline_layout(
            &wgpu::PipelineLayoutDescriptor {
                label: Some("Render Pipeline Layout"),
                bind_group_layouts: &[&uniform_bind_group_layout],
                push_constant_ranges: &[],
            }
        );

        let render_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Render Pipeline"),
            layout: Some(&render_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: "vs_main",
                buffers: &[Vertex::desc(), InstanceData::desc()], // Both vertex and instance buffers
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: "fs_main",
                targets: &[Some(wgpu::ColorTargetState {
                    format: config.format,
                    blend: Some(wgpu::BlendState::REPLACE),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                strip_index_format: None,
                front_face: wgpu::FrontFace::Ccw,
                cull_mode: Some(wgpu::Face::Back),
                polygon_mode: wgpu::PolygonMode::Fill,
                unclipped_depth: false,
                conservative: false,
            },
            depth_stencil: None,
            multisample: wgpu::MultisampleState {
                count: 1,
                mask: !0,
                alpha_to_coverage_enabled: false,
            },
            multiview: None,
        });

        let vertex_buffer = device.create_buffer_init(
            &wgpu::util::BufferInitDescriptor {
                label: Some("Vertex Buffer"),
                contents: bytemuck::cast_slice(VERTICES),
                usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
            }
        );

        // Create instance buffer for instanced rendering
        // Size for up to 100 entities (reasonable for collision testing)
        const MAX_INSTANCES: usize = 100;
        let instance_buffer_size = MAX_INSTANCES * std::mem::size_of::<InstanceData>();
        println!("Creating instance buffer: {} bytes for {} max instances", instance_buffer_size, MAX_INSTANCES);
        
        let instance_buffer = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Instance Buffer - DEV006"),
            size: instance_buffer_size as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

        // Initialize TypeScript runtime
        let mut js_runtime = JsRuntime::new(RuntimeOptions {
            extensions: vec![triangle_ops::init_ops()],
            ..Default::default()
        });

        // Initialize ECS and create test entity
        let test_script = r#"
            console.log("TypeScript runtime initialized!");
            
            // Initialize ECS
            const ecsReady = Deno.core.ops.op_init_ecs();
            console.log("ECS initialized:", ecsReady);
        "#;

        match js_runtime.execute_script("<init>", test_script) {
            Ok(_) => println!("TypeScript ECS initialization successful"),
            Err(e) => eprintln!("TypeScript execution error: {}", e),
        }

        let now = Instant::now();
        Self {
            surface,
            device,
            queue,
            config,
            size,
            render_pipeline,
            vertex_buffer,
            instance_buffer,
            uniform_buffer,
            uniform_bind_group,
            triangle_position: [0.0, 0.0],
            movement_speed: 0.2,
            start_time: now,
            frame_count: 0,
            js_runtime: Some(js_runtime),
            input_state: InputState::default(),
            last_frame_time: now,
            cached_entity_position: None,
        }
    }

    pub fn resize(&mut self, new_size: winit::dpi::PhysicalSize<u32>) {
        if new_size.width > 0 && new_size.height > 0 {
            self.size = new_size;
            self.config.width = new_size.width;
            self.config.height = new_size.height;
            self.surface.configure(&self.device, &self.config);
        }
    }

    pub fn input(&mut self, key_code: KeyCode, pressed: bool) -> bool {
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

    fn update_movement_system(&mut self) {
        let current_time = Instant::now();
        let delta_time = current_time.duration_since(self.last_frame_time).as_secs_f32();
        self.last_frame_time = current_time;

        // Process controllable entities
        if let Ok(mut manager_opt) = ENTITY_MANAGER.lock() {
            if let Some(ref mut manager) = *manager_opt {
                // Collect entity IDs and controllable data first
                let controllable_data: Vec<(EntityId, Controllable)> = manager.controllables
                    .iter()
                    .map(|(&id, controllable)| (id, controllable.clone()))
                    .collect();
                
                // Optional debug: Uncomment to see controllable entities and input state
                // if controllable_data.len() > 0 {
                //     println!("🎮 Found {} controllable entities", controllable_data.len());
                //     println!("🎮 Input state: up={}, down={}, left={}, right={}", 
                //             self.input_state.up, self.input_state.down, self.input_state.left, self.input_state.right);
                // }
                
                for (entity_id, controllable) in controllable_data {
                    if let Some(position) = manager.get_position_mut(entity_id) {
                        let old_pos = (position.x, position.y);
                        let speed = controllable.movement_speed * delta_time;
                        
                        // Process input based on controllable input type
                        match controllable.input_type {
                            InputType::ArrowKeys => {
                                if self.input_state.up { position.y -= speed; }
                                if self.input_state.down { position.y += speed; }
                                if self.input_state.left { position.x -= speed; }
                                if self.input_state.right { position.x += speed; }
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
                        
                        // Update cached position for rendering (avoid mutex in render loop)
                        self.cached_entity_position = Some([position.x, position.y]);
                    }
                }
            }
        }
    }

    fn update_uniforms(&self) {
        // Use position from global state (set by TypeScript) or local position
        let position = if let Ok(global_pos) = TRIANGLE_POSITION.lock() {
            *global_pos
        } else {
            self.triangle_position
        };
        
        let uniforms = Uniforms {
            offset: position,
            _padding: [0.0, 0.0],
        };
        
        self.queue.write_buffer(
            &self.uniform_buffer,
            0,
            bytemuck::cast_slice(&[uniforms])
        );
    }
    
    fn update_uniforms_from_entities(&self) {
        // Simple render system: render the first entity with render component as the triangle
        if let Ok(manager_opt) = ENTITY_MANAGER.lock() {
            if let Some(ref manager) = *manager_opt {
                let renderable_entities = manager.get_entities_with_render();
                
                if let Some((_entity_id, position, _render)) = renderable_entities.first() {
                    // Convert screen coordinates to NDC
                    let ndc_x = (position.x / 400.0) - 1.0;  // 0-800 -> -1 to 1
                    let ndc_y = 1.0 - (position.y / 300.0); // 0-600 -> 1 to -1 (flip Y)
                    
                    let uniforms = Uniforms {
                        offset: [ndc_x, ndc_y],
                        _padding: [0.0, 0.0],
                    };
                    
                    self.queue.write_buffer(
                        &self.uniform_buffer,
                        0,
                        bytemuck::cast_slice(&[uniforms])
                    );
                    
                    return; // Use entity position
                }
            }
        }
        // If no entities, fall through to update_uniforms()
    }
    
    fn update_rendering_from_entities(&mut self) {
        if let Ok(manager_opt) = ENTITY_MANAGER.lock() {
            if let Some(ref manager) = *manager_opt {
                let renderable_entities = manager.get_entities_with_render();
                
                if let Some((_entity_id, position, _render)) = renderable_entities.first() {
                    // Just update uniforms with entity position - geometry stays the same
                    let ndc_x = (position.x / 400.0) - 1.0;  // 0-800 -> -1 to 1
                    let ndc_y = 1.0 - (position.y / 300.0); // 0-600 -> 1 to -1 (flip Y)
                    
                    let uniforms = Uniforms {
                        offset: [ndc_x, ndc_y],
                        _padding: [0.0, 0.0],
                    };
                    
                    self.queue.write_buffer(
                        &self.uniform_buffer,
                        0,
                        bytemuck::cast_slice(&[uniforms])
                    );
                    
                    return; // Successfully rendered entity
                }
            }
        }
    }
    
    // Generate instance data from entities for instanced rendering
    fn generate_instance_data(&self) -> Vec<InstanceData> {
        let mut instance_data = Vec::new();
        
        if let Ok(manager_opt) = ENTITY_MANAGER.lock() {
            if let Some(ref manager) = *manager_opt {
                let renderable_entities = manager.get_entities_with_render();
                
                for (_entity_id, position, render) in renderable_entities {
                    // Convert world coordinates to NDC
                    let ndc_x = (position.x / 400.0) - 1.0;  // 0-800 -> -1 to 1
                    let ndc_y = 1.0 - (position.y / 300.0);  // 0-600 -> 1 to -1 (flip Y)
                    
                    // Convert size to NDC scale
                    let scale_x = render.width / 400.0;   // Width in NDC units
                    let scale_y = render.height / 300.0;  // Height in NDC units
                    
                    // Convert RGB to RGBA
                    let color = [render.color[0], render.color[1], render.color[2], 1.0];
                    
                    let instance = InstanceData {
                        position: [ndc_x, ndc_y],
                        scale: [scale_x, scale_y],
                        color,
                    };
                    
                    #[cfg(debug_assertions)]
                    instance.debug_print(instance_data.len());
                    
                    instance_data.push(instance);
                }
            }
        }
        
        #[cfg(debug_assertions)]
        {
            println!("=== Instance Data Generation ===");
            println!("Generated {} instances for rendering", instance_data.len());
            if !instance_data.is_empty() {
                println!("Instance data size: {} bytes", instance_data.len() * std::mem::size_of::<InstanceData>());
                println!("Buffer capacity: {} instances", 100); // MAX_INSTANCES from buffer creation
            }
        }
        
        instance_data
    }

    fn update_collision_system(&mut self) {
        // Update collision detection
        if let (Ok(manager_opt), Ok(mut collision_system_opt)) = (ENTITY_MANAGER.lock(), COLLISION_SYSTEM.lock()) {
            if let (Some(ref manager), Some(ref mut collision_system)) = (manager_opt.as_ref(), collision_system_opt.as_mut()) {
                collision_system.update(manager);
            }
        }
    }
    
    fn generate_square_vertices_with_color_and_size(&self, render: &Render) -> [Vertex; 6] {
        let color = render.color;
        
        // Generate square as two triangles (6 vertices)
        // Convert screen pixel size to NDC coordinates
        let half_width = (render.width / 2.0) / 400.0;  // Convert to NDC (400 = half screen width)
        let half_height = (render.height / 2.0) / 300.0; // Convert to NDC (300 = half screen height)
        
        [
            // First triangle (top-left, bottom-left, top-right)
            Vertex { position: [-half_width, half_height, 0.0], color },
            Vertex { position: [-half_width, -half_height, 0.0], color },
            Vertex { position: [half_width, half_height, 0.0], color },
            
            // Second triangle (bottom-left, bottom-right, top-right)  
            Vertex { position: [-half_width, -half_height, 0.0], color },
            Vertex { position: [half_width, -half_height, 0.0], color },
            Vertex { position: [half_width, half_height, 0.0], color },
        ]
    }

    fn render(&mut self) -> Result<(), wgpu::SurfaceError> {
        // Update movement system
        self.update_movement_system();
        
        // Update collision system
        self.update_collision_system();
        
        // Call TypeScript collision checking function
        if let Some(ref mut runtime) = self.js_runtime {
            if let Err(e) = runtime.execute_script("<collision_check>", "checkCollisions();") {
                eprintln!("Collision check error: {}", e);
            }
        }
        
        // Multi-entity rendering: get all renderable entities
        let renderable_entities: Vec<(EntityId, Position, Render)> = if let Ok(manager_opt) = ENTITY_MANAGER.lock() {
            if let Some(ref manager) = *manager_opt {
                // Clone the data to avoid lifetime issues
                manager.get_entities_with_render()
                    .into_iter()
                    .map(|(id, pos, render)| (id, pos.clone(), render.clone()))
                    .collect()
            } else {
                Vec::new()
            }
        } else {
            Vec::new()
        };
        
        let output = self.surface.get_current_texture()?;
        let view = output.texture.create_view(&wgpu::TextureViewDescriptor::default());

        let mut encoder = self.device.create_command_encoder(
            &wgpu::CommandEncoderDescriptor {
                label: Some("Render Encoder"),
            }
        );

        {
            let mut render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("Render Pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(wgpu::Color {
                            r: 0.1,
                            g: 0.2,
                            b: 0.3,
                            a: 1.0,
                        }),
                        store: wgpu::StoreOp::Store,
                    },
                })],
                depth_stencil_attachment: None,
                occlusion_query_set: None,
                timestamp_writes: None,
            });

            render_pass.set_pipeline(&self.render_pipeline);
            
            // NEW: Instanced rendering for multiple entities
            if !renderable_entities.is_empty() {
                // Generate instance data from entities  
                let mut instance_data = self.generate_instance_data();
                
                if !instance_data.is_empty() {
                    // Validate instance count doesn't exceed buffer capacity
                    const MAX_INSTANCES: usize = 100;
                    if instance_data.len() > MAX_INSTANCES {
                        eprintln!("WARNING: Too many instances ({}) for buffer capacity ({}). Truncating.", 
                                 instance_data.len(), MAX_INSTANCES);
                        instance_data.truncate(MAX_INSTANCES);
                    }
                    
                    // Update instance buffer with entity data
                    self.queue.write_buffer(
                        &self.instance_buffer,
                        0,
                        bytemuck::cast_slice(&instance_data)
                    );
                    
                    // Set global uniform (can be used for camera transforms)
                    let uniforms = Uniforms {
                        offset: [0.0, 0.0], // No global offset for instanced rendering
                        _padding: [0.0, 0.0],
                    };
                    
                    self.queue.write_buffer(
                        &self.uniform_buffer,
                        0,
                        bytemuck::cast_slice(&[uniforms])
                    );
                    
                    // Set vertex and instance buffers
                    render_pass.set_vertex_buffer(0, self.vertex_buffer.slice(..));
                    render_pass.set_vertex_buffer(1, self.instance_buffer.slice(..));
                    render_pass.set_bind_group(0, &self.uniform_bind_group, &[]);
                    
                    // Single instanced draw call for all entities!
                    render_pass.draw(0..6, 0..instance_data.len() as u32);
                    
                    #[cfg(debug_assertions)]
                    println!("✅ Instanced draw: {} entities with single draw call", instance_data.len());
                }
            }
        }

        self.queue.submit(std::iter::once(encoder.finish()));
        output.present();

        self.frame_count += 1;
        let elapsed = self.start_time.elapsed();
        if elapsed.as_secs() >= 1 {
            let fps = self.frame_count as f64 / elapsed.as_secs_f64();
            println!("FPS: {:.1}", fps);
            self.start_time = Instant::now();
            self.frame_count = 0;
        }

        Ok(())
    }
}

struct App {
    window: Option<Arc<Window>>,
    state: Option<State>,
    start_time: Instant,
    screenshot_taken: bool,
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

        let mut state = pollster::block_on(State::new(window.clone()));
        
        // Load and execute collision test script
        if let Some(ref mut runtime) = state.js_runtime {
            // Load the collision test TypeScript file
            let collision_script = std::fs::read_to_string("collision_demo.ts")
                .expect("Failed to read collision_demo.ts");
            
            match runtime.execute_script("<collision_test>", collision_script) {
                Ok(_) => println!("Collision test entities created successfully"),
                Err(e) => eprintln!("Collision test error: {}", e),
            }
        }
        
        self.window = Some(window);
        self.state = Some(state);
    }

    fn window_event(&mut self, event_loop: &ActiveEventLoop, _window_id: WindowId, event: WindowEvent) {
        match event {
            WindowEvent::CloseRequested => {
                println!("Window close requested. Shutting down...");
                event_loop.exit();
            }
            WindowEvent::Resized(physical_size) => {
                if let Some(ref mut state) = self.state {
                    state.resize(physical_size);
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
                if let Some(ref mut state) = self.state {
                    let pressed = key_state == ElementState::Pressed;
                    if state.input(key_code, pressed) {
                        // Only log on key press, not release
                        if pressed {
                            println!("Key {:?} pressed", key_code);
                        }
                    }
                }
            }
            WindowEvent::RedrawRequested => {
                if let Some(ref mut state) = self.state {
                    match state.render() {
                        Ok(_) => {}
                        Err(wgpu::SurfaceError::Lost) => state.resize(state.size),
                        Err(wgpu::SurfaceError::OutOfMemory) => event_loop.exit(),
                        Err(e) => eprintln!("{:?}", e),
                    }
                }
                if let Some(ref window) = self.window {
                    window.request_redraw();
                }
            }
            _ => {}
        }
    }

    fn about_to_wait(&mut self, event_loop: &ActiveEventLoop) {
        // Check if we should timeout and exit
        let elapsed = self.start_time.elapsed();
        
        // Take a screenshot after 3 seconds to capture initial state
        if elapsed > Duration::from_secs(3) && !self.screenshot_taken {
            println!("Taking screenshot after 3 seconds (initial separated state)...");
            let _ = Command::new("../agent-notes/agent-tools/screenshot.sh")
                .args(["--fullscreen", "collision-test-initial-state"])
                .output();
            self.screenshot_taken = true;
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

#[cfg(test)]
mod collision_tests {
    use super::*;

    #[test]
    fn test_aabb_no_overlap() {
        let collision_system = CollisionSystem::new();
        
        // Two rectangles that don't overlap
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [10.0, 10.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 20.0, y: 20.0 };
        let hitbox2 = Hitbox { size: [10.0, 10.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), false);
    }

    #[test]
    fn test_aabb_complete_overlap() {
        let collision_system = CollisionSystem::new();
        
        // Two identical rectangles that completely overlap
        let pos1 = Position { x: 10.0, y: 10.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 10.0, y: 10.0 };
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_partial_overlap() {
        let collision_system = CollisionSystem::new();
        
        // Two rectangles that partially overlap
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 10.0, y: 10.0 };
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_edge_touching() {
        let collision_system = CollisionSystem::new();
        
        // Two rectangles that touch at the edge (should be considered collision)
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 20.0, y: 0.0 }; // Right edge of first touches left edge of second
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_just_overlapping() {
        let collision_system = CollisionSystem::new();
        
        // Two rectangles that barely overlap
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 19.0, y: 0.0 }; // Just overlapping by 1 unit
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_with_offsets() {
        let collision_system = CollisionSystem::new();
        
        // Test collision with offset hitboxes
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [10.0, 10.0], offset: [5.0, 5.0], layer: 0, active: true };
        
        let pos2 = Position { x: 10.0, y: 10.0 };
        let hitbox2 = Hitbox { size: [10.0, 10.0], offset: [-5.0, -5.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_different_sizes() {
        let collision_system = CollisionSystem::new();
        
        // Small rectangle inside large rectangle
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [50.0, 50.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 5.0, y: 5.0 };
        let hitbox2 = Hitbox { size: [10.0, 10.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_bounds_calculation() {
        let collision_system = CollisionSystem::new();
        
        let pos = Position { x: 100.0, y: 200.0 };
        let hitbox = Hitbox { size: [40.0, 60.0], offset: [10.0, -5.0], layer: 0, active: true };
        
        let bounds = collision_system.get_aabb_bounds(&pos, &hitbox);
        
        // Center should be position + offset = (110, 195)
        // Half-sizes: width=20, height=30
        assert_eq!(bounds.left, 90.0);   // 110 - 20
        assert_eq!(bounds.right, 130.0); // 110 + 20
        assert_eq!(bounds.top, 165.0);   // 195 - 30
        assert_eq!(bounds.bottom, 225.0); // 195 + 30
    }

    #[test]
    fn test_collision_system_integration() {
        let mut entity_manager = EntityManager::new();
        let mut collision_system = CollisionSystem::new();
        
        // Create two overlapping entities
        let entity1 = entity_manager.create_entity();
        let entity2 = entity_manager.create_entity();
        
        entity_manager.add_position(entity1, Position { x: 0.0, y: 0.0 });
        entity_manager.add_hitbox(entity1, Hitbox { 
            size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true 
        });
        
        entity_manager.add_position(entity2, Position { x: 10.0, y: 10.0 });
        entity_manager.add_hitbox(entity2, Hitbox { 
            size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true 
        });
        
        // Run collision detection
        collision_system.update(&entity_manager);
        
        // Should detect one collision
        let events = collision_system.get_events();
        assert_eq!(events.len(), 1);
        
        // Check that the collision involves both entities (order may vary)
        let collision_entities = (events[0].entity_a, events[0].entity_b);
        assert!(
            (collision_entities.0 == entity1 && collision_entities.1 == entity2) ||
            (collision_entities.0 == entity2 && collision_entities.1 == entity1),
            "Expected collision between entities {} and {}, but got collision between {} and {}",
            entity1, entity2, collision_entities.0, collision_entities.1
        );
    }

    #[test]
    fn test_inactive_hitbox_no_collision() {
        let mut entity_manager = EntityManager::new();
        let mut collision_system = CollisionSystem::new();
        
        // Create two overlapping entities, but make one inactive
        let entity1 = entity_manager.create_entity();
        let entity2 = entity_manager.create_entity();
        
        entity_manager.add_position(entity1, Position { x: 0.0, y: 0.0 });
        entity_manager.add_hitbox(entity1, Hitbox { 
            size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true 
        });
        
        entity_manager.add_position(entity2, Position { x: 10.0, y: 10.0 });
        entity_manager.add_hitbox(entity2, Hitbox { 
            size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: false 
        });
        
        // Run collision detection
        collision_system.update(&entity_manager);
        
        // Should detect no collisions because entity2 hitbox is inactive
        let events = collision_system.get_events();
        assert_eq!(events.len(), 0);
    }

    #[test]
    fn test_negative_coordinates() {
        let collision_system = CollisionSystem::new();
        
        // Test collision detection with negative coordinates
        let pos1 = Position { x: -10.0, y: -10.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: -5.0, y: -5.0 };
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_zero_size_hitbox() {
        let collision_system = CollisionSystem::new();
        
        // Zero-size hitbox at same position should collide with regular hitbox
        let pos1 = Position { x: 5.0, y: 5.0 }; // Inside the second hitbox
        let hitbox1 = Hitbox { size: [0.0, 0.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 0.0, y: 0.0 };
        let hitbox2 = Hitbox { size: [10.0, 10.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
        
        // Zero-size hitbox outside should not collide
        let pos3 = Position { x: 20.0, y: 20.0 }; // Outside the second hitbox
        let hitbox3 = Hitbox { size: [0.0, 0.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos3, &hitbox3, &pos2, &hitbox2), false);
    }
}

fn main() {
    env_logger::init();
    
    println!("Viklings wgpu Triangle Prototype - ARCH-002");
    println!("Initializing wgpu rendering system...");

    let event_loop = EventLoop::new().unwrap();
    let mut app = App { 
        window: None, 
        state: None,
        start_time: Instant::now(),
        screenshot_taken: false,
    };
    
    event_loop.run_app(&mut app).unwrap();
}