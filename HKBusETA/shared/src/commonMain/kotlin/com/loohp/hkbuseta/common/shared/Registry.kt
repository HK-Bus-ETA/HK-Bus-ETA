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

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicLong
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.synchronize
import co.touchlab.stately.concurrency.value
import co.touchlab.stately.concurrency.withLock
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppShortcutIcon
import com.loohp.hkbuseta.common.appcontext.ReduceDataOmitted
import com.loohp.hkbuseta.common.appcontext.primaryThemeColor
import com.loohp.hkbuseta.common.branchedlist.MutableBranchedList
import com.loohp.hkbuseta.common.objects.AppAlert
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.DataContainer
import com.loohp.hkbuseta.common.objects.ETADisplayMode
import com.loohp.hkbuseta.common.objects.Fare
import com.loohp.hkbuseta.common.objects.FavouriteRouteGroup
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.FavouriteStop
import com.loohp.hkbuseta.common.objects.GMBRegion
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.objects.QueryTask
import com.loohp.hkbuseta.common.objects.RadiusCenterPosition
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteNotice
import com.loohp.hkbuseta.common.objects.RouteNoticeExternal
import com.loohp.hkbuseta.common.objects.RouteNoticeImportance
import com.loohp.hkbuseta.common.objects.RouteNoticeText
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.RouteSortMode
import com.loohp.hkbuseta.common.objects.RouteSortPreference
import com.loohp.hkbuseta.common.objects.RouteWaypoints
import com.loohp.hkbuseta.common.objects.SpecialTrafficNews
import com.loohp.hkbuseta.common.objects.StationBarrierFreeMapping
import com.loohp.hkbuseta.common.objects.StationInfo
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.StopInfo
import com.loohp.hkbuseta.common.objects.Theme
import com.loohp.hkbuseta.common.objects.TrafficNews
import com.loohp.hkbuseta.common.objects.TrafficSnapshotPoint
import com.loohp.hkbuseta.common.objects.TrainServiceStatus
import com.loohp.hkbuseta.common.objects.TrainServiceStatusMessage
import com.loohp.hkbuseta.common.objects.TrainServiceStatusMessageStatus
import com.loohp.hkbuseta.common.objects.TrainServiceStatusType
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.asStop
import com.loohp.hkbuseta.common.objects.calculateServiceTimeCategory
import com.loohp.hkbuseta.common.objects.defaultOperatorNotices
import com.loohp.hkbuseta.common.objects.endOfLineText
import com.loohp.hkbuseta.common.objects.fetchOperatorNotices
import com.loohp.hkbuseta.common.objects.findPointsWithinDistanceOrdered
import com.loohp.hkbuseta.common.objects.firstCo
import com.loohp.hkbuseta.common.objects.getBody
import com.loohp.hkbuseta.common.objects.getCircularPivotIndex
import com.loohp.hkbuseta.common.objects.getColor
import com.loohp.hkbuseta.common.objects.getDeepLink
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.getOperators
import com.loohp.hkbuseta.common.objects.getTitle
import com.loohp.hkbuseta.common.objects.hkkfStopCode
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.identifyStopCo
import com.loohp.hkbuseta.common.objects.isBus
import com.loohp.hkbuseta.common.objects.isCircular
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.lrtLineStatus
import com.loohp.hkbuseta.common.objects.mtrLineStatus
import com.loohp.hkbuseta.common.objects.prependTo
import com.loohp.hkbuseta.common.objects.resolveSpecialRemark
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.objects.routeComparator
import com.loohp.hkbuseta.common.objects.routeComparatorRouteNumberFirst
import com.loohp.hkbuseta.common.objects.routeGroupKey
import com.loohp.hkbuseta.common.objects.simplified
import com.loohp.hkbuseta.common.objects.splitByClosestPoints
import com.loohp.hkbuseta.common.objects.strictEquals
import com.loohp.hkbuseta.common.objects.uniqueKey
import com.loohp.hkbuseta.common.objects.waypointsId
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.utils.AutoSortedList
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.BoldStyle
import com.loohp.hkbuseta.common.utils.Colored
import com.loohp.hkbuseta.common.utils.EMSP
import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.InlineImage
import com.loohp.hkbuseta.common.utils.JsonIgnoreUnknownKeys
import com.loohp.hkbuseta.common.utils.MutableNonNullStateFlow
import com.loohp.hkbuseta.common.utils.NQSP
import com.loohp.hkbuseta.common.utils.SmallSize
import com.loohp.hkbuseta.common.utils.any
import com.loohp.hkbuseta.common.utils.asAutoSortedList
import com.loohp.hkbuseta.common.utils.asFormattedText
import com.loohp.hkbuseta.common.utils.buildFormattedString
import com.loohp.hkbuseta.common.utils.cache
import com.loohp.hkbuseta.common.utils.clearGlobalCache
import com.loohp.hkbuseta.common.utils.commonElementPercentage
import com.loohp.hkbuseta.common.utils.containsKeyAndNotNull
import com.loohp.hkbuseta.common.utils.createTimetable
import com.loohp.hkbuseta.common.utils.currentBranchStatus
import com.loohp.hkbuseta.common.utils.currentEpochSeconds
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.debugLog
import com.loohp.hkbuseta.common.utils.decodeFromStringReadChannel
import com.loohp.hkbuseta.common.utils.distinctBy
import com.loohp.hkbuseta.common.utils.doRetry
import com.loohp.hkbuseta.common.utils.editDistance
import com.loohp.hkbuseta.common.utils.elementsTo
import com.loohp.hkbuseta.common.utils.epochSeconds
import com.loohp.hkbuseta.common.utils.firstNonNull
import com.loohp.hkbuseta.common.utils.getCircledNumber
import com.loohp.hkbuseta.common.utils.getCompletedOrNull
import com.loohp.hkbuseta.common.utils.getJSONResponse
import com.loohp.hkbuseta.common.utils.getServiceTimeCategory
import com.loohp.hkbuseta.common.utils.getTextResponse
import com.loohp.hkbuseta.common.utils.getTextResponseWithPercentageCallback
import com.loohp.hkbuseta.common.utils.getXMLResponse
import com.loohp.hkbuseta.common.utils.gzipSupported
import com.loohp.hkbuseta.common.utils.hongKongTimeZone
import com.loohp.hkbuseta.common.utils.ifFalse
import com.loohp.hkbuseta.common.utils.indexesOf
import com.loohp.hkbuseta.common.utils.isNotNullAndNotEmpty
import com.loohp.hkbuseta.common.utils.minus
import com.loohp.hkbuseta.common.utils.nextLocalDateTimeAfter
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optInt
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.pad
import com.loohp.hkbuseta.common.utils.parseInstant
import com.loohp.hkbuseta.common.utils.parseLocalDateTime
import com.loohp.hkbuseta.common.utils.performGC
import com.loohp.hkbuseta.common.utils.plus
import com.loohp.hkbuseta.common.utils.postJSONResponse
import com.loohp.hkbuseta.common.utils.remove
import com.loohp.hkbuseta.common.utils.runIfNotNull
import com.loohp.hkbuseta.common.utils.strEq
import com.loohp.hkbuseta.common.utils.sundayZeroDayNumber
import com.loohp.hkbuseta.common.utils.toGroupedMap
import com.loohp.hkbuseta.common.utils.toIntOrElse
import com.loohp.hkbuseta.common.utils.toLocalDateTime
import com.loohp.hkbuseta.common.utils.toLongOrElse
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import com.loohp.hkbuseta.common.utils.wrap
import io.ktor.http.encodeURLQueryComponent
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.concurrent.Volatile
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class Registry {

    companion object {

        const val PREFERENCES_FILE_NAME = "preferences.json"
        const val CHECKSUM_FILE_NAME = "checksum.json"
        const val DATA_FILE_NAME = "data.json"

        fun checksumUrl(): String {
            return "${Shared.DATA_DOMAIN}/checksum.md5"
        }

        fun dataLengthUrl(full: Boolean, gzip: Boolean = gzipSupported()): String {
            return "${Shared.DATA_DOMAIN}/size${if (full) "_full" else ""}${if (gzip) ".gz" else ""}.dat"
        }

        fun dataUrl(full: Boolean, gzip: Boolean = gzipSupported()): String {
            return "${Shared.DATA_DOMAIN}/data${if (full) "_full" else ""}.json${if (gzip) ".gz" else ""}"
        }

        fun lastUpdatedUrl(): String {
            return "${Shared.DATA_DOMAIN}/last_updated.txt"
        }

        private val INSTANCE: AtomicReference<Registry?> = AtomicReference(null)
        private val INSTANCE_LOCK: Lock = Lock()

        fun getInstance(context: AppContext): Registry {
            INSTANCE_LOCK.withLock {
                return INSTANCE.value?: Registry(context, false).apply { INSTANCE.value = this }
            }
        }

        fun getInstanceNoUpdateCheck(context: AppContext): Registry {
            INSTANCE_LOCK.withLock {
                return INSTANCE.value?: Registry(context, true).apply { INSTANCE.value = this }
            }
        }

        fun getInstanceReadOnly(context: AppContext): Registry {
            INSTANCE_LOCK.withLock {
                return INSTANCE.value?: Registry(context, true, readOnlyMode = true).apply { INSTANCE.value = this }
            }
        }

        fun hasInstanceCreated(): Boolean {
            INSTANCE_LOCK.withLock {
                return INSTANCE.value != null
            }
        }

        fun clearInstance() {
            INSTANCE_LOCK.withLock {
                INSTANCE.value = null
            }
            clearGlobalCache()
        }

        suspend fun invalidateCache(context: AppContext) {
            try {
                context.deleteFile(CHECKSUM_FILE_NAME)
            } catch (ignore: Throwable) {
            }
        }

        suspend fun isNewInstall(context: AppContext): Boolean {
            return !context.listFiles().contains(PREFERENCES_FILE_NAME)
        }

        suspend fun getRawPreferences(context: AppContext): JsonObject {
            return if (context.listFiles().contains(PREFERENCES_FILE_NAME)) {
                Json.decodeFromStringReadChannel(context.readTextFile(PREFERENCES_FILE_NAME))
            } else {
                Preferences.createDefault().serialize()
            }
        }

        suspend fun writeRawPreference(raw: String, context: AppContext) {
            context.writeTextFile(PREFERENCES_FILE_NAME) { raw.toStringReadChannel() }
        }
    }

    private val syncPreferences: AtomicReference<Pair<AppContext, Preferences>?> = AtomicReference(null)
    private val syncPreferencesJob: Job = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            syncPreferences.value?.let { (context, preferences) ->
                context.syncPreference(preferences)
                syncPreferences.value = null
            }
            delay(3000)
        }
    }

    private var PREFERENCES: Preferences? = null
    private var DATA: DataContainer? = null

    private val readOnlyMode: Boolean

    val typhoonInfo: MutableNonNullStateFlow<TyphoonInfo> = MutableStateFlow(TyphoonInfo.NO_DATA).wrap()
    private val typhoonInfoDeferred: AtomicReference<Deferred<TyphoonInfo>> = AtomicReference(CompletableDeferred(TyphoonInfo.NO_DATA))
    val state: MutableNonNullStateFlow<State> = MutableStateFlow(State.LOADING).wrap()
    val updatePercentageState: MutableNonNullStateFlow<Float> = MutableStateFlow(0F).wrap()
    private val preferenceWriteLock = Lock()
    private val lastUpdateCheckHolder = AtomicLong(0)
    private val currentChecksumTask = AtomicReference<Job?>(null)
    private val objectCache: MutableMap<String, Any> = ConcurrentMutableMap()

    @Suppress("ConvertSecondaryConstructorToPrimary")
    private constructor(context: AppContext, suppressUpdateCheck: Boolean, readOnlyMode: Boolean = false) {
        this.readOnlyMode = readOnlyMode
        ensureData(context, suppressUpdateCheck)
    }

    val lastUpdateCheck: Long get() = lastUpdateCheckHolder.get()

    private fun savePreferences(context: AppContext, sync: Boolean = true) {
        if (!readOnlyMode) {
            var preferences: Preferences? = null
            preferenceWriteLock.withLock {
                preferences = PREFERENCES!!.apply { lastSaved = currentTimeMillis() }
                CoroutineScope(Dispatchers.IO).launch {
                    context.writeTextFile(PREFERENCES_FILE_NAME) { preferences!!.serialize().toString().toStringReadChannel() }
                }
            }
            if (sync) {
                preferences?.apply { syncPreferences.value = context to this@apply }
            }
        }
    }

    suspend fun syncPreference(context: AppContext, preferences: Preferences, sync: Boolean) {
        while (state.value.isProcessing) {
            delay(100)
        }
        val favouriteStopsUpdated = PREFERENCES!!.favouriteStops != preferences.favouriteStops
        val favouriteRouteStopsUpdated = PREFERENCES!!.favouriteRouteStops != preferences.favouriteRouteStops
        PREFERENCES!!.syncWith(preferences).ifFalse { return }
        if (favouriteStopsUpdated) {
            PREFERENCES!!.favouriteStops.mapNotNull {
                val stop = getStopById(it.stopId)
                if (stop != null) {
                    it.copy(stop = stop)
                } else {
                    val closestStop = it.stop?.location?.let { l ->
                        findNearestStops(l) { i, _ -> i.identifyStopCo().firstCo()?.isTrain != true }
                    }
                    if (closestStop == null || closestStop.distance > 0.3) {
                        null
                    } else {
                        it.copy(stopId = closestStop.stopId, stop = closestStop.stop)
                    }
                }
            }
        }
        if (favouriteRouteStopsUpdated) {
            PREFERENCES!!.favouriteRouteStops.forEachIndexed { idx, item ->
                val (name, list) = item
                PREFERENCES!!.favouriteRouteStops[idx] = FavouriteRouteGroup(name, list.mapNotNull { favouriteRoute ->
                    try {
                        val oldRoute = favouriteRoute.route
                        var stopId = favouriteRoute.stopId
                        val co = favouriteRoute.co
                        val newRoutes = findRoutes(
                            input = oldRoute.routeNumber,
                            exact = true,
                            sorted = false,
                            predicate = { it.bound.containsKey(co) && it.idBound(co) == oldRoute.idBound(co) && it.gmbRegion == oldRoute.gmbRegion }
                        )
                        if (newRoutes.isEmpty()) {
                            return@mapNotNull null
                        }
                        val newRouteData = newRoutes[0]
                        val newRoute = newRouteData.route!!
                        val stopList = getAllStops(newRoute.routeNumber, newRoute.idBound(co), co, newRoute.gmbRegion)
                        var index = stopList.indexesOf { it.stopId == stopId }
                            .minByOrNull { (favouriteRoute.index - it + 1).absoluteValue }
                            ?.let { it + 1 }
                            ?: 0
                        val stopData: StopData
                        if (index < 1) {
                            index = favouriteRoute.index.coerceIn(1, stopList.size)
                            stopData = stopList[index - 1]
                            stopId = stopData.stopId
                        } else {
                            stopData = stopList[index - 1]
                        }
                        val stop = stopList[index - 1].stop
                        val finalStopId = stopId
                        val finalIndex = index
                        return@mapNotNull FavouriteRouteStop(finalStopId, co, finalIndex, stop, stopData.route, favouriteRoute.favouriteStopMode, favouriteRoute.favouriteId)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        return@mapNotNull null
                    }
                }.distinctBy { it.similarityKey })
            }
        }
        savePreferences(context, sync)
        Shared.language = PREFERENCES!!.language
        Shared.etaDisplayMode = PREFERENCES!!.etaDisplayMode
        Shared.viewFavTab = PREFERENCES!!.viewFavTab
        Shared.lrtDirectionMode = PREFERENCES!!.lrtDirectionMode
        Shared.theme = PREFERENCES!!.theme
        Shared.color = PREFERENCES!!.color
        Shared.disableMarquee = PREFERENCES!!.disableMarquee
        Shared.disableBoldDest = PREFERENCES!!.disableBoldDest
        Shared.receiveAlerts = PREFERENCES!!.receiveAlerts
        Shared.historyEnabled = PREFERENCES!!.historyEnabled
        Shared.showRouteMap = PREFERENCES!!.showRouteMap
        Shared.downloadSplash = PREFERENCES!!.downloadSplash
        Shared.alternateStopNamesShowingState.value = PREFERENCES!!.alternateStopName
        Shared.lastNearbyLocation = PREFERENCES!!.lastNearbyLocation
        Shared.disableNavBarQuickActions = PREFERENCES!!.disableNavBarQuickActions
        Shared.updateFavoriteStops {
            if (it.value != PREFERENCES!!.favouriteStops) {
                it.value = PREFERENCES!!.favouriteStops.toImmutableList()
            }
        }
        var etaTileConfigurationChanged = Tiles.getRawEtaTileConfigurations() != PREFERENCES!!.etaTileConfigurations
        Shared.updateFavoriteRouteStops {
            if (it.value != PREFERENCES!!.favouriteRouteStops) {
                it.value = PREFERENCES!!.favouriteRouteStops.toImmutableList()
                etaTileConfigurationChanged = true
            }
        }
        if (etaTileConfigurationChanged) {
            val allIds = PREFERENCES!!.favouriteRouteStops.asSequence()
                .flatMap { it.favouriteRouteStops }
                .map { it.favouriteId }
                .toSet()
            val updated = PREFERENCES!!.etaTileConfigurations.asSequence()
                .map { (k, v) -> k to v.toMutableList().apply { retainAll(allIds) } }
                .filter { (_, v) -> v.isNotEmpty() }
                .associate { it }
            PREFERENCES!!.etaTileConfigurations.apply {
                clear()
                putAll(updated)
            }
            Tiles.updateEtaTileConfigurations {
                it.clear()
                it.putAll(updated)
            }
        }
        if (Shared.routeSortModePreference != PREFERENCES!!.routeSortModePreference) {
            (Shared.routeSortModePreference as MutableMap).apply {
                clear()
                putAll(PREFERENCES!!.routeSortModePreference)
            }
        }
        if (Shared.lastLookupRoutes.value != PREFERENCES!!.lastLookupRoutes) {
            Shared.clearLookupRoute()
            val lastLookupRoutes = PREFERENCES!!.lastLookupRoutes
            val itr = lastLookupRoutes.listIterator(lastLookupRoutes.size)
            while (itr.hasPrevious()) {
                val lastLookupRoute = itr.previous()
                if (DATA?.dataSheet?.routeList?.containsKey(lastLookupRoute.routeKey) != false) {
                    Shared.addLookupRoute(lastLookupRoute)
                } else {
                    itr.remove()
                }
            }
            Shared.lastLookupRoutes.value.firstOrNull()?.let { (routeKey) ->
                CoroutineScope(Dispatchers.IO).launch {
                    findRouteByKey(routeKey, null)?.let { route ->
                        val routeNumber = route.co.firstCo()!!.getDisplayRouteNumber(route.routeNumber)
                        val dest = route.resolvedDest(true)[Shared.language]
                        CoroutineScope(Dispatchers.Main).launch {
                            context.setAppShortcut(
                                id = "Recent",
                                shortLabel = "$routeNumber $dest",
                                longLabel = if (Shared.language == "en") {
                                    "Recent - $routeNumber $dest"
                                } else {
                                    "最近查看 - $routeNumber $dest"
                                },
                                icon = AppShortcutIcon.HISTORY,
                                tint = primaryThemeColor,
                                rank = 11,
                                url = route.getDeepLink(context, null, null)
                            )
                        }
                    }
                }
            }
        }
    }

    fun exportPreference(): JsonObject {
        preferenceWriteLock.withLock {
            return PREFERENCES!!.serialize()
        }
    }

    fun setLanguage(language: String, context: AppContext) {
        Shared.language = language
        PREFERENCES!!.language = language
        savePreferences(context)
        Tiles.requestTileUpdate()
    }

    fun setEtaDisplayMode(etaDisplayMode: ETADisplayMode, context: AppContext) {
        Shared.etaDisplayMode = etaDisplayMode
        PREFERENCES!!.etaDisplayMode = etaDisplayMode
        savePreferences(context)
        Tiles.requestTileUpdate()
    }

    fun setLrtDirectionMode(lrtDirectionMode: Boolean, context: AppContext) {
        Shared.lrtDirectionMode = lrtDirectionMode
        PREFERENCES!!.lrtDirectionMode = lrtDirectionMode
        savePreferences(context)
        Tiles.requestTileUpdate()
    }

    fun setTheme(theme: Theme, context: AppContext) {
        Shared.theme = theme
        PREFERENCES!!.theme = theme
        savePreferences(context)
        Tiles.requestTileUpdate()
    }

    fun setColor(color: Long?, context: AppContext) {
        Shared.color = color
        PREFERENCES!!.color = color
        savePreferences(context)
    }

    fun setViewFavTab(viewFavTab: Int, context: AppContext) {
        Shared.viewFavTab = viewFavTab
        PREFERENCES!!.viewFavTab = viewFavTab
        savePreferences(context)
        Tiles.requestTileUpdate()
    }

    fun setDisableMarquee(disableMarquee: Boolean, context: AppContext) {
        Shared.disableMarquee = disableMarquee
        PREFERENCES!!.disableMarquee = disableMarquee
        savePreferences(context)
    }

    fun setDisableBoldDest(disableBoldDest: Boolean, context: AppContext) {
        Shared.disableBoldDest = disableBoldDest
        PREFERENCES!!.disableBoldDest = disableBoldDest
        savePreferences(context)
    }

    fun setReceiveAlerts(receiveAlerts: Boolean, context: AppContext) {
        Shared.receiveAlerts = receiveAlerts
        PREFERENCES!!.receiveAlerts = receiveAlerts
        savePreferences(context)
    }

    fun setHistoryEnabled(historyEnabled: Boolean, context: AppContext) {
        Shared.historyEnabled = historyEnabled
        PREFERENCES!!.historyEnabled = historyEnabled
        savePreferences(context)
    }

    fun setShowRouteMap(showRouteMap: Boolean, context: AppContext) {
        Shared.showRouteMap = showRouteMap
        PREFERENCES!!.showRouteMap = showRouteMap
        savePreferences(context)
    }

    fun setDownloadSplash(downloadSplash: Boolean, context: AppContext) {
        Shared.downloadSplash = downloadSplash
        PREFERENCES!!.downloadSplash = downloadSplash
        CoroutineScope(Dispatchers.IO).launch {
            if (downloadSplash) {
                Splash.downloadMissingImages(context)
            } else {
                Splash.clearDownloadedImages(context)
            }
        }
        savePreferences(context)
    }

    fun setAlternateStopNames(alternateStopName: Boolean, context: AppContext) {
        Shared.alternateStopNamesShowingState.value = alternateStopName
        PREFERENCES!!.alternateStopName = alternateStopName
        savePreferences(context)
    }

    fun setLastNearbyLocation(lastNearbyLocation: RadiusCenterPosition?, context: AppContext) {
        Shared.lastNearbyLocation = lastNearbyLocation
        PREFERENCES!!.lastNearbyLocation = lastNearbyLocation
        savePreferences(context)
    }

    fun setDisableNavBarQuickActions(disableNavBarQuickActions: Boolean, context: AppContext) {
        Shared.disableNavBarQuickActions = disableNavBarQuickActions
        PREFERENCES!!.disableNavBarQuickActions = disableNavBarQuickActions
        savePreferences(context)
    }

    fun setFavouriteStops(favouriteStops: List<FavouriteStop>, context: AppContext) {
        val distinct = favouriteStops.asSequence().distinct().toList().toImmutableList()
        Shared.updateFavoriteStops { it.value = distinct }
        PREFERENCES!!.favouriteStops.apply {
            clear()
            addAll(distinct)
        }
        savePreferences(context)
    }

    fun setFavouriteRouteGroups(favouriteRouteStops: List<FavouriteRouteGroup>, context: AppContext) {
        val distinct = favouriteRouteStops.map { (name, routes) -> FavouriteRouteGroup(name, routes.asSequence().distinct().toList()) }.toImmutableList()
        Shared.updateFavoriteRouteStops { it.value = distinct }
        PREFERENCES!!.favouriteRouteStops.apply {
            clear()
            addAll(distinct)
        }
        val allIds = distinct.asSequence()
            .flatMap { it.favouriteRouteStops }
            .map { it.favouriteId }
            .toSet()
        val updated = PREFERENCES!!.etaTileConfigurations.asSequence()
            .map { (k, v) -> k to v.toMutableList().apply { retainAll(allIds) } }
            .filter { (_, v) -> v.isNotEmpty() }
            .associate { it }
        PREFERENCES!!.etaTileConfigurations.apply {
            clear()
            putAll(updated)
        }
        Tiles.updateEtaTileConfigurations {
            it.clear()
            it.putAll(updated)
        }
        savePreferences(context)
        Tiles.requestTileUpdate()
    }

    fun clearEtaTileConfiguration(tileId: Int, context: AppContext) {
        Tiles.updateEtaTileConfigurations { it.remove(tileId) }
        PREFERENCES!!.etaTileConfigurations.remove(tileId)
        savePreferences(context)
    }

    fun setEtaTileConfiguration(tileId: Int, favouriteIndexes: List<Int>, context: AppContext) {
        Tiles.updateEtaTileConfigurations { it[tileId] = favouriteIndexes }
        PREFERENCES!!.etaTileConfigurations[tileId] = favouriteIndexes
        savePreferences(context)
        Tiles.requestTileUpdate()
    }

    fun addLastLookupRoute(routeKey: String, context: AppContext) {
        if (!Shared.historyEnabled) return
        Shared.addLookupRoute(routeKey)
        PREFERENCES!!.lastLookupRoutes.clear()
        PREFERENCES!!.lastLookupRoutes.addAll(Shared.lastLookupRoutes.value)
        savePreferences(context)
        setRecentAppShortcut(routeKey, context)
    }

    fun removeLastLookupRoutes(routeKey: String, context: AppContext) {
        Shared.removeLookupRoute(routeKey)
        PREFERENCES!!.lastLookupRoutes.clear()
        PREFERENCES!!.lastLookupRoutes.addAll(Shared.lastLookupRoutes.value)
        savePreferences(context)
        Shared.lastLookupRoutes.value.firstOrNull().apply {
            if (this == null) {
                CoroutineScope(Dispatchers.Main).launch { context.removeAppShortcut(id = "Recent") }
            } else {
                setRecentAppShortcut(this.routeKey, context)
            }
        }
    }

    private fun setRecentAppShortcut(routeKey: String, context: AppContext) {
        CoroutineScope(Dispatchers.IO).launch {
            findRouteByKey(routeKey, null)?.let { route ->
                val routeNumber = route.co.firstCo()!!.getDisplayRouteNumber(route.routeNumber)
                val dest = route.resolvedDest(true)[Shared.language]
                CoroutineScope(Dispatchers.Main).launch {
                    context.setAppShortcut(
                        id = "Recent",
                        shortLabel = "$routeNumber $dest",
                        longLabel = if (Shared.language == "en") {
                            "Recent - $routeNumber $dest"
                        } else {
                            "最近查看 - $routeNumber $dest"
                        },
                        icon = AppShortcutIcon.HISTORY,
                        tint = primaryThemeColor,
                        rank = 11,
                        url = route.getDeepLink(context, null, null)
                    )
                }
            }
        }
    }

    fun clearLastLookupRoutes(context: AppContext) {
        Shared.clearLookupRoute()
        PREFERENCES!!.lastLookupRoutes.clear()
        savePreferences(context)
        CoroutineScope(Dispatchers.Main).launch { context.removeAppShortcut(id = "Recent") }
    }

    fun setRouteSortModePreference(context: AppContext, listType: RouteListType, sortMode: RouteSortMode) {
        val filterTimetableActive = (Shared.routeSortModePreference as MutableMap)[listType]?.filterTimetableActive == true
        setRouteSortModePreference(context, listType, RouteSortPreference(sortMode, filterTimetableActive))
    }

    fun setRouteSortModePreference(context: AppContext, listType: RouteListType, sortPreference: RouteSortPreference) {
        (Shared.routeSortModePreference as MutableMap)[listType] = sortPreference
        PREFERENCES!!.routeSortModePreference.clear()
        PREFERENCES!!.routeSortModePreference.putAll(Shared.routeSortModePreference)
        savePreferences(context)
    }

    fun cancelCurrentChecksumTask() {
        currentChecksumTask.get()?.cancel()
    }

    private fun ensureData(context: AppContext, suppressUpdateCheck: Boolean) {
        if (state.value == State.READY) {
            return
        }
        if (PREFERENCES != null && DATA != null) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            loadPreferences(context)
            checkUpdate(context, suppressUpdateCheck)
        }
    }

    fun getLastUpdatedTime(): Long? {
        return DATA?.updatedTime
    }

    fun getOptDataContainer(): DataContainer? {
        return DATA
    }

    fun getHolidays(): List<LocalDate> {
        return DATA!!.dataSheet.holidays
    }

    fun getRouteList(): Map<String, Route> {
        return DATA!!.dataSheet.routeList
    }

    fun getStopList(): Map<String, Stop> {
        return DATA!!.dataSheet.stopList
    }

    fun getStopMap(): Map<String, List<Pair<Operator, String>>> {
        return DATA!!.dataSheet.stopMap
    }

    fun getServiceDayMap(): Map<String, List<String>?> {
        return DATA!!.dataSheet.serviceDayMap
    }

    fun getMtrBusStopAlias(): Map<String, List<String>?> {
        return DATA!!.mtrBusStopAlias
    }

    fun getRouteRemarks(): Map<Operator, Map<String, BilingualText>> {
        return DATA!!.routeRemarks
    }

    @ReduceDataOmitted
    fun getMTRData(): Map<String, StationInfo> {
        return DATA!!.mtrData.orEmpty()
    }

    @ReduceDataOmitted
    fun getLRTData(): Map<String, StationInfo> {
        return DATA!!.lrtData.orEmpty()
    }

    @ReduceDataOmitted
    fun getMTRBarrierFreeMapping(): StationBarrierFreeMapping {
        return DATA!!.mtrBarrierFreeMapping?: StationBarrierFreeMapping.EMPTY_MAPPING
    }

    suspend fun loadPreferences(context: AppContext) {
        val files = context.listFiles()
        if (files.contains(PREFERENCES_FILE_NAME)) {
            try {
                PREFERENCES = Preferences.deserialize(Json.decodeFromStringReadChannel<JsonObject>(context.readTextFile(PREFERENCES_FILE_NAME)))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        if (PREFERENCES == null) {
            PREFERENCES = Preferences.createDefault()
            savePreferences(context, false)
        }
        Shared.language = PREFERENCES!!.language
        Shared.etaDisplayMode = PREFERENCES!!.etaDisplayMode
        Shared.viewFavTab = PREFERENCES!!.viewFavTab
        Shared.lrtDirectionMode = PREFERENCES!!.lrtDirectionMode
        Shared.theme = PREFERENCES!!.theme
        Shared.color = PREFERENCES!!.color
        Shared.downloadSplash = PREFERENCES!!.downloadSplash
        Shared.alternateStopNamesShowingState.value = PREFERENCES!!.alternateStopName
        Shared.lastNearbyLocation = PREFERENCES!!.lastNearbyLocation
        Shared.disableMarquee = PREFERENCES!!.disableMarquee
        Shared.disableBoldDest = PREFERENCES!!.disableBoldDest
        Shared.receiveAlerts = PREFERENCES!!.receiveAlerts
        Shared.historyEnabled = PREFERENCES!!.historyEnabled
        Shared.showRouteMap = PREFERENCES!!.showRouteMap
        Shared.disableNavBarQuickActions = PREFERENCES!!.disableNavBarQuickActions
        Shared.updateFavoriteStops {
            it.value = PREFERENCES!!.favouriteStops.toImmutableList()
        }
        Shared.updateFavoriteRouteStops {
            it.value = PREFERENCES!!.favouriteRouteStops.toImmutableList()
        }
        Tiles.updateEtaTileConfigurations {
            it.clear()
            it.putAll(PREFERENCES!!.etaTileConfigurations)
        }
        Shared.clearLookupRoute()
        PREFERENCES!!.lastLookupRoutes.reversed().forEach { Shared.addLookupRoute(it) }
        Shared.routeSortModePreference as MutableMap
        Shared.routeSortModePreference.clear()
        Shared.routeSortModePreference.putAll(PREFERENCES!!.routeSortModePreference)
    }

    fun checkUpdate(context: AppContext, suppressUpdateCheck: Boolean) {
        state.value = State.LOADING
        if (!suppressUpdateCheck) {
            lastUpdateCheckHolder.set(currentTimeMillis())
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val files = context.listFiles()
                val hasConnection = context.hasConnection()
                val updateChecked = AtomicBoolean(false)
                val checksumFetcher: suspend (Boolean) -> String? = { forced ->
                    val future = async { withTimeout(10000) {
                        val version = context.versionCode
                        "${getTextResponse(checksumUrl())?.string()}_$version"
                    } }
                    currentChecksumTask.set(future)
                    if (!forced && files.contains(CHECKSUM_FILE_NAME) && files.contains(DATA_FILE_NAME)) {
                        state.value = State.UPDATE_CHECKING
                    }
                    try {
                        val result = future.await()
                        updateChecked.value = true
                        result
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    } finally {
                        if (state.value == State.UPDATE_CHECKING) {
                            state.value = State.LOADING
                        }
                    }
                }
                var cached = false
                var checksum = if (!suppressUpdateCheck && hasConnection) checksumFetcher.invoke(false) else null
                if (files.contains(CHECKSUM_FILE_NAME) && files.contains(DATA_FILE_NAME)) {
                    if (checksum == null) {
                        cached = true
                    } else {
                        val localChecksum = context.readTextFile(CHECKSUM_FILE_NAME).string()
                        if (localChecksum == checksum && PREFERENCES!!.referenceChecksum == checksum) {
                            cached = true
                        }
                    }
                }
                if (cached) {
                    if (DATA == null) {
                        try {
                            DATA = JsonIgnoreUnknownKeys.decodeFromStringReadChannel(context.readTextFile(DATA_FILE_NAME))
                            Tiles.requestTileUpdate()
                            Shared.setKmbSubsidiary(DATA!!.kmbSubsidiary)
                            state.value = State.READY
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    } else {
                        state.value = State.READY
                    }
                }
                if (state.value != State.READY) {
                    if (!hasConnection) {
                        state.value = State.ERROR
                        try {
                            context.deleteFile(CHECKSUM_FILE_NAME)
                        } catch (ignore: Throwable) {
                        }
                    } else {
                        state.value = State.UPDATING
                        updatePercentageState.value = 0f
                        Shared.pinnedItems.value = emptyList()
                        val percentageOffset = if (Shared.favoriteRouteStops.value.all { it.favouriteRouteStops.isEmpty() }) 0.15f else 0f
                        context.withHighBandwidthNetwork {
                            if (!updateChecked.value) {
                                checksum = checksumFetcher.invoke(true)
                            }
                            val gzip = gzipSupported()
                            val length = getTextResponse(dataLengthUrl(!context.formFactor.reduceData, gzip))?.string().toLongOrElse(-1)
                            val textResponse = getTextResponseWithPercentageCallback(dataUrl(!context.formFactor.reduceData, gzip), length, gzip) { p -> updatePercentageState.value = p * 0.85f + percentageOffset }?: throw RuntimeException("Error downloading bus data")
                            DATA = JsonIgnoreUnknownKeys.decodeFromStringReadChannel(textResponse)
                            Shared.setKmbSubsidiary(DATA!!.kmbSubsidiary)
                            getTextResponse(lastUpdatedUrl())?.string()?.toLong()?.apply { Shared.scheduleBackgroundUpdateService(context, this) }
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            if (!readOnlyMode) {
                                context.writeTextFile(DATA_FILE_NAME, Json, DataContainer.serializer()) { DATA!! }
                                context.writeTextFile(CHECKSUM_FILE_NAME) { (checksum?: "").toStringReadChannel() }
                            }
                            PREFERENCES!!.referenceChecksum = checksum?: ""
                            savePreferences(context)
                        }
                        updatePercentageState.value = 0.85f + percentageOffset
                        var localUpdatePercentage = updatePercentageState.value
                        val percentagePerFav = 0.15f / Shared.favoriteRouteStops.value.flatMap { it.favouriteRouteStops }.count()
                        val updateFavouriteStops = Shared.favoriteStops.value.mapNotNull {
                            val stop = getStopById(it.stopId)
                            if (stop != null) {
                                it.copy(stop = stop)
                            } else {
                                val closestStop = it.stop?.location?.let { l ->
                                    findNearestStops(l) { i, _ -> i.identifyStopCo().firstCo()?.isTrain != true }
                                }
                                if (closestStop == null || closestStop.distance > 0.3) {
                                    null
                                } else {
                                    it.copy(stopId = closestStop.stopId, stop = closestStop.stop)
                                }
                            }
                        }
                        val updateFavouriteRoutes = Shared.favoriteRouteStops.value.map { (name, list) ->
                            FavouriteRouteGroup(name, list.mapNotNull { favouriteRoute ->
                                try {
                                    val oldRoute = favouriteRoute.route
                                    var stopId = favouriteRoute.stopId
                                    val co = favouriteRoute.co
                                    val newRoutes = findRoutes(
                                        input = oldRoute.routeNumber,
                                        exact = true,
                                        sorted = false,
                                        predicate = { it.bound.containsKey(co) && it.idBound(co) == oldRoute.idBound(co) && it.gmbRegion == oldRoute.gmbRegion }
                                    )
                                    when {
                                        newRoutes.isEmpty() -> null
                                        newRoutes.any { it.route strictEquals favouriteRoute.route } -> favouriteRoute
                                        else -> {
                                            val keyOrder = DATA!!.dataSheet.standardSortedRouteKeys.await()
                                            val newRouteData = newRoutes.minBy { keyOrder.indexOf(it.routeKey) }
                                            val newRoute = newRouteData.route!!
                                            val stopList = getAllStops(newRoute.routeNumber, newRoute.idBound(co), co, newRoute.gmbRegion)
                                            var index = stopList.indexesOf { it.stopId == stopId }
                                                .minByOrNull { (favouriteRoute.index - it + 1).absoluteValue }
                                                ?.let { it + 1 }
                                                ?: 0
                                            val stopData: StopData
                                            if (index < 1) {
                                                index = favouriteRoute.index.coerceIn(1, stopList.size)
                                                stopData = stopList[index - 1]
                                                stopId = stopData.stopId
                                            } else {
                                                stopData = stopList[index - 1]
                                            }
                                            val stop = stopList[index - 1].stop
                                            val finalStopId = stopId
                                            val finalIndex = index
                                            FavouriteRouteStop(finalStopId, co, finalIndex, stop, stopData.route, favouriteRoute.favouriteStopMode, favouriteRoute.favouriteId)
                                        }
                                    }
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                    return@mapNotNull null
                                } finally {
                                    localUpdatePercentage += percentagePerFav
                                    updatePercentageState.value = localUpdatePercentage
                                }
                            }.distinctBy { it.similarityKey })
                        }
                        Shared.clearLookupRoute()
                        val lastLookupRoutes = PREFERENCES!!.lastLookupRoutes
                        val itr = lastLookupRoutes.listIterator(lastLookupRoutes.size)
                        while (itr.hasPrevious()) {
                            val lastLookupRoute = itr.previous()
                            if (DATA!!.dataSheet.routeList.containsKey(lastLookupRoute.routeKey)) {
                                Shared.addLookupRoute(lastLookupRoute)
                            } else {
                                itr.remove()
                            }
                        }
                        setFavouriteStops(updateFavouriteStops, context)
                        setFavouriteRouteGroups(updateFavouriteRoutes, context)
                        Splash.reloadEntries(DATA!!.splashEntries, context)
                        updatePercentageState.value = 1f
                        Tiles.requestTileUpdate()
                        state.value = State.READY
                    }
                }
                updatePercentageState.value = 1f
            } catch (e: Exception) {
                e.printStackTrace()
                state.value = State.ERROR
            } finally {
                performGC()
            }
            if (state.value != State.READY) {
                state.value = State.ERROR
            }
        }
    }

    fun getRouteKey(route: Route?): String? {
        return DATA!!.dataSheet.routeList.entries.asSequence()
            .filter{ (_, value) -> value == route }
            .firstOrNull()?.let { (key) -> key }
    }

    fun findRouteByKey(lookupKey: String, routeNumber: String?): Route? {
        var inputKey = lookupKey
        val exact = DATA!!.dataSheet.routeList[inputKey]
        if (exact != null) return exact
        inputKey = inputKey.lowercase()
        var nearestRoute: Route? = null
        var distance = Int.MAX_VALUE
        for ((key, route) in DATA!!.dataSheet.routeList.entries) {
            if (routeNumber == null || route.routeNumber.equals(routeNumber, ignoreCase = true)) {
                val editDistance = key.lowercase().editDistance(inputKey)
                if (editDistance < distance) {
                    nearestRoute = route
                    distance = editDistance
                }
            }
        }
        return nearestRoute
    }

    fun getStopById(stopId: String?): Stop? {
        return DATA!!.dataSheet.stopList[stopId]
    }

    fun getPossibleNextChar(input: String): PossibleNextCharResult {
        val result = mutableSetOf<Char>()
        var exactMatch = false
        for (routeNumber in DATA!!.dataSheet.routeNumberList) {
            if (routeNumber.startsWith(input)) {
                if (routeNumber.length > input.length) {
                    result.add(routeNumber[input.length])
                } else {
                    exactMatch = true
                }
            }
        }
        return PossibleNextCharResult(result, exactMatch)
    }

    @Immutable
    data class PossibleNextCharResult(val characters: Set<Char>, val hasExactMatch: Boolean)

    fun isPublicHoliday(date: LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SUNDAY || DATA!!.dataSheet.holidays.contains(date)
    }

    fun randomRoute(): Route {
        return DATA!!.dataSheet.routeList.let { it.values.elementAt(Random.nextInt(0, it.size)) }
    }

    fun randomStop(): Stop {
        return DATA!!.dataSheet.stopList.let { it.values.elementAt(Random.nextInt(0, it.size)) }
    }

    fun <T> findNearbyStopsBulk(items: Collection<T>, center: (T) -> Coordinates, radius: Double): List<NearbyStopSearchBulkResult<T>> {
        val results = mutableListOf<NearbyStopSearchBulkResult<T>>()
        for ((stopId, stop) in DATA!!.dataSheet.stopList) {
            var shortestDistance = Double.MAX_VALUE
            var shortestDistanceStopId: String? = null
            var shortestDistanceStop: Stop? = null
            var shortestDistanceItem: T? = null
            for (item in items) {
                val distance = stop.location.distance(center.invoke(item))
                if (distance < shortestDistance) {
                    shortestDistance = distance
                    shortestDistanceStopId = stopId
                    shortestDistanceStop = stop
                    shortestDistanceItem = item
                }
            }
            if (shortestDistance <= radius && shortestDistanceStop != null && shortestDistanceStopId != null && shortestDistanceItem != null) {
                results.add(NearbyStopSearchBulkResult(shortestDistanceStopId, shortestDistanceStop, shortestDistance, shortestDistanceItem))
            }
        }
        return results.sortedBy { it.distance }
    }

    fun findNearbyStops(center: Coordinates, radius: Double): List<NearbyStopSearchResult> {
        return DATA!!.dataSheet.stopList.asSequence()
            .map { (i, s) -> NearbyStopSearchResult(i, s, s.location.distance(center)) }
            .filter { it.distance <= radius }
            .sortedBy { it.distance }
            .toList()
    }

    fun findNearestStops(center: Coordinates, predicate: ((String, Stop) -> Boolean)? = null): NearbyStopSearchResult? {
        return DATA!!.dataSheet.stopList.asSequence()
            .runIfNotNull(predicate) { filter { (i, s) -> it.invoke(i, s) } }
            .map { (i, s) -> NearbyStopSearchResult(i, s, s.location.distance(center)) }
            .minByOrNull { it.distance }
    }

    fun findJointAlternateStop(stopId: String, routeNumber: String): NearbyStopSearchResult {
        return findJointAlternateStops(listOf(element = stopId), routeNumber).first()
    }

    fun findJointAlternateStops(stopIds: List<String>, routeNumber: String): List<NearbyStopSearchResult> {
        if (stopIds.isEmpty()) return emptyList()
        val routeListSequence = DATA!!.dataSheet.routeKeysByRouteNumber.getCompletedOrNull()?.get(routeNumber)?.asSequence()
            ?.map { DATA!!.dataSheet.routeList[it]!! }
            ?: DATA!!.dataSheet.routeList.values.asSequence()
        val routeSpecificStopMap = routeListSequence
            .filter { it.isKmbCtbJoint && it.routeNumber == routeNumber }
            .map { it.stops[Operator.KMB] elementsTo it.stops[Operator.CTB] }
            .flatten()
            .distinct()
            .groupBy({ it.first }, { it.second })
        return stopIds.map { stopId ->
            val id = firstNonNull(
                { routeSpecificStopMap[stopId]?.firstOrNull() },
                { DATA!!.dataSheet.stopMap[stopId]?.asSequence()?.sortedBy { !routeSpecificStopMap.any { (_, v) -> v.contains(it.second) } }?.firstOrNull { it.first == Operator.CTB }?.second },
                { findNearbyStops(DATA!!.dataSheet.stopList[stopId]!!.location, 0.1).firstOrNull { it.stopId.identifyStopCo().contains(Operator.CTB) }?.stopId },
                orElse = { stopId }
            )
            NearbyStopSearchResult(id, DATA!!.dataSheet.stopList[id]!!, 0.0)
        }
    }

    fun findEquivalentStops(stopId: String, radius: Double = 0.1): List<NearbyStopSearchResult> {
        val ids = DATA!!.dataSheet.stopMap[stopId]?.asSequence()?.map { (_, s) -> s }?: findNearbyStops(DATA!!.dataSheet.stopList[stopId]!!.location, radius).asSequence().map { it.stopId }
        return ids.map { NearbyStopSearchResult(it, DATA!!.dataSheet.stopList[it]!!, 0.0) }.toList()
    }

    data class NearbyStopSearchResult(val stopId: String, val stop: Stop, val distance: Double)
    data class NearbyStopSearchBulkResult<T>(val stopId: String, val stop: Stop, val distance: Double, val item: T)

    fun findRoutes(input: String, exact: Boolean): List<RouteSearchResultEntry> {
        return findRoutes(input, exact, true, { true }, { _, _, _ -> true })
    }

    fun findRoutes(input: String, exact: Boolean, predicate: (Route) -> Boolean): List<RouteSearchResultEntry> {
        return findRoutes(input, exact, true, predicate) { _, _, _ -> true }
    }

    fun findRoutes(input: String, exact: Boolean, coPredicate: (String, Route, Operator) -> Boolean): List<RouteSearchResultEntry> {
        return findRoutes(input, exact, true, { true }, coPredicate)
    }

    private fun findRoutes(input: String, exact: Boolean, sorted: Boolean = true, predicate: (Route) -> Boolean = { true }, coPredicate: (String, Route, Operator) -> Boolean = { _, _, _ -> true }): List<RouteSearchResultEntry> {
        val (routeKeys, comparator) = DATA!!.dataSheet.let { when {
            !sorted -> it.routeList.keys to null
            !exact || input.isEmpty() -> it.routeNumberFirstSortedRouteKeys.getCompletedOrNull() to routeComparatorRouteNumberFirst
            else -> it.standardSortedRouteKeys.getCompletedOrNull() to routeComparator
        } }
        val routeMatcher: (String) -> Boolean = if (exact) ({ it == input }) else ({ it.startsWith(input) })
        val matchingRoutes: MutableMap<String, RouteSearchResultEntry> = linkedMapOf()
        for (key in routeKeys?: DATA!!.dataSheet.routeList.keys) {
            val data = DATA!!.dataSheet.routeList[key]!!
            if (data.isCtbIsCircular) continue
            if (routeMatcher.invoke(data.routeNumber) && predicate.invoke(data)) {
                val bound = data.bound
                val co = bound.keys.firstCo()?: continue
                if (!coPredicate.invoke(key, data, co)) continue
                val routeGroupKey = data.routeGroupKey
                if (matchingRoutes.containsKey(routeGroupKey)) {
                    try {
                        val existingMatchingRoute = matchingRoutes[routeGroupKey]
                        val type = data.serviceType.toInt()
                        val matchingType = existingMatchingRoute!!.route!!.serviceType.toInt()
                        if (type < matchingType) {
                            existingMatchingRoute.routeKey = key
                            existingMatchingRoute.route = data
                            existingMatchingRoute.co = co
                        } else if (type == matchingType) {
                            val gtfs = data.gtfsId.toIntOrElse(Int.MAX_VALUE)
                            val matchingGtfs = existingMatchingRoute.route!!.gtfsId.toIntOrElse(Int.MAX_VALUE)
                            if (gtfs < matchingGtfs) {
                                existingMatchingRoute.routeKey = key
                                existingMatchingRoute.route = data
                                existingMatchingRoute.co = co
                            }
                        }
                    } catch (ignore: NumberFormatException) {
                    }
                } else {
                    matchingRoutes[routeGroupKey] = RouteSearchResultEntry(key, data, co)
                }
            }
        }
        return if (routeKeys != null || comparator == null) {
            matchingRoutes.values.toList()
        } else {
            matchingRoutes.values.sortedWith(compareBy(comparator) { it.route!! })
        }
    }

    suspend fun getNearbyRoutes(origin: Coordinates, excludedRoutes: Map<Operator, Set<String>>, isInterchangeSearch: Boolean): NearbyRoutesResult {
        return getNearbyRoutes(origin, 0.3, excludedRoutes, isInterchangeSearch)
    }

    suspend fun getNearbyRoutes(origin: Coordinates, radius: Double, excludedRoutes: Map<Operator, Set<String>>, isInterchangeSearch: Boolean): NearbyRoutesResult {
        val nearbyStops: MutableList<StopInfo> = mutableListOf()
        var closestStop: Stop? = null
        var closestStopId: String? = null
        var closestDistance = Double.MAX_VALUE
        for ((stopId, entry) in DATA!!.dataSheet.stopList) {
            val location = entry.location
            val distance = origin.distance(location)
            if (distance < closestDistance) {
                closestStop = entry
                closestStopId = stopId
                closestDistance = distance
            }
            if (distance <= radius) {
                for (co in stopId.identifyStopCo()) {
                    nearbyStops.add(StopInfo(stopId, entry, distance, co))
                }
            }
        }
        val serviceMap = DATA!!.dataSheet.serviceDayMap
        nearbyStops.sortBy { it.distance }
        val now = currentLocalDateTime()
        val nearbyRoutes: MutableMap<String, Pair<Array<RouteSearchResultEntry?>, MutableList<Route>?>> = mutableMapOf()
        val allStopsCache: MutableMap<String, List<StopData>> = mutableMapOf()
        for (nearbyStopOriginal in nearbyStops) {
            val stopId = nearbyStopOriginal.stopId
            for ((key, branchStopIndex) in DATA!!.dataSheet.routeKeysByStopId.await()[stopId].orEmpty()) {
                val data = DATA!!.dataSheet.routeList[key]!!
                val co = if (data.isKmbCtbJoint) {
                    if (nearbyStopOriginal.co === Operator.CTB) continue
                    Operator.KMB
                } else {
                    nearbyStopOriginal.co
                }
                if (!data.co.contains(nearbyStopOriginal.co)) continue
                if (data.co.any { excludedRoutes[it]?.contains(data.routeNumber) == true }) continue
                if (data.isCtbIsCircular) continue
                val routeGroupKey = data.routeGroupKey(co)
                val allStops = allStopsCache.getOrPut(routeGroupKey) { getAllStops(data.routeNumber, data.idBound(co), co, data.gmbRegion) }
                val stopIndex = allStops.indexesOf { it.stopId == stopId }.minByOrNull { (it - branchStopIndex).absoluteValue }?: -1
                if (stopIndex < 0) continue
                val nearbyStop = nearbyStopOriginal.copy(stopIndex = stopIndex + 1)
                if (!nearbyRoutes.containsKey(routeGroupKey)) {
                    nearbyRoutes[routeGroupKey] = arrayOfNulls<RouteSearchResultEntry?>(2) to mutableListOf()
                }
                val (existingNearbyRoutes, routeSet) = nearbyRoutes[routeGroupKey]!!
                routeSet!!.apply {
                    if (!contains(data)) {
                        add(data)
                        if (size > 1) {
                            val status = currentBranchStatus(now, serviceMap, getHolidays()) { null }
                            sortWith(compareByDescending<Route> { status[it]?.activeness?: 0 }.thenBy { it.serviceType.toIntOrElse(Int.MAX_VALUE) })
                        }
                    }
                }
                val routeSetActive = routeSet.first()
                val middleIndex = routeSetActive.getCircularPivotIndex(allStops)
                if (routeSetActive == data) {
                    if (!routeSetActive.isCircular || stopIndex + 1 < middleIndex) {
                        val existingNearbyRoute = existingNearbyRoutes[0]
                        if (existingNearbyRoute == null) {
                            existingNearbyRoutes[0] = RouteSearchResultEntry(key, data, co, nearbyStop, origin, isInterchangeSearch)
                        } else {
                            val match = (existingNearbyRoute.stopInfo!!.distance > nearbyStop.distance || existingNearbyRoute.route != routeSetActive)
                            if (match) {
                                existingNearbyRoute.routeKey = key
                                existingNearbyRoute.stopInfo = nearbyStop
                                existingNearbyRoute.route = data
                                existingNearbyRoute.co = co
                                existingNearbyRoute.origin = origin
                                existingNearbyRoute.isInterchangeSearch = isInterchangeSearch
                            }
                        }
                        if (!routeSetActive.isCircular) {
                            existingNearbyRoutes[1] = null
                        }
                    } else {
                        val existingNearbyRoute = existingNearbyRoutes[1]
                        if (existingNearbyRoute == null) {
                            existingNearbyRoutes[1] = RouteSearchResultEntry(key, data, co, nearbyStop, origin, isInterchangeSearch)
                        } else {
                            val match = (existingNearbyRoute.stopInfo!!.distance > nearbyStop.distance || existingNearbyRoute.route != routeSetActive)
                            if (match) {
                                existingNearbyRoute.routeKey = key
                                existingNearbyRoute.stopInfo = nearbyStop
                                existingNearbyRoute.route = data
                                existingNearbyRoute.co = co
                                existingNearbyRoute.origin = origin
                                existingNearbyRoute.isInterchangeSearch = isInterchangeSearch
                            }
                        }
                    }
                }
            }
        }
        if (nearbyRoutes.isEmpty()) {
            return NearbyRoutesResult(emptyList(), closestStop!!, closestStopId!!, closestDistance, origin)
        }
        val hongKongTime = currentLocalDateTime()
        val hour = hongKongTime.hour
        val isNight = hour in 1..4
        val weekday = hongKongTime.dayOfWeek
        val date = hongKongTime.date
        val isHoliday = weekday == DayOfWeek.SATURDAY || isPublicHoliday(date)
        val nightRouteByTimetable = buildMap {
            for ((ks, v) in nearbyRoutes.values) {
                for (k in ks) {
                    if (k != null) {
                        val routeNumber = k.route!!.routeNumber
                        getOrPut(k.routeKey) {
                            calculateServiceTimeCategory(routeNumber, k.co) { v!!.createTimetable(serviceMap, null) { null }.getServiceTimeCategory() }.night
                        }
                    }
                }
            }
        }
        return NearbyRoutesResult(nearbyRoutes.values.asSequence()
            .flatMap { it.first.asSequence().filterNotNull() }
            .sortedWith(compareBy<RouteSearchResultEntry> { a ->
                val route = a.route!!
                val routeNumber = route.routeNumber
                val bound = route.bound
                val pa = routeNumber[0].toString()
                val sa = routeNumber[routeNumber.length - 1].toString()
                var na = routeNumber.remove("[^0-9]".toRegex()).toIntOrElse()
                if (bound.containsKey(Operator.GMB)) {
                    na += 1000
                } else if (bound.containsKey(Operator.MTR)) {
                    na += if (isInterchangeSearch) -2000 else 2000
                }
                if (nightRouteByTimetable[a.routeKey] == true) {
                    na -= (if (isNight) 1 else -1) * 10000
                }
                if (sa == "S" && routeNumber != "89S" && routeNumber != "796S") {
                    na += 3000
                }
                if (!isHoliday && (pa == "R" || sa == "R")) {
                    na += 100000
                }
                na
            }
            .thenBy { a -> a.route!!.routeNumber }
            .thenBy { a -> a.route!!.serviceType.toIntOrElse() }
            .thenBy { a -> a.co }
            .thenBy { a ->
                val route = a.route!!
                val bound = route.bound
                if (bound.containsKey(Operator.MTR)) {
                    return@thenBy -Shared.getMtrLineSortingIndex(route.routeNumber)
                }
                -10
            }
            .thenByDescending { a -> a.route!!.bound[a.co] })
            .distinctBy { it.uniqueKey }
            .toList(), closestStop!!, closestStopId!!, closestDistance, origin)
    }

    @Immutable
    data class NearbyRoutesResult(
        val result: List<RouteSearchResultEntry>,
        val closestStop: Stop,
        val closestStopId: String,
        val closestDistance: Double,
        val origin: Coordinates
    )

    @Immutable
    data class AllBranchRoutesSearchParameters(
        val routeNumber: String,
        val bound: String,
        val co: Operator,
        val gmbRegion: GMBRegion?
    )

    fun getAllBranchRoutesBulk(parameters: List<AllBranchRoutesSearchParameters>, includeFakeRoutes: Boolean = false): List<List<Route>> {
        return try {
            val lists: Array<MutableList<Route>> = Array(parameters.size) { mutableListOf() }
            for (route in DATA!!.dataSheet.routeList.values) {
                if (includeFakeRoutes || !route.fakeRoute) {
                    for (i in parameters.indices) {
                        val (routeNumber, bound, co, gmbRegion) = parameters[i]
                        if (routeNumber == route.routeNumber && route.co.contains(co)) {
                            val match = when {
                                co === Operator.NLB -> bound == route.nlbId
                                bound.length > 1 && !co.isTrain -> !(route.isCtbIsCircular && route.freq == null) && (co !== Operator.GMB || gmbRegion == route.gmbRegion)
                                else -> (bound == route.bound[co] || route.isCircular) && (co !== Operator.GMB || gmbRegion == route.gmbRegion)
                            }
                            if (match) {
                                lists[i].add(route)
                            }
                        }
                    }
                }
            }
            buildList {
                for (i in parameters.indices) {
                    val co = parameters[i].co
                    val list = lists[i]
                    list.sortWith(
                        when (co) {
                            Operator.GMB, Operator.NLB -> compareBy<Route> { it.gtfsId.toIntOrElse(Int.MAX_VALUE) }.thenBy { it.serviceType.toIntOrElse(Int.MAX_VALUE) }
                            else -> compareBy<Route> { it.serviceType.toIntOrElse(Int.MAX_VALUE) }.thenBy { it.gtfsId.toIntOrElse(Int.MAX_VALUE) }
                        }
                    )
                    add(list.distinctBy { it.stops[co] to it.freq })
                }
            }
        } catch (e: Throwable) {
            throw RuntimeException("Error occurred while getting branch routes for ${parameters.size} routes: ${e.message}", e)
        }
    }

    fun getAllBranchRoutes(routeNumber: String, bound: String, co: Operator, gmbRegion: GMBRegion?, includeFakeRoutes: Boolean = false): List<Route> {
        return try {
            getAllBranchRoutesBulk(listOf(element = AllBranchRoutesSearchParameters(routeNumber, bound, co, gmbRegion)), includeFakeRoutes).first()
        } catch (e: Throwable) {
            throw RuntimeException("Error occurred while getting branch routes for $routeNumber, $bound, $co, $gmbRegion: ${e.message}", e)
        }
    }

    fun getAllStops(routeNumber: String, bound: String, co: Operator, gmbRegion: GMBRegion?): List<StopData> {
        return getAllStops(routeNumber, bound, co, gmbRegion, true)
    }

    fun getAllStops(routeNumber: String, bound: String, co: Operator, gmbRegion: GMBRegion?, mergeEtaSeparateIds: Boolean): List<StopData> {
        return cache("getAllStops", routeNumber, bound, co, gmbRegion, mergeEtaSeparateIds) {
            try {
                val branches = getAllBranchRoutes(routeNumber, bound, co, gmbRegion, true)
                val stopList = DATA!!.dataSheet.stopList
                val lists: MutableList<BranchData> = mutableListOf()
                for (route in branches) {
                    val localStops: MutableBranchedList<String, StopData, Route> = MutableBranchedList(route)
                    val stops = route.stops[co]
                    val serviceType = route.serviceType.toIntOrElse(1)
                    for ((index, stopId) in stops!!.withIndex()) {
                        val fare = route.fares?.getOrNull(index)
                        val holidayFare = route.faresHoliday?.getOrNull(index)
                        localStops.add(stopId, StopData(stopId, serviceType, stopList[stopId]!!, route, fare = fare, holidayFare = holidayFare))
                    }
                    lists.add(BranchData(localStops, route, serviceType, co))
                }
                if (lists.isEmpty()) {
                    emptyList()
                } else {
                    val mergedStopIds: Set<Set<String>>
                    val mergedStopIdBranch: Map<String, List<Route>>
                    if (mergeEtaSeparateIds) {
                        val excludeStops = branches.asSequence()
                            .mapNotNull { it.stops[co] }
                            .flatMapTo(mutableSetOf()) {
                                it.asSequence()
                                    .filter { i ->
                                        val stopI = stopList[i]?.name
                                        it.any(2) { d -> stopI == stopList[d]?.name }
                                    }
                            }
                        val mapped = branches.asSequence()
                            .mapNotNull {
                                val stops = it.stops[co]?: return@mapNotNull null
                                stops.asSequence()
                                    .filterNot { i -> excludeStops.contains(i) }
                                    .map { i -> i to it }
                                    .toSet()
                            }
                            .flatten()
                            .toList()
                        mergedStopIds = mapped.asSequence()
                            .map { it.first }
                            .groupBy { stopList[it]?.name }
                            .values
                            .mapNotNullTo(mutableSetOf()) { if (it.size > 1) it.toSet() else null }
                        mergedStopIdBranch = mapped.toGroupedMap()
                    } else {
                        mergedStopIds = emptySet()
                        mergedStopIdBranch = emptyMap()
                    }
                    lists.sortWith(naturalOrder())
                    val result: MutableBranchedList<String, StopData, Route> = MutableBranchedList(
                        branchId = lists.first().branchedList.branchId,
                        conflictResolve = { a, b ->
                            val aType = a.serviceType
                            val bType = b.serviceType
                            if (aType == bType) {
                                val aGtfs = a.route.gtfsId.toIntOrElse(Int.MAX_VALUE)
                                val bGtfs = b.route.gtfsId.toIntOrElse(Int.MAX_VALUE)
                                if (aGtfs > bGtfs) b else a
                            } else {
                                if (aType > bType) b else a
                            }
                        },
                        equalityPredicate = { a, b ->
                            a == b || mergedStopIds.any { it.contains(a) && it.contains(b) }
                        }
                    )
                    val isMainBranchCircular = lists.firstOrNull { !it.route.fakeRoute }?.branchedList?.branchId?.isCircular == true
                    for ((first) in lists) {
                        result.merge(first, isMainBranchCircular || !first.branchId.isCircular)
                    }
                    result.asSequenceWithBranchIds()
                        .map { (f, s) ->
                            f.with(
                                branchIds = s.filterNotTo(mutableSetOf()) { it.fakeRoute },
                                mergedStopIds = mergedStopIds.firstOrNull { it.contains(f.stopId) }?.associateWith { mergedStopIdBranch[it]!! }
                            )
                        }
                        .toList()
                }
            } catch (e: Throwable) {
                throw RuntimeException("Error occurred while getting stops for $routeNumber, $bound, $co, $gmbRegion: ${e.message}", e)
            }
        }
    }

    @Immutable
    data class BranchData(
        val branchedList: MutableBranchedList<String, StopData, Route>,
        val route: Route,
        val serviceType: Int,
        val co: Operator
    ): Comparable<BranchData> {
        companion object {
            val COMPARATOR = compareBy<BranchData> { it.serviceType }.thenBy { !it.route.isCircular }.thenBy { it.route.stops[it.co]?.size?: 0 }
        }
        override fun compareTo(other: BranchData): Int {
            return COMPARATOR.compare(this, other)
        }
    }

    @Immutable
    data class StopData(
        val stopId: String,
        val serviceType: Int,
        val stop: Stop,
        val route: Route,
        val branchIds: Set<Route> = emptySet(),
        val mergedStopIds: Map<String, List<Route>> = mapOf(stopId to listOf(route)),
        val fare: Fare?,
        val holidayFare: Fare?
    ) {
        fun with(branchIds: Set<Route>?, mergedStopIds: Map<String, List<Route>>?): StopData {
            return StopData(
                stopId = stopId,
                serviceType = serviceType,
                stop = stop,
                route = route,
                branchIds = branchIds?: this.branchIds,
                mergedStopIds = mergedStopIds?: this.mergedStopIds,
                fare = fare,
                holidayFare = holidayFare
            )
        }
    }

    fun getStopSpecialDestinations(stopId: String, co: Operator, route: Route, prependTo: Boolean): BilingualText {
        if (route.lrtCircular != null) {
            return route.lrtCircular
        }
        val bound = route.bound[co]!!
        return when (stopId) {
            "LHP" -> if (bound.contains("UT")) {
                BilingualText("康城", "LOHAS Park")
            } else {
                BilingualText("北角/寶琳", "North Point/Po Lam")
            }
            "HAH", "POA" -> if (bound.contains("UT")) {
                BilingualText("寶琳", "Po Lam")
            } else {
                BilingualText("北角/康城", "North Point/LOHAS Park")
            }
            "AIR", "AWE" -> if (bound.contains("UT")) {
                BilingualText("博覽館", "AsiaWorld-Expo")
            } else {
                route.dest
            }
            else -> route.dest
        }.let {
            if (prependTo) {
                it.prependTo()
            } else {
                it
            }
        }
    }

    fun getAllDestinationsByDirection(routeNumber: String, co: Operator, nlbId: String?, gmbRegion: GMBRegion?, referenceRoute: Route, stopId: String): Pair<Set<BilingualText>, Set<BilingualText>> {
        return try {
            val direction: MutableSet<BilingualText> = HashSet()
            val all: MutableSet<BilingualText> = HashSet()
            for (route in DATA!!.dataSheet.routeList.values) {
                if (routeNumber == route.routeNumber && route.stops.containsKey(co)) {
                    val match = when (co) {
                        Operator.NLB -> nlbId == route.nlbId
                        Operator.GMB -> gmbRegion == route.gmbRegion
                        else -> true
                    }
                    if (match) {
                        val routeStops = route.stops[co]
                        if (routeStops!!.contains(stopId) && routeStops.commonElementPercentage(referenceRoute.stops[co]!!) > 0.5) {
                            direction.add(route.dest)
                        }
                    }
                    all.add(route.dest)
                }
            }
            direction to all
        } catch (e: Throwable) {
            throw RuntimeException("Error occurred while getting destinations by direction for " + routeNumber + ", " + nlbId + ", " + co + ", " + gmbRegion + ": " + e.message, e)
        }
    }

    fun isLrtStopOnOrAfter(thisStopId: String, targetStopNameZh: String, route: Route): Boolean {
        if (route.lrtCircular != null && targetStopNameZh == route.lrtCircular.zh) {
            return true
        }
        val stopIds = route.stops[Operator.LRT]?: return false
        val stopIndex = stopIds.indexOf(thisStopId)
        if (stopIndex < 0) {
            return false
        }
        for (i in stopIndex until stopIds.size) {
            val stopId = stopIds[i]
            val stop: Stop? = DATA!!.dataSheet.stopList[stopId]
            if (stop != null && stop.name.zh == targetStopNameZh) {
                return true
            }
        }
        return false
    }

    fun isMtrStopOnOrAfter(stopId: String, relativeTo: String, lineName: String, bound: String?): Boolean {
        for (data in DATA!!.dataSheet.routeList.values) {
            if (lineName == data.routeNumber && data.bound[Operator.MTR]!!.endsWith(bound!!)) {
                val stopsList = data.stops[Operator.MTR]!!
                val index = stopsList.indexOf(stopId)
                val indexRef = stopsList.indexOf(relativeTo)
                if (indexRef in 0..index) {
                    return true
                }
            }
        }
        return false
    }

    fun isMtrStopEndOfLine(stopId: String, lineName: String, bound: String?): Boolean {
        for (data in DATA!!.dataSheet.routeList.values) {
            if (lineName == data.routeNumber && data.bound[Operator.MTR]!!.endsWith(bound!!)) {
                val stopsList = data.stops[Operator.MTR]!!
                val index = stopsList.indexOf(stopId)
                if (index >= 0 && index + 1 < stopsList.size) {
                    return false
                }
            }
        }
        return true
    }

    fun getMtrStationInterchange(stopId: String, lineName: String): MTRInterchangeData {
        val lines: MutableSet<String> = mutableSetOf()
        val outOfStationLines: MutableSet<String> = mutableSetOf()
        if (stopId == "KOW" || stopId == "AUS") {
            outOfStationLines.add("HighSpeed")
        }
        val isOutOfStationPaid = when (stopId) {
            "ETS", "TST", "KOW", "AUS" -> false
            else -> true
        }
        var hasLightRail = false
        val outOfStationStopName = when (stopId) {
            "ETS" -> "尖沙咀"
            "TST" -> "尖東"
            "HOK" -> "中環"
            "CEN" -> "香港"
            else -> null
        }
        val stopName = DATA!!.dataSheet.stopList[stopId]!!.name.zh
        for (data in DATA!!.dataSheet.routeList.values) {
            val bound = data.bound
            val routeNumber = data.routeNumber
            if (routeNumber != lineName) {
                if (bound.containsKey(Operator.MTR)) {
                    val stopsList = data.stops[Operator.MTR]!!.asSequence()
                        .map { id: String? -> DATA!!.dataSheet.stopList[id]!!.name.zh }
                        .toList()
                    if (stopsList.contains(stopName)) {
                        lines.add(routeNumber)
                    } else if (outOfStationStopName != null && stopsList.contains(outOfStationStopName)) {
                        outOfStationLines.add(routeNumber)
                    }
                } else if (bound.containsKey(Operator.LRT) && !hasLightRail) {
                    if (data.stops[Operator.LRT]!!.any { DATA!!.dataSheet.stopList[it]!!.name.zh == stopName }) {
                        hasLightRail = true
                    }
                }
            }
        }
        return MTRInterchangeData(
            lines = lines.asSequence().distinct().sortedBy { Shared.getMtrLineSortingIndex(it) }.toList(),
            isOutOfStationPaid = isOutOfStationPaid,
            outOfStationLines = outOfStationLines.asSequence().distinct().sortedBy { Shared.getMtrLineSortingIndex(it) }.toList(),
            isHasLightRail = hasLightRail
        )
    }

    fun getLrtSameDirectionRoutes(stopId: String, stopIndex: Int, route: Route): Set<Route> {
        val stops = route.stops[Operator.LRT]!!
        val thisStopIndex = stops.indexesOf(stopId).takeIf { it.first() >= 0 }?.run {
            if (route.lrtCircular == null) minByOrNull { (it - stopIndex).absoluteValue } else min()
        }
        if (thisStopIndex == null || thisStopIndex + 1 >= stops.size) {
            return emptySet()
        }
        val nextStopId = stops[thisStopIndex + 1]
        val results: MutableSet<Route> = mutableSetOf()
        results.add(route)
        for (data in DATA!!.dataSheet.routeList.values) {
            if (data.co.contains(Operator.LRT) && !results.contains(data)) {
                val dataStops = data.stops[Operator.LRT]!!
                val indexes = dataStops.indexesOf(stopId).takeIf { it.first() >= 0 }?: continue
                val nextStopIds = indexes.mapNotNull { i -> i.takeIf { it + 1 < dataStops.size }?.let { dataStops[it + 1] } }
                if (nextStopIds.contains(nextStopId)) {
                    results.add(data)
                }
            }
        }
        return results
    }

    @Immutable
    data class MTRInterchangeData(val lines: List<String>, val isOutOfStationPaid: Boolean, val outOfStationLines: List<String>, val isHasLightRail: Boolean)

    private fun getNoScheduledDepartureMessage(language: String, altMessageInput: FormattedText?, isAboveTyphoonSignalEight: Boolean, typhoonWarningTitle: String): FormattedText {
        var altMessage = altMessageInput
        if (altMessage.isNullOrEmpty()) {
            altMessage = (if (language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次").asFormattedText()
        }
        if (isAboveTyphoonSignalEight) {
            altMessage += " ($typhoonWarningTitle)".asFormattedText()
        }
        if (isAboveTyphoonSignalEight) {
            altMessage = buildFormattedString {
                append(altMessage!!, Colored(0xFF88A3D1))
            }
        }
        return altMessage
    }

    val currentTyphoonData: Deferred<TyphoonInfo> get() {
        val cache = typhoonInfo.value
        if (cache != TyphoonInfo.NO_DATA && currentTimeMillis() - cache.lastUpdated < 300000) {
            return CompletableDeferred(cache)
        }
        return typhoonInfoDeferred.value.takeIf { it.isActive }?: CoroutineScope(Dispatchers.IO).async {
            val data = getJSONResponse<JsonObject>("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=warnsum&lang=" + if (Shared.language == "en") "en" else "tc")
            if (data == null) {
                return@async typhoonInfo.value
            } else if (data.contains("WTCSGNL")) {
                val matchingGroups = "TC([0-9]+)(.*)".toRegex().find(data.optJsonObject("WTCSGNL")!!.optString("code"))?.groupValues
                if (matchingGroups?.getOrNull(1) != null) {
                    val signal = matchingGroups[1].toInt()
                    val isAboveTyphoonSignalEight = signal >= 8
                    val isAboveTyphoonSignalNine = signal >= 9
                    val typhoonWarningTitle: String = if (Shared.language == "en") {
                        data.optJsonObject("WTCSGNL")!!.optString("type") + " is in force"
                    } else {
                        data.optJsonObject("WTCSGNL")!!.optString("type") + " 現正生效"
                    }
                    val currentTyphoonSignalId = if (signal < 8) {
                        "tc$signal${(matchingGroups.getOrNull(2)?: "").lowercase()}"
                    } else {
                        "tc" + signal.pad(2) + (matchingGroups.getOrNull(2)?: "").lowercase()
                    }
                    val info = TyphoonInfo.info(
                        isAboveTyphoonSignalEight,
                        isAboveTyphoonSignalNine,
                        typhoonWarningTitle,
                        currentTyphoonSignalId
                    )
                    typhoonInfo.value = info
                    return@async info
                }
            }
            val info = TyphoonInfo.none()
            typhoonInfo.value = info
            return@async info
        }.apply { typhoonInfoDeferred.value = this }
    }

    @Immutable
    class TyphoonInfo private constructor(
        val isAboveTyphoonSignalEight: Boolean,
        val isAboveTyphoonSignalNine: Boolean,
        val typhoonWarningTitle: String,
        val currentTyphoonSignalId: String,
        val lastUpdated: Long
    ) {

        companion object {

            val NO_DATA = TyphoonInfo(
                isAboveTyphoonSignalEight = false,
                isAboveTyphoonSignalNine = false,
                typhoonWarningTitle = "",
                currentTyphoonSignalId = "",
                lastUpdated = 0
            )

            fun none(): TyphoonInfo {
                return TyphoonInfo(
                    isAboveTyphoonSignalEight = false,
                    isAboveTyphoonSignalNine = false,
                    typhoonWarningTitle = "",
                    currentTyphoonSignalId = "",
                    lastUpdated = currentTimeMillis()
                )
            }

            fun info(isAboveTyphoonSignalEight: Boolean, isAboveTyphoonSignalNine: Boolean, typhoonWarningTitle: String, currentTyphoonSignalId: String): TyphoonInfo {
                return TyphoonInfo(isAboveTyphoonSignalEight, isAboveTyphoonSignalNine, typhoonWarningTitle, currentTyphoonSignalId, currentTimeMillis())
            }

        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as TyphoonInfo

            if (isAboveTyphoonSignalEight != other.isAboveTyphoonSignalEight) return false
            if (isAboveTyphoonSignalNine != other.isAboveTyphoonSignalNine) return false
            if (typhoonWarningTitle != other.typhoonWarningTitle) return false
            if (currentTyphoonSignalId != other.currentTyphoonSignalId) return false
            return lastUpdated == other.lastUpdated
        }

        override fun hashCode(): Int {
            var result = isAboveTyphoonSignalEight.hashCode()
            result = 31 * result + isAboveTyphoonSignalNine.hashCode()
            result = 31 * result + typhoonWarningTitle.hashCode()
            result = 31 * result + currentTyphoonSignalId.hashCode()
            result = 31 * result + lastUpdated.hashCode()
            return result
        }

    }

    data class LocationSearchEntry(
        val name: BilingualText,
        val address: BilingualText,
        val district: BilingualText,
        private val locationProvider: suspend () -> Coordinates
    ) {
        val displayAddress by lazy { BilingualText(
            zh = district.zh + address.zh.let { if (it.isEmpty()) "" else " $it" },
            en = address.en.let { if (it.isEmpty()) "" else "$it, " } + district.en
        ) }
        @Volatile
        private var locationCache: Coordinates? = null
        fun resolveLocation(): Deferred<Coordinates> {
            if (locationCache != null) return CompletableDeferred(locationCache!!)
            return CoroutineScope(Dispatchers.IO).async { locationProvider.invoke().apply { locationCache = this } }
        }
    }

    fun searchLocation(query: String): Deferred<List<LocationSearchEntry>> {
        return CoroutineScope(Dispatchers.IO).async {
            val result = mutableListOf<LocationSearchEntry>()
            try {
                val data = getJSONResponse<JsonArray>("https://geodata.gov.hk/gs/api/v1.0.0/locationSearch?q=${query.encodeURLQueryComponent()}")!!
                for (entry in data) {
                    val json = entry.jsonObject
                    val name = json.optString("nameZH") withEn json.optString("nameEN")
                    val address = json.optString("addressZH") withEn json.optString("addressEN")
                    val district = json.optString("districtZH") withEn "${json.optString("districtEN")} District"
                    val x = json.optDouble("x")
                    val y = json.optDouble("y")
                    result.add(LocationSearchEntry(name, address, district) {
                        val convert = getJSONResponse<JsonObject>("https://www.geodetic.gov.hk/transform/v2/?insys=hkgrid&outsys=wgsgeog&e=$x&n=$y")!!
                        Coordinates(convert.optDouble("wgsLat"), convert.optDouble("wgsLong"))
                    })
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return@async result
        }
    }

    suspend fun getMtrLineServiceDisruption(): Map<String, TrainServiceStatus>? {
        val now = currentTimeMillis()
        return CoroutineScope(Dispatchers.IO).async {
            val map = mutableMapOf<String, TrainServiceStatus>()
            try {
                val data = getJSONResponse<JsonObject>("https://tnews.mtr.com.hk/alert/ryg_line_status.json?_=$now")!!.optJsonObject("ryg_status")!!
                val typhoon = typhoonInfo.value.isAboveTyphoonSignalNine
                for (jsonElement in data.optJsonArray("line")!!) {
                    val line = jsonElement.jsonObject
                    val lineCode = line.optString("line_code")
                    val status = line.optString("status")
                    val statusType = when (status) {
                        "green" -> TrainServiceStatusType.NORMAL
                        "grey" -> TrainServiceStatusType.NON_SERVICE_HOUR
                        "typhoon" -> TrainServiceStatusType.TYPHOON
                        else -> if (typhoon) TrainServiceStatusType.TYPHOON else TrainServiceStatusType.DISRUPTION
                    }
                    val statusMessage = if (line.containsKeyAndNotNull("messages")) {
                        val messages = line["messages"]
                        if (messages is JsonObject && messages.containsKeyAndNotNull("message")) {
                            val messageJson = messages.optJsonObject("message")!!
                            val messageTitle = messageJson.optString("title_tc").trim { it.isWhitespace() || it == '\t' } withEn messageJson.optString("title_en").trim { it.isWhitespace() || it == '\t' }
                            val messageStatus = if (messageJson.optString("status").equals("red", ignoreCase = true)) TrainServiceStatusMessageStatus.RED else TrainServiceStatusMessageStatus.NORMAL
                            val messageUrl = messageJson.optString("url_tc") withEn messageJson.optString("url_en")
                            val messageUrlMobile = messageJson.optString("url_tc_m") withEn messageJson.optString("url_en_m")
                            TrainServiceStatusMessage(
                                title = messageTitle,
                                status = messageStatus,
                                url = messageUrl,
                                urlMobile = messageUrlMobile
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                    map[lineCode] = TrainServiceStatus(
                        type = statusType,
                        messages = statusMessage
                    )
                }
                map
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }.await()
    }

    private val routeNoticeCache: MutableMap<Any, Triple<AutoSortedList<RouteNotice, SnapshotStateList<RouteNotice>>, Long, String>> = ConcurrentMutableMap()

    fun getOperatorNotices(co: Set<Operator>, context: AppContext): SnapshotStateList<RouteNotice> {
        val now = currentTimeMillis()
        return (routeNoticeCache[co]?.takeIf { now - it.second < 300000 && it.third == Shared.language }?.first?: run {
            mutableStateListOf<RouteNotice>().asAutoSortedList().apply {
                routeNoticeCache[co] = Triple(this, now, Shared.language)
                when {
                    co.contains(Operator.MTR) -> add(mtrLineStatus)
                    co.contains(Operator.LRT) -> add(lrtLineStatus)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        when {
                            co.contains(Operator.LRT) -> {
                                val data = getJSONResponse<JsonObject>("https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=001")!!
                                val titleZh = data.optString("red_alert_message_ch").ifBlank { null }
                                val titleEn = data.optString("red_alert_message_en").ifBlank { null }
                                val urlZh = data.optString("red_alert_url_ch").ifBlank { null }
                                val urlEn = data.optString("red_alert_url_en").ifBlank { null }
                                if (titleZh != null && titleEn != null) {
                                    if (urlZh == null || urlEn == null) {
                                        add(RouteNoticeText(
                                            title = titleZh withEn titleEn,
                                            co = Operator.LRT,
                                            importance = RouteNoticeImportance.IMPORTANT,
                                            content = "".asBilingualText(),
                                            isRealTitle = true,
                                            sort = 0
                                        ))
                                    } else {
                                        add(RouteNoticeExternal(
                                            title = titleZh withEn titleEn,
                                            co = Operator.LRT,
                                            importance = RouteNoticeImportance.IMPORTANT,
                                            url = urlZh withEn urlEn,
                                            sort = 0
                                        ))
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    try {
                        val data = getXMLResponse<TrafficNews>("https://td.gov.hk/tc/special_news/trafficnews.xml")!!
                        for (news in data.messages) {
                            val operators = news.getOperators()
                            val important = operators.any { co.contains(it) }
                            if (operators.isEmpty() || important) {
                                this@apply.add(RouteNoticeText(
                                    title = news.getTitle(),
                                    co = null,
                                    isRealTitle = true,
                                    importance = if (important) RouteNoticeImportance.IMPORTANT else RouteNoticeImportance.NOT_IMPORTANT,
                                    content = news.getBody(context)
                                ))
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    try {
                        val data = getXMLResponse<SpecialTrafficNews>("https://resource.data.one.gov.hk/td/en/specialtrafficnews.xml")!!
                        for (news in data.messages) {
                            val operators = news.getOperators()
                            val important = operators.any { co.contains(it) }
                            if (operators.isEmpty() || important) {
                                this@apply.add(RouteNoticeText(
                                    title = news.getTitle(),
                                    co = null,
                                    isRealTitle = false,
                                    importance = if (important) RouteNoticeImportance.IMPORTANT else RouteNoticeImportance.NOT_IMPORTANT,
                                    content = news.getBody(context)
                                ))
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }).backingList
    }

    fun getRouteNotices(route: Route, context: AppContext): SnapshotStateList<RouteNotice> {
        val now = currentTimeMillis()
        return (routeNoticeCache[route]?.takeIf { now - it.second < 300000 && it.third == Shared.language }?.first?: run {
            mutableStateListOf<RouteNotice>().asAutoSortedList().apply {
                routeNoticeCache[route] = Triple(this, now, Shared.language)
                route.defaultOperatorNotices(this)
                CoroutineScope(Dispatchers.IO).launch {
                    route.fetchOperatorNotices(this, this@apply)
                    launch {
                        try {
                            val data = getXMLResponse<TrafficNews>("https://td.gov.hk/tc/special_news/trafficnews.xml")!!
                            for (news in data.messages) {
                                val operators = news.getOperators()
                                val important = operators.any { route.co.contains(it) }
                                if (operators.isEmpty() || important) {
                                    this@apply.add(RouteNoticeText(
                                        title = news.getTitle(),
                                        co = null,
                                        isRealTitle = true,
                                        importance = if (important) RouteNoticeImportance.IMPORTANT else RouteNoticeImportance.NOT_IMPORTANT,
                                        content = news.getBody(context)
                                    ))
                                }
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                    launch {
                        try {
                            val data = getXMLResponse<SpecialTrafficNews>("https://resource.data.one.gov.hk/td/en/specialtrafficnews.xml")!!
                            for (news in data.messages) {
                                val operators = news.getOperators()
                                val important = operators.any { route.co.contains(it) }
                                if (operators.isEmpty() || important) {
                                    this@apply.add(RouteNoticeText(
                                        title = news.getTitle(),
                                        co = null,
                                        isRealTitle = false,
                                        importance = if (important) RouteNoticeImportance.IMPORTANT else RouteNoticeImportance.NOT_IMPORTANT,
                                        content = news.getBody(context)
                                    ))
                                }
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }).backingList
    }

    fun getCtbHasTwoWaySectionFare(routeNumber: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = Json.decodeFromString<JsonObject>(getTextResponse("https://www.citybus.com.hk/app/newfare/data/${routeNumber}.json")!!.string().remove("\uFEFF"))
                callback.invoke(data.optJsonArray(routeNumber)!!.any { it.jsonObject.optString("two_way") == "true" })
            } catch (e: Throwable) {
                e.printStackTrace()
                callback.invoke(false)
            }
        }
    }

    private val cachedPrefixes: MutableList<String> = ConcurrentMutableList()
    private val cachedTimes: MutableMap<String, MutableMap<String, Double>> = ConcurrentMutableMap()

    private var cachedHourly: Pair<DayOfWeek, Int>? = null
    private val cachedHourlyPrefixes: MutableList<String> = ConcurrentMutableList()
    private val cachedHourlyTimes: MutableMap<String, MutableMap<String, Double>> = ConcurrentMutableMap()

    private fun currentHourlyTimeBetweenStop(time: Long = currentTimeMillis()): Pair<DayOfWeek, Int> {
        return time.toLocalDateTime().run { dayOfWeek to hour }
    }

    private suspend fun cacheTimeBetweenStopPrefix(prefix: String) {
        if (!cachedPrefixes.contains(prefix)) {
            val data = getJSONResponse<JsonObject>("https://timeinterval.hkbuseta.com/times/$prefix.json")?: return
            for ((stopId, nextStopIds) in data) {
                val cacheMap = cachedTimes.getOrPut(stopId) { ConcurrentMutableMap() }
                for ((nextStopId, time) in nextStopIds.jsonObject) {
                    cacheMap[nextStopId] = time.jsonPrimitive.double
                }
            }
            cachedPrefixes.add(prefix)
        }
    }

    private suspend fun cacheTimeBetweenStopPrefixHourly(prefix: String) {
        val currentHourly = currentHourlyTimeBetweenStop()
        if (currentHourly != cachedHourly) {
            cachedHourlyPrefixes.clear()
            cachedHourlyTimes.clear()
        }
        if (!cachedHourlyPrefixes.contains(prefix)) {
            val (weekday, hour) = currentHourly
            val data = getJSONResponse<JsonObject>("https://timeinterval.hkbuseta.com/times_hourly/${weekday.sundayZeroDayNumber}/${hour.pad(2)}/$prefix.json")?: return
            for ((stopId, nextStopIds) in data) {
                val cacheMap = cachedHourlyTimes.getOrPut(stopId) { ConcurrentMutableMap() }
                for ((nextStopId, time) in nextStopIds.jsonObject) {
                    cacheMap[nextStopId] = time.jsonPrimitive.double
                }
            }
            cachedHourlyPrefixes.add(prefix)
        }
    }

    @Immutable
    data class TimeBetweenStopResult(val averageInterval: Int, val currentHourlyInterval: Int? = null) {
        companion object {
            val NOT_AVAILABLE: TimeBetweenStopResult = TimeBetweenStopResult(-1)
            val LOADING: TimeBetweenStopResult = TimeBetweenStopResult(Int.MAX_VALUE)
        }
        constructor(averageInterval: Double, currentHourlyInterval: Double? = null):
                this(averageInterval.roundToInt(), currentHourlyInterval?.roundToInt())
        val isLoading: Boolean = averageInterval >= Int.MAX_VALUE
        val interval: Int? = if (isLoading) null else (currentHourlyInterval?: averageInterval)
    }

    fun getTimeBetweenStop(stopIds: List<Pair<String, Boolean>>, startIndex: Int, endIndex: Int, lookupHourly: Boolean): Deferred<TimeBetweenStopResult> {
        return CoroutineScope(Dispatchers.IO).async {
            val intervals = buildList(stopIds.size) {
                for ((index, value) in stopIds.withIndex()) {
                    val (stopId, inBranch) = value
                    if (inBranch && index in startIndex until endIndex) {
                        val (nextStopId, _) = stopIds.subList(index + 1, stopIds.size).firstOrNull { (_, b) -> b }?: break
                        val prefix = if (stopId.length < 3) stopId else stopId.substring(0, 2)
                        add(CoroutineScope(Dispatchers.IO).async {
                            cacheTimeBetweenStopPrefix(prefix)
                            if (lookupHourly) cacheTimeBetweenStopPrefixHourly(prefix)
                            val cacheMap = cachedTimes.getOrPut(stopId) { ConcurrentMutableMap() }
                            val cacheHourlyMap = cachedHourlyTimes.getOrPut(stopId) { ConcurrentMutableMap() }
                            (cacheMap[nextStopId]?: -1.0) to (cacheHourlyMap[nextStopId]?: -1.0)
                        })
                    }
                }
            }.awaitAll()
            var time = 0.0
            for ((interval) in intervals) {
                if (interval < 0) return@async TimeBetweenStopResult.NOT_AVAILABLE
                time += interval
            }
            var timeHourly = 0.0
            for ((_, interval) in intervals) {
                if (interval < 0) return@async TimeBetweenStopResult(time)
                timeHourly += interval
            }
            return@async TimeBetweenStopResult(time, timeHourly)
        }
    }

    private val cachedWaypoints: MutableMap<Route, RouteWaypoints> = ConcurrentMutableMap()
    private val cachedTrafficSnapshots: MutableMap<RouteWaypoints, Array<out List<TrafficSnapshotPoint>>> = ConcurrentMutableMap()

    fun getRouteWaypoints(route: Route, allStops: List<StopData>, context: AppContext): Deferred<RouteWaypoints> {
        val id = route.waypointsId
        val gzip = gzipSupported()
        val (gzipFolder, gzipSuffix) = if (gzip) "_gzip" to ".gz" else "" to ""
        val routeNumber = route.routeNumber
        val co = route.co.firstCo()!!
        val isKmbCtbJoint = route.isKmbCtbJoint
        val stopIds = if (co.isTrain) allStops.map { it.stopId } else route.stops[co]!!
        val stops = if (co.isTrain) allStops.map { it.stop } else stopIds.map { it.asStop(context)!! }
        return CoroutineScope(Dispatchers.IO).async {
            return@async cachedWaypoints.getOrPut(route) {
                getJSONResponse<JsonObject>("https://waypoints.hkbuseta.com/waypoints$gzipFolder/$id.json$gzipSuffix", gzip)
                    ?.optJsonArray("features")
                    ?.optJsonObject(0)
                    ?.optJsonObject("geometry")
                    ?.optJsonArray("coordinates")
                    ?.let {
                        val path = it
                            .map { l -> l.jsonArray.map { e -> Coordinates.fromJsonArray(e.jsonArray, true) } }
                            .run {
                                if (size == 1) {
                                    var path = first()
                                    val firstStopLocation = stops.first().location
                                    if (path.first().distance(firstStopLocation) > path.last().distance(firstStopLocation)) {
                                        path = path.reversed()
                                    }
                                    if (stops.size > 2) {
                                        path.splitByClosestPoints(stops.map { s -> s.location })
                                    } else {
                                        listOf(element = path)
                                    }
                                } else {
                                    val firstStopLocation = stops.first().location
                                    map { path ->
                                        if (path.first().distance(firstStopLocation) > path.last().distance(firstStopLocation)) {
                                            path.reversed()
                                        } else {
                                            path
                                        }
                                    }
                                }
                            }
                            .map { p -> p.simplified() }
                        RouteWaypoints(routeNumber, co, isKmbCtbJoint, stopIds, stops, path, true)
                    }
                    ?: RouteWaypoints(routeNumber, co, isKmbCtbJoint, stopIds, stops, listOf(element = stops.map { it.location }), false)
            }
        }
    }

    @ReduceDataOmitted
    fun getRouteTrafficSnapshots(waypoints: RouteWaypoints): Array<out List<TrafficSnapshotPoint>>? {
        if (!waypoints.isHighRes && waypoints.co.isBus) return null
        return cachedTrafficSnapshots.getOrPut(waypoints) {
            val result: Array<MutableList<TrafficSnapshotPoint>> = Array(waypoints.stops.size) { mutableListOf() }
            val usedTrafficSnapshots: MutableSet<TrafficSnapshotPoint> = mutableSetOf()
            for ((i, path) in waypoints.paths.withIndex()) {
                val trafficSnapshots = path.findPointsWithinDistanceOrdered(
                    items = DATA!!.trafficSnapshot!!,
                    itemLocation = { location },
                    threshold = 0.05
                )
                for (trafficSnapshot in trafficSnapshots) {
                    if (usedTrafficSnapshots.add(trafficSnapshot)) {
                        result.getOrElse(i.coerceAtMost(waypoints.stops.size - 2)) { result.last() }.add(trafficSnapshot)
                    }
                }
            }
            result
        }
    }

    fun getAppAlerts(): Deferred<AppAlert?> {
        return CoroutineScope(Dispatchers.IO).async {
            try {
                getJSONResponse<AppAlert>("https://app-alerts.hkbuseta.com/alert.json")
            } catch (e: Throwable) {
                null
            }
        }
    }

    @Immutable
    data class NextBusPosition(
        val routeNumber: String,
        val stopId: String,
        val timeToStop: Long,
        val type: NextBusStatusType,
        val stopsCount: Int
    )

    enum class NextBusStatusType {
        ARRIVING, DEPARTING
    }

    @Immutable
    class NextBusPositionQueryTask(task: suspend () -> NextBusPosition?): QueryTask<NextBusPosition?>(null, task)

    fun findNextBusPosition(stopId: String, stopIndex: Int, co: Operator, route: Route, stopsList: List<StopData>, context: AppContext, options: EtaQueryOptions? = null): NextBusPositionQueryTask {
        if (!co.isBus) return NextBusPositionQueryTask { null }
        val startingIndex = stopsList
            .indexesOf { it.stopId == stopId }
            .minByOrNull { (it - stopIndex).absoluteValue }
            ?: return NextBusPositionQueryTask { null }
        val endingIndex = stopsList.indexOfFirst { it.branchIds.contains(route) }
        val timeBetweenStopsList = stopsList.map { it.stopId to it.branchIds.contains(route) }
        return NextBusPositionQueryTask {
            var previousStopTimeRounded = Long.MAX_VALUE
            var previousStopTime = previousStopTimeRounded.toDouble()
            var previousStop: String? = null
            var previousStopIndex = Int.MAX_VALUE
            var type = NextBusStatusType.ARRIVING
            var etaAtThisStop: Double? = null
            var stopsCount = -1
            for (i in startingIndex downTo endingIndex) {
                val stopData = stopsList[i]
                if (!stopData.branchIds.contains(route)) continue
                val result = buildEtaQuery(stopData.stopId, i, co, route, context, options).query().rawLines[1]
                if (i == startingIndex) {
                    etaAtThisStop = result?.eta
                }
                if (result != null && result.etaRounded > 0 && result.eta <= previousStopTime) {
                    previousStopTime = result.eta
                    previousStopTimeRounded = result.etaRounded
                    previousStop = stopData.stopId
                    previousStopIndex = i
                    if (i <= endingIndex) {
                        type = NextBusStatusType.DEPARTING
                    }
                    stopsCount++
                } else {
                    break
                }
            }
            previousStop
                ?.takeIf {
                    if (etaAtThisStop != null) {
                        val timeToStop = getTimeBetweenStop(timeBetweenStopsList, previousStopIndex, startingIndex, true).await().interval
                        if (timeToStop != null) {
                            val time = (timeToStop / 60) + previousStopTimeRounded
                            time <= (etaAtThisStop + (etaAtThisStop * 0.25).coerceAtLeast(5.0))
                        } else {
                            true
                        }
                    } else {
                        true
                    }
                }
                ?.let { NextBusPosition(route.routeNumber, it, previousStopTimeRounded, type, stopsCount) }
        }
    }

    @Immutable
    data class EtaQueryOptions(
        val lrtDirectionMode: Boolean = false,
        val lrtAllMode: Boolean = false
    )

    fun buildEtaQuery(stopId: String, stopIndex: Int, co: Operator, route: Route, context: AppContext, options: EtaQueryOptions? = null): ETAQueryTask {
        val errorResult: ETAQueryResult by lazy {
            val restrictionType = if (context is AppActiveContext) BackgroundRestrictionType.NONE else context.currentBackgroundRestrictions()
            ETAQueryResult.connectionError(restrictionType, co)
        }
        return ETAQueryTask(errorResult) {
            context.logFirebaseEvent("eta_query", AppBundle().apply {
                putString("by_stop", stopId + "," + stopIndex + "," + route.routeNumber + "," + co.name + "," + route.bound[co])
                putString("by_bound", route.routeNumber + "," + co.name + "," + route.bound[co])
                putString("by_route", route.routeNumber + "," + co.name)
            })
            try {
                val typhoonInfo = currentTyphoonData.await()
                when {
                    route.isKmbCtbJoint -> etaQueryKmbCtbJoint(typhoonInfo, stopId, stopIndex, co, route)
                    co === Operator.KMB -> etaQueryKmb(typhoonInfo, stopId, stopIndex, co, route, context)
                    co === Operator.CTB -> etaQueryCtb(typhoonInfo, stopId, stopIndex, co, route)
                    co === Operator.NLB -> etaQueryNlb(typhoonInfo, stopId, co, route)
                    co === Operator.MTR_BUS -> etaQueryMtrBus(typhoonInfo, stopId, co, route)
                    co === Operator.GMB -> etaQueryGmb(typhoonInfo, stopId, stopIndex, co, route, context)
                    co === Operator.LRT -> etaQueryLrt(typhoonInfo, stopId, stopIndex, co, route, options)
                    co === Operator.MTR -> etaQueryMtr(typhoonInfo, stopId, co, route)
                    co === Operator.SUNFERRY -> etaQuerySunFerry(typhoonInfo, stopId, co, route)
                    co === Operator.HKKF -> etaQueryHkkf(typhoonInfo, stopId, co, route)
                    co === Operator.FORTUNEFERRY -> etaQueryFortuneFerry(typhoonInfo, stopId, co, route)
                    else -> throw IllegalStateException("Unknown Operator ${co.name}")
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                val restrictionType = if (context is AppActiveContext) BackgroundRestrictionType.NONE else context.currentBackgroundRestrictions()
                ETAQueryResult.connectionError(restrictionType, co)
            }
        }
    }

    private suspend fun etaQueryKmbCtbJoint(typhoonInfo: TyphoonInfo, stopId: String, stopIndex: Int, co: Operator, route: Route): ETAQueryResult {
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        val isMtrEndOfLine = false
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
        var nextCo = co
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val jointOperated: MutableSet<JointOperatedEntry> = ConcurrentMutableSet()
        var kmbSpecialMessage: FormattedText? = null
        var kmbFirstScheduledBus = Long.MAX_VALUE
        coroutineScope {
            val kmbFuture = launch {
                val now = currentTimeMillis()
                val data = getJSONResponse<JsonObject>("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/$stopId?t=$now")
                val buses = data!!.optJsonArray("data")!!
                val stopSequences: MutableSet<Int> = HashSet()
                for (u in 0 until buses.size) {
                    val bus = buses.optJsonObject(u)!!
                    if (Operator.KMB === Operator.valueOf(bus.optString("co"))) {
                        val routeNumber = bus.optString("route")
                        val bound = bus.optString("dir")
                        if (routeNumber == route.routeNumber && bound == route.bound[Operator.KMB]) {
                            stopSequences.add(bus.optInt("seq"))
                        }
                    }
                }
                val matchingSeq = stopSequences.minByOrNull { (it - stopIndex).absoluteValue }?: -1
                val usedRealSeq: MutableSet<Int> = HashSet()
                for (u in 0 until buses.size) {
                    val bus = buses.optJsonObject(u)!!
                    if (Operator.KMB === Operator.valueOf(bus.optString("co"))) {
                        val routeNumber = bus.optString("route")
                        val bound = bus.optString("dir")
                        val stopSeq = bus.optInt("seq")
                        if (routeNumber == route.routeNumber && bound == route.bound[Operator.KMB] && stopSeq == matchingSeq && usedRealSeq.add(bus.optInt("eta_seq"))) {
                            val eta = bus.optString("eta")
                            if (eta.isNotEmpty() && !eta.equals("null", ignoreCase = true)) {
                                val mins = (eta.parseInstant().epochSeconds - currentEpochSeconds) / 60.0
                                val minsRounded = mins.roundToInt()
                                var timeMessage = "".asFormattedText()
                                var remarkMessage = "".asFormattedText()
                                if (language == "en") {
                                    if (minsRounded > 0) {
                                        timeMessage = buildFormattedString {
                                            append(minsRounded.toString(), BoldStyle)
                                            append(" Min.", SmallSize)
                                        }
                                    } else if (minsRounded > -60) {
                                        timeMessage = buildFormattedString {
                                            append("-", BoldStyle)
                                            append(" Min.", SmallSize)
                                        }
                                    }
                                    if (bus.optString("rmk_en").isNotEmpty()) {
                                        remarkMessage += buildFormattedString {
                                            if (timeMessage.isEmpty()) {
                                                append(bus.optString("rmk_en")
                                                    .replace("Final Bus", "Final KMB Bus"))
                                            } else {
                                                append(" (${bus.optString("rmk_en")
                                                    .replace("Final Bus", "Final KMB Bus")})", SmallSize)
                                            }
                                        }
                                    }
                                } else {
                                    if (minsRounded > 0) {
                                        timeMessage = buildFormattedString {
                                            append(minsRounded.toString(), BoldStyle)
                                            append(" 分鐘", SmallSize)
                                        }
                                    } else if (minsRounded > -60) {
                                        timeMessage = buildFormattedString {
                                            append("-", BoldStyle)
                                            append(" 分鐘", SmallSize)
                                        }
                                    }
                                    if (bus.optString("rmk_tc").isNotEmpty()) {
                                        remarkMessage += buildFormattedString {
                                            if (timeMessage.isEmpty()) {
                                                append(bus.optString("rmk_tc")
                                                    .replace("原定", "預定")
                                                    .replace("最後班次", "九巴尾班車")
                                                    .replace("九巴尾班車已過", "尾班車已過本站"))
                                            } else {
                                                append(" (${bus.optString("rmk_tc")
                                                    .replace("原定", "預定")
                                                    .replace("最後班次", "九巴尾班車")
                                                    .replace("九巴尾班車已過", "尾班車已過本站")})", SmallSize)
                                            }
                                        }
                                    }
                                }
                                if ((remarkMessage.contains("預定班次") || remarkMessage.contains("Scheduled Bus")) && mins < kmbFirstScheduledBus) {
                                    kmbFirstScheduledBus = minsRounded.toLong()
                                }
                                jointOperated.add(JointOperatedEntry(mins, minsRounded.toLong(), timeMessage, remarkMessage, Operator.KMB))
                            } else {
                                var remarkMessage = "".asFormattedText()
                                if (language == "en") {
                                    if (bus.optString("rmk_en").isNotEmpty()) {
                                        remarkMessage += buildFormattedString {
                                            append(bus.optString("rmk_en")
                                                .replace("Final Bus", "Final KMB Bus"))
                                        }
                                    }
                                } else {
                                    if (bus.optString("rmk_tc").isNotEmpty()) {
                                        remarkMessage += buildFormattedString {
                                            append(bus.optString("rmk_tc")
                                                .replace("原定", "預定")
                                                .replace("最後班次", "九巴尾班車")
                                                .replace("九巴尾班車已過", "尾班車已過本站"))
                                        }
                                    }
                                }
                                remarkMessage = if (remarkMessage.isEmpty() || typhoonInfo.isAboveTyphoonSignalEight && (remarkMessage strEq "ETA service suspended" || remarkMessage strEq "暫停預報")) {
                                    getNoScheduledDepartureMessage(language, remarkMessage, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                } else {
                                    "".asFormattedText(BoldStyle) + remarkMessage
                                }
                                kmbSpecialMessage = remarkMessage
                            }
                        }
                    }
                }
            }
            run {
                val routeNumber = route.routeNumber
                val matchingStops: List<Pair<Operator, String>>? = DATA!!.dataSheet.stopMap[stopId]
                val ctbStopIds: MutableList<String> = mutableListOf()
                if (matchingStops == null) {
                    val cacheKey = routeNumber + "_" + stopId + "_" + stopIndex + "_ctb"
                    @Suppress("UNCHECKED_CAST")
                    val cachedIds = objectCache[cacheKey] as List<String>?
                    if (cachedIds == null) {
                        val location: Coordinates = DATA!!.dataSheet.stopList[stopId]!!.location
                        for ((key, value) in DATA!!.dataSheet.stopList.entries) {
                            if (Operator.CTB.matchStopIdPattern(key) && location.distance(value.location) < 0.4) {
                                ctbStopIds.add(key)
                            }
                        }
                        objectCache[cacheKey] = ctbStopIds.toList()
                    } else {
                        ctbStopIds.addAll(cachedIds)
                    }
                } else {
                    for ((first, second) in matchingStops) {
                        if (Operator.CTB === first) {
                            ctbStopIds.add(second)
                        }
                    }
                }
                val (first, second) = getAllDestinationsByDirection(routeNumber, Operator.KMB, null, null, route, stopId)
                val destKeys = second.asSequence().map { it.zh.remove(" ") }.toSet()
                val ctbEtaEntries: ConcurrentMutableMap<String?, MutableSet<JointOperatedEntry>> = ConcurrentMutableMap()
                val stopQueryData = buildList {
                    for (ctbStopId in ctbStopIds) {
                        add(async { getJSONResponse<JsonObject>("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/$ctbStopId/$routeNumber") })
                    }
                }.awaitAll()
                val stopSequences: MutableMap<String, MutableSet<Int>> = mutableMapOf()
                val queryBusDests: Array<Array<String?>?> = arrayOfNulls(stopQueryData.size)
                for (i in stopQueryData.indices) {
                    val data = stopQueryData[i]
                    val buses = data!!.optJsonArray("data")!!
                    val busDests = arrayOfNulls<String>(buses.size)
                    for (u in 0 until buses.size) {
                        val bus = buses.optJsonObject(u)!!
                        if (Operator.CTB === Operator.valueOf(bus.optString("co")) && routeNumber == bus.optString("route")) {
                            val rawBusDest = bus.optString("dest_tc").remove(" ")
                            val busDest = destKeys.asSequence().minBy { it.editDistance(rawBusDest) }
                            busDests[u] = busDest
                            stopSequences.getOrPut(busDest) { HashSet() }.add(bus.optInt("seq"))
                        }
                    }
                    queryBusDests[i] = busDests
                }
                val matchingSeq = stopSequences.entries.asSequence()
                    .map { (key, value) -> key to (value.minByOrNull { (it - stopIndex).absoluteValue }?: -1) }
                    .toMap()
                for (i in stopQueryData.indices) {
                    val data = stopQueryData[i]!!
                    val buses = data.optJsonArray("data")!!
                    val usedRealSeq: MutableMap<String?, MutableSet<Int>> = mutableMapOf()
                    for (u in 0 until buses.size) {
                        val bus = buses.optJsonObject(u)!!
                        if (Operator.CTB === Operator.valueOf(bus.optString("co")) && routeNumber == bus.optString("route")) {
                            val busDest = queryBusDests[i]!![u]!!
                            val stopSeq = bus.optInt("seq")
                            if ((stopSeq == (matchingSeq[busDest]?: 0)) && usedRealSeq.getOrPut(busDest) { HashSet() }.add(bus.optInt("eta_seq"))) {
                                val eta = bus.optString("eta")
                                if (eta.isNotEmpty() && !eta.equals("null", ignoreCase = true)) {
                                    val mins = (eta.parseInstant().epochSeconds - currentEpochSeconds) / 60.0
                                    val minsRounded = mins.roundToInt()
                                    var timeMessage = "".asFormattedText()
                                    var remarkMessage = "".asFormattedText()
                                    if (language == "en") {
                                        if (minsRounded > 0) {
                                            timeMessage = buildFormattedString {
                                                append(minsRounded.toString(), BoldStyle)
                                                append(" Min.", SmallSize)
                                            }
                                        } else if (minsRounded > -60) {
                                            timeMessage = buildFormattedString {
                                                append("-", BoldStyle)
                                                append(" Min.", SmallSize)
                                            }
                                        }
                                        if (bus.optString("rmk_en").isNotEmpty()) {
                                            remarkMessage += buildFormattedString {
                                                if (timeMessage.isEmpty()) {
                                                    append(bus.optString("rmk_en"))
                                                } else {
                                                    append(" (${bus.optString("rmk_en")})", SmallSize)
                                                }
                                            }
                                        }
                                    } else {
                                        if (minsRounded > 0) {
                                            timeMessage = buildFormattedString {
                                                append(minsRounded.toString(), BoldStyle)
                                                append(" 分鐘", SmallSize)
                                            }
                                        } else if (minsRounded > -60) {
                                            timeMessage = buildFormattedString {
                                                append("-", BoldStyle)
                                                append(" 分鐘", SmallSize)
                                            }
                                        }
                                        if (bus.optString("rmk_tc").isNotEmpty()) {
                                            remarkMessage += buildFormattedString {
                                                if (timeMessage.isEmpty()) {
                                                    append(bus.optString("rmk_tc")
                                                        .replace("原定", "預定")
                                                        .replace("最後班次", "尾班車")
                                                        .replace("尾班車已過", "尾班車已過本站"))
                                                } else {
                                                    append(" (${bus.optString("rmk_tc")
                                                        .replace("原定", "預定")
                                                        .replace("最後班次", "尾班車")
                                                        .replace("尾班車已過", "尾班車已過本站")})", SmallSize)
                                                }
                                            }
                                        }
                                    }
                                    ctbEtaEntries.synchronize {
                                        ctbEtaEntries.getOrPut(busDest) { ConcurrentMutableSet() }.add(
                                            JointOperatedEntry(mins, minsRounded.toLong(), timeMessage, remarkMessage, Operator.CTB)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                first.asSequence().map { ctbEtaEntries[it.zh.remove(" ")] }.forEach {
                    if (it != null) {
                        jointOperated.addAll(it)
                    }
                }
            }
            kmbFuture.join()
        }
        if (jointOperated.isEmpty()) {
            if (kmbSpecialMessage.isNullOrEmpty()) {
                lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
            } else {
                lines[1] = ETALineEntry.textEntry(kmbSpecialMessage!!)
            }
        } else {
            var counter = 0
            for (entry in jointOperated.asSequence().sorted()) {
                val mins = entry.mins
                if (mins.isFinite() && mins < -10.0) continue
                val minsRounded = entry.minsRounded
                val timeMessage = "".asFormattedText(BoldStyle) + entry.time
                var remarkMessage = "".asFormattedText(BoldStyle) + entry.remark
                val entryCo = entry.co
                if (minsRounded > kmbFirstScheduledBus && !(remarkMessage.contains("預定班次") || remarkMessage.contains("Scheduled Bus"))) {
                    remarkMessage += (if (language == "en") " (Scheduled Bus)" else " (預定班次)").asFormattedText(SmallSize)
                }
                val operatorMessage = if (entryCo === Operator.KMB) {
                    if (route.routeNumber.getKMBSubsidiary() === KMBSubsidiary.LWB) {
                        (if (language == "en") " - LWB" else " - 龍運").asFormattedText(SmallSize)
                    } else {
                        (if (language == "en") " - KMB" else " - 九巴").asFormattedText(SmallSize)
                    }
                } else {
                    (if (language == "en") " - CTB" else " - 城巴").asFormattedText(SmallSize)
                }
                val seq = ++counter
                if (seq == 1) {
                    nextCo = entryCo
                }
                lines[seq] = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, operatorMessage, remarkMessage), toShortText(language, minsRounded, 0), route.routeNumber, mins, minsRounded)
            }
        }
        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, nextCo, lines)
    }

    private suspend fun etaQueryKmb(typhoonInfo: TyphoonInfo, rawStopId: String, stopIndex: Int, co: Operator, route: Route, context: AppContext): ETAQueryResult {
        val now = currentTimeMillis()
        val allStops = getAllStops(route.routeNumber, route.idBound(co), co, route.gmbRegion, false)
        val stopData = allStops.first { it.stopId == rawStopId }
        val sameStops = allStops
            .asSequence()
            .filter { it.stop.name == stopData.stop.name && it.stop.location.distance(stopData.stop.location) < 0.1 }
            .distinctBy(
                selector = { it.branchIds },
                equalityPredicate = { a, b -> a.intersect(b).isNotEmpty() }
            )
            .groupBy { it.stopId }
        val stopIds = if (sameStops.size > 1) {
            sameStops.asSequence()
                .map { (k, v) -> k to if (k == rawStopId) "" else (v.asSequence()
                    .flatMap { it.branchIds.asSequence() }
                    .map { b -> b.resolveSpecialRemark(context)[Shared.language] }
                    .filter { r -> r.isNotEmpty() }
                    .distinct()
                    .joinToString(separator = "/")
                    .takeIf { r -> r.isNotEmpty() }
                    ?: "")
                }
                .toList()
        } else {
            listOf(element = rawStopId to "")
        }
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        val isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
        val unsortedLines: MutableList<ETALineEntry> = mutableListOf()
        var suspendedMessage: ETALineEntry? = null
        for ((stopId, branchRemark) in stopIds) {
            val special = branchRemark.isNotEmpty()
            val data = getJSONResponse<JsonObject>("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/$stopId?t=$now")
            debugLog(data)
            val buses = data!!.optJsonArray("data")!!
            val stopSequences: MutableSet<Int> = HashSet()
            for (u in 0 until buses.size) {
                val bus = buses.optJsonObject(u)!!
                if (Operator.KMB === Operator.valueOf(bus.optString("co"))) {
                    val routeNumber = bus.optString("route")
                    val bound = bus.optString("dir")
                    if (routeNumber == route.routeNumber && bound == route.bound[Operator.KMB]) {
                        stopSequences.add(bus.optInt("seq"))
                    }
                }
            }
            val matchingSeq = stopSequences.minByOrNull { (it - stopIndex).absoluteValue }?: -1
            val usedRealSeq: MutableSet<Int> = HashSet()
            for (u in 0 until buses.size) {
                val bus = buses.optJsonObject(u)!!
                if (Operator.KMB === Operator.valueOf(bus.optString("co"))) {
                    val routeNumber = bus.optString("route")
                    val bound = bus.optString("dir")
                    val stopSeq = bus.optInt("seq")
                    if (routeNumber == route.routeNumber && bound == route.bound[Operator.KMB] && stopSeq == matchingSeq) {
                        if (usedRealSeq.add(bus.optInt("eta_seq"))) {
                            val eta = bus.optString("eta")
                            val mins = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) Double.NEGATIVE_INFINITY else (eta.parseInstant().epochSeconds - currentEpochSeconds) / 60.0
                            if (mins.isFinite() && mins < -10) continue
                            val minsRounded = mins.roundToInt()
                            var timeMessage = "".asFormattedText()
                            var remarkMessage = "".asFormattedText()
                            if (language == "en") {
                                if (minsRounded > 0) {
                                    timeMessage = buildFormattedString {
                                        append(minsRounded.toString(), BoldStyle)
                                        append(" Min.", SmallSize)
                                    }
                                } else if (minsRounded > -60) {
                                    timeMessage = buildFormattedString {
                                        append("-", BoldStyle)
                                        append(" Min.", SmallSize)
                                    }
                                }
                                if (bus.optString("rmk_en").isNotEmpty()) {
                                    remarkMessage += buildFormattedString {
                                        if (timeMessage.isEmpty()) {
                                            append(bus.optString("rmk_en"))
                                        } else {
                                            if (branchRemark.isNotEmpty()) {
                                                append(" - ", SmallSize)
                                                append(branchRemark, SmallSize, BoldStyle)
                                            }
                                            append(" (${bus.optString("rmk_en")})", SmallSize)
                                        }
                                    }
                                } else if (branchRemark.isNotEmpty()) {
                                    remarkMessage += buildFormattedString {
                                        append(" - ", SmallSize)
                                        append(branchRemark, SmallSize, BoldStyle)
                                    }
                                }
                            } else {
                                if (minsRounded > 0) {
                                    timeMessage = buildFormattedString {
                                        append(minsRounded.toString(), BoldStyle)
                                        append(" 分鐘", SmallSize)
                                    }
                                } else if (minsRounded > -60) {
                                    timeMessage = buildFormattedString {
                                        append("-", BoldStyle)
                                        append(" 分鐘", SmallSize)
                                    }
                                }
                                if (bus.optString("rmk_tc").isNotEmpty()) {
                                    remarkMessage += buildFormattedString {
                                        if (timeMessage.isEmpty()) {
                                            append(bus.optString("rmk_tc")
                                                .replace("原定", "預定")
                                                .replace("最後班次", "尾班車")
                                                .replace("尾班車已過", "尾班車已過本站"))
                                        } else {
                                            if (branchRemark.isNotEmpty()) {
                                                append(" - ", SmallSize)
                                                append(branchRemark, SmallSize, BoldStyle)
                                            }
                                            append(" (${bus.optString("rmk_tc")
                                                .replace("原定", "預定")
                                                .replace("最後班次", "尾班車")
                                                .replace("尾班車已過", "尾班車已過本站")})", SmallSize)
                                        }
                                    }
                                } else if (branchRemark.isNotEmpty()) {
                                    remarkMessage += buildFormattedString {
                                        append(" - ", SmallSize)
                                        append(branchRemark, SmallSize, BoldStyle)
                                    }
                                }
                            }
                            if ((timeMessage.isEmpty() && remarkMessage.isEmpty()) || typhoonInfo.isAboveTyphoonSignalEight && (remarkMessage strEq "ETA service suspended" || remarkMessage strEq "暫停預報")) {
                                if (!special) {
                                    remarkMessage = getNoScheduledDepartureMessage(language, remarkMessage, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                    suspendedMessage = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, remarkMessage), toShortText(language, minsRounded.toLong(), 0), routeNumber, mins, minsRounded.toLong())
                                }
                            } else {
                                remarkMessage = "".asFormattedText(BoldStyle) + remarkMessage
                                val entry = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, remarkMessage), toShortText(language, minsRounded.toLong(), 0, special), routeNumber, mins, minsRounded.toLong())
                                if (entry.eta >= 0 || stopId == rawStopId) {
                                    unsortedLines.add(entry)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (unsortedLines.isEmpty()) {
            if (suspendedMessage != null) {
                lines[1] = suspendedMessage
            }
        } else {
            unsortedLines.asSequence().sortedBy { it.eta.takeIf { d -> d >= 0 }?: Double.POSITIVE_INFINITY }.forEachIndexed { i, entry ->
                lines[i + 1] = entry
            }
        }

        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    private suspend fun etaQueryCtb(typhoonInfo: TyphoonInfo, stopId: String, stopIndex: Int, co: Operator, route: Route): ETAQueryResult {
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        val isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
        val routeNumber = route.routeNumber
        val routeBound = route.bound[Operator.CTB]
        val data = getJSONResponse<JsonObject>("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/$stopId/$routeNumber")
        val buses = data!!.optJsonArray("data")!!
        val stopSequences: MutableSet<Int> = HashSet()
        for (u in 0 until buses.size) {
            val bus = buses.optJsonObject(u)!!
            if (Operator.CTB === Operator.valueOf(bus.optString("co"))) {
                val bound = bus.optString("dir")
                if (routeNumber == bus.optString("route") && (routeBound!!.length > 1 || bound == routeBound)) {
                    stopSequences.add(bus.optInt("seq"))
                }
            }
        }
        val matchingSeq = stopSequences.minByOrNull { (it - stopIndex).absoluteValue }?: -1
        var counter = 0
        val usedRealSeq: MutableSet<Int> = HashSet()
        for (bus in buses.asSequence().map { it.jsonObject }.sortedBy { it.optInt("eta_seq", Int.MAX_VALUE) }) {
            if (Operator.CTB === Operator.valueOf(bus.optString("co"))) {
                val bound = bus.optString("dir")
                val stopSeq = bus.optInt("seq")
                if (routeNumber == bus.optString("route") && (routeBound!!.length > 1 || bound == routeBound) && stopSeq == matchingSeq) {
                    if (usedRealSeq.add(bus.optInt("eta_seq"))) {
                        val eta = bus.optString("eta")
                        val mins = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) Double.NEGATIVE_INFINITY else (eta.parseInstant().epochSeconds - currentEpochSeconds) / 60.0
                        if (mins.isFinite() && mins < -10.0) continue
                        val seq = ++counter
                        val minsRounded = mins.roundToInt()
                        var timeMessage = "".asFormattedText()
                        var remarkMessage = "".asFormattedText()
                        if (language == "en") {
                            if (minsRounded > 0) {
                                timeMessage = buildFormattedString {
                                    append(minsRounded.toString(), BoldStyle)
                                    append(" Min.", SmallSize)
                                }
                            } else if (minsRounded > -60) {
                                timeMessage = buildFormattedString {
                                    append("-", BoldStyle)
                                    append(" Min.", SmallSize)
                                }
                            }
                            if (bus.optString("rmk_en").isNotEmpty()) {
                                remarkMessage += buildFormattedString {
                                    if (timeMessage.isEmpty()) {
                                        append(bus.optString("rmk_en"))
                                    } else {
                                        append(" (${bus.optString("rmk_en")})", SmallSize)
                                    }
                                }
                            }
                        } else {
                            if (minsRounded > 0) {
                                timeMessage = buildFormattedString {
                                    append(minsRounded.toString(), BoldStyle)
                                    append(" 分鐘", SmallSize)
                                }
                            } else if (minsRounded > -60) {
                                timeMessage = buildFormattedString {
                                    append("-", BoldStyle)
                                    append(" 分鐘", SmallSize)
                                }
                            }
                            if (bus.optString("rmk_tc").isNotEmpty()) {
                                remarkMessage += buildFormattedString {
                                    if (timeMessage.isEmpty()) {
                                        append(bus.optString("rmk_tc")
                                            .replace("原定", "預定")
                                            .replace("最後班次", "尾班車")
                                            .replace("尾班車已過", "尾班車已過本站"))
                                    } else {
                                        append(" (${bus.optString("rmk_tc")
                                            .replace("原定", "預定")
                                            .replace("最後班次", "尾班車")
                                            .replace("尾班車已過", "尾班車已過本站")})", SmallSize)
                                    }
                                }
                            }
                        }
                        remarkMessage = if (timeMessage.isEmpty() && remarkMessage.isEmpty()) {
                            if (seq == 1) {
                                getNoScheduledDepartureMessage(language, remarkMessage, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                            } else {
                                buildFormattedString {
                                    append("", BoldStyle)
                                    append("-")
                                }
                            }
                        } else {
                            "".asFormattedText(BoldStyle) + remarkMessage
                        }
                        lines[seq] = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, remarkMessage), toShortText(language, minsRounded.toLong(), 0), routeNumber, mins, minsRounded.toLong())
                    }
                }
            }
        }
       return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    private suspend fun etaQueryNlb(typhoonInfo: TyphoonInfo, stopId: String, co: Operator, route: Route): ETAQueryResult {
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        val isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
        val data = getJSONResponse<JsonObject>("https://rt.data.gov.hk/v2/transport/nlb/stop.php?action=estimatedArrivals&routeId=${route.nlbId}&stopId=$stopId&language=${Shared.language}")
        if (data.isNotNullAndNotEmpty() && data.contains("estimatedArrivals")) {
            val buses = data.optJsonArray("estimatedArrivals")!!
            var counter = 0
            for (u in 0 until buses.size) {
                val bus = buses.optJsonObject(u)!!
                val eta = bus.optString("estimatedArrivalTime")
                val variant = bus.optString("routeVariantName").trim { it <= ' ' }
                val mins = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) Double.NEGATIVE_INFINITY else (eta.let { "${it.substring(0, 10)}T${it.substring(11)}" }.parseLocalDateTime().toInstant(hongKongTimeZone).epochSeconds - currentEpochSeconds) / 60.0
                if (mins.isFinite() && mins < -10.0) continue
                val seq = ++counter
                val minsRounded = mins.roundToInt()
                var timeMessage = "".asFormattedText()
                var remarkMessage = "".asFormattedText()
                if (language == "en") {
                    if (minsRounded > 0) {
                        timeMessage = buildFormattedString {
                            append(minsRounded.toString(), BoldStyle)
                            append(" Min.", SmallSize)
                        }
                    } else if (minsRounded > -60) {
                        timeMessage = buildFormattedString {
                            append("-", BoldStyle)
                            append(" Min.", SmallSize)
                        }
                    }
                } else {
                    if (minsRounded > 0) {
                        timeMessage = buildFormattedString {
                            append(minsRounded.toString(), BoldStyle)
                            append(" 分鐘", SmallSize)
                        }
                    } else if (minsRounded > -60) {
                        timeMessage = buildFormattedString {
                            append("-", BoldStyle)
                            append(" 分鐘", SmallSize)
                        }
                    }
                }
                if (variant.isNotEmpty()) {
                    remarkMessage += buildFormattedString {
                        if (timeMessage.isEmpty()) {
                            append(variant
                                .replace("原定", "預定")
                                .replace("最後班次", "尾班車")
                                .replace("尾班車已過", "尾班車已過本站"))
                        } else {
                            append(" (${variant
                                .replace("原定", "預定")
                                .replace("最後班次", "尾班車")
                                .replace("尾班車已過", "尾班車已過本站")})", SmallSize)
                        }
                    }
                }
                remarkMessage = if (timeMessage.isEmpty() && remarkMessage.isEmpty()) {
                    if (seq == 1) {
                        getNoScheduledDepartureMessage(language, remarkMessage, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                    } else {
                        buildFormattedString {
                            append("", BoldStyle)
                            append("-")
                        }
                    }
                } else {
                    "".asFormattedText(BoldStyle) + remarkMessage
                }
                lines[seq] = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, remarkMessage), toShortText(language, minsRounded.toLong(), 0), route.routeNumber, mins, minsRounded.toLong())
            }
        }
        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    private suspend fun etaQueryMtrBus(typhoonInfo: TyphoonInfo, stopId: String, co: Operator, route: Route): ETAQueryResult {
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        val isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
        val routeNumber = route.routeNumber
        val body = buildJsonObject {
            put("language", language)
            put("routeName", routeNumber)
        }
        val data = doRetry<JsonObject?>(
            block = { postJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/bus/getSchedule", body) },
            predicate = { it?.containsKeyAndNotNull("busStop") == true },
            maxTries = 3,
            limitReached = { throw RuntimeException() }
        )
        if (data!!.containsKeyAndNotNull("routeStatusRemarkTitle")) {
            val status = data.optString("routeStatusRemarkTitle")
            lines[1] = ETALineEntry.textEntry(status)
        }
        val busStops = data.optJsonArray("busStop")
        if (busStops != null) {
            var counter = 0
            for (k in 0 until busStops.size) {
                val busStop = busStops.optJsonObject(k)!!
                val buses = busStop.optJsonArray("bus")!!
                val busStopId = busStop.optString("busStopId")
                for (u in 0 until buses.size) {
                    val bus = buses.optJsonObject(u)!!
                    var eta = bus.optDouble("arrivalTimeInSecond")
                    if (eta >= 108000) {
                        eta = bus.optDouble("departureTimeInSecond")
                    }
                    var remark = bus.optString("busRemark")
                    if (remark.isEmpty() || remark.equals("null", ignoreCase = true)) {
                        remark = ""
                    }
                    val isScheduled = bus.optString("isScheduled") == "1"
                    if (isScheduled) {
                        if (remark.isNotEmpty()) {
                            remark += "/"
                        }
                        remark += if (language == "en") "Scheduled Bus" else "預定班次"
                    }
                    val isDelayed = bus.optString("isDelayed") == "1"
                    if (isDelayed) {
                        if (remark.isNotEmpty()) {
                            remark += "/"
                        }
                        remark += if (language == "en") "Bus Delayed" else "行車緩慢"
                    }
                    val mins = eta / 60.0
                    if (mins.isFinite() && mins < -10.0) continue
                    val minsRounded = floor(mins).toLong()
                    if (DATA!!.mtrBusStopAlias[stopId]!!.contains(busStopId)) {
                        val seq = ++counter
                        var timeMessage = "".asFormattedText()
                        var remarkMessage = "".asFormattedText()
                        if (language == "en") {
                            if (minsRounded > 0) {
                                timeMessage = buildFormattedString {
                                    append(minsRounded.toString(), BoldStyle)
                                    append(" Min.", SmallSize)
                                }
                            } else if (minsRounded > -60) {
                                timeMessage = buildFormattedString {
                                    append("-", BoldStyle)
                                    append(" Min.", SmallSize)
                                }
                            }
                        } else {
                            if (minsRounded > 0) {
                                timeMessage = buildFormattedString {
                                    append(minsRounded.toString(), BoldStyle)
                                    append(" 分鐘", SmallSize)
                                }
                            } else if (minsRounded > -60) {
                                timeMessage = buildFormattedString {
                                    append("-", BoldStyle)
                                    append(" 分鐘", SmallSize)
                                }
                            }
                        }
                        if (remark.isNotEmpty()) {
                            remarkMessage += buildFormattedString {
                                if (timeMessage.isEmpty()) {
                                    append(remark
                                        .replace("原定", "預定")
                                        .replace("最後班次", "尾班車")
                                        .replace("尾班車已過", "尾班車已過本站"))
                                } else {
                                    append(" (${remark
                                        .replace("原定", "預定")
                                        .replace("最後班次", "尾班車")
                                        .replace("尾班車已過", "尾班車已過本站")})", SmallSize)
                                }
                            }
                        }
                        remarkMessage = if (timeMessage.isEmpty() && remarkMessage.isEmpty()) {
                            if (seq == 1) {
                                getNoScheduledDepartureMessage(language, remarkMessage, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                            } else {
                                buildFormattedString {
                                    append("", BoldStyle)
                                    append("-")
                                }
                            }
                        } else {
                            "".asFormattedText(BoldStyle) + remarkMessage
                        }
                        lines[seq] = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, remarkMessage), toShortText(language, minsRounded, 0), routeNumber, mins, minsRounded)
                    }
                }
            }
        }
        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    @Immutable
    data class GMBETAEntry(
        val stopSeq: Int,
        val mins: Double,
        val jsonObject: JsonObject,
        val branch: Route
    )

    private suspend fun etaQueryGmb(typhoonInfo: TyphoonInfo, stopId: String, stopIndex: Int, co: Operator, route: Route, context: AppContext): ETAQueryResult {
        val allStops = getAllStops(route.routeNumber, route.idBound(Operator.GMB), Operator.GMB, route.gmbRegion)
        val branches = allStops.indexesOf { it.stopId == stopId }
            .minByOrNull { (it - stopIndex).absoluteValue }
            ?.let { allStops[it].branchIds.associateBy { b -> b.gtfsId } }
            ?: mapOf(route.gtfsId to route)
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        val isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
        val data = getJSONResponse<JsonObject>("https://data.etagmb.gov.hk/eta/stop/$stopId")
        val stopSequences: MutableMap<String, MutableSet<Int>> = mutableMapOf()
        val busList: MutableList<GMBETAEntry> = mutableListOf()
        for (i in 0 until data!!.optJsonArray("data")!!.size) {
            val routeData = data.optJsonArray("data")!!.optJsonObject(i)!!
            val buses = routeData.optJsonArray("eta")
            val bound = if (routeData.optInt("route_seq") <= 1) "O" else "I"
            val routeId = routeData.optString("route_id")
            val branch = branches[routeId]
            if (route.bound[co] == bound && branch != null && buses != null) {
                val routeNumber = route.routeNumber
                val stopSeq = routeData.optInt("stop_seq")
                for (u in 0 until buses.size) {
                    val bus = buses.optJsonObject(u)!!
                    if (routeNumber == route.routeNumber) {
                        val eta = bus.optString("timestamp")
                        val mins = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) Double.NEGATIVE_INFINITY else (eta.parseInstant().epochSeconds - currentEpochSeconds) / 60.0
                        stopSequences.getOrPut(branch.gtfsId) { mutableSetOf() }.add(stopSeq)
                        busList.add(GMBETAEntry(stopSeq, mins, bus, branch))
                    }
                }
            }
        }
        for ((r, s) in stopSequences) {
            if (s.size > 1) {
                val matchingSeq = s.minByOrNull { (it - stopIndex).absoluteValue }?: -1
                busList.removeAll { it.branch.gtfsId == r && it.stopSeq != matchingSeq }
            }
        }
        val sortedBusList = busList.asSequence()
            .sortedBy { it.mins }
            .distinctBy({ it }, { (_, m1, _, b1), (_, m2, _, b2) -> (m1 - m2).absoluteValue < 0.33 && b1 != b2 })
        var counter = 0
        for ((_, mins, bus) in sortedBusList) {
            if (mins.isFinite() && mins < -10.0) continue
            val seq = ++counter
            var remark = if (language == "en") bus.optString("remarks_en") else bus.optString("remarks_tc")
            if (remark.equals("null", ignoreCase = true)) {
                remark = ""
            }
            val minsRounded = mins.roundToInt()
            var timeMessage = "".asFormattedText()
            var remarkMessage = "".asFormattedText()
            if (language == "en") {
                if (minsRounded > 0) {
                    timeMessage = buildFormattedString {
                        append(minsRounded.toString(), BoldStyle)
                        append(" Min.", SmallSize)
                    }
                } else if (minsRounded > -60) {
                    timeMessage = buildFormattedString {
                        append("-", BoldStyle)
                        append(" Min.", SmallSize)
                    }
                }
            } else {
                if (minsRounded > 0) {
                    timeMessage = buildFormattedString {
                        append(minsRounded.toString(), BoldStyle)
                        append(" 分鐘", SmallSize)
                    }
                } else if (minsRounded > -60) {
                    timeMessage = buildFormattedString {
                        append("-", BoldStyle)
                        append(" 分鐘", SmallSize)
                    }
                }
            }
            if (remark.isNotEmpty()) {
                remarkMessage += buildFormattedString {
                    if (timeMessage.isEmpty()) {
                        append(remark
                            .replace("原定", "預定")
                            .replace("最後班次", "尾班車")
                            .replace("尾班車已過", "尾班車已過本站"))
                    } else {
                        append(" (${remark
                            .replace("原定", "預定")
                            .replace("最後班次", "尾班車")
                            .replace("尾班車已過", "尾班車已過本站")})", SmallSize)
                    }
                }
            }
            remarkMessage = if (timeMessage.isEmpty() && remarkMessage.isEmpty()) {
                if (seq == 1) {
                    getNoScheduledDepartureMessage(language, remarkMessage, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                } else {
                    buildFormattedString {
                        append("", BoldStyle)
                        append("-")
                    }
                }
            } else {
                "".asFormattedText(BoldStyle) + remarkMessage
            }
            lines[seq] = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, remarkMessage), toShortText(language, minsRounded.toLong(), 0), route.routeNumber, mins, minsRounded.toLong())
        }
        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    private suspend fun etaQueryLrt(typhoonInfo: TyphoonInfo, stopId: String, stopIndex: Int, co: Operator, route: Route, options: EtaQueryOptions?): ETAQueryResult {
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        var isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine
        val lrtDirectionMode = options?.lrtDirectionMode == true
        val lrtAllMode = options?.lrtAllMode == true
        val stopsList = route.stops[Operator.LRT]!!
        if (stopsList.indexOf(stopId) + 1 >= stopsList.size) {
            isMtrEndOfLine = true
            lines[1] = ETALineEntry.textEntry(route.endOfLineText[language])
        } else {
            val hongKongTime = Clock.System.now().toLocalDateTime(hongKongTimeZone)
            val hour = hongKongTime.hour
            val results: MutableList<LrtETAData> = mutableListOf()
            val data = getJSONResponse<JsonObject>("https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=${stopId.substring(2)}")
            if (data!!.optInt("status") != 0) {
                val platformList = data.optJsonArray("platform_list")!!
                if (!lrtAllMode) {
                    val matchRoutes = if (lrtDirectionMode) getLrtSameDirectionRoutes(stopId, stopIndex, route) else setOf(route)
                    for (i in 0 until platformList.size) {
                        val platform = platformList.optJsonObject(i)!!
                        val platformNumber = platform.optInt("platform_id")
                        val routeList = platform.optJsonArray("route_list")
                        if (routeList != null) {
                            for (u in 0 until routeList.size) {
                                val routeData = routeList.optJsonObject(u)!!
                                val routeNumber = routeData.optString("route_no")
                                if (routeData.contains("time_ch")) {
                                    val destCh = routeData.optString("dest_ch")
                                    if (matchRoutes.any { routeNumber == it.routeNumber && isLrtStopOnOrAfter(stopId, destCh, it) }) {
                                        val mins = "([0-9]+) *min".toRegex().find(routeData.optString("time_en"))?.groupValues?.getOrNull(1)?.toLong()?: 0
                                        val minsMsg = routeData.optString(if (language == "en") "time_en" else "time_ch")
                                        val dest = routeData.optString(if (language == "en") "dest_en" else "dest_ch")
                                        val trainLength = routeData.optInt("train_length")
                                        results.add(LrtETAData(routeNumber, dest, trainLength, platformNumber, mins, minsMsg))
                                    }
                                } else if (results.none { it.routeNumber == routeNumber } && matchRoutes.any { routeNumber == it.routeNumber }) {
                                    val message = if (language == "en") "Server unable to provide data" else "系統未能提供資訊"
                                    results.add(LrtETAData(routeNumber, "", 0, platformNumber, Long.MAX_VALUE, message))
                                }
                            }
                        }
                    }
                } else {
                    for (i in 0 until platformList.size) {
                        val platform = platformList.optJsonObject(i)!!
                        val platformNumber = platform.optInt("platform_id")
                        val routeList = platform.optJsonArray("route_list")
                        if (routeList != null) {
                            for (u in 0 until routeList.size) {
                                val routeData = routeList.optJsonObject(u)!!
                                val routeNumber = routeData.optString("route_no")
                                if (routeData.contains("time_ch")) {
                                    val mins = "([0-9]+) *min".toRegex().find(routeData.optString("time_en"))?.groupValues?.getOrNull(1)?.toLong()?: 0
                                    val minsMsg = routeData.optString(if (language == "en") "time_en" else "time_ch")
                                    val dest = routeData.optString(if (language == "en") "dest_en" else "dest_ch")
                                    val trainLength = routeData.optInt("train_length")
                                    results.add(LrtETAData(routeNumber, dest, trainLength, platformNumber, mins, minsMsg))
                                } else if (results.none { it.routeNumber == routeNumber }) {
                                    val message = if (language == "en") "Server unable to provide data" else "系統未能提供資訊"
                                    results.add(LrtETAData(routeNumber, "", 0, platformNumber, Long.MAX_VALUE, message))
                                }
                            }
                        }
                    }
                }
            }
            if (results.isEmpty()) {
                if (hour < 3) {
                    lines[1] = ETALineEntry.textEntry(if (language == "en") "Last train has departed" else "尾班車已開出")
                } else if (hour < 6) {
                    lines[1] = ETALineEntry.textEntry(if (language == "en") "Service has not yet started" else "今日服務尚未開始")
                } else {
                    lines[1] = ETALineEntry.textEntry(if (language == "en") "Server unable to provide data" else "系統未能提供資訊")
                }
            } else {
                val lineColor = co.getColor(route.routeNumber, 0xFFFFFFFFL)
                results.sortWith(naturalOrder())
                for (i in results.indices) {
                    val lrt = results[i]
                    val seq = i + 1
                    var minsMessage = lrt.etaMessage
                    if (minsMessage == "-") {
                        minsMessage = if (language == "en") "Departing" else "正在離開"
                    }
                    val annotatedMinsMessage = if (lrt.eta == Long.MAX_VALUE) {
                        minsMessage.asFormattedText()
                    } else if (minsMessage == "即將抵達" || minsMessage == "Arriving" || minsMessage == "正在離開" || minsMessage == "Departing") {
                        minsMessage.asFormattedText(BoldStyle)
                    } else {
                        val mins = "^([0-9]+)".toRegex().find(minsMessage)?.groupValues?.getOrNull(1)?.toLong()
                        if (mins == null) {
                            minsMessage.asFormattedText(BoldStyle)
                        } else {
                            buildFormattedString {
                                append(mins.toString(), BoldStyle)
                                append(if (language == "en") " Min." else " 分鐘", SmallSize)
                            }
                        }
                    }
                    val mins = lrt.eta
                    val message = when {
                        lrtDirectionMode -> ETALineEntryText.lrt(
                            platform = buildFormattedString {
                                append("", BoldStyle)
                                append(lrt.platformNumber.getCircledNumber(), Colored(lineColor))
                                append(" ")
                            },
                            routeNumber = buildFormattedString {
                                append("${lrt.routeNumber} ", SmallSize)
                            },
                            destination = buildFormattedString {
                                append("${lrt.dest}  ", SmallSize)
                            },
                            carts = buildFormattedString {
                                for (u in 0 until lrt.trainLength) {
                                    appendInlineContent(InlineImage.LRV, "\uD83D\uDE83")
                                }
                                if (lrt.trainLength == 1) {
                                    appendInlineContent(InlineImage.LRV_EMPTY, EMSP)
                                }
                                append(EMSP)
                            },
                            time = annotatedMinsMessage,
                            remark = "".asFormattedText()
                        )
                        lrtAllMode -> ETALineEntryText.lrt(
                            platform = buildFormattedString {
                                append("", BoldStyle)
                                append(lrt.platformNumber.getCircledNumber(), Colored(lineColor))
                                append(" ")
                            },
                            routeNumber = buildFormattedString {
                                append("${lrt.routeNumber} ")
                            },
                            destination = buildFormattedString {
                                append("${lrt.dest}  ")
                            },
                            carts = buildFormattedString {
                                for (u in 0 until lrt.trainLength) {
                                    appendInlineContent(InlineImage.LRV, "\uD83D\uDE83")
                                }
                                if (lrt.trainLength == 1) {
                                    appendInlineContent(InlineImage.LRV_EMPTY, EMSP)
                                }
                                append(EMSP)
                            },
                            time = annotatedMinsMessage,
                            remark = "".asFormattedText()
                        )
                        else -> ETALineEntryText.lrt(
                            platform = buildFormattedString {
                                append("", BoldStyle)
                                append(lrt.platformNumber.getCircledNumber(), Colored(lineColor))
                                append(NQSP)
                            },
                            carts = buildFormattedString {
                                for (u in 0 until lrt.trainLength) {
                                    appendInlineContent(InlineImage.LRV, "\uD83D\uDE83")
                                }
                                if (lrt.trainLength == 1) {
                                    appendInlineContent(InlineImage.LRV_EMPTY, EMSP)
                                }
                                append("  ")
                            },
                            time = annotatedMinsMessage,
                            remark = "".asFormattedText()
                        )
                    }
                    lines[seq] = ETALineEntry.etaEntry(message, toShortText(language, mins, 1), lrt.platformNumber, lrt.routeNumber, mins.toDouble(), mins)
                }
            }
        }
        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    private suspend fun etaQueryMtr(typhoonInfo: TyphoonInfo, stopId: String, co: Operator, route: Route): ETAQueryResult {
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        var isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine
        val lineName = route.routeNumber
        val lineColor = co.getColor(lineName, 0xFFFFFFFF)
        val bound = route.bound[Operator.MTR]
        if (isMtrStopEndOfLine(stopId, lineName, bound)) {
            isMtrEndOfLine = true
            lines[1] = ETALineEntry.textEntry(route.endOfLineText[language])
        } else {
            val hongKongTime = Clock.System.now().toLocalDateTime(hongKongTimeZone)
            val hour = hongKongTime.hour
            val dayOfWeek = hongKongTime.dayOfWeek
            val data = getJSONResponse<JsonObject>("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=$lineName&sta=$stopId")
            if (data!!.optInt("status") == 0) {
                lines[1] = ETALineEntry.textEntry(if (language == "en") "Server unable to provide data" else "系統未能提供資訊")
            } else {
                val lineStops = data.optJsonObject("data")!!.optJsonObject("$lineName-$stopId")
                val raceDay = dayOfWeek == DayOfWeek.WEDNESDAY || dayOfWeek == DayOfWeek.SUNDAY
                if (lineStops == null) {
                    if (stopId == "RAC") {
                        if (!raceDay) {
                            lines[1] = ETALineEntry.textEntry(if (language == "en") "Service on race days only" else "僅在賽馬日提供服務")
                        } else if (hour >= 15 || hour < 3) {
                            lines[1] = ETALineEntry.textEntry(if (language == "en") "Last train has departed" else "尾班車已開出")
                        } else {
                            lines[1] = ETALineEntry.textEntry(if (language == "en") "Service has not yet started" else "今日服務尚未開始")
                        }
                    } else if (hour < 3 || stopId == "LMC" && hour >= 22 || stopId == "SHS" && hour >= 23) {
                        lines[1] = ETALineEntry.textEntry(if (language == "en") "Last train has departed" else "尾班車已開出")
                    } else if (hour < 6) {
                        lines[1] = ETALineEntry.textEntry(if (language == "en") "Service has not yet started" else "今日服務尚未開始")
                    } else {
                        lines[1] = ETALineEntry.textEntry(if (language == "en") "Server unable to provide data" else "系統未能提供資訊")
                    }
                } else {
                    val delayed = data.optString("isdelay", "N") != "N"
                    val dir = if (bound == "UT") "UP" else "DOWN"
                    val trains = lineStops.optJsonArray(dir)
                    if (trains.isNullOrEmpty()) {
                        if (stopId == "RAC") {
                            if (!raceDay) {
                                lines[1] = ETALineEntry.textEntry(if (language == "en") "Service on race days only" else "僅在賽馬日提供服務")
                            } else if (hour >= 15 || hour < 3) {
                                lines[1] = ETALineEntry.textEntry(if (language == "en") "Last train has departed" else "尾班車已開出")
                            } else {
                                lines[1] = ETALineEntry.textEntry(if (language == "en") "Service has not yet started" else "今日服務尚未開始")
                            }
                        } else if (hour < 3 || stopId == "LMC" && hour >= 22 || stopId == "SHS" && hour >= 23) {
                            lines[1] = ETALineEntry.textEntry(if (language == "en") "Last train has departed" else "尾班車已開出")
                        } else if (hour < 6) {
                            lines[1] = ETALineEntry.textEntry(if (language == "en") "Service has not yet started" else "今日服務尚未開始")
                        } else {
                            lines[1] = ETALineEntry.textEntry(if (language == "en") "Server unable to provide data" else "系統未能提供資訊")
                        }
                    } else {
                        for (u in 0 until trains.size) {
                            val trainData = trains.optJsonObject(u)!!
                            val seq = trainData.optString("seq").toInt()
                            val platform = trainData.optString("plat").toInt()
                            val specialRoute = trainData.optString("route")
                            var dest: String = DATA!!.dataSheet.stopList[trainData.optString("dest")]!!.name[language]
                            if (stopId != "AIR") {
                                if (dest == "博覽館") {
                                    dest = "機場及博覽館"
                                } else if (dest == "AsiaWorld-Expo") {
                                    dest = "Airport & AsiaWorld-Expo"
                                }
                            }
                            var annotatedDest = dest.asFormattedText()
                            if (specialRoute.isNotEmpty() && !isMtrStopOnOrAfter(stopId, specialRoute, lineName, bound)) {
                                val via: String = DATA!!.dataSheet.stopList[specialRoute]!!.name[language]
                                annotatedDest += ((if (language == "en") " via " else " 經") + via).asFormattedText(SmallSize)
                            }
                            val timeType = trainData.optString("timeType")
                            val eta = trainData.optString("time").let { "${it.substring(0, 10)}T${it.substring(11)}" }
                            val mins = (eta.parseLocalDateTime().toInstant(hongKongTimeZone).toEpochMilliseconds() - currentTimeMillis()) / 60000.0
                            val minsRounded = mins.roundToInt()
                            val minsMessage = if (minsRounded > 59) {
                                val time = hongKongTime + minsRounded.minutes
                                "${time.hour.pad(2)}:${time.minute.pad(2)}".asFormattedText(BoldStyle)
                            } else if (minsRounded > 1) {
                                buildFormattedString {
                                    append(minsRounded.toString(), BoldStyle)
                                    append(if (language == "en") " Min." else " 分鐘", SmallSize)
                                }
                            } else if (minsRounded == 1 && timeType != "D") {
                                (if (language == "en") "Arriving" else "即將抵達").asFormattedText(BoldStyle)
                            } else {
                                (if (language == "en") "Departing" else "正在離開").asFormattedText(BoldStyle)
                            }
                            val message = ETALineEntryText.mtr(
                                platform = buildFormattedString {
                                    append("", BoldStyle)
                                    append(platform.getCircledNumber(), Colored(lineColor))
                                    append(" ")
                                },
                                destination = buildFormattedString {
                                    append(annotatedDest)
                                    append(EMSP)
                                },
                                time = minsMessage,
                                remark = if (seq == 1 && delayed) {
                                    (if (language == "en") " (Delayed)" else " (服務延誤)").asFormattedText(SmallSize)
                                } else {
                                    "".asFormattedText()
                                }
                            )
                            lines[seq] = ETALineEntry.etaEntry(message, toShortText(language, minsRounded.toLong(), 1), platform, route.routeNumber, mins, minsRounded.toLong())
                        }
                    }
                }
            }
        }
        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    private suspend fun etaQuerySunFerry(typhoonInfo: TyphoonInfo, stopId: String, co: Operator, route: Route): ETAQueryResult {
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        val isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
        val stops = route.stops[co]!!
        val timeKey = if (stops.indexOf(stopId) == 0) "depart_time" else "eta"
        val data = getJSONResponse<JsonObject>("https://www.sunferry.com.hk/eta/?route=${route.routeNumber}")!!.optJsonArray("data")!!
        val now = currentLocalDateTime() - 1.hours
        var i = 1
        for (element in data) {
            val entryData = element.jsonObject
            val time = entryData.optString(timeKey).takeIf { it.matches("[0-9]{2}:[0-9]{2}".toRegex()) }?.let {
                LocalTime(it.substring(0, 2).toInt(), it.substring(3, 5).toInt()).nextLocalDateTimeAfter(now)
            }
            val mins = if (time == null) Double.NEGATIVE_INFINITY else (time.epochSeconds - currentEpochSeconds) / 60.0
            val remark = entryData.optString("rmk_${if (language == "en") "en" else "tc"}").takeIf { it.isNotBlank() && !it.equals("null", true) }?: ""
            val minsRounded = mins.roundToInt()
            if (minsRounded < -5) continue
            val seq = i++
            var timeMessage = "".asFormattedText()
            var remarkMessage = "".asFormattedText()
            if (minsRounded > 59) {
                val clockTime = currentLocalDateTime() + minsRounded.minutes
                timeMessage = "${clockTime.hour.pad(2)}:${clockTime.minute.pad(2)}".asFormattedText(BoldStyle)
            } else if (language == "en") {
                if (minsRounded > 0) {
                    timeMessage = buildFormattedString {
                        append(minsRounded.toString(), BoldStyle)
                        append(" Min.", SmallSize)
                    }
                } else if (minsRounded > -60) {
                    timeMessage = buildFormattedString {
                        append("-", BoldStyle)
                        append(" Min.", SmallSize)
                    }
                }
            } else {
                if (minsRounded > 0) {
                    timeMessage = buildFormattedString {
                        append(minsRounded.toString(), BoldStyle)
                        append(" 分鐘", SmallSize)
                    }
                } else if (minsRounded > -60) {
                    timeMessage = buildFormattedString {
                        append("-", BoldStyle)
                        append(" 分鐘", SmallSize)
                    }
                }
            }
            if (remark.isNotEmpty()) {
                remarkMessage += buildFormattedString {
                    if (timeMessage.isEmpty()) {
                        append(remark)
                    } else {
                        append(" (${remark})", SmallSize)
                    }
                }
            }
            remarkMessage = if (timeMessage.isEmpty() && remarkMessage.isEmpty()) {
                if (seq == 1) {
                    getNoScheduledDepartureMessage(language, remarkMessage, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                } else {
                    buildFormattedString {
                        append("", BoldStyle)
                        append("-")
                    }
                }
            } else {
                "".asFormattedText(BoldStyle) + remarkMessage
            }
            lines[seq] = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, remarkMessage), toShortText(language, minsRounded.toLong(), 0), route.routeNumber, mins, minsRounded.toLong())
        }
        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    private suspend fun etaQueryHkkf(typhoonInfo: TyphoonInfo, stopId: String, co: Operator, route: Route): ETAQueryResult {
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        var isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
        val stops = route.stops[co]!!
        val index = stops.indexOf(stopId)
        if (index + 1 >= stops.size) {
            isMtrEndOfLine = true
        } else {
            lines[1] = ETALineEntry.textEntry(if (language == "en") "Check Timetable" else "查看時間表")
        }
        val routeId = route.routeNumber.substring(2, 3)
        val bound = if (route.bound[co] == "O") "outbound" else "inbound"
        val data = getJSONResponse<JsonObject>("https://hkkfeta.com/opendata/eta/$routeId/$bound")!!.optJsonArray("data")!!
        var i = 1
        for (element in data) {
            val entryData = element.jsonObject
            val eta = if (isMtrEndOfLine) entryData.optString("ETA") else "${entryData.optString("date")}T${entryData.optString("session_time")}+08:00"
            val time = if (eta.isNotEmpty() && !eta.contains("null", true)) eta.parseInstant().toLocalDateTime(hongKongTimeZone) else null
            val mins = if (time == null) Double.NEGATIVE_INFINITY else (time.epochSeconds - currentEpochSeconds) / 60.0
            val remark = entryData.optString("rmk_${if (language == "en") "en" else "tc"}").takeIf { it.isNotBlank() && !it.equals("null", true) }?: ""
            val minsRounded = mins.roundToInt()
            if (minsRounded < -5) continue
            val seq = i++
            var timeMessage = "".asFormattedText()
            var remarkMessage = "".asFormattedText()
            if (minsRounded > 59) {
                val clockTime = currentLocalDateTime() + minsRounded.minutes
                timeMessage = "${clockTime.hour.pad(2)}:${clockTime.minute.pad(2)}".asFormattedText(BoldStyle)
            } else if (language == "en") {
                if (minsRounded > 0) {
                    timeMessage = buildFormattedString {
                        append(minsRounded.toString(), BoldStyle)
                        append(" Min.", SmallSize)
                    }
                } else if (minsRounded > -60) {
                    timeMessage = buildFormattedString {
                        append("-", BoldStyle)
                        append(" Min.", SmallSize)
                    }
                }
            } else {
                if (minsRounded > 0) {
                    timeMessage = buildFormattedString {
                        append(minsRounded.toString(), BoldStyle)
                        append(" 分鐘", SmallSize)
                    }
                } else if (minsRounded > -60) {
                    timeMessage = buildFormattedString {
                        append("-", BoldStyle)
                        append(" 分鐘", SmallSize)
                    }
                }
            }
            if (remark.isNotEmpty()) {
                remarkMessage += buildFormattedString {
                    if (timeMessage.isEmpty()) {
                        append(remark)
                    } else {
                        append(" (${remark})", SmallSize)
                    }
                }
            } else if (time == null) {
                remarkMessage = (if (language == "en") "Check Timetable" else "查看時間表").asFormattedText()
            }
            remarkMessage = if (timeMessage.isEmpty() && remarkMessage.isEmpty()) {
                if (seq == 1) {
                    getNoScheduledDepartureMessage(language, remarkMessage, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                } else {
                    buildFormattedString {
                        append("", BoldStyle)
                        append("-")
                    }
                }
            } else {
                "".asFormattedText(BoldStyle) + remarkMessage
            }
            lines[seq] = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, remarkMessage), toShortText(language, minsRounded.toLong(), 0), route.routeNumber, mins, minsRounded.toLong())
        }
        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    private suspend fun etaQueryFortuneFerry(typhoonInfo: TyphoonInfo, stopId: String, co: Operator, route: Route): ETAQueryResult {
        val lines: MutableMap<Int, ETALineEntry> = mutableMapOf()
        var isMtrEndOfLine = false
        val language = Shared.language
        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(language, null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
        val isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
        val stops = route.stops[co]!!
        val index = stops.indexOf(stopId)
        if (index + 1 >= stops.size) {
            isMtrEndOfLine = true
            lines[1] = ETALineEntry.textEntry(route.endOfLineText[language])
        } else {
            val now = currentLocalDateTime() - 1.hours
            val stop = DATA!!.dataSheet.stopList[stops[index]]!!.hkkfStopCode
            val nextStop = DATA!!.dataSheet.stopList[stops[index + 1]]!!.hkkfStopCode
            val data = getJSONResponse<JsonObject>("https://www.hongkongwatertaxi.com.hk/eta/?route=${stop}${nextStop}")!!
            if (data.optString("generated_timestamp").takeIf { it.isNotBlank() && !it.equals("null", true) }?.parseInstant()?.toLocalDateTime(hongKongTimeZone)?.let { it < now } == true) {
                lines[1] = ETALineEntry.textEntry(if (language == "en") "Check Timetable" else "查看時間表")
            } else {
                var i = 1
                for (element in data.optJsonArray("data")!!) {
                    val entryData = element.jsonObject
                    val departTime = entryData.optString("depart_time").takeIf { it.matches("[0-9]{2}:[0-9]{2}".toRegex()) }?.let {
                        LocalTime(it.substring(0, 2).toInt(), it.substring(3, 5).toInt()).nextLocalDateTimeAfter(now)
                    }
                    val mins = if (departTime == null) Double.NEGATIVE_INFINITY else (departTime.epochSeconds - currentEpochSeconds) / 60.0
                    val remark = entryData.optString("rmk_${if (language == "en") "en" else "tc"}").takeIf { it.isNotBlank() && !it.equals("null", true) }?: ""
                    val minsRounded = mins.roundToInt()
                    if (minsRounded < -5) continue
                    val seq = i++
                    var timeMessage = "".asFormattedText()
                    var remarkMessage = "".asFormattedText()
                    if (minsRounded > 59) {
                        val clockTime = currentLocalDateTime() + minsRounded.minutes
                        timeMessage = "${clockTime.hour.pad(2)}:${clockTime.minute.pad(2)}".asFormattedText(BoldStyle)
                    } else if (language == "en") {
                        if (minsRounded > 0) {
                            timeMessage = buildFormattedString {
                                append(minsRounded.toString(), BoldStyle)
                                append(" Min.", SmallSize)
                            }
                        } else if (minsRounded > -60) {
                            timeMessage = buildFormattedString {
                                append("-", BoldStyle)
                                append(" Min.", SmallSize)
                            }
                        }
                    } else {
                        if (minsRounded > 0) {
                            timeMessage = buildFormattedString {
                                append(minsRounded.toString(), BoldStyle)
                                append(" 分鐘", SmallSize)
                            }
                        } else if (minsRounded > -60) {
                            timeMessage = buildFormattedString {
                                append("-", BoldStyle)
                                append(" 分鐘", SmallSize)
                            }
                        }
                    }
                    if (remark.isNotEmpty()) {
                        remarkMessage += buildFormattedString {
                            if (timeMessage.isEmpty()) {
                                append(remark)
                            } else {
                                append(" (${remark})", SmallSize)
                            }
                        }
                    }
                    remarkMessage = if (timeMessage.isEmpty() && remarkMessage.isEmpty()) {
                        if (seq == 1) {
                            getNoScheduledDepartureMessage(language, remarkMessage, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                        } else {
                            buildFormattedString {
                                append("", BoldStyle)
                                append("-")
                            }
                        }
                    } else {
                        "".asFormattedText(BoldStyle) + remarkMessage
                    }
                    lines[seq] = ETALineEntry.etaEntry(ETALineEntryText.bus(timeMessage, remarkMessage), toShortText(language, minsRounded.toLong(), 0), route.routeNumber, mins, minsRounded.toLong())
                }
            }
        }
        return ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, co, lines)
    }

    private fun toShortText(language: String, minsRounded: Long, arrivingThreshold: Long, special: Boolean = false): ETAShortText {
        return ETAShortText(
            first = if (minsRounded <= arrivingThreshold) "-" else minsRounded.toString(),
            second = if (language == "en") "Min." else "分鐘",
            special = special
        )
    }

    @Immutable
    data class JointOperatedEntry(
        val mins: Double,
        val minsRounded: Long,
        val time: FormattedText,
        val remark: FormattedText,
        val co: Operator
    ) : Comparable<JointOperatedEntry> {

        override operator fun compareTo(other: JointOperatedEntry): Int {
            return mins.compareTo(other.mins)
        }

    }

    @Immutable
    data class LrtETAData(
        val routeNumber: String,
        val dest: String,
        val trainLength: Int,
        val platformNumber: Int,
        val eta: Long,
        val etaMessage: String
    ) : Comparable<LrtETAData> {

        companion object {
            private val COMPARATOR = compareBy<LrtETAData>({ it.eta }, { it.platformNumber})
        }

        override operator fun compareTo(other: LrtETAData): Int {
            return COMPARATOR.compare(this, other)
        }

    }

    @Immutable
    class ETAQueryTask(errorResult: ETAQueryResult, task: suspend () -> ETAQueryResult): QueryTask<ETAQueryResult>(errorResult, task)

    @Immutable
    class ETALineEntryText private constructor(
        val clockTime: FormattedText = "".asFormattedText(),
        val platform: FormattedText = "".asFormattedText(),
        val routeNumber: FormattedText = "".asFormattedText(),
        val destination: FormattedText = "".asFormattedText(),
        val carts: FormattedText = "".asFormattedText(),
        val time: FormattedText,
        val operator: FormattedText = "".asFormattedText(),
        val remark: FormattedText = "".asFormattedText(),
        private val isTrain: Boolean = false
    ): FormattedText(if (isTrain) {
        platform + routeNumber + destination + carts + clockTime + time + remark
    } else {
        clockTime + platform + routeNumber + destination + carts + time + operator + remark
    }) {
        companion object {
            val EMPTY_NOTHING = ETALineEntryText(time = "".asFormattedText())
            val EMPTY = ETALineEntryText(time = "-".asFormattedText())
            fun remark(remark: FormattedText): ETALineEntryText {
                if (remark.isEmpty()) return EMPTY
                return ETALineEntryText(time = remark)
            }
            fun remark(remark: String): ETALineEntryText {
                if (remark.isEmpty()) return EMPTY
                return ETALineEntryText(time = remark.asFormattedText())
            }
            fun bus(time: FormattedText, remark: FormattedText): ETALineEntryText {
                if (time.isEmpty()) return remark(remark)
                return ETALineEntryText(time = time, remark = remark)
            }
            fun bus(time: FormattedText, operator: FormattedText, remark: FormattedText): ETALineEntryText {
                if (time.isEmpty()) return remark(remark)
                return ETALineEntryText(time = time, operator = operator, remark = remark)
            }
            fun lrt(platform: FormattedText, carts: FormattedText, time: FormattedText, remark: FormattedText): ETALineEntryText {
                if (time.isEmpty()) return remark(remark)
                return ETALineEntryText(platform = platform, carts = carts, time = time, remark = remark, isTrain = true)
            }
            fun lrt(platform: FormattedText, routeNumber: FormattedText, destination: FormattedText, carts: FormattedText, time: FormattedText, remark: FormattedText): ETALineEntryText {
                if (time.isEmpty()) return remark(remark)
                return ETALineEntryText(platform = platform, routeNumber = routeNumber, destination = destination, carts = carts, time = time, remark = remark, isTrain = true)
            }
            fun mtr(platform: FormattedText, destination: FormattedText, time: FormattedText, remark: FormattedText): ETALineEntryText {
                if (time.isEmpty()) return remark(remark)
                return ETALineEntryText(platform = platform, destination = destination, time = time, remark = remark, isTrain = true)
            }
        }

        val resolvedClockTime: FormattedText = clockTime.ifBlank { time }

        fun withDisplayMode(clockTime: FormattedText, displayMode: ETADisplayMode): ETALineEntryText {
            return when (displayMode) {
                ETADisplayMode.COUNTDOWN -> this
                ETADisplayMode.CLOCK_TIME -> ETALineEntryText(
                    platform = platform,
                    routeNumber = routeNumber,
                    destination = destination,
                    carts = carts,
                    time = buildFormattedString { append(clockTime, BoldStyle) },
                    operator = operator,
                    remark = remark,
                    isTrain = isTrain
                )
                ETADisplayMode.CLOCK_TIME_WITH_COUNTDOWN -> ETALineEntryText(
                    clockTime = buildFormattedString {
                        append(clockTime, Colored(0xFF2582C4, 0xFFFFE600))
                        append(" ")
                    },
                    platform = platform,
                    routeNumber = routeNumber,
                    destination = destination,
                    carts = carts,
                    time = time,
                    operator = operator,
                    remark = remark,
                    isTrain = isTrain
                )
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ETALineEntryText) return false
            if (!super.equals(other)) return false

            if (clockTime != other.clockTime) return false
            if (platform != other.platform) return false
            if (routeNumber != other.routeNumber) return false
            if (destination != other.destination) return false
            if (carts != other.carts) return false
            if (time != other.time) return false
            if (operator != other.operator) return false
            if (remark != other.remark) return false
            if (isTrain != other.isTrain) return false
            if (resolvedClockTime != other.resolvedClockTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + clockTime.hashCode()
            result = 31 * result + platform.hashCode()
            result = 31 * result + routeNumber.hashCode()
            result = 31 * result + destination.hashCode()
            result = 31 * result + carts.hashCode()
            result = 31 * result + time.hashCode()
            result = 31 * result + operator.hashCode()
            result = 31 * result + remark.hashCode()
            result = 31 * result + isTrain.hashCode()
            result = 31 * result + resolvedClockTime.hashCode()
            return result
        }


    }

    @Immutable
    class ETALineEntry private constructor(
        val text: ETALineEntryText,
        val shortText: ETAShortText,
        val platform: Int,
        val routeNumber: String,
        val eta: Double,
        val etaRounded: Long
    ) {

        companion object {

            val EMPTY = ETALineEntry(ETALineEntryText.EMPTY, ETAShortText.EMPTY, -1, "", -1.0, -1)

            fun textEntry(text: String): ETALineEntry {
                return ETALineEntry(ETALineEntryText.remark(text.asFormattedText()), ETAShortText.EMPTY, -1, "", -1.0, -1)
            }

            fun textEntry(text: FormattedText): ETALineEntry {
                return ETALineEntry(ETALineEntryText.remark(text), ETAShortText.EMPTY, -1, "", -1.0, -1)
            }

            fun etaEntry(text: ETALineEntryText, shortText: ETAShortText, routeNumber: String, eta: Double, etaRounded: Long): ETALineEntry {
                return etaEntry(text, shortText, -1, routeNumber, eta, etaRounded)
            }

            fun etaEntry(text: ETALineEntryText, shortText: ETAShortText, platform: Int, routeNumber: String, eta: Double, etaRounded: Long): ETALineEntry {
                return if (etaRounded > -60) {
                    ETALineEntry(text, shortText, platform, routeNumber, eta.coerceAtLeast(0.0), etaRounded.coerceAtLeast(0))
                } else {
                    ETALineEntry(text, shortText, platform, routeNumber, -1.0, -1)
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ETALineEntry) return false

            if (text != other.text) return false
            if (shortText != other.shortText) return false
            if (platform != other.platform) return false
            if (routeNumber != other.routeNumber) return false
            if (eta != other.eta) return false
            return etaRounded == other.etaRounded
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + shortText.hashCode()
            result = 31 * result + platform
            result = 31 * result + routeNumber.hashCode()
            result = 31 * result + eta.hashCode()
            result = 31 * result + etaRounded.hashCode()
            return result
        }
    }

    @Immutable
    data class ETAShortText(
        val first: String,
        val second: String
    ) {
        companion object {
            val EMPTY = ETAShortText("", "")
        }
        constructor(
            first: String,
            second: String,
            special: Boolean
        ): this(if (special) "*$first" else first, second)
    }

    @Immutable
    class ETAQueryResult private constructor(
        val isConnectionError: Boolean,
        val isMtrEndOfLine: Boolean,
        val isTyphoonSchedule: Boolean,
        val nextCo: Operator,
        lines: Map<Int, ETALineEntry>
    ) {

        companion object {

            val NULL_VALUE: ETAQueryResult? = null

            fun connectionError(restrictionType: BackgroundRestrictionType?, co: Operator): ETAQueryResult {
                val lines: Map<Int, ETALineEntry> = when (restrictionType) {
                    BackgroundRestrictionType.POWER_SAVE_MODE -> {
                        mapOf(
                            1 to ETALineEntry.textEntry(if (Shared.language == "en") "Background Internet Restricted" else "背景網絡存取被限制"),
                            2 to ETALineEntry.textEntry(if (Shared.language == "en") "Power Saving" else "省電模式")
                        )
                    }
                    BackgroundRestrictionType.RESTRICT_BACKGROUND_STATUS -> {
                        mapOf(
                            1 to ETALineEntry.textEntry(if (Shared.language == "en") "Background Internet Restricted" else "背景網絡存取被限制"),
                            2 to ETALineEntry.textEntry(if (Shared.language == "en") "Data Saver" else "數據節省器")
                        )
                    }
                    BackgroundRestrictionType.LOW_POWER_STANDBY -> {
                        mapOf(
                            1 to ETALineEntry.textEntry(if (Shared.language == "en") "Background Internet Restricted" else "背景網絡存取被限制"),
                            2 to ETALineEntry.textEntry(if (Shared.language == "en") "Low Power Standby" else "低耗電待機")
                        )
                    }
                    else -> {
                        mapOf(
                            1 to ETALineEntry.textEntry(if (Shared.language == "en") "Unable to Connect" else "無法連接伺服器")
                        )
                    }
                }
                return ETAQueryResult(isConnectionError = true, isMtrEndOfLine = false, isTyphoonSchedule = false, co, lines)
            }

            fun result(isMtrEndOfLine: Boolean, isTyphoonSchedule: Boolean, nextCo: Operator, lines: Map<Int, ETALineEntry>): ETAQueryResult {
                return ETAQueryResult(false, isMtrEndOfLine, isTyphoonSchedule, nextCo, lines)
            }

        }

        val time: Long = currentTimeMillis()

        fun isOutdated(): Boolean {
            return currentTimeMillis() - time > Shared.ETA_FRESHNESS
        }

        val rawLines: Map<Int, ETALineEntry> = lines
        val nextScheduledBus: Long = lines[1]?.etaRounded?: -1

        val firstLine: ETALineEntry get() = this[1]

        fun getLine(index: Int): ETALineEntry {
            return rawLines[index]?: ETALineEntry.EMPTY
        }

        operator fun get(index: Int): ETALineEntry {
            return getLine(index)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ETAQueryResult

            if (time != other.time) return false
            if (isConnectionError != other.isConnectionError) return false
            if (isMtrEndOfLine != other.isMtrEndOfLine) return false
            if (isTyphoonSchedule != other.isTyphoonSchedule) return false
            if (nextCo != other.nextCo) return false
            if (rawLines != other.rawLines) return false
            return nextScheduledBus == other.nextScheduledBus
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + isConnectionError.hashCode()
            result = 31 * result + isMtrEndOfLine.hashCode()
            result = 31 * result + isTyphoonSchedule.hashCode()
            result = 31 * result + nextCo.hashCode()
            result = 31 * result + rawLines.hashCode()
            result = 31 * result + nextScheduledBus.hashCode()
            return result
        }

    }

    @Immutable
    class MergedETAQueryResult<T> private constructor(
        val isConnectionError: Boolean,
        val isMtrEndOfLine: Boolean,
        val isTyphoonSchedule: Boolean,
        val nextCo: Operator,
        private val lines: Map<Int, Pair<T, ETALineEntry>>,
        val mergedCount: Int,
        val time: Long,
    ) {

        val nextScheduledBus: Long = lines[1]?.second?.etaRounded?: -1
        val firstKey: T? = lines.minByOrNull { it.key }?.value?.first
        val allKeys: Set<T> = lines.entries.asSequence().map { it.value.first }.toSet()

        fun getLine(index: Int): Pair<T?, ETALineEntry> {
            return lines[index]?: (null to ETALineEntry.EMPTY)
        }

        operator fun get(index: Int): Pair<T?, ETALineEntry> {
            return getLine(index)
        }

        fun isOutdated(): Boolean {
            return currentTimeMillis() - time > Shared.ETA_FRESHNESS
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MergedETAQueryResult<*>) return false

            if (time != other.time) return false
            if (isConnectionError != other.isConnectionError) return false
            if (isMtrEndOfLine != other.isMtrEndOfLine) return false
            if (isTyphoonSchedule != other.isTyphoonSchedule) return false
            if (nextCo != other.nextCo) return false
            if (lines != other.lines) return false
            if (mergedCount != other.mergedCount) return false
            if (nextScheduledBus != other.nextScheduledBus) return false
            if (firstKey != other.firstKey) return false
            return allKeys == other.allKeys
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + isConnectionError.hashCode()
            result = 31 * result + isMtrEndOfLine.hashCode()
            result = 31 * result + isTyphoonSchedule.hashCode()
            result = 31 * result + nextCo.hashCode()
            result = 31 * result + lines.hashCode()
            result = 31 * result + mergedCount
            result = 31 * result + nextScheduledBus.hashCode()
            result = 31 * result + (firstKey?.hashCode() ?: 0)
            result = 31 * result + allKeys.hashCode()
            return result
        }

        companion object {

            fun <T> loading(nextCo: Operator): MergedETAQueryResult<T> {
                return MergedETAQueryResult(
                    isConnectionError = false,
                    isMtrEndOfLine = false,
                    isTyphoonSchedule = false,
                    nextCo = nextCo,
                    lines = emptyMap(),
                    mergedCount = 1,
                    time = -1
                )
            }

            fun <T> merge(etaQueryResult: List<Pair<T, ETAQueryResult>>): MergedETAQueryResult<T> {
                if (etaQueryResult.size == 1) {
                    val (key, value) = etaQueryResult[0]
                    val lines = value.rawLines.mapValues { key to it.value }
                    return MergedETAQueryResult(value.isConnectionError, value.isMtrEndOfLine, value.isTyphoonSchedule, value.nextCo, lines, 1, value.time)
                }
                val time = etaQueryResult.minOf { it.second.time }
                val isConnectionError = etaQueryResult.all { it.second.isConnectionError }
                val isMtrEndOfLine = etaQueryResult.all { it.second.isMtrEndOfLine }
                val isTyphoonSchedule = etaQueryResult.any { it.second.isTyphoonSchedule }
                if (isConnectionError) {
                    val (key, value) = etaQueryResult[0]
                    val lines = value.rawLines.mapValues { key to it.value }
                    return MergedETAQueryResult(true, isMtrEndOfLine, isTyphoonSchedule, value.nextCo, lines, etaQueryResult.size, time)
                }
                val linesSorted: MutableList<Triple<T, ETALineEntry, Operator>> = etaQueryResult.asSequence()
                    .flatMap { it.second.rawLines.values.asSequence().map { line -> Triple(it.first, line, it.second.nextCo) } }
                    .sortedWith(
                        compareBy<Triple<T, ETALineEntry, Operator>> { it.second.eta.let { v -> if (v < 0) Double.MAX_VALUE else v } }
                            .thenBy { etaQueryResult.indexOfFirst { i -> i.first == it.first } }
                    )
                    .toMutableList()
                if (linesSorted.any { it.second.eta >= 0 }) {
                    linesSorted.removeAll { it.second.eta < 0 }
                }
                val nextCo = if (linesSorted.isEmpty()) etaQueryResult[0].second.nextCo else linesSorted[0].third
                val lines: MutableMap<Int, Pair<T, ETALineEntry>> = mutableMapOf()
                linesSorted.withIndex().forEach { lines[it.index + 1] = it.value.first to it.value.second }
                return MergedETAQueryResult(false, isMtrEndOfLine, isTyphoonSchedule, nextCo, lines, etaQueryResult.size, time)
            }

        }

    }

    enum class State(val isProcessing: Boolean) {
        LOADING(true),
        UPDATE_CHECKING(true),
        UPDATING(true),
        READY(false),
        ERROR(false)
    }
}

fun Collection<RouteSearchResultEntry>.getPossibleNextChar(input: String): Registry.PossibleNextCharResult {
    val result: MutableSet<Char> = HashSet()
    var exactMatch = false
    for (entry in this) {
        val routeNumber = entry.route?.routeNumber?: continue
        if (routeNumber.startsWith(input)) {
            if (routeNumber.length > input.length) {
                result.add(routeNumber[input.length])
            } else {
                exactMatch = true
            }
        }
    }
    return Registry.PossibleNextCharResult(result, exactMatch)
}