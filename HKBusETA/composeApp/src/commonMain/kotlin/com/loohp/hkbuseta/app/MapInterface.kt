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

package com.loohp.hkbuseta.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteWaypoints
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.buildImmutableList
import kotlinx.collections.immutable.ImmutableList


@Composable
expect fun MapRouteInterface(
    instance: AppActiveContext,
    waypoints: RouteWaypoints,
    stops: ImmutableList<Registry.StopData>,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    alternateStopNameShowing: Boolean,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>
)

@Composable
expect fun MapSelectInterface(
    instance: AppActiveContext,
    initialPosition: Coordinates,
    currentRadius: Float,
    onMove: (Coordinates, Float) -> Unit
)

fun RouteWaypoints.buildStopListMapping(allStops: List<Registry.StopData>): ImmutableList<Int> {
    return buildImmutableList {
        var waypointStopIndex = 0
        for ((index, stopData) in allStops.withIndex()) {
            val waypointStop = stops.getOrNull(waypointStopIndex)?: break
            if (waypointStop == stopData.stop) {
                add(index)
                waypointStopIndex++
            }
        }
    }
}