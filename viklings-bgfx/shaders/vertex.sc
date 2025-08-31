// bgfx vertex shader
// This is a placeholder - real implementation would require bgfx shader compilation tools

$input a_position, a_color0
$output v_color0

#include <bgfx_shader.sh>

void main()
{
    gl_Position = mul(u_modelViewProj, vec4(a_position, 1.0));
    v_color0 = a_color0;
}