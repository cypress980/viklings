// Vertex shader
struct Uniforms {
    offset: vec2<f32>,  // Keep for compatibility, may be used for global transforms
}

@group(0) @binding(0)
var<uniform> uniforms: Uniforms;

struct VertexInput {
    @location(0) position: vec3<f32>,
    @location(1) color: vec3<f32>,  // Base vertex color (will be overridden by instance)
}

// Instance data - per entity
struct InstanceInput {
    @location(2) instance_pos: vec2<f32>,    // Entity world position  
    @location(3) instance_scale: vec2<f32>,  // Entity size/scale
    @location(4) instance_color: vec4<f32>,  // Entity RGBA color
}

struct VertexOutput {
    @builtin(position) clip_position: vec4<f32>,
    @location(0) color: vec4<f32>,  // Changed to vec4 to support alpha
}

@vertex
fn vs_main(vertex: VertexInput, instance: InstanceInput) -> VertexOutput {
    var out: VertexOutput;
    
    // Use instance color instead of vertex color  
    out.color = instance.instance_color;
    
    // Apply instance transformations
    var position = vertex.position.xy;
    
    // Scale the vertex position by instance scale
    position = position * instance.instance_scale;
    
    // Translate to instance world position
    position = position + instance.instance_pos;
    
    // Apply global uniform offset (for camera/viewport transforms)
    position = position + uniforms.offset;
    
    out.clip_position = vec4<f32>(position, 0.0, 1.0);
    return out;
}

// Fragment shader
@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
    return in.color;  // Already vec4 with alpha from instance data
}