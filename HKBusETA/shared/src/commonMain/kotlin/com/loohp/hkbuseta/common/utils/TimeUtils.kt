/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days


private val minuteState: MutableStateFlow<LocalDateTime> = MutableStateFlow(currentLocalDateTime())
private val minuteStateJob: Job = CoroutineScope(Dispatchers.IO).launch {
    while (true) {
        minuteState.value = currentLocalDateTime().let { LocalDateTime(it.year, it.month, it.dayOfMonth, it.hour, it.minute) }
        delay(1000)
    }
}

expect val hongKongZoneId: String

val hongKongTimeZone: TimeZone get() = TimeZone.of(hongKongZoneId)

val currentMinuteState: StateFlow<LocalDateTime> get() = minuteState

private val weekdayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

fun Int.toEnglishWeekdayNames(publicHolidays: Boolean = false): String {
    val value = (this - 1) % 7
    return weekdayNames[value].let { if (value == 6 && publicHolidays) "$it & Public Holidays" else it }
}

fun Int.toChineseWeekdayNumber(publicHolidays: Boolean = false): String {
    val value = (this % 7).let { if (it == 0) 7 else it }
    return value.toChineseNumber().let { if (value == 7) (if (publicHolidays) "日及公眾假期" else "日") else it }
}

val currentEpochSeconds: Long get() {
    return Clock.System.now().epochSeconds
}

fun currentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

fun nextScheduledDataUpdateMillis(): Long {
    val hongKongZone = hongKongTimeZone
    val currentTime = Clock.System.now().toLocalDateTime(hongKongZone)
    val today430 = LocalDateTime(currentTime.year, currentTime.month, currentTime.dayOfMonth, 4, 30)
    val today430Milli = today430.toInstant(hongKongZone).toEpochMilliseconds()
    return if (today430 > currentTime) today430Milli else today430Milli + 86400000
}

fun currentLocalDateTime(): LocalDateTime {
    return Clock.System.now().toLocalDateTime(hongKongTimeZone)
}

fun currentLocalDateTime(plusDuration: Duration): LocalDateTime {
    return Clock.System.now().plus(plusDuration).toLocalDateTime(hongKongTimeZone)
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

operator fun LocalDateTime.minus(other: LocalDateTime): Duration {
    val hongKongZone = hongKongTimeZone
    return this.toInstant(hongKongZone) - other.toInstant(hongKongZone)
}

operator fun LocalDateTime.plus(duration: Duration): LocalDateTime {
    val hongKongZone = hongKongTimeZone
    return (toInstant(hongKongZone) + duration).toLocalDateTime(hongKongZone)
}

operator fun LocalDateTime.minus(duration: Duration): LocalDateTime {
    val hongKongZone = hongKongTimeZone
    return (toInstant(hongKongZone) - duration).toLocalDateTime(hongKongZone)
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
    return when {
        start == end -> this == start
        start < end -> start < this && this < end
        else -> this > start || this < end
    }
}

inline fun String.parseInstant(): Instant {
    return Instant.parse(this)
}

inline fun String.parseLocalDateTime(): LocalDateTime {
    return LocalDateTime.parse(this)
}

inline val DayOfWeek.sundayZeroDayNumber get() = isoDayNumber % 7