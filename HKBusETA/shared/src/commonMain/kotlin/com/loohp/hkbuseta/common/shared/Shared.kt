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

package com.loohp.hkbuseta.common.shared

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.value
import co.touchlab.stately.concurrency.withLock
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.AlertNotification
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.ETADisplayMode
import com.loohp.hkbuseta.common.objects.FavouriteResolvedStop
import com.loohp.hkbuseta.common.objects.FavouriteRouteGroup
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.FavouriteStop
import com.loohp.hkbuseta.common.objects.FavouriteStopMode
import com.loohp.hkbuseta.common.objects.GMBRegion
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.LastLookupRoute
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RadiusCenterPosition
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.RouteSortPreference
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.StopInfo
import com.loohp.hkbuseta.common.objects.TemporaryPinItem
import com.loohp.hkbuseta.common.objects.Theme
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getMtrLineName
import com.loohp.hkbuseta.common.objects.getMtrLineSortingIndex
import com.loohp.hkbuseta.common.objects.getRouteKey
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.isInterested
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.putExtra
import com.loohp.hkbuseta.common.objects.resolveStop
import com.loohp.hkbuseta.common.objects.uniqueKey
import com.loohp.hkbuseta.common.objects.url
import com.loohp.hkbuseta.common.utils.BoldStyle
import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.JsonIgnoreUnknownKeys
import com.loohp.hkbuseta.common.utils.MutableNonNullStateFlow
import com.loohp.hkbuseta.common.utils.MutableNonNullStateFlowList
import com.loohp.hkbuseta.common.utils.SmallSize
import com.loohp.hkbuseta.common.utils.asFormattedText
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.plus
import com.loohp.hkbuseta.common.utils.toLocalDateTime
import com.loohp.hkbuseta.common.utils.wrap
import com.loohp.hkbuseta.common.utils.wrapList
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.math.absoluteValue
import kotlin.native.ObjCName
import kotlin.time.DurationUnit
import kotlin.time.toDuration


expect val BASE_URL: String
expect val JOINT_OPERATED_COLOR_REFRESH_RATE: Long

@Immutable
object Shared {

    const val DATA_DOMAIN = "https://data.hkbuseta.com"
    const val SPLASH_DOMAIN = "https://splash.hkbuseta.com"

    const val ETA_UPDATE_INTERVAL: Int = 15000
    const val ETA_FRESHNESS: Int = 60000

    const val START_ACTIVITY_ID = "/HKBusETA/Launch"
    const val SYNC_PREFERENCES_ID = "/HKBusETA/SyncPreference"
    const val REQUEST_PREFERENCES_ID = "/HKBusETA/RequestPreference"
    const val REQUEST_ALIGHT_REMINDER_ID = "/HKBusETA/RequestAlightReminder"
    const val RESPONSE_ALIGHT_REMINDER_ID = "/HKBusETA/ResponseAlightReminder"
    const val UPDATE_ALIGHT_REMINDER_ID = "/HKBusETA/UpdateAlightReminder"
    const val TERMINATE_ALIGHT_REMINDER_ID = "/HKBusETA/TerminateAlightReminder"
    const val INVALIDATE_CACHE_ID = "/HKBusETA/InvalidateCache"

    const val JOURNEY_PLANNER_AVAILABLE = false

    val MTR_ROUTE_FILTER: (Route) -> Boolean = { r -> r.bound.containsKey(Operator.MTR) }
    val FERRY_ROUTE_FILTER: (Route) -> Boolean = { r -> r.bound.keys.any { it.isFerry } }
    val RECENT_ROUTE_FILTER: (String, Route, Operator) -> Boolean = { k, _, _ -> lastLookupRoutes.value.any { it.routeKey == k } }

    suspend fun invalidateCache(context: AppContext) {
        try {
            Registry.invalidateCache(context)
        } catch (_: Throwable) {}
    }

    private val backgroundUpdateScheduler: AtomicReference<(AppContext, Long) -> Unit> = AtomicReference { _, _ -> }

