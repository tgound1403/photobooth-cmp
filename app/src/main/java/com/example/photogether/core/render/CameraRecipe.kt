package com.example.photogether.core.render

/** Date stamp printed on a photo by the present pass (see DateStampMath and the stamp helper in Shaders.kt). */
enum class DateStampStyle { NONE, ORANGE_LCD, WHITE_DIGITAL }

/** Crop aspect of a camera body. Drives the live-preview mask and the centre crop of a captured photo. */
enum class CameraAspect(val label: String) {
    FIVE_FOUR("5:4"), FOUR_THREE("4:3"), THREE_TWO("3:2"), ONE_ONE("1:1"), SIXTEEN_NINE("16:9");

    /** Portrait width / height. FOUR_THREE is 3/4, the full sensor frame, so it needs no crop. */
    val widthOverHeight: Float
        get() = when (this) {
            FIVE_FOUR -> 4f / 5f
            FOUR_THREE -> 3f / 4f
            ONE_ONE -> 1f
            THREE_TWO -> 2f / 3f
            SIXTEEN_NINE -> 9f / 16f
        }

    /** True when the camera trims the 3:4 frame. */
    val crops: Boolean get() = this != FOUR_THREE
}

/**
 * Skin painted on a photo. It is no longer drawn on the camera preview; the editor offers it as an overlay
 * ([OverlaySpec.skin]), drawn by [SkinRenderer]. Cameras no longer choose one.
 * Colours are ARGB ints so this file stays free of Compose types.
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

/** Shutter sound a capture plays. [asset] is the path under assets/. */
enum class ShutterSound(val asset: String) {
    MECH("sounds/shutter_mech.wav"),
    SOFT("sounds/shutter_soft.wav"),
    DISPOSABLE("sounds/shutter_disposable.wav"),
}

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

