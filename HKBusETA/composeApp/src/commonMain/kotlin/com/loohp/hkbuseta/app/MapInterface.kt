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

package com.loohp.hkbuseta.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteWaypoints
import com.loohp.hkbuseta.common.objects.asStop
import com.loohp.hkbuseta.common.objects.getCircularPivotIndex
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.buildImmutableList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf


@Immutable
data class MapRouteSection(
    val waypoints: RouteWaypoints,
    val stops: List<Registry.StopData>,
    val alternateStopNames: List<Registry.NearbyStopSearchResult>?
)

@Composable
fun MapRouteInterface(
    instance: AppActiveContext,
    waypoints: RouteWaypoints,
    stops: ImmutableList<Registry.StopData>,
    selectedStopState: MutableIntState,
    alternateStopNameShowing: Boolean,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>
) {
    val sections = remember(waypoints, stops, alternateStopNames) {
        persistentListOf(MapRouteSection(waypoints, stops, alternateStopNames.value))
    }
    val selectedSectionState = remember { mutableIntStateOf(0) }
    MapRouteInterface(
        instance = instance,
        sections = sections,
        selectedStopState = selectedStopState,
        selectedSectionState = selectedSectionState,
        alternateStopNameShowing = alternateStopNameShowing
    )
}

@Composable
expect fun MapRouteInterface(
    instance: AppActiveContext,
    sections: ImmutableList<MapRouteSection>,
    selectedStopState: MutableIntState,
    selectedSectionState: MutableIntState,
    alternateStopNameShowing: Boolean
)

@Composable
expect fun MapSelectInterface(
    instance: AppActiveContext,
    initialPosition: Coordinates,
    currentRadius: Float,
    onMove: (Coordinates, Float) -> Unit
)

expect val isMapOverlayAlwaysOnTop: Boolean

fun RouteWaypoints.buildStopListMapping(context: AppContext, allStops: List<Registry.StopData>): ImmutableList<Int> {
    return buildImmutableList {
        var waypointStopIndex = 0
        for ((index, stopData) in allStops.withIndex().drop(firstStopIndexOffset)) {
            val waypointStop = stops.getOrNull(waypointStopIndex)?: break
            if (waypointStop == stopData.stop || stopData.mergedStopIds.keys.any { it.asStop(context) == waypointStop }) {
                add(index)
                waypointStopIndex++
            }
        }
    }
}

fun Route.isStopOnOtherSideOfPivot(stopIndex: Int, selectedStop: Int, allStops: List<Registry.StopData>, indexMap: ImmutableList<Int>): Boolean {
    val pivotIndex = ((getCircularPivotIndex(allStops) - 1) downTo 0).asSequence().map { indexMap.indexOf(it) }.firstOrNull { it >= 0 }?: return false
    if (pivotIndex == stopIndex) return false
    val selectedIndex = ((selectedStop - 1) downTo 0).asSequence().map { indexMap.indexOf(it) }.firstOrNull { it >= 0 }?: 0
    val selectedAfterPivot = selectedIndex >= pivotIndex
    val stopAfterPivot = stopIndex >= pivotIndex
    return selectedAfterPivot != stopAfterPivot
}