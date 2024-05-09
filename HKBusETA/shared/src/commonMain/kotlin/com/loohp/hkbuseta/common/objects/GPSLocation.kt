/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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


@Immutable
class GPSLocation(
    lat: Double,
    lng: Double,
    val altitude: Double?,
    val bearing: Float?
): Coordinates(lat, lng) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GPSLocation) return false
        if (!super.equals(other)) return false

        if (altitude != other.altitude) return false
        return bearing == other.bearing
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (altitude?.hashCode() ?: 0)
        result = 31 * result + (bearing?.hashCode() ?: 0)
        return result
    }
}