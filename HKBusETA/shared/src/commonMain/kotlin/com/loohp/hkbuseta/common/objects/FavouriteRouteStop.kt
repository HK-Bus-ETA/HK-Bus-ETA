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

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.optInt
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.random.Random

@Immutable
@Serializable
class FavouriteRouteStop(
    val stopId: String,
    val co: Operator,
    val index: Int,
    val stop: Stop,
    val route: Route,
    val favouriteStopMode: FavouriteStopMode,
    val favouriteId: Int
) : JSONSerializable {

    constructor(stopId: String, co: Operator, index: Int, stop: Stop, route: Route, favouriteStopMode: FavouriteStopMode):
            this(stopId, co, index, stop, route, favouriteStopMode, Random.nextInt())

    companion object {

        fun deserialize(json: JsonObject): FavouriteRouteStop {
            val stopId = json.optString("stopId")
            val co = Operator.valueOf(json.optString("co"))
            val index = json.optInt("index")
            val stop = Stop.deserialize(json.optJsonObject("stop")!!)
            val route = Route.deserialize(json.optJsonObject("route")!!)
            val favouriteStopMode = FavouriteStopMode.valueOfOrDefault(json.optString("favouriteStopMode"))
            val favouriteId = json.optInt("favouriteId")
            return FavouriteRouteStop(stopId, co, index, stop, route, favouriteStopMode, favouriteId)
        }

    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("stopId", stopId)
            put("co", co.name)
            put("index", index)
            put("stop", stop.serialize())
            put("route", route.serialize())
            put("favouriteStopMode", favouriteStopMode.name)
            put("favouriteId", favouriteId)
            platformDisplayInfo?.let { put("platformDisplayInfo", it) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FavouriteRouteStop) return false

        if (stopId != other.stopId) return false
        if (co != other.co) return false
        if (index != other.index) return false
        if (stop != other.stop) return false
        if (route != other.route) return false
        if (favouriteStopMode != other.favouriteStopMode) return false
        return favouriteId == other.favouriteId
    }

    override fun hashCode(): Int {
        var result = stopId.hashCode()
        result = 31 * result + co.hashCode()
        result = 31 * result + index
        result = 31 * result + stop.hashCode()
        result = 31 * result + route.hashCode()
        result = 31 * result + favouriteStopMode.hashCode()
        result = 31 * result + favouriteId
        return result
    }

}

expect val FavouriteRouteStop.platformDisplayInfo: JsonObject?

fun List<FavouriteRouteStop>.toRouteSearchResult(instance: AppContext, origin: Coordinates?): List<RouteSearchResultEntry> {
    return asSequence()
        .map { fav ->
            val (index, stopId, stop, route) = fav.resolveStop(instance) { origin }
            val favouriteStopMode = fav.favouriteStopMode
            RouteSearchResultEntry(route.getRouteKey(instance)!!, route, fav.co, StopInfo(stopId, stop, 0.0, fav.co, index), null, false, favouriteStopMode)
        }
        .distinctBy { routeEntry -> routeEntry.uniqueKey }
        .toList()
}