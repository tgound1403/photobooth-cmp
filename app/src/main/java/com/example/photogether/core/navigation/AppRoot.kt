package com.example.photogether.core.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photogether.core.permissions.AppPermissions
import com.example.photogether.data.prefs.OnboardingStatus
import com.example.photogether.ui.onboarding.OnboardingScreen
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.view.NoPermissionGrantedScreen
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.get

/**
 * Decides what the first frame shows: a blank paper screen while the stored flag loads, the
 * onboarding flow on first run, otherwise the app behind the camera permission gate.
 */
@Composable
fun AppRoot(status: OnboardingStatus = get()) {
    val completed by remember { status.completed.map<Boolean, Boolean?> { it } }
        .collectAsStateWithLifecycle(initialValue = null)

    when (completed) {
        null -> Box(Modifier.fillMaxSize().background(AppColors.background))
        false -> OnboardingScreen(replay = false, onFinished = {})
        true -> PermissionGate { AppNavigation() }
    }
}

/** Asks for the camera permission once; shows the "no permission" screen if it is refused. */
@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(AppPermissions.allGranted(context)) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = AppPermissions.allGranted(context)
        requested = true
    }
    LaunchedEffect(Unit) {
        if (!granted && !requested) launcher.launch(AppPermissions.REQUIRED)
    }
    when {
        granted -> content()
        requested -> NoPermissionGrantedScreen()
        else -> Box(Modifier.fillMaxSize().background(AppColors.background))
    }
}
