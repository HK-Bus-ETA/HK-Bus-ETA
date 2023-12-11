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

import androidx.compose.runtime.Stable
import com.loohp.hkbuseta.utils.IOSerializable
import com.loohp.hkbuseta.utils.JSONSerializable
import com.loohp.hkbuseta.utils.optBoolean
import com.loohp.hkbuseta.utils.optDouble
import com.loohp.hkbuseta.utils.optJsonObject
import com.loohp.hkbuseta.utils.optString
import com.loohp.hkbuseta.utils.readNullable
import com.loohp.hkbuseta.utils.readString
import com.loohp.hkbuseta.utils.writeNullable
import com.loohp.hkbuseta.utils.writeString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.text.Charsets.UTF_8

@Stable
open class RouteSearchResultEntry : JSONSerializable, IOSerializable {

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

        fun deserialize(inputStream: InputStream): RouteSearchResultEntry {
            val input = DataInputStream(inputStream)
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
        return deserialize(ByteArrayInputStream(toByteArray()))
    }

    fun strip() {
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

    override fun serialize(outputStream: OutputStream) {
        val out = DataOutputStream(outputStream)
        out.writeString(routeKey, UTF_8)
        out.writeNullable(route) { o, v -> v.serialize(o) }
        out.writeString(co.name, UTF_8)
        out.writeNullable(stopInfo) { o, v -> v.serialize(o) }
        out.writeNullable(origin) { o, v -> v.serialize(o) }
        out.writeBoolean(isInterchangeSearch)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteSearchResultEntry

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
class StopInfo(val stopId: String, var data: Stop?, val distance: Double, val co: Operator) : JSONSerializable, IOSerializable {

    companion object {

        fun deserialize(json: JsonObject): StopInfo {
            val stopId = json.optString("stopId")
            val data = if (json.contains("data")) Stop.deserialize(json.optJsonObject("data")!!) else null
            val distance = json.optDouble("distance")
            val co = Operator.valueOf(json.optString("co"))
            return StopInfo(stopId, data, distance, co)
        }

        fun deserialize(inputStream: InputStream): StopInfo {
            val input = DataInputStream(inputStream)
            val stopId = input.readString(UTF_8)
            val data = input.readNullable(Stop::deserialize)
            val distance = input.readDouble()
            val co = Operator.valueOf(input.readString(UTF_8))
            return StopInfo(stopId, data, distance, co)
        }
    }

    fun strip() {
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

    override fun serialize(outputStream: OutputStream) {
        val out = DataOutputStream(outputStream)
        out.writeString(stopId, UTF_8)
        out.writeNullable(data) { o, v -> v.serialize(o) }
        out.writeDouble(distance)
        out.writeString(co.name, UTF_8)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StopInfo

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
