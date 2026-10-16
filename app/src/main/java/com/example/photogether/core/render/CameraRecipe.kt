package com.example.photogether.core.render

/** Date stamp printed on a photo. Stored only; rendering is not implemented yet. */
enum class DateStampStyle { NONE, ORANGE_LCD, WHITE_DIGITAL }

/** Crop aspect of a camera body. Stored only; rendering is not implemented yet. */
enum class CameraAspect { FOUR_THREE, ONE_ONE, THREE_TWO, SIXTEEN_NINE }

/**
 * Film effects a camera body applies when it is chosen. The float fields mirror the same-named
 * [EditParams] fields and are applied on selection. [dateStamp] and [aspect] are only stored.
 */
data class CameraRecipe(
    val leak: Float = 0f,        // 0..1, light leak overlay
    val dust: Float = 0f,        // 0..1, dust and scratch overlay
    val halation: Float = 0f,    // 0..1, warm glow around highlights
    val grain: Float = 0f,       // 0..1, same meaning as EditParams.grain
    val vignette: Float = 0f,    // 0..1, same meaning as EditParams.vignette
    val dateStamp: DateStampStyle = DateStampStyle.NONE,
    val aspect: CameraAspect = CameraAspect.FOUR_THREE,
) {
    /** Same recipe with every float clamped to 0..1. */
    fun clamped(): CameraRecipe =
        copy(
            leak = leak.coerceIn(0f, 1f),
            dust = dust.coerceIn(0f, 1f),
            halation = halation.coerceIn(0f, 1f),
            grain = grain.coerceIn(0f, 1f),
            vignette = vignette.coerceIn(0f, 1f),
        )
}
