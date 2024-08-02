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

package com.loohp.hkbuseta.common.utils

import kotlinx.datetime.LocalTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

inline fun <T> KSerializer<T>.forList(): KSerializer<List<T>> {
    return ListSerializer(this)
}

object LocalTimeSpecialSerializer : KSerializer<LocalTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalTimeSpecial", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalTime {
        return decoder.decodeString().split(":").let { LocalTime(it[0].toInt(), it[1].toInt()) }
    }

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString("${value.hour.pad(2)}:${value.minute.pad(2)}")
    }
}

object InvalidAsNullLocalTimeSpecialSerializer : KSerializer<LocalTime?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EmptyAsNullLocalTimeSpecial", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalTime? {
        return decoder.decodeString().split(":").let {
            if (it.size == 2) {
                val hour = it[0].toIntOrNull()
                val min = it[1].toIntOrNull()
                if (hour != null && min != null && hour in 0..23 && min in 0..59) {
                    LocalTime(hour, min)
                } else null
            } else null
        }
    }

    override fun serialize(encoder: Encoder, value: LocalTime?) {
        encoder.encodeString(value?.let { "${it.hour.pad(2)}:${it.minute.pad(2)}" }?: "")
    }
}

object NullAsEmptyStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NullAsEmptyString", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeNullableSerializableValue(String.serializer())?: ""
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

object IntOrStringAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntOrStringAsString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeSerializableValue(JsonElement.serializer()).jsonPrimitive.content
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

object IntOrStringAsIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntOrStringAsInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeSerializableValue(JsonElement.serializer()).jsonPrimitive.int
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeString(value.toString())
    }
}