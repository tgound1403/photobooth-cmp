package com.example.photogether.core.render

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraProfilesTest {

    private val catalog by lazy {
        FilterCatalog.parse(File("src/main/assets/filters/catalog.json").readText())
    }

    @Test
    fun everyProfileFilterExistsInCatalog() {
        val ids = catalog.map { it.id }.toSet()
        CameraProfiles.ALL.forEach { profile ->
            profile.filterIds.forEach { assertTrue("missing filter $it", it in ids) }
        }
    }

    @Test
    fun everyProfileFilterIsFree() {
        CameraProfiles.ALL.forEach { profile ->
            val info = catalog.first { it.id == profile.filterId }
            assertEquals("premium ${profile.filterId}", false, info.premium)
        }
    }

    @Test
    fun profileIdsAreUniqueAndEveryFilterBelongsToOneCamera() {
        val all = CameraProfiles.ALL
        assertEquals(all.size, all.map { it.id }.toSet().size)
        val filters = all.flatMap { it.filterIds }
        assertEquals(filters.size, filters.toSet().size)
        assertEquals(catalog.map { it.id }.toSet() - FilterCatalog.ORIGINAL_ID, filters.toSet())
    }

    @Test
    fun originalBelongsToNoCamera() {
        assertNull(CameraProfiles.forFilter(FilterCatalog.ORIGINAL_ID))
    }

    @Test
    fun noCameraShowsOnlyTheOriginal() {
        assertEquals(listOf(FilterCatalog.ORIGINAL_ID), CameraProfiles.stripIds(null))
    }

    @Test
    fun switchingCameraReplacesTheStripInsteadOfAppending() {
        val first = CameraProfiles.ALL.first()
        val second = CameraProfiles.ALL[1]
        val before = CameraProfiles.stripIds(first)
        val after = CameraProfiles.stripIds(second)

        assertEquals(listOf(FilterCatalog.ORIGINAL_ID) + second.filterIds, after)
        assertEquals(listOf(FilterCatalog.ORIGINAL_ID) + first.filterIds, before)
        assertTrue(after.none { it in first.filterIds })
    }

    @Test
    fun allCamerasAreOneListOfProfiles() {
        // 5 hand-written cameras plus 41 from the Japan pack (Japanese Style 002 split into 5).
        assertEquals(46, CameraProfiles.ALL.size)
    }

    @Test
    fun everyProfileHasAShortDescriptionThatFitsTwoLines() {
        CameraProfiles.ALL.forEach { profile ->
            assertTrue("blank description ${profile.id}", profile.subtitle.isNotBlank())
            assertTrue("too long ${profile.id}: ${profile.subtitle.length}", profile.subtitle.length <= 90)
            assertTrue("placeholder ${profile.id}", !profile.subtitle.startsWith("Japan "))
        }
        val subtitles = CameraProfiles.ALL.map { it.subtitle }
        assertEquals(subtitles.size, subtitles.toSet().size)
    }

    @Test
    fun byIdFallsBackToFirstProfile() {
        assertEquals(CameraProfiles.ALL.first(), CameraProfiles.byId("xyz"))
        assertEquals(CameraProfiles.ALL.first(), CameraProfiles.byId(null))
        assertEquals(CameraProfiles.ALL.first(), CameraProfiles.byId(""))
    }

    @Test
    fun byIdFindsEveryProfile() {
        CameraProfiles.ALL.forEach { profile ->
            assertEquals(profile, CameraProfiles.byId(profile.id))
        }
    }

    @Test
    fun everyProfileHasABundledModelAndADistinctTint() {
        CameraProfiles.ALL.forEach { profile ->
            val asset = File("src/main/assets/${profile.modelAsset}")
            assertTrue("missing ${asset.path}", asset.exists())
        }
        val tints = CameraProfiles.ALL.map { it.modelTint }
        assertEquals(tints.size, tints.toSet().size)
    }

    @Test
    fun forFilterMapsKodakPortraToClassic35() {
        assertEquals("classic35", CameraProfiles.forFilter("kodak_portra")?.id)
    }

    @Test
    fun everyChipHasAtLeastOneCamera() {
        CameraChip.entries.forEach { chip ->
            assertTrue("empty chip $chip", CameraProfiles.ALL.any { chip in it.chips })
        }
    }

    @Test
    fun forFilterReturnsNullForUnknownFilter() {
        assertNull(CameraProfiles.forFilter("no_such_filter"))
        assertNull(CameraProfiles.forFilter(null))
    }
}
