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
import com.loohp.hkbuseta.utils.toJSONArray
import com.loohp.hkbuseta.utils.toJSONObject
import com.loohp.hkbuseta.utils.toList
import com.loohp.hkbuseta.utils.toMap
import com.loohp.hkbuseta.utils.toSet
import org.json.JSONArray
import org.json.JSONObject

@Immutable
class DataContainer(
    val dataSheet: DataSheet,
    val busRoute: Set<String>,
    val mtrBusStopAlias: Map<String, List<String>>
) : JSONSerializable {

    companion object {

        fun deserialize(json: JSONObject): DataContainer {
            val dataSheet = DataSheet.deserialize(json.optJSONObject("dataSheet")!!)
            val busRoute = json.optJSONArray("busRoute")!!.toSet(String::class.java)
            val mtrBusStopAlias = json.optJSONObject("mtrBusStopAlias")!!.toMap { (it as JSONArray).toList(String::class.java) }
            return DataContainer(dataSheet, busRoute, mtrBusStopAlias)
        }

    }

    override fun serialize(): JSONObject {
        val json = JSONObject()
        json.put("dataSheet", dataSheet.serialize())
        json.put("busRoute", busRoute.toJSONArray())
        json.put("mtrBusStopAlias", mtrBusStopAlias.toJSONObject { it.toJSONArray() })
        return json
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataContainer

        if (dataSheet != other.dataSheet) return false
        if (busRoute != other.busRoute) return false
        return mtrBusStopAlias == other.mtrBusStopAlias
    }

    override fun hashCode(): Int {
        var result = dataSheet.hashCode()
        result = 31 * result + busRoute.hashCode()
        result = 31 * result + mtrBusStopAlias.hashCode()
        return result
    }

}
