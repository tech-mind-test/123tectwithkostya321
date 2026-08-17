#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform vec3 color;
uniform vec3 color2;
uniform float time;

uniform float speed;
uniform float scale;
uniform float outline;
uniform float glow;
uniform float fill;
uniform float alpha;

#define TAU 6.28318530718
#define PI 3.14159265359
#define MAX_ITER 5

void main() {
    vec2 uvMain = gl_TexCoord[0].st;

    // Фикс моргания: не даём шейдеру лезть за валидные UV
    if (uvMain.x < 0.0 || uvMain.x > 1.0 || uvMain.y < 0.0 || uvMain.y > 1.0) {
        discard;
    }

    // Маска неба
    float m = texture2D(texture, uvMain).a;
    if (m <= 0.001) {
        discard;
    }

    float theta = uvMain.x * TAU;
    float phi = uvMain.y * PI;

    vec3 dir = vec3(
        sin(phi) * cos(theta),
        cos(phi),
        sin(phi) * sin(theta)
    );

    float t = time * speed + 23.0;

    vec3 p = dir * (2.0 + scale * 1.5);
    vec3 i = p;
    float c = 1.0;
    float inten = 0.35 / max(scale, 0.001);

    for (int n = 0; n < MAX_ITER; n++) {
        float fi = float(n + 1);
        float tIter = t * (1.0 - (3.5 / fi));

        i = p + vec3(
            cos(tIter - i.x) + sin(tIter + i.y),
            sin(tIter - i.y) + cos(tIter + i.z),
            sin(tIter - i.z) + cos(tIter + i.x)
        );

        vec3 denom = vec3(
            sin(i.x + tIter),
            cos(i.y + tIter),
            sin(i.z + tIter)
        );

        denom = sign(denom) * max(abs(denom), vec3(0.001));
        c += 1.0 / length(p / (denom / inten));
    }

    c /= float(MAX_ITER);
    c = 1.17 - pow(abs(c), 1.4);

    float pattern = clamp(pow(abs(c), 6.0), 0.0, 1.0);
    vec3 finalRGB = mix(color, color2, pattern);

    float contour = smoothstep(0.45 - outline * 0.25, 0.90 + outline * 0.25, pattern);
    float edgeGlow = pow(contour, 0.8) * glow * 0.08;

    vec3 outputColor = finalRGB * (fill * m + edgeGlow);
    float outA = clamp(alpha * (0.18 + pattern * 0.55 + edgeGlow * 0.35), 0.0, 1.0);

    gl_FragColor = vec4(outputColor, outA * m);
}