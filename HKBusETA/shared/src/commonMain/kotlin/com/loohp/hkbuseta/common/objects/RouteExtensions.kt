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
import com.loohp.hkbuseta.common.appcontext.ReduceDataOmitted
import com.loohp.hkbuseta.common.shared.BASE_URL
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.Colored
import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.common.utils.FormattingTextContentStyle
import com.loohp.hkbuseta.common.utils.SmallSize
import com.loohp.hkbuseta.common.utils.asFormattedText
import com.loohp.hkbuseta.common.utils.buildFormattedString
import com.loohp.hkbuseta.common.utils.cache
import com.loohp.hkbuseta.common.utils.createTimetable
import com.loohp.hkbuseta.common.utils.differenceSequence
import com.loohp.hkbuseta.common.utils.editDistance
import com.loohp.hkbuseta.common.utils.getRouteProportions
import com.loohp.hkbuseta.common.utils.mergeSequences
import com.loohp.hkbuseta.common.utils.outOfOrderSequence
import com.loohp.hkbuseta.common.utils.parseIntOr
import com.loohp.hkbuseta.common.utils.remove
import com.loohp.hkbuseta.common.utils.sequenceSimilarity
import kotlinx.datetime.LocalTime
import kotlin.math.max


val bilingualToPrefix = "往" withEn "To "

inline val Operator.isTrain: Boolean get() = when (this) {
    Operator.MTR, Operator.LRT -> true
    else -> false
}
inline val Operator.isFerry: Boolean get() = when (this) {
    Operator.SUNFERRY, Operator.HKKF, Operator.FORTUNEFERRY -> true
    else -> false
}

fun Route.idBound(co: Operator): String = when (co) {
    Operator.CTB -> if (isCtbIsCircular || (bound[co]?.length?: 0) > 1) "OI" else bound[co]
    Operator.NLB -> nlbId
    else -> bound[co]
}?: "O"

inline val Route.idBound: String get() = idBound(co.firstCo()!!)

fun Route.idBoundWithRegion(co: Operator): String = when (co) {
    Operator.CTB -> if (isCtbIsCircular || (bound[co]?.length?: 0) > 1) "OI" else bound[co]
    Operator.NLB -> nlbId
    Operator.GMB -> "${bound[co]?: "O"}_$gmbRegion"
    else -> bound[co]
}?: "O"

inline val Route.idBoundWithRegion: String get() = idBoundWithRegion(co.firstCo()!!)

fun Route.routeGroupKey(co: Operator): String = routeNumber + "," + co.name + "," + idBoundWithRegion(co)

inline val Route.routeGroupKey: String get() = routeGroupKey(co.firstCo()!!)

fun Operator.getOperatorColor(elseColor: Long): Long {
    return getColor("", elseColor)
}

fun Operator.getColor(routeNumber: String, elseColor: Long): Long {
    return when (this) {
        Operator.KMB -> if (routeNumber.getKMBSubsidiary() == KMBSubsidiary.LWB) 0xFFF26C33 else 0xFFFF4747
        Operator.CTB -> 0xFFFFE15E
        Operator.NLB -> 0xFF9BFFC6
        Operator.MTR_BUS -> 0xFFAAD4FF
        Operator.GMB -> 0xFF36FF42
        Operator.LRT -> 0xFFD3A809
        Operator.MTR -> when (routeNumber) {
            "AEL" -> 0xFF00888E
            "TCL" -> 0xFFF3982D
            "TML" -> 0xFF9C2E00
            "TKL" -> 0xFF7E3C93
            "EAL" -> 0xFF5EB7E8
            "SIL" -> 0xFFCBD300
            "TWL" -> 0xFFE60012
            "ISL" -> 0xFF0075C2
            "KTL" -> 0xFF00A040
            "DRL" -> 0xFFEB6EA5
            else -> 0xFFAAD4FF
        }
        Operator.SUNFERRY -> 0xFF6699CC
        Operator.HKKF -> 0xFF4899EA
        Operator.FORTUNEFERRY -> 0xFFF62342
        else -> elseColor
    }
}

fun Operator.getLineColor(routeNumber: String, elseColor: Long): Long {
    return if (this == Operator.LRT) when (routeNumber) {
        "505" -> 0xFFDA2127
        "507" -> 0xFF00A652
        "610" -> 0xFF551C15
        "614" -> 0xFF00BFF3
        "614P" -> 0xFFF4858E
        "615" -> 0xFFFFDD00
        "615P" -> 0xFF016682
        "705" -> 0xFF73BF43
        "706" -> 0xFFB47AB5
        "751" -> 0xFFF48221
        "761P" -> 0xFF6F2D91
        else -> getColor(routeNumber, elseColor)
    } else getColor(routeNumber, elseColor)
}

fun Operator.getDisplayRouteNumber(routeNumber: String, shortened: Boolean = false): String {
    return when {
        isFerry -> if (Shared.language == "en") "Ferry" else "渡輪"
        this === Operator.MTR -> if (shortened && Shared.language == "en") routeNumber else Shared.getMtrLineName(routeNumber, "???")
        this === Operator.KMB && routeNumber.getKMBSubsidiary() == KMBSubsidiary.SUNB -> "NR$routeNumber"
        else -> routeNumber
    }
}

