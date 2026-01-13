package org.example.physiotrack.util

import kotlinx.datetime.*
import kotlin.math.abs


fun parseInstantOrNull(iso: String?): Instant? {
    if (iso.isNullOrBlank()) return null
    return runCatching { Instant.parse(iso) }.getOrNull()
}

fun sessionLocalDate(iso: String, tz: TimeZone = TimeZone.currentSystemDefault()): LocalDate? {
    val inst = parseInstantOrNull(iso) ?: return null
    return inst.toLocalDateTime(tz).date
}

fun currentStreakDays(sessionDates: Set<LocalDate>, today: LocalDate): Int {
    var d = today
    var streak = 0
    while (sessionDates.contains(d)) {
        streak += 1
        d = d.minus(DatePeriod(days = 1))
    }
    return streak
}

fun longestStreakDays(sessionDates: Set<LocalDate>): Int {
    if (sessionDates.isEmpty()) return 0
    val sorted = sessionDates.sorted()
    var best = 1
    var cur = 1
    for (i in 1 until sorted.size) {
        val prev = sorted[i - 1]
        val next = sorted[i]
        val gap = abs(next.toEpochDays() - prev.toEpochDays())
        if (gap == 1) {
            cur += 1
            if (cur > best) best = cur
        } else {
            cur = 1
        }
    }
    return best
}

fun weekStartMonday(date: LocalDate): LocalDate {
    val dow = date.dayOfWeek.isoDayNumber // Mon=1..Sun=7
    return date.minus(DatePeriod(days = dow - 1))
}

fun isInWeek(date: LocalDate, weekStartMonday: LocalDate): Boolean {
    val end = weekStartMonday.plus(DatePeriod(days = 6))
    return date >= weekStartMonday && date <= end
}

data class WeekBucket(
    val start: LocalDate,
    val end: LocalDate,
)

fun lastNWeeksBuckets(today: LocalDate, n: Int): List<WeekBucket> {
    require(n > 0)
    val thisWeekStart = weekStartMonday(today)
    return (0 until n).map { offset ->
        val start = thisWeekStart.minus(DatePeriod(days = 7 * (n - 1 - offset)))
        WeekBucket(start = start, end = start.plus(DatePeriod(days = 6)))
    }
}
