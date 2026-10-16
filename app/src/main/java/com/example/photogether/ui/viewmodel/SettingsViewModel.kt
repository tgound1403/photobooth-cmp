package com.example.photogether.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.photogether.core.camera.ShutterSoundPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Settings screen state. The shutter sound value is saved through [ShutterSoundPrefs], which the camera reads. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _shutterSoundEnabled = MutableStateFlow(ShutterSoundPrefs.read(application))

    /** Whether the shutter sound plays on capture. */
    val shutterSoundEnabled: StateFlow<Boolean> = _shutterSoundEnabled.asStateFlow()

    /** Saves the shutter sound setting. The camera re-reads it when its screen resumes. */
    fun setShutterSoundEnabled(enabled: Boolean) {
        _shutterSoundEnabled.value = enabled
        ShutterSoundPrefs.write(getApplication(), enabled)
    }
}
