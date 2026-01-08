package com.example.cameraxapp.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ImageProcessingService {
    fun createPhotoFile(context: Context): File {
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val storageDir = context.filesDir
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "$timeStamp.jpg")
    }

    private fun cropToLandscape4x3(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
        val matrix = Matrix()
        // Rotate image based on Exif data to make it upright
        matrix.postRotate(rotationDegrees)
        // Default to Front Camera Mirroring (Applied after rotation or combined? usually rotate then flip? or flip then rotate?)
        // Let's try: Rotate to Upright -> Flip Horizontal (Mirror).
        matrix.postScale(-1f, 1f)

        // Matrix application order is post (append).
        // If we do postRotate then postScale:
        // 1. Rotate.
        // 2. Scale (Mirror).
        // This effectively mirrors the *rotated* image.

        val newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()

        val width = newBitmap.width
        val height = newBitmap.height

        val targetRatio = 4f / 3f
        val currentRatio = width.toFloat() / height

        val targetWidth: Int
        val targetHeight: Int
        val x: Int
        val y: Int

        // If image is Wider than target (e.g. 16:9 vs 4:3), crop width
        if (currentRatio > targetRatio) {
            targetHeight = height
            targetWidth = (height * 4 / 3)
            x = (width - targetWidth) / 2
            y = 0
        } else {
            // Image is Taller/Narrower than target (e.g. 9:16 or 3:4 vs 4:3), crop height
            targetWidth = width
            targetHeight = (width * 3 / 4)
            x = 0
            y = (height - targetHeight) / 2
        }

        // Safety check
        val safeWidth = targetWidth.coerceAtMost(width)
        val safeHeight = targetHeight.coerceAtMost(height)
        val safeX = x.coerceAtLeast(0)
        val safeY = y.coerceAtLeast(0)

        val croppedBitmap = Bitmap.createBitmap(newBitmap, safeX, safeY, safeWidth, safeHeight)
        newBitmap.recycle()
        return croppedBitmap
    }

    fun saveCroppedImage(context: Context, originalUri: Uri, photoFile: File): Uri {
        try {
            val rotation = getRotationDegrees(context, originalUri)

            val inputStream = context.contentResolver.openInputStream(originalUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                return originalUri
            }

            val croppedBitmap = cropToLandscape4x3(originalBitmap, rotation)

            val outputStream = FileOutputStream(photoFile)
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

            outputStream.close()
            originalBitmap.recycle()
            croppedBitmap.recycle()

            return Uri.fromFile(photoFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return originalUri
        }
    }

    private fun getRotationDegrees(context: Context, uri: Uri): Float {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0f
            val exifInterface = ExifInterface(inputStream)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return 0f
        }
    }
}
