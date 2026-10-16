package com.example.cameraxapp.ui.moments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.cameraxapp.core.moments.Moment
import com.example.cameraxapp.core.moments.MomentQuery
import com.example.cameraxapp.ui.theme.AppColors
import com.example.cameraxapp.ui.theme.BinsooType
import com.example.cameraxapp.ui.theme.InkColors
import com.example.cameraxapp.ui.theme.IconSize
import com.example.cameraxapp.ui.theme.Radius
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.theme.Stroke
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bottom pill height plus margin. The grid keeps its last row clear of the pill. */
private val PILL_CLEARANCE = 96.dp
private val PILL_SHAPE = RoundedCornerShape(28.dp)

/**
 * Grid of every image the app made, on the paper background. The read permission is requested on entry.
 * If it is denied, the images the system still lets us see are shown with a short note.
 * The bottom pill: latest photo (opens the viewer), add-frame and favourites (not wired yet, disabled).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MomentsScreen(
    viewModel: MomentsViewModel,
    onBack: () -> Unit,
    onOpen: (index: Int) -> Unit,
) {
    val context = LocalContext.current
    val moments by viewModel.moments.collectAsState()
    val permission = readImagesPermission()
    var granted by remember { mutableStateOf(hasPermission(context, permission)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        if (result) viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        if (granted) {
            viewModel.refresh()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = AppColors.textPrimary,
                    )
                }
                Text(
                    text = MomentQuery.countLabel(moments.size),
                    style = BinsooType.title,
                    color = AppColors.textPrimary,
                )
            }

            if (!granted) {
                Text(
                    text = "Chưa cấp quyền đọc ảnh nên chỉ hiện những ảnh hệ thống cho phép xem.",
                    style = BinsooType.body,
                    color = AppColors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }

            if (moments.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Chưa có ảnh nào",
                        style = BinsooType.title,
                        color = AppColors.textPrimary,
                    )
                    Text(
                        text = "Ảnh chụp, ảnh đã chỉnh và ảnh photobooth sẽ hiện ở đây.",
                        style = BinsooType.body,
                        color = AppColors.textSecondary,
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = Spacing.lg,
                        end = Spacing.lg,
                        top = Spacing.sm,
                        bottom = PILL_CLEARANCE,
                    ),
                    verticalItemSpacing = Spacing.md,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(moments, key = { _, moment -> moment.uri }) { index, moment ->
                        MomentTile(moment = moment, onClick = { onOpen(index) })
                    }
                }
            }
        }

        MomentsBottomPill(
            latest = moments.firstOrNull(),
            onOpenLatest = { onOpen(0) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = Spacing.lg),
        )
    }
}

/**
 * One grid tile: 2dp ink border, a cream card with "+" marks in the top corners,
 * the photo at its real aspect ratio (clamped so tiles stay sane), and a micro metadata line.
 */
@Composable
private fun MomentTile(moment: Moment, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.sm)
    val ratio = if (moment.width > 0 && moment.height > 0) {
        (moment.width.toFloat() / moment.height.toFloat()).coerceIn(0.6f, 1.8f)
    } else {
        1f
    }
    val meta = remember(moment.takenAt) { metadataLine(moment.takenAt) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(Stroke.border, AppColors.outline, shape)
            .padding(Stroke.border)
            .background(AppColors.surface)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio),
            ) {
                AsyncImage(
                    model = moment.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = meta,
                style = BinsooType.micro,
                color = AppColors.textSecondary,
                maxLines = 1,
            )
        }
        CornerMark(modifier = Modifier.align(Alignment.TopStart))
        CornerMark(modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** A small "+" crosshair mark in a card corner. */
@Composable
private fun CornerMark(modifier: Modifier = Modifier) {
    Text(
        text = "+",
        style = BinsooType.title,
        color = AppColors.textPrimary,
        modifier = modifier.padding(Spacing.xs),
    )
}

/** Floating dark pill with three icons. Only the latest-photo icon is wired to a callback. */
@Composable
private fun MomentsBottomPill(
    latest: Moment?,
    onOpenLatest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(PILL_SHAPE)
            .background(InkColors.surface)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        // Latest photo: opens the viewer at index 0 (the list is sorted newest first).
        Box(
            modifier = Modifier
                .size(PILL_THUMB_SIZE)
                .clip(RoundedCornerShape(Radius.sm))
                .background(InkColors.surfaceElevated)
                .clickable(enabled = latest != null, onClick = onOpenLatest),
        ) {
            if (latest != null) {
                AsyncImage(
                    model = latest.uri,
                    contentDescription = "Ảnh mới nhất",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Add frame: no callback is wired from AppNavigation yet. Shown disabled.
        PillIcon(
            icon = Icons.Default.AddPhotoAlternate,
            contentDescription = "Thêm khung",
            enabled = false,
            onClick = {},
        )
        // Favourites: no callback is wired from AppNavigation yet. Shown disabled.
        PillIcon(
            icon = Icons.Default.StarBorder,
            contentDescription = "Yêu thích",
            enabled = false,
            onClick = {},
        )
    }
}

@Composable
private fun PillIcon(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(IconSize.xl)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) InkColors.textPrimary else InkColors.textSecondary,
            modifier = Modifier.size(IconSize.sm),
        )
    }
}

private val PILL_THUMB_SIZE = 36.dp

/** "SHOT ON <device>  DATE <dd/MM/yy>". The device is this phone's model, not the camera that took the photo. */
private fun metadataLine(takenAt: Long): String {
    val date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(takenAt))
    return "SHOT ON ${Build.MODEL.uppercase()}  DATE $date"
}

/** READ_MEDIA_IMAGES from API 33, READ_EXTERNAL_STORAGE up to API 32. */
private fun readImagesPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
