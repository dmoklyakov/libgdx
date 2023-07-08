#ifdef GL_ES
    precision mediump float;
#endif

#ifdef GLSL3
    #define attribute in
    #define varying out
#endif

#ifdef TEXTURE_ARRAY
    attribute float a_textureIndex;
    varying float v_textureIndex;
#endif

attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute float a_msdfParamsIndex;
attribute float a_uiParamsIndex;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;
varying float v_msdfParamsIndex;
varying float v_uiParamsIndex;

void main()
{
    #ifdef TEXTURE_ARRAY
        v_textureIndex = a_textureIndex;
    #endif
    v_color = a_color;
    v_color.a = v_color.a * (255.0 / 254.0);
    v_texCoords = a_texCoord0;
    v_msdfParamsIndex = a_msdfParamsIndex;
    v_uiParamsIndex = a_uiParamsIndex;
    gl_Position = u_projTrans * a_position;
}
