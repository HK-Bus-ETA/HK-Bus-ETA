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
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Immutable
@Serializable
data class FavouriteStop(
    val stopId: String,
    val stop: Stop?,
    val kmbCtbJointRouteNumber: String?
) : JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): FavouriteStop {
            val stopId = json.optString("stopId")
            val stop = json.optJsonObject("stop")?.let { Stop.deserialize(it) }
            val kmbCtbJointRouteNumber = json.optString("kmbCtbJointRouteNumber").takeIf { it.isNotBlank() }
            return FavouriteStop(stopId, stop, kmbCtbJointRouteNumber)
        }

        fun fromLegacy(stopId: String): FavouriteStop {
            return FavouriteStop(stopId, null, null)
        }

    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("stopId", stopId)
            stop?.let { put("stop", it.serialize()) }
            kmbCtbJointRouteNumber?.let { put("kmbCtbJointRouteNumber", it) }
        }
    }

}

inline fun Collection<FavouriteStop>.hasStop(stopId: String): Boolean {
    return any { it.isStop(stopId) }
}

inline fun MutableCollection<FavouriteStop>.removeStop(stopId: String) {
    removeAll { it.isStop(stopId) }
}

inline fun FavouriteStop.isStop(stopId: String): Boolean {
    return this.stopId == stopId
}

inline fun FavouriteStop.stop(context: AppContext): Stop? {
    return stop?: Registry.getInstance(context).getStopById(stopId)
}