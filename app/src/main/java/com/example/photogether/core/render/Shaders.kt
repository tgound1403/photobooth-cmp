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
uniform highp sampler3D uLutA;
uniform highp sampler3D uLutB;
uniform bool uUseLutA;
uniform bool uUseLutB;
uniform float uLutMix;
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
uniform float uGrainSeed;
uniform float uGrainScale;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

// Crossfade between two LUTs. A side whose flag is false is the identity (no filter).
vec3 applyLut(vec3 c) {
    vec3 p = c * (63.0 / 64.0) + 0.5 / 64.0;
    vec3 a = uUseLutA ? clamp(texture(uLutA, p).rgb, 0.0, 1.0) : c;
    vec3 b = uUseLutB ? clamp(texture(uLutB, p).rgb, 0.0, 1.0) : c;
    return mix(a, b, uLutMix);
}

// Deterministic hash in [0, 1]. The same (pixel coordinate, seed) always gives the same value.
float hash01(uvec2 p, uint seed) {
    uint h = p.x * 1664525u + p.y * 1013904223u + seed * 2654435761u;
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
    if (uUseLutA || uUseLutB) {
        c = mix(c, applyLut(c), uFilterIntensity);
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

    // 8. Grain: noise from (pixel coordinate / grain scale, per-frame seed), strongest in the mid tones.
    //    uGrainScale keeps the grain size constant across output resolutions.
    float noise = hash01(uvec2(gl_FragCoord.xy / uGrainScale), uint(uGrainSeed)) * 2.0 - 1.0;
    float grainL = dot(c, LUMA);
    float grainWeight = clamp(4.0 * grainL * (1.0 - grainL), 0.0, 1.0);
    c = clamp(c + noise * uGrainAmplitude * mix(0.35, 1.0, grainWeight), 0.0, 1.0);

    // 9. Vignette: smooth radial falloff; r = 1 at the corners.
    float r = length(vUv - 0.5) * 1.41421356;
    c *= 1.0 - uVignetteStrength * smoothstep(0.4, 1.0, r);

    fragColor = vec4(clamp(c, 0.0, 1.0), texel.a);
}
"""

/**
 * Present pass: unsharp mask with a 3x3 binomial blur (sharpness 0 = plain copy), then the optional
 * halation screen composite of uBloom, the light leak (screen) and the dust overlay (mix).
 * Texture units: uSrc 0, uBloom 1, uLeakTex 3, uDustTex 4.
 *   out = c + sharpness * (c - blur3x3(c)),  blur weights [1 2 1; 2 4 2; 1 2 1] / 16
 */
const val SHARPEN_FRAGMENT_SHADER = """#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uSrc;
uniform vec2 uTexel;
uniform float uSharpness;
uniform sampler2D uBloom;
uniform bool uUseBloom;
uniform sampler2D uLeakTex;
uniform sampler2D uDustTex;
uniform float uLeak;        // already scaled by FxMath.leakScale
uniform float uDust;        // already scaled by FxMath.dustScale
uniform vec2 uLeakOffset;
uniform int uLeakFlip;
uniform vec2 uDustOffset;
uniform int uUseLeak;
uniform int uUseDust;

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
    vec3 sharp = clamp(c.rgb + uSharpness * (c.rgb - blur), 0.0, 1.0);
    if (uUseBloom) {
        // Halation composite (screen): 1 - (1 - c) * (1 - bloom). The bloom is already tinted and scaled.
        vec3 bloom = clamp(texture(uBloom, vUv).rgb, 0.0, 1.0);
        sharp = 1.0 - (1.0 - sharp) * (1.0 - bloom);
    }
    if (uUseLeak != 0) {
        // Light leak (screen): colour from the texture, strength from its alpha.
        vec2 luv = uLeakFlip != 0 ? vec2(1.0 - vUv.x, vUv.y) : vUv;
        luv = fract(luv + uLeakOffset * 0.25);
        vec4 leak = texture(uLeakTex, luv);
        sharp = 1.0 - (1.0 - sharp) * (1.0 - leak.rgb * (leak.a * uLeak));
    }
    if (uUseDust != 0) {
        // Dust and scratches: blend towards the speck colour by alpha.
        vec2 duv = fract(vUv + uDustOffset * 0.25);
        vec4 dust = texture(uDustTex, duv);
        sharp = mix(sharp, dust.rgb, dust.a * uDust);
    }
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

/**
 * Bloom pass B1: downsample the adjusted frame to 1/4 size and keep only highlights.
 * Four bilinear taps one source texel off-centre (a 4x4 footprint) avoid shimmer from a single tap.
 * Weight = smoothstep(KNEE_START, KNEE_END, luma), the same curve as BloomMath.thresholdWeight.
 */
val BLOOM_THRESHOLD_FRAGMENT_SHADER: String = """#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uSrc;
uniform vec2 uTexel; // 1 / source size

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

void main() {
    vec3 c = 0.25 * (
        texture(uSrc, vUv + uTexel * vec2(-1.0, -1.0)).rgb
        + texture(uSrc, vUv + uTexel * vec2(1.0, -1.0)).rgb
        + texture(uSrc, vUv + uTexel * vec2(-1.0, 1.0)).rgb
        + texture(uSrc, vUv + uTexel * vec2(1.0, 1.0)).rgb);
    float l = dot(c, LUMA);
    float w = smoothstep(${BloomMath.KNEE_START.glsl()}, ${BloomMath.KNEE_END.glsl()}, l);
    fragColor = vec4(c * w, 1.0);
}
"""

/**
 * Bloom pass B2/B3: one direction of the 9-tap separable Gaussian (weights from BloomMath).
 * uDir is the per-tap step in uv (1/w, 0) or (0, 1/h). uGain is (1,1,1) for the horizontal pass and
 * tint * bloomScale(halation) for the vertical pass, so the bloom texture is ready to composite.
 */
val BLOOM_BLUR_FRAGMENT_SHADER: String = """#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uSrc;
uniform vec2 uDir;
uniform vec3 uGain;

const float WEIGHTS[${BloomMath.TAPS}] = float[${BloomMath.TAPS}](${BloomMath.gaussianWeights().joinToString(", ") { it.glsl() }});

void main() {
    vec3 sum = vec3(0.0);
    for (int i = 0; i < ${BloomMath.TAPS}; i++) {
        sum += WEIGHTS[i] * texture(uSrc, vUv + uDir * float(i - ${BloomMath.TAPS / 2})).rgb;
    }
    fragColor = vec4(clamp(sum * uGain, 0.0, 1.0), 1.0);
}
"""

private fun Float.glsl(): String = "%.8f".format(java.util.Locale.US, this)
