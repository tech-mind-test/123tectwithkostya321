#version 120
uniform sampler2D textureIn;
uniform vec2 texelSize, direction;
uniform vec4 color;
uniform float radius, weights[32];
void main() {
    vec2 uv = gl_TexCoord[0].xy;
    float a = texture2D(textureIn, uv).a * weights[0];
    for (float i = 1.0; i <= radius; i++) {
        a += texture2D(textureIn, uv + (direction * texelSize * i)).a * weights[int(i)];
        a += texture2D(textureIn, uv - (direction * texelSize * i)).a * weights[int(i)];
    }
    gl_FragColor = vec4(color.rgb, color.a * a);
}