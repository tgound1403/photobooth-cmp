package com.example.cameraxapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxapp.core.navigation.AppNavigation
import com.example.cameraxapp.ui.view.NoPermissionGrantedScreen

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cameraxapp.ui.viewmodel.ThemeViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
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
        super.onCreate(savedInstanceState)

        if (allPermissionsGranted()) {
            setContent {
                val themeViewModel: ThemeViewModel = koinViewModel()
                val currentTheme by themeViewModel.theme.collectAsState()
                
                com.example.cameraxapp.ui.theme.CameraXAppTheme(appTheme = currentTheme) {
                    AppNavigation()
                }
            }
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && !it.value) permissionGranted = false
        }
        setContent {
            val themeViewModel: ThemeViewModel = koinViewModel()
            val currentTheme by themeViewModel.theme.collectAsState()
            
            com.example.cameraxapp.ui.theme.CameraXAppTheme(appTheme = currentTheme) {
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
