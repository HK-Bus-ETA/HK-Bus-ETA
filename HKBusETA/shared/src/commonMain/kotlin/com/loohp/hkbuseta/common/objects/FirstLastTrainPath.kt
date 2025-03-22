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
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.optString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
@Immutable
class FirstLastTrainPath(
    val id: String,
    val line: String? = null,
    val towards: String? = null
) : JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): FirstLastTrainPath {
            val id = json.optString("id")
            val line = json.optString("line").takeIf { it.isNotBlank() && it != "null" }
            val towards = json.optString("towards").takeIf { it.isNotBlank() && it != "null" }
            return FirstLastTrainPath(id, line, towards)
        }

    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("id", id)
            if (line != null) {
                put("line", line)
            }
            if (towards != null) {
                put("towards", towards)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FirstLastTrainPath) return false

        if (id != other.id) return false
        if (line != other.line) return false
        return towards == other.towards
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (line?.hashCode() ?: 0)
        result = 31 * result + (towards?.hashCode() ?: 0)
        return result
    }

}
