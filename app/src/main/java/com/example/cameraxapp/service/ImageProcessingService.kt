package com.example.cameraxapp.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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

    private fun cropToLandscape4x3(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(-90f)
        matrix.postScale(1f, -1f)
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()

        val flippedMatrix = Matrix()
        val flippedBitmap = Bitmap.createBitmap(
            rotatedBitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            flippedMatrix,
            true
        )
        bitmap.recycle()

        val width = flippedBitmap.width
        val height = flippedBitmap.height

        val targetWidth: Int
        val targetHeight: Int
        val x: Int
        val y: Int

        if (width.toFloat() / height > 4f / 3f) {
            targetHeight = height
            targetWidth = (height * 4 / 3)
            x = (width - targetWidth) / 2
            y = 0
        } else {
            targetWidth = width
            targetHeight = (width * 3 / 4)
            x = 0
            y = (height - targetHeight) / 2
        }

        val croppedBitmap = Bitmap.createBitmap(flippedBitmap, x, y, targetWidth, targetHeight)
        flippedBitmap.recycle()
        return croppedBitmap
    }

    fun saveCroppedImage(context: Context, originalUri: Uri, photoFile: File): Uri {
        try {
            val inputStream = context.contentResolver.openInputStream(originalUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                return originalUri
            }

            val croppedBitmap = cropToLandscape4x3(originalBitmap)

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
} 
