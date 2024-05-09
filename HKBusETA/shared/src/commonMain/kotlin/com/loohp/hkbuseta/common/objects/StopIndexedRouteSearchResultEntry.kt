/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.Stable
import com.loohp.hkbuseta.common.utils.indexOf
import com.loohp.hkbuseta.common.utils.optBoolean
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import kotlinx.serialization.json.JsonObject


@Stable
class StopIndexedRouteSearchResultEntry(
    routeKey: String,
    route: Route?,
    co: Operator,
    stopInfo: StopInfo?,
    var stopInfoIndex: Int,
    origin: Coordinates?,
    isInterchangeSearch: Boolean,
    favouriteStopMode: FavouriteStopMode?
) : RouteSearchResultEntry(routeKey, route, co, stopInfo, origin, isInterchangeSearch, favouriteStopMode) {

    companion object {

        fun deserialize(json: JsonObject): StopIndexedRouteSearchResultEntry {
            val routeKey = json.optString("routeKey");
            val route = if (json.contains("route")) Route.deserialize(json.optJsonObject("route")!!) else null
            val co = Operator.valueOf(json.optString("co"));
            val stop = if (json.contains("stop")) StopInfo.deserialize(json.optJsonObject("stop")!!) else null
            val origin = if (json.contains("origin")) Coordinates.deserialize(json.optJsonObject("origin")!!) else null
            val isInterchangeSearch = json.optBoolean("isInterchangeSearch")
            val favouriteStopMode = if (json.contains("favouriteStopMode")) FavouriteStopMode.valueOf(json.optString("favouriteStopMode")) else null
            return StopIndexedRouteSearchResultEntry(routeKey, route, co, stop, 0, origin, isInterchangeSearch, favouriteStopMode)
        }

        fun fromRouteSearchResultEntry(resultEntry: RouteSearchResultEntry): StopIndexedRouteSearchResultEntry {
            return StopIndexedRouteSearchResultEntry(
                routeKey = resultEntry.routeKey,
                route = resultEntry.route,
                co = resultEntry.co,
                stopInfo = resultEntry.stopInfo,
                stopInfoIndex = 0,
                origin = resultEntry.origin,
                isInterchangeSearch = resultEntry.isInterchangeSearch,
                favouriteStopMode = resultEntry.favouriteStopMode
            )
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StopIndexedRouteSearchResultEntry) return false
        if (!super.equals(other)) return false

        return stopInfoIndex == other.stopInfoIndex
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + stopInfoIndex
        return result
    }

}

fun RouteSearchResultEntry.toStopIndexed(instance: AppContext): StopIndexedRouteSearchResultEntry {
    val stopIndexed = StopIndexedRouteSearchResultEntry.fromRouteSearchResultEntry(this)
    val route = this.route
    val co = this.co
    val stopInfo = this.stopInfo
    if (route != null && stopInfo != null) {
        stopIndexed.stopInfoIndex = Registry.getInstance(instance).getAllStops(route.routeNumber, route.bound[co]!!, co, route.gmbRegion).indexOfFirst { i -> i.stopId == stopInfo.stopId }
    }
    return stopIndexed
}

fun List<RouteSearchResultEntry>.toStopIndexed(instance: AppContext): List<StopIndexedRouteSearchResultEntry> {
    return map { it.toStopIndexed(instance) }
}

fun List<StopIndexedRouteSearchResultEntry>.bySortModes(
    context: AppContext,
    recentSortMode: RecentSortMode,
    includeFavouritesInRecent: Boolean,
    proximitySortOrigin: Coordinates? = null
): Map<RouteSortMode, List<StopIndexedRouteSearchResultEntry>> = buildMap {
    this[RouteSortMode.NORMAL] = this@bySortModes
    if (recentSortMode.enabled) {
        this[RouteSortMode.RECENT] = if (includeFavouritesInRecent) {
            val favouriteRoutes = Shared.favoriteRouteStops.value.flatMap { it.favouriteRouteStops }
            val interestedStops = Shared.getAllInterestedStops()
            sortedWith(compareBy<StopIndexedRouteSearchResultEntry> {
                favouriteRoutes.indexOfFirst { f -> it.route similarAs f.route }.takeIf { i -> i >= 0 }?: Int.MAX_VALUE
            }.thenBy {
                interestedStops.indexOfFirst { s -> it.route!!.stops.values.any { l -> l.contains(s) } }.takeIf { i -> i >= 0 }?: Int.MAX_VALUE
            }.thenBy {
                Shared.lastLookupRoutes.value.indexOf { l -> l.routeKey.asRoute(context) similarAs it.route }.takeIf { i -> i >= 0 }?: Int.MAX_VALUE
            })
        } else {
            sortedBy {
                Shared.lastLookupRoutes.value.indexOf { l -> l.routeKey == it.routeKey }.takeIf { i -> i >= 0 }?: Int.MAX_VALUE
            }
        }
    }
    if (proximitySortOrigin != null) {
        this[RouteSortMode.PROXIMITY] = if (recentSortMode.enabled) {
            sortedWith(compareBy({
                val location = it.stopInfo!!.data!!.location
                proximitySortOrigin.distance(location)
            }, {
                Shared.lastLookupRoutes.value.indexOf { l -> l.routeKey == it.routeKey }.takeIf { i -> i >= 0 }?: Int.MAX_VALUE
            }))
        } else {
            sortedBy {
                val location = it.stopInfo!!.data!!.location
                proximitySortOrigin.distance(location)
            }
        }
    }
}