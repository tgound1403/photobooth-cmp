package com.example.photogether.ui.camera

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photogether.core.camera.CameraAdjustState
import com.example.photogether.core.camera.Click
import com.example.photogether.core.camera.ManualExposureSteps
import com.example.photogether.core.camera.SliderTickPlayer
import com.example.photogether.core.camera.formatEv
import com.example.photogether.ui.components.ApertureGalleryButton
import com.example.photogether.core.render.FilterInfo
import com.example.photogether.ui.components.FanUiState
import com.example.photogether.ui.components.FilterFanButton
import com.example.photogether.core.camera.RoundWheel
import com.example.photogether.ui.components.HorizontalDial
import com.example.photogether.ui.components.PickerButton
import com.example.photogether.ui.components.RotaryWheel
import com.example.photogether.ui.components.bounceClick
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.InkColors

/** One exposure control, whichever way it is shown (round wheel or horizontal dial). */
private class ControlSpec(
    val label: String,
    val valueText: String,
    val index: Int,
    val count: Int,
    val onIndexChange: (Int) -> Unit,
    val enabled: Boolean,
)

/**
 * Right-hand body of the landscape camera: a dark leather plate carrying the A/M switch, the filter fan
 * button and the shutter on top, two horizontal dials (swiped sideways), and at the bottom the gallery frame
 * and camera selector on the left with the round wheel (with its LCD readout) on the right.
 *
 * Which of EV, ISO and shutter is the round wheel is [roundWheel]; the other two are the dials. ISO and
 * shutter respond only in manual mode; EV only in auto, as on a real body. Without manual sensor control
 * ([CameraAdjustState.manualAvailable] false) the manual dials are locked and read "N/A".
 *
 * Height budget on a 360dp phone (328dp usable): padding 16 + top row 68 (shutter) + dials 2 x 46 + 4 + bottom
 * row about 140 (112 wheel + 5 + LCD ~22; the left column is shorter) = 320dp.
 *
 * [viewRatio] is the viewfinder's portrait width over height; the gallery frame uses its inverse, like the live view.
 */
@Composable
fun CameraBodyPlate(
    adjust: CameraAdjustState,
    roundWheel: RoundWheel,
    viewRatio: Float,
    onManualMode: (Boolean) -> Unit,
    onIso: (Int) -> Unit,
    onShutter: (Long) -> Unit,
    onEv: (Float) -> Unit,
    onCapture: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPicker: () -> Unit,
    pickerName: String?,
    pickerCameraId: String?,
    lastPhoto: Uri?,
    captureSeq: Int,
    filters: List<FilterInfo>,
    onSelectFilter: (String) -> Unit,
    onFan: (FanUiState?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isoRange = adjust.isoRange
    val shutterRange = adjust.exposureRangeNs
    val isoSteps = remember(isoRange) { isoRange?.let { ManualExposureSteps.isoSteps(it) }.orEmpty() }
    val shutterSteps = remember(shutterRange) { shutterRange?.let { ManualExposureSteps.shutterSteps(it) }.orEmpty() }
    val manual = adjust.manualExposureActive && adjust.manualAvailable
    val ev = adjust.evRange
    val evIndex = ev.indexFor(adjust.ev) - ev.minIndex
    val evCount = ev.maxIndex - ev.minIndex + 1

    fun specFor(which: RoundWheel): ControlSpec = when (which) {
        RoundWheel.TV -> ControlSpec(
            label = "TV",
            valueText = if (shutterSteps.isEmpty()) "N/A" else ManualExposureSteps.shutterLabel(adjust.manualExposureNs),
            index = if (shutterSteps.isEmpty()) 0 else ManualExposureSteps.nearestIndex(shutterSteps, adjust.manualExposureNs),
            count = shutterSteps.size.coerceAtLeast(1),
            onIndexChange = { onShutter(shutterSteps[it.coerceIn(shutterSteps.indices)]) },
            enabled = manual && shutterSteps.isNotEmpty(),
        )
        RoundWheel.ISO -> ControlSpec(
            label = "ISO",
            valueText = if (isoSteps.isEmpty()) "N/A" else ManualExposureSteps.isoLabel(adjust.manualIso),
            index = if (isoSteps.isEmpty()) 0 else ManualExposureSteps.nearestIndex(isoSteps, adjust.manualIso),
            count = isoSteps.size.coerceAtLeast(1),
            onIndexChange = { onIso(isoSteps[it.coerceIn(isoSteps.indices)]) },
            enabled = manual && isoSteps.isNotEmpty(),
        )
        RoundWheel.EV -> ControlSpec(
            label = "EV",
            valueText = formatEv(adjust.ev),
            index = evIndex,
            count = evCount.coerceAtLeast(1),
            onIndexChange = { onEv(ev.evFor(it + ev.minIndex)) },
            enabled = !manual,
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .leatherPlate()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top plate: the mode switch on the left, the filter fan button and the shutter release by the grip on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ModeSwitch(manual = manual, enabled = adjust.manualAvailable, onChange = onManualMode)
            FilterFanButton(filters = filters, onSelect = onSelectFilter, onFan = onFan)
            ShutterButton(onClick = onCapture)
        }
        // Two knurled dials swiped sideways, the two controls that are not the round wheel.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            roundWheel.dials().forEach { which ->
                val c = specFor(which)
                key(which) {
                HorizontalDial(
                    label = c.label,
                    valueText = c.valueText,
                    index = c.index,
                    count = c.count,
                    onIndexChange = c.onIndexChange,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    enabled = c.enabled,
                )
                }
            }
        }
        // Bottom plate: camera selector and the gallery frame stacked on the left, the single turning wheel on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Heights: picker ~44 + gap 4 + gallery frame up to 64 (a 1:1 view) = ~112dp.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PickerButton(name = pickerName, cameraId = pickerCameraId, onClick = onOpenPicker)
                ApertureGalleryButton(
                    lastPhoto = lastPhoto,
                    captureSeq = captureSeq,
                    onClick = onOpenGallery,
                    frameRatio = 1f / viewRatio,
                )
            }
            val w = specFor(roundWheel)
            key(roundWheel) {
                RotaryWheel(
                    label = w.label,
                    valueText = w.valueText,
                    index = w.index,
                    count = w.count,
                    onIndexChange = w.onIndexChange,
                    size = 112.dp,
                    enabled = w.enabled,
                )
            }
        }
    }
}

