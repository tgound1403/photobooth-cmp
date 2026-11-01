package com.example.photogether.ui.view.GaleryScreen

import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.photogether.core.camera.formatEv
import com.example.photogether.core.moments.ShotPalette
import com.example.photogether.core.moments.ShotSettings
import com.example.photogether.service.ShotSettingsStore
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.photogether.ui.theme.AppIcons
import com.example.photogether.ui.components.photoOpenLayer
import com.example.photogether.ui.components.rememberPhotoOpenState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.photogether.ui.components.ZoomablePhotoDialog
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/** Longest side of the copy used for the colour palette. Small is enough for a handful of colours. */
private const val PALETTE_SIDE = 96

private val BUTTON_HEIGHT = 52.dp
private val SWATCH_SIZE = 32.dp
private val PALETTE_WIDTH = 96.dp

/** What the detail dialog needs to show one photo. */
data class DetailShot(val uri: String, val takenAt: Long, val width: Int, val height: Int)

/** Single-photo form of [ShotDetailDialog]. */
@Composable
fun ShotDetailDialog(
    uri: String,
    takenAt: Long,
    width: Int,
    height: Int,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null,
) = ShotDetailDialog(
    shots = listOf(DetailShot(uri, takenAt, width, height)),
    initialIndex = 0,
    onDismiss = onDismiss,
    onEdit = { onEdit() },
    onDelete = onDelete?.let { delete -> { _: Int -> delete() } },
)

/**
 * Detail of camera shots, shown as a dialog: swipe left or right to move between [shots], starting at [initialIndex].
 * Each page shows when the shot was taken, its colours, the settings saved with it, and the delete and edit actions
 * (given the page's index). A setting that was not saved is left out. Tapping the photo opens it full screen with zoom.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShotDetailDialog(
    shots: List<DetailShot>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: ((Int) -> Unit)? = null,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, (shots.size - 1).coerceAtLeast(0))) { shots.size }
    var zoomUri by remember { mutableStateOf<String?>(null) }
    // Opens like every other photo: slides up from the bottom and bends as it settles.
    val openState = rememberPhotoOpenState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { openState.open() }
    val dismiss: () -> Unit = {
        scope.launch {
            openState.close()
            onDismiss()
        }
    }

    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().photoOpenLayer(openState),
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            pageSpacing = Spacing.md,
            verticalAlignment = Alignment.CenterVertically,
            key = { shots[it].uri },
        ) { page ->
            val shot = shots[page]
            DetailCard(
                shot = shot,
                position = "${page + 1} / ${shots.size}",
                onClose = dismiss,
                onZoom = { zoomUri = shot.uri },
                onEdit = { onEdit(page) },
                onDelete = onDelete?.let { delete -> { delete(page) } },
            )
        }
    }

    zoomUri?.let { ZoomablePhotoDialog(uri = it, onDismiss = { zoomUri = null }) }
}

@Composable
private fun DetailCard(
    shot: DetailShot,
    position: String,
    onClose: () -> Unit,
    onZoom: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    // Null while the photo is being read, empty when it could not be read.
    val palette by produceState<List<Int>?>(initialValue = null, shot.uri) {
        value = extractPalette(context, shot.uri)
    }
    // Null while the EXIF is being read, and when the shot has no saved settings.
    val settings by produceState<ShotSettings?>(initialValue = null, shot.uri) {
        value = withContext(Dispatchers.IO) { ShotSettingsStore.read(context, shot.uri) }
    }
    val takenAtLabel = remember(shot.takenAt) {
        SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(shot.takenAt))
    }
    val shape = RoundedCornerShape(Radius.sm)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.background)
            .border(Stroke.border, AppColors.outline, shape)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.detail_title),
                style = BinsooType.title,
                color = AppColors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(text = position, style = BinsooType.label, color = AppColors.textSecondary)
            IconButton(onClick = onClose) {
                Icon(AppIcons.Close, contentDescription = stringResource(R.string.action_close), tint = AppColors.textPrimary)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        val ratio = if (shot.width > 0 && shot.height > 0) shot.width.toFloat() / shot.height else 3f / 4f
        // The photo and its colours share one row: the colours run down beside the photo.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = shot.uri,
                contentDescription = stringResource(R.string.detail_open_fullscreen),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(ratio)
                    .clip(shape)
                    .border(Stroke.border, AppColors.outline, shape)
                    .clickable(onClick = onZoom),
            )
            PaletteColumn(palette)
        }

        DetailSection(title = stringResource(R.string.detail_capture_time)) {
            Text(text = takenAtLabel, style = BinsooType.title, color = AppColors.textPrimary)
        }

        val rows = settingRows(settings)
        if (rows.isNotEmpty()) {
            DetailSection(title = stringResource(R.string.detail_params)) {
                rows.forEach { (label, value) -> DetailRow(label = label, value = value) }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (onDelete != null) {
                DetailButton(
                    label = stringResource(R.string.detail_delete),
                    onClick = onDelete,
                    filled = false,
                    danger = true,
                    modifier = Modifier.weight(1f),
                )
            }
            DetailButton(
                label = stringResource(R.string.detail_edit),
                onClick = onEdit,
                filled = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The saved settings as label and value pairs, in display order. Settings that were not saved are left out. */
