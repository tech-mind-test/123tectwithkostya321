#version 120

uniform vec2 u_size;
uniform float u_radius;
uniform float u_round_type;
uniform float u_border_size;

uniform float u_alpha1, u_alpha2, u_alpha3, u_alpha4;
uniform vec4 u_color_1, u_color_2, u_color_3, u_color_4;

float signedDistanceField(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

float superellipseDistanceField(vec2 p, vec2 b, float r) {
    r = min(r * 1.85, min(b.x, b.y));
    vec2 q = abs(p) - b + r;
    vec2 outside = max(q, 0.0);
    float superLength = pow(pow(outside.x, 2.35) + pow(outside.y, 2.35), 1.0 / 2.35);

    return min(max(q.x, q.y), 0.0) + superLength - r;
}

void main(void) {
    vec2 tex = gl_TexCoord[0].st;

    vec4 c1 = vec4(u_color_1.rgb, u_alpha1);
    vec4 c2 = vec4(u_color_2.rgb, u_alpha2);
    vec4 c3 = vec4(u_color_3.rgb, u_alpha3);
    vec4 c4 = vec4(u_color_4.rgb, u_alpha4);

    vec4 finalColor = mix(
        mix(c1, c2, tex.y),
        mix(c3, c4, tex.y),
        tex.x
    );

    vec2 halfSize = u_size * 0.5;
    vec2 position = halfSize - tex * u_size;
    float regularSdf = signedDistanceField(position, halfSize - 1.0, u_radius);
    float iosSdf = superellipseDistanceField(halfSize - tex * u_size, halfSize - 1.0, u_radius);
    float sdf = mix(regularSdf, iosSdf, step(0.5, u_round_type));
    float borderAlpha = 1.0 - smoothstep(0.0, 0.32, abs(sdf) - u_border_size);

    gl_FragColor = vec4(finalColor.rgb, finalColor.a * borderAlpha);
}
