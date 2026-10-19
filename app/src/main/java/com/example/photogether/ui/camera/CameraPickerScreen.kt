package com.example.photogether.ui.camera

import androidx.compose.ui.platform.LocalContext
import com.tgound.photogether.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.render.CameraChip
import com.example.photogether.core.render.CameraProfile
import com.example.photogether.core.render.CameraProfiles
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.IconSize
import com.example.photogether.ui.theme.InkColors
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Stroke
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.example.photogether.ui.theme.AppIcons

/**
 * Camera picker on the ink (black) palette. Each camera owns a set of filters from the catalog and is shown
 * as the same 3D body ([CameraModelBody]) painted in the camera's colour. The camera at
 * [currentCameraId] is shown first; tapping a thumbnail changes the camera, and dragging the 3D view turns it.
 */
@Composable
fun CameraPickerScreen(
    profiles: List<CameraProfile> = CameraProfiles.ALL,
    currentCameraId: String? = null,
    moments: List<Moment> = emptyList(),
    onConfirm: (CameraProfile) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val initialPage = profiles.indexOfFirst { it.id == currentCameraId }.coerceAtLeast(0)
    var selectedIndex by rememberSaveable { mutableIntStateOf(initialPage) }
    val selected = profiles.getOrNull(selectedIndex)
    // Chip over the 3D view: null shows every camera. The strip below lists only the cameras with the chip.
    var activeChip by rememberSaveable { mutableStateOf<CameraChip?>(null) }
    val visible = remember(profiles, activeChip) { profiles.filter { inChip(it, activeChip) } }
    // Only the shots the selected camera took. Photos without a camera id (edits, photobooth) never match.
    val shots = remember(moments, selected?.id) { selected?.id?.let { id -> moments.filter { it.cameraId == id } }.orEmpty() }
    val spin = remember { PickerSpin() }
    var flingJob by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                // Offscreen so the 3D body's hole (BlendMode.Clear) is cut out of the background too.
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .background(InkColors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = stringResource(R.string.action_back),
                    tint = InkColors.textPrimary,
                )
            }
            Text(
                text = stringResource(R.string.picker_title),
                color = InkColors.textPrimary,
                style = BinsooType.label,
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // One 3D body for every camera. Swiping only changes its colour, so a single engine is
            // alive at a time.
            if (selected != null) {
                CameraModelBody(
                    modelAsset = selected.modelAsset,
                    tint = selected.modelTint,
                    spin = spin,
                    moments = shots,
                    modifier = Modifier.matchParentSize(),
                )
            }

            // Transparent layer on top: dragging turns the body and the photo orbits (fling keeps
            // them turning). Cameras are chosen with the thumbnails below.
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .pointerInput(spin) {
                            val tracker = VelocityTracker()
                            detectDragGestures(
                                onDragStart = {
                                    flingJob?.cancel()
                                    tracker.resetTracking()
                                },
                                onDragEnd = {
                                    val v = tracker.calculateVelocity()
                                    flingJob = scope.launch { spin.fling(v.x, v.y) }
                                },
                                onDragCancel = { flingJob = null },
                            ) { change, drag ->
                                tracker.addPosition(change.uptimeMillis, change.position)
                                spin.drag(drag.x, drag.y)
                                change.consume()
                            }
                        },
            )

        }

        // Model name and description (the camera's look and the scenes it suits, see CameraProfile.subtitle).
        Text(
            text = selected?.name?.uppercase() ?: "",
            color = InkColors.textPrimary,
            style = BinsooType.display.copy(fontSize = 28.sp, lineHeight = 32.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Text(
            text = selected?.subtitleText(LocalContext.current) ?: "",
            color = InkColors.textSecondary,
            style = BinsooType.label,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Filter chips sit right above the thumbnails they filter, so the link between them is clear.
        CameraChipRow(
            active = activeChip,
            onSelect = { chip ->
                // Tapping the active chip again clears it. If the chosen camera is filtered out, pick the first one shown.
                val next = if (chip == activeChip) null else chip
                activeChip = next
                val shown = profiles.filter { inChip(it, next) }
                if (selected == null || selected !in shown) shown.firstOrNull()?.let { selectedIndex = profiles.indexOf(it) }
            },
            modifier = Modifier.padding(bottom = 12.dp),
        )

        CameraThumbnailRow(
            profiles = visible,
            currentPage = selected?.let { visible.indexOf(it) } ?: -1,
            onSelect = { index -> selectedIndex = profiles.indexOf(visible[index]) },
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        ) {
            ConfirmButton(
                enabled = selected != null,
                onClick = { selected?.let(onConfirm) },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/**
 * One 64dp thumbnail per camera with its name underneath. The selected camera has a 2dp white
 * outline and a white label; the others are drawn at alpha 0.5.
 */
@Composable
private fun CameraThumbnailRow(
    profiles: List<CameraProfile>,
    currentPage: Int,
    onSelect: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(Radius.sm)
    val listState = rememberLazyListState()
    // Keep the chosen camera on screen, including when the picker opens on one far down the list.
    LaunchedEffect(currentPage) { if (currentPage >= 0) listState.animateScrollToItem(currentPage) }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        itemsIndexed(profiles, key = { _, profile -> profile.id }) { index, profile ->
            val isSelected = index == currentPage
            Column(
                modifier =
                    Modifier
                        .width(72.dp)
                        .alpha(if (isSelected) 1f else 0.5f)
                        .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val imageRes = cameraImageRes(profile.id)
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(shape)
                            .background(Color(0xFF16161A))
                            .drawBehind { if (imageRes == 0) drawCameraIcon(Color(profile.modelTint)) }
                            .then(
                                if (isSelected) {
                                    Modifier.border(Stroke.border, InkColors.textPrimary, shape)
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    // Still of the camera body, same model as the 3D picker. Falls back to the drawn icon.
                    if (imageRes != 0) {
                        Image(
                            painter = painterResource(imageRes),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = profile.name.uppercase(),
                    color = InkColors.textPrimary,
                    style = BinsooType.label,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Whether [profile] is listed under [chip]. A null chip lists every camera. */
private fun inChip(profile: CameraProfile, chip: CameraChip?): Boolean = chip == null || chip in profile.chips

/**
 * Row of pill chips over the 3D view. "Tất cả" (null) shows every camera; the active chip is filled white.
 * Scrolls sideways so every chip stays reachable on narrow screens.
 */
@Composable
private fun CameraChipRow(
    active: CameraChip?,
    onSelect: (CameraChip?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ChipPill(label = stringResource(R.string.chip_all), selected = active == null, shape = shape, onClick = { onSelect(null) })
        }
        items(CameraChip.entries, key = { it.name }) { chip ->
            ChipPill(label = stringResource(chip.labelRes), selected = active == chip, shape = shape, onClick = { onSelect(chip) })
        }
    }
}

/** One chip: filled white when [selected], otherwise an outlined pill. */
@Composable
private fun ChipPill(
    label: String,
    selected: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(shape)
                .background(if (selected) InkColors.textPrimary else Color(0xCC16161A))
                .then(if (selected) Modifier else Modifier.border(Stroke.border, InkColors.textSecondary, shape))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            color = if (selected) InkColors.onAccent else InkColors.textPrimary,
            style = BinsooType.label,
            maxLines = 1,
        )
    }
}

/** Round white 56dp button with a chevron down. Confirms the camera on the pager. */
@Composable
private fun ConfirmButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(56.dp)
                .alpha(if (enabled) 1f else 0.4f)
                .clip(CircleShape)
                .background(InkColors.textPrimary)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.ChevronDown,
            contentDescription = stringResource(R.string.picker_select_camera),
            tint = InkColors.onAccent,
            modifier = Modifier.size(IconSize.md),
        )
    }
}

/** Small front view of the camera body in [body] colour: top bumps, chrome strip, lens. */
private fun DrawScope.drawCameraIcon(body: Color) {
    val w = size.width
    val h = size.height
    val left = w * 0.12f
    val bodyW = w * 0.76f
    val top = h * 0.32f
    val bodyH = h * 0.46f
    drawRoundRect(body, Offset(w * 0.22f, h * 0.22f), Size(w * 0.18f, h * 0.14f), CornerRadius(w * 0.03f))
    drawRoundRect(Color(0xFFD9DCE0), Offset(w * 0.58f, h * 0.24f), Size(w * 0.2f, h * 0.12f), CornerRadius(w * 0.03f))
    drawRoundRect(body, Offset(left, top), Size(bodyW, bodyH), CornerRadius(w * 0.08f))
    drawRect(Color(0xFFD9DCE0), Offset(left, top + bodyH * 0.12f), Size(bodyW, bodyH * 0.1f))
    val c = Offset(w * 0.5f, top + bodyH * 0.58f)
    drawCircle(Color(0xFF24242A), w * 0.19f, c)
    drawCircle(Color(0xFF0B0B0E), w * 0.14f, c)
    drawCircle(Color(0x55FFFFFF), w * 0.04f, Offset(c.x - w * 0.05f, c.y - w * 0.05f))
}
