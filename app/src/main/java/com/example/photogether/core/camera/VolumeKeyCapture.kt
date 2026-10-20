package com.example.photogether.core.camera

/**
 * Lets the camera screen take a photo with the volume keys while it is showing the landscape body.
 * [MainActivity] forwards volume key presses here; the screen sets [handler] while it is active.
 */
object VolumeKeyCapture {
    /** Runs on each volume key press. Null when no screen is listening, so the keys keep their normal job. */
    @Volatile
    var handler: (() -> Unit)? = null
}
