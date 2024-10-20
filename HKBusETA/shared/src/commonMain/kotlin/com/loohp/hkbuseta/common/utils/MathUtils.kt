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

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


private val chineseDigits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
private val chinesePositions = listOf("", "十", "百", "千", "萬", "十萬", "百萬", "千萬", "億", "十億", "百億", "千億")

fun Int.toChineseNumber(isQuantity: Boolean = false): String {
    val string = toString()
    val stringBuilder = StringBuilder()
    var prevIsZero = false
    for (i in string.indices) {
        val ch = string[i].toString().toInt()
        if (ch != 0 && !prevIsZero) {
            stringBuilder.append(chineseDigits[ch] + chinesePositions[string.length - i - 1])
        } else if (ch == 0) {
            prevIsZero = true
        } else if (ch != 0) {
            stringBuilder.append("零" + chineseDigits[ch] + chinesePositions[string.length - i - 1])
            prevIsZero = false
        }
    }
    var result = stringBuilder.toString()
    if (this < 100) {
        result = result.replace("一十", "十")
    }
    if (isQuantity && result == "二") {
        result = "兩"
    }
    return result
}

fun String?.parseIntOr(otherwise: Int = 0): Int {
    return try {
        this?.toIntOrNull()?: otherwise
    } catch (e: NumberFormatException) {
        otherwise
    }
}

fun String?.parseLongOr(otherwise: Long = 0): Long {
    return try {
        this?.toLongOrNull()?: otherwise
    } catch (e: NumberFormatException) {
        otherwise
    }
}

inline val Double.radians: Double get() = this / 180.0 * PI

inline fun findDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val lat1Rad = lat1.radians
    val lat2Rad = lat2.radians
    val lng1Rad = lng1.radians
    val lng2Rad = lng2.radians

    val dLon = lng2Rad - lng1Rad
    val dLat = lat2Rad - lat1Rad
    val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)

    val c = 2 * asin(sqrt(a))

    return c * 6371
}

inline fun findDistanceToSegment(latP: Double, lngP: Double, lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val a = findDistance(latP, lngP, lat1, lng1)
    val b = findDistance(latP, lngP, lat2, lng2)
    val c = findDistance(lat1, lng1, lat2, lng2)

    val s = (a + b + c) / 2
    val area = sqrt(s * (s - a) * (s - b) * (s - c))

    val distanceToLine = (2 * area) / c

    return distanceToLine
}

inline fun findBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dLon = lng2 - lng1

    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

    var brng = atan2(y, x)

    brng = brng.toDegrees()
    brng = (brng + 360) % 360

    return -(brng - 180) + 180
}

inline fun Double.toRadians(): Double = this / 180.0 * PI
inline fun Double.toDegrees(): Double = this * 180.0 / PI

inline fun Float.floorToInt(): Int = floor(this).toInt()
inline fun Float.ceilToInt(): Int = ceil(this).toInt()

inline val IntRange.middle: Int get() = (last + first) / 2
inline val IntRange.isRange: Boolean get() = last != first
inline fun Int.asRange(): IntRange = this..this
inline infix fun IntRange.maxDifference(other: IntRange): Int = max(other.last - first, last - other.first)
inline infix fun IntRange.merge(other: IntRange): IntRange = min(first, other.first)..max(last, other.last)
inline fun IntRange.toDisplayText(): String = if (isRange) "$first-$last" else first.toString()