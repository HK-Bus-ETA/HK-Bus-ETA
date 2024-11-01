package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ColorContentStyle
import com.loohp.hkbuseta.common.utils.distinctBy
import com.loohp.hkbuseta.common.utils.gzipSupported
import com.loohp.hkbuseta.common.utils.indexesOf
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.absoluteValue


actual val FavouriteRouteStop.platformDisplayInfo: JsonObject? get() = buildJsonObject {
    put("routeNumber", co.getDisplayRouteNumber(route.routeNumber, false))
    put("co", co.name)
    put("coDisplay", buildJsonArray {
        co.getDisplayFormattedName(route.routeNumber, route.isKmbCtbJoint, route.gmbRegion, Shared.language).content.forEach {
            add(buildJsonObject {
                put("string", it.string)
                it.style.asSequence().filterIsInstance<ColorContentStyle>().firstOrNull()?.let { style ->
                    put("color", style.color)
                }
            })
        }
    })
    if (route.shouldPrependTo()) {
        put("prependTo", bilingualToPrefix[Shared.language])
    }
    route.resolvedDest(false).let {
        put("dest", it[Shared.language])
    }
    if (co == Operator.NLB) {
        if (Shared.language == "en") {
            put("coSpecialRemark", "From ${route.orig.en}")
        } else {
            put("coSpecialRemark", "從${route.orig.zh}開出")
        }
    } else if (co == Operator.KMB) {
        if (route.routeNumber.getKMBSubsidiary() == KMBSubsidiary.SUNB) {
            if (Shared.language == "en") {
                put("coSpecialRemark", "Sun Bus (NR${route.routeNumber})")
            } else {
                put("coSpecialRemark", "陽光巴士 (NR${route.routeNumber})")
            }
        } else if (route.routeNumber.isPetBus()) {
            if (Shared.language == "en") {
                put("coSpecialRemark", "Pet Bus \uD83D\uDC3E")
            } else {
                put("coSpecialRemark", "寵物巴士 \uD83D\uDC3E")
            }
        }
    }
    if (favouriteStopMode == FavouriteStopMode.FIXED) {
        put("secondLine", if (co.isTrain) stop.name[Shared.language] else "${index}. ${stop.name[Shared.language]}")
    }
    put("deeplink", route.getDeepLink(appContextForWidget, stopId, index))
    gzipSupported()
    put("precomputedData", JsonWidgetPrecomputedData.encodeToString(buildWidgetPrecomputedData()))
}

expect val appContextForWidget: AppContext

val JsonWidgetPrecomputedData: Json = Json {
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = true
}

