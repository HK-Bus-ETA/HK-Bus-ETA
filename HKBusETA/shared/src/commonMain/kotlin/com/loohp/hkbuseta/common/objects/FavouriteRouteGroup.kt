/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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
import com.loohp.hkbuseta.common.utils.mapToMutableList
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.toJsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject


@Immutable
class FavouriteRouteGroup(
    val name: BilingualText,
    val favouriteRouteStops: List<FavouriteRouteStop>
) : JSONSerializable {

    companion object {

        val DEFAULT_GROUP = FavouriteRouteGroup("收藏" withEn "Favourites", emptyList())

        fun deserialize(json: JsonObject): FavouriteRouteGroup {
            val name = BilingualText.deserialize(json.optJsonObject("name")!!)
            val favouriteRouteStops = json.optJsonArray("favouriteRouteStops")!!.mapToMutableList { FavouriteRouteStop.deserialize(it.jsonObject) }
            return FavouriteRouteGroup(name, favouriteRouteStops)
        }

        fun fromLegacy(legacy: MutableMap<Int, FavouriteRouteStop>): FavouriteRouteGroup {
            val name = DEFAULT_GROUP.name
            val favouriteRouteStops = legacy.map { (k ,v) -> FavouriteRouteStop(v.stopId, v.co, v.index, v.stop, v.route, v.favouriteStopMode, k) }
            return FavouriteRouteGroup(name, favouriteRouteStops)
        }

    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("name", name.serialize())
            put("favouriteRouteStops", favouriteRouteStops.toJsonArray())
        }
    }

    operator fun component1(): BilingualText {
        return name
    }

    operator fun component2(): List<FavouriteRouteStop> {
        return favouriteRouteStops
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FavouriteRouteGroup) return false

        if (name != other.name) return false
        return favouriteRouteStops == other.favouriteRouteStops
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + favouriteRouteStops.hashCode()
        return result
    }

}

inline val FavouriteRouteGroup.isDefaultGroup: Boolean get() = name == FavouriteRouteGroup.DEFAULT_GROUP.name

operator fun FavouriteRouteGroup.component3(): Boolean {
    return isDefaultGroup
}

fun FavouriteRouteGroup.add(favouriteRouteStop: FavouriteRouteStop): FavouriteRouteGroup {
    val stops = favouriteRouteStops.toMutableList()
    if (!stops.contains(favouriteRouteStop)) {
        stops.add(favouriteRouteStop)
    }
    return FavouriteRouteGroup(name, stops)
}

fun FavouriteRouteGroup.remove(favouriteRouteStop: FavouriteRouteStop): FavouriteRouteGroup {
    return FavouriteRouteGroup(name, favouriteRouteStops.toMutableList().apply {
        removeAll { it.favouriteStopMode == favouriteRouteStop.favouriteStopMode && it.sameAs(favouriteRouteStop.stopId, favouriteRouteStop.co, favouriteRouteStop.index, favouriteRouteStop.stop, favouriteRouteStop.route) }
    })
}

fun FavouriteRouteGroup.remove(stopId: String, co: Operator, index: Int, stop: Stop, route: Route, mode: FavouriteStopMode): FavouriteRouteGroup {
    return FavouriteRouteGroup(name, favouriteRouteStops.toMutableList().apply {
        removeAll { it.favouriteStopMode == mode && it.sameAs(stopId, co, index, stop, route) }
    })
}

fun FavouriteRouteGroup.hasStop(stopId: String, co: Operator, index: Int, stop: Stop, route: Route): Boolean {
    return favouriteRouteStops.any { it.sameAs(stopId, co, index, stop, route) }
}

fun List<FavouriteRouteStop>.findSame(stopId: String, co: Operator, index: Int, stop: Stop, route: Route): List<FavouriteRouteStop> {
    return filter { it.sameAs(stopId, co, index, stop, route) }
}

fun FavouriteRouteGroup.findSame(stopId: String, co: Operator, index: Int, stop: Stop, route: Route): List<FavouriteRouteStop> {
    return favouriteRouteStops.findSame(stopId, co, index, stop, route)
}

fun List<FavouriteRouteGroup>.hasStop(stopId: String, co: Operator, index: Int, stop: Stop, route: Route): Boolean {
    return any { it.hasStop(stopId, co, index, stop, route) }
}

fun List<FavouriteRouteGroup>.getByName(name: BilingualText): FavouriteRouteGroup? {
    return firstOrNull { it.name == name }
}

fun List<FavouriteRouteGroup>.indexOfName(name: BilingualText): Int {
    return indexOfFirst { it.name == name }
}

fun List<FavouriteRouteGroup>.getFavouriteRouteStop(favouriteId: Int): FavouriteRouteStop? {
    return asSequence().flatMap { it.favouriteRouteStops }.firstOrNull { it.favouriteId == favouriteId }
}

fun List<FavouriteRouteGroup>.removeFavouriteRouteStop(favouriteId: Int): List<FavouriteRouteGroup> {
    return map { (name, group) -> FavouriteRouteGroup(name, group.filter { stop -> stop.favouriteId != favouriteId }) }
}