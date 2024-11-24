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
import com.loohp.hkbuseta.common.utils.RouteBranchStatus
import com.loohp.hkbuseta.common.utils.ServiceTimeCategory
import com.loohp.hkbuseta.common.utils.SmallSize
import com.loohp.hkbuseta.common.utils.any
import com.loohp.hkbuseta.common.utils.asFormattedText
import com.loohp.hkbuseta.common.utils.buildFormattedString
import com.loohp.hkbuseta.common.utils.cache
import com.loohp.hkbuseta.common.utils.createTimetable
import com.loohp.hkbuseta.common.utils.currentBranchStatus
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.differenceSequence
import com.loohp.hkbuseta.common.utils.editDistance
import com.loohp.hkbuseta.common.utils.eitherContains
import com.loohp.hkbuseta.common.utils.getRouteProportions
import com.loohp.hkbuseta.common.utils.indexesOf
import com.loohp.hkbuseta.common.utils.mergeSequences
import com.loohp.hkbuseta.common.utils.nonNullEquals
import com.loohp.hkbuseta.common.utils.outOfOrderSequence
import com.loohp.hkbuseta.common.utils.remove
import com.loohp.hkbuseta.common.utils.sequenceSimilarity
import com.loohp.hkbuseta.common.utils.toIntOrElse
import io.ktor.http.encodeURLPathPart
import kotlinx.datetime.LocalTime
import kotlin.math.absoluteValue
import kotlin.math.max


val bilingualToPrefix = "往" withEn "To "
val bilingualOnlyToPrefix = "只往" withEn "To "

inline val Route.endOfLineText: BilingualText get() {
    val co = co.firstCo()
    return if (co?.isTrain == true) {
        if (co == Operator.MTR) {
            val lineName = routeNumber.getMtrLineName()
            (lineName.zh + "終點站") withEn ("End of the " + lineName.en)
        } else {
            "終點站" withEn "End of Line"
        }
    } else {
        "終點" withEn "End of Route"
    }
}

inline val Operator.isTrain: Boolean get() = when (this) {
    Operator.MTR, Operator.LRT -> true
    else -> false
}
inline val Operator.isFerry: Boolean get() = when (this) {
    Operator.SUNFERRY, Operator.HKKF, Operator.FORTUNEFERRY -> true
    else -> false
}
inline val Operator.isBus: Boolean get() = !(isTrain || isFerry)

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

fun Route.routeGroupKey(co: Operator): String = "$routeNumber,${co.name},${idBoundWithRegion(co)}"

inline val Route.routeGroupKey: String get() = routeGroupKey(co.firstCo()!!)

val mtrLines: List<String> = listOf("AEL", "TCL", "DRL", "EAL", "TML", "SIL", "ISL", "KTL", "TWL", "TKL")

fun String.getMtrLineSortingIndex(): Int {
    return mtrLines.indexOf(this).takeIf { it >= 0 }?: (substring(0, 3.coerceAtMost(length)).toIntOrElse(Int.MAX_VALUE) * 10 + length)
}

fun String.getMtrLineName(orElse: () -> BilingualText = { asBilingualText() }): BilingualText {
    return when (this) {
        "AEL" -> "機場快綫" withEn "Airport Express"
        "TCL" -> "東涌綫" withEn "Tung Chung Line"
        "TML" -> "屯馬綫" withEn "Tuen Ma Line"
        "TKL" -> "將軍澳綫" withEn "Tseung Kwan O Line"
        "EAL" -> "東鐵綫" withEn "East Rail Line"
        "SIL" -> "南港島綫" withEn "South Island Line"
        "TWL" -> "荃灣綫" withEn "Tsuen Wan Line"
        "ISL" -> "港島綫" withEn "Island Line"
        "KTL" -> "觀塘綫" withEn "Kwun Tong Line"
        "DRL" -> "迪士尼綫" withEn "Disneyland Resort Line"
        else -> orElse.invoke()
    }
}

fun Operator.getOperatorColor(elseColor: Long): Long {
    return getColor("", elseColor)
}