fun Operator.getListDisplayRouteNumber(routeNumber: String, shortened: Boolean = false): String {
    return when {
        isFerry -> if (Shared.language == "en") "Ferry" else "渡輪"
        this === Operator.MTR -> if (shortened && Shared.language == "en") routeNumber else Shared.getMtrLineName(routeNumber, "???")
        else -> routeNumber
    }
}

fun String.getKMBSubsidiary(): KMBSubsidiary {
    return Shared.kmbSubsidiary[this]?: KMBSubsidiary.KMB
}

fun Operator.getOperatorName(language: String, elseName: String = "???"): String {
    return getDisplayName("", false, language, elseName)
}

fun Operator.getDisplayName(routeNumber: String, kmbCtbJoint: Boolean, language: String, elseName: String = "???"): String {
    return if (language == "en") when (this) {
        Operator.KMB -> when (routeNumber.getKMBSubsidiary()) {
            KMBSubsidiary.SUNB -> "Sun Bus"
            KMBSubsidiary.LWB -> if (kmbCtbJoint) "LWB/CTB" else "LWB"
            else -> if (kmbCtbJoint) "KMB/CTB" else "KMB"
        }
        Operator.CTB -> "CTB"
        Operator.NLB -> "NLB"
        Operator.MTR_BUS -> "MTR Bus"
        Operator.GMB -> "GMB"
        Operator.LRT -> "LRT"
        Operator.MTR -> "MTR"
        Operator.SUNFERRY -> "Sun Ferry"
        Operator.HKKF -> "HKKF"
        Operator.FORTUNEFERRY -> "Fortune F."
        else -> elseName
    } else when (this) {
        Operator.KMB -> when (routeNumber.getKMBSubsidiary()) {
            KMBSubsidiary.SUNB -> "陽光巴士"
            KMBSubsidiary.LWB -> if (kmbCtbJoint) "龍運/城巴" else "龍運"
            else -> if (kmbCtbJoint) "九巴/城巴" else "九巴"
        }
        Operator.CTB -> "城巴"
        Operator.NLB -> "嶼巴"
        Operator.MTR_BUS -> "港鐵巴士"
        Operator.GMB -> "專線小巴"
        Operator.LRT -> "輕鐵"
        Operator.MTR -> "港鐵"
        Operator.SUNFERRY -> "新渡輪"
        Operator.HKKF -> "港九小輪"
        Operator.FORTUNEFERRY -> "富裕小輪"
        else -> elseName
    }
}

fun Operator.getDisplayFormattedName(routeNumber: String, kmbCtbJoint: Boolean, language: String, elseName: FormattedText = "???".asFormattedText(), elseColor: Long = 0xFFFFFFFF): FormattedText {
    val color = Colored(getColor(routeNumber, elseColor))
    return if (language == "en") when (this) {
        Operator.KMB -> when (routeNumber.getKMBSubsidiary()) {
            KMBSubsidiary.SUNB -> "Sun Bus".asFormattedText(color)
            KMBSubsidiary.LWB -> if (kmbCtbJoint) buildFormattedString {
                append("LWB", color)
                append("/")
                append("CTB", Colored(Operator.CTB.getOperatorColor(elseColor)))
            } else "LWB".asFormattedText(color)
            else -> if (kmbCtbJoint) buildFormattedString {
                append("KMB", color)
                append("/")
                append("CTB", Colored(Operator.CTB.getOperatorColor(elseColor)))
            } else "KMB".asFormattedText(color)
        }
        Operator.CTB -> "CTB".asFormattedText(color)
        Operator.NLB -> "NLB".asFormattedText(color)
        Operator.MTR_BUS -> "MTR Bus".asFormattedText(color)
        Operator.GMB -> "GMB".asFormattedText(color)
        Operator.LRT -> "LRT".asFormattedText(color)
        Operator.MTR -> "MTR".asFormattedText(color)
        Operator.SUNFERRY -> "Sun Ferry".asFormattedText(color)
        Operator.HKKF -> "HKKF".asFormattedText(color)
        Operator.FORTUNEFERRY -> "Fortune F.".asFormattedText(color)
        else -> elseName
    } else when (this) {
        Operator.KMB -> when (routeNumber.getKMBSubsidiary()) {
            KMBSubsidiary.SUNB -> "陽光巴士".asFormattedText(color)
            KMBSubsidiary.LWB -> if (kmbCtbJoint) buildFormattedString {
                append("龍運", color)
                append("/")
                append("城巴", Colored(Operator.CTB.getOperatorColor(elseColor)))
            } else "龍運".asFormattedText(color)
            else -> if (kmbCtbJoint) buildFormattedString {
                append("九巴", color)
                append("/")
                append("城巴", Colored(Operator.CTB.getOperatorColor(elseColor)))
            } else "九巴".asFormattedText(color)
        }
        Operator.CTB -> "城巴".asFormattedText(color)
        Operator.NLB -> "嶼巴".asFormattedText(color)
        Operator.MTR_BUS -> "港鐵巴士".asFormattedText(color)
        Operator.GMB -> "專線小巴".asFormattedText(color)
        Operator.LRT -> "輕鐵".asFormattedText(color)
        Operator.MTR -> "港鐵".asFormattedText(color)
        Operator.SUNFERRY -> "新渡輪".asFormattedText(color)
        Operator.HKKF -> "港九小輪".asFormattedText(color)
        Operator.FORTUNEFERRY -> "富裕小輪".asFormattedText(color)
        else -> elseName
    }
}

