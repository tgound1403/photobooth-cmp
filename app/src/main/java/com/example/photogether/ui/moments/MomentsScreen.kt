package com.example.photogether.ui.moments

import com.tgound.photogether.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.moments.MomentQuery
import com.example.photogether.data.moments.DeleteResult
import com.example.photogether.ui.view.GaleryScreen.ShotDetailDialog
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.photogether.ui.theme.AppIcons

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
    onEdit: (uri: String) -> Unit,
) {
    val context = LocalContext.current
    val moments by viewModel.moments.collectAsStateWithLifecycle()
    val permission = readImagesPermission()
    var granted by remember { mutableStateOf(hasPermission(context, permission)) }
    var selected by remember { mutableStateOf<Moment?>(null) }
    // Uris picked in selection mode. Empty means the grid is not in selection mode.
    var selectedUris by remember { mutableStateOf(setOf<String>()) }
    // Uris waiting for the app's own confirm dialog. Only used below API 30; API 30+ asks through the system dialog.
    var confirmUris by remember { mutableStateOf<List<String>?>(null) }
    // A delete asked for by the UI. A new instance each time, so LaunchedEffect runs again.
    var deleteRequest by remember { mutableStateOf<DeleteRequest?>(null) }
    // Uris whose delete is waiting on the system dialog (API 29 and up).
    var pendingConsent by remember { mutableStateOf<List<String>?>(null) }

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

    // Result of the system delete dialog. Confirmed uris are deleted; a cancel keeps the selection.
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val uris = pendingConsent
        pendingConsent = null
        if (uris != null && result.resultCode == Activity.RESULT_OK) {
            deleteRequest = DeleteRequest(uris, afterConsent = true)
        }
    }

    LaunchedEffect(deleteRequest) {
        val request = deleteRequest ?: return@LaunchedEffect
        val result = if (request.afterConsent) {
            viewModel.deleteAfterConsent(request.uris)
        } else {
            viewModel.delete(request.uris)
        }
        when (result) {
            // The grid refreshes itself from MediaStore after a delete.
            DeleteResult.Deleted -> selectedUris = emptySet()
            is DeleteResult.NeedsConfirmation -> {
                pendingConsent = result.pending
                consentLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
            }
            DeleteResult.Failed -> Toast.makeText(context, "Không xóa được ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * The only confirmation the user sees. API 30+ goes straight to the system dialog, which already asks.
     * Older versions ask in the app first, then delete.
     */
    fun requestDelete(uris: List<String>) {
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteRequest = DeleteRequest(uris, afterConsent = false)
        } else {
            confirmUris = uris
        }
    }

    BackHandler(enabled = selectedUris.isNotEmpty()) { selectedUris = emptySet() }

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
            if (selectedUris.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            AppIcons.Back,
                            contentDescription = stringResource(R.string.action_back),
                            tint = AppColors.textPrimary,
                        )
                    }
                    Text(
                        text = MomentQuery.countLabel(moments.size),
                        style = BinsooType.title,
                        color = AppColors.textPrimary,
                    )
                }
            } else {
                SelectionBar(
                    count = selectedUris.size,
                    onClose = { selectedUris = emptySet() },
                    onDelete = { requestDelete(selectedUris.toList()) },
                )
            }

            if (!granted) {
                Text(
                    text = stringResource(R.string.moments_limited_access),
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
                        text = stringResource(R.string.moments_empty_title),
                        style = BinsooType.title,
                        color = AppColors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.moments_empty_body),
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
                    items(moments, key = { moment -> moment.uri }) { moment ->
                        MomentTile(
                            moment = moment,
                            isSelected = moment.uri in selectedUris,
                            onClick = {
                                if (selectedUris.isEmpty()) {
                                    selected = moment
                                } else {
                                    selectedUris = selectedUris.toggle(moment.uri)
                                }
                            },
                            onLongClick = { selectedUris = selectedUris.toggle(moment.uri) },
                        )
                    }
                }
            }
        }

        MomentsBottomPill(
            latest = moments.firstOrNull(),
            onOpenLatest = { selected = moments.firstOrNull() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = Spacing.lg),
        )
    }

    selected?.let { moment ->
        ShotDetailDialog(
            uri = moment.uri,
            takenAt = moment.takenAt,
            width = moment.width,
            height = moment.height,
            onDismiss = { selected = null },
            onEdit = {
                selected = null
                onEdit(moment.uri)
            },
            onDelete = {
                selected = null
                requestDelete(listOf(moment.uri))
            },
        )
    }

    confirmUris?.let { uris ->
        AlertDialog(
            onDismissRequest = { confirmUris = null },
            title = { Text(if (uris.size == 1) "Xóa ảnh này?" else "Xóa ${uris.size} ảnh?") },
            text = { Text(stringResource(R.string.dialog_delete_photo_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmUris = null
                        deleteRequest = DeleteRequest(uris, afterConsent = false)
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUris = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** A delete the UI has asked for. Plain class, so every request is a new key for LaunchedEffect. */
private class DeleteRequest(val uris: List<String>, val afterConsent: Boolean)

private fun Set<String>.toggle(uri: String): Set<String> = if (uri in this) this - uri else this + uri

/** Top bar while selecting: clear the selection on the left, the count, and delete the selected photos on the right. */
@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(AppIcons.Close, contentDescription = stringResource(R.string.moments_deselect), tint = AppColors.textPrimary)
        }
        Text(
            text = "Đã chọn $count",
            style = BinsooType.title,
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(AppIcons.Delete, contentDescription = stringResource(R.string.moments_delete_selected), tint = AppColors.danger)
        }
    }
}

/**
 * One grid tile: 2dp ink border, a cream card with "+" marks in the top corners,
 * the photo at its real aspect ratio (clamped so tiles stay sane), and a micro metadata line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MomentTile(
    moment: Moment,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(AppColors.textPrimary.copy(alpha = 0.25f)),
            )
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = stringResource(R.string.moments_selected),
                tint = AppColors.onAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.sm)
                    .size(IconSize.md)
                    .background(AppColors.accent, CircleShape)
                    .padding(Spacing.xs),
            )
        }
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
                    contentDescription = stringResource(R.string.moments_latest),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // "Thêm khung" and "Yêu thích" are hidden until they have a real action wired from AppNavigation.
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
