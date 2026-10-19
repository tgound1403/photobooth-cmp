package com.example.photogether.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photogether.core.locale.AppLanguage
import com.example.photogether.core.locale.AppLocale
import com.example.photogether.ui.components.GlassButton
import com.example.photogether.ui.onboarding.anim.AppLogo
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Motion
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import com.tgound.photogether.R

/** Screen 1: pick the app language. Tapping a card switches the whole UI straight away. */
@Composable
fun LanguageStep(onContinue: () -> Unit) {
    var selected by remember { mutableStateOf(AppLocale.current()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        AppLogo(size = 104.dp)
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = "PHOTOGETHER",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 36.sp),
            color = AppColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.onboarding_lang_title),
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.onboarding_lang_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            AppLanguage.entries.forEach { language ->
                LanguageCard(
                    language = language,
                    selected = language == selected,
                    onClick = {
                        selected = language
                        AppLocale.apply(language)
                    },
                )
            }
        }
        Spacer(Modifier.weight(1.2f))
        GlassButton(
            onClick = onContinue,
            text = stringResource(R.string.onboarding_continue),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Language row styled like the app's cards: ink border, code badge, yellow tag when chosen. */
@Composable
fun LanguageCard(language: AppLanguage, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tilt by animateFloatAsState(if (selected) -1.2f else 0f, Motion.DefaultSpring, label = "tilt")
    val scale by animateFloatAsState(if (selected) 1.02f else 1f, Motion.DefaultSpring, label = "scale")
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = tilt; scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.md),
        color = AppColors.surface,
        border = BorderStroke(if (selected) Stroke.selected else Stroke.border, AppColors.outline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.lg),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(AppColors.accent)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            ) {
                Text(language.code, style = MaterialTheme.typography.labelLarge, color = AppColors.onAccent)
            }
            Spacer(Modifier.padding(start = Spacing.lg))
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(AppColors.highlight)
                        .padding(horizontal = Spacing.sm, vertical = 2.dp),
                ) {
                    Text("✓", style = MaterialTheme.typography.labelLarge, color = AppColors.textPrimary)
                }
            }
        }
    }
}
