package com.example.cameraxapp.core.moments

/** One image created by the app: camera shot, edited photo or photobooth result. */
data class Moment(
    val uri: String,
    val takenAt: Long,
    val width: Int,
    val height: Int,
)
