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
package com.loohp.hkbuseta.objects

import androidx.compose.runtime.Immutable
import com.loohp.hkbuseta.utils.IOSerializable
import com.loohp.hkbuseta.utils.JSONSerializable
import com.loohp.hkbuseta.utils.readNullable
import com.loohp.hkbuseta.utils.readString
import com.loohp.hkbuseta.utils.writeNullable
import com.loohp.hkbuseta.utils.writeString
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.text.Charsets.UTF_8

@Immutable
class Stop(
    val location: Coordinates,
    val name: BilingualText,
    val remark: BilingualText?,
    val kmbBbiId: String?
) : JSONSerializable, IOSerializable {

    companion object {

        fun deserialize(json: JSONObject): Stop {
            val location = Coordinates.deserialize(json.optJSONObject("location")!!)
            val name = BilingualText.deserialize(json.optJSONObject("name")!!)
            val remark = if (json.has("remark")) BilingualText.deserialize(json.optJSONObject("remark")!!) else null
            val kmbBbiId = if (json.has("kmbBbiId")) json.optString("kmbBbiId") else null
            return Stop(location, name, remark, kmbBbiId)
        }

        fun deserialize(inputStream: InputStream): Stop {
            val input = DataInputStream(inputStream)
            val location = Coordinates.deserialize(input)
            val name = BilingualText.deserialize(input)
            val remark = input.readNullable { BilingualText.deserialize(it) }
            val kmbBbiId = input.readNullable { it.readString(UTF_8) }
            return Stop(location, name, remark, kmbBbiId)
        }

    }

    override fun serialize(): JSONObject {
        val json = JSONObject()
        json.put("location", location.serialize())
        json.put("name", name.serialize())
        if (remark != null) {
            json.put("remark", remark.serialize())
        }
        if (kmbBbiId != null) {
            json.put("kmbBbiId", kmbBbiId)
        }
        return json
    }

    override fun serialize(outputStream: OutputStream) {
        val out = DataOutputStream(outputStream)
        location.serialize(out)
        name.serialize(out)
        out.writeNullable(remark) { o, v -> v.serialize(o) }
        out.writeNullable(kmbBbiId) { o, v -> o.writeString(v, UTF_8) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Stop

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
