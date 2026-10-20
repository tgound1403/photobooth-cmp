package com.example.photogether

import android.graphics.Color
import android.view.KeyEvent
import android.os.Bundle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.photogether.core.camera.VolumeKeyCapture
import com.example.photogether.core.navigation.AppRoot
import com.example.photogether.core.perf.JankLogger
import com.example.photogether.data.prefs.ThemeRepository
import com.example.photogether.ui.theme.LocalDarkThemeEnabled
import com.example.photogether.ui.theme.PhotogetherTheme
import org.koin.android.ext.android.inject

// AppCompatActivity (not ComponentActivity) so AppCompatDelegate.setApplicationLocales works below API 33.
class MainActivity : AppCompatActivity() {
    private var jankLogger: JankLogger? = null
    private val themeRepository: ThemeRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent bars with dark icons (paper default), whatever the system day/night mode.
        // PhotogetherTheme and InkTheme then flip the icons per screen.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        jankLogger = JankLogger(window)

        // Onboarding on first run, then the camera permission request; see AppRoot.
        setContent {
            val darkTheme by themeRepository.darkTheme.collectAsState()
            CompositionLocalProvider(LocalDarkThemeEnabled provides darkTheme) {
                PhotogetherTheme {
                    AppRoot()
                }
            }
        }
    }

    // Volume keys take the photo while the camera screen is listening (landscape body); elsewhere they change volume.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        val handler = VolumeKeyCapture.handler
        if (isVolumeKey && handler != null) {
            if (event?.repeatCount == 0) handler()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        jankLogger?.resume()
    }

    override fun onPause() {
        jankLogger?.pause()
        super.onPause()
    }
}
