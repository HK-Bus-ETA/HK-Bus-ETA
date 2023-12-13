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

package com.loohp.hkbuseta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Stable
import com.google.android.horologist.compose.ambient.AmbientAware
import com.loohp.hkbuseta.app.ListRouteMainElement
import com.loohp.hkbuseta.app.RecentSortMode
import com.loohp.hkbuseta.app.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.Route
import com.loohp.hkbuseta.objects.RouteListType
import com.loohp.hkbuseta.objects.StopInfo
import com.loohp.hkbuseta.objects.toCoordinates
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.utils.ifFalse
import com.loohp.hkbuseta.utils.mapToMutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


@Stable
class ListRoutesActivity : ComponentActivity() {

    private val sync: ExecutorService = Executors.newSingleThreadExecutor()
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(16)
    private val etaUpdatesMap: MutableMap<String, Pair<ScheduledFuture<*>?, () -> Unit>> = LinkedHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(this).ifFalse { return }
        Shared.setDefaultExceptionHandler(this)

        val result = Json.decodeFromString<JsonArray>(intent.extras!!.getString("result")!!).mapToMutableList { StopIndexedRouteSearchResultEntry.deserialize(it.jsonObject) }.also { list ->
            list.removeIf {
                if (it.route == null) {
                    val route = Registry.getInstance(appContext).findRouteByKey(it.routeKey, null)
                    if (route == null) {
                        return@removeIf true
                    } else {
                        it.route = route
                    }
                }
                if (it.stopInfo != null && it.stopInfo!!.data == null) {
                    val stop = Registry.getInstance(appContext).getStopById(it.stopInfo!!.stopId)
                    if (stop == null) {
                        return@removeIf true
                    } else {
                        it.stopInfo!!.data = stop
                    }
                }
                return@removeIf false
            }
        }.onEach {
            val route: Route? = it.route
            val co: Operator = it.co
            val stopInfo: StopInfo? = it.stopInfo
            if (route != null && stopInfo != null) {
                it.stopInfoIndex = Registry.getInstance(appContext).getAllStops(route.routeNumber, route.bound[co]!!, co, route.gmbRegion).indexOfFirst { i -> i.stopId == stopInfo.stopId }
            }
        }.toImmutableList()
        val listType = intent.extras!!.getString("listType")?.let { RouteListType.valueOf(it) }?: RouteListType.NORMAL
        val showEta = intent.extras!!.getBoolean("showEta", false)
        val recentSort = RecentSortMode.entries[intent.extras!!.getInt("recentSort", RecentSortMode.DISABLED.ordinal)]
        val proximitySortOrigin = intent.extras!!.getDoubleArray("proximitySortOrigin")?.toCoordinates()
        val allowAmbient = intent.extras!!.getBoolean("allowAmbient", false)

        setContent {
            AmbientAware (
                isAlwaysOnScreen = allowAmbient
            ) {
                ListRouteMainElement(it, appContext, result, listType, showEta, recentSort, proximitySortOrigin) { isAdd, key, task ->
                    sync.execute {
                        if (isAdd) {
                            etaUpdatesMap.computeIfAbsent(key) { executor.scheduleWithFixedDelay(task, 0, Shared.ETA_UPDATE_INTERVAL.toLong(), TimeUnit.MILLISECONDS) to task!! }
                        } else {
                            etaUpdatesMap.remove(key)?.first?.cancel(true)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Shared.setSelfAsCurrentActivity(this)
    }

    override fun onResume() {
        super.onResume()
        sync.execute {
            etaUpdatesMap.replaceAll { _, value ->
                value.first?.cancel(true)
                executor.scheduleWithFixedDelay(value.second, 0, Shared.ETA_UPDATE_INTERVAL.toLong(), TimeUnit.MILLISECONDS) to value.second
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sync.execute {
            etaUpdatesMap.forEach { it.value.first?.cancel(true) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            executor.shutdownNow()
            sync.shutdownNow()
            Shared.removeSelfFromCurrentActivity(this)
        }
    }

}