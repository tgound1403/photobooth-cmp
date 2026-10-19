package com.example.photogether.ui.onboarding.anim

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.photogether.core.colors.ColorStories
import com.example.photogether.core.locale.AppLanguage
import com.example.photogether.core.locale.AppLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Bundled photos from assets, decoded at half size and cached for the session. */
private object SamplePhotos {
    private val cache = HashMap<String, ImageBitmap>()

    fun get(context: Context, path: String): ImageBitmap? = synchronized(cache) {
        cache[path] ?: decode(context, path)?.also { cache[path] = it }
    }

    private fun decode(context: Context, path: String): ImageBitmap? {
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        return runCatching {
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

/** Asset path of the onboarding photo [number] for [language]: Vietnamese landmarks or generic samples. */
private fun photoPath(language: AppLanguage, number: Int): String = when (language) {
    AppLanguage.VIETNAMESE -> "landmarks/${VIETNAMESE_PHOTOS[(number - 1) % VIETNAMESE_PHOTOS.size]}"
    AppLanguage.ENGLISH -> "samples/" + ColorStories.SAMPLE_FILES[(number - 1) % ColorStories.SAMPLE_FILES.size]
}

/** One favourite per landmark, so the pages show the whole country rather than one city. */
private val VIETNAMESE_PHOTOS = listOf(
    "halong_1.jpg",
    "hoian_3.jpg",
    "dalat_3.jpg",
    "saigon_2.jpg",
    "hue_2.jpg",
)

/**
 * Loads onboarding photos by number off the main thread. Vietnamese shows the Vietnamese landmarks,
 * English the generic samples. Entries are null until decoded, so callers fall back to their drawn
 * placeholder for the first frame.
 */
@Composable
fun rememberSamplePhotos(vararg numbers: Int): List<ImageBitmap?> {
    val context = LocalContext.current.applicationContext
    val language = AppLocale.current()
    val key = numbers.toList()
    return produceState<List<ImageBitmap?>>(initialValue = List(key.size) { null }, key, language) {
        value = withContext(Dispatchers.IO) {
            key.map { SamplePhotos.get(context, photoPath(language, it)) }
        }
    }.value
}

/** Draws [image] over [dst] cropped to fill it, like a centre-crop. [colorFilter] grades the photo. */
internal fun DrawScope.drawPhotoCover(
    image: ImageBitmap,
    dst: Rect,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
) {
    val srcAspect = image.width.toFloat() / image.height
    val dstAspect = dst.width / dst.height
    val srcW: Int
    val srcH: Int
    if (srcAspect > dstAspect) {
        srcH = image.height
        srcW = (image.height * dstAspect).roundToInt()
    } else {
        srcW = image.width
        srcH = (image.width / dstAspect).roundToInt()
    }
    val srcX = (image.width - srcW) / 2
    val srcY = (image.height - srcH) / 2
    drawImage(
        image = image,
        srcOffset = IntOffset(srcX, srcY),
        srcSize = IntSize(srcW, srcH),
        dstOffset = IntOffset(dst.left.roundToInt(), dst.top.roundToInt()),
        dstSize = IntSize(dst.width.roundToInt(), dst.height.roundToInt()),
        alpha = alpha,
        colorFilter = colorFilter,
    )
}
