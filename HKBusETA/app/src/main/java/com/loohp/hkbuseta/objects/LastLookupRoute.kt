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
import com.loohp.hkbuseta.utils.JSONSerializable
import com.loohp.hkbuseta.utils.optString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


@Immutable
data class LastLookupRoute(val routeNumber: String, val co: Operator, val meta: String) :
    JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): LastLookupRoute {
            val routeNumber = json.optString("r")
            val co = json.optString("c").operator
            val meta = if (co == Operator.GMB || co == Operator.NLB) json.optString("m") else ""
            return LastLookupRoute(routeNumber, co, meta)
        }

    }

    fun isValid(): Boolean {
        if (routeNumber.isBlank() || !co.isBuiltIn) {
            return false
        }
        if ((co == Operator.GMB || co == Operator.NLB) && meta.isBlank()) {
            return false
        }
        if (co == Operator.GMB && meta.gmbRegion == null) {
            return false
        }
        return true
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("r", routeNumber)
            put("c", co.name)
            if (co == Operator.GMB || co == Operator.NLB) put("m", meta)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LastLookupRoute

        if (routeNumber != other.routeNumber) return false
        if (co != other.co) return false
        if ((co == Operator.GMB || co == Operator.NLB) && meta != other.meta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = routeNumber.hashCode()
        result = 31 * result + co.hashCode()
        if (co == Operator.GMB || co == Operator.NLB) result = 31 * result + meta.hashCode()
        return result
    }

}