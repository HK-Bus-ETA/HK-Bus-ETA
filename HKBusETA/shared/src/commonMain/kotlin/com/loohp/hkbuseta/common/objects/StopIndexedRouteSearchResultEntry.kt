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

import com.loohp.hkbuseta.common.utils.Stable
import com.loohp.hkbuseta.common.utils.optBoolean
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import kotlinx.serialization.json.JsonObject


@Stable
class StopIndexedRouteSearchResultEntry(
    routeKey: String,
    route: Route?,
    co: Operator,
    stopInfo: StopInfo?,
    var stopInfoIndex: Int,
    origin: Coordinates?,
    isInterchangeSearch: Boolean
) : RouteSearchResultEntry(routeKey, route, co, stopInfo, origin, isInterchangeSearch) {

    companion object {

        fun deserialize(json: JsonObject): StopIndexedRouteSearchResultEntry {
            val routeKey = json.optString("routeKey");
            val route = if (json.contains("route")) Route.deserialize(json.optJsonObject("route")!!) else null
            val co = Operator.valueOf(json.optString("co"));
            val stop = if (json.contains("stop")) StopInfo.deserialize(json.optJsonObject("stop")!!) else null
            val origin = if (json.contains("origin")) Coordinates.deserialize(json.optJsonObject("origin")!!) else null
            val isInterchangeSearch = json.optBoolean("isInterchangeSearch")
            return StopIndexedRouteSearchResultEntry(routeKey, route, co, stop, 0, origin, isInterchangeSearch)
        }

        fun fromRouteSearchResultEntry(resultEntry: RouteSearchResultEntry): StopIndexedRouteSearchResultEntry {
            return StopIndexedRouteSearchResultEntry(
                routeKey = resultEntry.routeKey,
                route = resultEntry.route,
                co = resultEntry.co,
                stopInfo = resultEntry.stopInfo,
                stopInfoIndex = 0,
                origin = resultEntry.origin,
                isInterchangeSearch = resultEntry.isInterchangeSearch
            )
        }

    }

}