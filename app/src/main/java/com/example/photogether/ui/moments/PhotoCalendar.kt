package com.example.photogether.ui.moments

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.photogether.core.moments.CalendarColors
import com.example.photogether.core.moments.CalendarDay
import com.example.photogether.core.moments.Moment
import com.example.photogether.core.moments.MomentCalendar
import com.example.photogether.core.moments.WeekdayLabels
import com.example.photogether.core.moments.YearMonth
import com.example.photogether.ui.theme.AppColors
import com.example.photogether.ui.theme.BinsooType
import com.example.photogether.ui.theme.Radius
import com.example.photogether.ui.theme.Spacing
import com.example.photogether.ui.theme.Stroke
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import com.example.photogether.ui.theme.AppIcons
import kotlinx.coroutines.launch
import com.tgound.photogether.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Photo prints keep a white border whatever the app theme is. */
private val PRINT_WHITE = Color(0xFFFDFCF7)
private val TODAY = Color(0xFFF2C21B)

/** Colours of the calendar sheet, all derived from the one colour the user picked. */
private class SheetPalette(argb: Int) {
    val paper = Color(CalendarColors.opaque(argb))
    val text = Color(CalendarColors.textOn(argb))
    val dim = Color(CalendarColors.dimTextOn(argb))
    val weekend = Color(CalendarColors.weekendOn(argb))
    val grid = Color(CalendarColors.gridLineOn(argb))
}

/** At most this many prints are pinned on one day cell. */
private const val STACK_PER_CELL = 3

/**
 * Month-by-month calendar with the photos stuck on their days: each at its own aspect ratio,
 * a little crooked, each held by a push pin. Swipe sideways and the month sheet flies away like a
 * torn-off page. Tapping a day with photos opens its drawer ([DayDrawerDialog]).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoCalendar(
    moments: List<Moment>,
    onEdit: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
    sheetColor: Int = CalendarColors.DEFAULT,
    onDayClosed: () -> Unit = {},
) {
    val palette = remember(sheetColor) { SheetPalette(sheetColor) }
    val byDay = remember(moments) { MomentCalendar.groupByDay(moments) }
    val thisMonth = remember { MomentCalendar.dayOf(System.currentTimeMillis()).yearMonth }
    val months = remember(byDay.keys, thisMonth) { MomentCalendar.months(byDay.keys, thisMonth) }
    val pagerState = rememberPagerState { months.size }
    val scope = rememberCoroutineScope()
    val firstDayOfWeek = remember { Calendar.getInstance().firstDayOfWeek }
    var openDay by remember { mutableStateOf<CalendarDay?>(null) }
    val today = remember { MomentCalendar.dayOf(System.currentTimeMillis()) }

    Box(modifier = modifier.fillMaxWidth().aspectRatio(SHEET_ASPECT)) {
    // Sheets still waiting in the pad: they show under the front sheet, each a little crooked.
    if (months.size > 1) PadSheet(depth = 2, palette = palette)
    if (months.size > 1) PadSheet(depth = 1, palette = palette)
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { page ->
        MonthSheet(
            palette = palette,
            month = months[page],
            firstDayOfWeek = firstDayOfWeek,
            byDay = byDay,
            today = today,
            onDayClick = {
                openDay = it
            },
            // Index 0 is the newest month, so "older" is the next page.
            onOlder = if (page < months.size - 1) {
                { scope.launch { pagerState.animateScrollToPage(page + 1) } }
            } else null,
            onNewer = if (page > 0) {
                { scope.launch { pagerState.animateScrollToPage(page - 1) } }
            } else null,
            modifier = Modifier
                .zIndex(-page.toFloat())
                .graphicsLayer {
                    // 0 when this sheet is the one in view, negative while it is flying up, positive while it waits below.
                    val offset = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                    if (offset <= 0f) {
                        val t = (-offset).coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0f, 0.12f)
                        rotationZ = -9f * t
                        translationX = -size.width * 0.12f * t
                        translationY = -size.height * 0.03f * t
                        alpha = 1f - 0.9f * t
                    } else {
                        // The page below stays put, like the next month under a torn-off sheet.
                        val t = offset.coerceIn(0f, 1f)
                        translationX = -offset * size.width
                        val s = 1f - 0.06f * t
                        scaleX = s
                        scaleY = s
                    }
                },
        )
    }
    }

    openDay?.let { day ->
        DayDrawerDialog(
            day = day,
            photos = byDay[day].orEmpty(),
            onDismiss = {
                openDay = null
                onDayClosed()
            },
            onEdit = {
                openDay = null
                onDayClosed()
                onEdit(it)
            },
        )
    }
}

private const val SHEET_ASPECT = 0.9f
private val PAD_SIDE = 10.dp
private val PAD_BOTTOM = 28.dp

/** A page lying under the front sheet, peeking out at the bottom. [depth] 1 is nearest. */
@Composable
private fun PadSheet(depth: Int, palette: SheetPalette) {
    val shape = RoundedCornerShape(Radius.lg)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = PAD_SIDE, end = PAD_SIDE, bottom = PAD_BOTTOM - 6.dp * depth)
            .graphicsLayer {
                rotationZ = if (depth == 1) 0.9f else -1.1f
                translationY = 7.dp.toPx() * depth
                scaleX = 1f - 0.012f * depth
            }
            .shadow(3.dp, shape)
            .clip(shape)
            .background(palette.paper.compositeOver(Color(0x22888888)))
            .border(Stroke.hairline, AppColors.outline.copy(alpha = 0.6f), shape),
    )
}

