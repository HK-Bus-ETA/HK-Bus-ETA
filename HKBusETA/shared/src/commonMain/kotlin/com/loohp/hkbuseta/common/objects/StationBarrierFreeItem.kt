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
import com.loohp.hkbuseta.common.utils.optString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
@Immutable
class StationBarrierFreeItem(
    val category: String,
    val name: BilingualText
) : JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): StationBarrierFreeItem {
            val category = json.optString("category")
            val name = BilingualText.deserialize(json.optJsonObject("name")!!)
            return StationBarrierFreeItem(category, name)
        }

    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("category", category)
            put("name", name.serialize())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StationBarrierFreeItem) return false

        if (category != other.category) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        var result = category.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

}
