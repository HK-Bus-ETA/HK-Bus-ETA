package com.loohp.hkbuseta.common.utils.widget

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import co.touchlab.stately.concurrency.synchronize
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.WidgetPrecomputedData
import com.loohp.hkbuseta.common.objects.endOfLineText
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.containsKeyAndNotNull
import com.loohp.hkbuseta.common.utils.currentEpochSeconds
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.distinctBy
import com.loohp.hkbuseta.common.utils.doRetry
import com.loohp.hkbuseta.common.utils.editDistance
import com.loohp.hkbuseta.common.utils.epochSeconds
import com.loohp.hkbuseta.common.utils.getJSONResponse
import com.loohp.hkbuseta.common.utils.hongKongTimeZone
import com.loohp.hkbuseta.common.utils.isNotNullAndNotEmpty
import com.loohp.hkbuseta.common.utils.minus
import com.loohp.hkbuseta.common.utils.nextLocalDateTimeAfter
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optInt
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.parseInstant
import com.loohp.hkbuseta.common.utils.parseLocalDateTime
import com.loohp.hkbuseta.common.utils.postJSONResponse
import com.loohp.hkbuseta.common.utils.remove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


data class WidgetETAResult(
    val lines: List<WidgetLineEntry>,
    val hasServices: Boolean
)

fun getEta(stopId: String, stopIndex: Int, co: Operator, route: Route, precomputedData: WidgetPrecomputedData): WidgetETAResult {
    return runBlocking {
        try {
            when {
                route.isKmbCtbJoint -> etaQueryKmbCtbJoint(this, stopId, stopIndex, route, precomputedData)
                co === Operator.KMB -> etaQueryKmb(stopId, stopIndex, route, precomputedData)
                co === Operator.CTB -> etaQueryCtb(stopId, stopIndex, route)
                co === Operator.NLB -> etaQueryNlb(stopId, route, precomputedData)
                co === Operator.MTR_BUS -> etaQueryMtrBus(route, precomputedData)
                co === Operator.GMB -> etaQueryGmb(stopId, stopIndex, co, route, precomputedData)
                co === Operator.LRT -> etaQueryLrt(stopId, route, precomputedData)
                co === Operator.MTR -> etaQueryMtr(stopId, route, precomputedData)
                co === Operator.SUNFERRY -> etaQuerySunFerry(stopId, co, route)
                co === Operator.HKKF -> etaQueryHkkf(co, route)
                co === Operator.FORTUNEFERRY -> etaQueryFortuneFerry(stopId, co, route, precomputedData)
                else -> throw IllegalStateException("Unknown Operator ${co.name}")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            WidgetETAResult(emptyList(), false)
        }
    }
}

private suspend fun etaQueryKmbCtbJoint(scope: CoroutineScope, stopId: String, stopIndex: Int, route: Route, precomputedData: WidgetPrecomputedData): WidgetETAResult {
    val jointOperated: MutableSet<LocalDateTime> = ConcurrentMutableSet()
    val kmbFuture = scope.launch {
        val data = getJSONResponse<JsonObject>("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/$stopId")
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
                        if (mins.isFinite() && mins < -10.0) continue
                        jointOperated.add(currentLocalDateTime(mins.minutes))
                    }
                }
            }
        }
    }
    run {
        val routeNumber = route.routeNumber
        val ctbStopIds = precomputedData.ctbStopIds?: emptyList()
        val (first, second) = precomputedData.ctbByDirectionResult!!
        val destKeys = second.asSequence().map { it.zh.remove(" ") }.toSet()
        val ctbEtaEntries: ConcurrentMutableMap<String?, MutableSet<LocalDateTime>> = ConcurrentMutableMap()
        val stopQueryData = buildList {
            for (ctbStopId in ctbStopIds) {
                add(scope.async { getJSONResponse<JsonObject>("https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/$ctbStopId/$routeNumber") })
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
                            if (mins.isFinite() && mins < -10.0) continue
                            ctbEtaEntries.synchronize {
                                ctbEtaEntries.getOrPut(busDest) { ConcurrentMutableSet() }.add(
                                    currentLocalDateTime(mins.minutes)
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
    return WidgetETAResult(
        lines = jointOperated.asSequence().sorted().map { it.toWidgetLineEntry() }.toList(),
        hasServices = jointOperated.isNotEmpty()
    )
}

private suspend fun etaQueryKmb(rawStopId: String, stopIndex: Int, route: Route, precomputedData: WidgetPrecomputedData): WidgetETAResult {
    val stopIds = precomputedData.kmbStopIds?: listOf(rawStopId)
    val unsortedLines: MutableList<LocalDateTime> = mutableListOf()
    for (stopId in stopIds) {
        val data = getJSONResponse<JsonObject>("https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/$stopId")
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
                        unsortedLines.add(currentLocalDateTime(mins.minutes))
                    }
                }
            }
        }
    }
    return WidgetETAResult(
        lines = unsortedLines.map { it.toWidgetLineEntry() },
        hasServices = unsortedLines.isNotEmpty()
    )
}

