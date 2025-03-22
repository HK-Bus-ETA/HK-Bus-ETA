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

import kotlin.math.roundToInt


fun interpolateColor(startColor: Long, endColor: Long, fraction: Float): Long {
    val startA = (startColor shr 24 and 0xFF) / 255.0
    val startR = (startColor shr 16 and 0xFF) / 255.0
    val startG = (startColor shr 8 and 0xFF) / 255.0
    val startB = (startColor and 0xFF) / 255.0

    val endA = (endColor shr 24 and 0xFF) / 255.0
    val endR = (endColor shr 16 and 0xFF) / 255.0
    val endG = (endColor shr 8 and 0xFF) / 255.0
    val endB = (endColor and 0xFF) / 255.0

    val a = (startA + (endA - startA) * fraction).coerceIn(0.0, 1.0)
    val r = (startR + (endR - startR) * fraction).coerceIn(0.0, 1.0)
    val g = (startG + (endG - startG) * fraction).coerceIn(0.0, 1.0)
    val b = (startB + (endB - startB) * fraction).coerceIn(0.0, 1.0)

    val colorA = (a * 255.0).roundToInt() shl 24
    val colorR = (r * 255.0).roundToInt() shl 16
    val colorG = (g * 255.0).roundToInt() shl 8
    val colorB = (b * 255.0).roundToInt()

    return (colorA or colorR or colorG or colorB).toLong()
}