/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2026. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2026. Contributors
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

import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.RouteWaypoints
import com.loohp.hkbuseta.common.objects.Stop
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.hypot
 
const val ROUTE_ARROW_SPACING = 96.0
const val ROUTE_ARROW_COLLISION_DISTANCE = 48.0
const val ROUTE_ARROW_OPPOSING_COLLISION_DISTANCE = 14.0
const val ROUTE_ARROW_STOP_CLEARANCE = 24.0
const val ROUTE_ARROW_MIN_ROUTE_LENGTH = 40.0
const val ROUTE_ARROW_MAX_VISIBLE = 24

data class ProjectedRoutePoint(
    val location: Coordinates,
    val x: Double,
    val y: Double
)

data class ProjectedScreenPoint(
    val x: Double,
    val y: Double
)

data class ProjectedScreenBounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double
) {
    fun contains(x: Double, y: Double): Boolean = x in left..right && y in top..bottom
}

data class RouteDirectionArrow(
    val location: Coordinates,
    val x: Double,
    val y: Double,
    val rotation: Float
)

fun calculateRouteDirectionArrows(
    paths: List<List<ProjectedRoutePoint>>,
    stops: List<ProjectedScreenPoint>,
    bounds: ProjectedScreenBounds,
    occupiedArrowPoints: List<ProjectedScreenPoint> = emptyList(),
    spacing: Double = ROUTE_ARROW_SPACING,
    collisionDistance: Double = ROUTE_ARROW_COLLISION_DISTANCE,
    stopClearance: Double = ROUTE_ARROW_STOP_CLEARANCE,
    maxVisible: Int = ROUTE_ARROW_MAX_VISIBLE
): List<RouteDirectionArrow> {
    if (spacing <= 0.0 || maxVisible <= 0) return emptyList()

    val segments = paths.flatMap { path ->
        path.zipWithNext().mapNotNull { (start, end) ->
            val length = hypot(end.x - start.x, end.y - start.y)
            if (length.isFinite() && length > 0.0) ProjectedSegment(start, end, length) else null
        }
    }
    val totalLength = segments.sumOf { it.length }
    if (!totalLength.isFinite() || totalLength < ROUTE_ARROW_MIN_ROUTE_LENGTH) return emptyList()

    val result = mutableListOf<RouteDirectionArrow>()
    var traversed = 0.0
    var nextArrowDistance = if (totalLength < spacing) totalLength / 2.0 else spacing / 2.0
    val lastArrowDistance = if (totalLength < spacing) nextArrowDistance else totalLength - spacing / 2.0
    var opposingPhaseShifted = false

    for (segment in segments) {
        val segmentEnd = traversed + segment.length
        while (nextArrowDistance <= segmentEnd && nextArrowDistance <= lastArrowDistance && result.size < maxVisible) {
            val fraction = ((nextArrowDistance - traversed) / segment.length).coerceIn(0.0, 1.0)
            val x = segment.start.x + (segment.end.x - segment.start.x) * fraction
            val y = segment.start.y + (segment.end.y - segment.start.y) * fraction
            val rotation = ((atan2(segment.end.y - segment.start.y, segment.end.x - segment.start.x) * 180.0 / PI + 90.0) % 360.0 + 360.0) % 360.0
            val acceptedConflicts = result.mapNotNull {
                val distance = hypot(it.x - x, it.y - y)
                if (distance < collisionDistance) it to distance else null
            }
            val hardOpposingOverlap = acceptedConflicts.any { (arrow, distance) ->
                arrow.rotation.angleDifference(rotation) >= 120.0 && distance < ROUTE_ARROW_OPPOSING_COLLISION_DISTANCE
            }
            if (hardOpposingOverlap && !opposingPhaseShifted && nextArrowDistance + spacing / 2.0 <= lastArrowDistance) {
                nextArrowDistance += spacing / 2.0
                opposingPhaseShifted = true
                continue
            }
            if (bounds.contains(x, y) &&
                stops.none { it.distanceTo(x, y) < stopClearance } &&
                occupiedArrowPoints.none { it.distanceTo(x, y) < collisionDistance } &&
                acceptedConflicts.none { (arrow, distance) ->
                    arrow.rotation.angleDifference(rotation) < 120.0 || distance < ROUTE_ARROW_OPPOSING_COLLISION_DISTANCE
                }
            ) {
                result += RouteDirectionArrow(
                    location = Coordinates(
                        segment.start.location.lat + (segment.end.location.lat - segment.start.location.lat) * fraction,
                        segment.start.location.lng + (segment.end.location.lng - segment.start.location.lng) * fraction
                    ),
                    x = x,
                    y = y,
                    rotation = rotation.toFloat()
                )
            }
            nextArrowDistance += spacing
        }
        if (result.size >= maxVisible) break
        traversed = segmentEnd
    }
    return result
}