private fun settingRows(settings: ShotSettings?): List<Pair<String, String>> = buildList {
    settings?.lutName?.let { add("LUT" to it) }
    settings?.filterIntensity?.let { add("CƯỜNG ĐỘ" to "${(it * 100).roundToInt()}%") }
    settings?.ev?.let { add("EV" to "${formatEv(it)} EV") }
    settings?.kelvin?.let { add("ĐỘ K" to "${it}K") }
    settings?.tint?.let { add("TINT" to signedPercent(it)) }
    settings?.focalMm?.let { add("TIÊU CỰ" to "$it mm") }
}

/** Tint as a signed percentage, such as "+40" (magenta), "-40" (green) or "0". */
private fun signedPercent(tint: Float): String {
    val n = (tint * 100f).roundToInt()
    return if (n > 0) "+$n" else "$n"
}

/** Titled block: small caps label, then the content. */
@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Spacer(modifier = Modifier.height(Spacing.lg))
    Text(
        text = title,
        style = BinsooType.label,
        fontWeight = FontWeight.Bold,
        color = AppColors.textSecondary,
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    content()
}

/** One key and value line, with a hairline underneath. */
@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label.uppercase(), style = BinsooType.label, color = AppColors.textSecondary)
            Text(
                text = value,
                style = BinsooType.body,
                color = AppColors.textPrimary,
                textAlign = TextAlign.End,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Stroke.hairline)
                .background(AppColors.outline),
        )
    }
}

/** The photo's main colours, one under another. Each swatch has its hex code beside it. */
@Composable
private fun PaletteColumn(palette: List<Int>?) {
    Column(
        modifier = Modifier.width(PALETTE_WIDTH),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        when {
            palette == null -> Text(text = stringResource(R.string.detail_analyzing_colors), style = BinsooType.micro, color = AppColors.textSecondary)
            palette.isEmpty() -> Text(text = stringResource(R.string.detail_colors_failed), style = BinsooType.micro, color = AppColors.textSecondary)
            else -> palette.forEach { argb ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(SWATCH_SIZE)
                            .clip(RoundedCornerShape(Radius.sm))
                            .border(Stroke.border, AppColors.outline, RoundedCornerShape(Radius.sm))
                            .background(Color(argb)),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(text = hex(argb), style = BinsooType.micro, color = AppColors.textSecondary)
                }
            }
        }
    }
}

/** Black-ink button with a 2dp border. [filled] gives it the solid accent; [danger] colours the border and label red. */
@Composable
private fun DetailButton(
    label: String,
    onClick: () -> Unit,
    filled: Boolean,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val shape = RoundedCornerShape(Radius.sm)
    val labelColor = when {
        filled -> AppColors.onAccent
        danger -> AppColors.danger
        else -> AppColors.textPrimary
    }
    Box(
        modifier = modifier
            .height(BUTTON_HEIGHT)
            .clip(shape)
            .background(if (filled) AppColors.accent else AppColors.background)
            .border(Stroke.border, if (danger) AppColors.danger else AppColors.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = BinsooType.label, color = labelColor)
    }
}

/**
 * Reads the photo at a small size and returns its main colours. Runs on IO.
 * Empty when the uri cannot be decoded.
 */
private suspend fun extractPalette(context: Context, uri: String): List<Int> = withContext(Dispatchers.IO) {
    val source = Uri.parse(uri)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext emptyList()

    val options = BitmapFactory.Options().apply {
        inSampleSize = max(1, max(bounds.outWidth, bounds.outHeight) / PALETTE_SIDE)
    }
    val bitmap = context.contentResolver.openInputStream(source)
        ?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return@withContext emptyList()
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    bitmap.recycle()
    ShotPalette.dominantColors(pixels)
}

private fun hex(argb: Int): String = String.format(Locale.US, "#%06X", argb and 0xFFFFFF)
