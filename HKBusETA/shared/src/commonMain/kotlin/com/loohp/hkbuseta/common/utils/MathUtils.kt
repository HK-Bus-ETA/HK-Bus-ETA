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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


private val chineseDigits = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
private val chinesePositions = arrayOf("", "十", "百", "千", "萬", "十萬", "百萬", "千萬", "億", "十億", "百億", "千億")

fun Int.toChineseNumber(isQuantity: Boolean = false): String {
    val charArray = toString().toCharArray()
    var result = ""
    var prevIsZero = false
    for (i in charArray.indices) {
        val ch = charArray[i].toString().toInt()
        if (ch != 0 && !prevIsZero) {
            result += chineseDigits[ch] + chinesePositions[charArray.size - i - 1]
        } else if (ch == 0) {
            prevIsZero = true
        } else if (ch != 0) {
            result += "零" + chineseDigits[ch] + chinesePositions[charArray.size - i - 1]
            prevIsZero = false
        }
    }
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

inline fun Float.floorToInt(): Int = floor(this).toInt()
inline fun Float.ceilToInt(): Int = ceil(this).toInt()