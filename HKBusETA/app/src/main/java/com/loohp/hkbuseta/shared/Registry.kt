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
package com.loohp.hkbuseta.shared

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.core.app.ComponentActivity
import androidx.core.util.AtomicFile
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.loohp.hkbuseta.branchedlist.BranchedList
import com.loohp.hkbuseta.objects.BilingualText
import com.loohp.hkbuseta.objects.Coordinates
import com.loohp.hkbuseta.objects.DataContainer
import com.loohp.hkbuseta.objects.FavouriteRouteStop
import com.loohp.hkbuseta.objects.FavouriteStopMode
import com.loohp.hkbuseta.objects.GMBRegion
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.Preferences
import com.loohp.hkbuseta.objects.Route
import com.loohp.hkbuseta.objects.RouteListType
import com.loohp.hkbuseta.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.objects.RouteSortMode
import com.loohp.hkbuseta.objects.Stop
import com.loohp.hkbuseta.objects.StopInfo
import com.loohp.hkbuseta.objects.getColorHex
import com.loohp.hkbuseta.objects.identifyStopCo
import com.loohp.hkbuseta.objects.isTrain
import com.loohp.hkbuseta.objects.prependTo
import com.loohp.hkbuseta.tiles.EtaTileServiceCommon
import com.loohp.hkbuseta.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.utils.IntUtils
import com.loohp.hkbuseta.utils.LongUtils
import com.loohp.hkbuseta.utils.asMutableStateFlow
import com.loohp.hkbuseta.utils.commonElementPercentage
import com.loohp.hkbuseta.utils.editDistance
import com.loohp.hkbuseta.utils.getCircledNumber
import com.loohp.hkbuseta.utils.getConnectionType
import com.loohp.hkbuseta.utils.getJSONResponse
import com.loohp.hkbuseta.utils.getTextResponse
import com.loohp.hkbuseta.utils.getTextResponseWithPercentageCallback
import com.loohp.hkbuseta.utils.indexOf
import com.loohp.hkbuseta.utils.isBackgroundRestricted
import com.loohp.hkbuseta.utils.postJSONResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import java.util.TreeSet
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.zip.GZIPInputStream
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.roundToInt

class Registry {

