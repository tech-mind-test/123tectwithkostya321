#version 120

varying vec3 vWorldDir;

void main() {
    vWorldDir = normalize(gl_Color.rgb * 2.0 - 1.0);
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
