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

import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.optBoolean
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.readBoolean
import com.loohp.hkbuseta.common.utils.readString
import com.loohp.hkbuseta.common.utils.writeBoolean
import com.loohp.hkbuseta.common.utils.writeString
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.io.Sink
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


@Immutable
@Serializable
data class RouteSortPreference(
    val routeSortMode: RouteSortMode,
    val filterTimetableActive: Boolean
) : JSONSerializable, IOSerializable {

    companion object {

        val DEFAULT: RouteSortPreference = RouteSortPreference(RouteSortMode.NORMAL, false)

        fun fromLegacy(json: JsonPrimitive): RouteSortPreference {
            return RouteSortPreference(RouteSortMode.valueOf(json.content), false)
        }

        fun deserialize(json: JsonObject): RouteSortPreference {
            val routeSortMode = RouteSortMode.valueOf(json.optString("routeSortMode"))
            val filterTimetableActive = json.optBoolean("filterTimetableActive", false)
            return RouteSortPreference(routeSortMode, filterTimetableActive)
        }

        suspend fun deserialize(input: ByteReadChannel): RouteSortPreference {
            val routeSortMode = RouteSortMode.valueOf(input.readString(Charsets.UTF_8))
            val filterTimetableActive = input.readBoolean()
            return RouteSortPreference(routeSortMode, filterTimetableActive)
        }
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("routeSortMode", routeSortMode.name)
            put("filterTimetableActive", filterTimetableActive)
        }
    }

    override fun serialize(out: Sink) {
        out.writeString(routeSortMode.name, Charsets.UTF_8)
        out.writeBoolean(filterTimetableActive)
    }

}

inline val RouteSortPreference.isDefault: Boolean get() = this == RouteSortPreference.DEFAULT