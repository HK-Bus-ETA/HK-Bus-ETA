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


@Immutable
class TemporaryPinItem private constructor(
    val key: String,
    val routeNumber: String,
    val bound: String,
    val co: Operator,
    val gmbRegion: GMBRegion?,
    val stopIndex: Int,
    val branches: List<Route>
) {
    constructor(
        route: Route,
        co: Operator,
        stopIndex: Int,
        branches: List<Route>
    ): this(
        key = "${route.routeGroupKey(co)},$stopIndex",
        routeNumber = route.routeNumber,
        bound = route.idBound(co),
        co = co,
        gmbRegion = route.gmbRegion,
        stopIndex = stopIndex,
        branches = branches
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TemporaryPinItem) return false
        if (key != other.key) return false
        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}