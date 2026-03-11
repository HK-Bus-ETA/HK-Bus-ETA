/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.common.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days


expect val hongKongZoneId: String

val hongKongTimeZone: TimeZone = TimeZone.of(hongKongZoneId)
val secondsInDay: Int = 1.days.inWholeSeconds.toInt()

private val weekdayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

fun Int.toEnglishWeekdayNames(publicHolidays: Boolean = false): String {
    val value = (this - 1) % 7
    return weekdayNames[value].let { if (value == 6 && publicHolidays) "$it & Public Holidays" else it }
}

fun Int.toChineseWeekdayNumber(publicHolidays: Boolean = false): String {
    val value = (this % 7).let { if (it == 0) 7 else it }
    return value.toChineseNumber().let { if (value == 7) (if (publicHolidays) "日及公眾假期" else "日") else it }
}

fun currentInstant(): Instant {
    return Clock.System.now()
}

val currentEpochSeconds: Long get() {
    return currentInstant().epochSeconds
}

fun currentTimeMillis(): Long {
    return currentInstant().toEpochMilliseconds()
}

fun nextScheduledDataUpdateMillis(): Long {
    val currentTime = currentLocalDateTime()
    val today430 = LocalDateTime(currentTime.year, currentTime.month, currentTime.dayOfMonth, 4, 30)
    val today430Milli = today430.toInstant(hongKongTimeZone).toEpochMilliseconds()
    return if (today430 > currentTime) today430Milli else today430Milli + 86400000
}

fun currentLocalDateTime(): LocalDateTime {
    return currentInstant().toLocalDateTime(hongKongTimeZone)
}

fun currentLocalDateTime(plusDuration: Duration): LocalDateTime {
    return currentInstant().plus(plusDuration).toLocalDateTime(hongKongTimeZone)
}

val LocalDateTime.epochSeconds: Long get() {
    return toInstant(hongKongTimeZone).epochSeconds
}

fun LocalDateTime.toEpochMilliseconds(): Long {
    return toInstant(hongKongTimeZone).toEpochMilliseconds()
}

fun LocalTime.toLocalDateTime(date: LocalDate = currentLocalDateTime().date): LocalDateTime {
    return LocalDateTime(date, this)
}

fun LocalTime.nextLocalDateTimeAfter(after: LocalDateTime): LocalDateTime {
    val timeAtDate = LocalDateTime(after.date, this)
    return if (timeAtDate < after) timeAtDate + 1.days else timeAtDate
}

fun LocalTime.previousLocalDateTimeBefore(before: LocalDateTime): LocalDateTime {
    val timeAtDate = LocalDateTime(before.date, this)
    return if (timeAtDate > before) timeAtDate - 1.days else timeAtDate
}

operator fun LocalDateTime.minus(other: LocalDateTime): Duration {
    return this.toInstant(hongKongTimeZone) - other.toInstant(hongKongTimeZone)
}

operator fun LocalDateTime.plus(duration: Duration): LocalDateTime {
    return (toInstant(hongKongTimeZone) + duration).toLocalDateTime(hongKongTimeZone)
}

operator fun LocalDateTime.minus(duration: Duration): LocalDateTime {
    return (toInstant(hongKongTimeZone) - duration).toLocalDateTime(hongKongTimeZone)
}

operator fun LocalTime.minus(earlierTime: LocalTime): Duration {
    val today = currentLocalDateTime().date
    val thisDated = atDate(today)
    val earlierTimeDated = earlierTime.atDate(if (earlierTime < this) today else (today - DatePeriod(days = 1)))
    return thisDated - earlierTimeDated
}

fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(hongKongTimeZone)
}

fun LocalTime.compareToBy(other: LocalTime, midnight: LocalTime = LocalTime(0, 0)): Int {
    val today = currentLocalDateTime().date
    val tomorrow = today + DatePeriod(days = 1)
    val thisDated = atDate(if (this < midnight) tomorrow else today)
    val otherDated = other.atDate(if (other < midnight) tomorrow else today)
    return thisDated.compareTo(otherDated)
}

fun LocalTime.isBetweenInclusive(start: LocalTime, end: LocalTime): Boolean {
    val timeSec = toSecondOfDay()
    val startSec = start.toSecondOfDay()
    val endSec = end.toSecondOfDay().let { if (it < startSec) it + secondsInDay else it }
    return (-1..1).any { sign ->
        val offset = secondsInDay * sign
        timeSec in (startSec + offset)..(endSec + offset)
    }
}

fun LocalTime.isBetweenExclusive(start: LocalTime, end: LocalTime): Boolean {
    val timeSec = toSecondOfDay()
    val startSec = start.toSecondOfDay()
    val endSec = end.toSecondOfDay().let { if (it < startSec) it + secondsInDay else it }
    return (-1..1).any { sign ->
        val offset = secondsInDay * sign
        timeSec in (startSec + 1 + offset) until (endSec + offset)
    }
}

fun Pair<LocalTime, LocalTime>.isBetweenInclusive(start: LocalTime, end: LocalTime): Boolean {
    val timeStartSec = first.toSecondOfDay()
    val timeEndSec = second.toSecondOfDay().let { if (it < timeStartSec) it + secondsInDay else it }
    val startSec = start.toSecondOfDay()
    val endSec = end.toSecondOfDay().let { if (it < startSec) it + secondsInDay else it }
    return (-1..1).any { sign ->
        val offset = secondsInDay * sign
        startSec + offset <= timeStartSec && timeEndSec <= endSec + offset
    }
}

fun Pair<LocalTime, LocalTime>.intersects(start: LocalTime, end: LocalTime): Pair<LocalTime, LocalTime>? {
    val timeStartSec = first.toSecondOfDay()
    val timeEndSec = second.toSecondOfDay().let { if (it < timeStartSec) it + secondsInDay else it }
    val startSec = start.toSecondOfDay()
    val endSec = end.toSecondOfDay().let { if (it < startSec) it + secondsInDay else it }
    return when {
        startSec <= timeEndSec && endSec >= timeStartSec -> {
            LocalTime.fromSecondOfDay(max(timeStartSec, startSec) % secondsInDay) to LocalTime.fromSecondOfDay(min(timeEndSec, endSec) % secondsInDay)
        }
        startSec + secondsInDay <= timeEndSec && endSec + secondsInDay >= timeStartSec -> {
            LocalTime.fromSecondOfDay(max(timeStartSec, startSec + secondsInDay) % secondsInDay) to LocalTime.fromSecondOfDay(min(timeEndSec, endSec + secondsInDay) % secondsInDay)
        }
        else -> null
    }
}

inline fun String.parseInstant(): Instant {
    return Instant.parse(this)
}

inline fun String.parseLocalDateTime(): LocalDateTime {
    return LocalDateTime.parse(this)
}

inline val DayOfWeek.sundayZeroDayNumber get() = isoDayNumber % 7

fun LocalTime.plus(duration: Duration): LocalTime {
    val date = currentLocalDateTime().date
    return toLocalDateTime(date).plus(duration).time
}