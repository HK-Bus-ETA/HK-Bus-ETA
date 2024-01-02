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
package com.loohp.hkbuseta.common.shared

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
import com.loohp.hkbuseta.common.branchedlist.BranchedList
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.DataContainer
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.FavouriteStopMode
import com.loohp.hkbuseta.common.objects.GMBRegion
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.RouteSortMode
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.StopInfo
import com.loohp.hkbuseta.common.objects.getColor
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.identifyStopCo
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.prependTo
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.BoldStyle
import com.loohp.hkbuseta.common.utils.Colored
import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.InlineImage
import com.loohp.hkbuseta.common.utils.IntUtils
import com.loohp.hkbuseta.common.utils.LongUtils
import com.loohp.hkbuseta.common.utils.SmallSize
import com.loohp.hkbuseta.common.utils.asFormattedText
import com.loohp.hkbuseta.common.utils.asMutableStateFlow
import com.loohp.hkbuseta.common.utils.buildFormattedString
import com.loohp.hkbuseta.common.utils.commonElementPercentage
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.editDistance
import com.loohp.hkbuseta.common.utils.getCircledNumber
import com.loohp.hkbuseta.common.utils.getJSONResponse
import com.loohp.hkbuseta.common.utils.getTextResponse
import com.loohp.hkbuseta.common.utils.getTextResponseWithPercentageCallback
import com.loohp.hkbuseta.common.utils.gzipSupported
import com.loohp.hkbuseta.common.utils.indexOf
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optInt
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.postJSONResponse
import com.loohp.hkbuseta.common.utils.strEq
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.roundToInt

class Registry {