/** Dark leather: a vertical gradient with a bevelled rim, lit from the left. */
private fun Modifier.leatherPlate(): Modifier {
    val shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp)
    return this
        .width(260.dp)
        .shadow(10.dp, shape)
        .background(Brush.verticalGradient(listOf(Color(0xFF2A2825), Color(0xFF191817), Color(0xFF211F1D))), shape)
        .border(
            1.dp,
            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.6f))),
            shape,
        )
}

/**
 * A/M switch: a recessed slot with a sliding metal knob. Tap or drag across to flip it; the knob clunks
 * and gives a haptic bump. Locked (inert) when [enabled] is false.
 */
@Composable
fun ModeSwitch(manual: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val trackWidth = 64.dp
    val knob = 30.dp
    val travel = trackWidth - knob - 4.dp
    val x: Dp by animateDpAsState(if (manual) travel else 0.dp, spring(dampingRatio = 0.6f, stiffness = 500f), label = "modeKnob")
    val flip: (Boolean) -> Unit = { target ->
        if (enabled && target != manual) {
            SliderTickPlayer.play(context, Click.SWITCH)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onChange(target)
        }
    }
    val slot = RoundedCornerShape(17.dp)
    Box(
        modifier = modifier
            .size(trackWidth, 34.dp)
            .semantics {
                contentDescription = "Chế độ phơi sáng"
                stateDescription = if (manual) "Thủ công" else "Tự động"
            }
            .background(Brush.verticalGradient(listOf(Color(0xFF050505), Color(0xFF1A1917))), slot)
            .border(1.dp, Brush.verticalGradient(listOf(Color.Black, Color.White.copy(alpha = 0.18f))), slot)
            .pointerInput(manual, enabled) {
                detectHorizontalDragGestures { change, drag ->
                    change.consume()
                    if (drag > 4f) flip(true) else if (drag < -4f) flip(false)
                }
            }
            .pointerInput(manual, enabled) { detectTapGestures { flip(!manual) } },
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("A", style = BinsooType.label, color = if (!manual) InkColors.accent else InkColors.textSecondary, modifier = Modifier.padding(start = 10.dp))
            Text("M", style = BinsooType.label, color = if (manual) InkColors.accent else InkColors.textSecondary, modifier = Modifier.padding(end = 10.dp))
        }
        Box(
            Modifier
                .offset(x = x + 2.dp, y = 2.dp)
                .size(knob)
                .shadow(4.dp, CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFE4E2DA), Color(0xFF8F8D86), Color(0xFFD2D0C8))), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        )
    }
}

/** Round release button: a chrome ring around a cream cap, pressed with a bounce. */
@Composable
fun ShutterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .size(68.dp)
            .shadow(8.dp, CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFEDEBE3), Color(0xFF7C7A74), Color(0xFFCFCDC5))), CircleShape)
            .padding(5.dp)
            .background(Color(0xFF151413), CircleShape)
            .padding(4.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFFF4F3E8), InkColors.accent, Color(0xFFBDBCAE))), CircleShape)
            .semantics { contentDescription = "Chụp ảnh" },
    )
}
