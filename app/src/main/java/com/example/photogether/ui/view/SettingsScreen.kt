package com.example.photogether.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tgound.photogether.BuildConfig
import com.tgound.photogether.R
import com.example.photogether.core.navigation.AppRoutes
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.photogether.ui.components.DefaultAppBar
import com.example.photogether.ui.components.GlassBox
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.PhotogetherTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.photogether.core.locale.AppLanguage
import com.example.photogether.core.locale.AppLocale
import com.example.photogether.ui.onboarding.LanguageCard
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import com.example.photogether.ui.theme.AppIcons

@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val context = LocalContext.current
    val shutterSoundOn by viewModel.shutterSoundEnabled.collectAsStateWithLifecycle()
    val cropZoomOn by viewModel.cropZoomEnabled.collectAsStateWithLifecycle()
    val comingSoon = stringResource(R.string.settings_coming_soon)
    val soon = { Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show() }
    SettingsContent(
        onBack = { navController.navigateUp() },
        shutterSoundOn = shutterSoundOn,
        onShutterSoundChange = viewModel::setShutterSoundEnabled,
        cropZoomOn = cropZoomOn,
        onCropZoomChange = viewModel::setCropZoomEnabled,
        versionName = BuildConfig.VERSION_NAME,
        onReplayTutorial = { navController.navigate(AppRoutes.ONBOARDING) },
        onTerms = soon,
        onPrivacy = soon,
        onLicenses = soon,
        onPhotoCredits = { navController.navigate(AppRoutes.PHOTO_CREDITS) },
        onFeedback = soon,
        onRateApp = soon,
    )
}

/** Stateless settings body. The app uses a single dark theme, so the appearance row is informational. */
@Composable
fun SettingsContent(
    onBack: () -> Unit,
    shutterSoundOn: Boolean = true,
    onShutterSoundChange: (Boolean) -> Unit = {},
    cropZoomOn: Boolean = false,
    onCropZoomChange: (Boolean) -> Unit = {},
    versionName: String = "1.0",
    onReplayTutorial: () -> Unit = {},
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onLicenses: () -> Unit = {},
    onPhotoCredits: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onRateApp: () -> Unit = {},
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val language = AppLocale.current()
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            confirmButton = {},
            containerColor = AppColors.background,
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    AppLanguage.entries.forEach { option ->
                        LanguageCard(
                            language = option,
                            selected = option == language,
                            onClick = {
                                showLanguageDialog = false
                                AppLocale.apply(option)
                            },
                        )
                    }
                }
            },
        )
    }
    Scaffold(
        topBar = { DefaultAppBar(onBack = onBack, title = stringResource(R.string.settings_title)) },
        containerColor = AppColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg)
        ) {
            SectionTitle(stringResource(R.string.settings_section_appearance))

            SettingsCard {
                Icon(
                    imageVector = AppIcons.DarkMode,
                    contentDescription = null,
                    tint = AppColors.accent,
                    modifier = Modifier.padding(end = Spacing.md)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_dark_theme),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.settings_dark_theme_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SettingsCard {
                Text(
                    text = stringResource(R.string.settings_shutter_sound),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = shutterSoundOn, onCheckedChange = onShutterSoundChange)
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SettingsCard {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_crop_zoom),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.settings_crop_zoom_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary,
                    )
                }
                Switch(checked = cropZoomOn, onCheckedChange = onCropZoomChange)
            }

            SectionTitle(stringResource(R.string.settings_section_general), topPadding = Spacing.xl)
            SettingsEntry(
                title = stringResource(R.string.settings_language),
                value = language.nativeName,
                onClick = { showLanguageDialog = true },
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            SettingsEntry(
                title = stringResource(R.string.settings_replay_onboarding),
                onClick = onReplayTutorial,
            )

            SectionTitle(stringResource(R.string.settings_section_about), topPadding = Spacing.xl)
            SettingsEntry(title = stringResource(R.string.settings_version), value = versionName)
            Spacer(modifier = Modifier.height(Spacing.md))
            SettingsEntry(title = stringResource(R.string.settings_terms), onClick = onTerms)
            Spacer(modifier = Modifier.height(Spacing.md))
            SettingsEntry(title = stringResource(R.string.settings_privacy), onClick = onPrivacy)
            Spacer(modifier = Modifier.height(Spacing.md))
            SettingsEntry(title = stringResource(R.string.settings_licenses), onClick = onLicenses)
            Spacer(modifier = Modifier.height(Spacing.md))
            SettingsEntry(title = stringResource(R.string.settings_photo_credits), onClick = onPhotoCredits)
            Spacer(modifier = Modifier.height(Spacing.md))
            SettingsEntry(title = stringResource(R.string.settings_feedback), onClick = onFeedback)
            Spacer(modifier = Modifier.height(Spacing.md))
            SettingsEntry(title = stringResource(R.string.settings_rate_app), onClick = onRateApp)
        }
    }
}

@Composable
private fun SectionTitle(text: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = AppColors.textPrimary,
        modifier = Modifier.padding(top = topPadding, bottom = Spacing.md)
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    GlassBox(modifier = modifier.fillMaxWidth(), cornerRadius = Radius.lg) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** A tappable row (chevron) when [onClick] is set, otherwise a static label/value row. */
@Composable
private fun SettingsEntry(title: String, value: String? = null, onClick: (() -> Unit)? = null) {
    SettingsCard(modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = AppColors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PhotogetherTheme {
        SettingsContent(onBack = {})
    }
}
