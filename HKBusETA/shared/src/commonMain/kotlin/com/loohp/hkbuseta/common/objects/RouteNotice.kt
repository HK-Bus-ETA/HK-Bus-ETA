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

package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.BigSize
import com.loohp.hkbuseta.common.utils.BoldStyle
import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.buildFormattedString
import com.loohp.hkbuseta.common.utils.getJSONResponse
import com.loohp.hkbuseta.common.utils.getTextResponse
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.remove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject


enum class RouteNoticeImportance {
    IMPORTANT, NORMAL, NOT_IMPORTANT
}

@Immutable
sealed class RouteNotice(
    val title: String,
    val co: Operator?,
    val importance: RouteNoticeImportance,
    val sort: Int = Int.MAX_VALUE
): Comparable<RouteNotice> {
    companion object {
        private val routeNoticeComparator = compareBy<RouteNotice> { it.importance }
            .thenBy { it.co?.ordinal?: Int.MAX_VALUE }
            .thenBy { it.sort }
    }
    override fun compareTo(other: RouteNotice): Int = routeNoticeComparator.compare(this, other)
}

@Immutable
class RouteNoticeExternal(
    title: String,
    co: Operator?,
    important: RouteNoticeImportance,
    val url: String,
    sort: Int = Int.MAX_VALUE
): RouteNotice(title, co, important, sort)

@Immutable
class RouteNoticeText(
    title: String,
    co: Operator?,
    important: RouteNoticeImportance,
    sort: Int = Int.MAX_VALUE,
    val isRealTitle: Boolean,
    val content: String
): RouteNotice(title, co, important, sort) {
    val display: FormattedText by lazy { buildFormattedString(extractUrls = true) {
        if (isRealTitle) {
            append(title, BigSize, BoldStyle)
            appendLineBreak(2)
        }
        append(content)
    } }
}

val mtrRouteMapNotice: RouteNoticeExternal get() {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "MTR System Map" else "港鐵路綫圖",
        co = Operator.MTR,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.mtr.com.hk/archive/${if (Shared.language == "en") "en" else "ch"}/services/routemap.pdf".prependPdfViewerToUrl()
    )
}

val lrtRouteMapNotice: RouteNoticeExternal get() {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "Light Rail Route Map" else "輕鐵路綫圖",
        co = Operator.LRT,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.mtr.com.hk/archive/${if (Shared.language == "en") "en" else "ch"}/services/LR_routemap.pdf".prependPdfViewerToUrl()
    )
}

val kmbBbiPage: RouteNoticeExternal get() {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "KMB Octopus and E-payment Bus-Bus Interchange Schemes" else "九巴八達通及電子支付巴士轉乘計劃",
        co = Operator.KMB,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.kmb.hk/interchange_bbi.html"
    )
}

fun ctbRouteNotice(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "Citybus Route Updates" else "城巴路線最新資訊",
        co = Operator.CTB,
        important = RouteNoticeImportance.NORMAL,
        url = "https://mobile.citybus.com.hk/nwp3/specialNote.php?r=${route.routeNumber}"
    )
}

fun ctbRouteInterchangePage(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "Citybus Route Interchange Discount" else "城巴路線轉乘優惠",
        co = Operator.CTB,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.citybus.com.hk/concession/${if (Shared.language == "en") "en" else "tc"}/route/${route.routeNumber}"
    )
}

fun kmbRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "KMB Route Info" else "九巴路線資訊",
        co = Operator.KMB,
        important = RouteNoticeImportance.NORMAL,
        url = "https://search.kmb.hk/KMBWebSite/?action=routesearch&route=${route.routeNumber}&lang=${if (Shared.language == "en") "en" else "zh-hk"}"
    )
}

fun ctbRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "Citybus Route Info" else "城巴路線資訊",
        co = Operator.CTB,
        important = RouteNoticeImportance.NORMAL,
        url = "https://mobile.citybus.com.hk/nwp3/?f=1&ds=${route.routeNumber}&dsmode=1&l=${if (Shared.language == "en") 1 else 0}"
    )
}

fun nlbRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "NLB Route Info" else "嶼巴路線資訊",
        co = Operator.NLB,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.nlb.com.hk/route/detail/${route.nlbId}"
    )
}

fun gmbRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "GMB Route Info" else "專線小巴路線資訊",
        co = Operator.GMB,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.16seats.net/${if (Shared.language === "en") "eng" else "chi"}/gmb/g${route.gmbRegion?.name?.take(1)?.lowercase()}_${route.routeNumber.lowercase()}.html"
    )
}

