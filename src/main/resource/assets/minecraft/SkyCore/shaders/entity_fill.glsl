#version 120

uniform sampler2D texture;
uniform vec3 colorA;
uniform vec3 colorB;
uniform float time;
uniform float alpha;

void main() {
    vec2 uv = gl_TexCoord[0].xy;
    float mixFactor = (uv.x + uv.y) * 0.5 + sin(uv.x * 8.0 + time) * cos(uv.y * 8.0 + time) * 0.2;
    mixFactor = clamp(mixFactor, 0.0, 1.0);
    
    vec3 finalColor = mix(colorA, colorB, mixFactor);
    float texAlpha = texture2D(texture, uv).a;
    
    gl_FragColor = vec4(finalColor, texAlpha * alpha);
}