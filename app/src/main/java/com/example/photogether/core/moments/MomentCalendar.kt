package com.example.photogether.core.moments

import java.util.Calendar
import java.util.TimeZone

/** A calendar month. [month] is 0-based, as in [Calendar]. */
data class YearMonth(val year: Int, val month: Int) {
    /** Sortable key: later months have bigger values. */
    val index: Int get() = year * 12 + month

    fun plus(months: Int): YearMonth = fromIndex(index + months)

    companion object {
        fun fromIndex(index: Int) = YearMonth(index / 12, index % 12)
    }
}

/** A day of the calendar. [month] is 0-based, [day] starts at 1. */
data class CalendarDay(val year: Int, val month: Int, val day: Int) {
    val yearMonth: YearMonth get() = YearMonth(year, month)
}

/** Where and how a photo is stuck on its calendar cell or tossed in a drawer. Offsets are fractions of the free space. */
data class StickPose(
    /** Degrees, clockwise. */
    val rotation: Float,
    /** -1..1 of the free space on each axis. */
    val dx: Float,
    val dy: Float,
)

/**
 * Pure logic for the home calendar and the day drawer: grouping by local day, month grids and
 * the repeatable "random" poses. No Android classes, so it runs in JVM tests.
 */
object MomentCalendar {

    fun dayOf(millis: Long, zone: TimeZone = TimeZone.getDefault()): CalendarDay {
        val c = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return CalendarDay(c[Calendar.YEAR], c[Calendar.MONTH], c[Calendar.DAY_OF_MONTH])
    }

    /** Moments grouped by local day. Order inside a day is kept (newest first when the input is). */
    fun groupByDay(moments: List<Moment>, zone: TimeZone = TimeZone.getDefault()): Map<CalendarDay, List<Moment>> =
        moments.groupBy { dayOf(it.takenAt, zone) }

    /**
     * Months to page through, newest first: from [current] back to the oldest month with a photo,
     * and at least [minSpan] months in total so the user can always step back through the calendar.
     * A photo dated after [current] (a clock change) extends the range upward, so it is never lost.
     */
    fun months(days: Collection<CalendarDay>, current: YearMonth, minSpan: Int = 12): List<YearMonth> {
        val newest = maxOf(current.index, days.maxOfOrNull { it.yearMonth.index } ?: current.index)
        val oldest = minOf(
            current.index - (minSpan - 1).coerceAtLeast(0),
            days.minOfOrNull { it.yearMonth.index } ?: current.index,
        )
        return (newest downTo oldest).map { YearMonth.fromIndex(it) }
    }

    fun daysInMonth(ym: YearMonth): Int =
        Calendar.getInstance().apply { clear(); set(ym.year, ym.month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)

    /**
     * Cells of a month grid, row by row, 7 per row: null for the blanks before the 1st and after the
     * last day. [firstDayOfWeek] is a [Calendar] constant (SUNDAY = 1, MONDAY = 2).
     */
    fun monthCells(ym: YearMonth, firstDayOfWeek: Int): List<Int?> {
        val first = Calendar.getInstance().apply { clear(); set(ym.year, ym.month, 1) }
        val lead = (first[Calendar.DAY_OF_WEEK] - firstDayOfWeek + 7) % 7
        val days = daysInMonth(ym)
        val total = (lead + days + 6) / 7 * 7
        return List(total) { i -> (i - lead + 1).takeIf { it in 1..days } }
    }

    /** Photo aspect ratio (width / height), clamped so a panorama or a sliver stays usable. */
    fun ratioOf(moment: Moment, min: Float = 0.5f, max: Float = 2f): Float =
        if (moment.width > 0 && moment.height > 0) (moment.width.toFloat() / moment.height).coerceIn(min, max) else 1f

    /**
     * The same pose for the same [seed] on every run, so a photo does not jump when the list refreshes.
     * [maxTilt] is the biggest rotation in degrees; [salt] gives a different pose to the same seed.
     */
    fun poseFor(seed: String, maxTilt: Float, salt: Int = 0): StickPose {
        var h = seed.hashCode() * 31 + salt
        fun next(): Float {
            h = h * 1103515245 + 12345
            return ((h ushr 8) and 0xFFFF) / 65535f * 2f - 1f
        }
        return StickPose(rotation = next() * maxTilt, dx = next(), dy = next())
    }
}
