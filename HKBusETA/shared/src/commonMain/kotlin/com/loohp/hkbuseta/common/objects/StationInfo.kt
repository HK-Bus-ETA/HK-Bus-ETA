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

import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.InvalidAsNullLocalTimeSpecialSerializer
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
class StationInfo(
    val fares: Map<String, Map<FareType, Fare>>,
    @SerialName("barrier_free") val barrierFree: Map<String, StationBarrierFreeFacility> = emptyMap(),
    @Serializable(with = InvalidAsNullLocalTimeSpecialSerializer::class) val opening: LocalTime? = null,
    @Serializable(with = InvalidAsNullLocalTimeSpecialSerializer::class) val closing: LocalTime? = null,
    @SerialName("first_trains") val firstTrains: Map<String, FirstLastTrain>,
    @SerialName("last_trains") val lastTrains: Map<String, FirstLastTrain>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StationInfo) return false

        if (fares != other.fares) return false
        if (barrierFree != other.barrierFree) return false
        if (opening != other.opening) return false
        if (closing != other.closing) return false
        if (firstTrains != other.firstTrains) return false
        return lastTrains == other.lastTrains
    }

    override fun hashCode(): Int {
        var result = fares.hashCode()
        result = 31 * result + barrierFree.hashCode()
        result = 31 * result + (opening?.hashCode() ?: 0)
        result = 31 * result + (closing?.hashCode() ?: 0)
        result = 31 * result + firstTrains.hashCode()
        result = 31 * result + lastTrains.hashCode()
        return result
    }

}
