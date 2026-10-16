package com.example.cameraxapp.core.render

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
        assertEquals(catalog.map { it.id }.toSet(), filters.toSet())
    }

    @Test
    fun hasFiveProfiles() {
        assertEquals(5, CameraProfiles.ALL.size)
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
    fun forFilterMapsKodakToClassic35() {
        assertEquals("classic35", CameraProfiles.forFilter("kodak")?.id)
    }

    @Test
    fun forFilterReturnsNullForUnknownFilter() {
        assertNull(CameraProfiles.forFilter("no_such_filter"))
        assertNull(CameraProfiles.forFilter(null))
    }
}
