package com.example.photogether.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_FAVORITE_FILTERS = stringSetPreferencesKey("favorite_filters")

/**
 * Favorite filter ids. Stored in its own DataStore file ("filter_prefs"), separate from camera
 * settings.
 */
class FavoriteFiltersRepository(private val store: DataStore<Preferences>) {

    val favorites: Flow<Set<String>> = store.data.map { it[KEY_FAVORITE_FILTERS] ?: emptySet() }

    /** Adds [id] to the favorites if it is missing, otherwise removes it. */
    suspend fun toggle(id: String) {
        store.edit { prefs ->
            val current = prefs[KEY_FAVORITE_FILTERS] ?: emptySet()
            prefs[KEY_FAVORITE_FILTERS] = if (id in current) current - id else current + id
        }
    }

    companion object {
        fun create(context: Context): FavoriteFiltersRepository =
            FavoriteFiltersRepository(
                PreferenceDataStoreFactory.create {
                    context.preferencesDataStoreFile("filter_prefs")
                }
            )
    }
}
