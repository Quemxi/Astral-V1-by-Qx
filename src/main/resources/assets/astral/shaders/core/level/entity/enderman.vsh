#version 330
//? if >= 26.3
//#extension GL_ARB_separate_shader_objects : require

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

//$ layout '0' 'in' >> vec
in vec3 Position;
//$ layout '1' 'in' >> vec
in vec4 Color;
//$ layout '2' 'in' >> vec
in vec2 UV0;
//$ layout '3' 'in' >> ivec
in ivec2 UV1;
//$ layout '4' 'in' >> ivec
in ivec2 UV2;
//$ layout '5' 'in' >> vec
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

//$ layout '0' 'out' >> float
out float sphericalVertexDistance;
//$ layout '1' 'out' >> float
out float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
//$ layout '2' 'out' >> vec
out vec4 vertexPerFaceColorBack;
//$ layout '3' 'out' >> vec
out vec4 vertexPerFaceColorFront;
#else
//$ layout '2' 'out' >> vec
out vec4 vertexColor;
#endif
//$ layout '4' 'out' >> vec
out vec4 lightMapColor;
//$ layout '5' 'out' >> vec
out vec4 overlayColor;
//$ layout '6' 'out' >> vec
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);

    #ifdef PER_FACE_LIGHTING
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, Normal);
    vertexPerFaceColorBack = minecraft_mix_light_separate(-light, Color);
    vertexPerFaceColorFront = minecraft_mix_light_separate(light, Color);
    #elif defined(NO_CARDINAL_LIGHTING)
    vertexColor = Color;
    #else
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    #endif

    #ifndef EMISSIVE
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    #else
    lightMapColor = texelFetch(Sampler2, ivec2(0), 0) * 0.0;
    #endif

    overlayColor = texelFetch(Sampler1, UV1, 0);

    #ifdef APPLY_TEXTURE_MATRIX
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    #else
    texCoord0 = UV0;
    #endif
}
