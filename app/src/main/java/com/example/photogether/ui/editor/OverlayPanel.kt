package com.example.photogether.ui.editor

import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.photogether.core.render.EditParams
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photogether.core.render.OverlayCatalog
import com.example.photogether.core.render.OverlayItem
import com.example.photogether.core.render.OverlayKind
import com.example.photogether.core.render.OverlaySpec
import com.example.photogether.core.render.ViewfinderSkin
import com.example.photogether.ui.theme.Spacing

/** Top-level groups of the overlay tab. One group is shown at a time so the panel keeps a fixed height. */
private enum class OverlayGroup(val label: String) {
    EFFECTS("Effects"),
    LEAK("Light leak"),
    SCRATCH("Scratch"),
    LENS("Lens"),
    SKIN("Skin"),
    STICKERS("Stickers"),
    TEXT("Text"),
}

private val EFFECT_TOOLS = listOf(
    ParamTool("Grain", 0f..1f, { it.grain }, { p, v -> p.copy(grain = v) }),
    ParamTool("Bloom", 0f..1f, { it.bloom }, { p, v -> p.copy(bloom = v) }),
    ParamTool("Glow", 0f..1f, { it.glow }, { p, v -> p.copy(glow = v) }),
    ParamTool("Dreamy", 0f..1f, { it.dreamy }, { p, v -> p.copy(dreamy = v) }),
    ParamTool("Halation", 0f..1f, { it.halation }, { p, v -> p.copy(halation = v) }),
)

/** Panel height cap, so opening the tab never pushes the photo off the screen. */
private val OVERLAY_PANEL_HEIGHT = 168.dp

/**
 * Overlay tab. A row of group chips (Effects, Light leak, Scratch, Lens, Skin, Stickers, Text) sits on top and the
 * chosen group fills a fixed-height area below, so the preview above keeps its size. Effects holds the strength
 * sliders; the other groups are one row of presets or an add field. Tapping an added item removes it.
 */
@Composable
fun OverlayPanel(
    params: EditParams,
    overlays: OverlaySpec,
    onLive: (ParamsEdit) -> Unit,
    onCommit: () -> Unit,
    onOverlays: (OverlaySpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    var group by rememberSaveable { mutableStateOf(OverlayGroup.EFFECTS) }
    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(OVERLAY_PANEL_HEIGHT)
            .padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OverlayGroup.entries.forEach { g ->
                EditorChip(label = g.label, selected = g == group, onClick = { group = g })
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when (group) {
                OverlayGroup.EFFECTS -> ParamToolStrip(
                    tools = EFFECT_TOOLS,
                    params = params,
                    selectionKey = "overlay_effects",
                    onLive = onLive,
                    onCommit = onCommit,
                    sidePadding = 0.dp,
                )
                OverlayGroup.LEAK -> PresetRow(OverlayCatalog.lightLeaks, overlays.lightLeak, PresetKind.LEAK) { id ->
                    onOverlays(overlays.copy(lightLeak = id))
                }
                OverlayGroup.SCRATCH -> PresetRow(OverlayCatalog.scratches, overlays.scratch, PresetKind.SCRATCH) { id ->
                    onOverlays(overlays.copy(scratch = id))
                }
                OverlayGroup.LENS -> PresetRow(OverlayCatalog.distortions, overlays.distortion, PresetKind.DISTORTION) { id ->
                    onOverlays(overlays.copy(distortion = id))
                }
                OverlayGroup.SKIN -> SkinRow(overlays.skin) { skin -> onOverlays(overlays.copy(skin = skin)) }
                OverlayGroup.STICKERS -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        OverlayCatalog.stickers.forEach { emoji ->
                            StyleTile(label = emoji, selected = false, onClick = {
                                onOverlays(overlays.copy(items = overlays.items + OverlayItem(kind = OverlayKind.STICKER, content = emoji)))
                            }) { ink ->
                                Text(text = emoji, fontSize = 22.sp, color = ink)
                            }
                        }
                    }
                    ItemsRow(overlays, onOverlays, OverlayKind.STICKER)
                }
                OverlayGroup.TEXT -> {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text(stringResource(R.string.overlay_type_text)) },
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
                            Text(stringResource(R.string.overlay_add))
                        }
                    }
                    ItemsRow(overlays, onOverlays, OverlayKind.TEXT)
                }
            }
        }
    }
}

