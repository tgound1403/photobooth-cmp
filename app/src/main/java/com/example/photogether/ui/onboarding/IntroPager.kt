package com.example.photogether.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photogether.ui.components.GlassButton
import com.example.photogether.ui.components.SecondaryButton
import com.example.photogether.ui.onboarding.anim.CameraPickIllustration
import com.example.photogether.ui.onboarding.anim.FilmLookIllustration
import com.example.photogether.ui.onboarding.anim.FocusZoomIllustration
import com.example.photogether.ui.onboarding.anim.ShootIllustration
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import com.tgound.photogether.R
import kotlinx.coroutines.launch

/** Screens 2 to 5: four animated how-to pages with a skip button and film-frame page dots. */
@Composable
fun IntroPager(onSkip: () -> Unit, onDone: () -> Unit) {
    val pager = rememberPagerState { INTRO_PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage

    BackHandler(enabled = page > 0) { scope.launch { pager.animateScrollToPage(page - 1) } }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.textSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            IntroPage(index = index, active = pager.currentPage == index)
        }
        FilmDots(count = INTRO_PAGE_COUNT, current = page)
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (page > 0) {
                SecondaryButton(
                    onClick = { scope.launch { pager.animateScrollToPage(page - 1) } },
                    text = stringResource(R.string.onboarding_back),
                    modifier = Modifier.weight(1f),
                )
            }
            GlassButton(
                onClick = {
                    val target = OnboardingFlow.nextPage(page)
                    if (target == null) onDone() else scope.launch { pager.animateScrollToPage(target) }
                },
                text = stringResource(if (page == INTRO_PAGE_COUNT - 1) R.string.onboarding_continue else R.string.onboarding_next),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun IntroPage(index: Int, active: Boolean) {
    val (title, body) = when (index) {
        0 -> R.string.onboarding_cam_title to R.string.onboarding_cam_desc
        1 -> R.string.onboarding_focus_title to R.string.onboarding_focus_desc
        2 -> R.string.onboarding_look_title to R.string.onboarding_look_desc
        else -> R.string.onboarding_shoot_title to R.string.onboarding_shoot_desc
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.widthIn(max = 420.dp), contentAlignment = Alignment.Center) {
            when (index) {
                0 -> CameraPickIllustration(active)
                1 -> FocusZoomIllustration(
                    active,
                    tapLabel = stringResource(R.string.onboarding_tag_tap),
                    pinchLabel = stringResource(R.string.onboarding_tag_pinch),
                )
                2 -> FilmLookIllustration(
                    active,
                    dragLabel = stringResource(R.string.onboarding_tag_drag),
                    stripLabel = stringResource(R.string.onboarding_tag_filters),
                )
                else -> ShootIllustration(
                    active,
                    shootLabel = stringResource(R.string.onboarding_tag_shoot),
                    galleryLabel = stringResource(R.string.onboarding_tag_gallery),
                    editLabel = stringResource(R.string.onboarding_tag_edit),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(title).uppercase(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp, lineHeight = 30.sp),
            color = AppColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}

/** Page indicator drawn as small film frames; the current one is filled yellow. */
@Composable
private fun FilmDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(width = 18.dp, height = 12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i == current) AppColors.highlight else AppColors.surface)
                    .border(BorderStroke(Stroke.border, AppColors.outline), RoundedCornerShape(2.dp)),
            )
        }
    }
}
