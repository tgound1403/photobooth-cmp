package com.example.photogether.data.settings

/**
 * Camera adjustments the user last chose.
 * kelvin = null means Auto white balance.
 * ev is the exposure compensation in stops.
 * tint is the green (-1) to magenta (+1) shift, 0 for none.
 * cameraId is the camera body picked last (CameraProfile.id), null before any pick.
 */
data class CameraSettings(
    val kelvin: Int? = null,
    val ev: Float = 0f,
    val cameraId: String? = null,
    val tint: Float = 0f,
)
