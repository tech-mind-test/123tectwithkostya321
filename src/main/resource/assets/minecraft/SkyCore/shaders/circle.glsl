#version 120

uniform vec2 size;
uniform vec4 color;
uniform float radius;
uniform float startAngle;
uniform float endAngle;
uniform float thickness;
uniform float smoothness;

void main() {
    // Центрируем систему координат
    vec2 st = (gl_TexCoord[0].st - 0.5) * size;
    float dist = length(st);

    // Сглаживание по радиусу (анти-алиасинг)
    float outer = radius;
    float inner = radius - thickness;
    
    float mask = smoothstep(outer, outer - smoothness, dist) - 
                 smoothstep(inner, inner - smoothness, dist);

    // Вычисление угла для прогресса (TargetHUD)
    float angle = degrees(atan(st.y, st.x));
    if (angle < 0.0) angle += 360.0;

    // Проверка попадания в сектор
    float s = mod(startAngle, 360.0);
    float e = mod(endAngle, 360.0);
    float angleMask = 1.0;

    if (s != e) {
        if (s < e) {
            if (angle < s || angle > e) angleMask = 0.0;
        } else {
            if (angle < s && angle > e) angleMask = 0.0;
        }
    }

    gl_FragColor = vec4(color.rgb, color.a * mask * angleMask);
}