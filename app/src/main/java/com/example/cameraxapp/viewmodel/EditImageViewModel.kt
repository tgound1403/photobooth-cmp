package com.example.cameraxapp.viewmodel

import android.content.Context
import android.graphics.*
import androidx.lifecycle.ViewModel
import com.example.cameraxapp.core.enum.EditType
import com.example.cameraxapp.core.enum.ImageInfo
import com.example.cameraxapp.core.enum.TabType
import com.example.cameraxapp.data.model.Preset
import com.example.cameraxapp.data.model.StickerOverlay
import com.example.cameraxapp.data.model.OverlayType
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
        EditType.DISTORTION
    )
    
    val decorFeatures = listOf(
        EditType.STICKER,
        EditType.TEXT
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

    val tabs = listOf(TabType.CUSTOMIZE, TabType.BASIC, TabType.DECOR, TabType.FAVOURITE)
    val imageInfo = listOf(ImageInfo.BRIGHTNESS, ImageInfo.TEMPERATURE, ImageInfo.CONTRAST, ImageInfo.SATURATION, ImageInfo.SHARPNESS, ImageInfo.HIGHLIGHT, ImageInfo.SHADOW)

    val frames = listOf("Frame 1", "Frame 2", "Frame 3", "Frame 4", "Frame 5")
    val lightLeaks = listOf("Light leak 1", "Light leak 2", "Light leak 3", "Light leak 4", "Light leak 5")
    val distortions = listOf("Distortion 1", "Distortion 2", "Distortion 3", "Distortion 4", "Distortion 5")
    val scratches = listOf("Scratch 1", "Scratch 2", "Scratch 3", "Scratch 4", "Scratch 5")
    
    // Sticker/Emoji options (using emojis as stickers)
    val stickers = listOf("❤️", "⭐", "✨", "🎉", "🎈", "🌈", "☀️", "🌙", "💫", "🔥")
    
    // Overlay state
    val overlays = mutableListOf<StickerOverlay>()

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

    suspend fun applyScratch(bitmap: Bitmap, type: String): Bitmap {
        return withContext(Dispatchers.IO) {
            val resultBitmap = bitmap.copy(bitmap.config!!, true)
            val canvas = Canvas(resultBitmap)
            val paint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
                alpha = 150
                isAntiAlias = true
            }
            
            // Deterministic random based on type hash for consistency
            val random = Random(type.hashCode())
            val count = 20 + random.nextInt(30)
            
            for (i in 0 until count) {
                val startX = random.nextFloat() * bitmap.width
                val startY = random.nextFloat() * bitmap.height
                val endX = startX + (random.nextFloat() - 0.5f) * 100
                val endY = startY + (random.nextFloat() - 0.5f) * 100
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
            resultBitmap
        }
    }

    suspend fun applyDistortion(bitmap: Bitmap, type: String): Bitmap {
        return withContext(Dispatchers.IO) {
             // Simple Chromatic Aberration (RGB Shift)
            val shift = 10f * (type.lastOrNull()?.digitToInt() ?: 1)
            val width = bitmap.width
            val height = bitmap.height
            val resultBitmap = Bitmap.createBitmap(width, height, bitmap.config!!) // Use raw config, prevent null safety issues if needed, strictly standard ARGB usually
            val canvas = Canvas(resultBitmap)
            
            val paint = Paint()
            
            // Draw Red Channel shifted Left
            paint.colorFilter = LightingColorFilter(0xFFFF0000.toInt(), 0)
            canvas.drawBitmap(bitmap, -shift, 0f, paint)
            
            // Draw Green Channel Centered (Add mode)
            paint.colorFilter = LightingColorFilter(0xFF00FF00.toInt(), 0)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            
            // Draw Blue Channel shifted Right
            paint.colorFilter = LightingColorFilter(0xFF0000FF.toInt(), 0)
            canvas.drawBitmap(bitmap, shift, 0f, paint)
            
            // Reset Xfermode for safety if reused
             paint.xfermode = null
             
             resultBitmap
        }
    }

    suspend fun applyLightLeak(bitmap: Bitmap, type: String): Bitmap {
        return withContext(Dispatchers.IO) {
            val resultBitmap = bitmap.copy(bitmap.config!!, true)
            val canvas = Canvas(resultBitmap)
            
            val width = bitmap.width.toFloat()
            val height = bitmap.height.toFloat()
            
            // Deterministic "random" based on type
            val hash = type.hashCode()
            val side = hash % 4 // 0: Left, 1: Top, 2: Right, 3: Bottom
            
            val centerX = when {
                side == 0 || side == 0 -> 0f 
                side == 2 -> width
                else -> width * 0.5f + (hash % 100)
            }
            // Allow random center
             val cx = if (hash % 2 == 0) 0f else width
             val cy = if (hash % 3 == 0) 0f else height

            val radius = width.coerceAtLeast(height) * (0.5f + (hash % 5) / 10f)
            
            val colors = intArrayOf(
                Color.argb(150, 255, 200, 0), // Orange/Yellow
                Color.argb(100, 255, 100, 0), // Reddish
                Color.TRANSPARENT
            )
            
            val gradient = RadialGradient(cx, cy, radius, colors, null, Shader.TileMode.CLAMP)
            
            val paint = Paint().apply {
                shader = gradient
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN) // Screen blend mode for light leak
                isAntiAlias = true
            }
            
            canvas.drawRect(0f, 0f, width, height, paint)
            
            resultBitmap
        }
    }

    suspend fun applySharpness(bitmap: Bitmap, sharpness: Float): Bitmap {
        return withContext(Dispatchers.IO) {
            // Convolution Matrix for Sharpness
            // Basic kernel: [ -1 -1 -1 ]
            //               [ -1  9 -1 ]
            //               [ -1 -1 -1 ]
            // We'll interpolate between original and sharpened based on 'sharpness' (0..1)
            // But here sharpness input is generic. Let's assume 0..1 range or similar.
            // If sharpness is 0, return original? Or allows negative blur?
            // Let's assume sharpness is 0..1 intensity.
            
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val resultPixels = IntArray(width * height)
            
            // Simple 3x3 Convolution
            // Doing this in pure Kotlin is slow for large images, but acceptable for typical phone camera photos if not too huge.
            // Optimized approach would be RenderScript, but let's stick to standard loop for compatibility/simplicity without NDK/RS.
            
            val newAlpha = 255
            
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val idx = y * width + x
                    
                    var rSum = 0
                    var gSum = 0
                    var bSum = 0
                    
                    // Kernel: 
                    // 0 -1  0
                    // -1 5 -1
                    // 0 -1  0
                    // A milder sharpen kernel.
                    
                    val centerVal = 5
                    val neighborVal = -1
                    
                    // Center
                    val p = pixels[idx]
                    rSum += Color.red(p) * centerVal
                    gSum += Color.green(p) * centerVal
                    bSum += Color.blue(p) * centerVal
                    
                    // Neighbors (Top, Bottom, Left, Right)
                    val pT = pixels[idx - width]
                    rSum += Color.red(pT) * neighborVal
                    gSum += Color.green(pT) * neighborVal
                    bSum += Color.blue(pT) * neighborVal
                    
                    val pB = pixels[idx + width]
                    rSum += Color.red(pB) * neighborVal
                    gSum += Color.green(pB) * neighborVal
                    bSum += Color.blue(pB) * neighborVal
                    
                    val pL = pixels[idx - 1]
                    rSum += Color.red(pL) * neighborVal
                    gSum += Color.green(pL) * neighborVal
                    bSum += Color.blue(pL) * neighborVal
                    
                    val pR = pixels[idx + 1]
                    rSum += Color.red(pR) * neighborVal
                    gSum += Color.green(pR) * neighborVal
                    bSum += Color.blue(pR) * neighborVal
                    
                    // Clamp
                    val r = rSum.coerceIn(0, 255)
                    val g = gSum.coerceIn(0, 255)
                    val b = bSum.coerceIn(0, 255)
                    
                    // Blend based on intensity (sharpness)
                    if (sharpness > 0) {
                         val origR = Color.red(p)
                         val origG = Color.green(p)
                         val origB = Color.blue(p)
                         
                         val finalR = (origR + (r - origR) * sharpness).toInt().coerceIn(0,255)
                         val finalG = (origG + (g - origG) * sharpness).toInt().coerceIn(0,255)
                         val finalB = (origB + (b - origB) * sharpness).toInt().coerceIn(0,255)
                         
                         resultPixels[idx] = Color.argb(newAlpha, finalR, finalG, finalB)
                    } else {
                         resultPixels[idx] = p
                    }
                }
            }
             // Fill borders
            for (x in 0 until width) {
                resultPixels[x] = pixels[x]
                resultPixels[(height-1)*width + x] = pixels[(height-1)*width + x]
            }
            for (y in 0 until height) {
                resultPixels[y*width] = pixels[y*width]
                resultPixels[y*width + width-1] = pixels[y*width + width-1]
            }

            val result = Bitmap.createBitmap(width, height, bitmap.config!!)
            result.setPixels(resultPixels, 0, width, 0, 0, width, height)
            result
        }
    }

    suspend fun applyHighlight(bitmap: Bitmap, highlight: Float): Bitmap {
        return withContext(Dispatchers.IO) {
             val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // Highlight adjustment: affects bright areas. 
            // Positive highlight -> Brighten brights
            // Negative highlight -> Darken brights (recover highlights)
            
            for (i in pixels.indices) {
                val p = pixels[i]
                var r = Color.red(p)
                var g = Color.green(p)
                var b = Color.blue(p)
                
                // Simple Luminance Approximation
                val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                
                // Apply only if luminance is high (e.g. > 0.5)
                if (lum > 0.5) {
                    val factor = 1f + highlight * (lum - 0.5).toFloat() * 2f // Scale effect by how bright it is
                    r = (r * factor).toInt().coerceIn(0, 255)
                    g = (g * factor).toInt().coerceIn(0, 255)
                    b = (b * factor).toInt().coerceIn(0, 255)
                }
                 pixels[i] = Color.argb(Color.alpha(p), r, g, b)
            }
            
            val result = Bitmap.createBitmap(width, height, bitmap.config!!)
            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
        }
    }

    suspend fun applyShadow(bitmap: Bitmap, shadow: Float): Bitmap {
        return withContext(Dispatchers.IO) {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // Shadow adjustment: affects dark areas.
            
            for (i in pixels.indices) {
                val p = pixels[i]
                var r = Color.red(p)
                var g = Color.green(p)
                var b = Color.blue(p)
                
                 val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                 
                 // Apply only if luminance is low (e.g. < 0.5)
                 if (lum < 0.5) {
                     val factor = 1f + shadow * (0.5 - lum).toFloat() * 2f
                     r = (r * factor).toInt().coerceIn(0, 255)
                     g = (g * factor).toInt().coerceIn(0, 255)
                     b = (b * factor).toInt().coerceIn(0, 255)
                 }
                pixels[i] = Color.argb(Color.alpha(p), r, g, b)
            }
             val result = Bitmap.createBitmap(width, height, bitmap.config!!)
            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
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
    
    // Overlay management methods
    fun addOverlay(overlay: StickerOverlay) {
        overlays.add(overlay)
    }
    
    fun removeOverlay(id: String) {
        overlays.removeAll { it.id == id }
    }
    
    fun updateOverlay(id: String, offsetX: Float, offsetY: Float, scale: Float, rotation: Float) {
        overlays.find { it.id == id }?.apply {
            this.offsetX = offsetX
            this.offsetY = offsetY
            this.scale = scale
            this.rotation = rotation
        }
    }
    
    suspend fun applyOverlaysToBitmap(bitmap: Bitmap): Bitmap {
        return withContext(Dispatchers.IO) {
            val resultBitmap = bitmap.copy(bitmap.config!!, true)
            val canvas = Canvas(resultBitmap)
            
            overlays.forEach { overlay ->
                val paint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                
                canvas.save()
                canvas.translate(overlay.offsetX, overlay.offsetY)
                canvas.rotate(overlay.rotation)
                canvas.scale(overlay.scale, overlay.scale)
                
                when (overlay.type) {
                    OverlayType.STICKER, OverlayType.TEXT -> {
                        paint.textSize = if (overlay.type == OverlayType.STICKER) 80f else 60f
                        paint.color = if (overlay.type == OverlayType.TEXT) Color.WHITE else Color.BLACK
                        if (overlay.type == OverlayType.TEXT) {
                            paint.setShadowLayer(5f, 0f, 0f, Color.BLACK)
                        }
                        canvas.drawText(overlay.content, 0f, 0f, paint)
                    }
                }
                
                canvas.restore()
            }
            
            resultBitmap
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
