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

package com.loohp.hkbuseta.objects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import com.loohp.hkbuseta.appcontext.AppContext
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.asAnnotatedString


private val bilingualToPrefix = "往" withEn "To "

inline val Operator.isTrain: Boolean get() = this == Operator.MTR || this == Operator.LRT

fun Operator.getColor(routeNumber: String, elseColor: Color): Color {
    return when (this) {
        Operator.KMB -> if (routeNumber.getKMBSubsidiary() == KMBSubsidiary.LWB) Color(0xFFF26C33) else Color(0xFFFF4747)
        Operator.CTB -> Color(0xFFFFE15E)
        Operator.NLB -> Color(0xFF9BFFC6)
        Operator.MTR_BUS -> Color(0xFFAAD4FF)
        Operator.GMB -> Color(0xFF36FF42)
        Operator.LRT -> Color(0xFFD3A809)
        Operator.MTR -> when (routeNumber) {
            "AEL" -> Color(0xFF00888E)
            "TCL" -> Color(0xFFF3982D)
            "TML" -> Color(0xFF9C2E00)
            "TKL" -> Color(0xFF7E3C93)
            "EAL" -> Color(0xFF5EB7E8)
            "SIL" -> Color(0xFFCBD300)
            "TWL" -> Color(0xFFE60012)
            "ISL" -> Color(0xFF0075C2)
            "KTL" -> Color(0xFF00A040)
            "DRL" -> Color(0xFFEB6EA5)
            else -> elseColor
        }
        else -> elseColor
    }
}

fun Operator.getLineColor(routeNumber: String, elseColor: Color): Color {
    return if (this == Operator.LRT) when (routeNumber) {
        "505" -> Color(0xFFDA2127)
        "507" -> Color(0xFF00A652)
        "610" -> Color(0xFF551C15)
        "614" -> Color(0xFF00BFF3)
        "614P" -> Color(0xFFF4858E)
        "615" -> Color(0xFFFFDD00)
        "615P" -> Color(0xFF016682)
        "705" -> Color(0xFF73BF43)
        "706" -> Color(0xFFB47AB5)
        "751" -> Color(0xFFF48221)
        "761P" -> Color(0xFF6F2D91)
        else -> getColor(routeNumber, elseColor)
    } else getColor(routeNumber, elseColor)
}

fun Operator.getDisplayRouteNumber(routeNumber: String, shortened: Boolean = false): String {
    return if (this == Operator.MTR) {
        if (shortened && Shared.language == "en") routeNumber else Shared.getMtrLineName(routeNumber, "???")
    } else if (this == Operator.KMB && routeNumber.getKMBSubsidiary() == KMBSubsidiary.SUNB) {
        "NR".plus(routeNumber)
    } else {
        routeNumber
    }
}

fun String.getKMBSubsidiary(): KMBSubsidiary {
    val routeNumberFiltered = if (this.startsWith("N")) this.substring(1) else this
    if (routeNumberFiltered.startsWith("A") || routeNumberFiltered.startsWith("E") || routeNumberFiltered.startsWith("S")) {
        return KMBSubsidiary.LWB
    }
    return when (this) {
        "N30", "N31", "N42", "N42A", "N64", "R8", "R33", "R42", "X1", "X33", "X34", "X40", "X43", "X47" -> KMBSubsidiary.LWB
        "331", "331S", "917", "918", "945" -> KMBSubsidiary.SUNB
        else -> KMBSubsidiary.KMB
    }

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
        else -> elseName
    }
}

fun Route.resolvedDest(prependTo: Boolean): BilingualText {
    return lrtCircular?: dest.let { if (prependTo) it.prependTo() else it }
}

infix fun String.withEn(en: String): BilingualText {
    return BilingualText(this, en)
}

infix fun String.withZh(zh: String): BilingualText {
    return BilingualText(zh, this)
}

fun BilingualText.prependTo(): BilingualText {
    return bilingualToPrefix + this
}

fun DoubleArray.toCoordinates(): Coordinates {
    return Coordinates.fromArray(this)
}

fun Route.getRouteKey(context: AppContext): String? {
    return Registry.getInstance(context).getRouteKey(this)
}

fun String.asStop(context: AppContext): Stop? {
    return Registry.getInstance(context).getStopById(this)
}

fun String.identifyStopCo(): Operator? {
    return Operator.values().firstOrNull { it.matchStopIdPattern(this) }
}

fun BilingualText.asAnnotatedText(spanStyle: SpanStyle? = null): BilingualAnnotatedText {
    return BilingualAnnotatedText(zh.asAnnotatedString(spanStyle), en.asAnnotatedString(spanStyle))
}

inline val Stop.remarkedName: BilingualAnnotatedText get() {
    return remark?.let { BilingualAnnotatedText(
        buildAnnotatedString {
            append(name.zh)
            append(it.zh, SpanStyle(fontSize = TextUnit.Small))
        },
        buildAnnotatedString {
            append(name.en)
            append(it.en, SpanStyle(fontSize = TextUnit.Small))
        }
    ) }?: name.asAnnotatedText()
}

inline val RouteSearchResultEntry.uniqueKey: String get() {
    return stopInfo?.let { routeKey.plus(":").plus(it.stopId) }?: routeKey
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
    return eachAllStop.withIndex().map { indexed ->
        val (index, allStops) = indexed
        allStops.withIndex()
            .map { it.value.stop.location.distance(closestStop.stop.location) to it }
            .minBy { it.first }
            .let { this[index] to (if (it.first <= 0.15) FavouriteResolvedStop(it.second.index + 1, it.second.value.stopId, it.second.value.stop, it.second.value.route) else null) }
    }
}