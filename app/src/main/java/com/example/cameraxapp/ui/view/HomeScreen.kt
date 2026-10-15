package com.example.cameraxapp.ui.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cameraxapp.core.navigation.AppRoutes
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.Motion
import com.example.cameraxapp.ui.theme.PhotogetherTheme
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.theme.Stroke
import com.example.cameraxapp.ui.viewmodel.GalleryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    galleryViewModel: GalleryViewModel = koinViewModel()
) {
    val photoBooths by galleryViewModel.photoBooths.collectAsState()
    val recentImages = remember(photoBooths) {
        photoBooths
            .sortedByDescending { it.createdAt }
            .mapNotNull { it.imagePaths.firstOrNull() }
            .take(RECENT_COUNT)
    }

    // Pick an image from the device and open it straight in the editor.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { navController.navigate(AppRoutes.imageDetail(it.toString())) }
    }

    HomeContent(
        recentImages = recentImages,
        onPhotoBoothClick = { navController.navigate(AppRoutes.FRAME_SELECTION) },
        onEditClick = { navController.navigate(AppRoutes.GALLERY) },
        onPickImageClick = { imagePickerLauncher.launch("image/*") },
        onSettingsClick = { navController.navigate(AppRoutes.SETTINGS) },
        onRecentClick = { path ->
            navController.navigate(AppRoutes.imageDetail(java.net.URLEncoder.encode(path, "UTF-8")))
        }
    )
}

private const val RECENT_COUNT = 10

@Composable
fun HomeContent(
    recentImages: List<String>,
    onPhotoBoothClick: () -> Unit,
    onEditClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecentClick: (String) -> Unit
) {
    Scaffold(containerColor = AppColors.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Photogether",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppColors.textPrimary
                    )
                    Text(
                        text = "Chụp và chỉnh ảnh của bạn",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppColors.textSecondary
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Cài đặt",
                        tint = AppColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            HomeCard(
                icon = Icons.Default.CameraAlt,
                title = "Photobooth",
                description = "Chụp ảnh thành dải, lưới hoặc GIF",
                onClick = onPhotoBoothClick,
                modifier = Modifier.fillMaxWidth().height(HERO_CARD_HEIGHT)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            HomeCard(
                icon = Icons.Default.Edit,
                title = "Edit photo",
                description = "Chỉnh sửa bộ lọc cho ảnh đã lưu",
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth().height(HERO_CARD_HEIGHT)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            HomeCard(
                icon = Icons.Default.PhotoLibrary,
                title = "Chỉnh ảnh từ máy",
                description = "Mở một ảnh trong thiết bị để chỉnh sửa",
                onClick = onPickImageClick,
                modifier = Modifier.fillMaxWidth().height(COMPACT_CARD_HEIGHT)
            )

            if (recentImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xl))
                Text(
                    text = "Gần đây",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(end = Spacing.lg)
                ) {
                    items(recentImages) { path ->
                        RecentThumbnail(path = path, onClick = { onRecentClick(path) })
                    }
                }
            }
        }
    }
}

private val HERO_CARD_HEIGHT = 168.dp
private val COMPACT_CARD_HEIGHT = 120.dp

/**
 * Large primary card. Scales to 0.97 on press with the default spring and gives a haptic tick
 * when tapped.
 */
@Composable
private fun HomeCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.PressedScale else 1f,
        animationSpec = Motion.DefaultSpring,
        label = "home_card_scale"
    )
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(Radius.lg)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(AppColors.surface)
            .border(Stroke.hairline, AppColors.outline, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(Spacing.xl)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(IconSize.xl)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(AppColors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.accent,
                    modifier = Modifier.size(IconSize.md)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun RecentThumbnail(path: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(RECENT_SIZE)
            .clip(RoundedCornerShape(Radius.sm))
            .background(AppColors.surfaceElevated)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = path,
            contentDescription = "Ảnh gần đây",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

private val RECENT_SIZE = 112.dp

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PhotogetherTheme {
        HomeContent(
            recentImages = emptyList(),
            onPhotoBoothClick = {},
            onEditClick = {},
            onPickImageClick = {},
            onSettingsClick = {},
            onRecentClick = {}
        )
    }
}
