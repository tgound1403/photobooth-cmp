package com.example.photogether.core.render

/** Date stamp printed on a photo by the present pass (see DateStampMath and the stamp helper in Shaders.kt). */
enum class DateStampStyle { NONE, ORANGE_LCD, WHITE_DIGITAL }

/** Crop aspect of a camera body. Drives the live-preview mask and the centre crop of a captured photo. */
enum class CameraAspect {
    FOUR_THREE, ONE_ONE, THREE_TWO, SIXTEEN_NINE;

    /** Portrait width / height. FOUR_THREE is 3/4, the full sensor frame, so it needs no crop. */
    val widthOverHeight: Float
        get() = when (this) {
            FOUR_THREE -> 3f / 4f
            ONE_ONE -> 1f
            THREE_TWO -> 2f / 3f
            SIXTEEN_NINE -> 9f / 16f
        }

    /** True when the camera trims the 3:4 frame. */
    val crops: Boolean get() = this != FOUR_THREE
}

/**
 * Viewfinder skin of a camera body: a Compose overlay on the live preview. [RANGEFINDER_MONO] is the grey
 * variant of [RANGEFINDER], kept separate so every camera has its own shutter tint.
 * Colours are ARGB ints so this file stays free of Compose types; see ViewfinderSkin.kt for drawing.
 */
enum class ViewfinderSkin(val shutterTint: Long, val shutterRing: Long) {
    NONE(0L, 0L),
    FILM_FRAME(0xFFF2E8D5, 0L),
    RANGEFINDER(0xFFFFFFFF, 0L),
    RANGEFINDER_MONO(0xFFB8BCC4, 0L),
    LCD_DIGICAM(0xFFD9DCE0, 0xFFE53935),
    SOFT_GLASS(0xFFFFF4D6, 0L);
}

/** Flash mode the user picks on the camera screen. TORCH keeps the light on; the others fire on capture. */
enum class FlashMode {
    OFF, AUTO, ON, TORCH;

    /** The next mode in the button cycle OFF, AUTO, ON, TORCH. */
    fun next(): FlashMode = entries[(ordinal + 1) % entries.size]
}

/** Look of the screen flash shown at capture. NONE shows nothing. */
enum class FlashStyle { NONE, HARSH, WARM }

/** What to tell CameraX for a [FlashMode]: the ImageCapture flash constant and whether the torch is on. */
data class FlashPlan(val captureMode: Int, val torch: Boolean)

/**
 * Maps a [FlashMode] to CameraX values. [off], [auto], [on] are the ImageCapture.FLASH_MODE_* constants,
 * passed in so this stays free of Android types. TORCH sets the torch and keeps capture flash off.
 */
fun flashPlan(mode: FlashMode, off: Int, auto: Int, on: Int): FlashPlan = when (mode) {
    FlashMode.OFF -> FlashPlan(off, false)
    FlashMode.AUTO -> FlashPlan(auto, false)
    FlashMode.ON -> FlashPlan(on, false)
    FlashMode.TORCH -> FlashPlan(off, true)
}

/**
 * Film effects a camera body applies when it is chosen. The float fields mirror the same-named
 * [EditParams] fields and are applied on selection. [dateStamp] is copied into EditParams and drawn by the GPU; [aspect] masks the preview and crops the capture.
 */
data class CameraRecipe(
    val leak: Float = 0f,        // 0..1, light leak overlay
    val dust: Float = 0f,        // 0..1, dust and scratch overlay
    val halation: Float = 0f,    // 0..1, warm glow around highlights
    val grain: Float = 0f,       // 0..1, same meaning as EditParams.grain
    val vignette: Float = 0f,    // 0..1, same meaning as EditParams.vignette
    val dateStamp: DateStampStyle = DateStampStyle.NONE,
    val aspect: CameraAspect = CameraAspect.FOUR_THREE,
    val defaultFlash: FlashMode = FlashMode.OFF,
    val flashStyle: FlashStyle = FlashStyle.NONE,
    val skin: ViewfinderSkin = ViewfinderSkin.NONE,
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
