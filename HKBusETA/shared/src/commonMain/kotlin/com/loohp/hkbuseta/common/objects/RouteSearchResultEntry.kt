/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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
package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.Stable
import com.loohp.hkbuseta.common.utils.Strippable
import com.loohp.hkbuseta.common.utils.optBoolean
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optInt
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.readBoolean
import com.loohp.hkbuseta.common.utils.readDouble
import com.loohp.hkbuseta.common.utils.readNullable
import com.loohp.hkbuseta.common.utils.readString
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.common.utils.writeBoolean
import com.loohp.hkbuseta.common.utils.writeDouble
import com.loohp.hkbuseta.common.utils.writeNullable
import com.loohp.hkbuseta.common.utils.writeString
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets.UTF_8
import io.ktor.utils.io.readInt
import kotlinx.io.Sink
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Stable
open class RouteSearchResultEntry : JSONSerializable, IOSerializable, Strippable {

    companion object {

        fun deserialize(json: JsonObject): RouteSearchResultEntry {
            val routeKey = json.optString("routeKey")
            val route = if (json.contains("route")) Route.deserialize(json.optJsonObject("route")!!) else null
            val co = Operator.valueOf(json.optString("co"))
            val stop = if (json.contains("stop")) StopInfo.deserialize(json.optJsonObject("stop")!!) else null
            val origin = if (json.contains("origin")) Coordinates.deserialize(json.optJsonObject("origin")!!) else null
            val isInterchangeSearch = json.optBoolean("isInterchangeSearch")
            val favouriteStopMode = if (json.contains("favouriteStopMode")) FavouriteStopMode.valueOf(json.optString("favouriteStopMode")) else null
            return RouteSearchResultEntry(routeKey, route, co, stop, origin, isInterchangeSearch, favouriteStopMode)
        }

        suspend fun deserialize(input: ByteReadChannel): RouteSearchResultEntry {
            val routeKey = input.readString(UTF_8)
            val route = input.readNullable { Route.deserialize(it) }
            val co = Operator.valueOf(input.readString(UTF_8))
            val stop = input.readNullable { StopInfo.deserialize(it) }
            val origin = input.readNullable { Coordinates.deserialize(it) }
            val isInterchangeSearch = input.readBoolean()
            val favouriteStopMode = input.readNullable { FavouriteStopMode.valueOf(it.readString(UTF_8)) }
            return RouteSearchResultEntry(routeKey, route, co, stop, origin, isInterchangeSearch, favouriteStopMode)
        }

    }

    var routeKey: String
    var route: Route?
    var co: Operator
    var stopInfo: StopInfo? = null
    var origin: Coordinates? = null
    var isInterchangeSearch = false
    var favouriteStopMode: FavouriteStopMode? = null

    constructor(routeKey: String, route: Route?, co: Operator) {
        this.routeKey = routeKey
        this.route = route
        this.co = co
    }

    constructor(routeKey: String, route: Route?, co: Operator, stopInfo: StopInfo?, origin: Coordinates?, isInterchangeSearch: Boolean) {
        this.routeKey = routeKey
        this.route = route
        this.co = co
        this.stopInfo = stopInfo
        this.origin = origin
        this.isInterchangeSearch = isInterchangeSearch
    }

    constructor(routeKey: String, route: Route?, co: Operator, stopInfo: StopInfo?, origin: Coordinates?, isInterchangeSearch: Boolean, favouriteStopMode: FavouriteStopMode?) {
        this.routeKey = routeKey
        this.route = route
        this.co = co
        this.stopInfo = stopInfo
        this.origin = origin
        this.isInterchangeSearch = isInterchangeSearch
        this.favouriteStopMode = favouriteStopMode
    }

    fun deepClone(): RouteSearchResultEntry {
        return deserialize(serialize())
    }

    override fun strip() {
        route = null
        stopInfo?.strip()
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("routeKey", routeKey)
            if (route != null) {
                put("route", route!!.serialize())
            }
            put("co", co.name)
            if (stopInfo != null) {
                put("stop", stopInfo!!.serialize())
            }
            if (origin != null) {
                put("origin", origin!!.serialize())
            }
            put("interchangeSearch", isInterchangeSearch)
            if (favouriteStopMode != null) {
                put("favouriteStopMode", favouriteStopMode!!.name)
            }
        }
    }

    override fun serialize(out: Sink) {
        out.writeString(routeKey, UTF_8)
        out.writeNullable(route) { o, v -> v.serialize(o) }
        out.writeString(co.name, UTF_8)
        out.writeNullable(stopInfo) { o, v -> v.serialize(o) }
        out.writeNullable(origin) { o, v -> v.serialize(o) }
        out.writeBoolean(isInterchangeSearch)
        out.writeNullable(favouriteStopMode) { o, v -> o.writeString(v.name, UTF_8) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteSearchResultEntry) return false

        if (routeKey != other.routeKey) return false
        if (route != other.route) return false
        if (co != other.co) return false
        if (stopInfo != other.stopInfo) return false
        if (origin != other.origin) return false
        if (isInterchangeSearch != other.isInterchangeSearch) return false
        return favouriteStopMode == other.favouriteStopMode
    }

    override fun hashCode(): Int {
        var result = routeKey.hashCode()
        result = 31 * result + (route?.hashCode() ?: 0)
        result = 31 * result + co.hashCode()
        result = 31 * result + (stopInfo?.hashCode() ?: 0)
        result = 31 * result + (origin?.hashCode() ?: 0)
        result = 31 * result + isInterchangeSearch.hashCode()
        result = 31 * result + (favouriteStopMode?.hashCode() ?: 0)
        return result
    }

}

@Stable
data class StopInfo(
    val stopId: String,
    var data: Stop?,
    val distance: Double,
    val co: Operator,
    val stopIndex: Int? = null
): JSONSerializable, IOSerializable, Strippable {

    companion object {

        fun deserialize(json: JsonObject): StopInfo {
            val stopId = json.optString("stopId")
            val data = if (json.contains("data")) Stop.deserialize(json.optJsonObject("data")!!) else null
            val distance = json.optDouble("distance")
            val co = Operator.valueOf(json.optString("co"))
            val stopIndex = if (json.contains("stopIndex")) json.optInt("stopIndex") else null
            return StopInfo(stopId, data, distance, co, stopIndex)
        }

        suspend fun deserialize(input: ByteReadChannel): StopInfo {
            val stopId = input.readString(UTF_8)
            val data = input.readNullable { Stop.deserialize(it) }
            val distance = input.readDouble()
            val co = Operator.valueOf(input.readString(UTF_8))
            val stopIndex = input.readNullable { it.readInt() }
            return StopInfo(stopId, data, distance, co, stopIndex)
        }
    }

    override fun strip() {
        data = null
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("stopId", stopId)
            if (data != null) {
                put("data", data!!.serialize())
            }
            put("distance", distance)
            put("co", co.name)
            if (stopIndex != null) {
                put("stopIndex", stopIndex)
            }
        }
    }

    override fun serialize(out: Sink) {
        out.writeString(stopId, UTF_8)
        out.writeNullable(data) { o, v -> v.serialize(o) }
        out.writeDouble(distance)
        out.writeString(co.name, UTF_8)
        out.writeNullable(stopIndex) { o, v -> o.writeInt(v) }
    }
}

fun AppIntent.putExtra(key: String, value: Sequence<RouteSearchResultEntry>) {
    if (Shared.isWearOS) {
        putExtra(key, value.map {
            it.strip()
            it.serialize()
        }.toJsonArray().toString())
    } else {
        extras.data[key] = value.toList()
    }
}
