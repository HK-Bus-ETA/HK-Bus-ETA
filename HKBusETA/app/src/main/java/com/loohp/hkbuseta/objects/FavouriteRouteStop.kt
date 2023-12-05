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
import org.json.JSONObject

@Immutable
class FavouriteRouteStop(
    val stopId: String,
    val co: Operator,
    val index: Int,
    val stop: Stop,
    val route: Route,
    val favouriteStopMode: FavouriteStopMode
) : JSONSerializable {

    companion object {

        fun deserialize(json: JSONObject): FavouriteRouteStop {
            val stopId = json.optString("stopId")
            val co = Operator.valueOf(json.optString("co"))
            val index = json.optInt("index")
            val stop = Stop.deserialize(json.optJSONObject("stop")!!)
            val route = Route.deserialize(json.optJSONObject("route")!!)
            val favouriteStopMode =
                FavouriteStopMode.valueOfOrDefault(json.optString("favouriteStopMode"))
            return FavouriteRouteStop(stopId, co, index, stop, route, favouriteStopMode)
        }

    }

    override fun serialize(): JSONObject {
        val json = JSONObject()
        json.put("stopId", stopId)
        json.put("co", co.name)
        json.put("index", index)
        json.put("stop", stop.serialize())
        json.put("route", route.serialize())
        json.put("favouriteStopMode", favouriteStopMode.name)
        return json
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FavouriteRouteStop

        if (stopId != other.stopId) return false
        if (co != other.co) return false
        if (index != other.index) return false
        if (stop != other.stop) return false
        if (route != other.route) return false
        return favouriteStopMode == other.favouriteStopMode
    }

    override fun hashCode(): Int {
        var result = stopId.hashCode()
        result = 31 * result + co.hashCode()
        result = 31 * result + index
        result = 31 * result + stop.hashCode()
        result = 31 * result + route.hashCode()
        result = 31 * result + favouriteStopMode.hashCode()
        return result
    }

}
