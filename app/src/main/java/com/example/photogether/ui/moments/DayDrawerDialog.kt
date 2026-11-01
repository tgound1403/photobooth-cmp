package com.example.photogether.ui.moments

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.photogether.core.moments.CalendarDay
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.moments.MomentCalendar
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.view.GaleryScreen.DetailShot
import com.example.photogether.ui.view.GaleryScreen.ShotDetailDialog
import com.tgound.photogether.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val WOOD = Color(0xFF6B5543)
private val WOOD_DARK = Color(0xFF3A2D23)
private val WOOD_FLOOR = Color(0xFF2A2019)
private val PRINT = Color(0xFFFDFCF7)
private val BRASS = Color(0xFFD9C79A)
private val FILM = Color(0xFF121212)
private val FILM_EDGE_TEXT = Color(0xFFF2C21B)

/** A day with a lot of photos shows this many lying in the drawer; the film strip holds all of them. */
private const val MAX_LOOSE = 24
private val FLOOR_HEIGHT = 440.dp
private val FRONT_HEIGHT = 84.dp

/** Strip along the front wall of the drawer floor, kept free of loose prints for the film roll. */
private val ROLL_ZONE = 68.dp

private const val CLOSE_MS = 340
private const val STRIP_MS = 600

/**
 * The photos of one day in a wooden drawer. It is open as soon as it shows. Prints lie in it at random
 * angles and overlap; touching one lifts it on top, and it can be dragged. A tap opens the photo's details.
 * The film roll in the drawer unwinds a strip when tapped, with every photo of the day on it.
 * Closing pushes the drawer back in before the dialog goes away.
 */
@Composable
fun DayDrawerDialog(
    day: CalendarDay,
    photos: List<Moment>,
    onDismiss: () -> Unit,
    onEdit: (uri: String) -> Unit,
) {
    var detail by remember { mutableStateOf<Moment?>(null) }
    var stripOpen by remember { mutableStateOf(false) }
    // 1 = drawer fully out, 0 = pushed in. It starts out: the drawer is already open when it appears.
    val open = remember { Animatable(1f) }
    var closing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loose = remember(photos) { photos.take(MAX_LOOSE) }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            open.animateTo(0f, tween(durationMillis = CLOSE_MS, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * open.value))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::close)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            // Fixed-size slot, so the drawer's top edge stays where it is while it closes.
            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.lg)
                    .fillMaxWidth()
                    .height(FLOOR_HEIGHT + FRONT_HEIGHT)
                    .graphicsLayer { alpha = (open.value * 6f).coerceIn(0f, 1f) },
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Swallows taps on the drawer itself, so only the dark area around it closes the dialog.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            ) {
                // Closing pushes the drawer back into the cabinet: the front panel travels up the screen and the
                // floor, with its prints, slides up under the top edge.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FLOOR_HEIGHT * open.value.coerceIn(0f, 1f))
                        .clipToBounds()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF2A1F17), WOOD_DARK, Color(0xFF2A1F17))),
                            RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg),
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                            .height(FLOOR_HEIGHT)
                            .padding(Spacing.sm)
                            .background(
                                Brush.radialGradient(listOf(Color(0xFF3A2D22), WOOD_FLOOR, Color(0xFF15100C))),
                                RoundedCornerShape(Radius.md),
                            ),
                    ) {
                        DrawerFloor(
                            looseShown = loose,
                            enabled = !closing,
                            onOpen = { detail = it },
                            onRoll = { stripOpen = true },
                        )
                        // Shade along the back wall and the side walls, so the floor looks deep.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(22.dp)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(22.dp)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))),
                        )
                    }
                }
                FrontPanel(day = day, count = photos.size)
            }
            }
        }
    }

    if (stripOpen) {
        FilmStripDialog(
            day = day,
            photos = photos,
            onDismiss = { stripOpen = false },
            onOpen = { detail = it },
        )
    }

    detail?.let { moment ->
        ShotDetailDialog(
            shots = photos.map { DetailShot(it.uri, it.takenAt, it.width, it.height) },
            initialIndex = photos.indexOfFirst { it.uri == moment.uri }.coerceAtLeast(0),
            onDismiss = { detail = null },
            onEdit = { page ->
                detail = null
                onEdit(photos[page].uri)
            },
        )
    }
}

