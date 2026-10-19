package com.example.photogether.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_COMPLETED_VERSION = intPreferencesKey("onboarding_completed_version")

/** What the rest of the app needs to know about onboarding. Faked in tests. */
interface OnboardingStatus {
    /** True once the user finished a flow at least as new as [OnboardingRepository.CURRENT_VERSION]. */
    val completed: Flow<Boolean>
    suspend fun markCompleted()
}

class OnboardingRepository(private val store: DataStore<Preferences>) : OnboardingStatus {

    override val completed: Flow<Boolean> =
        store.data.map { isCompleted(it[KEY_COMPLETED_VERSION]) }

    override suspend fun markCompleted() {
        store.edit { it[KEY_COMPLETED_VERSION] = CURRENT_VERSION }
    }

    companion object {
        /** Bump to show onboarding again after a release that changes how the camera is used. */
        const val CURRENT_VERSION = 1

        fun isCompleted(storedVersion: Int?): Boolean = (storedVersion ?: 0) >= CURRENT_VERSION

        fun create(context: Context): OnboardingRepository =
            OnboardingRepository(
                PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("onboarding") }
            )
    }
}
