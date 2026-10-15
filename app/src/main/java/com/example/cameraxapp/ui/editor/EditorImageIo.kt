package com.example.cameraxapp.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
}

class AndroidEditorImageIo(private val context: Context) : EditorImageIo {

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
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun decodeFromFile(path: String, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        return BitmapFactory.decodeFile(path, sampleOptions(bounds.outWidth, bounds.outHeight, maxSide))
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
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
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