/** Front of the drawer: a brass handle and a label plate with the date. */
@Composable
private fun FrontPanel(day: CalendarDay, count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FRONT_HEIGHT)
            .shadow(14.dp, RoundedCornerShape(bottomStart = Radius.lg, bottomEnd = Radius.lg), ambientColor = Color.Black, spotColor = Color.Black)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF8A6F58), WOOD, Color(0xFF54402F))),
                RoundedCornerShape(bottomStart = Radius.lg, bottomEnd = Radius.lg),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(12.dp)
                    .shadow(4.dp, RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFF1E4BC), BRASS, Color(0xFF9C8856))),
                        RoundedCornerShape(6.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .background(PRINT, RoundedCornerShape(Radius.sm))
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            ) {
                Text(
                    text = dayLabel(day) + "  ·  " + pluralStringResource(R.plurals.calendar_photo_count, count, count),
                    style = BinsooType.label,
                    color = Color(0xFF1B1713),
                )
            }
        }
    }
}

/** Where a print lies on the floor before the user drags it. */
private class Placement(val base: Offset, val size: Pair<Dp, Dp>, val tilt: Float)

/** The inside of the drawer: the loose prints of the day. */
@Composable
private fun DrawerFloor(
    looseShown: List<Moment>,
    enabled: Boolean,
    onOpen: (Moment) -> Unit,
    onRoll: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(Spacing.md)) {
        val density = LocalDensity.current
        val areaW = maxWidth
        // The bottom strip of the floor is where the film roll lies, so prints are kept off it.
        val areaH = maxHeight - ROLL_ZONE
        val maxPrintW = areaW * 0.5f
        val maxPrintH = areaH * 0.42f

        val placements = remember(looseShown, areaW, areaH) {
            looseShown.associate { m ->
                val ratio = MomentCalendar.ratioOf(m)
                val (w, h) = if (ratio >= maxPrintW / maxPrintH) maxPrintW to maxPrintW / ratio else maxPrintH * ratio to maxPrintH
                val pose = MomentCalendar.poseFor(m.uri, maxTilt = 16f, salt = 7)
                val x = (areaW - w) / 2 * (1f + pose.dx)
                val y = (areaH - h) / 2 * (1f + pose.dy)
                m.uri to Placement(Offset(with(density) { x.toPx() }, with(density) { y.toPx() }), w to h, pose.rotation)
            }
        }
        // Higher number lies on top. The newest photo starts on top; touching one raises it.
        val stacking = remember(looseShown) {
            mutableStateMapOf<String, Float>().apply { looseShown.forEachIndexed { i, m -> put(m.uri, (looseShown.size - i).toFloat()) } }
        }
        val drag = remember(looseShown) { mutableStateMapOf<String, Offset>() }
        var top by remember(looseShown) { mutableStateOf(looseShown.size + 1f) }

        for (moment in looseShown) {
            val placed = placements.getValue(moment.uri)
            val moved = drag[moment.uri] ?: Offset.Zero
            Box(
                modifier = Modifier
                    .zIndex(stacking[moment.uri] ?: 0f)
                    .offset { IntOffset((placed.base.x + moved.x).roundToInt(), (placed.base.y + moved.y).roundToInt()) }
                    .size(placed.size.first, placed.size.second)
                    .graphicsLayer {
                        rotationZ = placed.tilt
                        shadowElevation = 10.dp.toPx()
                        spotShadowColor = Color.Black
                        ambientShadowColor = Color.Black
                    }
                    .background(PRINT)
                    .padding(5.dp)
                    .pointerInput(moment.uri, enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures(onTap = {
                            top += 1f
                            stacking[moment.uri] = top
                            onOpen(moment)
                        })
                    }
                    .pointerInput(moment.uri, enabled) {
                        if (!enabled) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                top += 1f
                                stacking[moment.uri] = top
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                drag[moment.uri] = (drag[moment.uri] ?: Offset.Zero) + amount
                            },
                        )
                    },
            ) {
                AsyncImage(
                    model = moment.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        FilmRoll(
            onClick = { if (enabled) onRoll() },
            modifier = Modifier.zIndex(1000f).align(Alignment.BottomEnd),
        )
    }
}

private val FRAME_SIZE = 190.dp
private val SPROCKET_WIDTH = 22.dp
private val FRAME_PAD = 3.dp
private val REEL_WIDTH = 320.dp

/** How much of the big reel shows above the bottom edge of the screen. */
private val REEL_VISIBLE = 112.dp

