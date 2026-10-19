package com.example.photogether.service

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.photogether.core.moments.ShotSettings
import com.example.photogether.core.moments.decodeUserComment
import com.example.photogether.core.moments.encodeUserComment
import com.example.photogether.core.moments.evAsRational
import java.io.File
import kotlin.math.roundToInt

/**
 * Writes a shot's settings into its JPEG's EXIF and reads them back. Each field uses a standard tag:
 * the LUT name is the image description, EV is exposure bias, focal length is the 35mm equivalent. Kelvin
 * and filter strength have no standard tag, so they share the user comment as `kelvin=…;intensity=…`.
 */
object ShotSettingsStore {

    fun write(file: File, settings: ShotSettings) {
        val exif = ExifInterface(file)
        settings.lutName?.let { exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, it) }
        settings.ev?.let { exif.setAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, evAsRational(it)) }
        encodeUserComment(settings.kelvin, settings.filterIntensity?.let { (it * 100f).roundToInt() })
            ?.let { exif.setAttribute(ExifInterface.TAG_USER_COMMENT, it) }
        settings.focalMm?.let { exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, it.toString()) }
        exif.saveAttributes()
    }

    /**
     * Null when the file has none of the settings, such as a shot taken before they were saved, or when
     * the uri cannot be opened. Reads through the content resolver, so it works for MediaStore uris.
     */
    fun read(context: Context, uri: String): ShotSettings? {
        val exif = runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { ExifInterface(it) }
        }.getOrNull() ?: return null
        val comment = decodeUserComment(exif.getAttribute(ExifInterface.TAG_USER_COMMENT))
        val settings = ShotSettings(
            lutName = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.takeIf { it.isNotBlank() },
            ev = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, Double.NaN)
                .takeUnless { it.isNaN() }?.toFloat(),
            kelvin = comment.kelvin,
            focalMm = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0).takeIf { it > 0 },
            filterIntensity = comment.intensityPercent?.let { it / 100f },
        )
        return settings.takeIf {
            it.lutName != null || it.ev != null || it.kelvin != null || it.focalMm != null || it.filterIntensity != null
        }
    }
}
