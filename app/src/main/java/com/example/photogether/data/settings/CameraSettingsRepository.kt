package com.example.photogether.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_KELVIN = intPreferencesKey("wb_kelvin")
private val KEY_EV = floatPreferencesKey("ev")

class CameraSettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<CameraSettings> = store.data.map { it.toCameraSettings() }

    suspend fun setKelvin(kelvin: Int?) {
        store.edit { prefs ->
            if (kelvin == null) {
                prefs.remove(KEY_KELVIN)
            } else {
                prefs[KEY_KELVIN] = kelvin
            }
        }
    }

    suspend fun setEv(ev: Float) {
        store.edit { prefs -> prefs[KEY_EV] = ev }
    }

    companion object {
        fun create(context: Context): CameraSettingsRepository =
            CameraSettingsRepository(
                PreferenceDataStoreFactory.create {
                    context.preferencesDataStoreFile("camera_settings")
                }
            )
    }
}

internal fun Preferences.toCameraSettings(): CameraSettings = CameraSettings(
    kelvin = this[KEY_KELVIN],
    ev = this[KEY_EV] ?: 0f,
)