    fun provideBackgroundUpdateScheduler(runnable: (AppContext, Long) -> Unit) {
        backgroundUpdateScheduler.value = runnable
    }

    fun scheduleBackgroundUpdateService(context: AppContext, time: Long) {
        backgroundUpdateScheduler.value.invoke(context, time)
    }

    var isWearOS: Boolean = false
        private set

    fun setIsWearOS() {
        isWearOS = true
    }

    fun ensureRegistryDataAvailable(context: AppActiveContext): Boolean {
        return if (!Registry.hasInstanceCreated() || Registry.getInstanceNoUpdateCheck(context).state.value.isProcessing) {
            val intent = AppIntent(context, AppScreen.MAIN)
            intent.addFlags(AppIntentFlag.NEW_TASK, AppIntentFlag.CLEAR_TASK)
            context.startActivity(intent)
            context.finishAffinity()
            false
        } else {
            true
        }
    }

    internal val kmbSubsidiary: Map<String, KMBSubsidiary> = mutableMapOf()

    fun setKmbSubsidiary(values: Map<KMBSubsidiary, List<String>>) {
        kmbSubsidiary as MutableMap
        kmbSubsidiary.clear()
        for ((type, list) in values) {
            for (route in list) {
                kmbSubsidiary.put(route, type)
            }
        }
    }

