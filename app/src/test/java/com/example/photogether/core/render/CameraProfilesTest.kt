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
        // 12 hand-written cameras (Leica M, Ricoh GR, Sunlit, Cinema and Recipes included) plus 41 from the Japan pack.
        assertEquals(53, CameraProfiles.ALL.size)
    }

    @Test
    fun recipesCameraOffersEverySavedRecipeAndNoCatalogLook() {
        val recipes = CameraProfiles.byId("recipes")
        assertTrue(recipes.showsRecipes)
        assertTrue(recipes.offers("recipe:7"))
        assertTrue(!recipes.offers("kodak_portra"))
        assertEquals("recipes", CameraProfiles.forFilter("recipe:7")?.id)
        assertEquals(FilterCatalog.ORIGINAL_ID, recipes.filterId)
        assertEquals(listOf(FilterCatalog.ORIGINAL_ID), CameraProfiles.stripIds(recipes))
    }

    @Test
    fun sunlitCameraHoldsTheMeadowRuinsAlpineCyanSunTealSeaStreetGreenParkGoldDuskGoldAndSurfTealLooks() {
        assertEquals(
            listOf("matte_meadow", "sunlit_ruins", "clear_alpine", "cyan_sun", "turquoise_cream", "teal_sea", "street_green", "park_gold", "dusk_gold", "surf_teal"),
            CameraProfiles.byId("sunlit").filterIds,
        )
        assertEquals("sunlit", CameraProfiles.forFilter("clear_alpine")?.id)
        assertEquals("sunlit", CameraProfiles.forFilter("cyan_sun")?.id)
        assertEquals("sunlit", CameraProfiles.forFilter("teal_sea")?.id)
        assertEquals("sunlit", CameraProfiles.forFilter("street_green")?.id)
        assertEquals("sunlit", CameraProfiles.forFilter("park_gold")?.id)
        assertEquals("sunlit", CameraProfiles.forFilter("dusk_gold")?.id)
        assertEquals("sunlit", CameraProfiles.forFilter("surf_teal")?.id)
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
