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

import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.toString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.ceil

@Serializable(with = FareSerializer::class)
@Immutable
data class Fare(val value: Float): Number(), Comparable<Fare> {

    companion object {
        val ZERO = Fare(0F)
    }

    constructor(value: String) : this(value.toFloat())

    constructor(value: Int) : this(value.toFloat())

    val half: Fare by lazy { Fare(ceil(value * 5F) / 10F) }

    override fun toByte(): Byte {
        return value.toInt().toByte()
    }

    override fun toDouble(): Double {
        return value.toDouble()
    }

    override fun toFloat(): Float {
        return value
    }

    override fun toInt(): Int {
        return value.toInt()
    }

    override fun toLong(): Long {
        return value.toLong()
    }

    override fun toShort(): Short {
        return value.toInt().toShort()
    }

    override fun compareTo(other: Fare): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return toString(decimalPlaces = 1)
    }

    operator fun plus(other: Fare): Fare {
        return Fare(value + other.value)
    }

    operator fun minus(other: Fare): Fare {
        return Fare(value - other.value)
    }

    operator fun times(other: Fare): Fare {
        return Fare(value * other.value)
    }

    operator fun div(other: Fare): Fare {
        return Fare(value / other.value)
    }

}

object FareSerializer : KSerializer<Fare> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Fare", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Fare {
        return Fare(decoder.decodeSerializableValue(JsonElement.serializer()).jsonPrimitive.float)
    }

    override fun serialize(encoder: Encoder, value: Fare) {
        encoder.encodeString(value.value.toString(decimalPlaces = 1))
    }
}