private suspend fun etaQueryCtb(stopId: String, stopIndex: Int, route: Route): WidgetETAResult {
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
    val lines = mutableListOf<WidgetLineEntry>()
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
                    lines.add(currentLocalDateTime(mins.minutes).toWidgetLineEntry())
                }
            }
        }
    }
    return WidgetETAResult(
        lines = lines,
        hasServices = lines.isNotEmpty()
    )
}

private suspend fun etaQueryNlb(stopId: String, route: Route, precomputedData: WidgetPrecomputedData): WidgetETAResult {
    val language = precomputedData.language
    val lines = mutableListOf<WidgetLineEntry>()
    val data = getJSONResponse<JsonObject>("https://rt.data.gov.hk/v2/transport/nlb/stop.php?action=estimatedArrivals&routeId=${route.nlbId}&stopId=$stopId&language=${language}")
    if (data.isNotNullAndNotEmpty() && data.contains("estimatedArrivals")) {
        val buses = data.optJsonArray("estimatedArrivals")!!
        for (u in 0 until buses.size) {
            val bus = buses.optJsonObject(u)!!
            val eta = bus.optString("estimatedArrivalTime")
            val mins = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) Double.NEGATIVE_INFINITY else (eta.let { "${it.substring(0, 10)}T${it.substring(11)}" }.parseLocalDateTime().toInstant(hongKongTimeZone).epochSeconds - currentEpochSeconds) / 60.0
            if (mins.isFinite() && mins < -10.0) continue
            lines.add(currentLocalDateTime(mins.minutes).toWidgetLineEntry())
        }
    }
    return WidgetETAResult(
        lines = lines,
        hasServices = lines.isNotEmpty()
    )
}

private suspend fun etaQueryMtrBus(route: Route, precomputedData: WidgetPrecomputedData): WidgetETAResult {
    val language = precomputedData.language
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
    val lines = mutableListOf<WidgetLineEntry>()
    val busStops = data!!.optJsonArray("busStop")
    if (busStops != null) {
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
                if (precomputedData.mtrBusStopAlias?.contains(busStopId) == true) {
                    lines.add(currentLocalDateTime(mins.minutes).toWidgetLineEntry())
                }
            }
        }
    }
    return WidgetETAResult(
        lines = lines,
        hasServices = lines.isNotEmpty()
    )
}

@Immutable
data class GMBETAEntry(
    val stopSeq: Int,
    val mins: Double,
    val gtfsId: String
)

private suspend fun etaQueryGmb(stopId: String, stopIndex: Int, co: Operator, route: Route, precomputedData: WidgetPrecomputedData): WidgetETAResult {
    val branches = precomputedData.gmbBranches?: listOf(route.gtfsId)
    val data = getJSONResponse<JsonObject>("https://data.etagmb.gov.hk/eta/stop/$stopId")
    val stopSequences: MutableMap<String, MutableSet<Int>> = mutableMapOf()
    val busList: MutableList<GMBETAEntry> = mutableListOf()
    for (i in 0 until data!!.optJsonArray("data")!!.size) {
        val routeData = data.optJsonArray("data")!!.optJsonObject(i)!!
        val buses = routeData.optJsonArray("eta")
        val bound = if (routeData.optInt("route_seq") <= 1) "O" else "I"
        val routeId = routeData.optString("route_id")
        if (route.bound[co] == bound && branches.contains(routeId) && buses != null) {
            val routeNumber = route.routeNumber
            val stopSeq = routeData.optInt("stop_seq")
            for (u in 0 until buses.size) {
                val bus = buses.optJsonObject(u)!!
                if (routeNumber == route.routeNumber) {
                    val eta = bus.optString("timestamp")
                    val mins = if (eta.isEmpty() || eta.equals("null", ignoreCase = true)) Double.NEGATIVE_INFINITY else (eta.parseInstant().epochSeconds - currentEpochSeconds) / 60.0
                    stopSequences.getOrPut(routeId) { mutableSetOf() }.add(stopSeq)
                    busList.add(GMBETAEntry(stopSeq, mins, routeId))
                }
            }
        }
    }
    for ((r, s) in stopSequences) {
        if (s.size > 1) {
            val matchingSeq = s.minByOrNull { (it - stopIndex).absoluteValue }?: -1
            busList.removeAll { it.gtfsId == r && it.stopSeq != matchingSeq }
        }
    }
    val sortedBusList = busList.asSequence()
        .sortedBy { it.mins }
        .distinctBy({ it }, { (_, m1, b1), (_, m2, b2) -> (m1 - m2).absoluteValue < 0.33 && b1 != b2 })
    val lines = mutableListOf<WidgetLineEntry>()
    for ((_, mins) in sortedBusList) {
        if (mins.isFinite() && mins < -10.0) continue
        lines.add(currentLocalDateTime(mins.minutes).toWidgetLineEntry())
    }
    return WidgetETAResult(
        lines = lines,
        hasServices = lines.isNotEmpty()
    )
}

