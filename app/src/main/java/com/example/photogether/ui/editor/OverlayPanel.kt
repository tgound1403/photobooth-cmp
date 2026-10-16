package com.example.photogether.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.photogether.core.render.OverlayCatalog
import com.example.photogether.core.render.OverlayItem
import com.example.photogether.core.render.OverlayKind
import com.example.photogether.core.render.OverlaySpec
import com.example.photogether.ui.theme.Spacing

/**
 * Light leak, scratch and distortion presets, plus stickers and text placed at the centre.
 * Tapping an item removes it.
 */
@Composable
fun OverlayPanel(
    overlays: OverlaySpec,
    onOverlays: (OverlaySpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        PresetRow("Light leak", OverlayCatalog.lightLeaks, overlays.lightLeak) { id ->
            onOverlays(overlays.copy(lightLeak = id))
        }
        PresetRow("Scratch", OverlayCatalog.scratches, overlays.scratch) { id ->
            onOverlays(overlays.copy(scratch = id))
        }
        PresetRow("Distortion", OverlayCatalog.distortions, overlays.distortion) { id ->
            onOverlays(overlays.copy(distortion = id))
        }

        SectionLabel("Stickers")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OverlayCatalog.stickers.forEach { emoji ->
                EditorChip(
                    label = emoji,
                    selected = false,
                    onClick = {
                        onOverlays(overlays.copy(items = overlays.items + OverlayItem(kind = OverlayKind.STICKER, content = emoji)))
                    },
                )
            }
        }

        SectionLabel("Text")
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Type text") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    val content = text.trim()
                    if (content.isNotEmpty()) {
                        onOverlays(overlays.copy(items = overlays.items + OverlayItem(kind = OverlayKind.TEXT, content = content)))
                        text = ""
                    }
                },
                modifier = Modifier.padding(start = Spacing.sm),
            ) {
                Text("Add")
            }
        }

        if (overlays.items.isNotEmpty()) {
            SectionLabel("Items (tap to remove)")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                overlays.items.forEach { item ->
                    EditorChip(
                        label = item.content,
                        selected = item.kind == OverlayKind.TEXT,
                        onClick = { onOverlays(overlays.copy(items = overlays.items.filterNot { it.id == item.id })) },
                    )
                }
            }
        }
    }
}

/** One row of presets; tapping the active preset turns it off. */
@Composable
private fun PresetRow(
    title: String,
    presets: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    SectionLabel(title)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        EditorChip(label = "None", selected = selected == null, onClick = { onSelect(null) })
        presets.forEach { preset ->
            EditorChip(
                label = preset,
                selected = selected == preset,
                onClick = { onSelect(if (selected == preset) null else preset) },
            )
        }
    }
}
