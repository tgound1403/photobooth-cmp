package com.example.photogether.core.moments

/** One photo taken on the camera screen, as the camera gallery lists it. */
data class CameraShot(
    /** File path. Used to show the photo and to open it in the editor. */
    val path: String,
    /** MediaStore content uri. Used to delete the photo. */
    val uri: String,
    /** Date the photo was added to MediaStore, in millis. */
    val takenAt: Long,
    val width: Int,
    val height: Int,
    /** Camera body that took the photo. Null for shots saved before the id was added to the file name. */
    val cameraId: String?,
)
