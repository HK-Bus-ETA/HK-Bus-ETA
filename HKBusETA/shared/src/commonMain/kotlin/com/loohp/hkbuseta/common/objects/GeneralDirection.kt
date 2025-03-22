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

import com.loohp.hkbuseta.common.objects.GeneralDirection.Companion.ORDER
import kotlin.math.absoluteValue
import kotlin.math.min

enum class GeneralDirection(
    val bearing: Double
) {
    NORTH(0.0),
    EAST(90.0),
    SOUTH(180.0),
    WEST(270.0);

    companion object {
        val ORDER = listOf(null, NORTH, SOUTH, EAST, WEST)
    }

    val opposite: GeneralDirection get() = when (this) {
        NORTH -> SOUTH
        EAST -> WEST
        SOUTH -> NORTH
        WEST -> EAST
    }

    fun deviation(angle: Number): Double {
        val a = angle.toDouble() % 360
        val results = listOf(a - bearing, a - (bearing + 360))
        return results.minBy { it.absoluteValue }
    }
}

val GeneralDirection?.displayName: BilingualText get() = when (this) {
    GeneralDirection.NORTH -> "北行" withEn "N"
    GeneralDirection.EAST -> "東行" withEn "E"
    GeneralDirection.SOUTH -> "南行" withEn "S"
    GeneralDirection.WEST -> "西行" withEn "W"
    null -> "全部" withEn "All"
}

val GeneralDirection?.extendedDisplayName: BilingualText get() = when (this) {
    GeneralDirection.NORTH -> "北行" withEn "Northbound"
    GeneralDirection.EAST -> "東行" withEn "Eastbound"
    GeneralDirection.SOUTH -> "南行" withEn "Southbound"
    GeneralDirection.WEST -> "西行" withEn "Westbound"
    null -> "全部" withEn "All Routes"
}

fun GeneralDirection?.next(availableDirections: Collection<GeneralDirection>): GeneralDirection? {
    var current = this
    for (i in ORDER.indices) {
        val next = ORDER[(ORDER.indexOf(current) + 1) % ORDER.size]
        if (next == null || availableDirections.contains(next)) {
            return next
        }
        current = next
    }
    return null
}

fun Number.toGeneralDirection(availableDirections: Collection<GeneralDirection> = GeneralDirection.entries): GeneralDirection {
    val bearing = toDouble() % 360.0
    return availableDirections.minBy { min((it.bearing - bearing).absoluteValue, ((it.bearing + 360.0) - bearing).absoluteValue) }
}

enum class GeneralDirectionAxis(
    val directions: Set<GeneralDirection>
) {
    NORTH_SOUTH(GeneralDirection.NORTH, GeneralDirection.SOUTH),
    EAST_WEST(GeneralDirection.EAST, GeneralDirection.WEST);

    constructor(vararg directions: GeneralDirection): this(directions.toSet())
}

val GeneralDirection.axis: GeneralDirectionAxis get() = GeneralDirectionAxis.entries.first { it.directions.contains(this) }