fun mtrBusRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "MTR Bus Route Info" else "港鐵巴士路線資訊",
        co = Operator.MTR_BUS,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.mtr.com.hk/${if (Shared.language === "en") "en" else "ch"}/customer/services/searchBusRouteDetails.php?routeID=${route.routeNumber}"
    )
}

val lrtRouteInfo: RouteNoticeExternal get() {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "Light Rail Route Info" else "輕鐵路線資訊",
        co = Operator.LRT,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.mtr.com.hk/${if (Shared.language === "en") "en" else "ch"}/customer/services/schedule_index.html"
    )
}

val mtrRouteInfo: RouteNoticeExternal get() {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "MTR Route Info" else "港鐵路線資訊",
        co = Operator.MTR,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.mtr.com.hk/${if (Shared.language === "en") "en" else "ch"}/customer/services/train_service_index.html"
    )
}

val sunFerryInfo: RouteNoticeExternal get() {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "Sun Ferry Info" else "新渡輪資訊",
        co = Operator.SUNFERRY,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.sunferry.com.hk/${if (Shared.language === "en") "en" else "zh"}/route-and-fare"
    )
}

val hkkfInfo: RouteNoticeExternal get() {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "Hong Kong and Kowloon Ferry Info" else "港九小輪資訊",
        co = Operator.HKKF,
        important = RouteNoticeImportance.NORMAL,
        url = if (Shared.language == "en") "https://hkkf.com.hk/en/timetables/" else "https://hkkf.com.hk/%e8%88%aa%e7%8f%ad%e8%b3%87%e8%a8%8a/"
    )
}

val fortuneFerryInfo: RouteNoticeExternal get() {
    return RouteNoticeExternal(
        title = if (Shared.language == "en") "Fortune Ferry Info" else "富裕小輪資訊",
        co = Operator.FORTUNEFERRY,
        important = RouteNoticeImportance.NORMAL,
        url = "https://www.fortuneferry.com.hk/${if (Shared.language === "en") "en" else "zh"}/route-and-fare"
    )
}

private val operatorNoticeDefaults: Map<Operator, Route.(MutableList<RouteNotice>) -> Unit> = buildMap {
    this[Operator.KMB] = {
        it.add(kmbRouteInfo(this))
        it.add(kmbBbiPage)
    }
    this[Operator.CTB] = {
        it.add(ctbRouteInfo(this))
        it.add(ctbRouteNotice(this))
        it.add(ctbRouteInterchangePage(this))
    }
    this[Operator.NLB] = {
        it.add(nlbRouteInfo(this))
    }
    this[Operator.GMB] = {
        it.add(gmbRouteInfo(this))
    }
    this[Operator.MTR_BUS] = {
        it.add(mtrBusRouteInfo(this))
    }
    this[Operator.MTR] = {
        it.add(mtrRouteMapNotice)
        it.add(mtrRouteInfo)
    }
    this[Operator.LRT] = {
        it.add(lrtRouteMapNotice)
        it.add(lrtRouteInfo)
    }
    this[Operator.SUNFERRY] = {
        it.add(sunFerryInfo)
    }
    this[Operator.HKKF] = {
        it.add(hkkfInfo)
    }
    this[Operator.FORTUNEFERRY] = {
        it.add(fortuneFerryInfo)
    }
}

fun Route.defaultOperatorNotices(addTo: MutableList<RouteNotice>) {
    for (operator in co) {
        operatorNoticeDefaults[operator]?.invoke(this, addTo)
    }
}

private val mtrMatches: Set<String> = setOf(
    "機場快綫",
    "東涌綫",
    "屯馬綫",
    "將軍澳綫",
    "東鐵綫",
    "南港島綫",
    "荃灣綫",
    "港島綫",
    "觀塘綫",
    "迪士尼綫"
)

fun TrafficNewsEntry.getOperators(): Set<Operator> {
    val operators = Operator.values().asSequence().filter {
        val name = it.getOperatorName("zh")
        incidentHeadingCN.contains(name) || incidentDetailCN.contains(name)
    }.toSet()
    return if (!operators.contains(Operator.MTR)) {
        if (mtrMatches.any { incidentHeadingCN.contains(it) || incidentDetailCN.contains(it) }) {
            operators + Operator.MTR
        } else {
            operators
        }
    } else {
        operators
    }
}

fun TrafficNewsEntry.getTitle(): String {
    return (if (Shared.language == "en") incidentDetailEN else incidentDetailCN)
        .takeIf { it.isNotBlank() }?: if (Shared.language == "en") incidentHeadingEN else incidentHeadingCN
}

