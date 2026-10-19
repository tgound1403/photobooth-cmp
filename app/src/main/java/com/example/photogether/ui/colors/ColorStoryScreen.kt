package com.example.photogether.ui.colors

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.photogether.core.colors.ColorStory
import com.example.photogether.core.colors.LookDetails
import com.example.photogether.data.colors.ColorPhoto
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import org.koin.androidx.compose.koinViewModel
import com.example.photogether.ui.theme.AppIcons

/** Detail page of a colour group: title, description and its photos graded with the app filter. */
@Composable
fun ColorStoryScreen(
    story: ColorStory,
    onBack: () -> Unit,
    onOpenCredits: () -> Unit,
    onTryNow: () -> Unit,
    viewModel: ColorStoriesViewModel = koinViewModel(),
) {
    LaunchedEffect(story.id) { viewModel.loadDetail(story) }
    val state by viewModel.detail.collectAsStateWithLifecycle()

    Scaffold(containerColor = AppColors.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = stringResource(R.string.action_back),
                    tint = AppColors.textPrimary,
                    modifier = Modifier.size(IconSize.md),
                )
            }

            Box(
                modifier = Modifier
                    .background(AppColors.accent)
                    .padding(horizontal = Spacing.xs, vertical = 2.dp),
            ) {
                Text(
                    text = "#${story.code}",
                    style = BinsooType.label,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.highlight,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = story.name.uppercase(),
                style = BinsooType.display.copy(fontSize = 44.sp, lineHeight = 44.sp),
                color = AppColors.textPrimary,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = story.tagline,
                style = BinsooType.title,
                color = AppColors.textPrimary,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = story.description,
                style = BinsooType.body,
                color = AppColors.textSecondary,
            )
            Spacer(modifier = Modifier.height(Spacing.xl))

            state.photos.forEach { photo ->
                ColorPhotoCard(photo = photo, filterName = state.filterName, name = story.name)
                Spacer(modifier = Modifier.height(Spacing.xl))
            }

            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = AppColors.textPrimary) }
            }
            TryNowButton(onClick = onTryNow)
            Spacer(modifier = Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.story_view_credits),
                style = BinsooType.label,
                color = AppColors.textPrimary,
                modifier = Modifier.clickable(onClick = onOpenCredits),
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun TryNowButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(Stroke.border, AppColors.outline, RoundedCornerShape(Radius.sm))
            .background(AppColors.accent)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.story_try_now),
            style = BinsooType.label,
            fontWeight = FontWeight.Bold,
            color = AppColors.highlight,
        )
    }
}

@Composable
private fun ColorPhotoCard(photo: ColorPhoto, filterName: String, name: String) {
    val uriHandler = LocalUriHandler.current
    val details = LookDetails.lines(photo.recipe)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PHOTO_ASPECT)
                .border(Stroke.border, AppColors.outline, RoundedCornerShape(Radius.sm))
                .padding(Stroke.border)
                .background(AppColors.surfaceElevated),
        ) {
            AsyncImage(
                model = photo.path,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(AppColors.outline.copy(alpha = 0.85f))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            ) {
                Text(
                    text = filterName.uppercase(),
                    style = BinsooType.label,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.surfaceElevated,
                )
                if (details.isEmpty()) {
                    Text(
                        text = stringResource(R.string.story_keep_filter),
                        style = BinsooType.micro,
                        color = AppColors.surfaceElevated,
                    )
                } else {
                    details.forEach { line ->
                        Text(text = line, style = BinsooType.micro, color = AppColors.surfaceElevated)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = stringResource(R.string.story_photo_label), style = BinsooType.micro, color = AppColors.textSecondary)
            Text(
                text = photo.photographer,
                style = BinsooType.micro,
                color = AppColors.textPrimary,
                modifier = Modifier.clickable { runCatching { uriHandler.openUri(photo.photographerUrl) } },
            )
            Text(text = "/", style = BinsooType.micro, color = AppColors.textSecondary)
            Text(
                text = "UNSPLASH",
                style = BinsooType.micro,
                color = AppColors.textPrimary,
                modifier = Modifier.clickable { runCatching { uriHandler.openUri(photo.photoUrl) } },
            )
        }
    }
}

private const val PHOTO_ASPECT = 4f / 5f