/**
 * Full-screen view of the day's film. A film roll lies across the bottom edge of the
 * screen, with "PHOTOGETHER" on it, and the strip comes out of its top. Swiping up pulls more film out of
 * the roll (and turns it), one frame per photo; swiping down winds it back in. Closing drops the roll away.
 */
@Composable
private fun FilmStripDialog(
    day: CalendarDay,
    photos: List<Moment>,
    onDismiss: () -> Unit,
    onOpen: (Moment) -> Unit,
) {
    val pull = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { pull.animateTo(1f, tween(STRIP_MS, easing = FastOutSlowInEasing)) }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            pull.animateTo(0f, tween(STRIP_MS, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E0C0A).copy(alpha = 0.94f * pull.value))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::close),
        ) {
            val density = LocalDensity.current
            val screenH = maxHeight
            val heightPx = with(density) { screenH.toPx() }
            val itemPx = with(density) { (FRAME_SIZE + FRAME_PAD * 2).toPx() }
            val listState = rememberLazyListState()
            // How much film has come out of the roll; the roll turns by the same amount.
            val pulled by remember {
                derivedStateOf { listState.firstVisibleItemIndex * itemPx + listState.firstVisibleItemScrollOffset }
            }

            Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationY = (1f - pull.value) * heightPx }) {
                // The first frame starts just above the roll; later frames come out of the roll as the list is pulled up.
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = screenH - REEL_VISIBLE - FRAME_SIZE - FRAME_PAD * 2 + 22.dp,
                        bottom = screenH - FRAME_SIZE - 110.dp,
                    ),
                ) {
                    itemsIndexed(photos, key = { _, m -> m.uri }) { index, moment ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(
                                modifier = Modifier
                                    .width(FRAME_SIZE + SPROCKET_WIDTH * 2)
                                    .background(FILM)
                                    .padding(vertical = FRAME_PAD),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Sprockets()
                                Box(
                                    modifier = Modifier
                                        .size(FRAME_SIZE)
                                        .background(Color(0xFF1E1E1E))
                                        .clickable(enabled = !closing) { onOpen(moment) },
                                ) {
                                    // Fit, so the whole photo shows at its own proportions.
                                    AsyncImage(
                                        model = moment.uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Text(
                                        text = "▸ ${index + 1}",
                                        style = BinsooType.micro,
                                        color = FILM_EDGE_TEXT,
                                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 2.dp),
                                    )
                                }
                                Sprockets()
                            }
                        }
                    }
                }

                // Film passing under the top edge fades out; the date sits over the fade.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFF0E0C0A), Color.Transparent))),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = Spacing.md)
                        .graphicsLayer { alpha = pull.value },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = dayLabel(day), style = BinsooType.label, color = FILM_EDGE_TEXT)
                    Text(
                        text = pluralStringResource(R.plurals.calendar_photo_count, photos.size, photos.size),
                        style = BinsooType.micro,
                        color = FILM_EDGE_TEXT.copy(alpha = 0.7f),
                    )
                }

                FilmReel(pulled = pulled, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/**
 * A big film roll lying across the bottom of the screen, its top rising above the edge. The film comes off
 * the top of the roll and runs up the screen. The ridges on its body move up by [pulled] pixels, the film
 * that has come out, so the roll turns as the strip is pulled. "PHOTOGETHER" is on its label.
 */
@Composable
private fun FilmReel(pulled: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(REEL_WIDTH).height(REEL_VISIBLE)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val flangeW = 20.dp.toPx()
            val bodyTop = 14.dp.toPx()
            val bodyL = flangeW - 2.dp.toPx()
            val bodyR = size.width - flangeW + 2.dp.toPx()
            val corner = 18.dp.toPx()

            // Shadow of the roll on the film above it.
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), startY = bodyTop - 22.dp.toPx(), endY = bodyTop + 2.dp.toPx()),
                topLeft = Offset(bodyL, bodyTop - 22.dp.toPx()),
                size = Size(bodyR - bodyL, 24.dp.toPx()),
            )
            // Wound film: a cylinder lit from above, so it is dark at the top edge, bright where it turns
            // toward the light, and darker again as it curves away.
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1D1916), Color(0xFF4D463F), Color(0xFF2A2521), Color(0xFF0D0B0A)),
                    startY = bodyTop,
                    endY = size.height,
                ),
                topLeft = Offset(bodyL, bodyTop),
                size = Size(bodyR - bodyL, size.height * 2),
                cornerRadius = CornerRadius(corner),
            )
            // Layers of film: ridges across the body that follow the film as it is pulled.
            val spacing = 9.dp.toPx()
            val shift = pulled % spacing
            var y = bodyTop - shift
            while (y < size.height) {
                if (y > bodyTop) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(bodyL + 4.dp.toPx(), y),
                        end = Offset(bodyR - 4.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                y += spacing
            }
            // Bright strip along the curve, where the light hits.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(bodyL + 10.dp.toPx(), bodyTop + 7.dp.toPx()),
                size = Size(bodyR - bodyL - 20.dp.toPx(), 3.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            // Spool flanges at both ends: taller than the film, metal, with a yellow edge.
            for (x in listOf(0f, size.width - flangeW)) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = if (x == 0f) listOf(Color(0xFF8E8E88), Color(0xFFE6E6DE), Color(0xFF6A6A64))
                        else listOf(Color(0xFF6A6A64), Color(0xFFE6E6DE), Color(0xFF8E8E88)),
                        startX = x,
                        endX = x + flangeW,
                    ),
                    topLeft = Offset(x, 0f),
                    size = Size(flangeW, size.height * 2),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
                drawRoundRect(
                    color = FILM_EDGE_TEXT,
                    topLeft = Offset(x + 1.dp.toPx(), 1.dp.toPx()),
                    size = Size(flangeW - 2.dp.toPx(), size.height * 2),
                    cornerRadius = CornerRadius(7.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .background(FILM_EDGE_TEXT, RoundedCornerShape(4.dp))
                .padding(horizontal = 18.dp, vertical = 6.dp),
        ) {
            Text(
                text = "PHOTOGETHER",
                style = BinsooType.label.copy(fontSize = 16.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFF1B1713),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** One side of sprocket holes next to a frame. */
@Composable
private fun Sprockets() {
    Column(
        modifier = Modifier.width(SPROCKET_WIDTH).height(FRAME_SIZE),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(8) {
            Box(Modifier.size(width = 8.dp, height = 11.dp).background(Color(0xFFE9E2CF), RoundedCornerShape(2.dp)))
        }
    }
}

/**
 * A 35mm film cassette lying on its side on the drawer floor, seen from above like the prints: a dark metal
 * body with a yellow label and two metal caps, a spindle stub at the left end, and the leader of the film
 * coming out of the slot along the body and lying on the wood. Just big enough to tap.
 */
@Composable
private fun FilmRoll(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.calendar_film_roll)
    Box(
        modifier = modifier
            .size(width = 112.dp, height = 78.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            fun dp(v: Float) = v.dp.toPx()
            val bodyL = dp(16f)
            val bodyR = size.width - dp(10f)
            val bodyT = dp(6f)
            val bodyH = dp(30f)
            val bodyB = bodyT + bodyH
            val capW = dp(9f)
            val slotL = bodyL + capW + dp(4f)
            val slotR = bodyR - capW - dp(4f)

            // Soft shadow of everything on the wood.
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset((bodyL + bodyR) / 2 + dp(4f), bodyB + dp(6f)),
                    radius = (bodyR - bodyL) / 2 + dp(14f),
                ),
                topLeft = Offset(bodyL - dp(10f), bodyB - dp(8f)),
                size = Size(bodyR - bodyL + dp(28f), dp(24f)),
            )

            // Leader of the film: leaves the slot along the whole body, curls a little and ends in a cut tongue.
            val tip = size.height - dp(4f)
            val tongue = dp(10f)
            val leader = Path().apply {
                moveTo(slotL, bodyB - dp(5f))
                lineTo(slotR, bodyB - dp(5f))
                cubicTo(slotR + dp(2f), bodyB + dp(8f), slotR + dp(7f), tip - dp(14f), slotR + dp(5f), tip - tongue)
                lineTo(slotR - dp(4f), tip)
                lineTo(slotL + dp(10f), tip - dp(1f))
                lineTo(slotL + dp(2f), tip - tongue - dp(2f))
                cubicTo(slotL + dp(1f), tip - dp(16f), slotL - dp(1f), bodyB + dp(8f), slotL, bodyB - dp(5f))
                close()
            }
            translate(left = dp(2.5f), top = dp(3f)) { drawPath(leader, Color.Black.copy(alpha = 0.55f)) }
            // Film base: deep amber, darker where it comes out of the slot.
            drawPath(
                leader,
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1F1209), Color(0xFF5A3413), Color(0xFF7A4A1C)),
                    startY = bodyB - dp(5f),
                    endY = tip,
                ),
            )
            // Sprocket holes along both long edges, cut through to the wood.
            val holes = 6
            val span = tip - tongue - bodyB - dp(2f)
            for (k in 0 until holes) {
                val y = bodyB + dp(1f) + span * (k + 0.5f) / holes
                for (x in listOf(slotL + dp(3.5f), slotR - dp(6.5f))) {
                    drawRoundRect(
                        Color(0xFF140E09),
                        Offset(x + k * dp(0.5f), y),
                        Size(dp(3f), dp(3.6f)),
                        CornerRadius(dp(0.8f)),
                    )
                }
            }
            // Frame lines and a soft glint down the film.
            for (k in 1..2) {
                val y = bodyB + span * k / 3
                drawLine(Color.Black.copy(alpha = 0.3f), Offset(slotL + dp(10f), y), Offset(slotR - dp(10f), y), dp(0.8f))
            }
            drawRect(Color.White.copy(alpha = 0.07f), Offset(slotL + dp(14f), bodyB - dp(3f)), Size(dp(3f), tip - bodyB - dp(10f)))

            // Cassette body: black metal, lit from above with a glossy streak.
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF77726D), Color(0xFF3A3734), Color(0xFF181615), Color(0xFF2A2725)),
                    startY = bodyT,
                    endY = bodyB,
                ),
                topLeft = Offset(bodyL, bodyT),
                size = Size(bodyR - bodyL, bodyH),
                cornerRadius = CornerRadius(dp(6f)),
            )
            // Yellow label wrapped round the body: bright on top, darker where it turns away.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFEE9A), Color(0xFFF2C21B), Color(0xFFD29E0E), Color(0xFF8B6606)),
                    startY = bodyT,
                    endY = bodyB,
                ),
                topLeft = Offset(slotL + dp(2f), bodyT + dp(2.5f)),
                size = Size(slotR - slotL - dp(4f), bodyH - dp(5f)),
            )
            // Thin black bar at the top of the label, like a printed band.
            drawRect(Color.Black.copy(alpha = 0.75f), Offset(slotL + dp(2f), bodyT + dp(2.5f)), Size(slotR - slotL - dp(4f), dp(3.5f)))
            // Gloss along the top of the body.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.38f),
                topLeft = Offset(bodyL + capW + dp(3f), bodyT + dp(1.5f)),
                size = Size(bodyR - bodyL - capW * 2 - dp(6f), dp(1.8f)),
                cornerRadius = CornerRadius(dp(1f)),
            )
            // The slot the film leaves through.
            drawRect(Color.Black, Offset(slotL, bodyB - dp(5.5f)), Size(slotR - slotL, dp(2f)))

            // Metal end caps, a little taller than the body, with fine ridges.
            for (x in listOf(bodyL, bodyR - capW)) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFF4F4EC), Color(0xFFB2B2AA), Color(0xFF6A6A64)),
                        startY = bodyT - dp(2f),
                        endY = bodyB + dp(2f),
                    ),
                    topLeft = Offset(x, bodyT - dp(2f)),
                    size = Size(capW, bodyH + dp(4f)),
                    cornerRadius = CornerRadius(dp(4f)),
                )
                for (r in 1..3) {
                    drawLine(
                        Color.Black.copy(alpha = 0.18f),
                        Offset(x + capW * r / 4f, bodyT),
                        Offset(x + capW * r / 4f, bodyB),
                        dp(0.7f),
                    )
                }
            }
            // Spindle stub poking out of the left cap, with a flat key.
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFE8E8E0), Color(0xFF8A8A84)), startY = bodyT + dp(10f), endY = bodyT + dp(20f)),
                topLeft = Offset(bodyL - dp(8f), bodyT + dp(10f)),
                size = Size(dp(9f), dp(10f)),
                cornerRadius = CornerRadius(dp(2f)),
            )
            drawRect(Color(0xFF3A3A36), Offset(bodyL - dp(8f), bodyT + dp(14.5f)), Size(dp(3f), dp(1.5f)))
        }
        Text(
            text = "PHOTOGETHER",
            style = BinsooType.micro.copy(fontSize = 5.5.sp, letterSpacing = 0.3.sp, fontWeight = FontWeight.Bold),
            color = Color(0xFF2A1F05),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.align(Alignment.TopStart).offset(x = 31.dp, y = 16.dp),
        )
    }
}
