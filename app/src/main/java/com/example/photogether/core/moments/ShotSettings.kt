package com.example.photogether.core.moments

import kotlin.math.roundToInt

/**
 * Settings a camera shot was taken with. Saved in the photo's EXIF when it is taken and read back for the
 * shot detail dialog. A null field was not recorded (for example on shots taken before this was added),
 * and the dialog hides it.
 */
data class ShotSettings(
    /** Name of the LUT (filter) applied to the shot. */
    val lutName: String?,
    /** Exposure compensation in EV. */
    val ev: Float?,
    /** Colour temperature in Kelvin. Null when white balance was automatic. */
    val kelvin: Int?,
    /** 35mm-equivalent focal length in millimetres. */
    val focalMm: Int?,
    /** Strength of the filter, 0..1. Null for shots taken before it was saved. */
    val filterIntensity: Float? = null,
    /** White balance tint, -1 (green) to +1 (magenta). Null when it was 0, so the dialog hides it. */
    val tint: Float? = null,
)

/**
 * EXIF stores exposure bias as a fraction, such as "30/100". The denominator 100 keeps two decimal places.
 */
fun evAsRational(ev: Float): String = "${(ev * 100f).roundToInt()}/100"

/** Kelvin, filter strength and tint share EXIF's user comment as `kelvin=5500;intensity=72;tint=-40`. */
data class UserCommentValues(val kelvin: Int?, val intensityPercent: Int?, val tintPercent: Int? = null)

/**
 * Encodes kelvin, strength (0..100 percent) and tint (-100..100 percent) into the user comment. Null fields are
 * left out. Returns null when all are null, so the tag is not written.
 */
fun encodeUserComment(kelvin: Int?, intensityPercent: Int?, tintPercent: Int? = null): String? {
    val parts = buildList {
        kelvin?.let { add("kelvin=$it") }
        intensityPercent?.let { add("intensity=$it") }
        tintPercent?.let { add("tint=$it") }
    }
    return parts.joinToString(";").ifEmpty { null }
}

/**
 * Reads the user comment. Shots saved before strength existed hold only the kelvin digits, which still read
 * as kelvin. Blank or unknown text gives nulls.
 */
fun decodeUserComment(text: String?): UserCommentValues {
    val trimmed = text?.trim().orEmpty()
    if (trimmed.isEmpty()) return UserCommentValues(null, null)
    if (!trimmed.contains('=')) return UserCommentValues(trimmed.toIntOrNull(), null)
    val fields = trimmed.split(';').associate { part ->
        val key = part.substringBefore('=').trim()
        key to part.substringAfter('=', "").trim()
    }
    return UserCommentValues(
        kelvin = fields["kelvin"]?.toIntOrNull(),
        intensityPercent = fields["intensity"]?.toIntOrNull()?.coerceIn(0, 100),
        tintPercent = fields["tint"]?.toIntOrNull()?.coerceIn(-100, 100),
    )
}