private suspend fun etaQueryLrt(stopId: String, route: Route, precomputedData: WidgetPrecomputedData): WidgetETAResult {
    val language = precomputedData.language
    val stopsList = route.stops[Operator.LRT]!!
    return if (stopsList.indexOf(stopId) + 1 >= stopsList.size) {
        WidgetETAResult(
            lines = listOf(route.endOfLineText[language].toWidgetLineEntry()),
            hasServices = false
        )
    } else {
        val results: MutableList<LocalDateTime> = mutableListOf()
        val data = getJSONResponse<JsonObject>("https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=${stopId.substring(2)}")
        if (data!!.optInt("status") != 0) {
            val platformList = data.optJsonArray("platform_list")!!
            val matchRoutes = setOf(route)
            for (i in 0 until platformList.size) {
                val platform = platformList.optJsonObject(i)!!
                val routeList = platform.optJsonArray("route_list")
                if (routeList != null) {
                    for (u in 0 until routeList.size) {
                        val routeData = routeList.optJsonObject(u)!!
                        val routeNumber = routeData.optString("route_no")
                        if (routeData.contains("time_ch")) {
                            val destCh = routeData.optString("dest_ch")
                            if (matchRoutes.any { routeNumber == it.routeNumber && isLrtStopOnOrAfter(precomputedData.lrtStopList, stopId, destCh, it) }) {
                                val mins = "([0-9]+) *min".toRegex().find(routeData.optString("time_en"))?.groupValues?.getOrNull(1)?.toLong()?: 0
                                results.add(currentLocalDateTime(mins.minutes))
                            }
                        }
                    }
                }
            }
        }
        WidgetETAResult(
            lines = results.asSequence().sorted().map { it.toWidgetLineEntry() }.toList(),
            hasServices = results.isNotEmpty()
        )
    }
}

private suspend fun etaQueryMtr(stopId: String, route: Route, precomputedData: WidgetPrecomputedData): WidgetETAResult {
    val language = precomputedData.language
    val lineName = route.routeNumber
    val bound = route.bound[Operator.MTR]
    return if (precomputedData.isMtrEndOfLine == true) {
        WidgetETAResult(
            lines = listOf(route.endOfLineText[language].toWidgetLineEntry()),
            hasServices = false
        )
    } else {
        val data = getJSONResponse<JsonObject>("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=$lineName&sta=$stopId")
        if (data!!.optInt("status") == 0) {
            WidgetETAResult(
                lines = emptyList(),
                hasServices = false
            )
        } else {
            val lineStops = data.optJsonObject("data")!!.optJsonObject("$lineName-$stopId")
            if (lineStops == null) {
                WidgetETAResult(
                    lines = emptyList(),
                    hasServices = false
                )
            } else {
                val dir = if (bound == "UT") "UP" else "DOWN"
                val trains = lineStops.optJsonArray(dir)
                if (trains.isNullOrEmpty()) {
                    WidgetETAResult(
                        lines = emptyList(),
                        hasServices = false
                    )
                } else {
                    val lines = mutableListOf<WidgetLineEntry>()
                    for (u in 0 until trains.size) {
                        val trainData = trains.optJsonObject(u)!!
                        val eta = trainData.optString("time").let { "${it.substring(0, 10)}T${it.substring(11)}" }
                        val mins = (eta.parseLocalDateTime().toInstant(hongKongTimeZone).toEpochMilliseconds() - currentTimeMillis()) / 60000.0
                        lines.add(currentLocalDateTime(mins.minutes).toWidgetLineEntry())
                    }
                    WidgetETAResult(
                        lines = lines,
                        hasServices = lines.isNotEmpty()
                    )
                }
            }
        }
    }
}

