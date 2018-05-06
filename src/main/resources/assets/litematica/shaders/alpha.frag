#version 120

uniform sampler2D texture;
uniform float alpha_multiplier;

void main() {
    vec4 tex = texture2D(texture, gl_TexCoord[0].xy) * gl_Color;
    gl_FragColor = vec4(tex.r, tex.g, tex.b, tex.a * alpha_multiplier);
}