@Composable
private fun MonthSheet(
    palette: SheetPalette,
    month: YearMonth,
    firstDayOfWeek: Int,
    byDay: Map<CalendarDay, List<Moment>>,
    today: CalendarDay,
    onDayClick: (CalendarDay) -> Unit,
    onOlder: (() -> Unit)?,
    onNewer: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.lg)
    val cells = remember(month, firstDayOfWeek) { MomentCalendar.monthCells(month, firstDayOfWeek) }
    val title = remember(month) {
        val date = Calendar.getInstance().apply { clear(); set(month.year, month.month, 1) }.time
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date).uppercase()
    }
    val photoCount = remember(month, byDay) { byDay.entries.filter { it.key.yearMonth == month }.sumOf { it.value.size } }
    val weekdays = remember(firstDayOfWeek) { weekdayLabels(firstDayOfWeek) }

    val paper = palette.paper
    val gridLine = palette.grid
    val weekendIndexes = remember(firstDayOfWeek) {
        (0 until 7).filter { i -> (firstDayOfWeek - 1 + i) % 7 + 1 in listOf(Calendar.SATURDAY, Calendar.SUNDAY) }.toSet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = PAD_SIDE, end = PAD_SIDE, bottom = PAD_BOTTOM)
            .shadow(8.dp, shape, ambientColor = Color(0x66000000), spotColor = Color(0x99000000))
            .clip(shape)
            .background(paper)
            .border(Stroke.border, AppColors.outline, shape),
    ) {
        // Header band, like the coloured top of a wall calendar, with the binding holes in it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFF7D44A), Color(0xFFEBB91A))))
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Binding()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = BinsooType.title,
                    color = Color(0xFF0B0B0B),
                    modifier = Modifier.weight(1f),
                )
                if (photoCount > 0) {
                    Text(
                        text = pluralStringResource(R.plurals.calendar_photo_count, photoCount, photoCount),
                        style = BinsooType.micro,
                        color = Color(0xFF3A2E05),
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                }
                MonthArrow(older = true, onClick = onOlder)
                MonthArrow(older = false, onClick = onNewer)
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEachIndexed { i, label ->
                    Text(
                        text = label,
                        style = BinsooType.micro,
                        color = if (i in weekendIndexes) palette.weekend else palette.dim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = Spacing.xs),
                    )
                }
            }
            for (row in cells.chunked(7)) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    row.forEachIndexed { i, dayOfMonth ->
                        val day = dayOfMonth?.let { CalendarDay(month.year, month.month, it) }
                        DayCell(
                            palette = palette,
                            dayOfMonth = dayOfMonth,
                            photos = day?.let { byDay[it] }.orEmpty(),
                            isToday = day == today,
                            weekend = i in weekendIndexes,
                            onClick = { day?.let(onDayClick) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .border(Stroke.hairline, gridLine),
                        )
                    }
                }
            }
        }
    }
}

/** Round arrow in the header: older month (points left) or newer month (points right). Dimmed when there is none. */
@Composable
private fun MonthArrow(older: Boolean, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = stringResource(if (older) R.string.calendar_older else R.string.calendar_newer),
            tint = Color(0xFF0B0B0B).copy(alpha = if (onClick != null) 1f else 0.25f),
            modifier = Modifier.size(18.dp).rotate(if (older) 180f else 0f),
        )
    }
}

/** Row of ring holes along the top of the sheet, like a wall calendar. */
@Composable
private fun Binding() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(9) {
            // A punched hole with a bit of shade inside, and a metal ring through it.
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF3A3A3A))), CircleShape)
                    .border(1.dp, Color(0x55000000), CircleShape),
            )
        }
    }
}

/** Height of the strip at the top of a day cell that is kept for the day number. */
private val NUMBER_ROW = 15.dp

