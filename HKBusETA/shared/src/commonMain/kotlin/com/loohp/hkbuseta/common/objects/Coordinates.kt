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
package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.findBearing
import com.loohp.hkbuseta.common.utils.findDistance
import com.loohp.hkbuseta.common.utils.optDouble
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.writeDouble
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
@Immutable
open class Coordinates(
    val lat: Double,
    val lng: Double
) : JSONSerializable, IOSerializable {

    companion object {

        fun deserialize(json: JsonObject): Coordinates {
            val lat = json.optDouble("lat")
            val lng = json.optDouble("lng")
            return Coordinates(lat, lng)
        }

        suspend fun deserialize(input: ByteReadChannel): Coordinates {
            val lat = input.readDouble()
            val lng = input.readDouble()
            return Coordinates(lat, lng)
        }

        fun fromArray(array: DoubleArray): Coordinates {
            return Coordinates(array[0], array[1])
        }

        fun fromJsonArray(array: JsonArray, flip: Boolean = false): Coordinates {
            return if (flip) {
                Coordinates(array[1].jsonPrimitive.double, array[0].jsonPrimitive.double)
            } else {
                Coordinates(array[0].jsonPrimitive.double, array[1].jsonPrimitive.double)
            }
        }

    }

    infix fun distance(other: Coordinates): Double {
        return findDistance(lat, lng, other.lat, other.lng)
    }

    infix fun bearingTo(other: Coordinates): Double {
        return findBearing(other.lat, other.lng, lat, lng)
    }

    fun toArray(): DoubleArray {
        return doubleArrayOf(lat, lng)
    }

    operator fun component1(): Double {
        return lat
    }

    operator fun component2(): Double {
        return lng
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("lat", lat)
            put("lng", lng)
        }
    }

    override fun serialize(out: BytePacketBuilder) {
        out.writeDouble(lat)
        out.writeDouble(lng)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Coordinates) return false

        if (lat != other.lat) return false
        return lng == other.lng
    }

    override fun hashCode(): Int {
        var result = lat.hashCode()
        result = 31 * result + lng.hashCode()
        return result
    }

    override fun toString(): String {
        return "Coordinates(lat=$lat, lng=$lng)"
    }

}
