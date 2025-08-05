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

import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.InvalidAsNullLocalTimeSpecialSerializer
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@Immutable
data class StationInfo(
    val fares: Map<String, Map<FareType, Fare>>,
    @SerialName("barrier_free") val barrierFree: Map<String, StationBarrierFreeFacility> = emptyMap(),
    val facilities: Long = 0L,
    @Serializable(with = InvalidAsNullLocalTimeSpecialSerializer::class) val opening: LocalTime? = null,
    @Serializable(with = InvalidAsNullLocalTimeSpecialSerializer::class) val closing: LocalTime? = null,
    @SerialName("first_trains") val firstTrains: Map<String, FirstLastTrain>,
    @SerialName("last_trains") val lastTrains: Map<String, FirstLastTrain>
) {

    fun facilities(mapping: List<StationFacility>): List<StationFacility> {
        return buildList {
            for ((index, facility) in mapping.withIndex()) {
                if ((facilities shr index) and 1L == 1L) {
                    add(facility)
                }
            }
            removeAll { it.excludeIf.any { e -> contains(e) } }
        }
    }

}
