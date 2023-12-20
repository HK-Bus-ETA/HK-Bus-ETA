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

package com.loohp.hkbuseta.utils

import androidx.compose.runtime.Immutable
import com.loohp.hkbuseta.common.objects.Coordinates


@Immutable
class LocationResult internal constructor(val location: Coordinates?) {

    companion object {

        val FAILED_RESULT = LocationResult(null)

        fun fromLatLng(lat: Double, lng: Double): LocationResult {
            return LocationResult(Coordinates(lat, lng))
        }

        fun fromCoordinatesNullable(location: Coordinates?): LocationResult {
            return LocationResult(location)
        }

    }

    val isSuccess: Boolean get() = location != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationResult

        return location == other.location
    }

    override fun hashCode(): Int {
        return location?.hashCode() ?: 0
    }


}