fun Route.shouldPrependTo(): Boolean {
    return lrtCircular == null
}

fun Route.resolvedDest(prependTo: Boolean): BilingualText {
    return lrtCircular?: dest.let { if (prependTo) it.prependTo() else it }
}

fun Route.resolvedDestFormatted(prependTo: Boolean, vararg style: FormattingTextContentStyle): BilingualFormattedText {
    return lrtCircular?.asFormattedText(*style)?: dest.let { if (prependTo) it.prependToFormatted(*style) else it.asFormattedText(*style) }
}

fun Route.resolvedDestWithBranch(prependTo: Boolean, branch: Route): BilingualText {
    return lrtCircular?: branch.dest.let { if (prependTo) it.prependTo() else it }
}

fun Route.resolvedDestWithBranchFormatted(prependTo: Boolean, branch: Route, vararg style: FormattingTextContentStyle): BilingualFormattedText {
    return lrtCircular?.asFormattedText(*style)?: branch.dest.let { if (prependTo) it.prependToFormatted(*style) else it.asFormattedText(*style) }
}

val Route.isCircular: Boolean get() = dest.zh.contains("循環線")

val Route.journeyTimeCircular: Int? get() = journeyTime?.let { if (isCircular) it * 2 else it }

fun BilingualText.prependTo(): BilingualText {
    return bilingualToPrefix + this
}

fun BilingualText.prependToFormatted(vararg style: FormattingTextContentStyle): BilingualFormattedText {
    return bilingualToPrefix.asFormattedText(SmallSize) + this.asFormattedText(*style)
}

fun DoubleArray.toCoordinates(): Coordinates {
    return Coordinates.fromArray(this)
}

fun DoubleArray.toCoordinatesOrNull(): Coordinates? {
    return if (size >= 2) Coordinates.fromArray(this) else null
}

fun Route.getRouteKey(context: AppContext): String? {
    return Registry.getInstance(context).getRouteKey(this)
}

data class HKBusAppStopInfo(val stopId: String, val index: Int)

fun Route.getHKBusAppLink(context: AppContext, stop: HKBusAppStopInfo? = null): String {
    var url = "https://hkbus.app/${Shared.language}/"
    val routeKey = getRouteKey(context)?.replace("+", "-")?.replace(" ", "-")?.lowercase()?: return url
    url += "route/$routeKey/"
    if (stop == null) {
        return url
    }
    return url + "${stop.stopId}%2C${stop.index + 1}/"
}

fun String.asRoute(context: AppContext): Route? {
    return Registry.getInstance(context).findRouteByKey(this, null)
}

fun String.asStop(context: AppContext): Stop? {
    return Registry.getInstance(context).getStopById(this)
}

fun String.identifyStopCo(): Set<Operator> {
    return Operator.values().asSequence().filter { it.matchStopIdPattern(this) }.toSet()
}

fun BilingualText.asFormattedText(vararg style: FormattingTextContentStyle): BilingualFormattedText {
    return zh.asFormattedText(*style) withEn en.asFormattedText(*style)
}

inline val RouteSearchResultEntry.uniqueKey: String get() {
    return stopInfo?.let { "$routeKey:${it.stopId}:${favouriteStopMode.toString()}" }?: routeKey
}

inline val CharSequence.operator: Operator get() = Operator.valueOf(toString())

inline val CharSequence.gmbRegion: GMBRegion? get() = GMBRegion.valueOfOrNull(toString().uppercase())

data class FavouriteResolvedStop(val index: Int, val stopId: String, val stop: Stop, val route: Route)

fun FavouriteRouteStop.asResolvedStop(): FavouriteResolvedStop {
    return FavouriteResolvedStop(index, stopId, stop, route)
}

inline fun FavouriteRouteStop.resolveStop(context: AppContext, originGetter: () -> Coordinates?): FavouriteResolvedStop {
    if (favouriteStopMode == FavouriteStopMode.FIXED) {
        return FavouriteResolvedStop(index, stopId, stop, route)
    }
    val origin = originGetter.invoke()?: return FavouriteResolvedStop(index, stopId, stop, route)
    return Registry.getInstance(context).getAllStops(route.routeNumber, route.bound[co]!!, co, route.gmbRegion)
        .withIndex()
        .minBy { it.value.stop.location.distance(origin) }
        .let { FavouriteResolvedStop(it.index + 1, it.value.stopId, it.value.stop, it.value.route) }
}

