/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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
package com.loohp.hkbuseta.utils

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


fun toRadians(deg: Double): Double = deg / 180.0 * PI

fun findDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val lat1Rad = toRadians(lat1)
    val lat2Rad = toRadians(lat2)
    val lng1Rad = toRadians(lng1)
    val lng2Rad = toRadians(lng2)

    val dLon = lng2Rad - lng1Rad
    val dLat = lat2Rad - lat1Rad
    val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)

    val c = 2 * asin(sqrt(a))

    return c * 6371
}
