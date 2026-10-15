package com.example.cameraxapp.core.render

import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Pure Kotlin helpers shared by the renderer and its JVM unit tests.
 * The numeric constants here are the single source of truth for the GLSL uniforms
 * built in GlRenderer; the shader itself only contains the non-tunable maths.
 */

/** LUT texture is 512x512: 8x8 tiles of 64x64 texels, one tile per blue slice. */
const val LUT_SIZE = 512
const val LUT_LEVELS = 64
const val LUT_TILES_PER_ROW = 8
const val LUT_TILE_SIZE = 64

/** Rec.709 luma weights (sum to 1). */
const val LUMA_R = 0.2126f
const val LUMA_G = 0.7152f
const val LUMA_B = 0.0722f

/** Amplitude of the highlight/shadow luma offset. Monotone as long as 3 * amplitude < 1. */
const val TONE_AMPLITUDE = 0.25f

/** Grain noise peak, applied to 0..1 channel values. */
private const val GRAIN_AMPLITUDE = 0.06f

/** Fade lifts blacks by up to this fraction. */
private const val FADE_AMOUNT = 0.1f

/** Vignette darkens the corners by up to this fraction. */
private const val VIGNETTE_AMOUNT = 0.6f

/** White balance gain per unit of temperature / tint. */
private const val TEMPERATURE_GAIN = 0.1f
private const val TINT_GAIN_RB = 0.05f
private const val TINT_GAIN_G = 0.1f

/** Contrast slope per unit of contrast, around 0.5 mid grey. */
private const val CONTRAST_SLOPE = 0.5f

/**
 * Texture coordinates of the texel centre for an integer LUT input (r, g, b), each in 0..63.
 * Tile column = b % 8, tile row = b / 8, texel = (col * 64 + r, row * 64 + g), centre at +0.5.
 */
fun lutUv(r: Int, g: Int, b: Int): Pair<Float, Float> {
    require(r in 0 until LUT_LEVELS && g in 0 until LUT_LEVELS && b in 0 until LUT_LEVELS) {
        "LUT input out of range: ($r, $g, $b)"
    }
    val col = b % LUT_TILES_PER_ROW
    val row = b / LUT_TILES_PER_ROW
    val u = (col * LUT_TILE_SIZE + r + 0.5f) / LUT_SIZE
    val v = (row * LUT_TILE_SIZE + g + 0.5f) / LUT_SIZE
    return u to v
}

/** Linear gain for an exposure value in -1..1, mapping to -2..+2 EV: 2^(2 * exposure). */
fun exposureGain(exposure: Float): Float = 2f.pow(2f * exposure)

/** Per-channel RGB gains for temperature (-1..1, warm = positive) and tint (-1..1, magenta = positive). */
fun whiteBalanceGains(temperature: Float, tint: Float): Triple<Float, Float, Float> {
    val red = 1f + TEMPERATURE_GAIN * temperature + TINT_GAIN_RB * tint
    val green = 1f - TINT_GAIN_G * tint
    val blue = 1f - TEMPERATURE_GAIN * temperature + TINT_GAIN_RB * tint
    return Triple(red, green, blue)
}

/** Multiplier around mid grey for contrast in -1..1: 1 + 0.5 * contrast. */
fun contrastFactor(contrast: Float): Float = 1f + CONTRAST_SLOPE * contrast

/** Mix factor against Rec.709 luma for saturation in -1..1: 1 + saturation (0 = grayscale). */
fun saturationFactor(saturation: Float): Float = 1f + saturation

/** Black-lift amount for fade in 0..1. */
fun fadeAmount(fade: Float): Float = FADE_AMOUNT * fade

/** Peak grain noise for grain in 0..1. */
fun grainAmplitude(grain: Float): Float = GRAIN_AMPLITUDE * grain

/** Corner darkening for vignette in 0..1. */
fun vignetteStrength(vignette: Float): Float = VIGNETTE_AMOUNT * vignette

/** Rec.709 luma of an RGB triple. */
fun luma(r: Float, g: Float, b: Float): Float = LUMA_R * r + LUMA_G * g + LUMA_B * b

/**
 * Size after scaling the longest side down to [maxSize], preserving aspect ratio.
 * Images already within [maxSize] are returned unchanged; they are never upscaled.
 */
fun scaledSize(width: Int, height: Int, maxSize: Int?): Pair<Int, Int> {
    require(width > 0 && height > 0) { "Invalid size ${width}x$height" }
    if (maxSize == null) return width to height
    require(maxSize > 0) { "maxSize must be positive, was $maxSize" }
    val longest = maxOf(width, height)
    if (longest <= maxSize) return width to height
    val scale = maxSize.toDouble() / longest
    val w = (width * scale).roundToInt().coerceAtLeast(1)
    val h = (height * scale).roundToInt().coerceAtLeast(1)
    return w to h
}
