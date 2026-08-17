#version 120

varying vec3 vWorldDir;

void main() {
    gl_TexCoord[0] = gl_MultiTexCoord0;
    vWorldDir = gl_Vertex.xyz;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
