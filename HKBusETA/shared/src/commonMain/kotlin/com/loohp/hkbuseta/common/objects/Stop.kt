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
import com.loohp.hkbuseta.common.utils.SmallSize
import com.loohp.hkbuseta.common.utils.buildFormattedString
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.readNullable
import com.loohp.hkbuseta.common.utils.readString
import com.loohp.hkbuseta.common.utils.writeNullable
import com.loohp.hkbuseta.common.utils.writeString
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.charsets.Charsets.UTF_8
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
@Immutable
class Stop(
    val location: Coordinates,
    val name: BilingualText,
    val remark: BilingualText? = null,
    val kmbBbiId: String? = null
) : JSONSerializable, IOSerializable {

    companion object {

        fun deserialize(json: JsonObject): Stop {
            val location = Coordinates.deserialize(json.optJsonObject("location")!!)
            val name = BilingualText.deserialize(json.optJsonObject("name")!!)
            val remark = if (json.contains("remark")) BilingualText.deserialize(json.optJsonObject("remark")!!) else null
            val kmbBbiId = if (json.contains("kmbBbiId")) json.optString("kmbBbiId") else null
            return Stop(location, name, remark, kmbBbiId)
        }

        suspend fun deserialize(input: ByteReadChannel): Stop {
            val location = Coordinates.deserialize(input)
            val name = BilingualText.deserialize(input)
            val remark = input.readNullable { BilingualText.deserialize(it) }
            val kmbBbiId = input.readNullable { it.readString(UTF_8) }
            return Stop(location, name, remark, kmbBbiId)
        }

    }

    val remarkedName: BilingualFormattedText by lazy { remark?.let {
        BilingualFormattedText(
            buildFormattedString {
                append(name.zh)
                if (!it.zh.startsWith(" ")) {
                    append(" ")
                }
                append(it.zh, SmallSize)
            },
            buildFormattedString {
                append(name.en)
                if (!it.en.startsWith(" ")) {
                    append(" ")
                }
                append(it.en, SmallSize)
            }
        )
    }?: name.asFormattedText() }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("location", location.serialize())
            put("name", name.serialize())
            if (remark != null) {
                put("remark", remark.serialize())
            }
            if (kmbBbiId != null) {
                put("kmbBbiId", kmbBbiId)
            }
        }
    }

    override suspend fun serialize(out: ByteWriteChannel) {
        location.serialize(out)
        name.serialize(out)
        out.writeNullable(remark) { o, v -> v.serialize(o) }
        out.writeNullable(kmbBbiId) { o, v -> o.writeString(v, UTF_8) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Stop) return false

        if (location != other.location) return false
        if (name != other.name) return false
        if (remark != other.remark) return false
        return kmbBbiId == other.kmbBbiId
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (remark?.hashCode() ?: 0)
        result = 31 * result + (kmbBbiId?.hashCode() ?: 0)
        return result
    }

}