@Composable
private fun DayCell(
    palette: SheetPalette,
    dayOfMonth: Int?,
    photos: List<Moment>,
    isToday: Boolean,
    weekend: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (dayOfMonth == null) {
        Box(modifier)
        return
    }
    BoxWithConstraints(
        modifier = modifier
            .padding(1.dp)
            .clickable(enabled = photos.isNotEmpty(), onClick = onClick),
    ) {
        // Newest print is drawn last, so it lies on top of the older ones of the same day.
        val shown = photos.take(STACK_PER_CELL)
        for ((index, moment) in shown.withIndex().reversed()) {
            StuckPrint(
                moment = moment,
                salt = index,
                boxWidth = maxWidth,
                boxHeight = maxHeight,
                topInset = NUMBER_ROW,
            )
        }
        val numberStyle = BinsooType.micro
        if (photos.isEmpty()) {
            Text(
                text = dayOfMonth.toString(),
                style = numberStyle,
                color = when {
                    isToday -> Color(0xFF0B0B0B)
                    weekend -> palette.weekend
                    else -> palette.dim
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .then(if (isToday) Modifier.background(TODAY, CircleShape) else Modifier)
                    .padding(horizontal = 3.dp),
            )
        } else {
            // The number sits in its own row above the print, so the photo never hides it.
            Text(
                text = dayOfMonth.toString(),
                style = numberStyle,
                color = if (isToday) Color(0xFF0B0B0B) else palette.text,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .then(if (isToday) Modifier.background(TODAY, CircleShape) else Modifier)
                    .padding(horizontal = 3.dp),
            )
        }
    }
}

/**
 * A small print in a day cell, kept at the photo's own aspect ratio, tilted and nudged by a pose
 * that is the same on every run, with a push pin at the top.
 */
@Composable
private fun StuckPrint(
    moment: Moment,
    salt: Int,
    boxWidth: Dp,
    boxHeight: Dp,
    topInset: Dp,
) {
    val pose = remember(moment.uri, salt) { MomentCalendar.poseFor(moment.uri, maxTilt = 8f, salt = salt) }
    val ratio = remember(moment) { MomentCalendar.ratioOf(moment) }
    // The print lives below the row that holds the day number.
    val areaH = boxHeight - topInset
    val fitW = boxWidth * 0.98f
    val fitH = areaH * 0.98f
    val (w, h) = if (ratio >= fitW / fitH) fitW to fitW / ratio else fitH * ratio to fitH
    val freeX = (boxWidth - w) / 2
    val freeY = (areaH - h) / 2

    Box(
        modifier = Modifier
            .offset(x = freeX + freeX * pose.dx * 0.7f, y = topInset + freeY + freeY * pose.dy * 0.7f)
            .size(w, h)
            .graphicsLayer {
                rotationZ = pose.rotation
                shadowElevation = 3.5f * density
                ambientShadowColor = Color(0x80000000)
                spotShadowColor = Color(0xB0000000)
            }
            .background(PRINT_WHITE)
            .padding(1.5.dp),
    ) {
        AsyncImage(
            model = moment.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Pin(
            seed = moment.uri,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-1).dp),
        )
    }
}

/** A push pin: a round coloured head with a highlight and a small shadow on the print. The colour follows the photo. */
@Composable
private fun Pin(seed: String, modifier: Modifier = Modifier) {
    val head = PIN_COLORS[(seed.hashCode() and 0x7FFFFFFF) % PIN_COLORS.size]
    Box(modifier = modifier.size(10.dp), contentAlignment = Alignment.Center) {
        // Shadow cast on the paper, toward the lower right.
        Box(
            modifier = Modifier
                .offset(x = 1.5.dp, y = 2.dp)
                .size(8.dp)
                .background(Color(0x66000000), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer { shadowElevation = 2.dp.toPx(); shape = CircleShape }
                .background(
                    Brush.radialGradient(
                        colors = listOf(head.compositeOver(Color(0x88FFFFFF)), head, head.compositeOver(Color(0x66000000))),
                    ),
                    CircleShape,
                ),
        )
    }
}

private val PIN_COLORS = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFFFDD835), Color(0xFF43A047))

/** Short weekday names starting on [firstDayOfWeek] ([Calendar] constant), in the device language. */
private fun weekdayLabels(firstDayOfWeek: Int): List<String> =
    WeekdayLabels.of(Locale.getDefault(), firstDayOfWeek)

/** "22 OCT 2026" style label for a drawer title. */
internal fun dayLabel(day: CalendarDay): String {
    val date: Date = Calendar.getInstance().apply { clear(); set(day.year, day.month, day.day) }.time
    return SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault()).format(date).uppercase()
}
