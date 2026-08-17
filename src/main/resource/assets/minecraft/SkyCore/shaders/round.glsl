#version 120

uniform vec2 size;
uniform vec4 color;

void main() {
    vec2 p = gl_TexCoord[0].st * size - size * 0.5;
    float r = min(size.x, size.y) * 0.5;
    float d = length(p) - r;
    float alpha = (1.0 - smoothstep(0.0, 1.0, d)) * color.a;
    gl_FragColor = vec4(color.rgb, alpha);
}
