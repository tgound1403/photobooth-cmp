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
val SHARPEN_FRAGMENT_SHADER: String = """#version 300 es
precision highp float;
precision highp int;

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

// Editor effects (EffectMath), applied after everything above. All gated by an int flag.
uniform int uUseChromatic;
uniform float uChromaticPx;   // horizontal shift of red (-) and blue (+), in pixels
uniform int uUseLeakGrad;
uniform vec2 uLeakGradCorner; // corner of the image, each component 0 or 1
uniform float uLeakGradRadius; // times the longer side
uniform int uUseFxGrain;
uniform float uFxGrain;       // preset strength
uniform int uUseFxVignette;
uniform float uFxVignette;
uniform int uScratchCount;
uniform vec4 uScratchLines[49]; // x0, y0, x1, y1 in pixels

// Camera date stamp (DateStampMath), drawn last. 0 none, 1 orange LCD, 2 white digital.
uniform int uStampStyle;
uniform int uStampDigits[6];  // YY MM DD
uniform float uStampScale;    // output height / 1080, clamped
uniform vec2 uStampOut;       // size of the displayed (upright) image in pixels
uniform vec2 uStampImage;     // size of the rendered image in pixels
uniform int uStampRot;        // clockwise quarter turns that show the render upright
uniform int uStampTopDown;    // 1 when gl_FragCoord.y already counts from the top of the image

// Deterministic hash in [0, 1].
float hash01(uvec2 p, uint seed) {
    uint h = p.x * 1664525u + p.y * 1013904223u + seed * 2654435761u;
    h ^= h >> 16u;
    h *= 0x7feb352du;
    h ^= h >> 15u;
    h *= 0x846ca68bu;
    h ^= h >> 16u;
    return float(h) / 4294967295.0;
}

// Unsharp mask, bloom, light leak texture and dust at one uv. Returns the colour only.
vec3 shade(vec2 uv) {
    vec2 t = uTexel;
    vec3 c = texture(uSrc, uv).rgb;
    vec3 blur =
        (texture(uSrc, uv + t * vec2(-1.0, -1.0)).rgb
        + 2.0 * texture(uSrc, uv + t * vec2(0.0, -1.0)).rgb
        + texture(uSrc, uv + t * vec2(1.0, -1.0)).rgb
        + 2.0 * texture(uSrc, uv + t * vec2(-1.0, 0.0)).rgb
        + 4.0 * c
        + 2.0 * texture(uSrc, uv + t * vec2(1.0, 0.0)).rgb
        + texture(uSrc, uv + t * vec2(-1.0, 1.0)).rgb
        + 2.0 * texture(uSrc, uv + t * vec2(0.0, 1.0)).rgb
        + texture(uSrc, uv + t * vec2(1.0, 1.0)).rgb) / 16.0;
    vec3 sharp = clamp(c + uSharpness * (c - blur), 0.0, 1.0);
    if (uUseBloom) {
        // Halation composite (screen): 1 - (1 - c) * (1 - bloom). The bloom is already tinted and scaled.
        vec3 bloom = clamp(texture(uBloom, uv).rgb, 0.0, 1.0);
        sharp = 1.0 - (1.0 - sharp) * (1.0 - bloom);
    }
    if (uUseLeak != 0) {
        // Light leak (screen): colour from the texture, strength from its alpha.
        vec2 luv = uLeakFlip != 0 ? vec2(1.0 - uv.x, uv.y) : uv;
        luv = fract(luv + uLeakOffset * 0.25);
        vec4 leak = texture(uLeakTex, luv);
        sharp = 1.0 - (1.0 - sharp) * (1.0 - leak.rgb * (leak.a * uLeak));
    }
    if (uUseDust != 0) {
        // Dust and scratches: blend towards the speck colour by alpha.
        vec2 duv = fract(uv + uDustOffset * 0.25);
        vec4 dust = texture(uDustTex, duv);
        sharp = mix(sharp, dust.rgb, dust.a * uDust);
    }
    return sharp;
}

// Distance in pixels from p to the segment a-b.
float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 ab = b - a;
    float len2 = dot(ab, ab);
    float h = len2 > 0.0 ? clamp(dot(p - a, ab) / len2, 0.0, 1.0) : 0.0;
    return length(p - (a + ab * h));
}

// Seven-segment masks, bit 0 = a (top) .. bit 6 = g (middle). Same table as DateStampMath.GLYPH_MASKS.
const int STAMP_MASKS[10] = int[10](${DateStampMath.GLYPH_MASKS.joinToString(", ")});

// Distance in pixels from p to a box of half size h centred at c (negative inside).
float stampBox(vec2 p, vec2 c, vec2 h) {
    vec2 d = abs(p - c) - h;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}

// Draws YY MM DD in the bottom-right corner as 7-segment digits and blends it over col.
vec3 dateStamp(vec3 col) {
    float s = uStampScale;
    float dw = 20.0 * s;      // digit width
    float dh = 36.0 * s;      // digit height
    float t = 4.0 * s;        // segment thickness
    float gap = 6.0 * s;      // between digits of a pair
    float group = 18.0 * s;   // between YY, MM and DD
    // 9% of the short side: the live preview is centre-cropped by the view, so a small margin runs off screen.
    float margin = 0.09 * min(uStampOut.x, uStampOut.y);
    float total = 6.0 * dw + 5.0 * gap + 2.0 * (group - gap);
    float fy = uStampTopDown != 0 ? gl_FragCoord.y : uStampImage.y - gl_FragCoord.y;
    vec2 origin = vec2(uStampOut.x - margin - total, uStampOut.y - margin - dh);
    // Position in the rendered image, rows counted from the top, then turned into displayed coordinates.
    vec2 pix = vec2(gl_FragCoord.x, fy);
    if (uStampRot == 1) pix = vec2(uStampImage.y - pix.y, pix.x);
    else if (uStampRot == 2) pix = uStampImage - pix;
    else if (uStampRot == 3) pix = vec2(pix.y, uStampImage.x - pix.x);
    vec2 p = pix - origin;
    float reach = 10.0 * s;
    if (p.x < -reach || p.y < -reach || p.x > total + reach || p.y > dh + reach) return col;

    float dmin = 1e4;
    float hh = dh * 0.25 - t * 0.5;
    for (int i = 0; i < 6; i++) {
        int mask = STAMP_MASKS[clamp(uStampDigits[i], 0, 9)];
        vec2 q = p - vec2(float(i) * (dw + gap) + float(i / 2) * (group - gap), 0.0);
        if (q.x < -reach || q.x > dw + reach) continue;
        if ((mask & 1) != 0)  dmin = min(dmin, stampBox(q, vec2(dw * 0.5, t * 0.5), vec2(dw * 0.5 - t * 0.5, t * 0.5)));
        if ((mask & 2) != 0)  dmin = min(dmin, stampBox(q, vec2(dw - t * 0.5, dh * 0.25), vec2(t * 0.5, hh)));
        if ((mask & 4) != 0)  dmin = min(dmin, stampBox(q, vec2(dw - t * 0.5, dh * 0.75), vec2(t * 0.5, hh)));
        if ((mask & 8) != 0)  dmin = min(dmin, stampBox(q, vec2(dw * 0.5, dh - t * 0.5), vec2(dw * 0.5 - t * 0.5, t * 0.5)));
        if ((mask & 16) != 0) dmin = min(dmin, stampBox(q, vec2(t * 0.5, dh * 0.75), vec2(t * 0.5, hh)));
        if ((mask & 32) != 0) dmin = min(dmin, stampBox(q, vec2(t * 0.5, dh * 0.25), vec2(t * 0.5, hh)));
        if ((mask & 64) != 0) dmin = min(dmin, stampBox(q, vec2(dw * 0.5, dh * 0.5), vec2(dw * 0.5 - t * 0.5, t * 0.5)));
    }
    float cover = 1.0 - smoothstep(-0.5, 0.5, dmin);
    if (uStampStyle == 1) {
        vec3 orange = vec3(1.0, 0.55, 0.12);
        float glow = exp(-max(dmin, 0.0) / (3.0 * s)) * 0.45;
        col = mix(col, orange, glow);
        col = mix(col, orange, cover);
    } else {
        float outline = 1.0 - smoothstep(0.5, 1.5 * max(s, 1.0) + 0.5, dmin);
        col = mix(col, vec3(0.05), outline * 0.6);
        col = mix(col, vec3(0.95), cover);
    }
    return col;
}

void main() {
    float alpha = texture(uSrc, vUv).a;
    vec3 col;
    if (uUseChromatic != 0) {
        // Red looks right and blue looks left, so red moves left and blue moves right.
        vec2 o = vec2(uChromaticPx * uTexel.x, 0.0);
        col = vec3(shade(vUv + o).r, shade(vUv).g, shade(vUv - o).b);
    } else {
        col = shade(vUv);
    }
    vec2 size = 1.0 / uTexel;
    vec2 p = vUv * size;
    if (uUseLeakGrad != 0) {
        // Radial gradient from a corner, screen blended. Stops (straight colour, alpha):
        // t=0 (255,200,0) 150/255, t=0.5 (255,100,0) 100/255, t=1 transparent.
        float t = length(p - uLeakGradCorner * size) / (max(size.x, size.y) * uLeakGradRadius);
        vec3 lc;
        float la;
        if (t >= 1.0) {
            lc = vec3(0.0);
            la = 0.0;
        } else if (t < 0.5) {
            float u = t / 0.5;
            lc = vec3(1.0, mix(200.0, 100.0, u) / 255.0, 0.0);
            la = mix(150.0, 100.0, u) / 255.0;
        } else {
            float u = (t - 0.5) / 0.5;
            lc = vec3(mix(1.0, 0.0, u), mix(100.0, 0.0, u) / 255.0, 0.0);
            la = mix(100.0, 0.0, u) / 255.0;
        }
        col = 1.0 - (1.0 - col) * (1.0 - lc * la);
    }
    if (uUseFxGrain != 0) {
        float n = hash01(uvec2(gl_FragCoord.xy), 7u) * 2.0 - 1.0;
        col = clamp(col + n * uFxGrain * ${EffectMath.GRAIN_AMPLITUDE.glsl()}, 0.0, 1.0);
    }
    if (uUseFxVignette != 0) {
        // Black radial gradient from transparent at 1 - i to opaque at 1, scaled by i.
        float t = length(p - 0.5 * size) / (max(size.x, size.y) * 0.9);
        float a = clamp((t - (1.0 - uFxVignette)) / uFxVignette, 0.0, 1.0) * uFxVignette;
        col *= 1.0 - a;
    }
    for (int i = 0; i < 49; i++) {
        if (i >= uScratchCount) break;
        // White stroke 2 px wide at 150/255 opacity, one pixel of antialiasing.
        vec4 seg = uScratchLines[i];
        float cover = clamp(1.5 - segmentDistance(p, seg.xy, seg.zw), 0.0, 1.0);
        col = mix(col, vec3(1.0), cover * (150.0 / 255.0));
    }
    if (uStampStyle != 0) col = dateStamp(col);
    fragColor = vec4(clamp(col, 0.0, 1.0), alpha);
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
