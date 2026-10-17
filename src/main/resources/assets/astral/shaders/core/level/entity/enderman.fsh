#version 330
//? if >= 26.3
//#extension GL_ARB_separate_shader_objects : require

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

//$ layout '0' 'in' >> float
in float sphericalVertexDistance;
//$ layout '1' 'in' >> float
in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
//$ layout '2' 'in' >> vec
in vec4 vertexPerFaceColorBack;
//$ layout '3' 'in' >> vec
in vec4 vertexPerFaceColorFront;
#else
//$ layout '2' 'in' >> vec
in vec4 vertexColor;
#endif
//$ layout '4' 'in' >> vec
in vec4 lightMapColor;
//$ layout '5' 'in' >> vec
in vec4 overlayColor;
//$ layout '6' 'in' >> vec
in vec2 texCoord0;

//$ layout '0' 'out' >> vec
out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);

    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) discard;
    #endif

    #ifdef PER_FACE_LIGHTING
    vec4 vColor = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
    #else
    vec4 vColor = vertexColor;
    #endif

    color.rgb = mix(color.rgb, vec3(1.0), vColor.a);
    color.rgb *= vColor.rgb;
    color *= ColorModulator;

    #ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    #endif

    #ifndef EMISSIVE
    color *= lightMapColor;
    #else
    color += lightMapColor * 0.000001;
    #endif

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
