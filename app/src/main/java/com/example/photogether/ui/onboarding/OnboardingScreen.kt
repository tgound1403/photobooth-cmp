package com.example.photogether.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Motion
import org.koin.androidx.compose.koinViewModel

/**
 * First-run flow: language, four animated how-to pages, camera permission. With [replay] (from
 * Settings) it shows only the how-to pages and then calls [onFinished].
 */
@Composable
fun OnboardingScreen(
    replay: Boolean,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    LaunchedEffect(replay) { viewModel.begin(replay) }
    val step by viewModel.step.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(AppColors.background).systemBarsPadding()) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(Motion.DurationLong)) { it / 6 } + fadeIn(tween(Motion.DurationLong))) togetherWith
                    (slideOutHorizontally(tween(Motion.DurationLong)) { -it / 6 } + fadeOut(tween(Motion.DurationShort)))
            },
            label = "onboardingStep",
        ) { target ->
            when (target) {
                OnboardingStep.LANGUAGE -> LanguageStep(onContinue = { viewModel.next(onFinished) })
                OnboardingStep.INTRO -> IntroPager(
                    onSkip = { viewModel.skip(onFinished) },
                    onDone = { viewModel.next(onFinished) },
                )
                OnboardingStep.PERMISSION -> {
                    PermissionStep(onDone = { viewModel.next(onFinished) })
                }
                null -> Unit
            }
        }
    }
    if (replay) BackHandler(onBack = onFinished)
}
