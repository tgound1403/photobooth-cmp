package com.example.cameraxapp.viewmodel

import android.content.Context
import android.graphics.*
import androidx.lifecycle.ViewModel
import com.example.cameraxapp.core.enum.EditType
import com.example.cameraxapp.core.enum.ImageInfo
import com.example.cameraxapp.core.enum.TabType
import com.example.cameraxapp.data.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class EditImageViewModel : ViewModel() {
    val photoEditFeatures = listOf(
        EditType.FILTER,
        EditType.FRAME,
        EditType.SCRATCH,
        EditType.LIGHT_LEAK,
        EditType.DISTORTION,
        EditType.SCRATCH
    )

    val filters = listOf(
        Preset("None", ColorMatrix().apply {
            set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }),
        Preset("BnW", ColorMatrix().apply {
            set(
                floatArrayOf(
                    0f, 1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f
                )
            )
        }),
        Preset("Kodak", ColorMatrix().apply {
            set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    -0.4f, 1.3f, -0.4f, 0.2f, -0.1f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }),
        Preset("FujiFilm", ColorMatrix().apply {
            set(
                floatArrayOf(
                    1f, 0f, 0f, 1.7f, -1.7f,
                    0.6f, 1f, 0f, 0f, 0.3f,
                    -0.2f, 0f, 1f, 0f, 0f,
                    0.1f, 0f, 0f, .6f, .2f
                )
            )
        }),
        Preset("Nikon", ColorMatrix().apply {
            set(
                floatArrayOf(
                    1.3f, -.3f, 1.1f, 0f, 0f,
                    0f, 1.3f, .2f, 0f, 0f,
                    0f, 0f, .8f, .2f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }),
        Preset("Pentax", ColorMatrix().apply {
            set(
                floatArrayOf(
                    0f, 1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, .6f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }),
        Preset("Minolta", ColorMatrix().apply {
            set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    -.2f, 1f, .3f, .1f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        })
    )

    val tabs = listOf(TabType.CUSTOMIZE, TabType.BASIC, TabType.FAVOURITE)
    val imageInfo = listOf(ImageInfo.BRIGHTNESS, ImageInfo.TEMPERATURE, ImageInfo.CONTRAST, ImageInfo.SATURATION, ImageInfo.SHARPNESS, ImageInfo.HIGHLIGHT, ImageInfo.SHADOW)

    val frames = listOf("Frame 1", "Frame 2", "Frame 3", "Frame 4", "Frame 5")
    val lightLeaks = listOf("Light leak 1", "Light leak 2", "Light leak 3", "Light leak 4", "Light leak 5")
    val distortions = listOf("Distortion 1", "Distortion 2", "Distortion 3", "Distortion 4", "Distortion 5")
    val scratches = listOf("Scratch 1", "Scratch 2", "Scratch 3", "Scratch 4", "Scratch 5")

    suspend fun applyBrightness(bitmap: Bitmap, brightness: Float): Bitmap {
        return withContext(Dispatchers.IO){
            val colorMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        1f + brightness, 0f, 0f, 0f, 0f,
                        0f, 1f + brightness, 0f, 0f, 0f,
                        0f, 0f, 1f + brightness, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }

            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config!!)
            Canvas(resultBitmap).drawBitmap(bitmap, 0f, 0f, paint)
            resultBitmap
        }
    }

    suspend fun applyContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        return withContext(Dispatchers.IO){
            val colorMatrix = ColorMatrix().apply {
                setScale(1f + contrast, 1f + contrast, 1f + contrast, 1.0f) // Contrast
            }

            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config!!)
            Canvas(resultBitmap).drawBitmap(bitmap, 0f, 0f, paint)
            resultBitmap
        }
    }

    suspend fun applySaturation(bitmap: Bitmap, saturation: Float): Bitmap {
        return withContext(Dispatchers.IO){
            val colorMatrix = ColorMatrix().apply {
                setSaturation(1f + saturation)
            }

            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config!!)
            Canvas(resultBitmap).drawBitmap(bitmap, 0f, 0f, paint)
            resultBitmap
        }
    }

    suspend fun applyTemperature(bitmap: Bitmap, warmth: Float): Bitmap {
        return withContext(Dispatchers.IO) {
            val colorMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        warmth, 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 2f - warmth, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }

            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config!!)
            Canvas(resultBitmap).drawBitmap(bitmap, 0f, 0f, paint)
            resultBitmap
        }
    }

    fun applyPresetFilter(
        bitmap: Bitmap,
        matrix: ColorMatrix
    ): Bitmap {
//        val colorMatrix = ColorMatrix().apply {
//            setScale(contrast, contrast, contrast, 1.0f) // Contrast
//            postConcat(
//                ColorMatrix(
//                    floatArrayOf(
//                        1f, 0f, 0f, 0f, brightness * 255,
//                        0f, 1f, 0f, 0f, brightness * 255,
//                        0f, 0f, 1f, 0f, brightness * 255,
//                        0f, 0f, 0f, 1f, 0f
//                    )
//                )
//            )
//        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }

        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config!!)
        Canvas(resultBitmap).drawBitmap(bitmap, 0f, 0f, paint)
        return resultBitmap
    }

    suspend fun saveBitmapToDevice(bitmap: Bitmap, filePath: String): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun createPolaroidFrame(originalBitmap: Bitmap): Bitmap {
        val margin = 100f  // Khoảng trắng xung quanh ảnh
        val bottomMargin = 300f  // Phần trắng dưới kiểu Polaroid

        // Tạo bitmap mới với kích thước lớn hơn để chứa khung
        val frameWidth = originalBitmap.width + (margin * 2).toInt()
        val frameHeight = originalBitmap.height + (margin * 2 + bottomMargin).toInt()

        val resultBitmap = Bitmap.createBitmap(
            frameWidth,
            frameHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resultBitmap)

        // Vẽ nền trắng
        val framePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat(), framePaint)

        // Thêm đổ bóng nhẹ
        val shadowPaint = Paint().apply {
            color = Color.LTGRAY
            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRect(margin, margin,
            frameWidth - margin,
            frameHeight - bottomMargin, shadowPaint)

        // Vẽ ảnh gốc
        canvas.drawBitmap(originalBitmap, margin, margin, null)

        return resultBitmap
    }

    fun createPolaroidFrameWithDate(
        context: Context,
        originalBitmap: Bitmap,
        dateFormat: String = "dd.MM.yyyy"
    ): Bitmap {
        // Tính toán kích thước khung
        val margin = 80f  // Viền trắng xung quanh
        val bottomMargin = 200f  // Phần trắng dưới để chứa ngày tháng

        val frameWidth = originalBitmap.width + (margin * 2).toInt()
        val frameHeight = originalBitmap.height + (margin * 12 + bottomMargin).toInt()

        // Tạo bitmap mới với nền trắng
        val resultBitmap = Bitmap.createBitmap(
            frameWidth,
            frameHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resultBitmap)

        // Vẽ nền trắng cho frame
        val framePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat(), framePaint)

