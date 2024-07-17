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
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.google.android.horologist.compose.ambient.AmbientAware
import com.loohp.hkbuseta.app.ListRouteMainElement
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.toCoordinates
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ifFalse
import com.loohp.hkbuseta.common.utils.mapToMutableList
import com.loohp.hkbuseta.compose.ambientMode
import com.loohp.hkbuseta.compose.rememberIsInAmbientMode
import com.loohp.hkbuseta.shared.WearOSShared
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
    private val etaUpdatesMap: MutableMap<String, Pair<ScheduledFuture<*>?, () -> Unit>> = linkedMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(appContext).ifFalse { return }
        WearOSShared.setDefaultExceptionHandler(this)

        val result = Json.decodeFromString<JsonArray>(intent.extras!!.getString("result")!!).mapToMutableList { StopIndexedRouteSearchResultEntry.deserialize(it.jsonObject) }.also { list ->
            list.removeAll {
                if (it.route == null) {
                    val route = Registry.getInstance(appContext).findRouteByKey(it.routeKey, null)
                    if (route == null) {
                        return@removeAll true
                    } else {
                        it.route = route
                    }
                }
                if (it.stopInfo != null && it.stopInfo!!.data == null) {
                    val stop = Registry.getInstance(appContext).getStopById(it.stopInfo!!.stopId)
                    if (stop == null) {
                        return@removeAll true
                    } else {
                        it.stopInfo!!.data = stop
                    }
                }
                return@removeAll false
            }
        }.onEach {
            val route = it.route
            val co = it.co
            val stopInfo = it.stopInfo
            if (route != null && stopInfo != null) {
                it.stopInfoIndex = Registry.getInstance(appContext).getAllStops(route.routeNumber, route.idBound(co), co, route.gmbRegion).indexOfFirst { i -> i.stopId == stopInfo.stopId }
            }
        }.toImmutableList()
        val listType = intent.extras!!.getString("listType")?.let { RouteListType.valueOf(it) }?: RouteListType.NORMAL
        val showEta = intent.extras!!.getBoolean("showEta", false)
        val recentSort = RecentSortMode.entries[intent.extras!!.getInt("recentSort", RecentSortMode.DISABLED.ordinal)]
        val proximitySortOrigin = intent.extras!!.getDoubleArray("proximitySortOrigin")?.toCoordinates()
        val allowAmbient = intent.extras!!.getBoolean("allowAmbient", false)
        val mtrSearch = intent.extras!!.getString("mtrSearch", null)

        setContent {
            AmbientAware (
                isAlwaysOnScreen = allowAmbient
            ) {
                val ambientMode = rememberIsInAmbientMode(it)
                Box (
                    modifier = Modifier.ambientMode(it)
                ) {
                    ListRouteMainElement(ambientMode, appContext, result, listType, showEta, recentSort, proximitySortOrigin, mtrSearch) { isAdd, key, task ->
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
    }

    override fun onStart() {
        super.onStart()
        WearOSShared.setSelfAsCurrentActivity(this)
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
            WearOSShared.removeSelfFromCurrentActivity(this)
        }
    }

}