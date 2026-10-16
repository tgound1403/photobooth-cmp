package com.example.photogether.ui.view

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.navigation.AppRoutes
import com.example.photogether.core.render.FilterCatalog
import com.example.photogether.core.render.FilterGrouping
import com.example.photogether.core.render.FilterInfo
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
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    galleryViewModel: GalleryViewModel = koinViewModel(),
    momentsViewModel: MomentsViewModel = koinViewModel(),
    filterCatalog: FilterCatalog = get(),
) {
    val photoBooths by galleryViewModel.photoBooths.collectAsState()
    val recentImages = remember(photoBooths) {
        photoBooths
            .sortedByDescending { it.createdAt }
            .mapNotNull { it.imagePaths.firstOrNull() }
            .take(RECENT_COUNT)
    }
    val moments by momentsViewModel.moments.collectAsState()
    val filterGroups = remember(filterCatalog) { homeFilterGroups(filterCatalog.filters) }

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
        filterGroups = filterGroups,
        onFilterGroupClick = { navController.navigate(AppRoutes.CAMERA_PICKER) },
    )
}

private const val RECENT_COUNT = 10

/** One filter group tile on Home. [count] is the number of filters in the group. */
data class HomeFilterGroup(val name: String, val code: String, val count: Int)

/**
 * Filter groups for Home, in catalog order. Names come from [FilterGrouping.chips] (ALL is dropped);
 * the count is the number of catalog filters in each group.
 */
internal fun homeFilterGroups(filters: List<FilterInfo>): List<HomeFilterGroup> =
    FilterGrouping.chips(filters, hasFavorites = false)
        .filter { it.id != FilterGrouping.ALL }
        .map { chip ->
            HomeFilterGroup(
                name = chip.label,
                code = groupCode(chip.label),
                count = filters.count { it.group == chip.label },
            )
        }

/** Tag codes such as "#CLS". Unverified for the names not listed here. */
private fun groupCode(name: String): String = when (name) {
    "Classic" -> "CLS"
    "Film" -> "FLM"
    "Mood" -> "MOD"
    "Portrait" -> "PRT"
    "B&W" -> "BNW"
    else -> name.filter { it.isLetterOrDigit() }.take(3).uppercase()
}

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
    filterGroups: List<HomeFilterGroup> = emptyList(),
    onFilterGroupClick: () -> Unit = {},
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
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Cài đặt",
                            tint = AppColors.textPrimary
                        )
                    }
                }

                Text(
                    text = "PHOTO",
                    style = BinsooType.display,
                    color = AppColors.textPrimary
                )
                Text(
                    text = "GETHER",
                    style = BinsooType.display,
                    color = AppColors.textPrimary
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                filterGroups.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        pair.forEach { group ->
                            FilterGroupTile(
                                group = group,
                                onClick = onFilterGroupClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    OutlinedBlockButton(
                        label = "PHOTOBOOTH",
                        onClick = onPhotoBoothClick,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedBlockButton(
                        label = "CHỈNH ẢNH",
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
                        text = "Gần đây",
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
                    icon = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Chỉnh ảnh từ máy",
                    onClick = onPickImageClick,
                )
                FloatingRoundButton(
                    icon = Icons.Default.CameraAlt,
                    contentDescription = "Máy ảnh",
                    onClick = onCameraClick,
                )
            }
        }
    }
}

private val FOLDER_HEIGHT = 176.dp
private val FLOAT_BUTTON_SIZE = 56.dp
private val FLOAT_CLEARANCE = 96.dp
private val GROUP_THUMB_SIZE = 64.dp

/** Filter group entry: square thumbnail with a black VIBE label, code tag, name and count. */
@Composable
private fun FilterGroupTile(
    group: HomeFilterGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbShape = RoundedCornerShape(Radius.sm)
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(GROUP_THUMB_SIZE)
                .border(Stroke.border, AppColors.outline, thumbShape)
                .padding(Stroke.border)
                .background(AppColors.surfaceElevated),
        ) {
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
        Box(
            modifier = Modifier
                .background(AppColors.accent)
                .padding(horizontal = Spacing.xs, vertical = 2.dp),
        ) {
            Text(
                text = "#${group.code}",
                style = BinsooType.label,
                fontWeight = FontWeight.Bold,
                color = AppColors.highlight,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = group.name.uppercase(),
            style = BinsooType.body.copy(fontWeight = FontWeight.Bold),
            color = AppColors.textPrimary,
        )
        Text(
            text = "${group.count} MÀU | ↓",
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
            contentDescription = "Ảnh gần đây",
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
            filterGroups = listOf(
                HomeFilterGroup("Classic", "CLS", 7),
                HomeFilterGroup("Film", "FLM", 6),
            ),
        )
    }
}
