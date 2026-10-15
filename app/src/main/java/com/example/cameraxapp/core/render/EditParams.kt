package com.example.cameraxapp.core.render

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Crop aspect ratios. [ratio] is width / height; null means the full image. */
@Serializable
enum class CropAspect(val ratio: Float?) {
    FREE(null),
    SQUARE(1f),
    R4_5(4f / 5f),
    R3_4(3f / 4f),
    R9_16(9f / 16f),
}

/**
 * Geometric edit, applied after the GPU pass: rotation, flips and an aspect-ratio crop.
 * Every field has a default so recipes saved before crop existed still decode.
 */
@Serializable
data class CropParams(
    val aspect: CropAspect = CropAspect.FREE,
    val quarterTurns: Int = 0,         // rotation in 90 degree steps, clockwise
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
) {
    /** Same crop with [quarterTurns] wrapped into 0..3. */
    fun normalized(): CropParams = copy(quarterTurns = ((quarterTurns % 4) + 4) % 4)

    val isIdentity: Boolean
        get() =
            aspect == CropAspect.FREE &&
                normalized().quarterTurns == 0 &&
                !flipHorizontal &&
                !flipVertical
}

/**
 * Serializable description of a photo edit: one LUT filter plus global adjustments.
 * Stored as JSON so old recipes keep parsing when fields are added later.
 */
@Serializable
data class EditParams(
    val filterId: String = FilterCatalog.ORIGINAL_ID,
    val filterIntensity: Float = 1f, // 0..1, blend between original and LUT result
    val exposure: Float = 0f,        // -1..1, maps to -2..+2 EV in the renderer
    val contrast: Float = 0f,        // -1..1
    val saturation: Float = 0f,      // -1..1 (-1 = grayscale)
    val temperature: Float = 0f,     // -1..1 (negative = cooler/blue, positive = warmer/orange)
    val tint: Float = 0f,            // -1..1 (negative = green, positive = magenta)
    val highlight: Float = 0f,       // -1..1
    val shadow: Float = 0f,          // -1..1
    val fade: Float = 0f,            // 0..1, lifts blacks
    val sharpness: Float = 0f,       // 0..1
    val grain: Float = 0f,           // 0..1
    val vignette: Float = 0f,        // 0..1
    val chromatic: Float = 0f,       // 0..1, RGB channel split (CPU pass, not in the GPU shader)
    val crop: CropParams = CropParams(),
) {
    /** Returns a copy with every field clamped to its documented range. */
    fun clamped(): EditParams =
        copy(
            filterIntensity = filterIntensity.coerceIn(0f, 1f),
            exposure = exposure.coerceIn(-1f, 1f),
            contrast = contrast.coerceIn(-1f, 1f),
            saturation = saturation.coerceIn(-1f, 1f),
            temperature = temperature.coerceIn(-1f, 1f),
            tint = tint.coerceIn(-1f, 1f),
            highlight = highlight.coerceIn(-1f, 1f),
            shadow = shadow.coerceIn(-1f, 1f),
            fade = fade.coerceIn(0f, 1f),
            sharpness = sharpness.coerceIn(0f, 1f),
            grain = grain.coerceIn(0f, 1f),
            vignette = vignette.coerceIn(0f, 1f),
            chromatic = chromatic.coerceIn(0f, 1f),
            crop = crop.normalized(),
        )

    /** True when rendering these params would leave the photo unchanged. */
    val isIdentity: Boolean
        get() =
            (filterId == FilterCatalog.ORIGINAL_ID || filterIntensity == 0f) &&
                exposure == 0f &&
                contrast == 0f &&
                saturation == 0f &&
                temperature == 0f &&
                tint == 0f &&
                highlight == 0f &&
                shadow == 0f &&
                fade == 0f &&
                sharpness == 0f &&
                grain == 0f &&
                vignette == 0f &&
                chromatic == 0f &&
                crop.isIdentity

    fun toJson(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        fun fromJson(json: String): EditParams = JSON.decodeFromString(serializer(), json)
    }
}
