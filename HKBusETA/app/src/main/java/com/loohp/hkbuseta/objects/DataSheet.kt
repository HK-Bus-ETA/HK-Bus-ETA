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
import com.loohp.hkbuseta.utils.jsonArrayOf
import com.loohp.hkbuseta.utils.mapToList
import com.loohp.hkbuseta.utils.toJSONArray
import com.loohp.hkbuseta.utils.toJSONObject
import com.loohp.hkbuseta.utils.toMap
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Immutable
class DataSheet(
    val holidays: List<LocalDate>,
    val routeList: Map<String, Route>,
    val stopList: Map<String, Stop>,
    val stopMap: Map<String, List<Pair<Operator, String>>>
) : JSONSerializable {

    companion object {

        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

        fun deserialize(json: JSONObject): DataSheet {
            val holidays = json.optJSONArray("holidays")!!.mapToList() { LocalDate.parse(it as String, DATE_FORMATTER) }
            val routeList = json.optJSONObject("routeList")!!.toMap { Route.deserialize(it as JSONObject) }
            val stopList = json.optJSONObject("stopList")!!.toMap { Stop.deserialize(it as JSONObject) }
            val stopMap = json.optJSONObject("stopMap")!!.toMap { v -> (v as JSONArray).mapToList { vv ->
                val array = vv as JSONArray
                Operator.valueOf(array.optString(0)) to array.optString(1)
            } }
            return DataSheet(holidays, routeList, stopList, stopMap)
        }
    }

    override fun serialize(): JSONObject {
        val json = JSONObject()
        json.put("holidays", holidays.asSequence().map { DATE_FORMATTER.format(it) }.toJSONArray())
        json.put("routeList", routeList.toJSONObject { it.serialize() })
        json.put("stopList", stopList.toJSONObject { it.serialize() })
        json.put("stopMap", stopMap.toJSONObject { v -> v.asSequence().map { (first, second) -> jsonArrayOf(first.name, second) }.toJSONArray() })
        return json
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataSheet

        if (holidays != other.holidays) return false
        if (routeList != other.routeList) return false
        if (stopList != other.stopList) return false
        return stopMap == other.stopMap
    }

    override fun hashCode(): Int {
        var result = holidays.hashCode()
        result = 31 * result + routeList.hashCode()
        result = 31 * result + stopList.hashCode()
        result = 31 * result + stopMap.hashCode()
        return result
    }

}