fun TrafficNewsEntry.getBody(instance: AppContext): String {
    val time = instance.formatDateTime(LocalDateTime.parse(announcementDate), true)
    return "${if (Shared.language == "en") contentEN else contentCN}\n\n${if (Shared.language == "en") "Last Updated" else "更新時間"}: $time"
}

fun SpecialTrafficNewsEntry.getOperators(): Set<Operator> {
    val operators = Operator.values().asSequence().filter {
        val name = it.getOperatorName("zh")
        chinText.contains(name)
    }.toSet()
    return if (!operators.contains(Operator.MTR)) {
        if (mtrMatches.any { chinText.contains(it) }) {
            operators + Operator.MTR
        } else {
            operators
        }
    } else {
        operators
    }
}

fun SpecialTrafficNewsEntry.getTitle(): String {
    return (if (Shared.language == "en") engText else chinText)
        .split("\\R".toRegex()).firstOrNull()?: if (Shared.language == "en") "Special Arrangement" else "特別安排"
}

fun SpecialTrafficNewsEntry.getBody(instance: AppContext): String {
    val parts = "([0-9]{4})/([0-9]{1,2})/([0-9]{1,2}) (.{2}) ([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})".toRegex().matchEntire(referenceDate.trim())
    val time = if (parts?.groupValues?.size != 8) {
        referenceDate
    } else {
        try {
            val year = parts.groupValues[1].toInt()
            val month = parts.groupValues[2].toInt()
            val day = parts.groupValues[3].toInt()
            var hour = parts.groupValues[5].toInt()
            val minute = parts.groupValues[6].toInt()
            val second = parts.groupValues[7].toInt()
            if (parts.groupValues[4].contains("下午")) {
                hour = (hour % 12) + 12
            }
            instance.formatDateTime(LocalDateTime(year, month, day, hour, minute, second), true)
        } catch (e: Throwable) {
            e.printStackTrace()
            referenceDate
        }
    }
    return "${if (Shared.language == "en") engText else chinText}\n\n${if (Shared.language == "en") "Last Updated" else "更新時間"}: $time"
}

fun String.prependPdfViewerToUrl(): String {
    return if (endsWith(".pdf")) "https://docs.google.com/gview?embedded=true&url=$this" else this
}

private val ctbNoticePattern = "onclick=\\\"javascript:window\\.open\\('([^']+)'\\).*?<br>(.*?)</td>".toRegex()

private val operatorNoticeFetchers: Map<Operator, suspend Route.(MutableList<RouteNotice>) -> Unit> = buildMap {
    this[Operator.KMB] = { list ->
        val data = getJSONResponse<JsonObject>("https://search.kmb.hk/KMBWebSite/Function/FunctionRequest.ashx?action=getAnnounce&route=${routeNumber}&bound=${if (bound[Operator.KMB] == "O") 1 else 2}")!!
        val notices = data.optJsonArray("data")!!
        for ((index, obj) in notices.withIndex()) {
            val notice = obj.jsonObject
            val url = "https://search.kmb.hk/KMBWebSite/AnnouncementPicture.ashx?url=${notice.optString("kpi_noticeimageurl")}".prependPdfViewerToUrl()
            list.add(RouteNoticeExternal(
                title = notice.optString(if (Shared.language == "en") "kpi_title" else "kpi_title_chi"),
                co = Operator.KMB,
                important = RouteNoticeImportance.IMPORTANT,
                url = url,
                sort = index
            ))
        }
    }
    this[Operator.CTB] = { list ->
        val data = getTextResponse("https://mobile.citybus.com.hk/nwp3/getnotice.php?id=${routeNumber}&l=${if (Shared.language == "en") 1 else 0}")!!.string()
        for ((index, match) in ctbNoticePattern.findAll(data.remove("\\n|\\t".toRegex())).withIndex()) {
            list.add(RouteNoticeExternal(
                title = match.groupValues[2],
                co = Operator.CTB,
                important = RouteNoticeImportance.IMPORTANT,
                url = match.groupValues[1].prependPdfViewerToUrl(),
                sort = index
            ))
        }
    }
}

suspend fun Route.fetchOperatorNotices(scope: CoroutineScope, addTo: MutableList<RouteNotice>) {
    for (operator in co) {
        scope.launch {
            try {
                operatorNoticeFetchers[operator]?.invoke(this@fetchOperatorNotices, addTo)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}