    private const val jointOperatorColorTransitionTime: Long = 5000
    val jointOperatedColorFractionState: MutableNonNullStateFlow<Float> = MutableStateFlow(0F).wrap()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                val startTime = currentTimeMillis()
                while (currentTimeMillis() - startTime < jointOperatorColorTransitionTime) {
                    val progress = (currentTimeMillis() - startTime).toFloat() / jointOperatorColorTransitionTime
                    jointOperatedColorFractionState.value = progress
                    delay(JOINT_OPERATED_COLOR_REFRESH_RATE)
                }
                val yellowToRedStartTime = currentTimeMillis()
                while (currentTimeMillis() - yellowToRedStartTime < jointOperatorColorTransitionTime) {
                    val progress = (currentTimeMillis() - yellowToRedStartTime).toFloat() / jointOperatorColorTransitionTime
                    jointOperatedColorFractionState.value = 1F - progress
                    delay(JOINT_OPERATED_COLOR_REFRESH_RATE)
                }
            }
        }
    }

    fun Registry.ETAQueryResult?.getResolvedText(seq: Int, etaDisplayMode: ETADisplayMode, context: AppContext): Registry.ETALineEntryText {
        return this?.getLine(seq)?.let {
            if (etaDisplayMode.hasClockTime && it.etaRounded >= 0) {
                it.text.withDisplayMode(context.formatTime(time.toLocalDateTime() + it.eta.toDuration(DurationUnit.MINUTES)).asFormattedText(), etaDisplayMode)
            } else {
                it.text
            }
        }?: if (seq == 1) {
            Registry.ETALineEntryText.remark(if (language == "en") "Updating" else "更新中")
        } else {
            Registry.ETALineEntryText.EMPTY_NOTHING
        }
    }

    fun List<Registry.ETALineEntry>.getResolvedText(seq: Int, etaDisplayMode: ETADisplayMode, time: Long, context: AppContext): Registry.ETALineEntryText {
        return this.getOrNull(seq - 1)?.let {
            if (etaDisplayMode.hasClockTime && it.etaRounded >= 0) {
                it.text.withDisplayMode(context.formatTime(time.toLocalDateTime() + it.eta.toDuration(DurationUnit.MINUTES)).asFormattedText(), etaDisplayMode)
            } else {
                it.text
            }
        }?: if (seq == 1) {
            Registry.ETALineEntryText.remark(if (language == "en") "Updating" else "更新中")
        } else {
            Registry.ETALineEntryText.EMPTY_NOTHING
        }
    }

    @ObjCName("getResolvedTextWithStopIndexedRouteSearchResultEntry")
    fun Registry.MergedETAQueryResult<StopIndexedRouteSearchResultEntry>?.getResolvedText(seq: Int, etaDisplayMode: ETADisplayMode, context: AppContext): Triple<StopIndexedRouteSearchResultEntry?, FormattedText, Registry.ETALineEntryText> {
        if (this == null) {
            return Triple(null, if (seq == 1) (if (language == "en") "Updating" else "更新中").asFormattedText() else "".asFormattedText(), Registry.ETALineEntryText.EMPTY_NOTHING)
        }
        val line = this[seq]
        val lineRoute = line.first?.let { it.co.getDisplayRouteNumber(it.route!!.routeNumber, true).asFormattedText(BoldStyle) }
        val noRouteNumber = lineRoute == null ||
                (1..4).all { this[it].first?.route?.routeNumber.let { route -> route == null || route == line.first?.route?.routeNumber } } ||
                (1..4).all { this[it].first?.co?.isTrain == true } ||
                this.mergedCount <= 1
        return Triple(
            first = line.first,
            second = if (noRouteNumber) "".asFormattedText() else lineRoute!!,
            third = line.second.text.let { if (etaDisplayMode.hasClockTime && line.second.etaRounded >= 0) it.withDisplayMode(context.formatTime(time.toLocalDateTime() + line.second.eta.toDuration(DurationUnit.MINUTES)).asFormattedText(), etaDisplayMode) else it },
        )
    }

    @ObjCName("getResolvedTextWithFavouriteResolvedStopFavouriteRouteStopPair")
    fun Registry.MergedETAQueryResult<Pair<FavouriteResolvedStop, FavouriteRouteStop>>?.getResolvedText(seq: Int, etaDisplayMode: ETADisplayMode, context: AppContext): Pair<Pair<FavouriteResolvedStop, FavouriteRouteStop>?, FormattedText> {
        if (this == null) {
            return null to if (seq == 1) (if (language == "en") "Updating" else "更新中").asFormattedText() else "".asFormattedText()
        }
        val line = this[seq]
        val lineRoute = line.first?.let { it.second.co.getDisplayRouteNumber(it.second.route.routeNumber, true) }
        val noRouteNumber = lineRoute == null ||
                (1..3).all { this[it].first?.second?.route?.routeNumber.let { route -> route == null || route == line.first?.second?.route?.routeNumber } } ||
                this.allKeys.all { it.second.co.isTrain } ||
                this.mergedCount <= 1
        return line.first to (if (noRouteNumber) "".asFormattedText() else "$lineRoute > ".asFormattedText(SmallSize))
            .plus(line.second.text.let { if (etaDisplayMode.hasClockTime && line.second.etaRounded >= 0) it.withDisplayMode(context.formatTime(time.toLocalDateTime() + line.second.eta.toDuration(DurationUnit.MINUTES)).asFormattedText(), etaDisplayMode) else it })
    }

    fun getMtrLineSortingIndex(lineName: String): Int {
        return lineName.getMtrLineSortingIndex()
    }

    fun getMtrLineName(lineName: String): String {
        return lineName.getMtrLineName()[language]
    }

    fun getMtrLineName(lineName: String, orElse: String): String {
        return lineName.getMtrLineName { orElse.asBilingualText() }[language]
    }

    private val pinnedItemsLock: Lock = Lock()
    val pinnedItems: MutableNonNullStateFlowList<TemporaryPinItem> = MutableStateFlow(emptyList<TemporaryPinItem>()).wrapList()

    fun togglePinnedItems(item: TemporaryPinItem) {
        pinnedItemsLock.withLock {
            pinnedItems.value = pinnedItems.value.toMutableList().apply {
                if (!removeAll { it.key == item.key }) {
                    add(item)
                }
            }
        }
    }

    var language = "zh"
    var etaDisplayMode = ETADisplayMode.COUNTDOWN
    var lrtDirectionMode = false
    var theme = Theme.SYSTEM
    var color: Long? = null
    var viewFavTab = 0
    var disableMarquee = false
    var historyEnabled = true
    var showRouteMap = true
    var downloadSplash = true
    var lastNearbyLocation: RadiusCenterPosition? = null
    var disableNavBarQuickActions = false
    var disableBoldDest = false

    val alternateStopNamesShowingState: MutableNonNullStateFlow<Boolean> = MutableStateFlow(false).wrap()

    private val favouriteRouteStopLock: Lock = Lock()
    val favoriteRouteStops: MutableNonNullStateFlowList<FavouriteRouteGroup> = MutableStateFlow(emptyList<FavouriteRouteGroup>()).wrapList()

    private val favouriteStopLock: Lock = Lock()
    val favoriteStops: MutableNonNullStateFlowList<FavouriteStop> = MutableStateFlow(emptyList<FavouriteStop>()).wrapList()

    val shouldShowFavListRouteView: Boolean get() = favoriteRouteStops.value.flatMap { it.favouriteRouteStops }.count() > 2

    fun sortedForListRouteView(instance: AppContext, origin: Coordinates?): List<RouteSearchResultEntry> {
        return favoriteRouteStops.value.asSequence().flatMap { it.favouriteRouteStops }
            .map { fav ->
                val (index, stopId, stop, route) = fav.resolveStop(instance) { origin }
                val favouriteStopMode = fav.favouriteStopMode
                RouteSearchResultEntry(route.getRouteKey(instance)!!, route, fav.co, StopInfo(stopId, stop, 0.0, fav.co, index), null, false, favouriteStopMode)
            }
            .distinctBy { routeEntry -> routeEntry.uniqueKey }
            .toList()
    }

    fun updateFavoriteRouteStops(mutation: (MutableStateFlow<List<FavouriteRouteGroup>>) -> Unit) {
        favouriteRouteStopLock.withLock {
            mutation.invoke(favoriteRouteStops)
        }
    }

    fun updateFavoriteStops(mutation: (MutableStateFlow<List<FavouriteStop>>) -> Unit) {
        favouriteStopLock.withLock {
            mutation.invoke(favoriteStops)
        }
    }

    fun getAllInterestedStops(): List<String> {
        return buildList {
            favoriteStops.value.forEach {
                add(it.stopId)
            }
            favoriteRouteStops.value.asSequence()
                .flatMap { it.favouriteRouteStops }
                .forEach { when (it.favouriteStopMode) {
                    FavouriteStopMode.FIXED -> add(it.stopId)
                    FavouriteStopMode.CLOSEST -> it.route.stops[it.co]?.let { l -> addAll(l) }
                } }
        }
    }

    private const val LAST_LOOKUP_ROUTES_MEM_SIZE = 100
    private val lastLookupRouteLock: Lock = Lock()
    val lastLookupRoutes: MutableNonNullStateFlowList<LastLookupRoute> = MutableStateFlow(emptyList<LastLookupRoute>()).wrapList()

    fun addLookupRoute(routeKey: String, time: Long = currentTimeMillis()) {
        addLookupRoute(LastLookupRoute(routeKey, time))
    }

    fun addLookupRoute(lastLookupRoute: LastLookupRoute) {
        lastLookupRouteLock.withLock {
            lastLookupRoutes.value = lastLookupRoutes.value.toMutableList().apply {
                removeAll { it.routeKey == lastLookupRoute.routeKey }
                add(0, lastLookupRoute)
                while (size > LAST_LOOKUP_ROUTES_MEM_SIZE) {
                    removeLastOrNull()
                }
            }
        }
    }

    fun removeLookupRoute(routeKey: String) {
        lastLookupRouteLock.withLock {
            lastLookupRoutes.value = lastLookupRoutes.value.toMutableList().apply {
                removeAll { it.routeKey == routeKey }
            }
        }
    }

    fun clearLookupRoute() {
        lastLookupRouteLock.withLock {
            lastLookupRoutes.value = emptyList()
        }
    }

    fun findLookupRouteTime(routeKey: String): Long? {
        return lastLookupRoutes.value.asSequence().filter { it.routeKey == routeKey }.maxOfOrNull { it.time }
    }

    val routeSortModePreference: Map<RouteListType, RouteSortPreference> = ConcurrentMutableMap()

    @Suppress("NAME_SHADOWING")
    fun handleLaunchOptions(
        instance: AppActiveContext,
        stopId: String?,
        co: Operator?,
        index: Int?,
        stop: Any?,
        route: Any?,
        listStopRoute: ByteArray?,
        listStopScrollToStop: String?,
        listStopShowEta: Boolean?,
        queryKey: String?,
        queryRouteNumber: String?,
        queryBound: String?,
        queryCo: Operator?,
        queryDest: String?,
        queryGMBRegion: GMBRegion?,
        queryStop: String?,
        queryStopIndex: Int,
        queryStopDirectLaunch: Boolean,
        appScreen: AppScreen?,
        noAnimation: Boolean,
        skipTitle: Boolean,
        orElse: () -> Unit
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            while (Registry.getInstance(instance).state.value.isProcessing) {
                delay(100)
            }
            if (appScreen != null) {
                val flags = if (noAnimation) arrayOf(AppIntentFlag.NO_ANIMATION) else emptyArray()
                instance.startActivity(AppIntent(instance, AppScreen.TITLE).apply { addFlags(*flags) })
                when (appScreen) {
                    AppScreen.SEARCH, AppScreen.FAV, AppScreen.NEARBY, AppScreen.SETTINGS -> {
                        instance.startActivity(AppIntent(instance, appScreen).apply { addFlags(*flags) })
                    }
                    else -> { /* do nothing */ }
                }
                instance.finishAffinity()
                return@launch
            }
            val stop = stop
            val route = route
            val listStopRoute = listStopRoute
            var queryRouteNumber = queryRouteNumber
            var queryCo = queryCo
            var queryBound = queryBound
            var queryGMBRegion = queryGMBRegion
            if (stopId != null && co != null && (stop is String || stop is ByteArray) && (route is String || route is ByteArray)) {
                val routeParsed = if (route is String) Route.deserialize(Json.decodeFromString<JsonObject>(route)) else Route.deserialize(ByteReadChannel(route as ByteArray))
                Registry.getInstance(instance).findRoutes(routeParsed.routeNumber, true) { it ->
                    val bound = it.bound
                    if (!bound.containsKey(co) || bound[co] != routeParsed.bound[co]) {
                        return@findRoutes false
                    }
                    val stops = it.stops[co]?: return@findRoutes false
                    return@findRoutes stops.contains(stopId)
                }.firstOrNull()?.let {
                    val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                    intent.putExtra("shouldRelaunch", false)
                    intent.putExtra("route", it)
                    intent.putExtra("scrollToStop", stopId)
                    instance.startActivity(intent)
                }

                val intent = AppIntent(instance, AppScreen.ETA)
                intent.putExtra("shouldRelaunch", false)
                intent.putExtra("stopId", stopId)
                intent.putExtra("co", co.name)
                intent.putExtra("index", index!!)
                if (stop is String) {
                    intent.putExtra("stopStr", stop)
                } else {
                    intent.putExtra("stop", stop as ByteArray)
                }
                if (route is String) {
                    intent.putExtra("routeStr", route)
                } else {
                    intent.putExtra("route", route as ByteArray)
                }
                instance.startActivity(intent)
                instance.finish()
            } else if (listStopRoute != null && listStopScrollToStop != null && listStopShowEta != null) {
                val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                intent.putExtra("route", listStopRoute)
                intent.putExtra("scrollToStop", listStopScrollToStop)
                intent.putExtra("showEta", listStopShowEta)
                instance.startActivity(intent)
                instance.finish()
            } else if (queryRouteNumber != null || queryKey != null) {
                if (queryKey != null) {
                    val routeNumber = "^([0-9a-zA-Z]+)".toRegex().find(queryKey)?.groupValues?.getOrNull(1)
                    val nearestRoute = Registry.getInstance(instance).findRouteByKey(queryKey, routeNumber)
                    queryRouteNumber = nearestRoute!!.routeNumber
                    queryCo = if (nearestRoute.isKmbCtbJoint) Operator.KMB else nearestRoute.co[0]
                    queryBound = if (queryCo == Operator.NLB) nearestRoute.nlbId else nearestRoute.bound[queryCo]
                    queryGMBRegion = nearestRoute.gmbRegion
                }

                if (!skipTitle) {
                    instance.startActivity(AppIntent(instance, AppScreen.TITLE))
                }

                val result = Registry.getInstance(instance).findRoutes(queryRouteNumber?: "", true)
                if (result.isNotEmpty()) {
                    var filteredResult = result.asSequence().filter {
                        return@filter when (queryCo) {
                            Operator.NLB -> it.co == queryCo && (queryBound == null || it.route!!.nlbId == queryBound)
                            Operator.GMB -> {
                                val r = it.route!!
                                it.co == queryCo && (queryBound == null || r.bound[queryCo] == queryBound) && (queryGMBRegion == null || r.gmbRegion == queryGMBRegion)
                            }
                            else -> (queryCo == null || it.co == queryCo) && (queryBound == null || it.route!!.bound[queryCo] == queryBound)
                        }
                    }.toList()
                    if (queryDest != null) {
                        val destFiltered = filteredResult.asSequence().filter {
                            val dest = it.route!!.dest
                            return@filter queryDest == dest.zh || queryDest == dest.en
                        }.toList()
                        if (destFiltered.isNotEmpty()) {
                            filteredResult = destFiltered
                        }
                    }
                    if (filteredResult.isEmpty()) {
                        val intent = AppIntent(instance, AppScreen.LIST_ROUTES)
                        intent.putExtra("result", result.asSequence().map { it.deepClone() })
                        instance.startActivity(intent)
                    } else {
                        val intent = AppIntent(instance, AppScreen.LIST_ROUTES)
                        intent.putExtra("result", filteredResult.asSequence().map { it.deepClone() })
                        instance.startActivity(intent)

                        val it = filteredResult[0]
                        Registry.getInstance(instance).addLastLookupRoute(it.routeKey, instance)

                        if (queryStop != null) {
                            val stops = Registry.getInstance(instance).getAllStops(queryRouteNumber!!, queryBound!!, queryCo!!, queryGMBRegion)
                            val stopIndexed = stops.asSequence().withIndex().filter { it.value.stopId == queryStop }.minByOrNull { (queryStopIndex - it.index).absoluteValue }

                            val intent2 = AppIntent(instance, AppScreen.LIST_STOPS)
                            intent2.putExtra("route", it)
                            intent2.putExtra("scrollToStop", queryStop)
                            if (stopIndexed != null) {
                                intent2.putExtra("stopIndex", stopIndexed.index + 1)
                            }
                            instance.startActivity(intent2)

                            if (queryStopDirectLaunch) {
                                stopIndexed?.let { (i, stopData) ->
                                    val intent3 = AppIntent(instance, AppScreen.ETA)
                                    intent3.putExtra("stopId", stopData.stopId)
                                    intent3.putExtra("co", queryCo)
                                    intent3.putExtra("index", i + 1)
                                    intent3.putExtra("stop", stopData.stop)
                                    intent3.putExtra("route", stopData.route)
                                    instance.startActivity(intent3)
                                }
                            }
                        } else if (filteredResult.size == 1) {
                            val intent2 = AppIntent(instance, AppScreen.LIST_STOPS)
                            intent2.putExtra("route", it)
                            instance.startActivity(intent2)
                        }
                    }
                }
                instance.finishAffinity()
            } else {
                orElse.invoke()
            }
        }
    }

    suspend fun handleAlertRemoteNotification(payload: String, context: AppContext) {
        try {
            val registry = Registry.getInstanceNoUpdateCheck(context)
            while (registry.state.value.isProcessing) {
                delay(10)
            }
            val notification = JsonIgnoreUnknownKeys.decodeFromString<AlertNotification>(payload)
            if (notification.isInterested()) {
                context.sendLocalNotification(notification.id, "alert_channel", notification.title[language], notification.content[language], notification.url[language])
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

}