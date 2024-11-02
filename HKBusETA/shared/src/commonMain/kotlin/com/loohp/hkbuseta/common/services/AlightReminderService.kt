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

package com.loohp.hkbuseta.common.services

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.getDeepLink
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.LocationResult
import com.loohp.hkbuseta.common.utils.MutableNullableStateFlow
import com.loohp.hkbuseta.common.utils.indexesOf
import com.loohp.hkbuseta.common.utils.optInt
import com.loohp.hkbuseta.common.utils.optString
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.absoluteValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


data class AlightReminderRemoteData(
    val routeNumber: String,
    val stopsRemaining: Int,
    val titleLeading: String,
    val titleTrailing: String,
    val content: String,
    val url: String,
    val state: AlightReminderServiceState,
    val active: AlightReminderActiveState
): JSONSerializable {
    companion object {
        fun deserialize(json: JsonObject): AlightReminderRemoteData {
            val routeNumber = json.optString("routeNumber")
            val stopsRemaining = json.optInt("stopsRemaining")
            val titleLeading = json.optString("titleLeading")
            val titleTrailing = json.optString("titleTrailing")
            val content = json.optString("content")
            val url = json.optString("url")
            val state = AlightReminderServiceState.valueOf(json.optString("state"))
            val active = AlightReminderActiveState.valueOf(json.optString("active"))
            return AlightReminderRemoteData(routeNumber, stopsRemaining, titleLeading, titleTrailing, content, url, state, active)
        }
    }
    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("routeNumber", routeNumber)
            put("stopsRemaining", stopsRemaining)
            put("titleLeading", titleLeading)
            put("titleTrailing", titleTrailing)
            put("content", content)
            put("url", url)
            put("state", state.name)
            put("active", active.name)
        }
    }
}

data class StopIdIndexed(val stopId: String, val index: Int)

enum class AlightReminderServiceState {
    TRAVELLING, ALMOST_THERE, ARRIVED
}
enum class AlightReminderActiveState {
    ACTIVE, ARRIVED, STOPPED
}

