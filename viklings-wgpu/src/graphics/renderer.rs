use std::sync::Arc;
use wgpu::util::DeviceExt;
use winit::window::Window;
use log::{debug, info, warn};
use crate::ecs::{EntityManager, EntityId, Position, Render};
use super::vertex::{Vertex, VERTICES};
use super::instance_data::InstanceData;

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

pub struct GraphicsEngine {
    pub surface: wgpu::Surface<'static>,
    pub device: wgpu::Device,
    pub queue: wgpu::Queue,
    pub config: wgpu::SurfaceConfiguration,
    pub size: winit::dpi::PhysicalSize<u32>,
    pub render_pipeline: wgpu::RenderPipeline,
    pub vertex_buffer: wgpu::Buffer,
    pub instance_buffer: wgpu::Buffer,
    pub uniform_buffer: wgpu::Buffer,
    pub uniform_bind_group: wgpu::BindGroup,
}

impl GraphicsEngine {
    pub async fn new(window: Arc<Window>) -> Self {
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
            source: wgpu::ShaderSource::Wgsl(include_str!("../shader.wgsl").into()),
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
        info!("Creating instance buffer: {} bytes for {} max instances", instance_buffer_size, MAX_INSTANCES);
        
        let instance_buffer = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Instance Buffer - DEV006"),
            size: instance_buffer_size as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

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

    // Generate instance data from entities for instanced rendering
    fn generate_instance_data(&self, entity_manager: &EntityManager) -> Vec<InstanceData> {
        let mut instance_data = Vec::new();
        
        let renderable_entities = entity_manager.get_entities_with_render();
        
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
        
        #[cfg(debug_assertions)]
        {
            debug!("=== Instance Data Generation ===");
            debug!("Generated {} instances for rendering", instance_data.len());
            if !instance_data.is_empty() {
                debug!("Instance data size: {} bytes", instance_data.len() * std::mem::size_of::<InstanceData>());
                debug!("Buffer capacity: {} instances", 100); // MAX_INSTANCES from buffer creation
            }
        }
        
        instance_data
    }

    pub fn render(&mut self, entity_manager: &EntityManager) -> Result<(), wgpu::SurfaceError> {
        // Multi-entity rendering: get all renderable entities
        let renderable_entities: Vec<(EntityId, Position, Render)> = entity_manager.get_entities_with_render()
            .into_iter()
            .map(|(id, pos, render)| (id, pos.clone(), render.clone()))
            .collect();
        
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
                let mut instance_data = self.generate_instance_data(entity_manager);
                
                if !instance_data.is_empty() {
                    // Validate instance count doesn't exceed buffer capacity
                    const MAX_INSTANCES: usize = 100;
                    if instance_data.len() > MAX_INSTANCES {
                        warn!("Too many instances ({}) for buffer capacity ({}). Truncating.", 
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
                    debug!("Instanced draw: {} entities with single draw call", instance_data.len());
                }
            }
        }

        self.queue.submit(std::iter::once(encoder.finish()));
        output.present();

        Ok(())
    }
}