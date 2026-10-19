package com.example.photogether.core.colors

import com.example.photogether.core.locale.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorStoryPhotosTest {

    @Test
    fun vietnameseUsesTheStoryLandmarks() {
        ColorStories.ALL.forEach { story ->
            assertEquals(story.photos, ColorStories.photosFor(story, AppLanguage.VIETNAMESE))
        }
    }

    @Test
    fun englishUsesGenericSamplesOnly() {
        ColorStories.ALL.forEach { story ->
            val files = ColorStories.photosFor(story, AppLanguage.ENGLISH)
            assertEquals("${story.id}: count", ColorStories.PHOTOS_PER_STORY, files.size)
            assertEquals("${story.id}: distinct", files.size, files.toSet().size)
            files.forEach { file ->
                assertTrue("$file is not a sample", file in ColorStories.SAMPLE_FILES)
            }
        }
    }

}