fun Operator.getColor(routeNumber: String, elseColor: Long): Long {
    return when (this) {
        Operator.KMB -> if (routeNumber.getKMBSubsidiary() == KMBSubsidiary.LWB) 0xFFF26C33 else 0xFFFF4747
        Operator.CTB -> 0xFFFFE15E
        Operator.NLB -> 0xFF74C497
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

fun Operator.getRouteRemarks(context: AppContext, routeNumber: String): BilingualText? {
    return Registry.getInstance(context).getRouteRemarks()[this]?.get(routeNumber)
}

fun Operator.getOperatorName(language: String, elseName: String = "???"): String {
    return getDisplayName("", false, null, language, elseName)
}

fun Operator.getDisplayName(routeNumber: String, kmbCtbJoint: Boolean, gmbRegion: GMBRegion?, language: String, elseName: String = "???"): String {
    return if (language == "en") when (this) {
        Operator.KMB -> when (routeNumber.getKMBSubsidiary()) {
            KMBSubsidiary.SUNB -> "Sun Bus"
            KMBSubsidiary.LWB -> if (kmbCtbJoint) "LWB/CTB" else "LWB"
            else -> if (kmbCtbJoint) "KMB/CTB" else "KMB"
        }
        Operator.CTB -> "CTB"
        Operator.NLB -> "NLB"
        Operator.MTR_BUS -> "MTR Bus"
        Operator.GMB -> buildString {
            append("GMB")
            when (gmbRegion) {
                GMBRegion.HKI -> append(" HKI")
                GMBRegion.KLN -> append(" KLN")
                GMBRegion.NT -> append(" NT")
                null -> { /* do nothing */ }
            }
        }
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
        Operator.GMB -> buildString {
            append("專線小巴")
            when (gmbRegion) {
                GMBRegion.HKI -> append(" 港島")
                GMBRegion.KLN -> append(" 九龍")
                GMBRegion.NT -> append(" 新界")
                null -> { /* do nothing */ }
            }
        }
        Operator.LRT -> "輕鐵"
        Operator.MTR -> "港鐵"
        Operator.SUNFERRY -> "新渡輪"
        Operator.HKKF -> "港九小輪"
        Operator.FORTUNEFERRY -> "富裕小輪"
        else -> elseName
    }
}

fun Operator.getDisplayFormattedName(routeNumber: String, kmbCtbJoint: Boolean, gmbRegion: GMBRegion?, language: String, elseName: FormattedText = "???".asFormattedText(), elseColor: Long = 0xFFFFFFFF): FormattedText {
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
        Operator.GMB -> buildFormattedString {
            append("GMB", color)
            when (gmbRegion) {
                GMBRegion.HKI -> append(" HKI", color, SmallSize)
                GMBRegion.KLN -> append(" KLN", color, SmallSize)
                GMBRegion.NT -> append(" NT", color, SmallSize)
                null -> { /* do nothing */ }
            }
        }
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
        Operator.GMB -> buildFormattedString {
            append("專線小巴", color)
            when (gmbRegion) {
                GMBRegion.HKI -> append(" 港島", color, SmallSize)
                GMBRegion.KLN -> append(" 九龍", color, SmallSize)
                GMBRegion.NT -> append(" 新界", color, SmallSize)
                null -> { /* do nothing */ }
            }
        }
        Operator.LRT -> "輕鐵".asFormattedText(color)
        Operator.MTR -> "港鐵".asFormattedText(color)
        Operator.SUNFERRY -> "新渡輪".asFormattedText(color)
        Operator.HKKF -> "港九小輪".asFormattedText(color)
        Operator.FORTUNEFERRY -> "富裕小輪".asFormattedText(color)
        else -> elseName
    }
}

val bracketsRemovalRegex: Regex = " *\\([^)]*\\) *".toRegex()
val busTerminusZhRegex: Regex = " *(?:巴士)?總站".toRegex()
val busTerminusEnRegex: Regex = "(?i) *(?:Bus )?Terminus".toRegex()
val circularBracketRegex: Regex = "( *\\([^)]*(?:循環|Circular)[^)]*\\)) *".toRegex()

fun Route.shouldPrependTo(): Boolean {
    return lrtCircular == null
}

fun Route.getCircularPivotIndex(stops: List<Registry.StopData>): Int {
    if (!isCircular) return -1
    val circularMiddle = dest.zh.remove("(循環線)").trim()
    return stops.asSequence()
        .mapIndexedNotNull { i, s ->
            val name = s.stop.name.zh.trim()
            val matches = when {
                circularMiddle.contains("機場") -> name.contains("客運大樓")
                else -> name.eitherContains(circularMiddle)
            }
            if (matches) ((i + 1) to name) else null
        }
        .minByOrNull { (_, n) -> n.editDistance(circularMiddle) }
        ?.first
        ?: stops.first().stop.location.let {
            stops.asSequence()
                .mapIndexed { i, s -> (i + 1) to s }
                .maxBy { (_, s) -> s.stop.location.distance(it) }
                .first
        }
}

fun BilingualText.extractCircularBracket(): BilingualText {
    val zh = circularBracketRegex.findAll(this.zh).lastOrNull()?.groupValues?.get(1)
    val en = circularBracketRegex.findAll(this.en).lastOrNull()?.groupValues?.get(1)
    return (zh?: "") withEn (en?: "")
}

fun Route.resolvedDest(prependTo: Boolean): BilingualText {
    return lrtCircular?: dest.let { if (prependTo) it.prependTo() else it }
}

fun Route.resolvedDestFormatted(prependTo: Boolean, vararg style: FormattingTextContentStyle): BilingualFormattedText {
    return lrtCircular?.asFormattedText(*style)?: dest.let { if (prependTo) it.prependToFormatted(*style) else it.asFormattedText(*style) }
}

fun StopIndexedRouteSearchResultEntry.resolvedDest(prependTo: Boolean, context: AppContext): BilingualText {
    return if (stopInfo == null || (co !== Operator.KMB && co !== Operator.GMB) || !route!!.isCircular) {
        route!!.resolvedDest(prependTo)
    } else {
        val stops = Registry.getInstance(context).getAllStops(route!!.routeNumber, route!!.idBound(co), co, route!!.gmbRegion)
        val middleIndex = route!!.getCircularPivotIndex(stops)
        if (middleIndex >= 0) {
            val stopIndex = stops.indexesOf { it.stopId == stopInfo!!.stopId }.minByOrNull { (it - stopInfoIndex).absoluteValue }
            if (stopIndex != null && stopIndex + 1 >= middleIndex) {
                var dest = stops.last().stop.name
                if (co === Operator.GMB) {
                    dest = dest.zh.remove(bracketsRemovalRegex) withEn dest.en.replace(bracketsRemovalRegex, " ")
                }
                dest = dest.zh.remove(busTerminusZhRegex) withEn dest.en.remove(busTerminusEnRegex)
                dest.let { if (prependTo) it.prependTo() else it } + route!!.dest.extractCircularBracket()
            } else {
                route!!.resolvedDest(prependTo)
            }
        } else {
            route!!.resolvedDest(prependTo)
        }
    }
}

fun StopIndexedRouteSearchResultEntry.resolvedDestFormatted(prependTo: Boolean, context: AppContext, vararg style: FormattingTextContentStyle): BilingualFormattedText {
    return if (stopInfo == null || (co !== Operator.KMB && co !== Operator.GMB) || !route!!.isCircular) {
        route!!.resolvedDestFormatted(prependTo, *style)
    } else {
        val stops = Registry.getInstance(context).getAllStops(route!!.routeNumber, route!!.idBound(co), co, route!!.gmbRegion)
        val middleIndex = route!!.getCircularPivotIndex(stops)
        if (middleIndex >= 0) {
            val stopIndex = stops.indexesOf { it.stopId == stopInfo!!.stopId }.minByOrNull { (it - stopInfoIndex).absoluteValue }
            if (stopIndex != null && stopIndex + 1 >= middleIndex) {
                var dest = stops.last().stop.name
                if (co === Operator.GMB) {
                    dest = dest.zh.remove(bracketsRemovalRegex) withEn dest.en.replace(bracketsRemovalRegex, " ")
                }
                dest = dest.zh.remove(busTerminusZhRegex) withEn dest.en.remove(busTerminusEnRegex)
                dest.let { if (prependTo) it.prependToFormatted(*style) else it.asFormattedText(*style) } + route!!.dest.extractCircularBracket().asFormattedText(*style)
            } else {
                route!!.resolvedDestFormatted(prependTo, *style)
            }
        } else {
            route!!.resolvedDestFormatted(prependTo, *style)
        }
    }
}

fun Route.resolvedDestWithBranch(prependTo: Boolean, branch: Route): BilingualText {
    return lrtCircular?: branch.dest.let { if (prependTo) it.prependTo() else it }
}

fun Route.resolvedDestWithBranchFormatted(prependTo: Boolean, branch: Route, vararg style: FormattingTextContentStyle): BilingualFormattedText {
    return lrtCircular?.asFormattedText(*style)?: branch.dest.let { if (prependTo) it.prependToFormatted(*style) else it.asFormattedText(*style) }
}

fun Route.resolvedDestWithBranch(prependTo: Boolean, branch: Route, selectedStop: Int, selectedStopId: String, context: AppContext): BilingualText {
    val co = co.firstCo()!!
    return if ((co !== Operator.KMB && co !== Operator.GMB) || !branch.isCircular) {
        resolvedDestWithBranch(prependTo, branch)
    } else {
        val stops = Registry.getInstance(context).getAllStops(routeNumber, idBound(co), co, gmbRegion)
        val middleIndex = branch.getCircularPivotIndex(stops)
        if (middleIndex >= 0) {
            val stopIndex = stops.indexesOf { it.stopId == selectedStopId }.minByOrNull { (it - selectedStop).absoluteValue }
            if (stopIndex != null && stopIndex + 1 >= middleIndex) {
                var dest = stops.last().stop.name
                if (co === Operator.GMB) {
                    dest = dest.zh.remove(bracketsRemovalRegex) withEn dest.en.replace(bracketsRemovalRegex, " ")
                }
                dest = dest.zh.remove(busTerminusZhRegex) withEn dest.en.remove(busTerminusEnRegex)
                dest.let { if (prependTo) it.prependTo() else it } + branch.dest.extractCircularBracket()
            } else {
                resolvedDestWithBranch(prependTo, branch)
            }
        } else {
            resolvedDestWithBranch(prependTo, branch)
        }
    }
}

fun Route.resolvedDestWithBranchFormatted(prependTo: Boolean, branch: Route, selectedStop: Int, selectedStopId: String, context: AppContext, vararg style: FormattingTextContentStyle): BilingualFormattedText {
    val co = co.firstCo()!!
    return if ((co !== Operator.KMB && co !== Operator.GMB) || !branch.isCircular) {
        resolvedDestWithBranchFormatted(prependTo, branch, *style)
    } else {
        val stops = Registry.getInstance(context).getAllStops(routeNumber, idBound(co), co, gmbRegion)
        val middleIndex = branch.getCircularPivotIndex(stops)
        if (middleIndex >= 0) {
            val stopIndex = stops.indexesOf { it.stopId == selectedStopId }.minByOrNull { (it - selectedStop).absoluteValue }
            if (stopIndex != null && stopIndex + 1 >= middleIndex) {
                var dest = stops.last().stop.name
                if (co === Operator.GMB) {
                    dest = dest.zh.remove(bracketsRemovalRegex) withEn dest.en.replace(bracketsRemovalRegex, " ")
                }
                dest = dest.zh.remove(busTerminusZhRegex) withEn dest.en.remove(busTerminusEnRegex)
                dest.let { if (prependTo) it.prependToFormatted(*style) else it.asFormattedText(*style) } + branch.dest.extractCircularBracket().asFormattedText(*style)
            } else {
                resolvedDestWithBranchFormatted(prependTo, branch, *style)
            }
        } else {
            resolvedDestWithBranchFormatted(prependTo, branch, *style)
        }
    }
}

val Route.isCircular: Boolean get() {
    if (dest.zh.run { contains("循環線") || contains("循環行走") }) return true
    val stops = stops[co.firstCo()]
    return stops != null && stops.size > 1 && stops.first() == stops.last()
}

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
    return when {
        this is StopIndexedRouteSearchResultEntry -> stopInfo?.let { "$routeKey:${it.stopId}:${stopInfoIndex}:${favouriteStopMode.toString()}" }?: routeKey
        stopInfo?.stopIndex != null -> stopInfo?.let { "$routeKey:${it.stopId}:${it.stopIndex}:${favouriteStopMode.toString()}" }?: routeKey
        else -> stopInfo?.let { "$routeKey:${it.stopId}:${favouriteStopMode.toString()}" }?: routeKey
    }
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
    return Registry.getInstance(context).getAllStops(route.routeNumber, route.idBound(co), co, route.gmbRegion)
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
    val eachAllStop = map { Registry.getInstanceNoUpdateCheck(context).getAllStops(it.route.routeNumber, it.route.idBound(it.co), it.co, it.route.gmbRegion) }
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
    RAW, LABEL_MAIN_BRANCH, LABEL_ALL, LABEL_ALL_AND_MAIN
}

