package com.example.cameraxapp.ui.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.cameraxapp.core.navigation.AppNavigation
import com.example.cameraxapp.ui.components.GlassButton
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.PhotogetherTheme
import com.example.cameraxapp.ui.theme.Spacing

@Composable
fun NoPermissionGrantedScreen() {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(checkCameraPermission(context))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasCameraPermission = checkCameraPermission(context)
    }

    if (hasCameraPermission) {
        AppNavigation()
    } else {
        NoPermissionContent(onOpenSettings = { openAppDetailsSettings(context) })
    }
}

/** Stateless body shown when the camera permission is missing. */
@Composable
fun NoPermissionContent(onOpenSettings: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .padding(Spacing.lg)
    ) {
        Text(
            text = "Ứng dụng cần quyền truy cập camera để chụp ảnh",
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        GlassButton(
            onClick = onOpenSettings,
            text = "Mở cài đặt",
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

fun openAppDetailsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

@Preview(showBackground = true)
@Composable
private fun NoPermissionGrantedPreview() {
    PhotogetherTheme {
        NoPermissionContent(onOpenSettings = {})
    }
}
