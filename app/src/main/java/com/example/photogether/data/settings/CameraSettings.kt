package com.example.photogether.data.settings

/**
 * Camera adjustments the user last chose.
 * kelvin = null means Auto white balance.
 * ev is the exposure compensation in stops.
 */
data class CameraSettings(
    val kelvin: Int? = null,
    val ev: Float = 0f,
)