//        // Thêm đổ bóng nhẹ cho ảnh
//        val shadowPaint = Paint().apply {
//            color = Color.LTGRAY
//            maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
//        }
//        canvas.drawRect(
//            margin,
//            margin,
//            frameWidth - margin,
//            frameHeight - bottomMargin,
//            shadowPaint
//        )

        // Vẽ ảnh gốc
        canvas.drawBitmap(originalBitmap, margin, margin, null)

        // Thêm ngày tháng
        val currentDate = SimpleDateFormat(dateFormat, Locale.getDefault())
            .format(Date())

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 160f
            typeface = Typeface.create("sans-serif-bold", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Vẽ ngày tháng ở dưới ảnh
        canvas.drawText(
            currentDate,
            frameWidth / 2f,
            frameHeight - bottomMargin / 2,
            textPaint
        )

        // Thêm texture cho khung
        addPolaroidTexture(canvas, frameWidth.toFloat(), frameHeight.toFloat())

        return resultBitmap
    }

    private fun addPolaroidTexture(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.argb(5, 0, 0, 0)
        }

        // Tạo texture ngẫu nhiên nhẹ
        val random = Random
        for (i in 0..200) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            val radius = random.nextFloat() * 2f
            canvas.drawCircle(x, y, radius, paint)
        }
    }

    // Thêm texture cho khung
    fun addPolaroidTexture(bitmap: Bitmap): Bitmap {
        val paint = Paint().apply {
            colorFilter = PorterDuffColorFilter(
                Color.argb(20, 0, 0, 0),
                PorterDuff.Mode.OVERLAY
            )
        }

        val canvas = Canvas(bitmap)
        // Tạo texture ngẫu nhiên cho khung
        for (i in 0..100) {
            val x = Random.nextFloat() * bitmap.width
            val y = Random.nextFloat() * bitmap.height
            val radius = Random.nextFloat() * 2f
            canvas.drawCircle(x, y, radius, paint)
        }

        return bitmap
    }

    fun addVignette(bitmap: Bitmap): Bitmap {
        val paint = Paint()
        val canvas = Canvas(bitmap)

        // Tạo gradient cho hiệu ứng vignette
        val radialGradient = RadialGradient(
            bitmap.width / 2f,
            bitmap.height / 2f,
            bitmap.height * 0.7f,
            intArrayOf(Color.TRANSPARENT, Color.argb(180, 0, 0, 0)),
            null,
            Shader.TileMode.CLAMP
        )

        paint.shader = radialGradient
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)

        canvas.drawRect(0f, 0f,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            paint)

        return bitmap
    }

    fun addNoiseTexture(bitmap: Bitmap, intensity: Float = 0.1f): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0,
            bitmap.width, bitmap.height)

        val random = Random

        for (i in pixels.indices) {
            if (random.nextFloat() < intensity) {
                val noise = (random.nextFloat() * 255).toInt()
                pixels[i] = Color.argb(
                    255,
                    noise,
                    noise,
                    noise
                )
            }
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0,
            bitmap.width, bitmap.height)

        return bitmap
    }

    fun addColorShift(bitmap: Bitmap): Bitmap {
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setScale(1.1f, 1.0f, 0.9f, 1f) // Nhẹ về màu ấm
            })
        }

        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return bitmap
    }
}