    companion object {

        private const val PREFERENCES_FILE_NAME = "preferences.json"
        private const val CHECKSUM_FILE_NAME = "checksum.json"
        private const val DATA_FILE_NAME = "data.json"

        private var INSTANCE: Registry? = null
        private val INSTANCE_LOCK = Any()
        private val ETA_QUERY_EXECUTOR: ExecutorService = ThreadPoolExecutor(64, Int.MAX_VALUE, 60, TimeUnit.SECONDS, SynchronousQueue())

        fun getInstance(context: Context): Registry {
            synchronized(INSTANCE_LOCK) {
                return INSTANCE?: Registry(context, false).apply { INSTANCE = this }
            }
        }

        fun getInstanceNoUpdateCheck(context: Context): Registry {
            synchronized(INSTANCE_LOCK) {
                return INSTANCE?: Registry(context, true).apply { INSTANCE = this }
            }
        }

        fun hasInstanceCreated(): Boolean {
            synchronized(INSTANCE_LOCK) {
                return INSTANCE != null
            }
        }

        fun clearInstance() {
            synchronized(INSTANCE_LOCK) {
                INSTANCE = null
            }
        }

        fun initInstanceWithImportedPreference(context: Context, preferencesData: JSONObject) {
            synchronized(INSTANCE_LOCK) {
                try {
                    INSTANCE = Registry(context, true, preferencesData)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }

        fun invalidateCache(context: Context) {
            try {
                context.applicationContext.deleteFile(CHECKSUM_FILE_NAME)
            } catch (ignore: Throwable) {
            }
        }
    }

    private var PREFERENCES: Preferences? = null
    private var DATA: DataContainer? = null

    private val typhoonInfo: MutableStateFlow<TyphoonInfo> = TyphoonInfo.NULL.asMutableStateFlow()
    private val stateFlow: MutableStateFlow<State> = State.LOADING.asMutableStateFlow()
    private val updatePercentageStateFlow: MutableStateFlow<Float> = 0f.asMutableStateFlow()
    private val preferenceWriteLock = Any()
    private val lastUpdateCheckHolder = AtomicLong(0)
    private val currentChecksumTask = AtomicReference<Future<*>?>(null)
    private val objectCache: MutableMap<String, Any> = ConcurrentHashMap()

    private constructor(context: Context, suppressUpdateCheck: Boolean) {
        ensureData(context, suppressUpdateCheck)
    }

    private constructor(context: Context, suppressUpdateCheck: Boolean, importPreferencesData: JSONObject) {
        importPreference(context, importPreferencesData)
        ensureData(context, suppressUpdateCheck)
    }

    val state: StateFlow<State> get() = stateFlow
    val updatePercentageState: StateFlow<Float> get() = updatePercentageStateFlow
    val lastUpdateCheck: Long get() = lastUpdateCheckHolder.get()

    private fun savePreferences(context: Context) {
        synchronized(preferenceWriteLock) {
            val atomicFile = AtomicFile(context.applicationContext.getFileStreamPath(PREFERENCES_FILE_NAME))
            atomicFile.startWrite().use { fos ->
                PrintWriter(OutputStreamWriter(fos, StandardCharsets.UTF_8)).use { pw ->
                    pw.write(PREFERENCES!!.serialize().toString())
                    pw.flush()
                    atomicFile.finishWrite(fos)
                }
            }
        }
    }

    private fun importPreference(context: Context, preferencesData: JSONObject) {
        val preferences = Preferences.deserialize(preferencesData).cleanForImport()
        PrintWriter(OutputStreamWriter(context.applicationContext.openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8)).use { pw ->
            pw.write(preferences.serialize().toString())
            pw.flush()
        }
    }

    fun exportPreference(): JSONObject {
        synchronized(preferenceWriteLock) {
            return PREFERENCES!!.serialize()
        }
    }

    fun updateTileService() {
        EtaTileServiceCommon.requestTileUpdate(0)
        EtaTileServiceCommon.requestTileUpdate(1)
        EtaTileServiceCommon.requestTileUpdate(2)
        EtaTileServiceCommon.requestTileUpdate(3)
        EtaTileServiceCommon.requestTileUpdate(4)
        EtaTileServiceCommon.requestTileUpdate(5)
        EtaTileServiceCommon.requestTileUpdate(6)
        EtaTileServiceCommon.requestTileUpdate(7)
        EtaTileServiceCommon.requestTileUpdate(8)
    }

    fun updateTileService(favoriteIndex: Int) {
        EtaTileServiceCommon.requestTileUpdate(favoriteIndex)
    }

    fun setLanguage(language: String?, context: Context) {
        Shared.language = language!!
        PREFERENCES!!.language = language
        savePreferences(context)
        updateTileService()
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

    fun clearFavouriteRouteStop(favoriteIndex: Int, context: Context) {
        clearFavouriteRouteStop(favoriteIndex, true, context)
    }

    private fun clearFavouriteRouteStop(favoriteIndex: Int, save: Boolean, context: Context) {
        Shared.updateFavoriteRouteStops { it.remove(favoriteIndex) }
        PREFERENCES!!.favouriteRouteStops.remove(favoriteIndex)
        val changes: MutableMap<Int, List<Int>> = HashMap()
        val deletions: MutableList<Int> = ArrayList()
        for ((key, value) in Shared.getRawEtaTileConfigurations()) {
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
        Shared.updateEtaTileConfigurations {
            it.putAll(changes)
            deletions.forEach { key -> it.remove(key) }
        }
        if (save) {
            savePreferences(context)
            updateTileService(0)
            updateTileService(favoriteIndex)
        }
    }

    fun setFavouriteRouteStop(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, favouriteStopMode: FavouriteStopMode, context: Context) {
        setFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route, favouriteStopMode, bypassEtaTileCheck = false, save = true, context)
    }

    private fun setFavouriteRouteStop(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, favouriteStopMode: FavouriteStopMode, bypassEtaTileCheck: Boolean, save: Boolean, context: Context) {
        val favouriteRouteStop = FavouriteRouteStop(stopId, co, index, stop, route, favouriteStopMode)
        Shared.updateFavoriteRouteStops { it[favoriteIndex] = favouriteRouteStop }
        PREFERENCES!!.favouriteRouteStops[favoriteIndex] = favouriteRouteStop
        if (!bypassEtaTileCheck) {
            val changes: MutableMap<Int, List<Int>> = HashMap()
            val deletions: MutableList<Int> = ArrayList()
            for ((key, value) in Shared.getRawEtaTileConfigurations()) {
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
            Shared.updateEtaTileConfigurations {
                it.putAll(changes)
                deletions.forEach { key -> it.remove(key) }
            }
        }
        if (save) {
            savePreferences(context)
            updateTileService(0)
            updateTileService(favoriteIndex)
        }
    }

    fun clearEtaTileConfiguration(tileId: Int, context: Context) {
        Shared.updateEtaTileConfigurations { it.remove(tileId) }
        PREFERENCES!!.etaTileConfigurations.remove(tileId)
        savePreferences(context)
    }

    fun setEtaTileConfiguration(tileId: Int, favouriteIndexes: List<Int>, context: Context) {
        Shared.updateEtaTileConfigurations { it[tileId] = favouriteIndexes }
        PREFERENCES!!.etaTileConfigurations[tileId] = favouriteIndexes
        savePreferences(context)
        updateTileService(0)
    }

    fun addLastLookupRoute(routeNumber: String?, co: Operator?, meta: String?, context: Context) {
        Shared.addLookupRoute(routeNumber!!, co!!, meta!!)
        val lastLookupRoutes = Shared.getLookupRoutes()
        PREFERENCES!!.lastLookupRoutes.clear()
        PREFERENCES!!.lastLookupRoutes.addAll(lastLookupRoutes)
        savePreferences(context)
    }

    fun clearLastLookupRoutes(context: Context) {
        Shared.clearLookupRoute()
        PREFERENCES!!.lastLookupRoutes.clear()
        savePreferences(context)
    }

    fun setRouteSortModePreference(context: Context, listType: RouteListType, sortMode: RouteSortMode) {
        (Shared.routeSortModePreference as MutableMap<RouteListType, RouteSortMode>)[listType] = sortMode
        PREFERENCES!!.routeSortModePreference.clear()
        PREFERENCES!!.routeSortModePreference.putAll(Shared.routeSortModePreference)
        savePreferences(context)
    }

    fun cancelCurrentChecksumTask() {
        val task = currentChecksumTask.get()
        task?.cancel(true)
    }

    private fun ensureData(context: Context, suppressUpdateCheck: Boolean) {
        if (stateFlow.value == State.READY) {
            return
        }
        if (PREFERENCES != null && DATA != null) {
            return
        }
        val files = listOf(*context.applicationContext.fileList())
        if (files.contains(PREFERENCES_FILE_NAME)) {
            try {
                BufferedReader(InputStreamReader(context.applicationContext.openFileInput(PREFERENCES_FILE_NAME), StandardCharsets.UTF_8)).use { reader ->
                    PREFERENCES = Preferences.deserialize(JSONObject(reader.lines().collect(Collectors.joining())))
                }
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
        Shared.updateEtaTileConfigurations {
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

    fun checkUpdate(context: Context, suppressUpdateCheck: Boolean) {
        stateFlow.value = State.LOADING
        if (!suppressUpdateCheck) {
            lastUpdateCheckHolder.set(System.currentTimeMillis())
        }
        Thread {
            try {
                val files = listOf(*context.applicationContext.fileList())
                val connectionType = context.getConnectionType()
                val updateChecked = AtomicBoolean(false)
                val checksumFetcher: (Boolean) -> String? = { forced ->
                    val future = FutureTask {
                        val version = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
                        getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/checksum.md5") + "_" + version
                    }
                    currentChecksumTask.set(future)
                    if (!forced && files.contains(CHECKSUM_FILE_NAME) && files.contains(DATA_FILE_NAME)) {
                        stateFlow.value = State.UPDATE_CHECKING
                    }
                    try {
                        Thread(future).start()
                        val result = future[10, TimeUnit.SECONDS]
                        updateChecked.set(true)
                        result
                    } catch (e: ExecutionException) {
                        e.printStackTrace()
                        null
                    } catch (e: TimeoutException) {
                        e.printStackTrace()
                        null
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                        null
                    } catch (e: CancellationException) {
                        e.printStackTrace()
                        null
                    } finally {
                        if (stateFlow.value == State.UPDATE_CHECKING) {
                            stateFlow.value = State.LOADING
                        }
                    }
                }
                var cached = false
                var checksum = if (!suppressUpdateCheck && connectionType.hasConnection()) checksumFetcher.invoke(false) else null
                if (files.contains(CHECKSUM_FILE_NAME) && files.contains(DATA_FILE_NAME)) {
                    if (checksum == null) {
                        cached = true
                    } else {
                        BufferedReader(InputStreamReader(context.applicationContext.openFileInput(CHECKSUM_FILE_NAME), StandardCharsets.UTF_8)).use { reader ->
                            val localChecksum = reader.readLine()
                            if (localChecksum == checksum) {
                                cached = true
                            }
                        }
                    }
                }
                if (cached) {
                    if (DATA == null) {
                        try {
                            BufferedReader(InputStreamReader(context.applicationContext.openFileInput(DATA_FILE_NAME), StandardCharsets.UTF_8)).use { reader ->
                                DATA = DataContainer.deserialize(JSONObject(reader.lines().collect(Collectors.joining())))
                            }
                            updateTileService()
                            stateFlow.value = State.READY
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    } else {
                        stateFlow.value = State.READY
                    }
                }
                if (stateFlow.value != State.READY) {
                    if (!connectionType.hasConnection()) {
                        stateFlow.value = State.ERROR
                        try {
                            context.applicationContext.deleteFile(CHECKSUM_FILE_NAME)
                        } catch (ignore: Throwable) {
                        }
                    } else {
                        stateFlow.value = State.UPDATING
                        updatePercentageStateFlow.value = 0f
                        val percentageOffset =
                            if (Shared.favoriteRouteStops.isEmpty()) 0.15f else 0f
                        if (!updateChecked.get()) {
                            checksum = checksumFetcher.invoke(true)
                        }
                        val length: Long = LongUtils.parseOr(getTextResponse("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/size.gz.dat"), -1)
                        val textResponse: String = getTextResponseWithPercentageCallback("https://raw.githubusercontent.com/LOOHP/HK-Bus-ETA-WearOS/data/data.json.gz", length, { GZIPInputStream(it) }, { p -> updatePercentageStateFlow.value = p * 0.75f + percentageOffset })?: throw RuntimeException("Error downloading bus data")
                        DATA = DataContainer.deserialize(JSONObject(textResponse))
                        updatePercentageStateFlow.value = 0.75f + percentageOffset
                        val atomicDataFile = AtomicFile(context.applicationContext.getFileStreamPath(DATA_FILE_NAME))
                        atomicDataFile.startWrite().use { fos ->
                            PrintWriter(OutputStreamWriter(fos, StandardCharsets.UTF_8)).use { pw ->
                                pw.write(textResponse)
                                pw.flush()
                                atomicDataFile.finishWrite(fos)
                            }
                        }
                        updatePercentageStateFlow.value = 0.825f + percentageOffset
                        val atomicChecksumFile = AtomicFile(context.applicationContext.getFileStreamPath(CHECKSUM_FILE_NAME))
                        atomicChecksumFile.startWrite().use { fos ->
                            PrintWriter(OutputStreamWriter(fos, StandardCharsets.UTF_8)).use { pw ->
                                pw.write(checksum ?: "")
                                pw.flush()
                                atomicChecksumFile.finishWrite(fos)
                            }
                        }
                        updatePercentageStateFlow.value = 0.85f + percentageOffset
                        var localUpdatePercentage = updatePercentageStateFlow.value
                        val percentagePerFav = 0.15f / Shared.favoriteRouteStops.size
                        val updatedFavouriteRouteTasks: MutableList<Runnable> = ArrayList()
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
                            updatedFavouriteRouteTasks.forEach { it.run() }
                            savePreferences(context)
                        }
                        updatePercentageStateFlow.value = 1f
                        updateTileService()
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
        }.start()
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
        inputKey = inputKey.lowercase(Locale.getDefault())
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
                    val lineDiff = Shared.getMtrLineSortingIndex(routeNumberA).compareTo(Shared.getMtrLineSortingIndex(routeNumberB))
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
        val hongKongTime = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"))
        val hour = hongKongTime.hour
        val isNight = hour in 1..4
        val weekday = hongKongTime.dayOfWeek
        val date = hongKongTime.toLocalDate()
        val isHoliday = weekday == DayOfWeek.SATURDAY || weekday == DayOfWeek.SUNDAY || DATA!!.dataSheet.holidays.contains(date)
        return NearbyRoutesResult(nearbyRoutes.values.asSequence().sortedWith(Comparator.comparing<RouteSearchResultEntry, Int> { a ->
            val route: Route = a.route!!
            val routeNumber = route.routeNumber
            val bound = route.bound
            val pa = routeNumber[0].toString()
            val sa = routeNumber[routeNumber.length - 1].toString()
            var na: Int = IntUtils.parseOrZero(routeNumber.replace("[^0-9]".toRegex(), ""))
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
        }
        .thenComparing { a -> a.route!!.routeNumber }
        .thenComparing { a -> IntUtils.parseOrZero(a.route!!.serviceType) }
        .thenComparing { a -> a.co }
        .thenComparing(Comparator.comparing<RouteSearchResultEntry, Int> { a ->
            val route: Route = a.route!!
            val bound = route.bound
            if (bound.containsKey(Operator.MTR)) {
                return@comparing Shared.getMtrLineSortingIndex(route.routeNumber)
            }
            10
        }.reversed()))
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
            lists.sortWith(Comparator.comparing { it.second })
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
            result.valuesWithBranchIds().asSequence()
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
        val lines: MutableSet<String> = TreeSet(Comparator.comparing { Shared.getMtrLineSortingIndex(it) })
        val outOfStationLines: MutableSet<String> = TreeSet(Comparator.comparing { Shared.getMtrLineSortingIndex(it) })
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
        return MTRInterchangeData(lines, isOutOfStationPaid, outOfStationLines, hasLightRail)
    }

    @Immutable
    data class MTRInterchangeData(val lines: Set<String>, val isOutOfStationPaid: Boolean, val outOfStationLines: Set<String>, val isHasLightRail: Boolean)

    fun getNoScheduledDepartureMessage(altMessageInput: String?, isAboveTyphoonSignalEight: Boolean, typhoonWarningTitle: String): String {
        var altMessage = altMessageInput
        if (altMessage.isNullOrEmpty()) {
            altMessage = if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次"
        }
        if (isAboveTyphoonSignalEight) {
            altMessage += " ($typhoonWarningTitle)"
        }
        return if (isAboveTyphoonSignalEight) {
            "<span style=\"color: #88A3D1;\">$altMessage</span>"
        } else {
            altMessage
        }
    }

    val cachedTyphoonDataState: StateFlow<TyphoonInfo> get() = typhoonInfo

    val currentTyphoonData: Future<TyphoonInfo> get() {
        val cache = typhoonInfo.value
        if (cache == TyphoonInfo.NULL && System.currentTimeMillis() - cache.lastUpdated < 300000) {
            return CompletableFuture.completedFuture(cache)
        }
        val future = CompletableFuture<TyphoonInfo>()
        Thread {
            val data: JSONObject? = getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=warnsum&lang=" + if (Shared.language == "en") "en" else "tc")
            if (data != null && data.has("WTCSGNL")) {
                val matcher = Pattern.compile("TC([0-9]+)(.*)").matcher(data.optJSONObject("WTCSGNL")!!.optString("code"))
                if (matcher.find() && matcher.group(1) != null) {
                    val signal = matcher.group(1)!!.toInt()
                    val isAboveTyphoonSignalEight = signal >= 8
                    val isAboveTyphoonSignalNine = signal >= 9
                    val typhoonWarningTitle: String = if (Shared.language == "en") {
                        data.optJSONObject("WTCSGNL")!!.optString("type") + " is in force"
                    } else {
                        data.optJSONObject("WTCSGNL")!!.optString("type") + " 現正生效"
                    }
                    val currentTyphoonSignalId = if (signal < 8) {
                        "tc$signal" + (if (matcher.group(2) != null) matcher.group(2) else "").lowercase()
                    } else {
                        "tc" + signal.toString().padStart(2, '0') + (if (matcher.group(2) != null) matcher.group(2) else "").lowercase()
                    }
                    val info = TyphoonInfo.info(isAboveTyphoonSignalEight, isAboveTyphoonSignalNine, typhoonWarningTitle, currentTyphoonSignalId)
                    typhoonInfo.value = info
                    future.complete(info)
                    return@Thread
                }
            }
            val info = TyphoonInfo.none()
            typhoonInfo.value = info
            future.complete(info)
        }.start()
        return future
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

            val NULL = TyphoonInfo(isAboveTyphoonSignalEight = false, isAboveTyphoonSignalNine = false, "", "", 0)

            fun none(): TyphoonInfo {
                return TyphoonInfo(isAboveTyphoonSignalEight = false, isAboveTyphoonSignalNine = false, "", "", System.currentTimeMillis())
            }

            fun info(isAboveTyphoonSignalEight: Boolean, isAboveTyphoonSignalNine: Boolean, typhoonWarningTitle: String, currentTyphoonSignalId: String): TyphoonInfo {
                return TyphoonInfo(isAboveTyphoonSignalEight, isAboveTyphoonSignalNine, typhoonWarningTitle, currentTyphoonSignalId, System.currentTimeMillis())
            }

        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

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

    fun getEta(stopId: String, stopIndex: Int, co: Operator, route: Route, context: Context): PendingETAQueryResult {
        Firebase.analytics.logEvent("eta_query", Bundle().apply {
            putString("by_stop", stopId + "," + stopIndex + "," + route.routeNumber + "," + co.name + "," + route.bound[co])
            putString("by_bound", route.routeNumber + "," + co.name + "," + route.bound[co])
            putString("by_route", route.routeNumber + "," + co.name)
        })
        val pending = PendingETAQueryResult(context, co) {
            val typhoonInfo = currentTyphoonData.get()
            val lines: MutableMap<Int, ETALineEntry> = HashMap()
            var isMtrEndOfLine = false
            var isTyphoonSchedule = false
            var nextCo: Operator = co
            lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
            val language = Shared.language
            if (route.isKmbCtbJoint) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                val jointOperated: MutableSet<JointOperatedEntry> = ConcurrentHashMap.newKeySet()
                val kmbSpecialMessage = AtomicReference<String?>(null)
                val kmbFirstScheduledBus = AtomicLong(Long.MAX_VALUE)
                val kmbFuture = ETA_QUERY_EXECUTOR.submit {
                    val data: JSONObject? = getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/$stopId")
                    val buses = data!!.optJSONArray("data")!!
                    val stopSequences: MutableSet<Int> = HashSet()
                    for (u in 0 until buses.length()) {
                        val bus = buses.optJSONObject(u)
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
                    for (u in 0 until buses.length()) {
                        val bus = buses.optJSONObject(u)
                        if (Operator.KMB === Operator.valueOf(bus.optString("co"))) {
                            val routeNumber = bus.optString("route")
                            val bound = bus.optString("dir")
                            val stopSeq = bus.optInt("seq")
                            if (routeNumber == route.routeNumber && bound == route.bound[Operator.KMB] && stopSeq == matchingSeq && usedRealSeq.add(
                                    bus.optInt("eta_seq")
                                )
                            ) {
                                val eta = bus.optString("eta")
                                val formatter =
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                                if (eta.isNotEmpty() && !eta.equals("null", ignoreCase = true)) {
                                    val mins =
                                        (formatter.parse(eta) { temporal: TemporalAccessor? ->
                                            ZonedDateTime.from(temporal)
                                        }
                                            .toEpochSecond() - Instant.now().epochSecond) / 60.0
                                    val minsRounded = mins.roundToInt()
                                    var message = ""
                                    if (language == "en") {
                                        if (minsRounded > 0) {
                                            message = "<b>$minsRounded</b><small> Min.</small>"
                                        } else if (minsRounded > -60) {
                                            message = "<b>-</b><small> Min.</small>"
                                        }
                                        if (bus.optString("rmk_en").isNotEmpty()) {
                                            message += if (message.isEmpty()) bus.optString("rmk_en") else "<small> (" + bus.optString(
                                                "rmk_en"
                                            ) + ")</small>"
                                        }
                                    } else {
                                        if (minsRounded > 0) {
                                            message = "<b>$minsRounded</b><small> 分鐘</small>"
                                        } else if (minsRounded > -60) {
                                            message = "<b>-</b><small> 分鐘</small>"
                                        }
                                        if (bus.optString("rmk_tc").isNotEmpty()) {
                                            message += if (message.isEmpty()) bus.optString("rmk_tc") else "<small> (" + bus.optString(
                                                "rmk_tc"
                                            ) + ")</small>"
                                        }
                                    }
                                    message = message
                                        .replace("原定", "預定")
                                        .replace("最後班次", "尾班車")
                                        .replace("尾班車已過", "尾班車已過本站")
                                    if ((message.contains("預定班次") || message.contains("Scheduled Bus")) && mins < kmbFirstScheduledBus.get()) {
                                        kmbFirstScheduledBus.set(minsRounded.toLong())
                                    }
                                    jointOperated.add(JointOperatedEntry(mins, minsRounded.toLong(), message, Operator.KMB))
                                } else {
                                    var message = ""
                                    if (language == "en") {
                                        if (!bus.optString("rmk_en").isEmpty()) {
                                            message += if (message.isEmpty()) bus.optString("rmk_en") else "<small> (" + bus.optString("rmk_en") + ")</small>"
                                        }
                                    } else {
                                        if (!bus.optString("rmk_tc").isEmpty()) {
                                            message += if (message.isEmpty()) bus.optString("rmk_tc") else "<small> (" + bus.optString("rmk_tc") + ")</small>"
                                        }
                                    }
                                    message = message
                                        .replace("原定", "預定")
                                        .replace("最後班次", "尾班車")
                                        .replace("尾班車已過", "尾班車已過本站")
                                    message =
                                        if (message.isEmpty() || typhoonInfo.isAboveTyphoonSignalEight && (message == "ETA service suspended" || message == "暫停預報")) {
                                            getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                        } else {
                                            "<b></b>$message"
                                        }
                                    kmbSpecialMessage.set(message)
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
                    val ctbEtaEntries: MutableMap<String?, MutableSet<JointOperatedEntry>> = ConcurrentHashMap()
                    val stopQueryData: MutableList<JSONObject?> = ArrayList()
                    val ctbFutures: MutableList<Future<*>> = ArrayList(ctbStopIds.size)
                    for (ctbStopId in ctbStopIds) {
                        ctbFutures.add(ETA_QUERY_EXECUTOR.submit { stopQueryData.add(getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/$ctbStopId/$routeNumber")) })
                    }
                    for (future in ctbFutures) {
                        future.get()
                    }
                    val stopSequences: MutableMap<String, MutableSet<Int>> = HashMap()
                    val queryBusDests: Array<Array<String?>?> = arrayOfNulls(stopQueryData.size)
                    for (i in stopQueryData.indices) {
                        val data = stopQueryData[i]
                        val buses = data!!.optJSONArray("data")!!
                        val busDests = arrayOfNulls<String>(buses.length())
                        for (u in 0 until buses.length()) {
                            val bus = buses.optJSONObject(u)
                            if (Operator.CTB === Operator.valueOf(bus.optString("co")) && routeNumber == bus.optString("route")) {
                                val rawBusDest = bus.optString("dest_tc").replace(" ", "")
                                val busDest = destKeys.asSequence().minBy { it.editDistance(rawBusDest) }
                                busDests[u] = busDest
                                stopSequences.computeIfAbsent(busDest) { HashSet() }.add(bus.optInt("seq"))
                            }
                        }
                        queryBusDests[i] = busDests
                    }
                    val matchingSeq = stopSequences.entries.asSequence()
                        .map { (key, value) -> key to (value.minByOrNull { (it - stopIndex).absoluteValue }?: -1) }
                        .toMap()
                    for (i in stopQueryData.indices) {
                        val data = stopQueryData[i]!!
                        val buses = data.optJSONArray("data")!!
                        val usedRealSeq: MutableMap<String?, MutableSet<Int>> = HashMap()
                        for (u in 0 until buses.length()) {
                            val bus = buses.optJSONObject(u)
                            if (Operator.CTB === Operator.valueOf(bus.optString("co")) && routeNumber == bus.optString("route")) {
                                val busDest = queryBusDests[i]!![u]!!
                                val stopSeq = bus.optInt("seq")
                                if ((stopSeq == (matchingSeq[busDest]?: 0)) && usedRealSeq.computeIfAbsent(busDest) { HashSet() }.add(bus.optInt("eta_seq"))) {
                                    val eta = bus.optString("eta")
                                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                                    if (eta.isNotEmpty() && !eta.equals("null", ignoreCase = true)) {
                                        val mins = (formatter.parse(eta) { temporal: TemporalAccessor? -> ZonedDateTime.from(temporal) }.toEpochSecond() - Instant.now().epochSecond) / 60.0
                                        val minsRounded = mins.roundToInt()
                                        var message = ""
                                        if (language == "en") {
                                            if (minsRounded > 0) {
                                                message = "<b>$minsRounded</b><small> Min.</small>"
                                            } else if (minsRounded > -60) {
                                                message = "<b>-</b><small> Min.</small>"
                                            }
                                            if (bus.optString("rmk_en").isNotEmpty()) {
                                                message += if (message.isEmpty()) bus.optString("rmk_en") else "<small> (" + bus.optString("rmk_en") + ")</small>"
                                            }
                                        } else {
                                            if (minsRounded > 0) {
                                                message = "<b>$minsRounded</b><small> 分鐘</small>"
                                            } else if (minsRounded > -60) {
                                                message = "<b>-</b><small> 分鐘</small>"
                                            }
                                            if (bus.optString("rmk_tc").isNotEmpty()) {
                                                message += if (message.isEmpty()) bus.optString("rmk_tc") else "<small> (" + bus.optString("rmk_tc") + ")</small>"
                                            }
                                        }
                                        message = message
                                            .replace("原定", "預定")
                                            .replace("最後班次", "尾班車")
                                            .replace("尾班車已過", "尾班車已過本站")
                                        ctbEtaEntries.computeIfAbsent(busDest) { ConcurrentHashMap.newKeySet() }.add(JointOperatedEntry(mins, minsRounded.toLong(), message, Operator.CTB))
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
                kmbFuture.get()
                if (jointOperated.isEmpty()) {
                    if (kmbSpecialMessage.get() == null || kmbSpecialMessage.get()!!.isEmpty()) {
                        lines[1] = ETALineEntry.textEntry(getNoScheduledDepartureMessage(null, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle))
                    } else {
                        lines[1] = ETALineEntry.textEntry(kmbSpecialMessage.get())
                    }
                } else {
                    var counter = 0
                    val itr = jointOperated.asSequence().sorted().iterator()
                    while (itr.hasNext()) {
                        val entry = itr.next()
                        val mins = entry.mins
                        val minsRounded = entry.minsRounded
                        var message = "<b></b>" + entry.line.replace("(尾班車)", "").replace("(Final Bus)", "").trim { it <= ' ' }
                        val entryCo = entry.co
                        if (minsRounded > kmbFirstScheduledBus.get() && !(message.contains("預定班次") || message.contains("Scheduled Bus"))) {
                            message += "<small>" + (if (Shared.language == "en") " (Scheduled Bus)" else " (預定班次)") + "</small>"
                        }
                        message += if (entryCo === Operator.KMB) {
                            if (Shared.getKMBSubsidiary(route.routeNumber) === KMBSubsidiary.LWB) {
                                "<small>" + (if (Shared.language == "en") " - LWB" else " - 龍運") + "</small>"
                            } else {
                                "<small>" + (if (Shared.language == "en") " - KMB" else " - 九巴") + "</small>"
                            }
                        } else {
                            "<small>" + (if (Shared.language == "en") " - CTB" else " - 城巴") + "</small>"
                        }
                        val seq = ++counter
                        if (seq == 1) {
                            nextCo = entryCo
                        }
                        lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded)
                    }
                }
            } else if (co === Operator.KMB) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                val data: JSONObject? = getJSONResponse("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/$stopId")
                val buses = data!!.optJSONArray("data")!!
                val stopSequences: MutableSet<Int> = HashSet()
                for (u in 0 until buses.length()) {
                    val bus = buses.optJSONObject(u)
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
                for (u in 0 until buses.length()) {
                    val bus = buses.optJSONObject(u)
                    if (Operator.KMB === Operator.valueOf(bus.optString("co"))) {
                        val routeNumber = bus.optString("route")
                        val bound = bus.optString("dir")
                        val stopSeq = bus.optInt("seq")
                        if (routeNumber == route.routeNumber && bound == route.bound[Operator.KMB] && stopSeq == matchingSeq) {
                            val seq = ++counter
                            if (usedRealSeq.add(bus.optInt("eta_seq"))) {
                                val eta = bus.optString("eta")
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                                val mins: Double = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) -999.0 else (formatter.parse(eta) { temporal: TemporalAccessor? -> ZonedDateTime.from(temporal) }.toEpochSecond() - Instant.now().epochSecond) / 60.0
                                val minsRounded = mins.roundToInt()
                                var message = ""
                                if (language == "en") {
                                    if (minsRounded > 0) {
                                        message = "<b>$minsRounded</b><small> Min.</small>"
                                    } else if (minsRounded > -60) {
                                        message = "<b>-</b><small> Min.</small>"
                                    }
                                    if (!bus.optString("rmk_en").isEmpty()) {
                                        message += if (message.isEmpty()) bus.optString("rmk_en") else "<small> (" + bus.optString("rmk_en") + ")</small>"
                                    }
                                } else {
                                    if (minsRounded > 0) {
                                        message = "<b>$minsRounded</b><small> 分鐘</small>"
                                    } else if (minsRounded > -60) {
                                        message = "<b>-</b><small> 分鐘</small>"
                                    }
                                    if (!bus.optString("rmk_tc").isEmpty()) {
                                        message += if (message.isEmpty()) bus.optString("rmk_tc") else "<small> (" + bus.optString("rmk_tc") + ")</small>"
                                    }
                                }
                                message = message
                                    .replace("原定", "預定")
                                    .replace("最後班次", "尾班車")
                                    .replace("尾班車已過", "尾班車已過本站")
                                message = if (message.isEmpty() || typhoonInfo.isAboveTyphoonSignalEight && (message == "ETA service suspended" || message == "暫停預報")) {
                                        if (seq == 1) {
                                            getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                        } else {
                                            "<b></b>-"
                                        }
                                    } else {
                                        "<b></b>$message"
                                    }
                                lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 0), mins, minsRounded.toLong())
                            }
                        }
                    }
                }
            } else if (co === Operator.CTB) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                val routeNumber = route.routeNumber
                val routeBound = route.bound[Operator.CTB]
                val data: JSONObject? = getJSONResponse("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/$stopId/$routeNumber")
                val buses = data!!.optJSONArray("data")!!
                val stopSequences: MutableSet<Int> = HashSet()
                for (u in 0 until buses.length()) {
                    val bus = buses.optJSONObject(u)
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
                for (u in 0 until buses.length()) {
                    val bus = buses.optJSONObject(u)
                    if (Operator.CTB === Operator.valueOf(bus.optString("co"))) {
                        val bound = bus.optString("dir")
                        val stopSeq = bus.optInt("seq")
                        if (routeNumber == bus.optString("route") && (routeBound!!.length > 1 || bound == routeBound) && stopSeq == matchingSeq) {
                            val seq = ++counter
                            if (usedRealSeq.add(bus.optInt("eta_seq"))) {
                                val eta = bus.optString("eta")
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                                val mins: Double = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) -999.0 else (formatter.parse(eta) { temporal: TemporalAccessor? -> ZonedDateTime.from(temporal) }.toEpochSecond() - Instant.now().epochSecond) / 60.0
                                val minsRounded = mins.roundToInt()
                                var message = ""
                                if (language == "en") {
                                    if (minsRounded > 0) {
                                        message = "<b>$minsRounded</b><small> Min.</small>"
                                    } else if (minsRounded > -60) {
                                        message = "<b>-</b><small> Min.</small>"
                                    }
                                    if (!bus.optString("rmk_en").isEmpty()) {
                                        message += if (message.isEmpty()) bus.optString("rmk_en") else "<small> (" + bus.optString("rmk_en") + ")</small>"
                                    }
                                } else {
                                    if (minsRounded > 0) {
                                        message = "<b>$minsRounded</b><small> 分鐘</small>"
                                    } else if (minsRounded > -60) {
                                        message = "<b>-</b><small> 分鐘</small>"
                                    }
                                    if (!bus.optString("rmk_tc").isEmpty()) {
                                        message += if (message.isEmpty()) bus.optString("rmk_tc") else "<small> (" + bus.optString("rmk_tc") + ")</small>"
                                    }
                                }
                                message = message
                                    .replace("原定", "預定")
                                    .replace("最後班次", "尾班車")
                                    .replace("尾班車已過", "尾班車已過本站")
                                message = if (message.isEmpty()) {
                                    if (seq == 1) {
                                        getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                    } else {
                                        "<b></b>-"
                                    }
                                } else {
                                    "<b></b>$message"
                                }
                                lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 0), mins, minsRounded.toLong())
                            }
                        }
                    }
                }
            } else if (co === Operator.NLB) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                val data: JSONObject? = getJSONResponse("https://rt.data.gov.hk/v2/transport/nlb/stop.php?action=estimatedArrivals&routeId=${route.nlbId}&stopId=$stopId&language=${Shared.language}")
                if (data != null && data.length() > 0 && data.has("estimatedArrivals")) {
                    val buses = data.optJSONArray("estimatedArrivals")!!
                    for (u in 0 until buses.length()) {
                        val bus = buses.optJSONObject(u)
                        val seq = u + 1
                        val eta = bus.optString("estimatedArrivalTime") + "+08:00"
                        val variant = bus.optString("routeVariantName").trim { it <= ' ' }
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")
                        val mins: Double = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) -999.0 else (formatter.parse(eta) { temporal: TemporalAccessor? -> ZonedDateTime.from(temporal) }.toEpochSecond() - Instant.now().epochSecond) / 60.0
                        val minsRounded = mins.roundToInt()
                        var message = ""
                        if (language == "en") {
                            if (minsRounded > 0) {
                                message = "<b>$minsRounded</b><small> Min.</small>"
                            } else if (minsRounded > -60) {
                                message = "<b>-</b><small> Min.</small>"
                            }
                        } else {
                            if (minsRounded > 0) {
                                message = "<b>$minsRounded</b><small> 分鐘</small>"
                            } else if (minsRounded > -60) {
                                message = "<b>-</b><small> 分鐘</small>"
                            }
                        }
                        if (variant.isNotEmpty()) {
                            message += if (message.isEmpty()) variant else "<small> ($variant)</small>"
                        }
                        message = message
                            .replace("原定", "預定")
                            .replace("最後班次", "尾班車")
                            .replace("尾班車已過", "尾班車已過本站")
                        message = if (message.isEmpty()) {
                            if (seq == 1) {
                                getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                            } else {
                                "<b></b>-"
                            }
                        } else {
                            "<b></b>$message"
                        }
                        lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 0), mins, minsRounded.toLong())
                    }
                }
            } else if (co === Operator.MTR_BUS) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                val routeNumber = route.routeNumber
                val body = JSONObject()
                body.put("language", Shared.language)
                body.put("routeName", routeNumber)
                val data: JSONObject? = postJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/bus/getSchedule", body)
                val busStops = data!!.optJSONArray("busStop")!!
                for (k in 0 until busStops.length()) {
                    val busStop = busStops.optJSONObject(k)
                    val buses = busStop.optJSONArray("bus")!!
                    val busStopId = busStop.optString("busStopId")
                    for (u in 0 until buses.length()) {
                        val bus = buses.optJSONObject(u)
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
                            var message = ""
                            if (language == "en") {
                                if (minsRounded > 0) {
                                    message = "<b>$minsRounded</b><small> Min.</small>"
                                } else if (minsRounded > -60) {
                                    message = "<b>-</b><small> Min.</small>"
                                }
                            } else {
                                if (minsRounded > 0) {
                                    message = "<b>$minsRounded</b><small> 分鐘</small>"
                                } else if (minsRounded > -60) {
                                    message = "<b>-</b><small> 分鐘</small>"
                                }
                            }
                            if (remark.isNotEmpty()) {
                                message += if (message.isEmpty()) remark else "<small> ($remark)</small>"
                            }
                            message = message
                                .replace("原定", "預定")
                                .replace("最後班次", "尾班車")
                                .replace("尾班車已過", "尾班車已過本站")
                            message = if (message.isEmpty()) {
                                if (seq == 1) {
                                    getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                                } else {
                                    "<b></b>-"
                                }
                            } else {
                                "<b></b>$message"
                            }
                            lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded, 0), mins, minsRounded)
                        }
                    }
                }
            } else if (co === Operator.GMB) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalEight
                val data: JSONObject? = getJSONResponse("https://data.etagmb.gov.hk/eta/stop/$stopId")
                val stopSequences: MutableSet<Int> = HashSet()
                val busList: MutableList<Triple<Int, Double, JSONObject>> = ArrayList()
                for (i in 0 until data!!.optJSONArray("data")!!.length()) {
                    val routeData = data.optJSONArray("data")!!.optJSONObject(i)
                    val buses = routeData.optJSONArray("eta")
                    val filteredEntry = DATA!!.dataSheet.routeList.values.firstOrNull { it.bound.containsKey(Operator.GMB) && it.gtfsId == routeData.optString("route_id") }
                    if (filteredEntry != null && buses != null) {
                        val routeNumber = filteredEntry.routeNumber
                        val stopSeq = routeData.optInt("stop_seq")
                        for (u in 0 until buses.length()) {
                            val bus = buses.optJSONObject(u)
                            if (routeNumber == route.routeNumber) {
                                val eta = bus.optString("timestamp")
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                                val mins: Double = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) -999.0 else (formatter.parse(eta) { temporal: TemporalAccessor? -> ZonedDateTime.from(temporal) }.toEpochSecond() - Instant.now().epochSecond) / 60.0
                                stopSequences.add(stopSeq)
                                busList.add(Triple(stopSeq, mins, bus))
                            }
                        }
                    }
                }
                if (stopSequences.size > 1) {
                    val matchingSeq = stopSequences.minByOrNull { (it - stopIndex).absoluteValue }?: -1
                    busList.removeIf { it.first != matchingSeq }
                }
                busList.sortWith(Comparator.comparing { it.second })
                for (i in busList.indices) {
                    val (_, mins, bus) = busList[i]
                    val seq = i + 1
                    var remark = if (language == "en") bus.optString("remarks_en") else bus.optString("remarks_tc")
                    if (remark.equals("null", ignoreCase = true)) {
                        remark = ""
                    }
                    val minsRounded = mins.roundToInt()
                    var message = ""
                    if (language == "en") {
                        if (minsRounded > 0) {
                            message = "<b>$minsRounded</b><small> Min.</small>"
                        } else if (minsRounded > -60) {
                            message = "<b>-</b><small> Min.</small>"
                        }
                    } else {
                        if (minsRounded > 0) {
                            message = "<b>$minsRounded</b><small> 分鐘</small>"
                        } else if (minsRounded > -60) {
                            message = "<b>-</b><small> 分鐘</small>"
                        }
                    }
                    if (remark.isNotEmpty()) {
                        message += if (message.isEmpty()) remark else "<small> ($remark)</small>"
                    }
                    message = message
                        .replace("原定", "預定")
                        .replace("最後班次", "尾班車")
                        .replace("尾班車已過", "尾班車已過本站")
                    message = if (message.isEmpty()) {
                        if (seq == 1) {
                            getNoScheduledDepartureMessage(message, typhoonInfo.isAboveTyphoonSignalEight, typhoonInfo.typhoonWarningTitle)
                        } else {
                            "<b></b>-"
                        }
                    } else {
                        "<b></b>$message"
                    }
                    lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 0), mins, minsRounded.toLong())
                }
            } else if (co === Operator.LRT) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine
                val stopsList = route.stops[Operator.LRT]!!
                if (stopsList.indexOf(stopId) + 1 >= stopsList.size) {
                    isMtrEndOfLine = true
                    lines[1] =
                        ETALineEntry.textEntry(if (Shared.language == "en") "End of Line" else "終點站")
                } else {
                    val hongKongTime = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"))
                    val hour = hongKongTime.hour
                    val results: MutableList<LrtETAData> = ArrayList()
                    val data: JSONObject? = getJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=${stopId.substring(2)}")
                    if (data!!.optInt("status") != 0) {
                        val platformList = data.optJSONArray("platform_list")!!
                        for (i in 0 until platformList.length()) {
                            val platform = platformList.optJSONObject(i)
                            val platformNumber = platform.optInt("platform_id")
                            val routeList = platform.optJSONArray("route_list")
                            if (routeList != null) {
                                for (u in 0 until routeList.length()) {
                                    val routeData = routeList.optJSONObject(u)
                                    val routeNumber = routeData.optString("route_no")
                                    val destCh = routeData.optString("dest_ch")
                                    if (routeNumber == route.routeNumber && isLrtStopOnOrAfter(stopId, destCh, route)) {
                                        val matcher = Pattern.compile("([0-9]+) *min").matcher(routeData.optString("time_en"))
                                        val mins = if (matcher.find()) matcher.group(1)!!.toLong() else 0
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
                        val lineColor: String = co.getColorHex(route.routeNumber, 0xFFFFFFFFL)
                        results.sortWith(Comparator.naturalOrder())
                        for (i in results.indices) {
                            val lrt = results[i]
                            val seq = i + 1
                            var minsMessage = lrt.etaMessage
                            if (minsMessage == "-") {
                                minsMessage = if (Shared.language == "en") "Departing" else "正在離開"
                            }
                            minsMessage = if (minsMessage == "即將抵達" || minsMessage == "Arriving" || minsMessage == "正在離開" || minsMessage == "Departing") {
                                "<b>$minsMessage</b>"
                            } else {
                                minsMessage.replace("^([0-9]+)".toRegex(), "<b>$1</b>")
                                    .replace(" min", "<small> Min.</small>")
                                    .replace(" 分鐘", "<small> 分鐘</small>")
                            }
                            val cartsMessage = StringBuilder(lrt.trainLength * 21)
                            for (u in 0 until lrt.trainLength) {
                                cartsMessage.append("<img src=\"lrv\">")
                            }
                            if (lrt.trainLength == 1) {
                                cartsMessage.append("<img src=\"lrv_empty\">")
                            }
                            val mins = lrt.eta
                            val message = "<b></b><span style=\"color: $lineColor\">" + lrt.platformNumber.getCircledNumber() + "</span> " + cartsMessage + " " + minsMessage
                            lines[seq] = ETALineEntry.etaEntry(message, toShortText(mins, 1), mins.toDouble(), mins)
                        }
                    }
                }
            } else if (co === Operator.MTR) {
                isTyphoonSchedule = typhoonInfo.isAboveTyphoonSignalNine
                val lineName = route.routeNumber
                val lineColor: String = co.getColorHex(lineName, 0xFFFFFFFFL)
                val bound = route.bound[Operator.MTR]
                if (isMtrStopEndOfLine(stopId, lineName, bound)) {
                    isMtrEndOfLine = true
                    lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "End of Line" else "終點站")
                } else {
                    val hongKongTime = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"))
                    val hour = hongKongTime.hour
                    val dayOfWeek = hongKongTime.dayOfWeek
                    val data: JSONObject? = getJSONResponse("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=$lineName&sta=$stopId")
                    if (data!!.optInt("status") == 0) {
                        lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Server unable to provide data" else "系統未能提供資訊")
                    } else {
                        val lineStops = data.optJSONObject("data")!!.optJSONObject("$lineName-$stopId")
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
                            } else if (hour < 3 || stopId == "LMC" && hour >= 10) {
                                lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Last train has departed" else "尾班車已開出")
                            } else if (hour < 6) {
                                lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service has not yet started" else "今日服務尚未開始")
                            } else {
                                lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Server unable to provide data" else "系統未能提供資訊")
                            }
                        } else {
                            val delayed = data.optString("isdelay", "N") != "N"
                            val dir = if (bound == "UT") "UP" else "DOWN"
                            val trains = lineStops.optJSONArray(dir)
                            if (trains == null || trains.length() == 0) {
                                if (stopId == "RAC") {
                                    if (!raceDay) {
                                        lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service on race days only" else "僅在賽馬日提供服務")
                                    } else if (hour >= 15 || hour < 3) {
                                        lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Last train has departed" else "尾班車已開出")
                                    } else {
                                        lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service has not yet started" else "今日服務尚未開始")
                                    }
                                } else if (hour < 3 || stopId == "LMC" && hour >= 10) {
                                    lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Last train has departed" else "尾班車已開出")
                                } else if (hour < 6) {
                                    lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Service has not yet started" else "今日服務尚未開始")
                                } else {
                                    lines[1] = ETALineEntry.textEntry(if (Shared.language == "en") "Server unable to provide data" else "系統未能提供資訊")
                                }
                            } else {
                                for (u in 0 until trains.length()) {
                                    val trainData = trains.optJSONObject(u)
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
                                    if (specialRoute.isNotEmpty() && !isMtrStopOnOrAfter(stopId, specialRoute, lineName, bound)) {
                                        val via: String = DATA!!.dataSheet.stopList[specialRoute]!!.name[Shared.language]
                                        dest += "<small>" + (if (Shared.language == "en") " via " else " 經") + via + "</small>"
                                    }
                                    val timeType = trainData.optString("timeType")
                                    val eta = trainData.optString("time")
                                    @SuppressLint("SimpleDateFormat")
                                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                    format.timeZone = TimeZone.getTimeZone(hongKongTime.zone)
                                    val mins = (format.parse(eta)!!.time - Instant.now().toEpochMilli()) / 60000.0
                                    val minsRounded = mins.roundToInt()
                                    val minsMessage = if (minsRounded > 59) {
                                        "<b>" + hongKongTime.plusMinutes(minsRounded.toLong()).format(DateTimeFormatter.ofPattern("HH:mm")) + "</b>"
                                    } else if (minsRounded > 1) {
                                        "<b>" + minsRounded + "</b><small>" + (if (Shared.language == "en") " Min." else " 分鐘") + "</small>"
                                    } else if (minsRounded == 1 && timeType != "D") {
                                        "<b>" + (if (Shared.language == "en") "Arriving" else "即將抵達") + "</b>"
                                    } else {
                                        "<b>" + (if (Shared.language == "en") "Departing" else "正在離開") + "</b>"
                                    }
                                    var message = "<b></b><span style=\"color: $lineColor\">${platform.getCircledNumber()}</span> $dest $minsMessage"
                                    if (seq == 1) {
                                        if (delayed) {
                                            message += "<small>" + (if (Shared.language == "en") " (Delayed)" else " (服務延誤)") + "</small>"
                                        }
                                    }
                                    lines[seq] = ETALineEntry.etaEntry(message, toShortText(minsRounded.toLong(), 1), mins, minsRounded.toLong())
                                }
                            }
                        }
                    }
                }
            }
            ETAQueryResult.result(isMtrEndOfLine, isTyphoonSchedule, nextCo, lines)
        }
        ETA_QUERY_EXECUTOR.submit(pending)
        return pending
    }

    private fun toShortText(minsRounded: Long, arrivingThreshold: Long): ETAShortText {
        return ETAShortText(
            if (minsRounded <= arrivingThreshold) "-" else minsRounded.toString(),
            if (Shared.language == "en") "Min." else "分鐘"
        )
    }

    @Immutable
    data class JointOperatedEntry(val mins: Double, val minsRounded: Long, val line: String, val co: Operator) : Comparable<JointOperatedEntry> {

        override operator fun compareTo(other: JointOperatedEntry): Int {
            return mins.compareTo(other.mins)
        }

    }

    @Immutable
    class PendingETAQueryResult(
        private val context: Context,
        private val co: Operator,
        callable: Callable<ETAQueryResult>
    ) : FutureTask<ETAQueryResult>(callable) {

        private val errorResult: ETAQueryResult @SuppressLint("RestrictedApi") get() {
            val restrictionType = if (context is ComponentActivity) BackgroundRestrictionType.NONE else context.isBackgroundRestricted()
            return ETAQueryResult.connectionError(restrictionType, co)
        }

        override fun get(): ETAQueryResult {
            return try {
                super.get()!!
            } catch (e: ExecutionException) {
                e.printStackTrace()
                try { cancel(true) } catch (ignore: Throwable) { }
                errorResult
            } catch (e: InterruptedException) {
                e.printStackTrace()
                try { cancel(true) } catch (ignore: Throwable) { }
                errorResult
            } catch (e: CancellationException) {
                e.printStackTrace()
                try { cancel(true) } catch (ignore: Throwable) { }
                errorResult
            }
        }

        override fun get(timeout: Long, unit: TimeUnit): ETAQueryResult {
            return try {
                super.get(timeout, unit)!!
            } catch (e: ExecutionException) {
                e.printStackTrace()
                try { cancel(true) } catch (ignore: Throwable) { }
                errorResult
            } catch (e: InterruptedException) {
                e.printStackTrace()
                try { cancel(true) } catch (ignore: Throwable) { }
                errorResult
            } catch (e: TimeoutException) {
                e.printStackTrace()
                try { cancel(true) } catch (ignore: Throwable) { }
                errorResult
            } catch (e: CancellationException) {
                e.printStackTrace()
                try { cancel(true) } catch (ignore: Throwable) { }
                errorResult
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
            private val COMPARATOR = Comparator.comparing { it: LrtETAData -> it.eta }.thenComparing { it: LrtETAData -> it.platformNumber }
        }

        override operator fun compareTo(other: LrtETAData): Int {
            return COMPARATOR.compare(this, other)
        }

    }

    @Immutable
    class ETALineEntry private constructor(
        val text: String?,
        val shortText: ETAShortText,
        val eta: Double,
        val etaRounded: Long
    ) {

        companion object {

            val EMPTY = ETALineEntry("-", ETAShortText.EMPTY, -1.0, -1)

            fun textEntry(text: String?): ETALineEntry {
                return ETALineEntry(text, ETAShortText.EMPTY, -1.0, -1)
            }

            fun etaEntry(text: String?, shortText: ETAShortText, eta: Double, etaRounded: Long): ETALineEntry {
                return if (etaRounded > -60) {
                    ETALineEntry(text, shortText, eta.coerceAtLeast(0.0), etaRounded.coerceAtLeast(0))
                } else {
                    ETALineEntry(text, shortText, -1.0, -1)
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

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
            if (javaClass != other?.javaClass) return false

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

        val rawLines: Map<Int, ETALineEntry> = Collections.unmodifiableMap(lines)
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
            if (javaClass != other?.javaClass) return false

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
