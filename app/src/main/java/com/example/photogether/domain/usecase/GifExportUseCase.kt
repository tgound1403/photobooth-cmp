package com.example.photogether.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.photogether.data.billing.shouldShowWatermark
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Use case for exporting a list of images as an animated GIF. Free users get the same watermark as
 * the photobooth strip. [isPremium] is read per export.
 */
class GifExportUseCase(
        private val isPremium: StateFlow<Boolean> = MutableStateFlow(false),
) {
    
    companion object {
        private const val TAG = "GifExportUseCase"
        private const val DEFAULT_DELAY_MS = 500
        private const val MAX_GIF_WIDTH = 480 // Max width to optimize file size and performance
        private const val GIF_QUALITY = 256 // Color quality (256 colors)
        private const val WATERMARK_PADDING_DP = 8
    }

    /**
     * Exports a list of images as an animated GIF
     * 
     * @param context Android context
     * @param imagePaths List of image file paths to include in the GIF
     * @param delayMs Delay between frames in milliseconds (default: 500ms)
     * @return Result containing the URI string of the saved GIF file, or error
     */
    suspend fun execute(
            context: Context,
            imagePaths: List<String>,
            delayMs: Int = DEFAULT_DELAY_MS
    ): Result<String> =
            withContext(Dispatchers.IO) {
                runCatching {
                    if (imagePaths.isEmpty()) {
                        throw IllegalArgumentException("Danh sách ảnh trống")
                    }

                    Log.d(TAG, "Bắt đầu tạo GIF từ ${imagePaths.size} ảnh")

                    // Create GIF encoder
                    val encoder = SimpleGifEncoder()
                    val bos = ByteArrayOutputStream()
                    
                    encoder.start(bos)
                    encoder.setDelay(delayMs)
                    encoder.setRepeat(0) // 0 = infinite repeat

                    val showWatermark = shouldShowWatermark(isPremium.value)

                    // Process each image
                    val processedCount = imagePaths.count { imagePath ->
                        try {
                            val bitmap = BitmapFactory.decodeFile(imagePath)
                            if (bitmap != null) {
                                // Resize bitmap if too large to save memory and processing time
                                val scaledBitmap = scaleBitmap(bitmap, MAX_GIF_WIDTH)
                                val frame = if (showWatermark) withWatermark(context, scaledBitmap) else scaledBitmap

                                // Add frame to GIF
                                encoder.addFrame(frame)

                                // Recycle bitmaps to free memory
                                if (frame != scaledBitmap) {
                                    frame.recycle()
                                }
                                if (scaledBitmap != bitmap) {
                                    scaledBitmap.recycle()
                                }
                                bitmap.recycle()
                                
                                true
                            } else {
                                Log.w(TAG, "Không thể decode ảnh: $imagePath")
                                false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Lỗi khi xử lý ảnh: $imagePath", e)
                            false
                        }
                    }

                    if (processedCount == 0) {
                        throw IOException("Không thể xử lý bất kỳ ảnh nào")
                    }

                    // Finish encoding
                    encoder.finish()
                    
                    Log.d(TAG, "Đã tạo GIF với $processedCount frames")

                    // Save GIF to MediaStore
                    val gifUri = saveGifToMediaStore(context, bos.toByteArray())
                    
                    Log.d(TAG, "Đã lưu GIF: $gifUri")
                    
                    gifUri
                }
            }

    /**
     * Scales a bitmap to a maximum width while maintaining aspect ratio
     * 
     * @param bitmap Original bitmap
     * @param maxWidth Maximum width for the scaled bitmap
     * @return Scaled bitmap or original if already smaller
     */
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val height = (maxWidth * aspectRatio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }

    /**
     * Returns a mutable copy of [frame] with the shared photobooth watermark drawn on it. The source
     * may be immutable, so it is never drawn on directly.
     */
    private fun withWatermark(context: Context, frame: Bitmap): Bitmap {
        val out = frame.copy(Bitmap.Config.ARGB_8888, true)
        val density = context.resources.displayMetrics.density
        drawPhotoBoothWatermark(
                context,
                Canvas(out),
                out.width,
                out.height,
                density,
                (WATERMARK_PADDING_DP * density).toInt(),
                Paint()
        )
        return out
    }

    /**
     * Saves GIF data to MediaStore and returns the URI
     * 
     * @param context Android context
     * @param gifData GIF file data as byte array
     * @return URI string of the saved GIF
     * @throws IOException if saving fails
     */
    private fun saveGifToMediaStore(context: Context, gifData: ByteArray): String {
        val contentValues = ContentValues().apply {
            put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "PhotoBooth_${System.currentTimeMillis()}.gif"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Không thể tạo MediaStore record cho GIF")

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(gifData)
            } ?: throw IOException("Không thể mở output stream để lưu GIF")

            // Mark as not pending (Android Q+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            return uri.toString()
        } catch (e: Exception) {
            // Clean up on error
            try {
                resolver.delete(uri, null, null)
            } catch (deleteException: Exception) {
                Log.e(TAG, "Lỗi khi xóa file lỗi", deleteException)
            }
            throw IOException("Lỗi khi lưu GIF: ${e.message}", e)
        }
    }
}
