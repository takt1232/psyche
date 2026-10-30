package com.psyche.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formats {
    private val locale = Locale("en", "PH")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy", locale)
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", locale)
    private val monthShortFormatter = DateTimeFormatter.ofPattern("MMM", locale)
    private val dayFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
    private val fullDayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    private val weekdayDayFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", locale)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", locale)
    private val clockFormatter = DateTimeFormatter.ofPattern("h:mm", locale)
    private val periodFormatter = DateTimeFormatter.ofPattern("a", locale)

    fun monthUpper(month: YearMonth): String =
        month.format(monthFormatter).uppercase(locale)

    fun monthYear(month: YearMonth): String =
        month.format(monthYearFormatter).uppercase(locale)

    fun monthShort(month: YearMonth): String =
        month.format(monthShortFormatter).uppercase(locale)

    fun weekdayDate(instant: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(instant).atZone(zone).format(weekdayDayFormatter).uppercase(locale)

    fun monthTitle(month: YearMonth): String {
        val name = month.format(monthFormatter)
        return "$name ${month.year}"
    }

    fun shortDate(instant: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(instant).atZone(zone).format(dayFormatter).uppercase(locale)

    fun fullDate(instant: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(instant).atZone(zone).format(fullDayFormatter)

    fun time(instant: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(instant).atZone(zone).format(timeFormatter)

    /** Clock face only, for the narrow time column. Pair with [timePeriod]. */
    fun clock(instant: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(instant).atZone(zone).format(clockFormatter)

    fun timePeriod(instant: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(instant).atZone(zone).format(periodFormatter)

    fun entryCount(count: Int): String = "$count ${if (count == 1) "ENTRY" else "ENTRIES"}"

    fun dayLabel(
        instant: Long,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val date = Instant.ofEpochMilli(instant).atZone(zone).toLocalDate()
        return when (date) {
            today -> "TODAY"
            today.minusDays(1) -> "YESTERDAY"
            else -> date.format(dayFormatter).uppercase(locale)
        }
    }

    fun sequence(month: YearMonth): String = "%03d".format(month.monthValue)

    fun periodReference(month: YearMonth): String {
        val year = month.year.toString().takeLast(2)
        val monthPart = month.monthValue.toString().padStart(2, '0')
        return "PSYCHE-$year$monthPart"
    }

    fun monthKey(month: YearMonth): String = month.toString()

    fun parseMonth(value: String): YearMonth? = runCatching { YearMonth.parse(value) }.getOrNull()
}
