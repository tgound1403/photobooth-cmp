package com.example.photogether.ui.editor

import android.content.ContentValues
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/** Image loading and saving for the editor. Faked in ViewModel tests. */
interface EditorImageIo {
    /** Decodes [uri] (content://, file:// or a plain path), sampled so the longest side is <= [maxSide]. */
    fun decode(uri: String, maxSide: Int): Bitmap?

    /** Writes [bitmap] as a new JPEG in the gallery. Returns the new content uri, or null on failure. */
    fun saveJpeg(bitmap: Bitmap): String?

    /** Longest side an export may render at, or null for the full source size. */
    val exportMaxSide: Int? get() = null
}

class AndroidEditorImageIo(private val context: Context) : EditorImageIo {

    /** Low-memory devices export at a capped size, so a full-size render does not run out of heap. */
    override val exportMaxSide: Int? by lazy {
        val memoryClassMb = context.getSystemService(ActivityManager::class.java)?.memoryClass ?: 0
        if (memoryClassMb in 1..LOW_MEMORY_CLASS_MB) LOW_MEMORY_EXPORT_SIDE else null
    }

    override fun decode(uri: String, maxSide: Int): Bitmap? =
        try {
            if (uri.startsWith("content://") || uri.startsWith("file://")) {
                decodeFromStream(Uri.parse(uri), maxSide)
            } else {
                val file = File(uri)
                if (file.exists() && file.canRead()) {
                    decodeFromFile(file.absolutePath, maxSide)
                } else {
                    decodeFromStream(Uri.fromFile(file), maxSide)
                }
            }
        } catch (e: Exception) {
            null
        }

    private fun decodeFromStream(uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = sampleOptions(bounds.outWidth, bounds.outHeight, maxSide)
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val orientation =
            context.contentResolver.openInputStream(uri)?.use { readOrientation(it) } ?: ExifInterface.ORIENTATION_NORMAL
        return bitmap?.let { upright(it, orientation) }
    }

    private fun decodeFromFile(path: String, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val bitmap = BitmapFactory.decodeFile(path, sampleOptions(bounds.outWidth, bounds.outHeight, maxSide))
        val orientation = File(path).inputStream().use { readOrientation(it) }
        return bitmap?.let { upright(it, orientation) }
    }

    private fun readOrientation(stream: java.io.InputStream): Int =
        runCatching {
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    /** BitmapFactory ignores the EXIF orientation, so a portrait shot would show sideways. Bakes it into the pixels. */
    private fun upright(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    /** Largest power-of-two sample size that keeps the longest side at or above [maxSide]. */
    private fun sampleOptions(width: Int, height: Int, maxSide: Int): BitmapFactory.Options {
        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.Options().apply { inSampleSize = sample }
    }

    override fun saveJpeg(bitmap: Bitmap): String? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Edited_$timeStamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        // Always a new file: the original is never overwritten.
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, EXPORT_JPEG_QUALITY, it) }
                ?: throw IOException("Failed to open output stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            }
            uri.toString()
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }
}

/** Devices at or below this heap class export at [LOW_MEMORY_EXPORT_SIDE] on the longest side. */
private const val LOW_MEMORY_CLASS_MB = 256
private const val LOW_MEMORY_EXPORT_SIDE = 3072

/** Visually lossless for photos; q100 costs file size and encode time without a visible gain. */
private const val EXPORT_JPEG_QUALITY = 95
