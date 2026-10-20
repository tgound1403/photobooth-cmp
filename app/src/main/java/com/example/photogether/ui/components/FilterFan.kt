package com.example.photogether.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.example.photogether.core.camera.Click
import com.example.photogether.core.camera.SliderTickPlayer
import com.example.photogether.core.render.FanLayoutMath
import com.example.photogether.core.render.FilterInfo
import com.example.photogether.ui.theme.AppIcons
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors
import kotlin.math.roundToInt

/** Fan geometry, in dp: outer radius, gap between rings, chip size, centre-to-centre spacing on a ring, hit radius, window margin. */
private const val FAN_RADIUS_DP = 140f
private const val FAN_RING_GAP_DP = 46f
private const val FAN_CHIP_DP = 36f
private const val FAN_SPACING_DP = 40f
private const val FAN_HIT_DP = 40f
private const val FAN_MARGIN_DP = 10f
private const val FAN_LABEL_RESERVE_DP = 34f
private val FAN_CHIP_SIZE = FAN_CHIP_DP.dp

/** What the fan overlay draws: where the fan was placed and the entry under the finger (index into the filters). */
data class FanUiState(val placement: FanLayoutMath.FanPlacement, val highlighted: Int?)

/**
 * Press-and-hold filter picker. After the long press the fan opens ([onFan] gets a [FanUiState], and null when
 * it closes). The fan is placed inside the window by [FanLayoutMath.place], opening towards [preferredMidDeg]
 * when it fits (180 is left, 270 is up). Dragging over an entry selects it at once through [onSelect], so the
 * live view follows the finger. Lifting the finger keeps the last selection.
 *
 * With [pillName] null the trigger is the small icon-over-label glyph of the landscape plate; otherwise it is a
 * wider pill carrying that name (portrait).
 */
@Composable
fun FilterFanButton(
    filters: List<FilterInfo>,
    onSelect: (String) -> Unit,
    onFan: (FanUiState?) -> Unit,
    modifier: Modifier = Modifier,
    preferredMidDeg: Float = 180f,
    pillName: String? = null,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val currentFilters by rememberUpdatedState(filters)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentFan by rememberUpdatedState(onFan)
    val currentMid by rememberUpdatedState(preferredMidDeg)
    var rootTopLeft by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = modifier
            .semantics { contentDescription = "Bộ lọc: giữ rồi kéo để chọn" }
            .onGloballyPositioned { c ->
                rootTopLeft = c.positionInRoot()
                boxSize = c.size
            }
            .pointerInput(Unit) {
                val hitPx = FAN_HIT_DP.dp.toPx()
                var placement: FanLayoutMath.FanPlacement? = null
                var current: Int? = null
                fun update(pos: Offset) {
                    val pl = placement ?: return
                    val root = rootTopLeft + pos
                    val hit = FanLayoutMath.nearestIndex(pl.points, root.x, root.y, hitPx) ?: return
                    if (hit != current) {
                        current = hit
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SliderTickPlayer.play(context, Click.WHEEL)
                        currentFilters.getOrNull(hit)?.let { currentSelect(it.id) }
                        currentFan(FanUiState(pl, hit))
                    }
                }
                detectDragGesturesAfterLongPress(
                    onDragStart = { pos ->
                        val pl = FanLayoutMath.place(
                            count = currentFilters.size,
                            anchorX = rootTopLeft.x + boxSize.width / 2f,
                            anchorY = rootTopLeft.y + boxSize.height / 2f,
                            radius = FAN_RADIUS_DP.dp.toPx(),
                            ringGap = FAN_RING_GAP_DP.dp.toPx(),
                            chipSize = FAN_CHIP_DP.dp.toPx(),
                            spacing = FAN_SPACING_DP.dp.toPx(),
                            width = view.width.toFloat(),
                            height = view.height.toFloat(),
                            margin = FAN_MARGIN_DP.dp.toPx(),
                            topReserve = FAN_LABEL_RESERVE_DP.dp.toPx(),
                            preferredMidDeg = currentMid,
                        )
                        placement = pl
                        current = null
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentFan(FanUiState(pl, null))
                        update(pos)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        update(change.position)
                    },
                    onDragEnd = { placement = null; currentFan(null) },
                    onDragCancel = { placement = null; currentFan(null) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (pillName == null) {
            Column(
                modifier = Modifier.width(52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(InkColors.surfaceElevated.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Filter, contentDescription = null, tint = InkColors.textPrimary, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "FILTER",
                    style = BinsooType.label.copy(fontSize = 9.sp, lineHeight = 12.sp),
                    color = InkColors.textPrimary,
                    maxLines = 1,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .widthIn(min = 180.dp, max = 260.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(InkColors.surfaceElevated.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Icon(AppIcons.Filter, contentDescription = null, tint = InkColors.textPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = pillName.uppercase(),
                    style = BinsooType.label,
                    color = InkColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The fan itself, drawn over the whole camera screen: a light scrim, a short code on each entry and, just
 * above the fan, one readable label with the full name of the highlighted entry (the selected one before the
 * finger reaches any). Composed only while the fan is open. It takes no touches; the button owns the gesture.
 */
@Composable
fun FilterFanOverlay(
    state: FanUiState,
    filters: List<FilterInfo>,
    selectedId: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .background(Color.Black.copy(alpha = 0.35f)),
    ) {
        val half = with(density) { FAN_CHIP_SIZE.toPx() / 2f }
        val points = state.placement.points
        filters.forEachIndexed { i, filter ->
            val p = points.getOrNull(i) ?: return@forEachIndexed
            val on = i == state.highlighted
            val active = filter.id == selectedId
            Box(
                modifier = Modifier
                    .offset { IntOffset((p.x - origin.x - half).roundToInt(), (p.y - origin.y - half).roundToInt()) }
                    .size(FAN_CHIP_SIZE)
                    .clip(CircleShape)
                    .background(if (on) InkColors.accent else InkColors.surfaceElevated.copy(alpha = 0.92f))
                    .border(1.dp, if (active) InkColors.accent else Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = FanLayoutMath.shortCode(filter.name),
                    style = BinsooType.label.copy(fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.sp),
                    color = if (on) InkColors.onAccent else InkColors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        val shown = state.highlighted?.let { filters.getOrNull(it) } ?: filters.firstOrNull { it.id == selectedId }
        if (shown != null) {
            val box = state.placement.box
            val centreX = box.centerX - origin.x
            val topY = box.top - origin.y
            Box(
                modifier = Modifier.layout { measurable, constraints ->
                    val margin = FAN_MARGIN_DP.dp.roundToPx()
                    val p = measurable.measure(Constraints(maxWidth = (constraints.maxWidth - 2 * margin).coerceAtLeast(0)))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val x = (centreX - p.width / 2f).roundToInt().coerceIn(margin, (constraints.maxWidth - margin - p.width).coerceAtLeast(margin))
                        val y = (topY - p.height - 6.dp.roundToPx()).roundToInt().coerceAtLeast(margin)
                        p.place(x, y)
                    }
                },
            ) {
                Text(
                    text = shown.name,
                    style = BinsooType.label.copy(fontSize = 14.sp, lineHeight = 18.sp),
                    color = InkColors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
        }
    }
}
