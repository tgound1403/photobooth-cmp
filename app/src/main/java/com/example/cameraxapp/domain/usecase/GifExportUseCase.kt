package com.example.cameraxapp.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.nbadal.androidgifencoder.AnimatedGifEncoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GifExportUseCase {
    suspend fun execute(
            context: Context,
            imagePaths: List<String>,
            delayMs: Int = 500
    ): Result<String> =
            withContext(Dispatchers.IO) {
                runCatching {
                    if (imagePaths.isEmpty()) {
                        throw IllegalArgumentException("Danh sách ảnh trống")
                    }

                    val bos = ByteArrayOutputStream()
                    val encoder = AnimatedGifEncoder()
                    encoder.start(bos)
                    encoder.setDelay(delayMs)
                    encoder.setRepeat(0) // 0 for infinite repeat

                    imagePaths.forEach { path ->
                        val bitmap = BitmapFactory.decodeFile(path)
                        if (bitmap != null) {
                            // Resize bitmap if too large to save memory and processing time
                            val scaledBitmap = scaleBitmap(bitmap, 480)
                            encoder.addFrame(scaledBitmap)
                            if (scaledBitmap != bitmap) {
                                scaledBitmap.recycle()
                            }
                            bitmap.recycle()
                        }
                    }

                    encoder.finish()

                    val gifFile = createGifFile(context)
                    FileOutputStream(gifFile).use { fos -> fos.write(bos.toByteArray()) }

                    gifFile.absolutePath
                }
            }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val height = (maxWidth * aspectRatio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }

    private fun createGifFile(context: Context): File {
        val fileName = "GIF_${System.currentTimeMillis()}.gif"
        val storageDir = context.getExternalFilesDir(null)
        if (storageDir?.exists() == false) {
            storageDir.mkdirs()
        }
        return File(storageDir, fileName)
    }
}
