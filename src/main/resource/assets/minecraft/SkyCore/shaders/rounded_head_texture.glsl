#version 120

uniform sampler2D texture;
uniform vec2 size;
uniform float radius;
uniform float roundType;
uniform float hurt_time;
uniform float alpha;

uniform float texXSize;
uniform float texYSize;

float signedDistanceField(vec2 p, vec2 b, float r) {
    return length(max(abs(p) - b, 0.0)) - r;
}

float superellipseDistanceField(vec2 p, vec2 b, float r) {
    r = min(r * 1.85, min(b.x, b.y));
    vec2 q = abs(p) - b + r;
    vec2 outside = max(q, 0.0);
    float superLength = pow(pow(outside.x, 3.0) + pow(outside.y, 3.0), 1.0 / 3.0);

    return min(max(q.x, q.y), 0.0) + superLength - r;
}

float interfaceDistanceField(vec2 p, vec2 centre, float r) {
    float regular = signedDistanceField(p, centre - r - 1.0, r);
    float ios = superellipseDistanceField(p, centre - 1.0, r);
    return mix(regular, ios, step(0.5, roundType));
}

void main() {
    vec2 tex = gl_TexCoord[0].st;

    vec2 pixel = tex * size;
    vec2 centre = 0.5 * size;
    float roundMask = 1.0 - smoothstep(0.0, 1.0, interfaceDistanceField(centre - pixel, centre, radius));

    vec2 baseSkinCoord = vec2(
    mix(8.0 / texXSize, 16.0 / texXSize, tex.x),
    mix(8.0 / texYSize, 16.0 / texYSize, tex.y)
    );
    vec4 baseSkin = texture2D(texture, baseSkinCoord);

    vec2 overlayCoord = vec2(
    mix(40.0 / texXSize, 48.0 / texXSize, tex.x),
    mix(8.0 / texYSize, 16.0 / texYSize, tex.y)
    );
    vec4 overlay = texture2D(texture, overlayCoord);

    vec3 finalColor = mix(baseSkin.rgb, overlay.rgb, overlay.a);
    float finalAlpha = max(baseSkin.a, overlay.a);

    finalColor = mix(finalColor, vec3(1.0, 0.0, 0.0), hurt_time);
    finalAlpha = finalAlpha * roundMask * alpha;


    gl_FragColor = vec4(finalColor, finalAlpha);
}
