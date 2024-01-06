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

import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.mapToSet
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.common.utils.toJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Immutable
class DataContainer(
    val dataSheet: DataSheet,
    val busRoute: Set<String>,
    val mtrBusStopAlias: Map<String, List<String>>,
    val kmbSubsidiary: Map<KMBSubsidiary, List<String>>
) : JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): DataContainer {
            val dataSheet = DataSheet.deserialize(json.optJsonObject("dataSheet")!!)
            val busRoute = json.optJsonArray("busRoute")!!.mapToSet { it.jsonPrimitive.content }
            val mtrBusStopAlias = json.optJsonObject("mtrBusStopAlias")!!.mapValues { it.value.jsonArray.map { e -> e.jsonPrimitive.content } }
            val kmbSubsidiary = json.optJsonObject("kmbSubsidiary")!!.mapToMutableMap({ KMBSubsidiary.valueOf(it) }, { it.jsonArray.map { e -> e.jsonPrimitive.content } })
            return DataContainer(dataSheet, busRoute, mtrBusStopAlias, kmbSubsidiary)
        }

    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("dataSheet", dataSheet.serialize())
            put("busRoute", busRoute.toJsonArray())
            put("mtrBusStopAlias", mtrBusStopAlias.toJsonObject { it.toJsonArray() })
            put("kmbSubsidiary", kmbSubsidiary.toJsonObject { it.toJsonArray() })
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataContainer) return false

        if (dataSheet != other.dataSheet) return false
        if (busRoute != other.busRoute) return false
        if (mtrBusStopAlias != other.mtrBusStopAlias) return false
        return kmbSubsidiary == other.kmbSubsidiary
    }

    override fun hashCode(): Int {
        var result = dataSheet.hashCode()
        result = 31 * result + busRoute.hashCode()
        result = 31 * result + mtrBusStopAlias.hashCode()
        result = 31 * result + kmbSubsidiary.hashCode()
        return result
    }

}
