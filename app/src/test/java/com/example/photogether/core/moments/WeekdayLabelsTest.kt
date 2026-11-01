package com.example.photogether.core.moments

import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekdayLabelsTest {

    private val vi = Locale.forLanguageTag("vi")

    @Test
    fun english_sundayFirst() {
        assertEquals(
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"),
            WeekdayLabels.of(Locale.ENGLISH, Calendar.SUNDAY),
        )
    }

    @Test
    fun english_mondayFirst() {
        assertEquals(
            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
            WeekdayLabels.of(Locale.ENGLISH, Calendar.MONDAY),
        )
    }

    @Test
    fun vietnamese_sundayFirst_isDistinctAndStartsWithCn() {
        val labels = WeekdayLabels.of(vi, Calendar.SUNDAY)
        assertEquals(listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7"), labels)
        assertEquals(7, labels.toSet().size)
    }

    @Test
    fun vietnamese_mondayFirst_endsWithCn() {
        assertEquals(listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN"), WeekdayLabels.of(vi, Calendar.MONDAY))
    }
}
