package com.example.photogether

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.photogether.core.navigation.AppNavigation
import com.example.photogether.core.perf.JankLogger
import com.example.photogether.ui.theme.PhotogetherTheme
import com.example.photogether.ui.view.NoPermissionGrantedScreen

class MainActivity : ComponentActivity() {
    private var jankLogger: JankLogger? = null

    companion object {
        val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent bars with dark icons (paper default), whatever the system day/night mode.
        // PhotogetherTheme and InkTheme then flip the icons per screen.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        jankLogger = JankLogger(window)

        if (allPermissionsGranted()) {
            setContent {
                PhotogetherTheme {
                    AppNavigation()
                }
            }
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
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

    val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && !it.value) permissionGranted = false
        }
        setContent {
            PhotogetherTheme {
                if (!permissionGranted) {
                    NoPermissionGrantedScreen()
                } else AppNavigation()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}
