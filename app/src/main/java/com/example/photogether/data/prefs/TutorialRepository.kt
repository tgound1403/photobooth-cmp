package com.example.photogether.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.photogether.core.tutorial.TutorialStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_STEP = stringPreferencesKey("tutorial_step")

/**
 * Where the user is in the guided tour. Null means no tour is running: it has not started, or it is finished.
 * Kept in DataStore so a tour in progress survives the app being closed.
 */
class TutorialRepository(private val store: DataStore<Preferences>) {

    val step: Flow<TutorialStep?> = store.data.map { TutorialStep.fromName(it[KEY_STEP]) }

    suspend fun setStep(step: TutorialStep?) {
        store.edit {
            if (step == null) it.remove(KEY_STEP) else it[KEY_STEP] = step.name
        }
    }

    companion object {
        fun create(context: Context): TutorialRepository =
            TutorialRepository(
                PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("tutorial") }
            )
    }
}