sealed interface RouteDifference {
    fun displayText(stopList: Map<String, Stop>): BilingualText
}
data object RouteDifferenceMain: RouteDifference {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return BilingualText.EMPTY
    }
}
data object RouteDifferenceUnknown: RouteDifference {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return "常規特別路線" withEn "Regular Alt. Route"
    }
}
data class RouteDifferenceDestination(
    val destinationName: BilingualText
): RouteDifference {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return ("開往" withEn "To ") + destinationName
    }
}
data class RouteDifferenceOrigin(
    val originName: BilingualText
): RouteDifference {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return ("從" withEn "From ") + originName + ("開出" withEn "")
    }
}
data class RouteDifferenceVia(
    val viaDiff: List<List<String>>
): RouteDifference {
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
): RouteDifference {
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
): RouteDifference {
    override fun displayText(stopList: Map<String, Stop>): BilingualText {
        return ("先經" withEn "Via ") + firstVia + ("" withEn " first")
    }
}

fun Collection<RouteDifference>.displayText(allStops: List<String>, stopList: Map<String, Stop>): BilingualText {
    val diff = distinct()
    if (diff.size == 1) return diff.first().displayText(stopList)
    return buildList {
        diff.asSequence().filterIsInstance<RouteDifferenceMain>().forEach { add(it.displayText(stopList)) }
        diff.asSequence().filterIsInstance<RouteDifferenceDestination>().forEach { add(it.displayText(stopList)) }
        diff.asSequence().filterIsInstance<RouteDifferenceOrigin>().forEach { add(it.displayText(stopList)) }
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
        diff.asSequence().filterIsInstance<RouteDifferenceViaFirst>().forEach { add(it.displayText(stopList)) }
    }.joinBilingualText(" ".asBilingualText())
}

