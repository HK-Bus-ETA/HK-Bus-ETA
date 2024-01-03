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

import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.mapToMutableList
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.optBoolean
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.common.utils.toJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class Preferences(
    var language: String,
    var clockTimeMode: Boolean,
    val favouriteRouteStops: MutableMap<Int, FavouriteRouteStop>,
    val lastLookupRoutes: MutableList<LastLookupRoute>,
    val etaTileConfigurations: MutableMap<Int, List<Int>>,
    val routeSortModePreference: MutableMap<RouteListType, RouteSortMode>
) : JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): Preferences {
            val language = json.optString("language")
            val clockTimeMode = json.optBoolean("clockTimeMode", false)
            val favouriteRouteStops = json.optJsonObject("favouriteRouteStops")!!.mapToMutableMap({ it.toInt() }) { FavouriteRouteStop.deserialize(it.jsonObject) }
            val lastLookupRoutes = json.optJsonArray("lastLookupRoutes")!!.mapToMutableList { LastLookupRoute.deserialize(it.jsonObject) }
            val etaTileConfigurations = if (json.contains("etaTileConfigurations")) json.optJsonObject("etaTileConfigurations")!!.mapToMutableMap<Int, List<Int>>({ it.toInt() }) { it.jsonArray.mapToMutableList { e -> e.jsonPrimitive.int } } else HashMap()
            val routeSortModePreference = if (json.contains("routeSortModePreference")) json.optJsonObject("routeSortModePreference")!!.mapToMutableMap({ RouteListType.valueOf(it) }, { RouteSortMode.valueOf(it.jsonPrimitive.content) }) else HashMap()
            return Preferences(language, clockTimeMode, favouriteRouteStops, lastLookupRoutes, etaTileConfigurations, routeSortModePreference)
        }

        fun createDefault(): Preferences {
            return Preferences("zh", false, HashMap(), ArrayList(), HashMap(), HashMap())
        }

    }

    fun cleanForImport(): Preferences {
        etaTileConfigurations.clear()
        return this
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("language", language)
            put("clockTimeMode", clockTimeMode)
            put("favouriteRouteStops", favouriteRouteStops.toJsonObject { it.serialize() })
            put("lastLookupRoutes", lastLookupRoutes.asSequence().map { it.serialize() }.toJsonArray())
            put("etaTileConfigurations", etaTileConfigurations.toJsonObject { it.toJsonArray() })
            put("routeSortModePreference", routeSortModePreference.toJsonObject { it.name })
        }
    }

}
