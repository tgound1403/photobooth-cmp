package com.example.photogether.core.moments

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentCalendarTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun millis(y: Int, m: Int, d: Int, h: Int = 12): Long =
        Calendar.getInstance(utc).apply { clear(); set(y, m, d, h, 0, 0) }.timeInMillis

    private fun moment(uri: String, at: Long, w: Int = 4, h: Int = 3, kind: MomentKind = MomentKind.CAMERA) =
        Moment(uri = uri, takenAt = at, width = w, height = h, kind = kind)

    @Test
    fun groupByDay_splitsOnLocalMidnight() {
        val late = moment("a", millis(2026, Calendar.OCTOBER, 3, 23))
        val early = moment("b", millis(2026, Calendar.OCTOBER, 4, 0))
        val grouped = MomentCalendar.groupByDay(listOf(late, early), utc)
        assertEquals(setOf(CalendarDay(2026, 9, 3), CalendarDay(2026, 9, 4)), grouped.keys)
    }

    @Test
    fun groupByDay_keepsInputOrderInsideADay() {
        val first = moment("a", millis(2026, 9, 3, 18))
        val second = moment("b", millis(2026, 9, 3, 9))
        val day = MomentCalendar.groupByDay(listOf(first, second), utc).getValue(CalendarDay(2026, 9, 3))
        assertEquals(listOf("a", "b"), day.map { it.uri })
    }

    @Test
    fun months_runsNewestToOldestWithoutGaps() {
        val days = listOf(CalendarDay(2026, 7, 9), CalendarDay(2026, 4, 2))
        val months = MomentCalendar.months(days, current = YearMonth(2026, 9), minSpan = 1)
        assertEquals(YearMonth(2026, 9), months.first())
        assertEquals(YearMonth(2026, 4), months.last())
        assertEquals(6, months.size)
    }

    @Test
    fun months_noPhotosGivesJustTheCurrentMonth() {
        assertEquals(listOf(YearMonth(2026, 9)), MomentCalendar.months(emptyList(), YearMonth(2026, 9), minSpan = 1))
    }

    @Test
    fun months_acrossNewYear() {
        val months = MomentCalendar.months(listOf(CalendarDay(2025, 11, 30)), YearMonth(2026, 1), minSpan = 1)
        assertEquals(listOf(YearMonth(2026, 1), YearMonth(2026, 0), YearMonth(2025, 11)), months)
    }

    @Test
    fun months_alwaysOffersAYearBack() {
        val months = MomentCalendar.months(emptyList(), YearMonth(2026, 9))
        assertEquals(12, months.size)
        assertEquals(YearMonth(2025, 10), months.last())
    }

    @Test
    fun months_oldPhotoBeyondTheMinimumStillWins() {
        val months = MomentCalendar.months(listOf(CalendarDay(2024, 0, 5)), YearMonth(2026, 9))
        assertEquals(YearMonth(2024, 0), months.last())
    }

    @Test
    fun monthCells_octoberFridayStartWithMondayFirst() {
        // 1 Oct 2026 is a Thursday: Monday-first grid has 3 blanks.
        val cells = MomentCalendar.monthCells(YearMonth(2026, Calendar.OCTOBER), Calendar.MONDAY)
        assertEquals(35, cells.size)
        assertEquals(listOf(null, null, null, 1), cells.take(4))
        assertEquals(31, cells.filterNotNull().max())
        assertEquals(0, cells.size % 7)
    }

    @Test
    fun monthCells_sundayFirstShiftsTheBlanks() {
        val cells = MomentCalendar.monthCells(YearMonth(2026, Calendar.OCTOBER), Calendar.SUNDAY)
        assertEquals(listOf(null, null, null, null, 1), cells.take(5))
    }

    @Test
    fun monthCells_leapFebruaryHasTwentyNineDays() {
        val cells = MomentCalendar.monthCells(YearMonth(2028, Calendar.FEBRUARY), Calendar.MONDAY)
        assertEquals(29, cells.filterNotNull().size)
    }

    @Test
    fun ratioOf_clampsAndHandlesMissingSize() {
        assertEquals(1f, MomentCalendar.ratioOf(moment("a", 0, w = 0, h = 0)), 0f)
        assertEquals(2f, MomentCalendar.ratioOf(moment("a", 0, w = 100, h = 10)), 0f)
        assertEquals(0.5f, MomentCalendar.ratioOf(moment("a", 0, w = 10, h = 100)), 0f)
        assertEquals(0.75f, MomentCalendar.ratioOf(moment("a", 0, w = 3, h = 4)), 0.0001f)
    }

    @Test
    fun poseFor_isRepeatableAndWithinBounds() {
        for (i in 0 until 200) {
            val pose = MomentCalendar.poseFor("content://media/$i", maxTilt = 7f)
            assertEquals(pose, MomentCalendar.poseFor("content://media/$i", maxTilt = 7f))
            assertTrue(pose.rotation in -7f..7f)
            assertTrue(pose.dx in -1f..1f && pose.dy in -1f..1f)
        }
    }

    @Test
    fun poseFor_differsAcrossSeedsAndSalts() {
        val poses = (0 until 20).map { MomentCalendar.poseFor("p$it", 10f) }.toSet()
        assertTrue(poses.size > 15)
        assertTrue(MomentCalendar.poseFor("x", 10f, salt = 1) != MomentCalendar.poseFor("x", 10f, salt = 2))
    }

    @Test
    fun drawer_splitsPhotoboothFromEverythingElse() {
        val list = listOf(
            moment("c", 3, kind = MomentKind.CAMERA),
            moment("p", 2, kind = MomentKind.PHOTOBOOTH),
            moment("e", 1, kind = MomentKind.EDITED),
        )
        assertEquals(listOf("p"), MomentDrawer.PHOTOBOOTH.filter(list).map { it.uri })
        assertEquals(listOf("c", "e"), MomentDrawer.SHOTS.filter(list).map { it.uri })
    }

    @Test
    fun kindOf_readsDisplayName() {
        assertEquals(MomentKind.PHOTOBOOTH, MomentQuery.kindOf("PhotoBooth_2026.jpg"))
        assertEquals(MomentKind.EDITED, MomentQuery.kindOf("Edited_1.jpg"))
        assertEquals(MomentKind.CAMERA, MomentQuery.kindOf("classic35_2026-10-17.jpg"))
        assertEquals(MomentKind.CAMERA, MomentQuery.kindOf(null))
        assertNull(MomentQuery.cameraIdOf("PhotoBooth_1.jpg"))
    }
}
