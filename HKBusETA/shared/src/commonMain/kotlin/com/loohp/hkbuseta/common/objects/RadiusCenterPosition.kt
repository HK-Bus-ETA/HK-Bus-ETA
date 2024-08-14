/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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

package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optFloat
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Immutable
class RadiusCenterPosition(
    lat: Double,
    lng: Double,
    val radius: Float
): Coordinates(lat, lng) {

    companion object {

        fun deserialize(json: JsonObject): RadiusCenterPosition {
            val lat = json.optDouble("lat")
            val lng = json.optDouble("lng")
            val radius = json.optFloat("radius")
            return RadiusCenterPosition(lat, lng, radius)
        }

    }

    operator fun component3(): Float {
        return radius
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("lat", lat)
            put("lng", lng)
            put("radius", radius)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RadiusCenterPosition) return false
        if (!super.equals(other)) return false

        return radius == other.radius
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + radius.hashCode()
        return result
    }
}

fun Coordinates.asRadiusCenterPosition(radius: Float = 0.3F): RadiusCenterPosition {
    return RadiusCenterPosition(lat, lng, radius)
}