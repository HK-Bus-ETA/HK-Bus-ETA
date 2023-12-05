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

import com.loohp.hkbuseta.shared.LastLookupRoute
import com.loohp.hkbuseta.utils.JSONSerializable
import com.loohp.hkbuseta.utils.mapToList
import com.loohp.hkbuseta.utils.toJSONArray
import com.loohp.hkbuseta.utils.toJSONObject
import com.loohp.hkbuseta.utils.toList
import com.loohp.hkbuseta.utils.toMap
import org.json.JSONArray
import org.json.JSONObject

class Preferences(
    var language: String,
    val favouriteRouteStops: MutableMap<Int, FavouriteRouteStop>,
    val lastLookupRoutes: MutableList<LastLookupRoute>,
    val etaTileConfigurations: MutableMap<Int, List<Int>>,
    val routeSortModePreference: MutableMap<RouteListType, RouteSortMode>
) : JSONSerializable {

    companion object {

        fun deserialize(json: JSONObject): Preferences {
            val language = json.optString("language")
            val favouriteRouteStops = json.optJSONObject("favouriteRouteStops")!!.toMap({ it.toInt() }) { FavouriteRouteStop.deserialize(it as JSONObject) }
            val lastLookupRoutes = json.optJSONArray("lastLookupRoutes")!!.mapToList { LastLookupRoute.deserialize(it as JSONObject) }
            val etaTileConfigurations = if (json.has("etaTileConfigurations")) json.optJSONObject("etaTileConfigurations")!!.toMap<Int, List<Int>>({ it.toInt() }) { (it as JSONArray).toList(Int::class.javaPrimitiveType!!) } else HashMap()
            val routeSortModePreference = if (json.has("routeSortModePreference")) json.optJSONObject("routeSortModePreference")!!.toMap({ RouteListType.valueOf(it) }, { RouteSortMode.valueOf(it as String) }) else HashMap()
            return Preferences(language, favouriteRouteStops, lastLookupRoutes, etaTileConfigurations, routeSortModePreference)
        }

        fun createDefault(): Preferences {
            return Preferences("zh", HashMap(), ArrayList(), HashMap(), HashMap())
        }

    }

    fun cleanForImport(): Preferences {
        etaTileConfigurations.clear()
        return this
    }

    override fun serialize(): JSONObject {
        val json = JSONObject()
        json.put("language", language)
        json.put("favouriteRouteStops", favouriteRouteStops.toJSONObject { it.serialize() })
        json.put("lastLookupRoutes", lastLookupRoutes.asSequence().map { it.serialize() }.toJSONArray())
        json.put("etaTileConfigurations", etaTileConfigurations.toJSONObject { it.toJSONArray() })
        json.put("routeSortModePreference", routeSortModePreference.toJSONObject { it.name })
        return json
    }

}