inline fun List<FavouriteRouteStop>.resolveStops(context: AppContext, originGetter: () -> Coordinates?): List<Pair<FavouriteRouteStop, FavouriteResolvedStop?>> {
    if (isEmpty()) {
        return emptyList()
    }
    if (any { it.favouriteStopMode == FavouriteStopMode.FIXED }) {
        return map { it to FavouriteResolvedStop(it.index, it.stopId, it.stop, it.route) }
    }
    val origin = originGetter.invoke()?: this[0].stop.location
    val eachAllStop = map { Registry.getInstanceNoUpdateCheck(context).getAllStops(it.route.routeNumber, it.route.bound[it.co]!!, it.co, it.route.gmbRegion) }
    val closestStop = eachAllStop.flatten().minBy { it.stop.location.distance(origin) }
    return eachAllStop.withIndex().map { (index, allStops) ->
        allStops.withIndex()
            .map { it.value.stop.location.distance(closestStop.stop.location) to it }
            .minBy { it.first }
            .let { this[index] to (if (it.first <= 0.15) FavouriteResolvedStop(it.second.index + 1, it.second.value.stopId, it.second.value.stop, it.second.value.route) else null) }
    }
}

fun Route.getFare(stopIndex: Int, holidayFare: Boolean = false): Fare? {
    val list = faresHoliday?.takeIf { holidayFare }?: fares?: return null
    return list.getOrNull(stopIndex)
}

fun Route.getFare(stopId: String, holidayFare: Boolean = false): Fare? {
    val index = stops.firstNotNullOfOrNull { (_, v) -> v.indexOf(stopId).takeIf { it >= 0 } }?: return null
    return getFare(index, holidayFare)
}

inline fun Collection<Operator>.firstCo(): Operator? {
    return Operator.values().firstOrNull { contains(it) }
}

enum class RemarkType {
    RAW, LABEL_MAIN_BRANCH, LABEL_ALL
}

abstract class RouteDifference {
    abstract fun displayText(stopList: Map<String, Stop>): BilingualText
}
data object RouteDifferenceMain: RouteDifference() {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return BilingualText.EMPTY
    }
}
data class RouteDifferenceDestination(
    val destinationName: BilingualText
): RouteDifference() {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return ("開往" withEn "To ") + destinationName
    }
}
data class RouteDifferenceOrigin(
    val originName: BilingualText
): RouteDifference() {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return ("從" withEn "From ") + originName + ("開出" withEn "")
    }
}
data class RouteDifferenceVia(
    val viaDiff: List<List<String>>
): RouteDifference() {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return viaDiff.asSequence().mapNotNull {
            when (it.size) {
                0 -> null
                1 -> stopList[it.first()]!!.name
                else -> stopList[it.first()]!!.name + ("至" withEn " to ") + stopList[it.last()]!!.name
            }
        }.joinBilingualText(", ".asBilingualText(), "經" withEn "Via ")
    }
}
data class RouteDifferenceOmit(
    val omitDiff: List<List<String>>
): RouteDifference() {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return omitDiff.asSequence().mapNotNull {
            when (it.size) {
                0 -> null
                1 -> stopList[it.first()]!!.name
                else -> stopList[it.first()]!!.name + ("至" withEn " to ") + stopList[it.last()]!!.name
            }
        }.joinBilingualText(", ".asBilingualText(), "不經" withEn "Omit ")
    }
}
data class RouteDifferenceViaFirst(
    val firstVia: BilingualText
): RouteDifference() {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return ("先經" withEn "Via ") + firstVia + ("" withEn " first")
    }
}

fun Collection<RouteDifference>.displayText(allStops: List<String>, stopList: Map<String, Stop>): BilingualText {
    val diff = distinct()
    if (diff.size == 1) return diff.first().displayText(stopList)
    return buildList {
        diff.filterIsInstance<RouteDifferenceMain>().forEach { add(it.displayText(stopList)) }
        diff.filterIsInstance<RouteDifferenceDestination>().forEach { add(it.displayText(stopList)) }
        diff.filterIsInstance<RouteDifferenceOrigin>().forEach { add(it.displayText(stopList)) }
        diff.filterIsInstance<RouteDifferenceVia>().let { f ->
            if (f.isNotEmpty()) {
                add(RouteDifferenceVia(f.asSequence().map { it.viaDiff }.flatten().toList().mergeSequences(allStops)).displayText(stopList))
            }
        }
        diff.filterIsInstance<RouteDifferenceOmit>().let { f ->
            if (f.isNotEmpty()) {
                add(RouteDifferenceOmit(f.asSequence().map { it.omitDiff }.flatten().toList().mergeSequences(allStops)).displayText(stopList))
            }
        }
        diff.filterIsInstance<RouteDifferenceViaFirst>().forEach { add(it.displayText(stopList)) }
    }.joinBilingualText(" ".asBilingualText())
}

