#version 120

uniform sampler2D originalTexture;
uniform sampler2D blurredTexture;
uniform vec4 multiplier;
uniform vec2 viewOffset;
uniform vec2 resolution;
uniform float mixFactor;

void main() {
    vec2 texCoord = gl_TexCoord[0].xy;
    vec4 srcColor = texture2D(originalTexture, texCoord);
    vec2 blurredUV = (gl_FragCoord.xy + viewOffset) / resolution;
    blurredUV.y = 1.0 - blurredUV.y;
    vec4 tint = multiplier;
    if (tint.a <= 0.0) {
        tint.a = 1.0;
    }
    if (tint.r + tint.g + tint.b <= 0.0) {
        tint.rgb = vec3(1.0);
    }

    vec4 blurTinted = texture2D(blurredTexture, blurredUV) * tint;
    blurTinted.a = srcColor.a;
    float factor = mixFactor <= 0.0 ? 1.0 : clamp(mixFactor, 0.0, 1.0);
    gl_FragColor = mix(srcColor, blurTinted, factor);
}
