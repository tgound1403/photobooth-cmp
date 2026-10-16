package com.example.photogether.core.camera

/** Manual shutter and sensitivity. Setting this turns auto-exposure off. */
data class ManualExposure(
    val exposureNs: Long,
    val iso: Int,
)

/**
 * Camera2 request settings the user has chosen explicitly. Pure data, no Android types,
 * so the mapping to CaptureRequest keys can be unit-tested on the JVM.
 */
data class Camera2Overrides(
    val awbLocked: Boolean = false,
    val manual: ManualExposure? = null,
)

/** The subset of CaptureRequest keys these overrides can touch. */
enum class Camera2Setting { AE_MODE_OFF, EXPOSURE_TIME, SENSITIVITY, AWB_LOCK }

/**
 * Expands the overrides into the CaptureRequest key/value pairs to apply.
 * Keys that are not set are absent from the map, so the caller must rebuild the whole
 * option set each time rather than merging into the previous one.
 * AWB lock is dropped when the device does not support it.
 */
fun Camera2Overrides.toSettings(awbLockAvailable: Boolean): Map<Camera2Setting, Any> {
    val settings = linkedMapOf<Camera2Setting, Any>()
    manual?.let {
        settings[Camera2Setting.AE_MODE_OFF] = true
        settings[Camera2Setting.EXPOSURE_TIME] = it.exposureNs
        settings[Camera2Setting.SENSITIVITY] = it.iso
    }
    if (awbLocked && awbLockAvailable) {
        settings[Camera2Setting.AWB_LOCK] = true
    }
    return settings
}
