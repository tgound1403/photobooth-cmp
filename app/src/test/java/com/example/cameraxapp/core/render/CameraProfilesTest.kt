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
            assertTrue("missing filter ${profile.filterId}", profile.filterId in ids)
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
    fun profileIdsAreUniqueAndFilterIdsAreUnique() {
        val all = CameraProfiles.ALL
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all.size, all.map { it.filterId }.toSet().size)
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
    fun forFilterMapsKodakToClassic35() {
        assertEquals("classic35", CameraProfiles.forFilter("kodak")?.id)
    }

    @Test
    fun forFilterReturnsNullWhenNoProfileUsesFilter() {
        assertNull(CameraProfiles.forFilter("bnw"))
        assertNull(CameraProfiles.forFilter(null))
    }
}
