#version 120

uniform vec2 size;
uniform vec4 radius;
uniform vec4 color;
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

void main() {
    vec2 rectHalf = size * 0.5;
    float smoothedAlpha = (1.0 - smoothstep(0.0, 1.0, interfaceDistanceField(rectHalf - (gl_TexCoord[0].st * size), rectHalf - 1.0, radius))) * color.a;
    gl_FragColor = vec4(color.rgb, smoothedAlpha);
}
