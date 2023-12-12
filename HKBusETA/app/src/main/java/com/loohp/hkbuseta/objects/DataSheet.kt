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
import com.loohp.hkbuseta.utils.mapToMutableList
import com.loohp.hkbuseta.utils.mapToMutableMap
import com.loohp.hkbuseta.utils.optJsonArray
import com.loohp.hkbuseta.utils.optJsonObject
import com.loohp.hkbuseta.utils.optString
import com.loohp.hkbuseta.utils.toJsonArray
import com.loohp.hkbuseta.utils.toJsonObject
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Immutable
class DataSheet(
    val holidays: List<LocalDate>,
    val routeList: Map<String, Route>,
    val stopList: Map<String, Stop>,
    val stopMap: Map<String, List<Pair<Operator, String>>>
) : JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): DataSheet {
            val holidays = json.optJsonArray("holidays")!!.map { LocalDate.parse(formatHolidaysDate(it.jsonPrimitive.content)) }
            val routeList = json.optJsonObject("routeList")!!.mapToMutableMap { Route.deserialize(it.jsonObject) }
            val stopList = json.optJsonObject("stopList")!!.mapToMutableMap { Stop.deserialize(it.jsonObject) }
            val stopMap = json.optJsonObject("stopMap")!!.mapToMutableMap { it.jsonArray.mapToMutableList { v ->
                val array = v.jsonArray
                Operator.valueOf(array.optString(0)) to array.optString(1)
            } }
            return DataSheet(holidays, routeList, stopList, stopMap)
        }

        private fun formatHolidaysDate(date: String): String {
            return if (date.length == 8 && !date.contains('-')) {
                "${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}"
            } else {
                date
            }
        }

    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("holidays", holidays.asSequence().map { it.toString() }.toJsonArray())
            put("routeList", routeList.toJsonObject { it.serialize() })
            put("stopList", stopList.toJsonObject { it.serialize() })
            put("stopMap", stopMap.toJsonObject { v -> v.asSequence().map { (first, second) -> buildJsonArray { add(first.name); add(second) } }.toJsonArray() })
        }
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
