package com.example.photogether.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.photogether.core.permissions.AppPermissions
import com.example.photogether.ui.components.GlassButton
import com.example.photogether.ui.components.SecondaryButton
import com.example.photogether.ui.onboarding.anim.CameraLensIllustration
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.view.openAppDetailsSettings
import com.tgound.photogether.R

/** Last screen: explain why the camera is needed, then ask. The lens iris opens once it is granted. */
@Composable
fun PermissionStep(onDone: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(AppPermissions.allGranted(context)) }
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = AppPermissions.allGranted(context)
        denied = !granted
    }
    // Coming back from the system settings screen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = AppPermissions.allGranted(context) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        CameraLensIllustration(granted = granted)
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(
                if (granted) R.string.onboarding_permission_done_title else R.string.onboarding_permission_title
            ).uppercase(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp, lineHeight = 30.sp),
            color = AppColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(
                if (granted) R.string.onboarding_permission_done_desc else R.string.onboarding_permission_desc
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (denied && !granted) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.onboarding_permission_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.danger,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            when {
                granted -> GlassButton(
                    onClick = onDone,
                    text = stringResource(R.string.onboarding_start),
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> {
                    GlassButton(
                        onClick = { launcher.launch(AppPermissions.REQUIRED) },
                        text = stringResource(R.string.onboarding_permission_allow),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (denied) {
                        SecondaryButton(
                            onClick = { openAppDetailsSettings(context) },
                            text = stringResource(R.string.permission_open_settings),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
