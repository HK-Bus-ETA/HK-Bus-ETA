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
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.readString
import com.loohp.hkbuseta.common.utils.writeString
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.charsets.Charsets.UTF_8
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


@Immutable
class BilingualText(val zh: String, val en: String) : JSONSerializable, IOSerializable {

    companion object {

        val EMPTY = BilingualText("", "")

        fun deserialize(json: JsonObject): BilingualText {
            val zh = json.optString("zh")
            val en = json.optString("en")
            return BilingualText(zh, en)
        }

        suspend fun deserialize(input: ByteReadChannel): BilingualText {
            val zh = input.readString(UTF_8)
            val en = input.readString(UTF_8)
            return BilingualText(zh, en)
        }

    }

    operator fun get(language: String): String {
        return if (language == "en") en else zh
    }

    operator fun component1(): String {
        return this.zh
    }

    operator fun component2(): String {
        return this.en
    }

    operator fun plus(other: BilingualText): BilingualText {
        return BilingualText(this.zh + other.zh, this.en + other.en)
    }

    operator fun plus(other: String): BilingualText {
        return BilingualText(this.zh + other, this.en + other)
    }

    override fun toString(): String {
        return "$zh $en"
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("zh", zh)
            put("en", en)
        }
    }

    override suspend fun serialize(out: ByteWriteChannel) {
        out.writeString(zh, UTF_8)
        out.writeString(en, UTF_8)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BilingualText

        if (zh != other.zh) return false
        return en == other.en
    }

    override fun hashCode(): Int {
        var result = zh.hashCode()
        result = 31 * result + en.hashCode()
        return result
    }

}
