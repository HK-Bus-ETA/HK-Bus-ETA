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

package com.loohp.hkbuseta.common.objects

enum class ETADisplayMode(
    val hasClockTime: Boolean,
    val wearableShortTextClockTime: Boolean
) {
    COUNTDOWN(
        hasClockTime = false,
        wearableShortTextClockTime = false
    ),
    CLOCK_TIME(
        hasClockTime = true,
        wearableShortTextClockTime = true
    ),
    CLOCK_TIME_WITH_COUNTDOWN(
        hasClockTime = true,
        wearableShortTextClockTime = false
    );

    val next: ETADisplayMode get() = entries[(ordinal + 1) % entries.size]
}

inline val Boolean.etaDisplayMode: ETADisplayMode get() = if (this) ETADisplayMode.CLOCK_TIME else ETADisplayMode.COUNTDOWN
inline val String.etaDisplayMode: ETADisplayMode get() = ETADisplayMode.entries.firstOrNull { it.name.equals(this, true) }?: ETADisplayMode.COUNTDOWN