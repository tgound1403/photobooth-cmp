package com.example.photogether.core.moments

import java.text.DateFormatSymbols
import java.util.Locale

/**
 * Short, distinct weekday headers for the calendar, starting on [firstDayOfWeek]
 * ([java.util.Calendar] constant, 1 = Sunday). Pure JVM, so it can be unit tested.
 *
 * English gives SUN MON TUE. Names such as Vietnamese "Th 2" are shortened to "T2", keeping the
 * digit that tells the days apart. If the result still has duplicates, the whitespace-free
 * names are used in full.
 */
object WeekdayLabels {

    private val LETTERS_THEN_DIGITS = Regex("""^(\p{L}+)(\d+)$""")

    fun of(locale: Locale, firstDayOfWeek: Int): List<String> {
        val names = DateFormatSymbols.getInstance(locale).shortWeekdays // index 1 = Sunday
        val stripped = List(7) { i ->
            val dow = (firstDayOfWeek - 1 + i) % 7 + 1
            names[dow].filterNot { it.isWhitespace() }
        }
        fun List<String>.upper() = map { it.uppercase(locale) }

        val compact = stripped.map { name ->
            val m = LETTERS_THEN_DIGITS.matchEntire(name)
            if (m != null) m.groupValues[1].take(1) + m.groupValues[2] else name.take(3)
        }.upper()
        if (compact.toSet().size == 7) return compact
        return stripped.upper()
    }
}