fun FavouriteRouteStop.buildWidgetPrecomputedData(): WidgetPrecomputedData {
    val registry = Registry.getInstanceNoUpdateCheck(appContextForWidget)
    val allBranches = registry.getAllBranchRoutes(route.routeNumber, route.idBound(co), co, route.gmbRegion)
    val allStops = registry.getAllStops(route.routeNumber, route.idBound(co), co, route.gmbRegion)
    val usedServiceDayKeys = allBranches.asSequence().mapNotNull { it.freq?.keys }.flatten().toSet()
    val serviceDayMap = registry.getServiceDayMap().asSequence()
        .filter { usedServiceDayKeys.contains(it.key) }
        .associate { (k, v) -> k to v }
    val holidays = registry.getHolidays()
    return when {
        route.isKmbCtbJoint -> {
            val matchingStops: List<Pair<Operator, String>>? = registry.getStopMap()[stopId]
            val ctbStopIds: MutableList<String> = mutableListOf()
            if (matchingStops == null) {
                val location: Coordinates = registry.getStopList()[stopId]!!.location
                for ((key, value) in registry.getStopList().entries) {
                    if (Operator.CTB.matchStopIdPattern(key) && location.distance(value.location) < 0.4) {
                        ctbStopIds.add(key)
                    }
                }
            } else {
                for ((first, second) in matchingStops) {
                    if (Operator.CTB === first) {
                        ctbStopIds.add(second)
                    }
                }
            }
            WidgetPrecomputedData(
                language = Shared.language,
                fav = this,
                serviceDayMap = serviceDayMap,
                holidays = holidays,
                co = co,
                allBranches = allBranches,
                allStops = allStops.map { WidgetStopData(it.stopId, it.stop, allBranches.indexOf(it.route)) },
                ctbStopIds = ctbStopIds,
                ctbByDirectionResult = registry.getAllDestinationsByDirection(route.routeNumber, Operator.KMB, null, null, route, stopId)
            )
        }
        co === Operator.KMB -> {
            val stopData = allStops.first { it.stopId == stopId }
            val stopIds = allStops
                .asSequence()
                .filter { it.stop.name == stopData.stop.name && it.stop.location.distance(stopData.stop.location) < 0.1 }
                .distinctBy(
                    selector = { it.branchIds },
                    equalityPredicate = { a, b -> a.intersect(b).isNotEmpty() }
                )
                .map { it.stopId }
                .distinct()
                .toList()
            WidgetPrecomputedData(
                language = Shared.language,
                fav = this,
                serviceDayMap = serviceDayMap,
                holidays = holidays,
                co = co,
                allBranches = allBranches,
                allStops = allStops.map { WidgetStopData(it.stopId, it.stop, allBranches.indexOf(it.route)) },
                kmbStopIds = stopIds
            )
        }
        co === Operator.MTR_BUS -> {
            WidgetPrecomputedData(
                language = Shared.language,
                fav = this,
                serviceDayMap = serviceDayMap,
                holidays = holidays,
                co = co,
                allBranches = allBranches,
                allStops = allStops.map { WidgetStopData(it.stopId, it.stop, allBranches.indexOf(it.route)) },
                mtrBusStopAlias = registry.getMtrBusStopAlias()[stopId]
            )
        }
        co === Operator.GMB -> {
            val branches = allStops.indexesOf { it.stopId == stopId }
                .minByOrNull { (it - index).absoluteValue }
                ?.let { allStops[it].branchIds.associateBy { b -> b.gtfsId } }
                ?: mapOf(route.gtfsId to route)
            WidgetPrecomputedData(
                language = Shared.language,
                fav = this,
                serviceDayMap = serviceDayMap,
                holidays = holidays,
                co = co,
                allBranches = allBranches,
                allStops = allStops.map { WidgetStopData(it.stopId, it.stop, allBranches.indexOf(it.route)) },
                gmbBranches = branches.keys.toList()
            )
        }
        co === Operator.LRT -> {
            WidgetPrecomputedData(
                language = Shared.language,
                fav = this,
                serviceDayMap = serviceDayMap,
                holidays = holidays,
                co = co,
                allBranches = allBranches,
                allStops = allStops.map { WidgetStopData(it.stopId, it.stop, allBranches.indexOf(it.route)) },
                lrtStopList = registry.getStopList().asSequence()
                    .filter { it.key.identifyStopCo().contains(Operator.LRT) }
                    .associate { (k, v) -> k to v }
            )
        }
        co === Operator.MTR -> {
            WidgetPrecomputedData(
                language = Shared.language,
                fav = this,
                serviceDayMap = serviceDayMap,
                holidays = holidays,
                co = co,
                allBranches = allBranches,
                allStops = allStops.map { WidgetStopData(it.stopId, it.stop, allBranches.indexOf(it.route)) },
                isMtrEndOfLine = registry.isMtrStopEndOfLine(stopId, route.routeNumber, route.bound[Operator.MTR])
            )
        }
        co === Operator.HKKF -> {
            val stops = route.stops[co]!!
            WidgetPrecomputedData(
                language = Shared.language,
                fav = this,
                serviceDayMap = serviceDayMap,
                holidays = holidays,
                co = co,
                allBranches = allBranches,
                allStops = allStops.map { WidgetStopData(it.stopId, it.stop, allBranches.indexOf(it.route)) },
                hkkfStopCode = registry.getStopList().let { it[stops[index]]!!.hkkfStopCode to it[stops[index + 1]]!!.hkkfStopCode }
            )
        }
        else -> {
            WidgetPrecomputedData(
                language = Shared.language,
                fav = this,
                serviceDayMap = serviceDayMap,
                holidays = holidays,
                co = co,
                allBranches = allBranches,
                allStops = allStops.map { WidgetStopData(it.stopId, it.stop, allBranches.indexOf(it.route)) }
            )
        }
    }
}

@Serializable
data class WidgetPrecomputedData(
    val language: String,
    val fav: FavouriteRouteStop,
    val serviceDayMap: Map<String, List<String>?>,
    val holidays: Collection<LocalDate>,
    val co: Operator,
    val allBranches: List<Route>,
    val allStops: List<WidgetStopData>,
    val ctbStopIds: List<String>? = null,
    val ctbByDirectionResult: Pair<Set<BilingualText>, Set<BilingualText>>? = null,
    val kmbStopIds: List<String>? = null,
    val mtrBusStopAlias: List<String>? = null,
    val gmbBranches: List<String>? = null,
    val lrtStopList: Map<String, Stop>? = null,
    val isMtrEndOfLine: Boolean? = null,
    val hkkfStopCode: Pair<String, String>? = null
)

@Serializable
data class WidgetStopData(val stopId: String, val stop: Stop, val route: Int)