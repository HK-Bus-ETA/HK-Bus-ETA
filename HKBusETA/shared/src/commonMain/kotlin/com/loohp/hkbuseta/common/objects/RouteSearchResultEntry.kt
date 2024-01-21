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

import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.Platform
import com.loohp.hkbuseta.common.appcontext.platform
import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.Stable
import com.loohp.hkbuseta.common.utils.Strippable
import com.loohp.hkbuseta.common.utils.optBoolean
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.readNullable
import com.loohp.hkbuseta.common.utils.readString
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.common.utils.writeNullable
import com.loohp.hkbuseta.common.utils.writeString
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.charsets.Charsets.UTF_8
import io.ktor.utils.io.writeBoolean
import kotlinx.coroutines.runBlocking
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
            return RouteSearchResultEntry(routeKey, route, co, stop, origin, isInterchangeSearch)
        }

        suspend fun deserialize(input: ByteReadChannel): RouteSearchResultEntry {
            val routeKey = input.readString(UTF_8)
            val route = input.readNullable { Route.deserialize(it) }
            val co = Operator.valueOf(input.readString(UTF_8))
            val stop = input.readNullable { StopInfo.deserialize(it) }
            val origin = input.readNullable { Coordinates.deserialize(it) }
            val isInterchangeSearch = input.readBoolean()
            return RouteSearchResultEntry(routeKey, route, co, stop, origin, isInterchangeSearch)
        }

    }

    var routeKey: String
    var route: Route?
    var co: Operator
    var stopInfo: StopInfo? = null
    var origin: Coordinates? = null
    var isInterchangeSearch = false

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

    fun deepClone(): RouteSearchResultEntry {
        return runBlocking { deserialize(ByteReadChannel(toByteArray())) }
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
        }
    }

    override suspend fun serialize(out: ByteWriteChannel) {
        out.writeString(routeKey, UTF_8)
        out.writeNullable(route) { o, v -> v.serialize(o) }
        out.writeString(co.name, UTF_8)
        out.writeNullable(stopInfo) { o, v -> v.serialize(o) }
        out.writeNullable(origin) { o, v -> v.serialize(o) }
        out.writeBoolean(isInterchangeSearch)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteSearchResultEntry) return false

        if (routeKey != other.routeKey) return false
        if (route != other.route) return false
        if (co != other.co) return false
        if (stopInfo != other.stopInfo) return false
        if (origin != other.origin) return false
        return isInterchangeSearch == other.isInterchangeSearch
    }

    override fun hashCode(): Int {
        var result = routeKey.hashCode()
        result = 31 * result + (route?.hashCode() ?: 0)
        result = 31 * result + co.hashCode()
        result = 31 * result + (stopInfo?.hashCode() ?: 0)
        result = 31 * result + (origin?.hashCode() ?: 0)
        result = 31 * result + isInterchangeSearch.hashCode()
        return result
    }

}

@Stable
class StopInfo(val stopId: String, var data: Stop?, val distance: Double, val co: Operator) : JSONSerializable,
    IOSerializable, Strippable {

    companion object {

        fun deserialize(json: JsonObject): StopInfo {
            val stopId = json.optString("stopId")
            val data = if (json.contains("data")) Stop.deserialize(json.optJsonObject("data")!!) else null
            val distance = json.optDouble("distance")
            val co = Operator.valueOf(json.optString("co"))
            return StopInfo(stopId, data, distance, co)
        }

        suspend fun deserialize(input: ByteReadChannel): StopInfo {
            val stopId = input.readString(UTF_8)
            val data = input.readNullable { Stop.deserialize(it) }
            val distance = input.readDouble()
            val co = Operator.valueOf(input.readString(UTF_8))
            return StopInfo(stopId, data, distance, co)
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
        }
    }

    override suspend fun serialize(out: ByteWriteChannel) {
        out.writeString(stopId, UTF_8)
        out.writeNullable(data) { o, v -> v.serialize(o) }
        out.writeDouble(distance)
        out.writeString(co.name, UTF_8)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StopInfo) return false

        if (stopId != other.stopId) return false
        if (data != other.data) return false
        if (distance != other.distance) return false
        return co == other.co
    }

    override fun hashCode(): Int {
        var result = stopId.hashCode()
        result = 31 * result + (data?.hashCode() ?: 0)
        result = 31 * result + distance.hashCode()
        result = 31 * result + co.hashCode()
        return result
    }

}

fun AppIntent.putExtra(key: String, value: Sequence<RouteSearchResultEntry>) {
    when (platform()) {
        Platform.WEAROS -> putExtra(key, value.map {
            it.strip()
            it.serialize()
        }.toJsonArray().toString())
        Platform.WATCHOS -> extras.data[key] = value.toList()
    }
}
