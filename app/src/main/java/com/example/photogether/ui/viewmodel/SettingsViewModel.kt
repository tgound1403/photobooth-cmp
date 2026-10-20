package com.example.photogether.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.photogether.core.camera.CropZoomPrefs
import com.example.photogether.core.camera.GridPrefs
import com.example.photogether.core.camera.RoundWheel
import com.example.photogether.core.camera.RoundWheelPrefs
import com.example.photogether.core.camera.ShutterSoundPrefs
import com.example.photogether.data.prefs.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Settings screen state. The shutter sound value is saved through [ShutterSoundPrefs], which the camera reads. */
class SettingsViewModel(
    application: Application,
    private val themeRepository: ThemeRepository,
) : AndroidViewModel(application) {

    /** Whether the paper screens use the dark palette. */
    val darkThemeEnabled: StateFlow<Boolean> = themeRepository.darkTheme

    /** Saves the dark theme setting. Every paper screen follows it at once. */
    fun setDarkThemeEnabled(enabled: Boolean) = themeRepository.setDarkTheme(enabled)

    private val _shutterSoundEnabled = MutableStateFlow(ShutterSoundPrefs.read(application))

    /** Whether the shutter sound plays on capture. */
    val shutterSoundEnabled: StateFlow<Boolean> = _shutterSoundEnabled.asStateFlow()

    /** Saves the shutter sound setting. The camera re-reads it when its screen resumes. */
    fun setShutterSoundEnabled(enabled: Boolean) {
        _shutterSoundEnabled.value = enabled
        ShutterSoundPrefs.write(getApplication(), enabled)
    }

    private val _cropZoomEnabled = MutableStateFlow(CropZoomPrefs.read(application))

    /** Whether zoom shows a crop frame instead of magnifying the preview. */
    val cropZoomEnabled: StateFlow<Boolean> = _cropZoomEnabled.asStateFlow()

    /** Saves the crop-frame zoom setting. The camera re-reads it when its screen resumes. */
    fun setCropZoomEnabled(enabled: Boolean) {
        _cropZoomEnabled.value = enabled
        CropZoomPrefs.write(getApplication(), enabled)
    }

    private val _gridEnabled = MutableStateFlow(GridPrefs.read(application))

    /** Whether the rule-of-thirds grid shows over the viewfinder. */
    val gridEnabled: StateFlow<Boolean> = _gridEnabled.asStateFlow()

    /** Saves the grid setting. The camera re-reads it when its screen resumes. */
    fun setGridEnabled(enabled: Boolean) {
        _gridEnabled.value = enabled
        GridPrefs.write(getApplication(), enabled)
    }

    private val _roundWheel = MutableStateFlow(RoundWheelPrefs.read(application))

    /** Which exposure control the round wheel turns on the landscape camera. */
    val roundWheel: StateFlow<RoundWheel> = _roundWheel.asStateFlow()

    /** Saves the round wheel choice. The camera re-reads it when its screen resumes. */
    fun setRoundWheel(wheel: RoundWheel) {
        _roundWheel.value = wheel
        RoundWheelPrefs.write(getApplication(), wheel)
    }
}