class PolaroidFrameGenerator {
    fun createPolaroidFrame(originalBitmap: Bitmap): Bitmap {
        val margin = 100f  // Khoảng trắng xung quanh ảnh
        val bottomMargin = 300f  // Phần trắng dưới kiểu Polaroid

        // Tạo bitmap mới với kích thước lớn hơn để chứa khung
        val frameWidth = originalBitmap.width + (margin * 2).toInt()
        val frameHeight = originalBitmap.height + (margin * 2 + bottomMargin).toInt()

        val resultBitmap = Bitmap.createBitmap(
            frameWidth,
            frameHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resultBitmap)

        // Vẽ nền trắng
        val framePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat(), framePaint)

        // Thêm đổ bóng nhẹ
        val shadowPaint = Paint().apply {
            color = Color.LTGRAY
            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRect(margin, margin,
            frameWidth - margin,
            frameHeight - bottomMargin, shadowPaint)

        // Vẽ ảnh gốc
        canvas.drawBitmap(originalBitmap, margin, margin, null)

        return resultBitmap
    }

    // Thêm texture cho khung
    fun addPolaroidTexture(bitmap: Bitmap): Bitmap {
        val paint = Paint().apply {
            colorFilter = PorterDuffColorFilter(
                Color.argb(20, 0, 0, 0),
                PorterDuff.Mode.OVERLAY
            )
        }

        val canvas = Canvas(bitmap)
        // Tạo texture ngẫu nhiên cho khung
        for (i in 0..100) {
            val x = Random.nextFloat() * bitmap.width
            val y = Random.nextFloat() * bitmap.height
            val radius = Random.nextFloat() * 2f
            canvas.drawCircle(x, y, radius, paint)
        }

        return bitmap
    }
}

class PolaroidEffects {
    fun addVignette(bitmap: Bitmap): Bitmap {
        val paint = Paint()
        val canvas = Canvas(bitmap)

        // Tạo gradient cho hiệu ứng vignette
        val radialGradient = RadialGradient(
            bitmap.width / 2f,
            bitmap.height / 2f,
            bitmap.height * 0.7f,
            intArrayOf(Color.TRANSPARENT, Color.argb(180, 0, 0, 0)),
            null,
            Shader.TileMode.CLAMP
        )

        paint.shader = radialGradient
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)

        canvas.drawRect(0f, 0f,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            paint)

        return bitmap
    }

    fun addNoiseTexture(bitmap: Bitmap, intensity: Float = 0.1f): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0,
            bitmap.width, bitmap.height)

        val random = Random

        for (i in pixels.indices) {
            if (random.nextFloat() < intensity) {
                val noise = (random.nextFloat() * 255).toInt()
                pixels[i] = Color.argb(
                    255,
                    noise,
                    noise,
                    noise
                )
            }
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0,
            bitmap.width, bitmap.height)

        return bitmap
    }

    fun addColorShift(bitmap: Bitmap): Bitmap {
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setScale(1.1f, 1.0f, 0.9f, 1f) // Nhẹ về màu ấm
            })
        }

        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return bitmap
    }
}