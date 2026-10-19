package com.example.photogether.core.colors

import com.example.photogether.core.render.CameraProfiles
import com.example.photogether.core.render.FilterCatalog
import java.io.File
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorStoriesTest {

    private val assetsDir = File("src/main/assets")

    private val filters by lazy {
        FilterCatalog.parse(File(assetsDir, "filters/catalog.json").readText()).associateBy { it.id }
    }

    @Test
    fun sevenStoriesWithUniqueIdsAndCodes() {
        assertEquals(7, ColorStories.ALL.size)
        assertEquals(7, ColorStories.ALL.map { it.id }.toSet().size)
        assertEquals(7, ColorStories.ALL.map { it.code }.toSet().size)
    }

    @Test
    fun everyStoryUsesAnExistingFreeFilter() {
        ColorStories.ALL.forEach { story ->
            val filter = filters[story.filterId]
            assertNotNull("${story.id}: unknown filter ${story.filterId}", filter)
            assertTrue("${story.id}: filter is premium", !filter!!.premium)
        }
    }

    @Test
    fun everyStoryHasDistinctBundledPhotos() {
        ColorStories.ALL.forEach { story ->
            assertEquals("${story.id}: photo count", ColorStories.PHOTOS_PER_STORY, story.photos.size)
            assertEquals("${story.id}: duplicate photos", story.photos.size, story.photos.toSet().size)
            story.photos.forEach { file ->
                assertTrue("${story.id}: missing $file", File(assetsDir, "landmarks/$file").isFile)
            }
        }
    }

    @Test
    fun everyBundledSampleHasCredit() {
        val credited = JsonParser.parseString(File(assetsDir, "landmarks/credits.json").readText())
            .asJsonObject.getAsJsonArray("photos")
            .map { it.asJsonObject.get("file").asString }
            .toSet()
        ColorStories.ALL.flatMap { it.photos }.forEach { file ->
            assertTrue("$file has no credit", file in credited)
        }
    }

    @Test
    fun everyStoryFilterBelongsToACameraBody() {
        // "Try now" opens the body that owns the story's filter, so the camera keeps that filter.
        ColorStories.ALL.forEach { story ->
            val camera = CameraProfiles.forFilter(story.filterId)
            assertNotNull("${story.id}: no camera body offers ${story.filterId}", camera)
        }
    }
}
