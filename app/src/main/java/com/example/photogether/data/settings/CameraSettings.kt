package com.example.photogether.data.settings

/**
 * Camera adjustments the user last chose.
 * kelvin = null means Auto white balance.
 * ev is the exposure compensation in stops.
 * cameraId is the camera body picked last (CameraProfile.id), null before any pick.
 */
data class CameraSettings(
    val kelvin: Int? = null,
    val ev: Float = 0f,
    val cameraId: String? = null,
)
