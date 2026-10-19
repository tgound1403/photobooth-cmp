package com.example.photogether

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.photogether.core.navigation.AppRoot
import com.example.photogether.core.perf.JankLogger
import com.example.photogether.ui.theme.PhotogetherTheme

// AppCompatActivity (not ComponentActivity) so AppCompatDelegate.setApplicationLocales works below API 33.
class MainActivity : AppCompatActivity() {
    private var jankLogger: JankLogger? = null

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
            PhotogetherTheme {
                AppRoot()
            }
        }
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
