#version 120

uniform vec2 size;
uniform vec3 color0, color1, color2, color3;
uniform vec4 radius;
uniform float alpha, glowRadius;
uniform float roundType;

float signedDistanceField(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;

    vec2 q = abs(p) - b + r.x;

    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float superellipseDistanceField(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    float maxRadius = min(b.x, b.y);
    r *= 1.85;
    r = min(r, vec4(maxRadius));

    vec2 q = abs(p) - b + r.x;
    vec2 outside = max(q, 0.0);
    float superLength = pow(pow(outside.x, 3.0) + pow(outside.y, 3.0), 1.0 / 3.0);

    return min(max(q.x, q.y), 0.0) + superLength - r.x;
}

float interfaceDistanceField(vec2 p, vec2 b, vec4 r) {
    return mix(signedDistanceField(p, b, r), superellipseDistanceField(p, b, r), step(0.5, roundType));
}

vec3 createGradient(vec2 pos) {
    return mix(mix(color0.rgb, color1.rgb, pos.y), mix(color2.rgb, color3.rgb, pos.y), pos.x);
}

void main() {
    vec2 halfSize = size * 0.5;
    float sdf = interfaceDistanceField(halfSize - gl_TexCoord[0].st * size, halfSize - glowRadius, radius);
    float a = (1.0 - smoothstep(-glowRadius, glowRadius, sdf)) * alpha;
    gl_FragColor = mix(vec4(createGradient(gl_TexCoord[0].st), 0.0), vec4(createGradient(gl_TexCoord[0].st), a), a);
}