fun Route.findDifference(mainRoute: Route, stopList: Map<String, Stop>, definitiveMain: Boolean): RouteDifference {
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
                                    if (definitiveMain) RouteDifferenceUnknown else RouteDifferenceMain
                                }
                            } else {
                                if (definitiveMain) RouteDifferenceUnknown else RouteDifferenceMain
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Route.resolveSpecialRemark(context: AppContext, labelType: RemarkType = RemarkType.RAW): BilingualText {
    return cache("resolveSpecialRemark", this, labelType) {
        val co = bound.keys.firstCo()?: throw RuntimeException()
        val bound = idBound(co)
        val (routeList, stopList) = Registry.getInstance(context).let { it.getAllBranchRoutes(routeNumber, bound, co, gmbRegion) to it.getStopList() }
        val allStops by lazy { Registry.getInstance(context).getAllStops(routeNumber, bound, co, gmbRegion).map { it.stopId } }
        val timetable = routeList.createTimetable(Registry.getInstance(context).getServiceDayMap()) { BilingualText.EMPTY }
        val proportions = timetable.takeIf { co == Operator.KMB || co == Operator.CTB }?.getRouteProportions()?: mapOf(routeList.first() to 1F)
        val mainRoute = proportions.maxBy { it.value }.takeIf { (_, v) -> v > 0.9F }?.key?: routeList.first()
        if (proportions[mainRoute]?.let { it > 0.4F } == true) {
            val remark = (if (this != mainRoute) findDifference(mainRoute, stopList, true) else RouteDifferenceMain).displayText(stopList)
            when (labelType) {
                RemarkType.RAW -> remark
                RemarkType.LABEL_MAIN_BRANCH -> remark.let {
                    if (it.isNotBlank()) {
                        it
                    } else {
                        "正常路線" withEn "Normal Route"
                    }
                }
                RemarkType.LABEL_ALL, RemarkType.LABEL_ALL_AND_MAIN -> remark.let {
                    if (it.isNotBlank()) {
                        val entries = timetable.values.asSequence().flatten().filter { e -> e.route == this }.toList()
                        it + when {
                            entries.isNotEmpty() && entries.all { l -> l.within(LocalTime(5, 0), LocalTime(10, 0)) } -> " - 早上繁忙時間特別班" withEn " - Morning Peak Special"
                            entries.isNotEmpty() && entries.all { l -> l.within(LocalTime(16, 30), LocalTime(21, 0)) } -> " - 下午繁忙時間特別班" withEn " - Evening Peak Special"
                            else -> " - 特別班" withEn " - Special"
                        }
                    } else {
                        if (labelType == RemarkType.LABEL_ALL) {
                            "正常路線" withEn "Normal Route"
                        } else {
                            resolvedDest(true) + (" - 正常路線" withEn " - Normal Route")
                        }
                    }
                }
            }
        } else {
            val remark = buildList {
                for (branch in routeList.sortedBy { proportions[it]?: 0F }) {
                    if (this@resolveSpecialRemark != branch) {
                        add(findDifference(branch, stopList, false))
                    }
                }
            }.displayText(allStops, stopList)
            when (labelType) {
                RemarkType.RAW -> remark
                RemarkType.LABEL_MAIN_BRANCH, RemarkType.LABEL_ALL, RemarkType.LABEL_ALL_AND_MAIN -> remark.let {
                    val extra = if (routeList.size != 2) {
                        BilingualText.EMPTY
                    } else {
                        val entries = timetable.values.asSequence().flatten().filter { e -> e.route == this }.toList()
                        when {
                            entries.isNotEmpty() && entries.all { l -> l.within(LocalTime(5, 0), LocalTime(13, 0)) } -> "上午班次" withEn "AM Service"
                            entries.isNotEmpty() && entries.all { l -> l.within(LocalTime(11, 30), LocalTime(1, 0)) } -> "下午班次" withEn "PM Service"
                            else -> BilingualText.EMPTY
                        }
                    }
                    if (it.isNotBlank()) {
                        it + extra.let { e -> if (e.isBlank()) e else " - ".asBilingualText() + e }
                    } else {
                        extra
                    }
                }
            }
        }
    }
}

sealed interface SpecialRouteAlerts {
    data object AlightingStop : SpecialRouteAlerts
    data object CheckRoute : SpecialRouteAlerts
    data object CheckDest : SpecialRouteAlerts
    data class SpecialDest(val routes: List<Route>) : SpecialRouteAlerts
}

fun StopIndexedRouteSearchResultEntry.getSpecialRouteAlerts(context: AppContext): Set<SpecialRouteAlerts> {
    val isAlightingStop = if (co.isBus) {
        if (stopInfo?.data?.name?.zh?.contains("落客") == true) {
            true
        } else {
            val allStops = cachedAllStops?: Registry.getInstance(context).getAllStops(route!!.routeNumber, route!!.idBound(co), co, route!!.gmbRegion)
            val branches = allStops.asSequence().flatMap { it.branchIds }.toSet()
            val branchStops = branches.associateWith { b ->
                allStops.mapIndexedNotNull { i, e -> if (e.branchIds.contains(b)) ((i + 1) to e) else null }
            }
            branchStops.values.all {
                when {
                    it.last().first == stopInfoIndex -> true
                    co === Operator.KMB && !route!!.isCircular -> {
                        val stopIndex = it.indexOfFirst { (i) -> i == stopInfoIndex }
                        val stopName = it.getOrNull(stopIndex)?.second?.stop?.name?.zh?.remove(bracketsRemovalRegex)
                        val nextStopName = it.getOrNull(stopIndex + 1)?.second?.stop?.name?.zh?.remove(bracketsRemovalRegex)
                        stopName nonNullEquals nextStopName
                    }
                    else -> false
                }
            }
        }
    } else {
        false
    }
    return if (isAlightingStop) {
        buildSet {
            addAll(route!!.getSpecialRouteAlerts(context))
            add(SpecialRouteAlerts.AlightingStop)
        }
    } else {
        route!!.getSpecialRouteAlerts(context)
    }
}

fun Route.getSpecialRouteAlerts(context: AppContext): Set<SpecialRouteAlerts> {
    if (co.firstCo()!!.let { it.isTrain || it.isFerry }) return emptySet()
    val branches = cache("getSpecialRouteAlerts", routeGroupKey) {
        Registry.getInstance(context).getAllBranchRoutes(routeNumber, idBound, co.firstCo()!!, gmbRegion)
    }
    val status = branches.currentBranchStatus(currentLocalDateTime(), context, false)
    return buildSet {
        val nonInactiveRoutes = status.filter { (_, v) -> v.activeness > 1 }
        if (nonInactiveRoutes.size > 1) {
            add(SpecialRouteAlerts.CheckRoute)
            if (nonInactiveRoutes.asSequence().mapNotNull { (k) -> k.stops[k.co.firstCo()!!]?.lastOrNull() }.distinct().any(2)) {
                add(SpecialRouteAlerts.CheckDest)
            }
        }
        val activeRoutes = status.asSequence()
            .filter { (_, v) -> v == RouteBranchStatus.ACTIVE }
            .distinctBy { (k) -> k.stops[k.co.firstCo()!!]?.lastOrNull() }
            .map { (k) -> k }
            .toList()
        if (activeRoutes.size > 1) {
            add(SpecialRouteAlerts.SpecialDest(activeRoutes))
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

infix fun Route?.similarTo(other: Route?): Boolean {
    if (this == other) return true
    if (this == null || other == null) return false
    if (routeNumber != other.routeNumber) return false
    if (!co.containsAll(other.co) && !other.co.containsAll(co)) return false
    return true
}

fun String.getMTRStationStreetMapUrl(): String {
    return "https://www.mtr.com.hk/archive/${if (Shared.language == "en") "en" else "ch"}/services/maps/${lowercase()}.pdf"
}

fun String.getMTRStationLayoutUrl(): String {
    return "https://www.mtr.com.hk/archive/${if (Shared.language == "en") "en" else "ch"}/services/layouts/${lowercase()}.pdf"
}

fun Route.getDeepLink(context: AppContext, stopId: String?, stopIndex: Int?): String {
    var link = "${BASE_URL}/route/${getRouteKey(context)?.encodeURLPathPart()}"
    if (stopId != null) {
        link += "/$stopId%2C${stopIndex?: 0}"
    }
    return link
}

fun RouteSearchResultEntry.getDeepLink(): String {
    var link = "${BASE_URL}/route/${routeKey.encodeURLPathPart()}"
    if (stopInfo != null) {
        link += "/${stopInfo!!.stopId}"
    }
    return link
}

fun StopIndexedRouteSearchResultEntry.getDeepLink(): String {
    var link = "${BASE_URL}/route/${routeKey.encodeURLPathPart()}"
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

val Route.waypointsId: String get() {
    val operator = co.firstCo()
    return when {
        operator === Operator.MTR -> routeNumber.substring(0, routeNumber.indexOf("-").takeIf { it >= 0 }?: 3).lowercase()
        operator === Operator.LRT -> if (lrtCircular != null) routeNumber else "${routeNumber}_${bound[operator]}"
        operator?.isFerry == true -> routeNumber
        gtfsId.isBlank() -> "${routeNumber}-${operator}-${if (bound[operator] == "I") "I" else "O"}-${serviceType}"
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
    if (it.co.firstCo() === Operator.NLB) it.nlbId.toIntOrElse() else 0
}.thenBy {
    if (it.co.firstCo() === Operator.GMB) it.gtfsId.toIntOrElse() else 0
}.thenBy {
    it.serviceType.toIntOrElse()
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
    if (it.co.firstCo() === Operator.NLB) it.nlbId.toIntOrElse() else 0
}.thenBy {
    if (it.co.firstCo() === Operator.GMB) it.gtfsId.toIntOrElse() else 0
}.thenBy {
    it.serviceType.toIntOrElse()
}.thenByDescending {
    if (it.co.firstCo() !== Operator.CTB) it.bound[it.co.firstCo()!!] else ""
}

fun RouteSearchResultEntry.findReverse(context: AppContext): RouteSearchResultEntry? {
    return when {
        co === Operator.NLB || co.isFerry -> {
            val stops = route!!.stops[co]?.takeIf { it.size > 1 }?: return null
            val firstId = stops.first()
            val lastId = stops.last()
            val first = firstId.asStop(context)!!.location
            val last = lastId.asStop(context)!!.location
            val predicate: (String, Route, Operator) -> Boolean = predicate@{ _, r, c ->
                if (co != c) return@predicate false
                if (co === Operator.NLB && r.nlbId == route!!.nlbId) return@predicate false
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
            if (byBound.isEmpty() && co === Operator.GMB) {
                result.firstOrNull { it.routeKey != routeKey }
            } else {
                byBound.firstOrNull()
            }
        }
        else -> {
            val result = Registry.getInstance(context).findRoutes(route!!.routeNumber.substring(0, 2), false) { _, r, c -> co == c && r.lrtCircular != null }
            result.firstOrNull { it.route!!.routeNumber != route!!.routeNumber }
        }
    }
}

fun Route.findSimilarRoutes(co: Operator, context: AppContext): List<RouteSearchResultEntry> {
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
        routeNumber.firstOrNull()?.takeIf { c -> c.isLetter() } != it.route!!.routeNumber.firstOrNull()?.takeIf { c -> c.isLetter() }
    }.thenBy {
        routeNumber.lastOrNull()?.takeIf { c -> c.isLetter() } != it.route!!.routeNumber.lastOrNull()?.takeIf { c -> c.isLetter() }
    }.thenBy {
        similarities.getOrPut(it.route!!.routeGroupKey(it.co)) {
            val s = it.route!!.stops[it.co]?.takeIf { s -> s.isNotEmpty() }?: listOf(element = "----")
            max(stops.sequenceSimilarity(s, equality), s.sequenceSimilarity(stops, equality))
        }
    })
}

fun List<StopIndexedRouteSearchResultEntry>.identifyGeneralDirections(context: AppContext): Map<GeneralDirection, List<StopIndexedRouteSearchResultEntry>> {
    val registry = Registry.getInstance(context)
    val axisDeviations: MutableMap<String, MutableMap<GeneralDirectionAxis, MutableList<Double>>> = mutableMapOf()
    val map: MutableMap<StopIndexedRouteSearchResultEntry, Double> = mutableMapOf()
    for (entry in this@identifyGeneralDirections) {
        val originStop = entry.stopInfo?: continue
        val origin = originStop.data?.location?: continue
        val stops = entry.route?.stops?.get(entry.co)?: continue
        val stopIndex = stops.indexOfFirst { it == originStop.stopId }.takeIf { it >= 0 }?: continue
        val nextStop = stops.getOrNull(stopIndex + 1)?.asStop(context)?.location
        val (direction, bearing) = if (nextStop == null) {
            val previousStop = stops.getOrNull(stopIndex - 1)?.asStop(context)?.location?: continue
            val bearing = previousStop.bearingTo(origin)
            bearing.toGeneralDirection().opposite to bearing
        } else {
            val bearing = nextStop.bearingTo(origin)
            bearing.toGeneralDirection() to bearing
        }
        val deviation = direction.deviation(bearing)
        map[entry] = bearing
        registry.findEquivalentStops(originStop.stopId, 0.3).forEach {
            axisDeviations.getOrPut(it.stopId) { mutableMapOf() }.getOrPut(direction.axis) { mutableListOf() }.add(deviation)
        }
    }
    val axisDeviationsResult: Map<String, GeneralDirectionAxis> = axisDeviations.mapValues { (_, v) ->
        v.minBy { (_, d) -> d.asSequence().map { it.absoluteValue }.average() }.key
    }
    val result: MutableMap<GeneralDirection, MutableList<StopIndexedRouteSearchResultEntry>> = mutableMapOf()
    for ((entry, bearing) in map) {
        val axis = axisDeviationsResult[entry.stopInfo?.stopId]?.directions?: GeneralDirection.entries
        val direction = bearing.toGeneralDirection(axis)
        result.getOrPut(direction) { mutableListOf() }.add(entry)
    }
    return result.asSequence().sortedBy { (k) -> k }.associate { (k, v) -> k to v }
}

fun calculateServiceTimeCategory(routeNumber: String, co: Operator, timetableCalculate: () -> ServiceTimeCategory): ServiceTimeCategory {
    if (co.isBus) {
        if (routeNumber.firstOrNull() == 'N' || routeNumber.lastOrNull() == 'N') return ServiceTimeCategory.NIGHT
    }
    return timetableCalculate.invoke()
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

fun List<Registry.StopData>.mapTrafficSnapshots(waypoints: RouteWaypoints?, trafficSnapshots: Array<out List<TrafficSnapshotPoint>>?): Array<out List<TrafficSnapshotPoint>>? {
    return if (waypoints == null || trafficSnapshots == null) {
        null
    } else {
        val array = Array(size) { emptyList<TrafficSnapshotPoint>() }
        var currentIndex = 0
        for ((i, s) in this@mapTrafficSnapshots.withIndex()) {
            if (waypoints.stops.getOrNull(currentIndex) nonNullEquals s.stop) {
                array[i] = trafficSnapshots.getOrElse(currentIndex++) { emptyList() }
            }
        }
        array
    }
}

fun getRedirectToMTRJourneyPlannerUrl(startingStationId: String?, destinationStationId: String?, context: AppContext): String {
    val url = "https://www.mtr.com.hk/${if (Shared.language == "en") "en" else "ch"}/customer/jp/index.php"

    val startingStationIsLightRail = startingStationId?.identifyStopCo()?.contains(Operator.LRT) == true
    val startingStationTypeId = if (startingStationIsLightRail) "LRStation" else "HRStation"
    val startingStationMtrId = if (startingStationIsLightRail) startingStationId?.substring(2)?.toInt() else startingStationId?.asStop(context)?.mtrIds?.firstOrNull()

    val destinationStationIsLightRail = destinationStationId?.identifyStopCo()?.contains(Operator.LRT) == true
    val destinationStationTypeId = if (destinationStationIsLightRail) "LRStation" else "HRStation"
    val destinationStationMtrId = if (destinationStationIsLightRail) destinationStationId?.substring(2)?.toInt() else destinationStationId?.asStop(context)?.mtrIds?.firstOrNull()

    val args = buildList {
        if (startingStationMtrId != null) {
            add("oValue=$startingStationMtrId&oType=$startingStationTypeId")
        }

        if (destinationStationMtrId != null) {
            add("dValue=$destinationStationMtrId&dType=$destinationStationTypeId")
        }
    }

    return if (args.isEmpty()) url else url + args.joinToString(prefix = "?", separator = "&")
}