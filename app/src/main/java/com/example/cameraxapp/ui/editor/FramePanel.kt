package com.example.cameraxapp.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cameraxapp.core.render.FrameSpec
import com.example.cameraxapp.core.render.PolaroidMode
import com.example.cameraxapp.core.render.PolaroidPlacement
import com.example.cameraxapp.data.model.FrameThemes
import com.example.cameraxapp.ui.theme.Spacing
import com.example.cameraxapp.ui.view.DeviceImagePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Frame choices: none, Polaroid (fit or fill), a theme, or a photo background.
 * While a Polaroid is selected, [PolaroidControls] sets its style, border colour and caption.
 * The date chip applies to theme and background frames only.
 *
 * [onFrameLive] receives changes that should update the preview without an undo step (typing a
 * caption). [onCommit] runs when that typing is finished. Both default so existing callers still
 * compile.
 */
@Composable
fun FramePanel(
    frame: FrameSpec,
    onFrame: (FrameSpec) -> Unit,
    modifier: Modifier = Modifier,
    onFrameLive: (FrameSpec) -> Unit = onFrame,
    onCommit: () -> Unit = {},
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.lg)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            EditorChip(
                label = "None",
                selected = frame.polaroid == null && frame.themeId == null && frame.backgroundUri == null,
                onClick = { onFrame(FrameSpec()) },
            )
            PolaroidMode.values().forEach { mode ->
                EditorChip(
                    label = if (mode == PolaroidMode.FIT) "Polaroid fit" else "Polaroid fill",
                    selected = frame.polaroid == mode,
                    onClick = {
                        if (frame.polaroid != mode) {
                            // Switching between fit and fill keeps style, colour and caption.
                            // Pan and zoom are reset because they only apply to FILL.
                            onFrame(
                                if (frame.polaroid == null) {
                                    FrameSpec(polaroid = mode)
                                } else {
                                    frame.copy(polaroid = mode, placement = PolaroidPlacement())
                                },
                            )
                        }
                    },
                )
            }
            EditorChip(
                label = "Photo",
                selected = frame.backgroundUri != null,
                onClick = { showPicker = true },
            )
        }

        if (frame.polaroid != null) {
            PolaroidControls(
                frame = frame,
                onFrame = onFrame,
                onFrameLive = onFrameLive,
                onCommit = onCommit,
            )
        }

        if (frame.polaroid == null) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                EditorChip(
                    label = "Date",
                    selected = frame.caption != null,
                    onClick = {
                        onFrame(frame.copy(caption = if (frame.caption == null) todayText() else null))
                    },
                )
            }
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(FrameThemes.ALL_THEMES, key = { it.id }) { theme ->
                    EditorChip(
                        label = theme.name,
                        selected = frame.themeId == theme.id,
                        onClick = { onFrame(FrameSpec(themeId = theme.id, caption = frame.caption)) },
                    )
                }
            }
        }
    }

    if (showPicker) {
        DeviceImagePicker(
            onImageSelected = { uri ->
                onFrame(FrameSpec(backgroundUri = uri.toString(), caption = frame.caption))
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

private fun todayText(): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
