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
import com.loohp.hkbuseta.common.utils.optJsonObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

@Serializable
@Immutable
class StationBarrierFreeCategory(
    val name: BilingualText
) : JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): StationBarrierFreeCategory {
            val name = BilingualText.deserialize(json.optJsonObject("name")!!)
            return StationBarrierFreeCategory(name)
        }

    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("name", name.serialize())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StationBarrierFreeCategory) return false

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}
