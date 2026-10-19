package com.example.photogether.ui.view

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.core.colors.ColorStories
import com.example.photogether.core.colors.ColorStory
import com.example.photogether.data.colors.ColorPhoto
import com.example.photogether.ui.colors.ColorStoriesViewModel
import com.example.photogether.ui.moments.CrystalFolder
import com.example.photogether.ui.moments.MomentsViewModel
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.PhotogetherTheme
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import com.example.photogether.ui.viewmodel.GalleryViewModel
import org.koin.androidx.compose.koinViewModel
import com.example.photogether.ui.theme.AppIcons

@Composable
fun HomeScreen(
    navController: NavController,
    galleryViewModel: GalleryViewModel = koinViewModel(),
    momentsViewModel: MomentsViewModel = koinViewModel(),
    colorStoriesViewModel: ColorStoriesViewModel = koinViewModel(),
) {
    val photoBooths by galleryViewModel.photoBooths.collectAsStateWithLifecycle()
    val recentImages = remember(photoBooths) {
        photoBooths
            .sortedByDescending { it.createdAt }
            .mapNotNull { it.imagePaths.firstOrNull() }
            .take(RECENT_COUNT)
    }
    val moments by momentsViewModel.moments.collectAsStateWithLifecycle()
    val covers by colorStoriesViewModel.covers.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { colorStoriesViewModel.loadCovers() }

    // Pick an image from the device and open it straight in the editor.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { navController.navigate(AppRoutes.imageDetail(it.toString())) }
    }

    HomeContent(
        recentImages = recentImages,
        onPhotoBoothClick = { navController.navigate(AppRoutes.FRAME_SELECTION) },
        onEditClick = { navController.navigate(AppRoutes.PHOTO_PICKER) },
        onPickImageClick = { imagePickerLauncher.launch("image/*") },
        onSettingsClick = { navController.navigate(AppRoutes.SETTINGS) },
        onRecentClick = { path ->
            navController.navigate(AppRoutes.imageDetail(java.net.URLEncoder.encode(path, "UTF-8")))
        },
        onCameraClick = { navController.navigate(AppRoutes.MAIN) },
        moments = moments,
        onMomentsClick = { navController.navigate(AppRoutes.MOMENTS) },
        stories = ColorStories.ALL,
        covers = covers,
        onStoryClick = { navController.navigate(AppRoutes.colorStory(it.id)) },
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
    onRecentClick: (String) -> Unit,
    onCameraClick: () -> Unit = {},
    moments: List<Moment> = emptyList(),
    onMomentsClick: () -> Unit = {},
    stories: List<ColorStory> = emptyList(),
    covers: Map<String, ColorPhoto> = emptyMap(),
    onStoryClick: (ColorStory) -> Unit = {},
) {
    Scaffold(containerColor = AppColors.background) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg)
            ) {
                // Hamburger, top right. Opens Settings.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = AppIcons.Menu,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = AppColors.textPrimary
                        )
                    }
                }

                Text(
                    text = "PHOTO",
                    style = HOME_TITLE_STYLE,
                    color = AppColors.textPrimary
                )
                Text(
                    text = "GETHER",
                    style = HOME_TITLE_STYLE,
                    color = AppColors.textPrimary
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "WHAT'S NEW",
                        style = BinsooType.title,
                        color = AppColors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${stories.size} MÀU →",
                        style = BinsooType.micro,
                        color = AppColors.textSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Colour groups: two rows, scrolling sideways.
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height(STORY_GRID_HEIGHT),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    contentPadding = PaddingValues(end = Spacing.lg),
                ) {
                    gridItems(stories, key = { it.id }) { story ->
                        ColorStoryTile(
                            story = story,
                            cover = covers[story.id],
                            onClick = { onStoryClick(story) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    OutlinedBlockButton(
                        label = stringResource(R.string.home_photobooth),
                        onClick = onPhotoBoothClick,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedBlockButton(
                        label = stringResource(R.string.home_edit_photo),
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                CrystalFolder(
                    moments = moments,
                    onClick = onMomentsClick,
                    modifier = Modifier.fillMaxWidth().height(FOLDER_HEIGHT)
                )

                if (recentImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    Text(
                        text = stringResource(R.string.home_recent),
                        style = BinsooType.title,
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

                // Keeps the last row clear of the floating buttons.
                Spacer(modifier = Modifier.height(FLOAT_CLEARANCE))
            }

            // Two floating round buttons: pick an image from the device (left), open the camera (right).
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = Spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                FloatingRoundButton(
                    icon = AppIcons.AddPhoto,
                    contentDescription = stringResource(R.string.home_edit_from_device),
                    onClick = onPickImageClick,
                )
                FloatingRoundButton(
                    icon = AppIcons.Camera,
                    contentDescription = stringResource(R.string.home_camera),
                    onClick = onCameraClick,
                )
            }
        }
    }
}

private val HOME_TITLE_STYLE = BinsooType.display
private val FOLDER_HEIGHT = 176.dp
private val FLOAT_BUTTON_SIZE = 56.dp
private val FLOAT_CLEARANCE = 96.dp
private val GROUP_THUMB_SIZE = 72.dp
private val STORY_TILE_HEIGHT = 124.dp
private val STORY_GRID_HEIGHT = STORY_TILE_HEIGHT * 2 + Spacing.lg

/** Colour group entry: the place's photo graded with its filter, then its name and photo count. */
@Composable
private fun ColorStoryTile(
    story: ColorStory,
    cover: ColorPhoto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbShape = RoundedCornerShape(Radius.sm)
    Column(modifier = modifier.width(GROUP_THUMB_SIZE).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(GROUP_THUMB_SIZE)
                .border(Stroke.border, AppColors.outline, thumbShape)
                .padding(Stroke.border)
                .background(AppColors.surfaceElevated),
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover.path,
                    contentDescription = story.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(AppColors.accent)
                    .padding(horizontal = Spacing.xs, vertical = 2.dp),
            ) {
                Text(
                    text = "VIBE",
                    style = BinsooType.micro,
                    color = AppColors.onAccent,
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = story.name.uppercase(),
            style = BinsooType.label.copy(fontWeight = FontWeight.Bold),
            color = AppColors.textPrimary,
            maxLines = 1,
        )
        Text(
            text = "${ColorStories.PHOTOS_PER_STORY} ẢNH | →",
            style = BinsooType.micro,
            color = AppColors.textSecondary,
        )
    }
}

/** Rectangular button with a 2dp black ink border and a centred label. */
@Composable
private fun OutlinedBlockButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier = modifier
            .height(BLOCK_BUTTON_HEIGHT)
            .border(Stroke.border, AppColors.outline, shape)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = BinsooType.label,
            color = AppColors.textPrimary,
        )
    }
}

private val BLOCK_BUTTON_HEIGHT = 52.dp

/** Round black button with a white icon, over the paper background. */
@Composable
private fun FloatingRoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(FLOAT_BUTTON_SIZE)
            .clip(CircleShape)
            .background(InkColors.background.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = InkColors.textPrimary,
            modifier = Modifier.size(IconSize.md),
        )
    }
}

@Composable
private fun RecentThumbnail(path: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.sm)
    Box(
        modifier = Modifier
            .size(RECENT_SIZE)
            .border(Stroke.border, AppColors.outline, shape)
            .padding(Stroke.border)
            .background(AppColors.surfaceElevated)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = path,
            contentDescription = stringResource(R.string.home_recent_photo),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

private val RECENT_SIZE: Dp = 112.dp

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
            onRecentClick = {},
            stories = ColorStories.ALL,
        )
    }
}
