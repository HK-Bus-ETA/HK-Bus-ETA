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

import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.optBoolean
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.readCollection
import com.loohp.hkbuseta.common.utils.readMap
import com.loohp.hkbuseta.common.utils.readNullable
import com.loohp.hkbuseta.common.utils.readString
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.common.utils.toJsonObject
import com.loohp.hkbuseta.common.utils.mapToMutableList
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.writeCollection
import com.loohp.hkbuseta.common.utils.writeMap
import com.loohp.hkbuseta.common.utils.writeNullable
import com.loohp.hkbuseta.common.utils.writeString
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.charsets.Charsets.UTF_8
import io.ktor.utils.io.writeBoolean
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Immutable
class Route(
    val routeNumber: String,
    val bound: Map<Operator, String>,
    val co: List<Operator>,
    val serviceType: String,
    val nlbId: String,
    val gtfsId: String,
    val isCtbIsCircular: Boolean,
    val isKmbCtbJoint: Boolean,
    val gmbRegion: GMBRegion?,
    val lrtCircular: BilingualText?,
    val dest: BilingualText,
    val orig: BilingualText,
    val stops: Map<Operator, List<String>>
) : JSONSerializable, IOSerializable {

    companion object {

        fun deserialize(json: JsonObject): Route {
            val route = json.optString("route")
            val bound = json.optJsonObject("bound")!!.mapToMutableMap({ Operator.valueOf(it) }, { it.jsonPrimitive.content })
            val co = json.optJsonArray("co")!!.map { Operator.valueOf(it.jsonPrimitive.content) }
            val serviceType = json.optString("serviceType")
            val nlbId = json.optString("nlbId")
            val gtfsId = json.optString("gtfsId")
            val ctbIsCircular = json.optBoolean("ctbIsCircular")
            val kmbCtbJoint = json.optBoolean("kmbCtbJoint")
            val gmbRegion = if (json.contains("gmbRegion")) GMBRegion.valueOfOrNull(json.optString("gmbRegion")) else null
            val lrtCircular = if (json.contains("lrtCircular")) BilingualText.deserialize(json.optJsonObject("lrtCircular")!!) else null
            val dest = BilingualText.deserialize(json.optJsonObject("dest")!!)
            val orig = BilingualText.deserialize(json.optJsonObject("orig")!!)
            val stops = json.optJsonObject("stops")!!.mapToMutableMap({ Operator.valueOf(it) }, { it.jsonArray.mapToMutableList { e -> e.jsonPrimitive.content } })
            return Route(route, bound, co, serviceType, nlbId, gtfsId, ctbIsCircular, kmbCtbJoint, gmbRegion, lrtCircular, dest, orig, stops)
        }

        suspend fun deserialize(input: ByteReadChannel): Route {
            val route = input.readString(UTF_8)
            val bound = input.readMap(LinkedHashMap()) { Operator.valueOf(it.readString(UTF_8)) to it.readString(UTF_8) }
            val co = input.readCollection(ArrayList()) { Operator.valueOf(it.readString(UTF_8)) }
            val serviceType = input.readString(UTF_8)
            val nlbId = input.readString(UTF_8)
            val gtfsId = input.readString(UTF_8)
            val ctbIsCircular = input.readBoolean()
            val kmbCtbJoint = input.readBoolean()
            val gmbRegion = input.readNullable { GMBRegion.valueOfOrNull(it.readString(UTF_8)) }
            val lrtCircular = input.readNullable { BilingualText.deserialize(it) }
            val dest = BilingualText.deserialize(input)
            val orig = BilingualText.deserialize(input)
            val stops = input.readMap(LinkedHashMap()) { Operator.valueOf(it.readString(UTF_8)) to it.readCollection(ArrayList()) { it1 -> it1.readString(UTF_8) } }
            return Route(route, bound, co, serviceType, nlbId, gtfsId, ctbIsCircular, kmbCtbJoint, gmbRegion, lrtCircular, dest, orig, stops)
        }
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("route", routeNumber)
            put("bound", bound.toJsonObject { it })
            put("co", co.asSequence().map { o -> o.name }.toJsonArray())
            put("serviceType", serviceType)
            put("nlbId", nlbId)
            put("gtfsId", gtfsId)
            put("ctbIsCircular", isCtbIsCircular)
            put("kmbCtbJoint", isKmbCtbJoint)
            if (gmbRegion != null) {
                put("gmbRegion", gmbRegion.name)
            }
            if (lrtCircular != null) {
                put("lrtCircular", lrtCircular.serialize())
            }
            put("dest", dest.serialize())
            put("orig", orig.serialize())
            put("stops", stops.toJsonObject { it.toJsonArray() })
        }
    }

    override suspend fun serialize(out: ByteWriteChannel) {
        out.writeString(routeNumber, UTF_8)
        out.writeMap(bound) { o, k, v -> o.writeString(k.name, UTF_8) to o.writeString(v, UTF_8) }
        out.writeCollection(co) { o, t -> o.writeString(t.name, UTF_8) }
        out.writeString(serviceType, UTF_8)
        out.writeString(nlbId, UTF_8)
        out.writeString(gtfsId, UTF_8)
        out.writeBoolean(isCtbIsCircular)
        out.writeBoolean(isKmbCtbJoint)
        out.writeNullable(gmbRegion) { o, v -> o.writeString(v.name, UTF_8) }
        out.writeNullable(lrtCircular) { o, v -> v.serialize(o) }
        dest.serialize(out)
        orig.serialize(out)
        out.writeMap(stops) { o, k, v -> o.writeString(k.name, UTF_8) to o.writeCollection(v) { o1, t1 -> o1.writeString(t1, UTF_8) } }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Route) return false

        if (routeNumber != other.routeNumber) return false
        if (bound != other.bound) return false
        if (co != other.co) return false
        if (serviceType != other.serviceType) return false
        if (nlbId != other.nlbId) return false
        if (gtfsId != other.gtfsId) return false
        if (isCtbIsCircular != other.isCtbIsCircular) return false
        if (isKmbCtbJoint != other.isKmbCtbJoint) return false
        if (gmbRegion != other.gmbRegion) return false
        if (lrtCircular != other.lrtCircular) return false
        if (dest != other.dest) return false
        if (orig != other.orig) return false
        return stops == other.stops
    }

    override fun hashCode(): Int {
        var result = routeNumber.hashCode()
        result = 31 * result + bound.hashCode()
        result = 31 * result + co.hashCode()
        result = 31 * result + serviceType.hashCode()
        result = 31 * result + nlbId.hashCode()
        result = 31 * result + gtfsId.hashCode()
        result = 31 * result + isCtbIsCircular.hashCode()
        result = 31 * result + isKmbCtbJoint.hashCode()
        result = 31 * result + (gmbRegion?.hashCode() ?: 0)
        result = 31 * result + (lrtCircular?.hashCode() ?: 0)
        result = 31 * result + dest.hashCode()
        result = 31 * result + orig.hashCode()
        result = 31 * result + stops.hashCode()
        return result
    }

}