/** The added items of one kind; tapping one removes it. */
@Composable
private fun ItemsRow(overlays: OverlaySpec, onOverlays: (OverlaySpec) -> Unit, kind: OverlayKind) {
    val shown = overlays.items.filter { it.kind == kind }
    if (shown.isEmpty()) return
    SectionLabel("Added (tap to remove)")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        shown.forEach { item ->
            StyleTile(
                label = item.content,
                selected = kind == OverlayKind.TEXT,
                onClick = { onOverlays(overlays.copy(items = overlays.items.filterNot { it.id == item.id })) },
            ) { ink ->
                Text(text = if (kind == OverlayKind.STICKER) item.content else "Aa", fontSize = 20.sp, color = ink)
            }
        }
    }
}

/** Which picture a preset tile shows. */
private enum class PresetKind { NONE, LEAK, SCRATCH, DISTORTION, SKIN }

/** Camera body skins painted over the photo. Tapping the active skin keeps it; "None" turns it off. */
@Composable
private fun SkinRow(selected: ViewfinderSkin, onSelect: (ViewfinderSkin) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ViewfinderSkin.entries.forEach { skin ->
            StyleTile(
                label = skin.label(),
                selected = selected == skin,
                onClick = { onSelect(skin) },
            ) { ink -> PresetGlyph(if (skin == ViewfinderSkin.NONE) PresetKind.NONE else PresetKind.SKIN, ink) }
        }
    }
}

private fun ViewfinderSkin.label(): String = when (this) {
    ViewfinderSkin.NONE -> "None"
    ViewfinderSkin.FILM_FRAME -> "Film frame"
    ViewfinderSkin.RANGEFINDER -> "Rangefinder"
    ViewfinderSkin.RANGEFINDER_MONO -> "Mono rangefinder"
    ViewfinderSkin.SOFT_GLASS -> "Soft glass"
    ViewfinderSkin.LCD_DIGICAM -> "LCD digicam"
}

/** One row of presets; tapping the active preset turns it off. */
@Composable
private fun PresetRow(
    presets: List<String>,
    selected: String?,
    kind: PresetKind,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StyleTile(
            label = stringResource(R.string.option_none),
            selected = selected == null,
            onClick = { onSelect(null) },
        ) { ink -> PresetGlyph(PresetKind.NONE, ink) }
        presets.forEach { preset ->
            StyleTile(
                label = preset,
                selected = selected == preset,
                onClick = { onSelect(if (selected == preset) null else preset) },
            ) { ink -> PresetGlyph(kind, ink) }
        }
    }
}

/** Small picture of an effect category: glow for light leaks, lines for scratches, and so on. */
@Composable
private fun PresetGlyph(kind: PresetKind, ink: Color) {
    Canvas(modifier = Modifier.size(36.dp)) {
        val s = size.minDimension
        val stroke = DrawStroke(1.5f.dp.toPx())
        when (kind) {
            PresetKind.NONE -> {
                drawRect(ink, Offset(s * 0.1f, s * 0.1f), Size(s * 0.8f, s * 0.8f), style = stroke)
                drawLine(ink, Offset(s * 0.1f, s * 0.9f), Offset(s * 0.9f, s * 0.1f), strokeWidth = 1.5f.dp.toPx())
            }
            PresetKind.LEAK -> drawCircle(
                brush = Brush.radialGradient(listOf(ink.copy(alpha = 0.9f), Color.Transparent), center, s / 2f),
                radius = s / 2f,
                center = center,
            )
            PresetKind.SCRATCH -> for (i in 0..3) {
                drawLine(
                    ink,
                    Offset(s * (0.15f + i * 0.2f), s * 0.9f),
                    Offset(s * (0.35f + i * 0.2f), s * 0.1f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            PresetKind.DISTORTION -> drawOval(
                ink,
                topLeft = Offset(s * 0.1f, s * 0.25f),
                size = Size(s * 0.8f, s * 0.5f),
                style = stroke,
            )
            PresetKind.SKIN -> {
                drawRect(ink, Offset(s * 0.1f, s * 0.1f), Size(s * 0.8f, s * 0.8f), style = stroke)
                drawRect(ink, Offset(s * 0.25f, s * 0.25f), Size(s * 0.5f, s * 0.5f), style = stroke)
            }
        }
    }
}
