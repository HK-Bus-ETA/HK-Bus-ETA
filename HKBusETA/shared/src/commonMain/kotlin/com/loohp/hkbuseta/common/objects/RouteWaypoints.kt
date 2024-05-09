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

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.utils.Immutable

@Immutable
data class RouteWaypoints(
    val routeNumber: String,
    val co: Operator,
    val isKmbCtbJoint: Boolean,
    val stops: List<Stop>,
    val paths: List<List<Coordinates>> = listOf(stops.map { it.location })
) {
    val simplifiedPaths: List<List<Coordinates>> by lazy { paths.map { it.simplified() } }
}

fun Route.defaultWaypoints(context: AppContext): RouteWaypoints {
    val operator = co.firstCo()!!
    return RouteWaypoints(
        routeNumber = routeNumber,
        co = operator,
        isKmbCtbJoint = isKmbCtbJoint,
        stops = stops[operator]!!.map { it.asStop(context)!! }
    )
}

fun RouteSearchResultEntry.defaultWaypoints(context: AppContext): RouteWaypoints {
    val routeNumber = route!!.routeNumber
    val bound = route!!.idBound(co)
    val gmbRegion = route!!.gmbRegion
    return RouteWaypoints(
        routeNumber = routeNumber,
        co = co,
        isKmbCtbJoint = route!!.isKmbCtbJoint,
        stops = Registry.getInstance(context).getAllStops(routeNumber, bound, co, gmbRegion).map { it.stop }
    )
}

fun List<Coordinates>.simplified(resolution: Double = 0.001, threshold: Int = 5000): List<Coordinates> {
    if (size < threshold.coerceAtLeast(2)) return this
    val simplifiedList = mutableListOf<Coordinates>()
    val first = first()
    val last = last()
    simplifiedList.add(first)
    var prevPosition = first
    for (position in this) {
        if (prevPosition.distance(position) >= resolution) {
            simplifiedList.add(position)
            prevPosition = position
        }
    }
    if (prevPosition != last) {
        simplifiedList.add(last)
    }
    return simplifiedList
}