@OptIn(ExperimentalUuidApi::class)
class AlightReminderService private constructor(
    val context: AppContext,
    locationUpdater: (AppContext, Long, (LocationResult) -> Unit) -> Deferred<() -> Unit>,
    val selectedRoute: Route,
    val co: Operator,
    val originStopId: StopIdIndexed,
    val destinationStopId: StopIdIndexed
) {

    val instanceId = Uuid.random()

    val routeNumber = selectedRoute.routeNumber
    val bound = selectedRoute.idBound(co)
    val allStops = Registry.getInstance(context).getAllStops(routeNumber, bound, co, selectedRoute.gmbRegion)
    val allStopsOnBranch = if (co.isTrain) allStops else allStops.filter { it.branchIds.contains(selectedRoute) }
    val allStopsOnBranchBetween = allStops.subList(originStopId.index - 1, destinationStopId.index).let { s -> if (co.isTrain) s else s.filter { it.branchIds.contains(selectedRoute) } }

    val originStop = allStops[allStops.indexesOf { it.stopId == originStopId.stopId }.minBy { (originStopId.index - it).absoluteValue }]
    val destinationStop = allStops[allStops.indexesOf { it.stopId == destinationStopId.stopId }.minBy { (destinationStopId.index - it).absoluteValue }]

    init {
        context.startForegroundService(AppIntent(context, AppScreen.ALIGHT_REMINDER_SERVICE))
    }

    private val task = locationUpdater.invoke(context, Shared.ETA_UPDATE_INTERVAL.toLong()) { it.location?.apply { update(this) } }

    var currentStop = originStopId
        private set
    var state = AlightReminderServiceState.TRAVELLING
        private set
    var isActive = AlightReminderActiveState.ACTIVE
        private set

    val stopsRemaining: Int get() = allStops.subList(currentStop.index - 1, destinationStopId.index - 1).filter { it.branchIds.contains(selectedRoute) }.size
    val titleLeading: String get() = "${co.getDisplayRouteNumber(routeNumber)} ${selectedRoute.resolvedDest(true)[Shared.language]}"
    val titleTrailing: String get() = if (Shared.language == "en") {
        "Currently at ${allStops[currentStop.index - 1].stop.name.en}"
    } else {
        "目前在 ${allStops[currentStop.index - 1].stop.name.zh}"
    }
    val content: String get() = when (state) {
        AlightReminderServiceState.TRAVELLING -> if (Shared.language == "en") {
            "Going to ${destinationStop.stop.name.en}  ($stopsRemaining stops remaining)"
        } else {
            "正在前往 ${destinationStop.stop.name.zh}  (剩餘 $stopsRemaining 個站)"
        }
        AlightReminderServiceState.ALMOST_THERE -> if (Shared.language == "en") {
            "Arriving at ${destinationStop.stop.name.en}  ($stopsRemaining stops remaining)"
        } else {
            "即將到達 ${destinationStop.stop.name.zh}  (剩餘 $stopsRemaining 個站)"
        }
        AlightReminderServiceState.ARRIVED -> if (Shared.language == "en") {
            "Arrived at ${destinationStop.stop.name.en}"
        } else {
            "已到達 ${destinationStop.stop.name.zh}"
        }
    }
    val remoteContent: String get() = when (state) {
        AlightReminderServiceState.TRAVELLING -> if (Shared.language == "en") {
            "Going to ${destinationStop.stop.name.en}"
        } else {
            "正在前往 ${destinationStop.stop.name.zh}"
        }
        AlightReminderServiceState.ALMOST_THERE -> if (Shared.language == "en") {
            "Arriving at ${destinationStop.stop.name.en}"
        } else {
            "即將到達 ${destinationStop.stop.name.zh}"
        }
        AlightReminderServiceState.ARRIVED -> if (Shared.language == "en") {
            "Arrived at ${destinationStop.stop.name.en}"
        } else {
            "已到達 ${destinationStop.stop.name.zh}"
        }
    }
    val deepLink = selectedRoute.getDeepLink(context, null, null)
    val remoteData: AlightReminderRemoteData get() = AlightReminderRemoteData(
        routeNumber = routeNumber,
        stopsRemaining = stopsRemaining,
        titleLeading = titleLeading,
        titleTrailing = titleTrailing,
        content = remoteContent,
        url = deepLink,
        state = state,
        active = isActive
    )

    private fun update(location: Coordinates) {
        val closestStop = allStopsOnBranchBetween.minBy { location.distance(it.stop.location) }
        val closestStopIndex = allStops.indexOf(closestStop) + 1
        if (closestStopIndex > currentStop.index + 1 || location.distance(closestStop.stop.location) < 0.3) {
            currentStop = StopIdIndexed(closestStop.stopId, closestStopIndex)
        }
        val distanceToDestination = destinationStop.stop.location.distance(location)
        if ((distanceToDestination < 0.3 && currentStop.stopId == destinationStop.stopId) || originStop == destinationStop) {
            state = AlightReminderServiceState.ARRIVED
            terminate(false)
        } else if (distanceToDestination < 1.1 && stopsRemaining <= 5) {
            state = AlightReminderServiceState.ALMOST_THERE
            updateSubscribes.values.forEach { it.invoke() }
        } else {
            state = AlightReminderServiceState.TRAVELLING
            updateSubscribes.values.forEach { it.invoke() }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun terminate(killed: Boolean) {
        task.invokeOnCompletion { task.getCompleted().invoke() }
        isActive = if (killed) AlightReminderActiveState.STOPPED else AlightReminderActiveState.ARRIVED
        updateSubscribes.values.forEach { it.invoke() }
    }

    companion object {

        val updateSubscribes: MutableMap<String, () -> Unit> = ConcurrentMutableMap()
        val currentInstance: MutableNullableStateFlow<AlightReminderService> = MutableNullableStateFlow(null)

        fun startNewService(context: AppContext, locationUpdater: (AppContext, Long, (LocationResult) -> Unit) -> Deferred<() -> Unit>, selectedRoute: Route, co: Operator, originStopId: StopIdIndexed, destinationStopId: StopIdIndexed) {
            currentInstance.valueNullable?.terminate(true)
            currentInstance.valueNullable = AlightReminderService(context, locationUpdater, selectedRoute, co, originStopId, destinationStopId)
        }

        fun kill() {
            currentInstance.valueNullable?.terminate(true)
            currentInstance.valueNullable = null
        }

    }

}