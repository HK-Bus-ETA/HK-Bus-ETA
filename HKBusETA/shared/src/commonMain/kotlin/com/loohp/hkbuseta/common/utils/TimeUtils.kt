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

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration


fun currentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

fun nextScheduledDataUpdateMillis(): Long {
    val hongKongZone = TimeZone.of("Asia/Hong_Kong")
    val currentTime = Clock.System.now().toLocalDateTime(hongKongZone)
    val today430 = LocalDateTime(currentTime.year, currentTime.month, currentTime.dayOfMonth, 4, 30)
    val today430Milli = today430.toInstant(hongKongZone).toEpochMilliseconds()
    return if (today430 > currentTime) today430Milli else today430Milli + 86400000
}

fun currentLocalDateTime(): LocalDateTime {
    return Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Hong_Kong"))
}

fun currentLocalDateTime(plusDuration: Duration): LocalDateTime {
    return Clock.System.now().plus(plusDuration).toLocalDateTime(TimeZone.of("Asia/Hong_Kong"))
}