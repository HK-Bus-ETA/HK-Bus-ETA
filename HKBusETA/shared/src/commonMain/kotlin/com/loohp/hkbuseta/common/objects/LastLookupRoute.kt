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
import com.loohp.hkbuseta.common.utils.optLong
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.readString
import com.loohp.hkbuseta.common.utils.toLocalDateTime
import com.loohp.hkbuseta.common.utils.writeString
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets.UTF_8
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.writeLong
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Immutable
class LastLookupRoute(
    val routeKey: String,
    val time: Long
) : JSONSerializable, IOSerializable {

    companion object {

        fun deserialize(json: JsonObject): LastLookupRoute {
            val routeKey = json.optString("routeKey")
            val time = json.optLong("time")
            return LastLookupRoute(routeKey, time)
        }

        suspend fun deserialize(input: ByteReadChannel): LastLookupRoute {
            val routeKey = input.readString(UTF_8)
            val time = input.readLong()
            return LastLookupRoute(routeKey, time)
        }

        fun fromLegacy(routeKey: String): LastLookupRoute {
            return LastLookupRoute(routeKey, 0)
        }

    }

    val dateTime: LocalDateTime by lazy { time.toLocalDateTime() }

    operator fun component1(): String {
        return routeKey
    }

    operator fun component2(): Long {
        return time
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("routeKey", routeKey)
            put("time", time)
        }
    }

    override fun serialize(out: BytePacketBuilder) {
        out.writeString(routeKey, UTF_8)
        out.writeLong(time)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LastLookupRoute) return false

        if (routeKey != other.routeKey) return false
        if (time != other.time) return false

        return true
    }

    override fun hashCode(): Int {
        var result = routeKey.hashCode()
        result = 31 * result + time.hashCode()
        return result
    }

}
