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
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.toJsonObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

@Serializable
@Immutable
class StationBarrierFreeMapping(
    val categories: Map<String, StationBarrierFreeCategory>,
    val items: Map<String, StationBarrierFreeItem>
) {

    companion object {

        val EMPTY_MAPPING = StationBarrierFreeMapping(emptyMap(), emptyMap())

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StationBarrierFreeMapping) return false

        if (categories != other.categories) return false
        return items == other.items
    }

    override fun hashCode(): Int {
        var result = categories.hashCode()
        result = 31 * result + items.hashCode()
        return result
    }
}
