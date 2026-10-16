package com.example.photogether.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.photogether.core.render.CaptionPlacement
import com.example.photogether.core.render.FrameColors
import com.example.photogether.core.render.FrameSpec
import com.example.photogether.core.render.PolaroidStyle
import com.example.photogether.core.render.PolaroidStyles
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import kotlin.math.min

/** Which control group the left-hand buttons show. */
private enum class PolaroidSection { BORDER, TEXT }

/** Longest caption the field accepts. */
private const val MAX_CAPTION_LENGTH = 24

/**
 * Controls shown while a Polaroid frame is selected. BORDER shows the frame style and border
 * colour; TEXT shows the caption field. [onFrameLive] is for changes that should update the
 * preview without an undo step (typing); [onCommit] runs once the caption edit is finished.
 */
@Composable
fun PolaroidControls(
    frame: FrameSpec,
    onFrame: (FrameSpec) -> Unit,
    onFrameLive: (FrameSpec) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var section by remember { mutableStateOf(PolaroidSection.BORDER) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            EditorChip(
                label = "BORDER",
                selected = section == PolaroidSection.BORDER,
                onClick = { section = PolaroidSection.BORDER },
            )
            EditorChip(
                label = "TEXT",
                selected = section == PolaroidSection.TEXT,
                onClick = { section = PolaroidSection.TEXT },
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when (section) {
                PolaroidSection.BORDER -> {
                    PolaroidStyleRow(
                        selectedId = frame.polaroidStyleId,
                        borderColor = frame.borderColor,
                        onSelect = { id -> onFrame(frame.copy(polaroidStyleId = id)) },
                    )
                    BorderColorDots(
                        selected = frame.borderColor,
                        onSelect = { color -> onFrame(frame.copy(borderColor = color)) },
                    )
                }
                PolaroidSection.TEXT -> {
                    CaptionField(
                        value = frame.caption.orEmpty(),
                        onValueChange = { text ->
                            onFrameLive(frame.copy(caption = text.ifBlank { null }))
                        },
                        onDone = onCommit,
                    )
                }
            }
        }
    }
}

/**
 * Row of Polaroid style thumbnails. Each thumbnail is drawn in the style's proportions with the
 * current border colour. The selected style gets the thick accent outline.
 */
@Composable
fun PolaroidStyleRow(
    selectedId: String,
    borderColor: Int,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedStyleId = PolaroidStyles.byId(selectedId).id
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(PolaroidStyles.ALL, key = { it.id }) { style ->
            val selected = style.id == selectedStyleId
            val shape = RoundedCornerShape(Radius.sm)
            Column(
                modifier = Modifier
                    .clip(shape)
                    .border(
                        width = if (selected) Stroke.selected else Stroke.hairline,
                        color = if (selected) AppColors.accent else AppColors.outline,
                        shape = shape,
                    )
                    .clickable { onSelect(style.id) }
                    .padding(Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FrameThumbnail(
                    style = style,
                    borderColor = borderColor,
                    modifier = Modifier.size(width = 44.dp, height = 56.dp),
                )
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) AppColors.accent else AppColors.textPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Small Polaroid drawing: grey photo window and a caption bar in the readable ink colour. */
@Composable
private fun FrameThumbnail(
    style: PolaroidStyle,
    borderColor: Int,
    modifier: Modifier = Modifier,
) {
    val captionInk = Color(FrameColors.textColorOn(borderColor))
    Canvas(modifier = modifier) {
        // Work in units of the photo window side, then scale to fit the box.
        val unitW = 1f + 2f * style.sideRatio
        val unitH = style.topRatio + 1f + style.bottomRatio
        val scale = min(size.width / unitW, size.height / unitH)
        val frameW = unitW * scale
        val frameH = unitH * scale
        val frameLeft = (size.width - frameW) / 2f
        val frameTop = (size.height - frameH) / 2f

        drawRect(
            color = Color(borderColor),
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameW, frameH),
        )

        val photoLeft = frameLeft + style.sideRatio * scale
        val photoTop = frameTop + style.topRatio * scale
        drawRect(
            color = Color(0xFF8A8A8F),
            topLeft = Offset(photoLeft, photoTop),
            size = Size(scale, scale),
        )

        val captionAtBottom = style.captionPlacement == CaptionPlacement.BOTTOM
        val stripTop = if (captionAtBottom) photoTop + scale else frameTop
        val stripHeight = if (captionAtBottom) frameTop + frameH - stripTop else photoTop - frameTop
        val barHeight = stripHeight * 0.35f
        val barWidth = frameW * 0.55f
        drawRect(
            color = captionInk,
            topLeft = Offset(
                frameLeft + (frameW - barWidth) / 2f,
                stripTop + (stripHeight - barHeight) / 2f,
            ),
            size = Size(barWidth, barHeight),
        )
    }
}

/** Row of the palette colours for the Polaroid border. The selected one gets a thick outline. */
@Composable
fun BorderColorDots(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FrameColors.PALETTE.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (isSelected) Stroke.selected else Stroke.hairline,
                        color = if (isSelected) AppColors.accent else AppColors.outline,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(color) },
            )
        }
    }
}

/**
 * One-line caption field. The value is cut to [MAX_CAPTION_LENGTH] characters. [onDone] runs when
 * the user presses Done or leaves the field, but only if the text was edited since focus came in.
 */
@Composable
fun CaptionField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var edited by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val finish: () -> Unit = {
        if (edited) {
            edited = false
            onDone()
        }
    }

    OutlinedTextField(
        value = value.take(MAX_CAPTION_LENGTH),
        onValueChange = { text ->
            edited = true
            onValueChange(text.take(MAX_CAPTION_LENGTH))
        },
        placeholder = {
            Text("Thêm chữ, để trống thì in ngày", color = AppColors.textSecondary)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                finish()
                focusManager.clearFocus()
            },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColors.textPrimary,
            unfocusedTextColor = AppColors.textPrimary,
            focusedBorderColor = AppColors.accent,
            unfocusedBorderColor = AppColors.outline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (hadFocus && !state.isFocused) finish()
                hadFocus = state.isFocused
            },
    )
}
