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

/** Longest side of the copy used for the colour palette. Small is enough for a handful of colours. */
private const val PALETTE_SIDE = 96

private val BUTTON_HEIGHT = 52.dp
private val SWATCH_SIZE = 32.dp
private val PALETTE_WIDTH = 96.dp

/**
 * Detail of one camera shot, shown as a dialog over the camera gallery: when it was taken, its colours,
 * the settings saved with it, and the delete and edit actions. A setting that was not saved is left out.
 */
@Composable
fun ShotDetailDialog(
    uri: String,
    takenAt: Long,
    width: Int,
    height: Int,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    // Null while the photo is being read, empty when it could not be read.
    val palette by produceState<List<Int>?>(initialValue = null, uri) {
        value = extractPalette(context, uri)
    }
    // Null while the EXIF is being read, and when the shot has no saved settings.
    val settings by produceState<ShotSettings?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { ShotSettingsStore.read(context, uri) }
    }
    val takenAtLabel = remember(takenAt) {
        SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(takenAt))
    }
    val shape = RoundedCornerShape(Radius.sm)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
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
                IconButton(onClick = onDismiss) {
                    Icon(AppIcons.Close, contentDescription = stringResource(R.string.action_close), tint = AppColors.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            val ratio = if (width > 0 && height > 0) width.toFloat() / height else 3f / 4f
            // The photo and its colours share one row: the colours run down beside the photo.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(ratio)
                        .clip(shape)
                        .border(Stroke.border, AppColors.outline, shape),
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
                DetailButton(
                    label = stringResource(R.string.detail_delete),
                    onClick = onDelete,
                    filled = false,
                    danger = true,
                    modifier = Modifier.weight(1f),
                )
                DetailButton(
                    label = stringResource(R.string.detail_edit),
                    onClick = onEdit,
                    filled = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The saved settings as label and value pairs, in display order. Settings that were not saved are left out. */
private fun settingRows(settings: ShotSettings?): List<Pair<String, String>> = buildList {
    settings?.lutName?.let { add("LUT" to it) }
    settings?.filterIntensity?.let { add("CƯỜNG ĐỘ" to "${(it * 100).roundToInt()}%") }
    settings?.ev?.let { add("EV" to "${formatEv(it)} EV") }
    settings?.kelvin?.let { add("ĐỘ K" to "${it}K") }
    settings?.focalMm?.let { add("TIÊU CỰ" to "$it mm") }
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
