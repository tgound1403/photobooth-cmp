package com.example.photogether.core.moments

/** What made the image. Read from the MediaStore display name. */
enum class MomentKind { CAMERA, PHOTOBOOTH, EDITED }

/** One image created by the app: camera shot, edited photo or photobooth result. */
data class Moment(
    val uri: String,
    val takenAt: Long,
    val width: Int,
    val height: Int,
    /** Id of the camera that took the photo. Null for edits, photobooth results and shots taken before tagging. */
    val cameraId: String? = null,
    val kind: MomentKind = MomentKind.CAMERA,
)
