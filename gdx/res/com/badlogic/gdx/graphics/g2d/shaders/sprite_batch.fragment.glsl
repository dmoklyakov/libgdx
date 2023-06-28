#ifdef GL_ES
#define LOWP lowp
precision highp float;
#else
#define LOWP
#endif

#ifdef GLSL3
#define varying in
out vec4 out_FragColor;
#define textureCube texture
#define texture2D texture
#else
#define out_FragColor gl_FragColor
#endif

#define textureCube texture
#define texture2D texture

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
varying float v_msdfParamsIndex;
varying float v_uiParamsIndex;
#ifdef TEXTURE_ARRAY
    varying float v_textureIndex;
    uniform sampler2D u_textures[MAX_TEXTURE_UNITS];
    uniform vec2 u_textureSizes[MAX_TEXTURE_UNITS];
    #define TEXTURE u_textures[int(v_textureIndex)]
    #define TEXTURE_SIZE u_textureSizes[int(v_textureIndex)]
#else
    uniform sampler2D u_texture;
    uniform vec2 u_textureSize;
    #define TEXTURE u_texture
    #define TEXTURE_SIZE u_textureSize
#endif

// Begin MSDF stuff.
struct MsdfParams {
    vec4 color;
    vec4 shadowColor;
    vec4 innerShadowColor;
    vec4 data1; // shadowOffset (x, y), distanceFactor, fontWeight
    vec4 data2; // shadowClipped, shadowSmoothing, innerShadowRange, shadowIntensity
};
uniform MsdfParams u_msdfParams[MAX_MSDF_PARAMS];

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

vec4 blend(vec4 src, vec4 dst, float alpha) {
    // src OVER dst porter-duff blending
    float a = src.a + dst.a * (1.0 - src.a);
    vec3 rgb = (src.a * src.rgb + dst.a * dst.rgb * (1.0 - src.a)) / (a == 0.0 ? 1.0 : a);
    return vec4(rgb, a * alpha);
}

float linearstep(float a, float b, float x) {
    return clamp((x - a) / (b - a), 0.0, 1.0);
}

const vec4 ZERO_VECTOR = vec4(0.0, 0.0, 0.0, 0.0);
// End MSDF stuff.

// Begin UI stuff.
float roundedBoxSDF(vec2 center, vec2 size, vec4 radius) {
    radius.xy = (center.x > 0.0) ? radius.xy : radius.zw;
    radius.x = (center.y > 0.0) ? radius.x : radius.y;
    vec2 q = abs(center) - size + radius.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius.x;
}

struct UiParams {
    vec4 region;  // px: [x, y, width, height]
    vec4 cornerRadii; // px: [br, tr, bl, tl].
    vec4 borderColor;
    vec4 data; // contentScale, borderOutSoftness (px), borderThickness (px), borderInSoftness (px).
};
uniform UiParams u_uiParams[MAX_UI_PARAMS];

vec4 blendBorder(vec4 texColor, vec4 borderColor) {
    vec4 result;
    // Calculate the blend factor for the RGB channels
    float blendFactor = min(texColor.a, 1.0 - borderColor.a);
    // Blend the RGB channels without multiplication by alpha
    result.rgb = texColor.rgb * blendFactor + borderColor.rgb * (1.0 - blendFactor);
    // Blend the alpha channel
    result.a = borderColor.a + texColor.a * (1.0 - borderColor.a);
    return result;
}

// End UI stuff.

void main()
{
    int msdfParamsIndex = int(v_msdfParamsIndex);
    int uiParamsIndex = int(v_uiParamsIndex);
    vec4 outColor = vec4(0.0, 0.0, 0.0, 0.0);
    if (msdfParamsIndex >= 0) {
        MsdfParams params = u_msdfParams[msdfParamsIndex];
        // Glyph
        vec4 msdf = texture(TEXTURE, v_texCoords);
        float distance = params.data1.z * (median(msdf.r, msdf.g, msdf.b) + params.data1.w - 0.5);
        float glyphAlpha = clamp(distance + 0.5, 0.0, 1.0);
        vec4 glyph = vec4(params.color.rgb, glyphAlpha * params.color.a);

        // Shadow
        distance = texture(TEXTURE, v_texCoords - params.data1.xy / TEXTURE_SIZE).a + params.data1.w;
        float shadowAlpha = linearstep(0.5 - params.data2.y, 0.5 + params.data2.y, distance) * params.shadowColor.a * params.data2.w;
        shadowAlpha *= 1.0 - glyphAlpha * params.data2.x;
        vec4 shadow = vec4(params.shadowColor.rgb, shadowAlpha);

        // Inner shadow
        distance = msdf.a + params.data1.w;
        float innerShadowAlpha = linearstep(0.5 + params.data2.z, 0.5, distance) * params.innerShadowColor.a * glyphAlpha;
        vec4 innerShadow = vec4(params.innerShadowColor.rgb, innerShadowAlpha);

        outColor = blend(blend(innerShadow, glyph, 1.0), shadow, v_color.a);
    } else if (uiParamsIndex >= 0) {
        UiParams params = u_uiParams[uiParamsIndex];

        vec2 center = params.region.zw / 2.0;
        vec2 boxPosition = v_texCoords * TEXTURE_SIZE - params.region.xy;
        vec2 texPos = ((v_texCoords * TEXTURE_SIZE - center - params.region.xy) / params.data.x + params.region.xy + center) / TEXTURE_SIZE;
        vec4 textureColor = texture(TEXTURE, texPos);
        vec4 inCornerRadii = max(params.cornerRadii - params.data.z - params.data.w - params.data.y, ZERO_VECTOR);
        vec4 borderCornerRadii = max(params.cornerRadii - params.data.y - params.data.z, ZERO_VECTOR);
        vec4 outCornerRadii = max(params.cornerRadii - params.data.y, ZERO_VECTOR);
        float inDistance = roundedBoxSDF(boxPosition - center, center - params.data.z - params.data.w - params.data.y, inCornerRadii);
        float borderDistance = roundedBoxSDF(boxPosition - center, center - params.data.y - params.data.z, borderCornerRadii);
        float outDistance = roundedBoxSDF(boxPosition - center, center - params.data.y, outCornerRadii);
        float inBorder = smoothstep(0.0, params.data.w, inDistance);
        float outBorder = 1.0 - smoothstep(0.0, params.data.y, outDistance);
        float actualBorderAlpha = inBorder * outBorder;

        params.borderColor.a = actualBorderAlpha;
        textureColor.a = 1.0 - borderDistance;
        textureColor = clamp(textureColor, 0.0, 1.0);
        outColor = blendBorder(textureColor, params.borderColor);
    } else {
        outColor = texture(TEXTURE, v_texCoords);
    }
    out_FragColor = v_color * clamp(outColor, 0.0, 1.0);
}