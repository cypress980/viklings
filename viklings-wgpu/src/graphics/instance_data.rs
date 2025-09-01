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
    pub fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<InstanceData>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Instance, // Key difference!
            attributes: &Self::ATTRIBS,
        }
    }
    
    // Debug helper to print instance data
    #[cfg(debug_assertions)]
    pub fn debug_print(&self, index: usize) {
        use log::trace;
        trace!("Instance {}: pos={:?}, scale={:?}, color={:?}", 
               index, self.position, self.scale, self.color);
    }
}