    companion object {

        private const val PREFERENCES_FILE_NAME = "preferences.json"
        private const val CHECKSUM_FILE_NAME = "checksum.json"
        private const val DATA_FILE_NAME = "data.json"

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

        fun hasInstanceCreated(): Boolean {
            INSTANCE_LOCK.withLock {
                return INSTANCE.value != null
            }
        }

        fun clearInstance() {
            INSTANCE_LOCK.withLock {
                INSTANCE.value = null
            }
        }

        fun initInstanceWithImportedPreference(context: AppContext, preferencesData: JsonObject) {
            INSTANCE_LOCK.withLock {
                try {
                    INSTANCE.value = Registry(context, true, preferencesData)
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
        }

        fun invalidateCache(context: AppContext) {
            try {
                context.deleteFile(CHECKSUM_FILE_NAME)
            } catch (ignore: Throwable) {
            }
        }
    }

    private var PREFERENCES: Preferences? = null
    private var DATA: DataContainer? = null

    private val typhoonInfo: MutableStateFlow<TyphoonInfo> = TyphoonInfo.NO_DATA.asMutableStateFlow()
    private val typhoonInfoDeferred: AtomicReference<Deferred<TyphoonInfo>> = AtomicReference(CompletableDeferred(TyphoonInfo.NO_DATA))
    private val stateFlow: MutableStateFlow<State> = State.LOADING.asMutableStateFlow()
    private val updatePercentageStateFlow: MutableStateFlow<Float> = 0f.asMutableStateFlow()
    private val preferenceWriteLock = Lock()
    private val lastUpdateCheckHolder = AtomicLong(0)
    private val currentChecksumTask = AtomicReference<Job?>(null)
    private val objectCache: MutableMap<String, Any> = ConcurrentMutableMap()

    private constructor(context: AppContext, suppressUpdateCheck: Boolean) {
        ensureData(context, suppressUpdateCheck)
    }

    private constructor(context: AppContext, suppressUpdateCheck: Boolean, importPreferencesData: JsonObject) {
        importPreference(context, importPreferencesData)
        ensureData(context, suppressUpdateCheck)
    }

    @NativeCoroutinesState
    val state: StateFlow<State> = stateFlow
    @NativeCoroutinesState
    val updatePercentageState: StateFlow<Float> = updatePercentageStateFlow
    val lastUpdateCheck: Long get() = lastUpdateCheckHolder.get()

    private fun savePreferences(context: AppContext) {
        preferenceWriteLock.withLock {
            context.writeTextFile(PREFERENCES_FILE_NAME) { PREFERENCES!!.serialize().toString() }
        }
    }

    private fun importPreference(context: AppContext, preferencesData: JsonObject) {
        val preferences = Preferences.deserialize(preferencesData).cleanForImport()
        context.writeTextFile(PREFERENCES_FILE_NAME) { preferences.serialize().toString() }
    }

    fun exportPreference(): JsonObject {
        preferenceWriteLock.withLock {
            return PREFERENCES!!.serialize()
        }
    }

    fun setLanguage(language: String?, context: AppContext) {
        Shared.language = language!!
        PREFERENCES!!.language = language
        savePreferences(context)
        Tiles.requestTileUpdate()
    }

    fun hasFavouriteRouteStop(favoriteIndex: Int): Boolean {
        return Shared.favoriteRouteStops[favoriteIndex] != null
    }

    fun isFavouriteRouteStop(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route): Boolean {
        val favouriteRouteStop = Shared.favoriteRouteStops[favoriteIndex]?: return false
        if (co !== favouriteRouteStop.co) {
            return false
        }
        if (route.routeNumber != favouriteRouteStop.route.routeNumber) {
            return false
        }
        if (route.bound[co] != favouriteRouteStop.route.bound[co]) {
            return false
        }
        if (favouriteRouteStop.favouriteStopMode == FavouriteStopMode.FIXED) {
            return if (index != favouriteRouteStop.index) {
                false
            } else if (stopId != favouriteRouteStop.stopId) {
                false
            } else {
                stop.name.zh == favouriteRouteStop.stop.name.zh
            }
        }
        return true
    }

    fun clearFavouriteRouteStop(favoriteIndex: Int, context: AppContext) {
        clearFavouriteRouteStop(favoriteIndex, true, context)
    }

    private fun clearFavouriteRouteStop(favoriteIndex: Int, save: Boolean, context: AppContext) {
        Shared.updateFavoriteRouteStops { it.remove(favoriteIndex) }
        PREFERENCES!!.favouriteRouteStops.remove(favoriteIndex)
        val changes: MutableMap<Int, List<Int>> = HashMap()
        val deletions: MutableList<Int> = ArrayList()
        for ((key, value) in Tiles.getRawEtaTileConfigurations()) {
            if (value.contains(favoriteIndex)) {
                val updated: MutableList<Int> = ArrayList(
                    value
                )
                updated.remove(favoriteIndex)
                if (updated.isEmpty()) {
                    deletions.add(key)
                } else {
                    changes[key] = updated
                }
            }
        }
        PREFERENCES!!.etaTileConfigurations.putAll(changes)
        deletions.forEach { PREFERENCES!!.etaTileConfigurations.remove(it) }
        Tiles.updateEtaTileConfigurations {
            it.putAll(changes)
            deletions.forEach { key -> it.remove(key) }
        }
        if (save) {
            savePreferences(context)
            Tiles.requestTileUpdate(0)
            Tiles.requestTileUpdate(favoriteIndex)
        }
    }

    fun setFavouriteRouteStop(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, favouriteStopMode: FavouriteStopMode, context: AppContext) {
        setFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route, favouriteStopMode, bypassEtaTileCheck = false, save = true, context)
    }

    private fun setFavouriteRouteStop(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, favouriteStopMode: FavouriteStopMode, bypassEtaTileCheck: Boolean, save: Boolean, context: AppContext) {
        val favouriteRouteStop = FavouriteRouteStop(stopId, co, index, stop, route, favouriteStopMode)
        Shared.updateFavoriteRouteStops { it[favoriteIndex] = favouriteRouteStop }
        PREFERENCES!!.favouriteRouteStops[favoriteIndex] = favouriteRouteStop
        if (!bypassEtaTileCheck) {
            val changes: MutableMap<Int, List<Int>> = HashMap()
            val deletions: MutableList<Int> = ArrayList()
            for ((key, value) in Tiles.getRawEtaTileConfigurations()) {
                if (value.contains(favoriteIndex)) {
                    val updated: MutableList<Int> = ArrayList(value)
                    updated.remove(favoriteIndex)
                    if (updated.isEmpty()) {
                        deletions.add(key)
                    } else {
                        changes[key] = updated
                    }
                }
            }
            PREFERENCES!!.etaTileConfigurations.putAll(changes)
            deletions.forEach { PREFERENCES!!.etaTileConfigurations.remove(it) }
            Tiles.updateEtaTileConfigurations {
                it.putAll(changes)
                deletions.forEach { key -> it.remove(key) }
            }
        }
        if (save) {
            savePreferences(context)
            Tiles.requestTileUpdate(0)
            Tiles.requestTileUpdate(favoriteIndex)
        }
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
        Tiles.requestTileUpdate(0)
    }

    fun addLastLookupRoute(routeNumber: String?, co: Operator?, meta: String?, context: AppContext) {
        Shared.addLookupRoute(routeNumber!!, co!!, meta!!)
        val lastLookupRoutes = Shared.getLookupRoutes()
        PREFERENCES!!.lastLookupRoutes.clear()
        PREFERENCES!!.lastLookupRoutes.addAll(lastLookupRoutes)
        savePreferences(context)
    }

    fun clearLastLookupRoutes(context: AppContext) {
        Shared.clearLookupRoute()
        PREFERENCES!!.lastLookupRoutes.clear()
        savePreferences(context)
    }

    fun setRouteSortModePreference(context: AppContext, listType: RouteListType, sortMode: RouteSortMode) {
        (Shared.routeSortModePreference as MutableMap<RouteListType, RouteSortMode>)[listType] = sortMode
        PREFERENCES!!.routeSortModePreference.clear()
        PREFERENCES!!.routeSortModePreference.putAll(Shared.routeSortModePreference)
        savePreferences(context)
    }

    fun cancelCurrentChecksumTask() {
        currentChecksumTask.get()?.cancel()
    }

    private fun ensureData(context: AppContext, suppressUpdateCheck: Boolean) {
        if (stateFlow.value == State.READY) {
            return
        }
        if (PREFERENCES != null && DATA != null) {
            return
        }
        val files = context.listFiles()
        if (files.contains(PREFERENCES_FILE_NAME)) {
            try {
                PREFERENCES = Preferences.deserialize(Json.decodeFromString<JsonObject>(context.readTextFile(
                    PREFERENCES_FILE_NAME
                )))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        if (PREFERENCES == null) {
            PREFERENCES = Preferences.createDefault()
            savePreferences(context)
        }
        Shared.language = PREFERENCES!!.language
        Shared.updateFavoriteRouteStops {
            it.clear()
            it.putAll(PREFERENCES!!.favouriteRouteStops)
        }
        Tiles.updateEtaTileConfigurations {
            it.clear()
            it.putAll(PREFERENCES!!.etaTileConfigurations)
        }
        Shared.clearLookupRoute()
        val lastLookupRoutes = PREFERENCES!!.lastLookupRoutes
        val itr = lastLookupRoutes.iterator()
        while (itr.hasNext()) {
            val lastLookupRoute = itr.next()
            if (lastLookupRoute.isValid()) {
                Shared.addLookupRoute(lastLookupRoute)
            } else {
                itr.remove()
            }
        }
        Shared.routeSortModePreference as MutableMap<RouteListType, RouteSortMode>
        Shared.routeSortModePreference.clear()
        Shared.routeSortModePreference.putAll(PREFERENCES!!.routeSortModePreference)
        checkUpdate(context, suppressUpdateCheck)
    }

    fun checkUpdate(context: AppContext, suppressUpdateCheck: Boolean) {
        stateFlow.value = State.LOADING
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
                        getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/checksum.md5") + "_" + version
                    } }
                    currentChecksumTask.set(future)
                    if (!forced && files.contains(CHECKSUM_FILE_NAME) && files.contains(
                            DATA_FILE_NAME
                        )) {
                        stateFlow.value = State.UPDATE_CHECKING
                    }
                    try {
                        val result = future.await()
                        updateChecked.value = true
                        result
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    } finally {
                        if (stateFlow.value == State.UPDATE_CHECKING) {
                            stateFlow.value = State.LOADING
                        }
                    }
                }
                var cached = false
                var checksum = if (!suppressUpdateCheck && hasConnection) checksumFetcher.invoke(false) else null
                if (files.contains(CHECKSUM_FILE_NAME) && files.contains(DATA_FILE_NAME)) {
                    if (checksum == null) {
                        cached = true
                    } else {
                        val localChecksum = context.readTextFile(CHECKSUM_FILE_NAME)
                        if (localChecksum == checksum) {
                            cached = true
                        }
                    }
                }
                if (cached) {
                    if (DATA == null) {
                        try {
                            DATA = DataContainer.deserialize(Json.decodeFromString<JsonObject>(context.readTextFile(
                                DATA_FILE_NAME
                            )))
                            Tiles.requestTileUpdate()
                            stateFlow.value = State.READY
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    } else {
                        stateFlow.value = State.READY
                    }
                }
                if (stateFlow.value != State.READY) {
                    if (!hasConnection) {
                        stateFlow.value = State.ERROR
                        try {
                            context.deleteFile(CHECKSUM_FILE_NAME)
                        } catch (ignore: Throwable) {
                        }
                    } else {
                        stateFlow.value = State.UPDATING
                        updatePercentageStateFlow.value = 0f
                        val percentageOffset =
                            if (Shared.favoriteRouteStops.isEmpty()) 0.15f else 0f
                        if (!updateChecked.value) {
                            checksum = checksumFetcher.invoke(true)
                        }
                        val gzip = gzipSupported()
                        val gzLabel = if (gzip) ".gz" else ""
                        val length: Long = LongUtils.parseOr(getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/size$gzLabel.dat"), -1)
                        val textResponse: String = getTextResponseWithPercentageCallback("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/data.json$gzLabel", length, gzip) { p -> updatePercentageStateFlow.value = p * 0.75f + percentageOffset }?: throw RuntimeException("Error downloading bus data")
                        DATA = DataContainer.deserialize(Json.decodeFromString<JsonObject>(textResponse))
                        updatePercentageStateFlow.value = 0.75f + percentageOffset
                        context.writeTextFile(DATA_FILE_NAME) { textResponse }
                        updatePercentageStateFlow.value = 0.825f + percentageOffset
                        context.writeTextFile(CHECKSUM_FILE_NAME) { checksum ?: "" }
                        updatePercentageStateFlow.value = 0.85f + percentageOffset
                        var localUpdatePercentage = updatePercentageStateFlow.value
                        val percentagePerFav = 0.15f / Shared.favoriteRouteStops.size
                        val updatedFavouriteRouteTasks: MutableList<() -> Unit> = ArrayList()
                        for ((favouriteRouteIndex, favouriteRoute) in Shared.favoriteRouteStops) {
                            try {
                                val oldRoute = favouriteRoute.route
                                var stopId = favouriteRoute.stopId
                                val co = favouriteRoute.co
                                val newRoutes = findRoutes(oldRoute.routeNumber, true) { r ->
                                    if (!r.bound.containsKey(co)) {
                                        return@findRoutes false
                                    }
                                    if (co === Operator.GMB) {
                                        if (r.gmbRegion != oldRoute.gmbRegion) {
                                            return@findRoutes false
                                        }
                                    } else if (co === Operator.NLB) {
                                        return@findRoutes r.nlbId == oldRoute.nlbId
                                    }
                                    r.bound[co] == oldRoute.bound[co]
                                }
                                if (newRoutes.isEmpty()) {
                                    updatedFavouriteRouteTasks.add { clearFavouriteRouteStop(favouriteRouteIndex, false, context) }
                                    continue
                                }
                                val newRouteData = newRoutes[0]
                                val newRoute: Route = newRouteData.route!!
                                val stopList = getAllStops(newRoute.routeNumber, (if (co === Operator.NLB) newRoute.nlbId else newRoute.bound[co])!!, co, newRoute.gmbRegion)
                                val finalStopIdCompare = stopId
                                var index: Int = stopList.indexOf { it.stopId == finalStopIdCompare } + 1
                                var stopData: StopData
                                if (index < 1) {
                                    index = favouriteRoute.index.coerceIn(1, stopList.size)
                                    stopData = stopList[index - 1]
                                    stopId = stopData.stopId
                                } else {
                                    stopData = stopList[index - 1]
                                }
                                val stop: Stop = stopList[index - 1].stop
                                val finalStopId = stopId
                                val finalIndex = index
                                updatedFavouriteRouteTasks.add { setFavouriteRouteStop(favouriteRouteIndex, finalStopId, co, finalIndex, stop, stopData.route, favouriteRoute.favouriteStopMode, bypassEtaTileCheck = true, save = false, context) }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                            localUpdatePercentage += percentagePerFav
                            updatePercentageStateFlow.value = localUpdatePercentage
                        }
                        if (updatedFavouriteRouteTasks.isNotEmpty()) {
                            updatedFavouriteRouteTasks.forEach { it.invoke() }
                            savePreferences(context)
                        }
                        updatePercentageStateFlow.value = 1f
                        Tiles.requestTileUpdate()
                        stateFlow.value = State.READY
                    }
                }
                updatePercentageStateFlow.value = 1f
            } catch (e: Exception) {
                e.printStackTrace()
                stateFlow.value = State.ERROR
            }
            if (stateFlow.value != State.READY) {
                stateFlow.value = State.ERROR
            }
        }
    }

    fun getRouteKey(route: Route?): String? {
        return DATA!!.dataSheet.routeList.entries.asSequence()
            .filter{ (_, value) -> value == route }
            .firstOrNull()?.let { (key, _) -> key }
    }

    fun findRouteByKey(lookupKey: String, routeNumber: String?): Route? {
        var inputKey = lookupKey
        val exact = DATA!!.dataSheet.routeList[inputKey]
        if (exact != null) {
            return exact
        }
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
        val result: MutableSet<Char> = HashSet()
        var exactMatch = false
        for (routeNumber in DATA!!.busRoute) {
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

    fun findRoutes(input: String, exact: Boolean): List<RouteSearchResultEntry> {
        return findRoutes(input, exact, { true }, { _, _ -> true })
    }

    fun findRoutes(input: String, exact: Boolean, predicate: (Route) -> Boolean): List<RouteSearchResultEntry> {
        return findRoutes(input, exact, predicate) { _, _ -> true }
    }

    fun findRoutes(input: String, exact: Boolean, coPredicate: (Route, Operator) -> Boolean): List<RouteSearchResultEntry> {
        return findRoutes(input, exact, { true }, coPredicate)
    }

    private fun findRoutes(input: String, exact: Boolean, predicate: (Route) -> Boolean = { true }, coPredicate: (Route, Operator) -> Boolean = { _, _ -> true }): List<RouteSearchResultEntry> {
        val routeMatcher: (String) -> Boolean = if (exact) ({ it == input }) else ({ it.startsWith(input) })
        val matchingRoutes: MutableMap<String, RouteSearchResultEntry> = HashMap()
        for ((key, data) in DATA!!.dataSheet.routeList.entries) {
            if (data.isCtbIsCircular) {
                continue
            }
            if (routeMatcher.invoke(data.routeNumber) && predicate.invoke(data)) {
                var co: Operator
                val bound = data.bound
                co = if (bound.containsKey(Operator.KMB)) {
                    Operator.KMB
                } else if (bound.containsKey(Operator.CTB)) {
                    Operator.CTB
                } else if (bound.containsKey(Operator.NLB)) {
                    Operator.NLB
                } else if (bound.containsKey(Operator.MTR_BUS)) {
                    Operator.MTR_BUS
                } else if (bound.containsKey(Operator.GMB)) {
                    Operator.GMB
                } else if (bound.containsKey(Operator.LRT)) {
                    Operator.LRT
                } else if (bound.containsKey(Operator.MTR)) {
                    Operator.MTR
                } else {
                    continue
                }
                if (!coPredicate.invoke(data, co)) {
                    continue
                }
                val key0 = (data.routeNumber + "," + co.name) + "," + (if (co === Operator.NLB) data.nlbId else data.bound[co]) + if (co === Operator.GMB) "," + data.gmbRegion else ""
                if (matchingRoutes.containsKey(key0)) {
                    try {
                        val existingMatchingRoute: RouteSearchResultEntry? = matchingRoutes[key0]
                        val type = data.serviceType.toInt()
                        val matchingType: Int = existingMatchingRoute!!.route!!.serviceType.toInt()
                        if (type < matchingType) {
                            existingMatchingRoute.routeKey = key
                            existingMatchingRoute.route = data
                            existingMatchingRoute.co = co
                        } else if (type == matchingType) {
                            val gtfs: Int = IntUtils.parseOr(data.gtfsId, Int.MAX_VALUE)
                            val matchingGtfs: Int =
                                IntUtils.parseOr(existingMatchingRoute.route!!.gtfsId, Int.MAX_VALUE)
                            if (gtfs < matchingGtfs) {
                                existingMatchingRoute.routeKey = key
                                existingMatchingRoute.route = data
                                existingMatchingRoute.co = co
                            }
                        }
                    } catch (ignore: NumberFormatException) {
                    }
                } else {
                    matchingRoutes[key0] = RouteSearchResultEntry(key, data, co)
                }
            }
        }
        return if (matchingRoutes.isEmpty()) {
            emptyList()
        } else matchingRoutes.values.asSequence().sortedWith { a, b ->
                val routeA: Route = a.route!!
                val routeB: Route = b.route!!
                val boundA = routeA.bound
                val boundB = routeB.bound
                val coA = boundA.keys.max()
                val coB = boundB.keys.max()
                val coDiff = coA.compareTo(coB)
                if (coDiff != 0) {
                    return@sortedWith coDiff
                }
                val routeNumberA = routeA.routeNumber
                val routeNumberB = routeB.routeNumber
                if (coA.isTrain && coB.isTrain) {
                    val lineDiff = Shared.getMtrLineSortingIndex(routeNumberA)
                        .compareTo(Shared.getMtrLineSortingIndex(routeNumberB))
                    if (lineDiff != 0) {
                        return@sortedWith lineDiff
                    }
                    return@sortedWith -boundA[coA]!!.compareTo(boundB[coB]!!)
                } else {
                    val routeNumberDiff = routeNumberA.compareTo(routeNumberB)
                    if (routeNumberDiff != 0) {
                        return@sortedWith routeNumberDiff
                    }
                }
                if (coA === Operator.NLB) {
                    return@sortedWith IntUtils.parseOrZero(routeA.nlbId) - IntUtils.parseOrZero(routeB.nlbId)
                }
                if (coA === Operator.GMB) {
                    val gtfsDiff: Int =
                        IntUtils.parseOrZero(routeA.gtfsId) - IntUtils.parseOrZero(routeB.gtfsId)
                    if (gtfsDiff != 0) {
                        return@sortedWith gtfsDiff
                    }
                }
                val typeDiff: Int =
                    IntUtils.parseOrZero(routeA.serviceType) - IntUtils.parseOrZero(routeB.serviceType)
                if (typeDiff == 0) {
                    if (coA === Operator.CTB) {
                        return@sortedWith 0
                    }
                    return@sortedWith -boundA[coA]!!.compareTo(boundB[coB]!!)
                }
                typeDiff
            }.toList()
    }

    fun getNearbyRoutes(lat: Double, lng: Double, excludedRouteNumbers: Set<String?>, isInterchangeSearch: Boolean): NearbyRoutesResult {
        val origin = Coordinates(lat, lng)
        val stops: Map<String, Stop> = DATA!!.dataSheet.stopList
        val nearbyStops: MutableList<StopInfo> = ArrayList()
        var closestStop: Stop? = null
        var closestDistance = Double.MAX_VALUE
        for ((stopId, entry) in stops) {
            val location: Coordinates = entry.location
            val distance: Double = origin.distance(location)
            if (distance < closestDistance) {
                closestStop = entry
                closestDistance = distance
            }
            if (distance <= 0.3) {
                val co: Operator = stopId.identifyStopCo()?: continue
                nearbyStops.add(StopInfo(stopId, entry, distance, co))
            }
        }
        val nearbyRoutes: MutableMap<String, RouteSearchResultEntry> = HashMap()
        for (nearbyStop in nearbyStops) {
            val stopId: String = nearbyStop.stopId
            for ((key, data) in DATA!!.dataSheet.routeList.entries) {
                if (excludedRouteNumbers.contains(data.routeNumber)) {
                    continue
                }
                if (data.isCtbIsCircular) {
                    continue
                }
                val co = Operator.values().firstOrNull {
                    if (!data.bound.containsKey(it)) {
                        return@firstOrNull false
                    }
                    val coStops: List<String> = data.stops[it]?: return@firstOrNull false
                    coStops.contains(stopId)
                }
                if (co != null) {
                    val key0 = (data.routeNumber + "," + co.name) + "," + (if (co === Operator.NLB) data.nlbId else data.bound[co]) + if (co === Operator.GMB) "," + data.gmbRegion else ""
                    if (nearbyRoutes.containsKey(key0)) {
                        val existingNearbyRoute: RouteSearchResultEntry? = nearbyRoutes[key0]
                        if (existingNearbyRoute!!.stopInfo!!.distance > nearbyStop.distance) {
                            try {
                                val type = data.serviceType.toInt()
                                val matchingType: Int =
                                    existingNearbyRoute.route!!.serviceType.toInt()
                                if (type < matchingType) {
                                    existingNearbyRoute.routeKey = key
                                    existingNearbyRoute.stopInfo = nearbyStop
                                    existingNearbyRoute.route = data
                                    existingNearbyRoute.co = co
                                    existingNearbyRoute.origin = origin
                                    existingNearbyRoute.isInterchangeSearch = isInterchangeSearch
                                } else if (type == matchingType) {
                                    val gtfs: Int = IntUtils.parseOr(data.gtfsId, Int.MAX_VALUE)
                                    val matchingGtfs: Int = IntUtils.parseOr(
                                        existingNearbyRoute.route!!.gtfsId,
                                        Int.MAX_VALUE
                                    )
                                    if (gtfs < matchingGtfs) {
                                        existingNearbyRoute.routeKey = key
                                        existingNearbyRoute.stopInfo = nearbyStop
                                        existingNearbyRoute.route = data
                                        existingNearbyRoute.co = co
                                        existingNearbyRoute.origin = origin
                                        existingNearbyRoute.isInterchangeSearch =
                                            isInterchangeSearch
                                    }
                                }
                            } catch (ignore: NumberFormatException) {
                                existingNearbyRoute.routeKey = key
                                existingNearbyRoute.stopInfo = nearbyStop
                                existingNearbyRoute.route = data
                                existingNearbyRoute.co = co
                                existingNearbyRoute.origin = origin
                                existingNearbyRoute.isInterchangeSearch = isInterchangeSearch
                            }
                        }
                    } else {
                        nearbyRoutes[key0] = RouteSearchResultEntry(key, data, co, nearbyStop, origin, isInterchangeSearch)
                    }
                }
            }
        }
        if (nearbyRoutes.isEmpty()) {
            return NearbyRoutesResult(emptyList(), closestStop!!, closestDistance, lat, lng)
        }
        val hongKongTime = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Hong_Kong"))
        val hour = hongKongTime.hour
        val isNight = hour in 1..4
        val weekday = hongKongTime.dayOfWeek
        val date = hongKongTime.date
        val isHoliday = weekday == DayOfWeek.SATURDAY || weekday == DayOfWeek.SUNDAY || DATA!!.dataSheet.holidays.contains(date)
        return NearbyRoutesResult(nearbyRoutes.values.asSequence().sortedWith(compareBy({ a ->
            val route: Route = a.route!!
            val routeNumber = route.routeNumber
            val bound = route.bound
            val pa = routeNumber[0].toString()
            val sa = routeNumber[routeNumber.length - 1].toString()
            var na: Int = IntUtils.parseOrZero(routeNumber.replace(Regex("[^0-9]"), ""))
            if (bound.containsKey(Operator.GMB)) {
                na += 1000
            } else if (bound.containsKey(Operator.MTR)) {
                na += if (isInterchangeSearch) -2000 else 2000
            }
            if (pa == "N" || routeNumber == "270S" || routeNumber == "271S" || routeNumber == "293S" || routeNumber == "701S" || routeNumber == "796S") {
                na -= (if (isNight) 1 else -1) * 10000
            }
            if (sa == "S" && routeNumber != "89S" && routeNumber != "796S") {
                na += 3000
            }
            if (!isHoliday && (pa == "R" || sa == "R")) {
                na += 100000
            }
            na
        },
        { a -> a.route!!.routeNumber },
        { a -> IntUtils.parseOrZero(a.route!!.serviceType) },
        { a -> a.co },
        { a ->
            val route: Route = a.route!!
            val bound = route.bound
            if (bound.containsKey(Operator.MTR)) {
                return@compareBy -Shared.getMtrLineSortingIndex(route.routeNumber)
            }
            -10
        }))
        .distinctBy { it.routeKey }
        .toList(), closestStop!!, closestDistance, lat, lng)
    }

    @Immutable
    data class NearbyRoutesResult(val result: List<RouteSearchResultEntry>, val closestStop: Stop, val closestDistance: Double, val lat: Double, val lng: Double)

    fun getAllStops(routeNumber: String, bound: String, co: Operator, gmbRegion: GMBRegion?): List<StopData> {
        return try {
            val lists: MutableList<Pair<BranchedList<String, StopData>, Int>> = ArrayList()
            for (route in DATA!!.dataSheet.routeList.values) {
                if (routeNumber == route.routeNumber && route.co.contains(co)) {
                    var flag: Boolean
                    if (co === Operator.NLB) {
                        flag = bound == route.nlbId
                    } else {
                        flag = bound == route.bound[co]
                        if (co === Operator.GMB) {
                            flag = flag and (gmbRegion == route.gmbRegion)
                        }
                    }
                    if (flag) {
                        val localStops: BranchedList<String, StopData> = BranchedList()
                        val stops = route.stops[co]
                        val serviceType: Int = IntUtils.parseOr(route.serviceType, 1)
                        for (stopId in stops!!) {
                            localStops.add(stopId, StopData(stopId, serviceType, DATA!!.dataSheet.stopList[stopId]!!, route))
                        }
                        lists.add(localStops to serviceType)
                    }
                }
            }
            lists.sortBy { it.second }
            val result: BranchedList<String, StopData> = BranchedList(label@{ a, b ->
                val aType: Int = a.serviceType
                val bType: Int = b.serviceType
                if (aType == bType) {
                    val aGtfs: Int = IntUtils.parseOr(a.route.gtfsId, Int.MAX_VALUE)
                    val bGtfs: Int = IntUtils.parseOr(b.route.gtfsId, Int.MAX_VALUE)
                    return@label if (aGtfs > bGtfs) b else a
                }
                if (aType > bType) b else a
            })
            for ((first) in lists) {
                result.merge(first)
            }
            result.asSequenceWithBranchIds()
                .map { (f, s) -> f.withBranchIndex(s) }
                .toList()
        } catch (e: Throwable) {
            throw RuntimeException("Error occurred while getting stops for " + routeNumber + ", " + bound + ", " + co + ", " + gmbRegion + ": " + e.message, e)
        }
    }

    @Immutable
    data class StopData(val stopId: String, val serviceType: Int, val stop: Stop, val route: Route, val branchIds: Set<Int>) {

        constructor(stopId: String, serviceType: Int, stop: Stop, route: Route) : this(stopId, serviceType, stop, route, emptySet())

        fun withBranchIndex(branchIds: Set<Int>): StopData {
            return StopData(stopId, serviceType, stop, route, branchIds)
        }
    }

    fun getAllOriginsAndDestinations(routeNumber: String, bound: String, co: Operator, gmbRegion: GMBRegion?): Pair<List<BilingualText>, List<BilingualText>> {
        return try {
            val origs: MutableList<Triple<BilingualText, Int, String>> = ArrayList()
            val dests: MutableList<Pair<BilingualText, Int>> = ArrayList()
            var mainRouteServiceType = Int.MAX_VALUE
            var mainRouteStops: List<String?>? = emptyList<String>()
            for (route in DATA!!.dataSheet.routeList.values) {
                if (routeNumber == route.routeNumber && route.co.contains(co)) {
                    var flag: Boolean
                    if (co == Operator.NLB) {
                        flag = bound == route.nlbId
                    } else {
                        flag = bound == route.bound[co]
                        if (co == Operator.GMB) {
                            flag = flag and (gmbRegion == route.gmbRegion)
                        }
                    }
                    if (flag) {
                        val serviceType: Int = IntUtils.parseOr(route.serviceType, 1)
                        val stops: List<String> = route.stops[co]!!
                        if (mainRouteServiceType > serviceType || mainRouteServiceType == serviceType && stops.size > mainRouteStops!!.size) {
                            mainRouteServiceType = serviceType
                            mainRouteStops = stops
                        }
                        val orig: BilingualText = route.orig
                        val oldOrig: Triple<BilingualText, Int, String>? = origs.firstOrNull { it.first.zh == orig.zh }
                        if (oldOrig == null || oldOrig.second > serviceType) {
                            origs.add(Triple(orig, serviceType, stops[0]))
                        }
                        val dest: BilingualText = route.dest
                        val oldDest: Pair<BilingualText, Int>? = dests.firstOrNull { it.first.zh == dest.zh }
                        if (oldDest == null || oldDest.second > serviceType) {
                            dests.add(dest to serviceType)
                        }
                    }
                }
            }
            val finalMainRouteStops = mainRouteStops
            origs.asSequence().filter { !finalMainRouteStops!!.contains(it.third) }.sortedBy { it.second }.map { it.first }.toList() to
                dests.asSequence().sortedBy { it.second }.map { it.first }.toList()
        } catch (e: Throwable) {
            throw RuntimeException("Error occurred while getting origins and destinations for " + routeNumber + ", " + bound + ", " + co + ", " + gmbRegion + ": " + e.message, e)
        }
    }

    fun getStopSpecialDestinations(stopId: String, co: Operator, route: Route, prependTo: Boolean): BilingualText {
        if (route.lrtCircular != null) {
            return route.lrtCircular
        }
        val bound = route.bound[co]
        when (stopId) {
            "LHP" -> {
                return if (bound!!.contains("UT")) {
                    BilingualText("康城", "LOHAS Park").prependTo()
                } else {
                    BilingualText("北角/寶琳", "North Point/Po Lam").prependTo()
                }
            }
            "HAH", "POA" -> {
                return if (bound!!.contains("UT")) {
                    BilingualText("寶琳", "Po Lam").prependTo()
                } else {
                    BilingualText("北角/康城", "North Point/LOHAS Park").prependTo()
                }
            }
            "AIR", "AWE" -> {
                if (bound!!.contains("UT")) {
                    return BilingualText("博覽館", "AsiaWorld-Expo").prependTo()
                }
            }
        }
        return route.dest.prependTo()
    }

    fun getAllDestinationsByDirection(routeNumber: String, co: Operator, nlbId: String?, gmbRegion: GMBRegion?, referenceRoute: Route, stopId: String): Pair<Set<BilingualText>, Set<BilingualText>> {
        return try {
            val direction: MutableSet<BilingualText> = HashSet()
            val all: MutableSet<BilingualText> = HashSet()
            for (route in DATA!!.dataSheet.routeList.values) {
                if (routeNumber == route.routeNumber && route.stops.containsKey(co)) {
                    var flag = true
                    if (co === Operator.NLB) {
                        flag = nlbId == route.nlbId
                    } else if (co === Operator.GMB) {
                        flag = gmbRegion == route.gmbRegion
                    }
                    if (flag) {
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
        val stopIds = route.stops[Operator.LRT] ?: return false
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
        val isOutOfStationPaid =
            stopId != "ETS" && stopId != "TST" && stopId != "KOW" && stopId != "AUS"
        var hasLightRail = false
        val outOfStationStopName: String? = when (stopId) {
            "ETS" -> "尖沙咀"
            "TST" -> "尖東"
            "HOK" -> "中環"
            "CEN" -> "香港"
            else -> null
        }
        val stopName: String = DATA!!.dataSheet.stopList[stopId]!!.name.zh
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

    @Immutable
    data class MTRInterchangeData(val lines: List<String>, val isOutOfStationPaid: Boolean, val outOfStationLines: List<String>, val isHasLightRail: Boolean)

    fun getNoScheduledDepartureMessage(altMessageInput: FormattedText?, isAboveTyphoonSignalEight: Boolean, typhoonWarningTitle: String): FormattedText {
        var altMessage = altMessageInput
        if (altMessage.isNullOrEmpty()) {
            altMessage = (if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次").asFormattedText()
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

    val cachedTyphoonDataState: StateFlow<TyphoonInfo> get() = typhoonInfo

    val currentTyphoonData: Deferred<TyphoonInfo> get() {
        val cache = typhoonInfo.value
        if (cache != TyphoonInfo.NO_DATA && currentTimeMillis() - cache.lastUpdated < 300000) {
            return CompletableDeferred(cache)
        }
        return typhoonInfoDeferred.value.takeIf { it.isActive }?: CoroutineScope(Dispatchers.IO).async {
            val data: JsonObject? = getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=warnsum&lang=" + if (Shared.language == "en") "en" else "tc")
            if (data != null && data.contains("WTCSGNL")) {
                val matchingGroups = Regex("TC([0-9]+)(.*)").find(data.optJsonObject("WTCSGNL")!!.optString("code"))?.groupValues
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
                        "tc" + signal.toString().padStart(2, '0') + (matchingGroups.getOrNull(2)?: "").lowercase()
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

            val NO_DATA = TyphoonInfo(isAboveTyphoonSignalEight = false, isAboveTyphoonSignalNine = false, typhoonWarningTitle = "", currentTyphoonSignalId = "", lastUpdated = 0)

            fun none(): TyphoonInfo {
                return TyphoonInfo(isAboveTyphoonSignalEight = false, isAboveTyphoonSignalNine = false, typhoonWarningTitle = "", currentTyphoonSignalId = "", lastUpdated = currentTimeMillis())
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

    fun getEta(stopId: String, stopIndex: Int, co: Operator, route: Route, context: AppContext): PendingETAQueryResult {
        context.logFirebaseEvent("eta_query", AppBundle().apply {
            putString("by_stop", stopId + "," + stopIndex + "," + route.routeNumber + "," + co.name + "," + route.bound[co])
            putString("by_bound", route.routeNumber + "," + co.name + "," + route.bound[co])
            putString("by_route", route.routeNumber + "," + co.name)
        })
        val pending = PendingETAQueryResult(context, co, CoroutineScope(Dispatchers.IO).async {
            try {
                val typhoonInfo = currentTyphoonData.await()
                val lines: MutableMap<Int, ETALineEntry> = HashMap()
                var isMtrEndOfLine = false
                var isTyphoonSchedule = false
                var nextCo: Operator = co
                lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
                val language = Shared.language
                when {
                    route.isKmbCtbJoint -> {
                        isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                        val jointOperated: MutableSet<JointOperatedEntry> = ConcurrentMutableSet()
                        var kmbSpecialMessage: FormattedText? = null
                        var kmbFirstScheduledBus = Long.MAX_VALUE
                        val kmbFuture = launch {
                            val data: JsonObject? = getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/$stopId")
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
                                            val mins = (eta.toInstant().epochSeconds - Clock.System.now().epochSeconds) / 60.0
                                            val minsRounded = mins.roundToInt()
                                            var message = "".asFormattedText()
                                            if (language == "en") {
                                                if (minsRounded > 0) {
                                                    message = buildFormattedString {
                                                        append(minsRounded.toString(), BoldStyle)
                                                        append(" Min.", SmallSize)
                                                    }
                                                } else if (minsRounded > -60) {
                                                    message = buildFormattedString {
                                                        append("-", BoldStyle)
                                                        append(" Min.", SmallSize)
                                                    }
                                                }
                                                if (bus.optString("rmk_en").isNotEmpty()) {
                                                    message += buildFormattedString {
                                                        if (message.isEmpty()) {
                                                            append(bus.optString("rmk_en"))
                                                        } else {
                                                            append(" (${bus.optString("rmk_en")})", SmallSize)
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (minsRounded > 0) {
                                                    message = buildFormattedString {
                                                        append(minsRounded.toString(), BoldStyle)
                                                        append(" 分鐘", SmallSize)
                                                    }
                                                } else if (minsRounded > -60) {
                                                    message = buildFormattedString {
                                                        append("-", BoldStyle)
                                                        append(" 分鐘", SmallSize)
                                                    }
                                                }
                                                if (bus.optString("rmk_tc").isNotEmpty()) {
                                                    message += buildFormattedString {
                                                        if (message.isEmpty()) {
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
                                            if ((message.contains("預定班次") || message.contains("Scheduled Bus")) && mins < kmbFirstScheduledBus) {
                                                kmbFirstScheduledBus = minsRounded.toLong()
                                            }
                                            jointOperated.add(JointOperatedEntry(mins, minsRounded.toLong(), message, Operator.KMB))
                                        } else {
                                            var message = "".asFormattedText()
                                            if (language == "en") {
                                                if (bus.optString("rmk_en").isNotEmpty()) {
                                                    message += buildFormattedString {
                                                        if (message.isEmpty()) {
                                                            append(bus.optString("rmk_en").replace("(Final Bus)", ""))
                                                        } else {
                                                            append(" (${bus.optString("rmk_en").replace("(Final Bus)", "")})", SmallSize)
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (bus.optString("rmk_tc").isNotEmpty()) {
                                                    message += buildFormattedString {
                                                        if (message.isEmpty()) {
                                                            append(bus.optString("rmk_tc")
                                                                .replace("(尾班車)", "")
                                                                .replace("原定", "預定")
                                                                .replace("最後班次", "尾班車")
                                                                .replace("尾班車已過", "尾班車已過本站"))
                                                        } else {
                                                            append(" (${bus.optString("rmk_tc")
                                                                .replace("(尾班車)", "")
                                                                .replace("原定", "預定")
                                                                .replace("最後班次", "尾班車")
                                                                .replace("尾班車已過", "尾班車已過本站")})", SmallSize)
                                                        }
                                                    }
                                                }
                                            }
                                            message = if (message.isEmpty() || typhoonInfo.isAboveTyphoonSignalEight && (message strEq "ETA service suspended" || message strEq "暫停預報")) {
                                                getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                            } else {
                                                "".asFormattedText(BoldStyle) + message
                                            }
                                            kmbSpecialMessage = message
                                        }
                                    }
                                }
                            }
                        }
                        run {
                            val routeNumber = route.routeNumber
                            val matchingStops: List<Pair<Operator, String>>? = DATA!!.dataSheet.stopMap[stopId]
                            val ctbStopIds: MutableList<String> = ArrayList()
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
                                    objectCache[cacheKey] = ArrayList(ctbStopIds)
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
                            val destKeys = second.asSequence().map { it.zh.replace(" ", "") }.toSet()
                            val ctbEtaEntries: ConcurrentMutableMap<String?, MutableSet<JointOperatedEntry>> = ConcurrentMutableMap()
                            val stopQueryData: MutableList<JsonObject?> = ArrayList()
                            val ctbFutures: MutableList<Deferred<*>> = ArrayList(ctbStopIds.size)
                            for (ctbStopId in ctbStopIds) {
                                ctbFutures.add(async { stopQueryData.add(getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/$ctbStopId/$routeNumber")) })
                            }
                            for (future in ctbFutures) {
                                future.await()
                            }
                            val stopSequences: MutableMap<String, MutableSet<Int>> = HashMap()
                            val queryBusDests: Array<Array<String?>?> = arrayOfNulls(stopQueryData.size)
                            for (i in stopQueryData.indices) {
                                val data = stopQueryData[i]
                                val buses = data!!.optJsonArray("data")!!
                                val busDests = arrayOfNulls<String>(buses.size)
                                for (u in 0 until buses.size) {
                                    val bus = buses.optJsonObject(u)!!
                                    if (Operator.CTB === Operator.valueOf(bus.optString("co")) && routeNumber == bus.optString("route")) {
                                        val rawBusDest = bus.optString("dest_tc").replace(" ", "")
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
                                val usedRealSeq: MutableMap<String?, MutableSet<Int>> = HashMap()
                                for (u in 0 until buses.size) {
                                    val bus = buses.optJsonObject(u)!!
                                    if (Operator.CTB === Operator.valueOf(bus.optString("co")) && routeNumber == bus.optString("route")) {
                                        val busDest = queryBusDests[i]!![u]!!
                                        val stopSeq = bus.optInt("seq")
                                        if ((stopSeq == (matchingSeq[busDest]?: 0)) && usedRealSeq.getOrPut(busDest) { HashSet() }.add(bus.optInt("eta_seq"))) {
                                            val eta = bus.optString("eta")
                                            if (eta.isNotEmpty() && !eta.equals("null", ignoreCase = true)) {
                                                val mins = (eta.toInstant().epochSeconds - Clock.System.now().epochSeconds) / 60.0
                                                val minsRounded = mins.roundToInt()
                                                var message = "".asFormattedText()
                                                if (language == "en") {
                                                    if (minsRounded > 0) {
                                                        message = buildFormattedString {
                                                            append(minsRounded.toString(), BoldStyle)
                                                            append(" Min.", SmallSize)
                                                        }
                                                    } else if (minsRounded > -60) {
                                                        message = buildFormattedString {
                                                            append("-", BoldStyle)
                                                            append(" Min.", SmallSize)
                                                        }
                                                    }
                                                    if (bus.optString("rmk_en").isNotEmpty()) {
                                                        message += buildFormattedString {
                                                            if (message.isEmpty()) {
                                                                append(bus.optString("rmk_en"))
                                                            } else {
                                                                append(" (${bus.optString("rmk_en")})", SmallSize)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (minsRounded > 0) {
                                                        message = buildFormattedString {
                                                            append(minsRounded.toString(), BoldStyle)
                                                            append(" 分鐘", SmallSize)
                                                        }
                                                    } else if (minsRounded > -60) {
                                                        message = buildFormattedString {
                                                            append("-", BoldStyle)
                                                            append(" 分鐘", SmallSize)
                                                        }
                                                    }
                                                    if (bus.optString("rmk_tc").isNotEmpty()) {
                                                        message += buildFormattedString {
                                                            if (message.isEmpty()) {
                                                                append(bus.optString("rmk_tc")
                                                                    .replace("(尾班車)", "")
                                                                    .replace("原定", "預定")
                                                                    .replace("最後班次", "尾班車")
                                                                    .replace("尾班車已過", "尾班車已過本站"))
                                                            } else {
                                                                append(" (${bus.optString("rmk_tc")
                                                                    .replace("(尾班車)", "")
                                                                    .replace("原定", "預定")
                                                                    .replace("最後班次", "尾班車")
                                                                    .replace("尾班車已過", "尾班車已過本站")})", SmallSize)
                                                            }
                                                        }
                                                    }
                                                }
                                                ctbEtaEntries.synchronize {
                                                    ctbEtaEntries.getOrPut(busDest) { ConcurrentMutableSet() }.add(
                                                        JointOperatedEntry(mins, minsRounded.toLong(), message, Operator.CTB)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            first.asSequence().map { ctbEtaEntries[it.zh.replace(" ", "")] }.forEach {
                                if (it != null) {
                                    jointOperated.addAll(it)
                                }
                            }
                        }
                        kmbFuture.join()
                        if (jointOperated.isEmpty()) {
                            if (kmbSpecialMessage.isNullOrEmpty()) {
                                lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
                            } else {
                                lines[1] = ETALineEntry.textEntry(kmbSpecialMessage)
                            }
                        } else {
                            var counter = 0
                            val itr = jointOperated.asSequence().sorted().iterator()
                            while (itr.hasNext()) {
                                val entry = itr.next()
                                val mins = entry.mins
                                val minsRounded = entry.minsRounded
                                var message = "".asFormattedText(BoldStyle) + entry.line
                                val entryCo = entry.co
                                if (minsRounded > kmbFirstScheduledBus && !(message.contains("預定班次") || message.contains("Scheduled Bus"))) {
                                    message += (if (Shared.language == "en") " (Scheduled Bus)" else " (預定班次)").asFormattedText(SmallSize)
                                }
                                message += if (entryCo === Operator.KMB) {
                                    if (route.routeNumber.getKMBSubsidiary() === KMBSubsidiary.LWB) {
                                        (if (Shared.language == "en") " - LWB" else " - 龍運").asFormattedText(SmallSize)
                                    } else {
                                        (if (Shared.language == "en") " - KMB" else " - 九巴").asFormattedText(SmallSize)
                                    }
                                } else {
                                    (if (Shared.language == "en") " - CTB" else " - 城巴").asFormattedText(SmallSize)
                                }
                                val seq = ++counter
                                if (seq == 1) {
                                    nextCo = entryCo
                                }
                                lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded)
                            }
                        }
                    }
                    co === Operator.KMB -> {
                        isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                        val data: JsonObject? = getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/$stopId")
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
                        var counter = 0
                        val usedRealSeq: MutableSet<Int> = HashSet()
                        for (u in 0 until buses.size) {
                            val bus = buses.optJsonObject(u)!!
                            if (Operator.KMB === Operator.valueOf(bus.optString("co"))) {
                                val routeNumber = bus.optString("route")
                                val bound = bus.optString("dir")
                                val stopSeq = bus.optInt("seq")
                                if (routeNumber == route.routeNumber && bound == route.bound[Operator.KMB] && stopSeq == matchingSeq) {
                                    val seq = ++counter
                                    if (usedRealSeq.add(bus.optInt("eta_seq"))) {
                                        val eta = bus.optString("eta")
                                        val mins: Double = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) -999.0 else (eta.toInstant().epochSeconds - Clock.System.now().epochSeconds) / 60.0
                                        val minsRounded = mins.roundToInt()
                                        var message = "".asFormattedText()
                                        if (language == "en") {
                                            if (minsRounded > 0) {
                                                message = buildFormattedString {
                                                    append(minsRounded.toString(), BoldStyle)
                                                    append(" Min.", SmallSize)
                                                }
                                            } else if (minsRounded > -60) {
                                                message = buildFormattedString {
                                                    append("-", BoldStyle)
                                                    append(" Min.", SmallSize)
                                                }
                                            }
                                            if (bus.optString("rmk_en").isNotEmpty()) {
                                                message += buildFormattedString {
                                                    if (message.isEmpty()) {
                                                        append(bus.optString("rmk_en"))
                                                    } else {
                                                        append(" (${bus.optString("rmk_en")})", SmallSize)
                                                    }
                                                }
                                            }
                                        } else {
                                            if (minsRounded > 0) {
                                                message = buildFormattedString {
                                                    append(minsRounded.toString(), BoldStyle)
                                                    append(" 分鐘", SmallSize)
                                                }
                                            } else if (minsRounded > -60) {
                                                message = buildFormattedString {
                                                    append("-", BoldStyle)
                                                    append(" 分鐘", SmallSize)
                                                }
                                            }
                                            if (bus.optString("rmk_tc").isNotEmpty()) {
                                                message += buildFormattedString {
                                                    if (message.isEmpty()) {
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
                                        message = if (message.isEmpty() || typhoonInfo.isAboveTyphoonSignalEight && (message strEq "ETA service suspended" || message strEq "暫停預報")) {
                                            if (seq == 1) {
                                                getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                            } else {
                                                buildFormattedString {
                                                    append("", BoldStyle)
                                                    append("-")
                                                }
                                            }
                                        } else {
                                            "".asFormattedText(BoldStyle) + message
                                        }
                                        lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 0), mins, minsRounded.toLong())
                                    }
                                }
                            }
                        }
                    }
                    co === Operator.CTB -> {
                        isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                        val routeNumber = route.routeNumber
                        val routeBound = route.bound[Operator.CTB]
                        val data: JsonObject? = getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/$stopId/$routeNumber")
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
                        for (u in 0 until buses.size) {
                            val bus = buses.optJsonObject(u)!!
                            if (Operator.CTB === Operator.valueOf(bus.optString("co"))) {
                                val bound = bus.optString("dir")
                                val stopSeq = bus.optInt("seq")
                                if (routeNumber == bus.optString("route") && (routeBound!!.length > 1 || bound == routeBound) && stopSeq == matchingSeq) {
                                    val seq = ++counter
                                    if (usedRealSeq.add(bus.optInt("eta_seq"))) {
                                        val eta = bus.optString("eta")
                                        val mins: Double = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) -999.0 else (eta.toInstant().epochSeconds - Clock.System.now().epochSeconds) / 60.0
                                        val minsRounded = mins.roundToInt()
                                        var message = "".asFormattedText()
                                        if (language == "en") {
                                            if (minsRounded > 0) {
                                                message = buildFormattedString {
                                                    append(minsRounded.toString(), BoldStyle)
                                                    append(" Min.", SmallSize)
                                                }
                                            } else if (minsRounded > -60) {
                                                message = buildFormattedString {
                                                    append("-", BoldStyle)
                                                    append(" Min.", SmallSize)
                                                }
                                            }
                                            if (bus.optString("rmk_en").isNotEmpty()) {
                                                message += buildFormattedString {
                                                    if (message.isEmpty()) {
                                                        append(bus.optString("rmk_en"))
                                                    } else {
                                                        append(" (${bus.optString("rmk_en")})", SmallSize)
                                                    }
                                                }
                                            }
                                        } else {
                                            if (minsRounded > 0) {
                                                message = buildFormattedString {
                                                    append(minsRounded.toString(), BoldStyle)
                                                    append(" 分鐘", SmallSize)
                                                }
                                            } else if (minsRounded > -60) {
                                                message = buildFormattedString {
                                                    append("-", BoldStyle)
                                                    append(" 分鐘", SmallSize)
                                                }
                                            }
                                            if (bus.optString("rmk_tc").isNotEmpty()) {
                                                message += buildFormattedString {
                                                    if (message.isEmpty()) {
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
                                        message = if (message.isEmpty()) {
                                            if (seq == 1) {
                                                getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                            } else {
                                                buildFormattedString {
                                                    append("", BoldStyle)
                                                    append("-")
                                                }
                                            }
                                        } else {
                                            "".asFormattedText(BoldStyle) + message
                                        }
                                        lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 0), mins, minsRounded.toLong())
                                    }
                                }
                            }
                        }
                    }
                    co === Operator.NLB -> {
                        isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                        val data: JsonObject? = getJSONResponse("https://rt.data.gov.hk/v2/transport/nlb/stop.php?action=estimatedArrivals&routeId=${route.nlbId}&stopId=$stopId&language=${Shared.language}")
                        if (!data.isNullOrEmpty() && data.contains("estimatedArrivals")) {
                            val buses = data.optJsonArray("estimatedArrivals")!!
                            for (u in 0 until buses.size) {
                                val bus = buses.optJsonObject(u)!!
                                val seq = u + 1
                                val eta = bus.optString("estimatedArrivalTime")
                                val variant = bus.optString("routeVariantName").trim { it <= ' ' }
                                val mins: Double = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) -999.0 else (eta.let { "${it.substring(0, 10)}T${it.substring(11)}" }.toLocalDateTime().toInstant(TimeZone.of("Asia/Hong_Kong")).epochSeconds - Clock.System.now().epochSeconds) / 60.0
                                val minsRounded = mins.roundToInt()
                                var message = "".asFormattedText()
                                if (language == "en") {
                                    if (minsRounded > 0) {
                                        message = buildFormattedString {
                                            append(minsRounded.toString(), BoldStyle)
                                            append(" Min.", SmallSize)
                                        }
                                    } else if (minsRounded > -60) {
                                        message = buildFormattedString {
                                            append("-", BoldStyle)
                                            append(" Min.", SmallSize)
                                        }
                                    }
                                } else {
                                    if (minsRounded > 0) {
                                        message = buildFormattedString {
                                            append(minsRounded.toString(), BoldStyle)
                                            append(" 分鐘", SmallSize)
                                        }
                                    } else if (minsRounded > -60) {
                                        message = buildFormattedString {
                                            append("-", BoldStyle)
                                            append(" 分鐘", SmallSize)
                                        }
                                    }
                                }
                                if (variant.isNotEmpty()) {
                                    message += buildFormattedString {
                                        if (message.isEmpty()) {
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
                                message = if (message.isEmpty()) {
                                    if (seq == 1) {
                                        getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                    } else {
                                        buildFormattedString {
                                            append("", BoldStyle)
                                            append("-")
                                        }
                                    }
                                } else {
                                    "".asFormattedText(BoldStyle) + message
                                }
                                lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 0), mins, minsRounded.toLong())
                            }
                        }
                    }
                    co === Operator.MTR_BUS -> {
                        isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                        val routeNumber = route.routeNumber
                        val body = buildJsonObject {
                            put("language", Shared.language)
                            put("routeName", routeNumber)
                        }
                        val data: JsonObject? = postJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/bus/getSchedule", body)
                        val busStops = data!!.optJsonArray("busStop")!!
                        for (k in 0 until busStops.size) {
                            val busStop = busStops.optJsonObject(k)!!
                            val buses = busStop.optJsonArray("bus")!!
                            val busStopId = busStop.optString("busStopId")
                            for (u in 0 until buses.size) {
                                val bus = buses.optJsonObject(u)!!
                                val seq = u + 1
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
                                val minsRounded = floor(mins).toLong()
                                if (DATA!!.mtrBusStopAlias[stopId]!!.contains(busStopId)) {
                                    var message = "".asFormattedText()
                                    if (language == "en") {
                                        if (minsRounded > 0) {
                                            message = buildFormattedString {
                                                append(minsRounded.toString(), BoldStyle)
                                                append(" Min.", SmallSize)
                                            }
                                        } else if (minsRounded > -60) {
                                            message = buildFormattedString {
                                                append("-", BoldStyle)
                                                append(" Min.", SmallSize)
                                            }
                                        }
                                    } else {
                                        if (minsRounded > 0) {
                                            message = buildFormattedString {
                                                append(minsRounded.toString(), BoldStyle)
                                                append(" 分鐘", SmallSize)
                                            }
                                        } else if (minsRounded > -60) {
                                            message = buildFormattedString {
                                                append("-", BoldStyle)
                                                append(" 分鐘", SmallSize)
                                            }
                                        }
                                    }
                                    if (remark.isNotEmpty()) {
                                        message += buildFormattedString {
                                            if (message.isEmpty()) {
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
                                    message = if (message.isEmpty()) {
                                        if (seq == 1) {
                                            getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                        } else {
                                            buildFormattedString {
                                                append("", BoldStyle)
                                                append("-")
                                            }
                                        }
                                    } else {
                                        "".asFormattedText(BoldStyle) + message
                                    }
                                    lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded)
                                }
                            }
                        }
                    }
                    co === Operator.GMB -> {
                        isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                        val data: JsonObject? = getJSONResponse("https://data.etagmb.gov.hk/eta/stop/$stopId")
                        val stopSequences: MutableSet<Int> = HashSet()
                        val busList: MutableList<Triple<Int, Double, JsonObject>> = ArrayList()
                        for (i in 0 until data!!.optJsonArray("data")!!.size) {
                            val routeData = data.optJsonArray("data")!!.optJsonObject(i)!!
                            val buses = routeData.optJsonArray("eta")
                            val filteredEntry = DATA!!.dataSheet.routeList.values.firstOrNull { it.bound.containsKey(Operator.GMB) && it.gtfsId == routeData.optString("route_id") }
                            if (filteredEntry != null && buses != null) {
                                val routeNumber = filteredEntry.routeNumber
                                val stopSeq = routeData.optInt("stop_seq")
                                for (u in 0 until buses.size) {
                                    val bus = buses.optJsonObject(u)!!
                                    if (routeNumber == route.routeNumber) {
                                        val eta = bus.optString("timestamp")
                                        val mins: Double = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) -999.0 else (eta.toInstant().epochSeconds - Clock.System.now().epochSeconds) / 60.0
                                        stopSequences.add(stopSeq)
                                        busList.add(Triple(stopSeq, mins, bus))
                                    }
                                }
                            }
                        }
                        if (stopSequences.size > 1) {
                            val matchingSeq = stopSequences.minByOrNull { (it - stopIndex).absoluteValue }?: -1
                            busList.removeAll { it.first != matchingSeq }
                        }
                        busList.sortBy { it.second }
                        for (i in busList.indices) {
                            val (_, mins, bus) = busList[i]
                            val seq = i + 1
                            var remark = if (language == "en") bus.optString("remarks_en") else bus.optString("remarks_tc")
                            if (remark.equals("null", ignoreCase = true)) {
                                remark = ""
                            }
                            val minsRounded = mins.roundToInt()
                            var message = "".asFormattedText()
                            if (language == "en") {
                                if (minsRounded > 0) {
                                    message = buildFormattedString {
                                        append(minsRounded.toString(), BoldStyle)
                                        append(" Min.", SmallSize)
                                    }
                                } else if (minsRounded > -60) {
                                    message = buildFormattedString {
                                        append("-", BoldStyle)
                                        append(" Min.", SmallSize)
                                    }
                                }
                            } else {
                                if (minsRounded > 0) {
                                    message = buildFormattedString {
                                        append(minsRounded.toString(), BoldStyle)
                                        append(" 分鐘", SmallSize)
                                    }
                                } else if (minsRounded > -60) {
                                    message = buildFormattedString {
                                        append("-", BoldStyle)
                                        append(" 分鐘", SmallSize)
                                    }
                                }
                            }
                            if (remark.isNotEmpty()) {
                                message += buildFormattedString {
                                    if (message.isEmpty()) {
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
                            message = if (message.isEmpty()) {
                                if (seq == 1) {
                                    getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                } else {
                                    buildFormattedString {
                                        append("", BoldStyle)
                                        append("-")
                                    }
                                }
                            } else {
                                "".asFormattedText(BoldStyle) + message
                            }
                            lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 0), mins, minsRounded.toLong())
                        }
                    }
                    co === Operator.LRT -> {
                        isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine
                        val stopsList = route.stops[Operator.LRT]!!
                        if (stopsList.indexOf(stopId) + 1 >= stopsList.size) {
                            isMtrEndOfLine = true
                            lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "End of Line" else "終點站")
                        } else {
                            val hongKongTime = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Hong_Kong"))
                            val hour = hongKongTime.hour
                            val results: MutableList<LrtETAData> = ArrayList()
                            val data: JsonObject? = getJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=${stopId.substring(2)}")
                            if (data!!.optInt("status") != 0) {
                                val platformList = data.optJsonArray("platform_list")!!
                                for (i in 0 until platformList.size) {
                                    val platform = platformList.optJsonObject(i)!!
                                    val platformNumber = platform.optInt("platform_id")
                                    val routeList = platform.optJsonArray("route_list")
                                    if (routeList != null) {
                                        for (u in 0 until routeList.size) {
                                            val routeData = routeList.optJsonObject(u)!!
                                            val routeNumber = routeData.optString("route_no")
                                            val destCh = routeData.optString("dest_ch")
                                            if (routeNumber == route.routeNumber && isLrtStopOnOrAfter(stopId, destCh, route)) {
                                                val mins = Regex("([0-9]+) *min").find(routeData.optString("time_en"))?.groupValues?.getOrNull(1)?.toLong()?: 0
                                                val minsMsg = routeData.optString(if (Shared.language == "en") "time_en" else "time_ch")
                                                val dest = routeData.optString(if (Shared.language == "en") "dest_en" else "dest_ch")
                                                val trainLength = routeData.optInt("train_length")
                                                results.add(LrtETAData(routeNumber, dest, trainLength, platformNumber, mins, minsMsg))
                                            }
                                        }
                                    }
                                }
                            }
                            if (results.isEmpty()) {
                                if (hour < 3) {
                                    lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Last train has departed" else "尾班車已開出")
                                } else if (hour < 6) {
                                    lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service has not yet started" else "今日服務尚未開始")
                                } else {
                                    lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Server unable to provide data" else "系統未能提供資訊")
                                }
                            } else {
                                val lineColor = co.getColor(route.routeNumber, 0xFFFFFFFFL)
                                results.sortWith(naturalOrder())
                                for (i in results.indices) {
                                    val lrt = results[i]
                                    val seq = i + 1
                                    var minsMessage = lrt.etaMessage
                                    if (minsMessage == "-") {
                                        minsMessage = if (Shared.language == "en") "Departing" else "正在離開"
                                    }
                                    val annotatedMinsMessage = if (minsMessage == "即將抵達" || minsMessage == "Arriving" || minsMessage == "正在離開" || minsMessage == "Departing") {
                                        minsMessage.asFormattedText(BoldStyle)
                                    } else {
                                        val mins = Regex("^([0-9]+)").find(minsMessage)?.groupValues?.getOrNull(1)?.toLong()
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
                                    val message = buildFormattedString {
                                        append("", BoldStyle)
                                        append(lrt.platformNumber.getCircledNumber(), Colored(lineColor))
                                        append(" ")
                                        for (u in 0 until lrt.trainLength) {
                                            appendInlineContent(InlineImage.LRV, "\uD83D\uDE83")
                                        }
                                        if (lrt.trainLength == 1) {
                                            appendInlineContent(InlineImage.LRV_EMPTY, " ")
                                        }
                                        append(" ")
                                        append(annotatedMinsMessage)
                                    }
                                    lines[seq] = ETALineEntry.etaEntry(message, toShortText(mins, 1), mins.toDouble(), mins)
                                }
                            }
                        }
                    }
                    co === Operator.MTR -> {
                        isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine
                        val lineName = route.routeNumber
                        val lineColor = co.getColor(lineName, 0xFFFFFFFF)
                        val bound = route.bound[Operator.MTR]
                        if (isMtrStopEndOfLine(stopId, lineName, bound)) {
                            isMtrEndOfLine = true
                            lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "End of Line" else "終點站")
                        } else {
                            val hongKongTimeZone = TimeZone.of("Asia/Hong_Kong")
                            val hongKongTime = Clock.System.now().toLocalDateTime(hongKongTimeZone)
                            val hour = hongKongTime.hour
                            val dayOfWeek = hongKongTime.dayOfWeek
                            val data: JsonObject? = getJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=$lineName&sta=$stopId")
                            if (data!!.optInt("status") == 0) {
                                lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Server unable to provide data" else "系統未能提供資訊")
                            } else {
                                val lineStops = data.optJsonObject("data")!!.optJsonObject("$lineName-$stopId")
                                val raceDay = dayOfWeek == DayOfWeek.WEDNESDAY || dayOfWeek == DayOfWeek.SUNDAY
                                if (lineStops == null) {
                                    if (stopId == "RAC") {
                                        if (!raceDay) {
                                            lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service on race days only" else "僅在賽馬日提供服務")
                                        } else if (hour >= 15 || hour < 3) {
                                            lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Last train has departed" else "尾班車已開出")
                                        } else {
                                            lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service has not yet started" else "今日服務尚未開始")
                                        }
                                    } else if (hour < 3 || stopId == "LMC" && hour >= 10 || stopId == "SHS" && hour >= 11) {
                                        lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Last train has departed" else "尾班車已開出")
                                    } else if (hour < 6) {
                                        lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service has not yet started" else "今日服務尚未開始")
                                    } else {
                                        lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Server unable to provide data" else "系統未能提供資訊")
                                    }
                                } else {
                                    val delayed = data.optString("isdelay", "N") != "N"
                                    val dir = if (bound == "UT") "UP" else "DOWN"
                                    val trains = lineStops.optJsonArray(dir)
                                    if (trains.isNullOrEmpty()) {
                                        if (stopId == "RAC") {
                                            if (!raceDay) {
                                                lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service on race days only" else "僅在賽馬日提供服務")
                                            } else if (hour >= 15 || hour < 3) {
                                                lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Last train has departed" else "尾班車已開出")
                                            } else {
                                                lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service has not yet started" else "今日服務尚未開始")
                                            }
                                        } else if (hour < 3 || stopId == "LMC" && hour >= 10 || stopId == "SHS" && hour >= 11) {
                                            lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Last train has departed" else "尾班車已開出")
                                        } else if (hour < 6) {
                                            lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service has not yet started" else "今日服務尚未開始")
                                        } else {
                                            lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Server unable to provide data" else "系統未能提供資訊")
                                        }
                                    } else {
                                        for (u in 0 until trains.size) {
                                            val trainData = trains.optJsonObject(u)!!
                                            val seq = trainData.optString("seq").toInt()
                                            val platform = trainData.optString("plat").toInt()
                                            val specialRoute = trainData.optString("route")
                                            var dest: String = DATA!!.dataSheet.stopList[trainData.optString("dest")]!!.name[Shared.language]
                                            if (stopId != "AIR") {
                                                if (dest == "博覽館") {
                                                    dest = "機場及博覽館"
                                                } else if (dest == "AsiaWorld-Expo") {
                                                    dest = "Airport & AsiaWorld-Expo"
                                                }
                                            }
                                            var annotatedDest = dest.asFormattedText()
                                            if (specialRoute.isNotEmpty() && !isMtrStopOnOrAfter(stopId, specialRoute, lineName, bound)) {
                                                val via: String = DATA!!.dataSheet.stopList[specialRoute]!!.name[Shared.language]
                                                annotatedDest += ((if (Shared.language == "en") " via " else " 經") + via).asFormattedText(
                                                    SmallSize
                                                )
                                            }
                                            val timeType = trainData.optString("timeType")
                                            val eta = trainData.optString("time").let { "${it.substring(0, 10)}T${it.substring(11)}" }
                                            val mins = (eta.toLocalDateTime().toInstant(hongKongTimeZone).toEpochMilliseconds() - currentTimeMillis()) / 60000.0
                                            val minsRounded = mins.roundToInt()
                                            val minsMessage = if (minsRounded > 59) {
                                                val time = hongKongTime.toInstant(hongKongTimeZone).plus(minsRounded, DateTimeUnit.MINUTE, hongKongTimeZone).toLocalDateTime(hongKongTimeZone)
                                                "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}".asFormattedText(BoldStyle)
                                            } else if (minsRounded > 1) {
                                                buildFormattedString {
                                                    append(minsRounded.toString(), BoldStyle)
                                                    append(if (Shared.language == "en") " Min." else " 分鐘", SmallSize)
                                                }
                                            } else if (minsRounded == 1 && timeType != "D") {
                                                (if (Shared.language == "en") "Arriving" else "即將抵達").asFormattedText(BoldStyle)
                                            } else {
                                                (if (Shared.language == "en") "Departing" else "正在離開").asFormattedText(BoldStyle)
                                            }
                                            var message = buildFormattedString {
                                                append("", BoldStyle)
                                                append(platform.getCircledNumber(), Colored(lineColor))
                                                append(" ")
                                                append(annotatedDest)
                                                append(" ")
                                                append(minsMessage)
                                            }
                                            if (seq == 1) {
                                                if (delayed) {
                                                    message += (if (Shared.language == "en") " (Delayed)" else " (服務延誤)").asFormattedText(SmallSize)
                                                }
                                            }
                                            lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 1), mins, minsRounded.toLong())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, nextCo, lines)
            } catch (e: Throwable) {
                e.printStackTrace()
                ETAQueryResult.connectionError(context.currentBackgroundRestrictions(), co)
            }
        })
        return pending
    }

    private fun toShortText(minsRounded: Long, arrivingThreshold: Long): ETAShortText {
        return ETAShortText(
            if (minsRounded <= arrivingThreshold) "-" else minsRounded.toString(),
            if (Shared.language == "en") "Min." else "分鐘"
        )
    }

    @Immutable
    data class JointOperatedEntry(val mins: Double, val minsRounded: Long, val line: FormattedText, val co: Operator) : Comparable<JointOperatedEntry> {

        override operator fun compareTo(other: JointOperatedEntry): Int {
            return mins.compareTo(other.mins)
        }

    }

    @Immutable
    class PendingETAQueryResult(
        private val context: AppContext,
        private val co: Operator,
        private val deferred: Deferred<ETAQueryResult>
    ) {

        private val errorResult: ETAQueryResult
            get() {
            val restrictionType = if (context is AppActiveContext) BackgroundRestrictionType.NONE else context.currentBackgroundRestrictions()
            return ETAQueryResult.connectionError(restrictionType, co)
        }

        fun get(): ETAQueryResult {
            return try {
                runBlocking { deferred.await() }
            } catch (e: Exception) {
                e.printStackTrace()
                try { deferred.cancel() } catch (ignore: Throwable) { }
                errorResult
            }
        }

        fun get(timeout: Int, unit: DateTimeUnit.TimeBased): ETAQueryResult {
            return try {
                runBlocking { withTimeout(unit.duration.times(timeout).inWholeMilliseconds) { deferred.await() } }
            } catch (e: Exception) {
                e.printStackTrace()
                try { deferred.cancel() } catch (ignore: Throwable) { }
                errorResult
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        fun onComplete(callback: (ETAQueryResult) -> Unit) {
            deferred.invokeOnCompletion { it?.let {
                it.printStackTrace()
                callback.invoke(errorResult)
            }?: callback.invoke(deferred.getCompleted()) }
        }

        fun onComplete(timeout: Int, unit: DateTimeUnit.TimeBased, callback: (ETAQueryResult) -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
               try {
                   callback.invoke(withTimeout(unit.duration.times(timeout).inWholeMilliseconds) { deferred.await() })
               } catch (e: Exception) {
                   e.printStackTrace()
                   try { deferred.cancel() } catch (ignore: Throwable) { }
                   callback.invoke(errorResult)
               }
            }
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
    class ETALineEntry private constructor(
        val text: FormattedText?,
        val shortText: ETAShortText,
        val eta: Double,
        val etaRounded: Long
    ) {

        companion object {

            val EMPTY = ETALineEntry("-".asFormattedText(), ETAShortText.EMPTY, -1.0, -1)

            fun textEntry(text: String?): ETALineEntry {
                return ETALineEntry(text?.asFormattedText(), ETAShortText.EMPTY, -1.0, -1)
            }

            fun textEntry(text: FormattedText?): ETALineEntry {
                return ETALineEntry(text, ETAShortText.EMPTY, -1.0, -1)
            }

            fun etaEntry(text: String?, shortText: ETAShortText, eta: Double, etaRounded: Long): ETALineEntry {
                return if (etaRounded > -60) {
                    ETALineEntry(text?.asFormattedText(), shortText, eta.coerceAtLeast(0.0), etaRounded.coerceAtLeast(0))
                } else {
                    ETALineEntry(text?.asFormattedText(), shortText, -1.0, -1)
                }
            }

            fun etaEntry(text: FormattedText?, shortText: ETAShortText, eta: Double, etaRounded: Long): ETALineEntry {
                return if (etaRounded > -60) {
                    ETALineEntry(text, shortText, eta.coerceAtLeast(0.0), etaRounded.coerceAtLeast(0))
                } else {
                    ETALineEntry(text, shortText, -1.0, -1)
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ETALineEntry

            if (text != other.text) return false
            if (shortText != other.shortText) return false
            if (eta != other.eta) return false
            return etaRounded == other.etaRounded
        }

        override fun hashCode(): Int {
            var result = text?.hashCode() ?: 0
            result = 31 * result + shortText.hashCode()
            result = 31 * result + eta.hashCode()
            result = 31 * result + etaRounded.hashCode()
            return result
        }

    }

    @Immutable
    class ETAShortText(val first: String, val second: String) {

        companion object {
            val EMPTY = ETAShortText("", "")
        }

        operator fun component1(): String {
            return first
        }

        operator fun component2(): String {
            return second
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ETAShortText

            if (first != other.first) return false
            return second == other.second
        }

        override fun hashCode(): Int {
            var result = first.hashCode()
            result = 31 * result + second.hashCode()
            return result
        }

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

        val rawLines: Map<Int, ETALineEntry> = lines
        val nextScheduledBus: Long = lines[1]?.etaRounded ?: -1

        val firstLine: ETALineEntry get() = this[1]

        fun getLine(index: Int): ETALineEntry {
            return rawLines[index] ?: ETALineEntry.EMPTY
        }

        operator fun get(index: Int): ETALineEntry {
            return getLine(index)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ETAQueryResult

            if (isConnectionError != other.isConnectionError) return false
            if (isMtrEndOfLine != other.isMtrEndOfLine) return false
            if (isTyphoonSchedule != other.isTyphoonSchedule) return false
            if (nextCo != other.nextCo) return false
            if (rawLines != other.rawLines) return false
            return nextScheduledBus == other.nextScheduledBus
        }

        override fun hashCode(): Int {
            var result = isConnectionError.hashCode()
            result = 31 * result + isMtrEndOfLine.hashCode()
            result = 31 * result + isTyphoonSchedule.hashCode()
            result = 31 * result + nextCo.hashCode()
            result = 31 * result + rawLines.hashCode()
            result = 31 * result + nextScheduledBus.hashCode()
            return result
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
