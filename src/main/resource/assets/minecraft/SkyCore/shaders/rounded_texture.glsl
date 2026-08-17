#version 120

uniform sampler2D texture;
uniform vec2 size;
uniform float radius;
uniform float alpha;

float signedDistanceField(vec2 p, vec2 b, float r) {
    return length(max(abs(p) - b, 0.0)) - r;
}

void main() {
    vec2 tex = gl_TexCoord[0].st;
    vec2 pixel = tex * size;
    vec2 centre = 0.5 * size;

    float roundMask = 1.0 - smoothstep(0.0, 1.0, signedDistanceField(centre - pixel, centre - radius - 1.0, radius));

    vec4 col = texture2D(texture, tex);
    col.a *= (roundMask * alpha);
    gl_FragColor = col;
}