private suspend fun etaQuerySunFerry(stopId: String, co: Operator, route: Route): WidgetETAResult {
    val stops = route.stops[co]!!
    val timeKey = if (stops.indexOf(stopId) == 0) "depart_time" else "eta"
    val data = getJSONResponse<JsonObject>("https://www.sunferry.com.hk/eta/?route=${route.routeNumber}")!!.optJsonArray("data")!!
    val now = currentLocalDateTime() - 1.hours
    val lines = mutableListOf<WidgetLineEntry>()
    for (element in data) {
        val entryData = element.jsonObject
        val time = entryData.optString(timeKey).takeIf { it.matches("[0-9]{2}:[0-9]{2}".toRegex()) }?.let {
            LocalTime(it.substring(0, 2).toInt(), it.substring(3, 5).toInt()).nextLocalDateTimeAfter(now)
        }
        val mins = if (time == null) Double.NEGATIVE_INFINITY else (time.epochSeconds - currentEpochSeconds) / 60.0
        if (mins.roundToInt() < -5) continue
        lines.add(currentLocalDateTime(mins.minutes).toWidgetLineEntry())
    }
    return WidgetETAResult(
        lines = lines,
        hasServices = lines.isNotEmpty()
    )
}

private suspend fun etaQueryHkkf(co: Operator, route: Route): WidgetETAResult {
    val routeId = route.routeNumber.substring(2, 3)
    val bound = if (route.bound[co] == "O") "outbound" else "inbound"
    val data = getJSONResponse<JsonObject>("https://hkkfeta.com/opendata/eta/$routeId/$bound")!!.optJsonArray("data")!!
    val lines = mutableListOf<WidgetLineEntry>()
    for (element in data) {
        val entryData = element.jsonObject
        val eta = entryData.optString("ETA")
        val time = if (eta.isNotEmpty() && !eta.equals("null", true)) eta.parseInstant().toLocalDateTime(
            hongKongTimeZone
        ) else null
        val mins = if (time == null) Double.NEGATIVE_INFINITY else (time.epochSeconds - currentEpochSeconds) / 60.0
        if (mins.roundToInt() < -5) continue
        lines.add(currentLocalDateTime(mins.minutes).toWidgetLineEntry())
    }
    return WidgetETAResult(
        lines = lines,
        hasServices = lines.isNotEmpty()
    )
}

private suspend fun etaQueryFortuneFerry(stopId: String, co: Operator, route: Route, precomputedData: WidgetPrecomputedData): WidgetETAResult {
    val language = precomputedData.language
    val stops = route.stops[co]!!
    val index = stops.indexOf(stopId)
    return if (index + 1 >= stops.size) {
        WidgetETAResult(
            lines = listOf(route.endOfLineText[language].toWidgetLineEntry()),
            hasServices = false
        )
    } else {
        val now = currentLocalDateTime() - 1.hours
        val (stop, nextStop) = precomputedData.hkkfStopCode!!
        val data = getJSONResponse<JsonObject>("https://www.hongkongwatertaxi.com.hk/eta/?route=${stop}${nextStop}")!!
        if (data.optString("generated_timestamp").takeIf { it.isNotBlank() && !it.equals("null", true) }?.parseInstant()?.toLocalDateTime(hongKongTimeZone)?.let { it < now } == true) {
            WidgetETAResult(
                lines = listOf((if (language == "en") "Check Timetable" else "查看時間表").toWidgetLineEntry()),
                hasServices = true
            )
        } else {
            val lines = mutableListOf<WidgetLineEntry>()
            for (element in data.optJsonArray("data")!!) {
                val entryData = element.jsonObject
                val departTime = entryData.optString("depart_time").takeIf { it.matches("[0-9]{2}:[0-9]{2}".toRegex()) }?.let {
                    LocalTime(it.substring(0, 2).toInt(), it.substring(3, 5).toInt()).nextLocalDateTimeAfter(now)
                }
                val mins = if (departTime == null) Double.NEGATIVE_INFINITY else (departTime.epochSeconds - currentEpochSeconds) / 60.0
                if (mins.roundToInt() < -5) continue
                lines.add(currentLocalDateTime(mins.minutes).toWidgetLineEntry())
            }
            WidgetETAResult(
                lines = lines,
                hasServices = lines.isNotEmpty()
            )
        }
    }
}

private fun isLrtStopOnOrAfter(lrtStopList: Map<String, Stop>?, thisStopId: String, targetStopNameZh: String, route: Route): Boolean {
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
        val stop: Stop? = lrtStopList?.get(stopId)
        if (stop != null && stop.name.zh == targetStopNameZh) {
            return true
        }
    }
    return false
}