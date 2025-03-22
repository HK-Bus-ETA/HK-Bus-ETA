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

package com.loohp.hkbuseta.common.utils

import com.loohp.hkbuseta.common.objects.GPSLocation


@Immutable
class LocationResult(val location: GPSLocation?) {

    companion object {

        val FAILED_RESULT = LocationResult(null)

        fun of(lat: Double, lng: Double, altitude: Double? = null, bearing: Float? = null): LocationResult {
            return LocationResult(GPSLocation(lat, lng, altitude, bearing))
        }

    }

    val isSuccess: Boolean = location != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocationResult) return false

        return location == other.location
    }

    override fun hashCode(): Int {
        return location?.hashCode() ?: 0
    }

}

enum class LocationPriority {
    FASTEST, FASTER, ACCURATE, MOST_ACCURATE;
}