fun RouteWaypoints.pathsInRouteDirection(): List<List<Coordinates>> {
    if (stops.size < 2) return paths
    val isCircular = stopIds.firstOrNull() == stopIds.lastOrNull() ||
        (stops.first().location distance stops.last().location) < 0.05
    val orderedStops = if (isCircular && stops.first().location distance stops.last().location < 0.05) stops.dropLast(1) else stops

    if (!isCircular && paths.size == stops.size - 1) {
        return paths.mapIndexed { index, path ->
            if (path.size < 2) path else path.orientedBetween(stops[index].location, stops[index + 1].location)
        }
    }

    if (!isCircular && paths.size != stops.size - 1 && paths.isNotEmpty()) {
        paths.orientedConnectedChain(orderedStops)?.let { return it }
        val mainPathIndex = paths.indices.maxBy { paths[it].geographicLength() }
        val mainPath = paths[mainPathIndex].orientedBy(orderedStops)
        return paths.mapIndexed { index, path ->
            when {
                path.size < 2 -> path
                index == mainPathIndex -> mainPath
                else -> path.orientedByMainLineOrStops(mainPath, orderedStops)
            }
        }
    }

    var previousPathEnd: Coordinates? = null

    return paths.map { path ->
        if (path.size < 2) return@map path
        val previousEnd = previousPathEnd
        val orientedPath = if (isCircular && previousEnd != null) {
            if ((path.first() distance previousEnd) <= (path.last() distance previousEnd)) path else path.reversed()
        } else {
            path.orientedBy(orderedStops)
        }
        previousPathEnd = orientedPath.last()
        orientedPath
    }
}

private fun List<List<Coordinates>>.orientedConnectedChain(orderedStops: List<Stop>): List<List<Coordinates>>? {
    if (isEmpty() || any { it.size < 2 }) return null
    val result = ArrayList<List<Coordinates>>(size)
    var previousEnd: Coordinates? = null
    forEachIndexed { index, path ->
        val orientedPath = if (index == 0) {
            path.orientedBy(orderedStops)
        } else {
            val end = previousEnd!!
            val distanceToFirst = end distance path.first()
            val distanceToLast = end distance path.last()
            if (minOf(distanceToFirst, distanceToLast) > 0.05) return null
            if (distanceToFirst <= distanceToLast) path else path.reversed()
        }
        result += orientedPath
        previousEnd = orientedPath.last()
    }
    return result
}

private fun List<Coordinates>.orientedByMainLineOrStops(mainPath: List<Coordinates>, orderedStops: List<Stop>): List<Coordinates> {
    val startAttachment = mainPath.closestIndexAndDistance(first())
    val endAttachment = mainPath.closestIndexAndDistance(last())
    return if (startAttachment.second < 0.75 && endAttachment.second < 0.75 && startAttachment.first != endAttachment.first) {
        if (startAttachment.first < endAttachment.first) this else reversed()
    } else {
        orientedBy(orderedStops)
    }
}

private fun List<Coordinates>.orientedBy(orderedStops: List<Stop>): List<Coordinates> {
    val directScore = orderedEndpointScore(orderedStops)
    val reversed = reversed()
    val reverseScore = reversed.orderedEndpointScore(orderedStops)
    return if (abs(directScore - reverseScore) < 0.000001) {
        if (orderViolationScore(orderedStops) <= reversed.orderViolationScore(orderedStops)) this else reversed
    } else if (directScore < reverseScore) this else reversed
}

private fun List<Coordinates>.closestIndexAndDistance(location: Coordinates): Pair<Int, Double> {
    val index = indices.minBy { this[it] distance location }
    return index to (this[index] distance location)
}

private fun List<Coordinates>.geographicLength(): Double = zipWithNext().sumOf { (start, end) -> start distance end }

private fun List<Coordinates>.orientedBetween(start: Coordinates, end: Coordinates): List<Coordinates> {
    val directDistance = (first() distance start) + (last() distance end)
    val reverseDistance = (last() distance start) + (first() distance end)
    return if (reverseDistance < directDistance) reversed() else this
}

private fun List<Coordinates>.orderedEndpointScore(orderedStops: List<Stop>): Double {
    if (orderedStops.size < 2) return Double.POSITIVE_INFINITY
    var score = Double.POSITIVE_INFINITY
    for (startIndex in 0 until orderedStops.lastIndex) {
        for (endIndex in startIndex + 1..orderedStops.lastIndex) {
            score = minOf(score, (first() distance orderedStops[startIndex].location) + (last() distance orderedStops[endIndex].location))
        }
    }
    return score
}

private fun List<Coordinates>.orderViolationScore(orderedStops: List<Stop>): Long {
    var previousIndex = -1
    var score = 0L
    for (stop in orderedStops) {
        val closestIndex = indices.minBy { this[it] distance stop.location }
        if (closestIndex < previousIndex) score += previousIndex - closestIndex + 1L
        previousIndex = maxOf(previousIndex, closestIndex)
    }
    return score
}

private data class ProjectedSegment(
    val start: ProjectedRoutePoint,
    val end: ProjectedRoutePoint,
    val length: Double
)

private fun ProjectedScreenPoint.distanceTo(x: Double, y: Double): Double = hypot(this.x - x, this.y - y)

private fun Float.angleDifference(other: Double): Double {
    val difference = (toDouble() - other).absoluteValue % 360.0
    return minOf(difference, 360.0 - difference)
}
