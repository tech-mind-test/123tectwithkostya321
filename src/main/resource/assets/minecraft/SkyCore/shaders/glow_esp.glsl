#version 120

uniform sampler2D textureIn;
uniform vec2 texelSize, direction;
uniform vec4 color;
uniform bool avoidTexture;
uniform float exposure, radius;
uniform float weights[32];

#define offset direction * texelSize

void main() {
    if (direction.y == 1 && avoidTexture) {
        if (texture2D(textureIn, gl_TexCoord[0].st).a != 0.0) discard;
    }

    float innerAlpha = texture2D(textureIn, gl_TexCoord[0].st).a * weights[0];

    for (float r = 1.0; r <= radius; r ++) {
        innerAlpha += texture2D(textureIn, gl_TexCoord[0].st + offset * r).a * weights[int(r)];
        innerAlpha += texture2D(textureIn, gl_TexCoord[0].st - offset * r).a * weights[int(r)];
    }

    gl_FragColor = vec4(color.rgb, color.a * innerAlpha);
}