package com.example.photogether.core.render

/*
 * GLSL ES 3.00 sources used by GlRenderer.
 * Tunable magnitudes are passed as uniforms computed in LutMath.kt, so the shader only holds maths.
 */

/** Like [VERTEX_SHADER], but uv is transformed by a matrix (SurfaceTexture or SurfaceOutput transform). */
const val VERTEX_SHADER_TEXMATRIX = """#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUv;
uniform mat4 uTexMatrix;
out vec2 vUv;
void main() {
    vUv = (uTexMatrix * vec4(aUv, 0.0, 1.0)).xy;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

/** Full-screen quad. Position is in clip space; uv (0,0) is the first row of the bitmap. */
const val VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUv;
out vec2 vUv;
void main() {
    vUv = aUv;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

/**
 * Pass 1, in this order:
 * pre-gain (Kelvin white balance and software EV), LUT blend, exposure, white balance (tint and
 * temperature), contrast, saturation, highlights/shadows, fade, grain, vignette.
 * The pre-gain runs before the LUT so the filter sees the corrected colour. uWbGain stays after the
 * LUT so recipes saved before Kelvin keep their look. Both gains are 1 when unused.
 *
 * LUT lookup: a 64x64x64 GL_TEXTURE_3D (built from the 512x512 tiled PNG, see GlUtil.uploadLut3D),
 * sampled with hardware trilinear filtering. Grid node n sits at texel centre (n + 0.5) / 64, so
 * the input is mapped with c * 63/64 + 0.5/64; an exact grid input (c = n / 63) returns node n.
 */
const val ADJUST_FRAGMENT_SHADER = """#version 300 es
precision highp float;
precision highp int;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uSrc;
uniform highp sampler3D uLut;
uniform bool uUseLut;
uniform vec3 uPreGain;
uniform float uFilterIntensity;
uniform float uExposureGain;
uniform vec3 uWbGain;
uniform float uContrastFactor;
uniform float uSaturationFactor;
uniform float uToneAmount;
uniform float uHighlight;
uniform float uShadow;
uniform float uFadeAmount;
uniform float uGrainAmplitude;
uniform float uVignetteStrength;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

vec3 applyLut(vec3 c) {
    return texture(uLut, c * (63.0 / 64.0) + 0.5 / 64.0).rgb;
}

// Deterministic per-pixel hash in [0, 1]. Same pixel coordinate always gives the same value.
float hash01(uvec2 p) {
    uint h = p.x * 1664525u + p.y * 1013904223u;
    h ^= h >> 16u;
    h *= 0x7feb352du;
    h ^= h >> 15u;
    h *= 0x846ca68bu;
    h ^= h >> 16u;
    return float(h) / 4294967295.0;
}

void main() {
    vec4 texel = texture(uSrc, vUv);
    vec3 c = clamp(texel.rgb, 0.0, 1.0);

    // 0. Pre-gain: Kelvin white balance and software EV, before the LUT. (1,1,1) when unused.
    c = clamp(c * uPreGain, 0.0, 1.0);

    // 1. Filter: blend the original with the LUT result by intensity.
    if (uUseLut) {
        c = mix(c, clamp(applyLut(c), 0.0, 1.0), uFilterIntensity);
    }

    // 2. Exposure: linear gain on the (sRGB-encoded) values.
    c = clamp(c * uExposureGain, 0.0, 1.0);

    // 3. White balance: small per-channel gains.
    c = clamp(c * uWbGain, 0.0, 1.0);

    // 4. Contrast around mid grey.
    c = clamp((c - 0.5) * uContrastFactor + 0.5, 0.0, 1.0);

    // 5. Saturation: mix towards Rec.709 luma.
    float luma = dot(c, LUMA);
    c = clamp(mix(vec3(luma), c, uSaturationFactor), 0.0, 1.0);

    // 6. Highlights and shadows: add a luma offset weighted by luma, same offset to each channel.
    //    The offset slope is at least 1 - 3 * uToneAmount > 0, so output luma stays monotone.
    float l = dot(c, LUMA);
    float wHighlight = smoothstep(0.5, 1.0, l);
    float wShadow = 1.0 - smoothstep(0.0, 0.5, l);
    c = clamp(c + uToneAmount * (uHighlight * wHighlight + uShadow * wShadow), 0.0, 1.0);

    // 7. Fade: lift blacks.
    c = c * (1.0 - uFadeAmount) + uFadeAmount;

    // 8. Grain: deterministic noise from the output pixel coordinate.
    float noise = hash01(uvec2(gl_FragCoord.xy)) * 2.0 - 1.0;
    c = clamp(c + noise * uGrainAmplitude, 0.0, 1.0);

    // 9. Vignette: smooth radial falloff; r = 1 at the corners.
    float r = length(vUv - 0.5) * 1.41421356;
    c *= 1.0 - uVignetteStrength * smoothstep(0.4, 1.0, r);

    fragColor = vec4(clamp(c, 0.0, 1.0), texel.a);
}
"""

/**
 * Pass 2 (only when sharpness > 0): unsharp mask with a 3x3 binomial blur.
 *   out = c + sharpness * (c - blur3x3(c)),  blur weights [1 2 1; 2 4 2; 1 2 1] / 16
 */
const val SHARPEN_FRAGMENT_SHADER = """#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uSrc;
uniform vec2 uTexel;
uniform float uSharpness;

void main() {
    vec2 t = uTexel;
    vec4 c = texture(uSrc, vUv);
    vec3 blur =
        (texture(uSrc, vUv + t * vec2(-1.0, -1.0)).rgb
        + 2.0 * texture(uSrc, vUv + t * vec2(0.0, -1.0)).rgb
        + texture(uSrc, vUv + t * vec2(1.0, -1.0)).rgb
        + 2.0 * texture(uSrc, vUv + t * vec2(-1.0, 0.0)).rgb
        + 4.0 * c.rgb
        + 2.0 * texture(uSrc, vUv + t * vec2(1.0, 0.0)).rgb
        + texture(uSrc, vUv + t * vec2(-1.0, 1.0)).rgb
        + 2.0 * texture(uSrc, vUv + t * vec2(0.0, 1.0)).rgb
        + texture(uSrc, vUv + t * vec2(1.0, 1.0)).rgb) / 16.0;
    vec3 sharp = c.rgb + uSharpness * (c.rgb - blur);
    fragColor = vec4(clamp(sharp, 0.0, 1.0), c.a);
}
"""

/**
 * Camera frames arrive as an external OES texture (SurfaceTexture). This variant of the adjust
 * shader samples that texture instead of a sampler2D. The rest of the shader is unchanged.
 */
val ADJUST_FRAGMENT_SHADER_OES: String = oesVariant(ADJUST_FRAGMENT_SHADER)

private fun oesVariant(source: String): String {
    val header = "#version 300 es\n"
    val declaration = "uniform sampler2D uSrc;"
    require(source.startsWith(header)) { "Shader must start with the GLSL ES 3.00 header" }
    require(source.contains(declaration)) { "Shader does not declare $declaration" }
    return header +
        "#extension GL_OES_EGL_image_external_essl3 : require\n" +
        source.removePrefix(header).replace(declaration, "uniform samplerExternalOES uSrc;")
}
