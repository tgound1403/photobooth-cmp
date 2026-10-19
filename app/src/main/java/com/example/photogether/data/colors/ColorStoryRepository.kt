package com.example.photogether.data.colors

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.photogether.core.colors.ColorStories
import com.example.photogether.core.colors.ColorStory
import com.example.photogether.core.locale.AppLanguage
import com.example.photogether.core.render.EditParams
import com.example.photogether.core.render.EditRenderer
import com.example.photogether.core.render.EditSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Attribution of one bundled landmark photo, read from `assets/landmarks/credits.json`. */
@Serializable
data class SampleCredit(
    val file: String,
    val query: String = "",
    val photoId: String = "",
    val photographer: String,
    val photographerUrl: String,
    val photoUrl: String,
    val description: String = "",
)

@Serializable
data class SampleCredits(
    val source: String = "",
    val photos: List<SampleCredit> = emptyList(),
)

/** A bundled sample photo after a colour story's filter was applied, cached as a JPEG on disk. */
data class ColorPhoto(
    val file: String,
    /** Absolute path of the graded JPEG. */
    val path: String,
    val photographer: String,
    val photographerUrl: String,
    val photoUrl: String,
    /** The look applied to this photo, including its per-photo adjustments. */
    val recipe: EditParams,
)

/**
 * Applies a [ColorStory]'s filter to its bundled sample photos with the same [EditRenderer] the
 * editor uses. Every render starts from the original asset, never from a previous output. Graded
 * JPEGs are cached in [cacheRoot], so after the first render the photos load without GL work.
 */
class ColorStoryRepository(
    private val assets: AssetManager,
    private val renderer: EditRenderer,
    private val cacheRoot: File,
) {
    // The GL renderer renders one bitmap at a time.
    private val renderLock = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private var creditsCache: SampleCredits? = null

    /** The first photo of [story] in [language], for the Home tile. */
    suspend fun cover(story: ColorStory, language: AppLanguage): ColorPhoto? =
        photos(story, language, count = 1).firstOrNull()

    /** Up to [count] graded photos of [story] in [language]. A photo that fails to render is skipped. */
    suspend fun photos(
        story: ColorStory,
        language: AppLanguage,
        count: Int = ColorStories.PHOTOS_PER_STORY,
    ): List<ColorPhoto> = ColorStories.photosFor(story, language).take(count).mapNotNull { file ->
        try {
            photo(story, file)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Attribution for every bundled photo: landmarks first, then samples. */
    suspend fun credits(): SampleCredits = creditsCache ?: withContext(Dispatchers.IO) {
        val photos = listOf(LANDMARKS_DIR, SAMPLES_DIR).flatMap { dir ->
            json.decodeFromString<SampleCredits>(
                assets.open("$dir/$CREDITS_FILE").bufferedReader().use { it.readText() },
            ).photos
        }
        SampleCredits(photos = photos)
    }.also { creditsCache = it }

    private suspend fun photo(story: ColorStory, file: String): ColorPhoto {
        val credit = credits().photos.firstOrNull { it.file == file }
            ?: error("No credit for $file")
        val out = File(
            File(cacheRoot, story.id),
            "${file.removeSuffix(".jpg")}_${story.filterId}_v$GRADE_VERSION.jpg",
        )
        if (!out.isFile) grade(story, file, out)
        return ColorPhoto(
            file = file,
            path = out.absolutePath,
            photographer = credit.photographer,
            photographerUrl = credit.photographerUrl,
            photoUrl = credit.photoUrl,
            recipe = normalised(file, story.filterId),
        )
    }

    private suspend fun grade(story: ColorStory, file: String, out: File) {
        val source = withContext(Dispatchers.IO) {
            assets.open("${dirOf(file)}/$file").use { BitmapFactory.decodeStream(it) }
        } ?: error("Cannot decode $file")
        val graded = try {
            renderLock.withLock {
                renderer.render(
                    source = source,
                    snapshot = EditSnapshot(params = normalised(file, story.filterId)),
                    background = null,
                    density = 1f,
                    maxSize = MAX_SIDE,
                )
            }
        } finally {
            source.recycle()
        }
        withContext(Dispatchers.IO) {
            out.parentFile?.mkdirs()
            out.outputStream().use { graded.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        }
        graded.recycle()
    }

    companion object {
        private const val LANDMARKS_DIR = "landmarks"
        private const val SAMPLES_DIR = "samples"
        private const val CREDITS_FILE = "credits.json"

        /** Generic samples are `sample_NN.jpg`; everything else is a Vietnamese landmark. */
        private fun dirOf(file: String): String =
            if (file.startsWith("sample_")) SAMPLES_DIR else LANDMARKS_DIR
        private const val MAX_SIDE = 1080
        private const val JPEG_QUALITY = 88

        /** Bump when the grade changes, so cached JPEGs from older renders are not reused. */
        private const val GRADE_VERSION = 6

        /**
         * Per-photo exposure offset (-1..1, mapped to -2..+2 EV), for photos whose mean brightness
         * under the chosen filter is too dark or too bright. Photos not listed keep 0.
         */
        private val PHOTO_EXPOSURE = mapOf(
            "saigon_5.jpg" to 0.36f,
            "hanoi_4.jpg" to 0.2f,
            "danang_3.jpg" to -0.21f,
            "danang_5.jpg" to -0.25f,
            "hoian_1.jpg" to 0.22f,
            "hoian_2.jpg" to 0.2f,
            "hoian_3.jpg" to 0.19f,
        )

        /** Photos whose colour is too strong under the filter; the look is blended back toward the original. */
        private val CALMER_PHOTOS = setOf("danang_1.jpg", "danang_2.jpg")

        /**
         * The base look of a colour story: its filter with the tone guard. The tone guard pulls
         * highlights back so skies do not blow out, lifts shadows so they do not crush, and softens
         * contrast a little. Used as-is for the live camera.
         */
        fun look(filterId: String): EditParams = EditParams(
            filterId = filterId,
            highlight = -0.3f,
            shadow = 0.2f,
            contrast = -0.1f,
        )

        /** The look of one bundled photo: [look] plus that photo's blend and exposure offset. */
        fun normalised(file: String, filterId: String): EditParams {
            val calm = file in CALMER_PHOTOS
            return look(filterId).copy(
                filterIntensity = if (calm) 0.75f else 1f,
                saturation = if (calm) -0.25f else 0f,
                exposure = PHOTO_EXPOSURE[file] ?: 0f,
            )
        }
    }
}