fun Route.findDifference(mainRoute: Route, stopList: Map<String, Stop>): RouteDifference {
    val co = bound.keys.firstCo()?: throw RuntimeException()
    return when {
        mainRoute.stops[co]?.last() != stops[co]?.last() -> RouteDifferenceDestination(dest)
        mainRoute.stops[co]?.first() != stops[co]?.first() -> RouteDifferenceOrigin(orig)
        else -> {
            val mainRouteStops = mainRoute.stops[co]!!
            val routeStops = stops[co]!!

            val mainRouteFirstStop = stopList[mainRouteStops.first()]!!.name
            val mainRouteLastStop = stopList[mainRouteStops.last()]!!.name
            val routeFirstStop = stopList[routeStops.first()]!!.name
            val routeLastStop = stopList[routeStops.last()]!!.name

            when {
                mainRouteLastStop["zh"] != routeLastStop["zh"] -> RouteDifferenceDestination(routeLastStop)
                mainRouteFirstStop["zh"] != routeFirstStop["zh"] -> RouteDifferenceOrigin(routeFirstStop)
                else -> {
                    val viaDiff = if (routeStops.none { x -> !mainRouteStops.contains(x) }) emptyList() else routeStops.differenceSequence(mainRouteStops)
                    if (viaDiff.isNotEmpty()) {
                        RouteDifferenceVia(viaDiff)
                    } else {
                        val omitDiff = if (mainRouteStops.none { x -> !routeStops.contains(x) }) emptyList() else mainRouteStops.differenceSequence(routeStops)
                        if (omitDiff.isNotEmpty()) {
                            RouteDifferenceOmit(omitDiff)
                        } else {
                            val orderDiff = outOfOrderSequence(mainRouteStops, routeStops)
                            if (orderDiff.isNotEmpty()) {
                                val (diff, shuffled, isViaLater) = orderDiff.first()
                                if (diff.isNotEmpty() && shuffled.isNotEmpty()) {
                                    RouteDifferenceViaFirst(if (isViaLater) {
                                        if (mainRouteFirstStop.zh == mainRouteLastStop.zh) {
                                            val matching = mainRoute.dest.zh.remove(" *\\(?循環線\\)?".toRegex())
                                            shuffled.asSequence()
                                                .map { stopList[it]!!.name }
                                                .filter { it.zh.contains(matching) }
                                                .minByOrNull { it.zh.editDistance(matching) }
                                                ?: stopList[shuffled.first()]!!.name
                                        } else {
                                            stopList[shuffled.first()]!!.name
                                        }
                                    } else {
                                        stopList[diff.first()]!!.name
                                    })
                                } else {
                                    RouteDifferenceMain
                                }
                            } else {
                                RouteDifferenceMain
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ReduceDataOmitted::class)
fun Route.resolveSpecialRemark(context: AppContext, labelType: RemarkType = RemarkType.RAW): BilingualText {
    return cache("resolveSpecialRemark", this, labelType) {
        val co = bound.keys.firstCo()?: throw RuntimeException()
        val bound = idBound(co)
        val (routeList, stopList) = Registry.getInstance(context).let { it.getAllBranchRoutes(routeNumber, bound, co, gmbRegion) to it.getStopList() }
        val allStops by lazy { Registry.getInstance(context).getAllStops(routeNumber, bound, co, gmbRegion).map { it.stopId } }
        val timetable = if (!context.formFactor.reduceData) {
            routeList.createTimetable(Registry.getInstance(context).getServiceDayMap()) { BilingualText.EMPTY }
        } else {
            null
        }
        val mainRoute = routeList.first()
        val proportions = timetable?.takeIf { co == Operator.KMB || co == Operator.CTB }?.getRouteProportions()?: mapOf(mainRoute to 1F)
        if (proportions[mainRoute]?.let { it > 0.4F } == true) {
            val remark = (if (this != mainRoute) findDifference(mainRoute, stopList) else RouteDifferenceMain).displayText(stopList)
            when (labelType) {
                RemarkType.RAW -> remark
                RemarkType.LABEL_MAIN_BRANCH -> remark.let {
                    if (it.zh.isNotBlank()) {
                        it
                    } else {
                        "正常路線" withEn "Normal Route"
                    }
                }
                RemarkType.LABEL_ALL -> remark.let {
                    if (it.zh.isNotBlank()) {
                        val entries = timetable?.values?.flatten()?.filter { e -> e.route == this }
                        it + when {
                            entries == null -> " - 特別班" withEn " - Special"
                            entries.isNotEmpty() && entries.all { l -> l.within(LocalTime(5, 0), LocalTime(10, 0)) } -> " - 早上繁忙時間特別班" withEn " - Morning Peak Special"
                            entries.isNotEmpty() && entries.all { l -> l.within(LocalTime(16, 30), LocalTime(21, 0)) } -> " - 下午繁忙時間特別班" withEn " - Evening Peak Special"
                            else -> " - 特別班" withEn " - Special"
                        }
                    } else {
                        "正常路線" withEn "Normal Route"
                    }
                }
            }
        } else {
            val remark = buildList {
                for (branch in routeList.sortedBy { proportions[it]?: 0F }) {
                    if (this@resolveSpecialRemark != branch) {
                        add(findDifference(branch, stopList))
                    }
                }
            }.displayText(allStops, stopList)
            when (labelType) {
                RemarkType.RAW -> remark
                RemarkType.LABEL_MAIN_BRANCH, RemarkType.LABEL_ALL -> remark.let {
                    val extra = if (routeList.size != 2) {
                        BilingualText.EMPTY
                    } else {
                        val entries = timetable?.values?.flatten()?.filter { e -> e.route == this }
                        when {
                            entries == null -> BilingualText.EMPTY
                            entries.isNotEmpty() && entries.all { l -> l.within(LocalTime(5, 0), LocalTime(13, 0)) } -> "上午班次" withEn "AM Service"
                            entries.isNotEmpty() && entries.all { l -> l.within(LocalTime(11, 30), LocalTime(1, 0)) } -> "下午班次" withEn "PM Service"
                            else -> BilingualText.EMPTY
                        }
                    }
                    if (it.zh.isNotBlank()) {
                        it + extra.let { e -> if (e.zh.isBlank()) e else " - ".asBilingualText() + e }
                    } else {
                        extra
                    }
                }
            }
        }
    }
}

fun FavouriteRouteStop.sameAs(stopId: String, co: Operator, index: Int, stop: Stop, route: Route): Boolean {
    if (this.co !== co) return false
    if (this.route.routeNumber != route.routeNumber) return false
    if (this.route.bound[co] != route.bound[co]) return false
    if (this.favouriteStopMode == FavouriteStopMode.FIXED) {
        return if (this.index != index || this.stopId != stopId) {
            false
        } else {
            this.stop.name.zh == stop.name.zh
        }
    }
    return true
}

infix fun Route?.similarAs(other: Route?): Boolean {
    if (this == other) return true
    if (this == null || other == null) return false
    if (routeNumber != other.routeNumber) return false
    if (!co.containsAll(other.co) && !other.co.containsAll(co)) return false
    return true
}

fun String.getMTRStationStreetMapUrl(): String {
    return "https://docs.google.com/gview?embedded=true&url=https://www.mtr.com.hk/archive/${if (Shared.language == "en") "en" else "ch"}/services/maps/${lowercase()}.pdf"
}

fun String.getMTRStationLayoutUrl(): String {
    return "https://docs.google.com/gview?embedded=true&url=https://www.mtr.com.hk/archive/${if (Shared.language == "en") "en" else "ch"}/services/layouts/${lowercase()}.pdf"
}

fun Route.getDeepLink(context: AppContext, stopId: String?, stopIndex: Int?): String {
    var link = "${BASE_URL}/route/${getRouteKey(context)}"
    if (stopId != null) {
        link += "/$stopId%2C${stopIndex?: 0}"
    }
    return link
}

fun RouteSearchResultEntry.getDeepLink(): String {
    var link = "${BASE_URL}/route/$routeKey"
    if (stopInfo != null) {
        link += "/${stopInfo!!.stopId}"
    }
    return link
}

fun StopIndexedRouteSearchResultEntry.getDeepLink(): String {
    var link = "${BASE_URL}/route/$routeKey"
    if (stopInfo != null) {
        link += "/${stopInfo!!.stopId}%2C$stopInfoIndex"
    }
    return link
}

private val numberExtractRegex = "[0-9]+".toRegex()

val routeNumberComparator: Comparator<String> = Comparator { a, b ->
    val matchA = numberExtractRegex.find(a)
    val matchB = numberExtractRegex.find(b)
    val diff = (matchA?.range?.start?: Int.MAX_VALUE).compareTo(matchB?.range?.start?: Int.MAX_VALUE)
    if (diff != 0) diff else (matchA?.value?.toIntOrNull()?: Int.MAX_VALUE).compareTo(matchB?.value?.toIntOrNull()?: Int.MAX_VALUE)
}

fun String.compareRouteNumber(other: String): Int {
    val diff = routeNumberComparator.compare(this, other)
    return if (diff == 0) compareTo(other) else diff
}

val Route.waypointsId: String? get() {
    val operator = co.firstCo()
    return when {
        operator === Operator.MTR -> routeNumber.substring(0, routeNumber.indexOf("-").takeIf { it >= 0 }?: 3).lowercase()
        operator === Operator.LRT -> if (lrtCircular != null) routeNumber else "${routeNumber}_${bound[operator]}"
        operator?.isFerry == true -> routeNumber
        gtfsId.isBlank() -> null
        else -> "${gtfsId}-${if (bound[operator] == "I") "I" else "O"}"
    }
}

val routeComparatorRouteNumberFirst: Comparator<Route> = compareBy<Route, String>(routeNumberComparator) {
    it.routeNumber
}.thenBy {
    it.routeNumber
}.thenBy {
    if (it.co.firstCo()!!.isTrain) Shared.getMtrLineSortingIndex(it.routeNumber) else -1
}.thenByDescending {
    if (it.co.firstCo()!!.isTrain) it.bound[it.co.firstCo()!!] else ""
}.thenBy {
    it.co.firstCo()!!
}.thenBy {
    if (it.co.firstCo() === Operator.NLB) it.nlbId.parseIntOr() else 0
}.thenBy {
    if (it.co.firstCo() === Operator.GMB) it.gtfsId.parseIntOr() else 0
}.thenBy {
    it.serviceType.parseIntOr()
}.thenByDescending {
    if (it.co.firstCo() !== Operator.CTB) it.bound[it.co.firstCo()!!] else ""
}

val routeComparator: Comparator<Route> = compareBy<Route> {
    it.co.firstCo()!!
}.thenBy {
    if (it.co.firstCo()!!.isTrain) Shared.getMtrLineSortingIndex(it.routeNumber) else -1
}.thenByDescending {
    if (it.co.firstCo()!!.isTrain) it.bound[it.co.firstCo()!!] else ""
}.thenBy(routeNumberComparator) {
    it.routeNumber
}.thenBy {
    it.routeNumber
}.thenBy {
    if (it.co.firstCo() === Operator.NLB) it.nlbId.parseIntOr() else 0
}.thenBy {
    if (it.co.firstCo() === Operator.GMB) it.gtfsId.parseIntOr() else 0
}.thenBy {
    it.serviceType.parseIntOr()
}.thenByDescending {
    if (it.co.firstCo() !== Operator.CTB) it.bound[it.co.firstCo()!!] else ""
}

suspend fun RouteSearchResultEntry.findReverse(context: AppContext): RouteSearchResultEntry? {
    return when {
        co === Operator.NLB || co.isFerry -> {
            val stops = route!!.stops[co]?.takeIf { it.size > 1 }?: return null
            val firstId = stops.first()
            val lastId = stops.last()
            val first = firstId.asStop(context)!!.location
            val last = lastId.asStop(context)!!.location
            val predicate: (String, Route, Operator) -> Boolean = predicate@{ _, r, c ->
                if (co != c) return@predicate false
                val routeStops = r.stops[c]?.takeIf { it.size > 1 }?: return@predicate false
                val routeFirstStopId = routeStops.first()
                val routeLastStopId = routeStops.last()
                if (routeFirstStopId == lastId && routeLastStopId == firstId) return@predicate true
                if (routeFirstStopId.asStop(context)!!.location.distance(last) >= 0.3F) return@predicate false
                return@predicate routeLastStopId.asStop(context)!!.location.distance(first) < 0.3F
            }
            if (co.isFerry) {
                Registry.getInstance(context).findRoutes("", false, predicate).firstOrNull()
            } else {
                Registry.getInstance(context).findRoutes(route!!.routeNumber, true, predicate).firstOrNull()
            }
        }
        route!!.lrtCircular == null -> {
            val result = Registry.getInstance(context).findRoutes(route!!.routeNumber, true) { _, r, c -> co == c && (co != Operator.GMB || r.gmbRegion == route!!.gmbRegion) }
            val byBound = result.filter { it.route!!.bound[co] != route!!.bound[co] }
            if (byBound.isEmpty()) {
                result.firstOrNull { it.routeKey != routeKey }
            } else {
                byBound.first()
            }
        }
        else -> {
            val result = Registry.getInstance(context).findRoutes(route!!.routeNumber.substring(0, 2), false) { _, r, c -> co == c && r.lrtCircular != null }
            result.firstOrNull { it.route!!.routeNumber != route!!.routeNumber }
        }
    }
}

suspend fun Route.findSimilarRoutes(co: Operator, context: AppContext): List<RouteSearchResultEntry> {
    val registry = Registry.getInstance(context)
    val stops = stops[co]?.takeIf { it.isNotEmpty() }?: emptyList()
    val equality: (String, String) -> Boolean = { a, b ->
        a == b || a.asStop(context)!!.location.distance(b.asStop(context)!!.location) < 0.3
    }
    val similarities = mutableMapOf<String, Float>()
    return registry.findRoutes("", false) { _, r, c ->
        if (c != co) return@findRoutes false
        if (r.routeNumber == routeNumber) return@findRoutes false
        val s = r.stops[c]?.takeIf { it.isNotEmpty() }?: return@findRoutes false
        val simA = stops.sequenceSimilarity(s, equality)
        val simB = s.sequenceSimilarity(stops, equality)
        if (simA > 0.7F || simB > 0.5F) {
            val key = r.routeGroupKey(c)
            similarities[key] = max(simA, simB).coerceAtLeast(similarities[key]?: 0F)
            true
        } else {
            false
        }
    }.sortedWith(compareBy<RouteSearchResultEntry> {
        routeNumber.isNightRoute != it.route!!.routeNumber.isNightRoute
    }.thenBy {
        similarities.getOrPut(it.route!!.routeGroupKey(it.co)) {
            val s = it.route!!.stops[it.co]?.takeIf { s -> s.isNotEmpty() }?: listOf("----")
            max(stops.sequenceSimilarity(s, equality), s.sequenceSimilarity(stops, equality))
        }
    })
}

inline val String.isNightRoute: Boolean get() {
    return this[0] == 'N' || this == "270S" || this == "271S" || this == "293S" || this == "701S" || this == "796S"
}

val airportExpressExclusiveStations = setOf("AIR", "AWE")
val changeableAirportExpressStations = listOf("HOK", "KOW", "TSY")
val airportExpressStations = setOf("HOK", "KOW", "TSY", "AIR", "AWE")

@ReduceDataOmitted
fun String.findMTRFares(otherStopId: String, context: AppContext): Map<String?, Map<FareType, Fare>> {
    return if ((airportExpressExclusiveStations.contains(this) || airportExpressExclusiveStations.contains(otherStopId)) && !(airportExpressStations.contains(this) && airportExpressStations.contains(otherStopId))) {
        changeableAirportExpressStations.associateWith {
            val fareTableFirst = Registry.getInstance(context).getMTRData()[this]?.fares?: return emptyMap()
            val fareTableSecond = Registry.getInstance(context).getMTRData()[it]?.fares?: return emptyMap()
            val fareFirst = fareTableFirst[it]?: emptyMap()
            val fareSecond = fareTableSecond[otherStopId]?: emptyMap()
            if (airportExpressExclusiveStations.contains(this)) {
                fareFirst.merge(fareSecond, true)
            } else {
                fareSecond.merge(fareFirst, true)
            }
        }
    } else {
        val fareTable = Registry.getInstance(context).getMTRData()[this]?.fares?: return emptyMap()
        fareTable[otherStopId]?.let { mapOf(null to it) }?: emptyMap()
    }
}

@ReduceDataOmitted
fun String.findLRTFares(otherStopId: String, context: AppContext): Map<FareType, Fare> {
    val fareTable = Registry.getInstance(context).getLRTData()[this]?.fares?: return emptyMap()
    return fareTable[otherStopId]?: emptyMap()
}

fun Map<FareType, Fare>.findFare(fareCategory: FareCategory, ticketCategory: TicketCategory): Fare? {
    if (isEmpty()) return null
    entries.firstOrNull { it.key.category == fareCategory && it.key.ticketCategory == ticketCategory }?.let { return it.value }
    entries.firstOrNull { it.key.category == FareCategory.ADULT && it.key.ticketCategory == ticketCategory }?.let { return it.value }
    return null
}

@ReduceDataOmitted
fun String.getMTRStationBarrierFree(context: AppContext): Map<String, StationBarrierFreeFacility> {
    return Registry.getInstance(context).getMTRData()[this]?.barrierFree?: emptyMap()
}

@ReduceDataOmitted
fun String.getStationBarrierFreeDetails(context: AppContext): StationBarrierFreeItem? {
    return Registry.getInstance(context).getMTRBarrierFreeMapping().items[this]
}

@ReduceDataOmitted
fun StationBarrierFreeItem.getCategoryDetails(context: AppContext): StationBarrierFreeCategory? {
    return Registry.getInstance(context).getMTRBarrierFreeMapping().categories[category]
}

@ReduceDataOmitted
fun getMTRBarrierFreeCategories(context: AppContext): Map<String, StationBarrierFreeCategory> {
    return Registry.getInstance(context).getMTRBarrierFreeMapping().categories
}

fun Map<FareType, Fare>.merge(other: Map<FareType, Fare>, octopusFree: Boolean): Map<FareType, Fare> {
    val result: MutableMap<FareType, Fare> = mutableMapOf()
    for (key in FareType.entries) {
        if (containsKey(key) || other.containsKey(key)) {
            if (octopusFree && key.ticketCategory == TicketCategory.OCTO) {
                result[key] = findFare(key.category, key.ticketCategory)?: Fare.ZERO
            } else {
                result[key] = (findFare(key.category, key.ticketCategory)?: Fare.ZERO) + (other.findFare(key.category, key.ticketCategory)?: Fare.ZERO)
            }
        }
    }
    return result
}

@ReduceDataOmitted
fun String.findMTROpeningTimes(context: AppContext): Pair<LocalTime, LocalTime>? {
    return Registry.getInstance(context).getMTRData()[this]?.let {
        if (it.opening != null && it.closing != null) it.opening to it.closing else null
    }
}

@ReduceDataOmitted
fun String.findMTRFirstTrain(destinationId: String, context: AppContext): FirstLastTrain? {
    return Registry.getInstance(context).getMTRData()[this]?.firstTrains?.get(destinationId)
}

@ReduceDataOmitted
fun String.findMTRLastTrain(destinationId: String, context: AppContext): FirstLastTrain? {
    return Registry.getInstance(context).getMTRData()[this]?.lastTrains?.get(destinationId)
}

@ReduceDataOmitted
fun String.findLRTFirstTrain(destinationId: String, context: AppContext): FirstLastTrain? {
    return Registry.getInstance(context).getLRTData()[this]?.firstTrains?.get(destinationId)
}

@ReduceDataOmitted
fun String.findLRTLastTrain(destinationId: String, context: AppContext): FirstLastTrain? {
    return Registry.getInstance(context).getLRTData()[this]?.lastTrains?.get(destinationId)
}

inline val Stop.hkkfStopCode: String get() = when {
    name.zh.contains("中環") -> "CL"
    name.zh.contains("紅磡") -> "HH"
    name.zh.contains("北角") -> "NP"
    name.zh.contains("觀塘") -> "KT"
    name.zh.contains("啟德") -> "KTK"
    name.zh.contains("屯門") -> "TM"
    name.zh.contains("東涌") -> "TC"
    name.zh.contains("沙螺灣") -> "SLW"
    name.zh.contains("大澳") -> "TO"
    else -> "CL"
}