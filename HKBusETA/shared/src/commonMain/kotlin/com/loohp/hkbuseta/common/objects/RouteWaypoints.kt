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

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.getOrClosest
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Immutable
data class RouteWaypoints(
    val routeNumber: String,
    val co: Operator,
    val isKmbCtbJoint: Boolean,
    val stopIds: List<String>,
    val stops: List<Stop>,
    val paths: List<List<Coordinates>> = listOf(element = stops.map { it.location }),
    val isHighRes: Boolean,
    val firstStopIndexOffset: Int = 0
) {
    fun subRoute(startingStopIndex: Int, endingStopIndex: Int, firstStopIndexOffset: Int): RouteWaypoints {
        return RouteWaypoints(
            routeNumber = routeNumber,
            co = co,
            isKmbCtbJoint = isKmbCtbJoint,
            stopIds = stopIds.subList(startingStopIndex, endingStopIndex + 1),
            stops = stops.subList(startingStopIndex, endingStopIndex + 1),
            paths = if (paths.size == stops.size - 1) {
                paths.subList(startingStopIndex, endingStopIndex)
            } else {
                paths.mapNotNull { it.subPathByClosest(stops[startingStopIndex].location, stops[endingStopIndex].location) }
            },
            isHighRes = isHighRes,
            firstStopIndexOffset = firstStopIndexOffset
        )
    }
}

fun Route.defaultWaypoints(context: AppContext): RouteWaypoints {
    val operator = co.firstCo()!!
    return RouteWaypoints(
        routeNumber = routeNumber,
        co = operator,
        isKmbCtbJoint = isKmbCtbJoint,
        stopIds = stops[operator]!!,
        stops = stops[operator]!!.map { it.asStop(context)!! },
        isHighRes = false
    )
}

fun RouteSearchResultEntry.defaultWaypoints(context: AppContext): RouteWaypoints {
    val routeNumber = route!!.routeNumber
    val bound = route!!.idBound(co)
    val gmbRegion = route!!.gmbRegion
    val stops = Registry.getInstance(context).getAllStops(routeNumber, bound, co, gmbRegion)
    return RouteWaypoints(
        routeNumber = routeNumber,
        co = co,
        isKmbCtbJoint = route!!.isKmbCtbJoint,
        stopIds = stops.map { it.stopId },
        stops = stops.map { it.stop },
        isHighRes = false
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

fun closestPointOnSegment(point: Coordinates, path1: Coordinates, path2: Coordinates): Coordinates {
    val (latP, lngP) = point
    val (lat1, lng1) = path1
    val (lat2, lng2) = path2
    val t = ((latP - lat1) * (lat2 - lat1) + (lngP - lng1) * (lng2 - lng1)) / ((lat2 - lat1).pow(2) + (lng2 - lng1).pow(2))
    val tClamped = max(0.0, min(1.0, t))
    val closestLat = lat1 + tClamped * (lat2 - lat1)
    val closestLng = lng1 + tClamped * (lng2 - lng1)
    return Coordinates(closestLat, closestLng)
}

fun List<Coordinates>.splitByClosestPoints(referencePoints: List<Coordinates>): List<List<Coordinates>> {
    val segments = Array<MutableList<Coordinates>>(referencePoints.size - 1) { mutableListOf() }

    var currentIndex = 1
    var lastPosition = first()

    for (refIndex in 1 until referencePoints.size) {
        val thisSegment = segments[refIndex - 1]
        thisSegment.add(lastPosition)
        val refPoint = referencePoints[refIndex]
        var minDistance = Double.MAX_VALUE
        var closestSegmentEndIndex = currentIndex
        for (i in currentIndex until size - 1) {
            val closest = closestPointOnSegment(refPoint, getOrClosest(i), getOrClosest(i + 1))
            val distanceToSegment = refPoint.distance(closest)
            if (distanceToSegment < minDistance) {
                minDistance = distanceToSegment
                closestSegmentEndIndex = i
            }
        }
        for (index in currentIndex..closestSegmentEndIndex) {
            thisSegment.add(getOrClosest(index))
        }
        if (thisSegment.size <= 1) {
            thisSegment.add(lastPosition)
        } else {
            lastPosition = thisSegment.last()
        }
        currentIndex = closestSegmentEndIndex + 1
    }

    for (index in currentIndex until size) {
        segments.last().add(getOrClosest(index))
    }

    return segments.asList()
}

fun List<Coordinates>.subPathByClosest(start: Coordinates, end: Coordinates): List<Coordinates>? {
    var closestStartIndex = 0
    var closestStartPoint = first()
    var minStartDistance = Double.MAX_VALUE

    for (index in 0 until size - 1) {
        val closestPoint = closestPointOnSegment(start, this[index], this[index + 1])
        val distance = closestPoint.distance(start)
        if (distance < minStartDistance) {
            closestStartIndex = index
            closestStartPoint = closestPoint
            minStartDistance = distance
        }
    }

    var closestEndIndex = closestStartIndex
    var closestEndPoint = closestStartPoint
    var minEndDistance = Double.MAX_VALUE

    for (index in closestStartIndex until size - 1) {
        val closestPoint = closestPointOnSegment(end, this[index], this[index + 1])
        val distance = closestPoint.distance(end)
        if (distance <= minEndDistance) {
            closestEndIndex = index
            closestEndPoint = closestPoint
            minEndDistance = distance
        }
    }

    return if (closestStartPoint.distance(closestEndPoint) < 0.001) {
        null
    } else {
        buildList {
            add(closestStartPoint)
            for (index in (closestStartIndex + 1)..closestEndIndex) {
                add(this@subPathByClosest[index])
            }
            add(closestEndPoint)
        }
    }
}

fun <T> List<Coordinates>.findPointsWithinDistanceOrdered(items: List<T>, itemLocation: T.() -> Coordinates, threshold: Double): List<T> {
    val result = mutableListOf<T>()
    val usedItems: MutableSet<T> = mutableSetOf()
    for (i in 0 until size - 1) {
        val pathStart = this[i]
        val pathEnd = this[i + 1]
        val pathItems = mutableMapOf<T, Double>()
        for (item in items) {
            if (!usedItems.contains(item)) {
                val point = itemLocation.invoke(item)
                val closestPoint = closestPointOnSegment(point, pathStart, pathEnd)
                val distanceToLine = point.distance(closestPoint)
                if (distanceToLine <= threshold) {
                    val distanceAlongPath = pathStart.distance(closestPoint)
                    usedItems.add(item)
                    pathItems[item] = distanceAlongPath
                }
            }
        }
        pathItems.asSequence().sortedBy { (_, distance) -> distance }.forEach { (item) -> result.add(item) }
    }
    return result
}