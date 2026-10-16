package com.example.cameraxapp.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.mutablePreferencesOf
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CameraSettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newRepo(): CameraSettingsRepository =
        CameraSettingsRepository(
            PreferenceDataStoreFactory.create {
                File(tmp.root, "camera_settings.preferences_pb")
            }
        )

    @Test
    fun defaultsWhenNothingStored() = runBlocking {
        val repo = newRepo()

        assertEquals(CameraSettings(), repo.settings.first())
    }

    @Test
    fun kelvinIsStored() = runBlocking {
        val repo = newRepo()

        repo.setKelvin(3200)

        assertEquals(3200, repo.settings.first().kelvin)
    }

    @Test
    fun settingKelvinToNullClearsIt() = runBlocking {
        val repo = newRepo()
        repo.setKelvin(3200)

        repo.setKelvin(null)

        assertNull(repo.settings.first().kelvin)
    }

    @Test
    fun evIsStoredIndependentlyOfKelvin() = runBlocking {
        val repo = newRepo()

        repo.setKelvin(6500)
        repo.setEv(-0.7f)

        val settings = repo.settings.first()
        assertEquals(6500, settings.kelvin)
        assertEquals(-0.7f, settings.ev, 1e-6f)
    }

    @Test
    fun emptyPreferencesMapToDefaults() {
        assertEquals(CameraSettings(), mutablePreferencesOf().toCameraSettings())
    }
}
