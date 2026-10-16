package com.example.cameraxapp.core.camera

import com.example.cameraxapp.core.render.EditParams

/** Which quick-adjust panel is open on the camera screen, if any. */
enum class CameraTool { WHITE_BALANCE, EXPOSURE }

/** Where EV compensation is applied: the camera hardware, or the shader as a fallback. */
enum class EvMode { HARDWARE, SOFTWARE }

/** What the opened camera supports. [evRange] is null when hardware EV compensation is unavailable. */
data class CameraCapabilities(
    val evRange: EvRange?,
    val awbLockAvailable: Boolean,
)

/** Live camera-adjust state held by the view model. */
data class CameraAdjustState(
    val kelvin: Int? = null,
    val ev: Float = 0f,
    val evRange: EvRange = EvRange.SOFTWARE,
    val evMode: EvMode = EvMode.SOFTWARE,
    val awbLockAvailable: Boolean = false,
    val manualExposureActive: Boolean = false,
    val openTool: CameraTool? = null,
)

/** Sets the colour temperature; null returns to auto white balance. Kelvin is clamped to the supported range. */
fun CameraAdjustState.withKelvin(k: Int?): CameraAdjustState =
    copy(kelvin = k?.coerceIn(EditParams.KELVIN_MIN, EditParams.KELVIN_MAX))

/** Sets the EV, snapped to the nearest step of [CameraAdjustState.evRange]. */
fun CameraAdjustState.withEv(ev: Float): CameraAdjustState =
    copy(ev = evRange.evFor(evRange.indexFor(ev)))

/**
 * Applies what the camera reports. Without hardware EV, falls back to software EV over
 * [EvRange.SOFTWARE]. The current EV is re-snapped to the new range.
 */
fun CameraAdjustState.withCapabilities(c: CameraCapabilities): CameraAdjustState {
    val range = c.evRange ?: EvRange.SOFTWARE
    return copy(
        evRange = range,
        evMode = if (c.evRange != null) EvMode.HARDWARE else EvMode.SOFTWARE,
        awbLockAvailable = c.awbLockAvailable,
        ev = range.evFor(range.indexFor(ev)),
    )
}

/**
 * Camera2 overrides for this state. AWB is locked only while a Kelvin value is set,
 * because auto white balance would otherwise fight the manual shader correction.
 */
fun CameraAdjustState.overrides(manual: ManualExposure?): Camera2Overrides =
    Camera2Overrides(
        awbLocked = kelvin != null && awbLockAvailable,
        manual = manual,
    )

/**
 * Writes the camera-adjust values into the edit recipe. Software EV goes into the shader
 * only when [EvMode.SOFTWARE] is active; hardware EV is applied by the camera instead.
 */
fun EditParams.withCameraAdjust(s: CameraAdjustState): EditParams =
    copy(
        kelvin = s.kelvin,
        softwareEv = if (s.evMode == EvMode.SOFTWARE) s.ev else 0f,
    )
