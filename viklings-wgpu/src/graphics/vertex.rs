#[repr(C)]
#[derive(Copy, Clone, Debug, bytemuck::Pod, bytemuck::Zeroable)]
pub struct Vertex {
    pub position: [f32; 3],
    pub color: [f32; 3],
}

impl Vertex {
    pub fn desc() -> wgpu::VertexBufferLayout<'static> {
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

pub const VERTICES: &[Vertex] = &[
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