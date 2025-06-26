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

package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.utils.BigSize
import com.loohp.hkbuseta.common.utils.BoldStyle
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject


enum class RouteNoticeImportance {
    IMPORTANT, NORMAL, NOT_IMPORTANT
}

@Serializable
@Immutable
sealed interface RouteNotice: Comparable<RouteNotice> {
    companion object {
        private val routeNoticeComparator = compareBy<RouteNotice> { it.importance }
            .thenBy { it.co?.ordinal?: Int.MAX_VALUE }
            .thenBy { it.sort }
    }

    val title: BilingualText
    val co: Operator?
    val importance: RouteNoticeImportance
    val sort: Int
    val possibleTwoWaySectionFareInfo: Boolean
    
    override fun compareTo(other: RouteNotice): Int = routeNoticeComparator.compare(this, other)
}

@Serializable
@Immutable
class RouteNoticeExternal(
    override val title: BilingualText,
    override val co: Operator?,
    override val importance: RouteNoticeImportance,
    val url: BilingualText,
    override val sort: Int = Int.MAX_VALUE,
    override val possibleTwoWaySectionFareInfo: Boolean = false
): RouteNotice {
    val isPdf: Boolean = url.zh.endsWith(".pdf") && url.en.endsWith(".pdf")
}

@Serializable
@Immutable
class RouteNoticeText(
    override val title: BilingualText,
    override val co: Operator?,
    override val importance: RouteNoticeImportance,
    override val sort: Int = Int.MAX_VALUE,
    val isRealTitle: Boolean,
    val content: BilingualText,
    override val possibleTwoWaySectionFareInfo: Boolean = false
): RouteNotice {
    val display: BilingualFormattedText by lazy {
        buildFormattedString(extractUrls = true) {
            if (isRealTitle) {
                append(title.zh, BigSize, BoldStyle)
                appendLineBreak(2)
            }
            append(content.zh)
        } withEn buildFormattedString(extractUrls = true) {
            if (isRealTitle) {
                append(title.en, BigSize, BoldStyle)
                appendLineBreak(2)
            }
            append(content.en)
        }
    }
}

val mtrLineStatus: RouteNoticeExternal = RouteNoticeExternal(
    title = "車務狀況" withEn "Service Status",
    co = Operator.MTR,
    importance = RouteNoticeImportance.NORMAL,
    url = "https://tnews.mtr.com.hk/alert/service_status_tc_m.html" withEn "https://tnews.mtr.com.hk/alert/service_status_m.html"
)

val lrtLineStatus: RouteNoticeExternal get() {
    val status = mtrLineStatus
    return RouteNoticeExternal(
        title = status.title,
        co = Operator.LRT,
        importance = status.importance,
        url = status.url
    )
}

val mtrRouteMapNotice: RouteNoticeExternal = RouteNoticeExternal(
    title = "港鐵路綫圖" withEn "MTR System Map",
    co = Operator.MTR,
    importance = RouteNoticeImportance.NORMAL,
    url = "https://www.mtr.com.hk/archive/ch/services/routemap.pdf" withEn "https://www.mtr.com.hk/archive/en/services/routemap.pdf"
)

val lrtRouteMapNotice: RouteNoticeExternal = RouteNoticeExternal(
    title = "輕鐵路綫圖" withEn "Light Rail Route Map",
    co = Operator.LRT,
    importance = RouteNoticeImportance.NORMAL,
    url = "https://www.mtr.com.hk/archive/ch/services/LR_routemap.pdf" withEn "https://www.mtr.com.hk/archive/en/services/LR_routemap.pdf"
)

val kmbBbiPage: RouteNoticeExternal = RouteNoticeExternal(
    title = "九巴八達通及電子支付巴士轉乘計劃" withEn "KMB Octopus and E-payment Bus-Bus Interchange Schemes",
    co = Operator.KMB,
    importance = RouteNoticeImportance.NORMAL,
    url = "https://www.kmb.hk/interchange_bbi.html".asBilingualText()
)

fun ctbRouteNotice(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = "城巴路線最新資訊" withEn "Citybus Route Updates",
        co = Operator.CTB,
        importance = RouteNoticeImportance.NORMAL,
        url = "https://mobile.citybus.com.hk/nwp3/specialNote.php?r=${route.routeNumber}".asBilingualText()
    )
}

fun ctbRouteInterchangePage(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = "城巴路線轉乘優惠" withEn "Citybus Route Interchange Discount",
        co = Operator.CTB,
        importance = RouteNoticeImportance.NORMAL,
        url = "https://www.citybus.com.hk/concession/tc/route/${route.routeNumber}" withEn "https://www.citybus.com.hk/concession/en/route/${route.routeNumber}"
    )
}

fun kmbRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = "九巴路線資訊" withEn "KMB Route Info",
        co = Operator.KMB,
        importance = RouteNoticeImportance.NORMAL,
        url = "https://search.kmb.hk/KMBWebSite/?action=routesearch&route=${route.routeNumber}&lang=zh-hk" withEn "https://search.kmb.hk/KMBWebSite/?action=routesearch&route=${route.routeNumber}&lang=en"
    )
}

fun ctbRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = "城巴路線資訊" withEn "Citybus Route Info",
        co = Operator.CTB,
        importance = RouteNoticeImportance.NORMAL,
        url = "https://mobile.citybus.com.hk/nwp3/?f=1&ds=${route.routeNumber}&dsmode=1&l=0" withEn "https://mobile.citybus.com.hk/nwp3/?f=1&ds=${route.routeNumber}&dsmode=1&l=1",
        possibleTwoWaySectionFareInfo = true
    )
}

fun nlbRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = "嶼巴路線資訊" withEn "NLB Route Info",
        co = Operator.NLB,
        importance = RouteNoticeImportance.NORMAL,
        url = "https://www.nlb.com.hk/route/detail/${route.nlbId}".asBilingualText(),
        possibleTwoWaySectionFareInfo = true
    )
}

fun gmbRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = "專線小巴路線資訊" withEn "GMB Route Info",
        co = Operator.GMB,
        importance = RouteNoticeImportance.NORMAL,
        url = "https://www.16seats.net/chi/gmb/g${route.gmbRegion?.name?.take(1)?.lowercase()}_${route.routeNumber.lowercase()}.html" withEn "https://www.16seats.net/eng/gmb/g${route.gmbRegion?.name?.take(1)?.lowercase()}_${route.routeNumber.lowercase()}.html"
    )
}

fun mtrBusRouteInfo(route: Route): RouteNoticeExternal {
    return RouteNoticeExternal(
        title = "港鐵巴士路線資訊" withEn "MTR Bus Route Info",
        co = Operator.MTR_BUS,
        importance = RouteNoticeImportance.NORMAL,
        url = "https://www.mtr.com.hk/ch/customer/services/searchBusRouteDetails.php?routeID=${route.routeNumber}" withEn "https://www.mtr.com.hk/en/customer/services/searchBusRouteDetails.php?routeID=${route.routeNumber}"
    )
}

val lrtRouteInfo: RouteNoticeExternal = RouteNoticeExternal(
    title = "輕鐵路線資訊" withEn "Light Rail Route Info",
    co = Operator.LRT,
    importance = RouteNoticeImportance.NORMAL,
    url = "https://www.mtr.com.hk/ch/customer/services/schedule_index.html" withEn "https://www.mtr.com.hk/en/customer/services/schedule_index.html"
)

val mtrRouteInfo: RouteNoticeExternal = RouteNoticeExternal(
    title = "港鐵路線資訊" withEn "MTR Route Info",
    co = Operator.MTR,
    importance = RouteNoticeImportance.NORMAL,
    url = "https://www.mtr.com.hk/ch/customer/services/train_service_index.html" withEn "https://www.mtr.com.hk/en/customer/services/train_service_index.html"
)

val sunFerryInfo: RouteNoticeExternal = RouteNoticeExternal(
    title = "新渡輪資訊" withEn "Sun Ferry Info",
    co = Operator.SUNFERRY,
    importance = RouteNoticeImportance.NORMAL,
    url = "https://www.sunferry.com.hk/zh/route-and-fare" withEn "https://www.sunferry.com.hk/en/route-and-fare"
)

val hkkfInfo: RouteNoticeExternal = RouteNoticeExternal(
    title = "港九小輪資訊" withEn "Hong Kong and Kowloon Ferry Info",
    co = Operator.HKKF,
    importance = RouteNoticeImportance.NORMAL,
    url = "https://hkkf.com.hk/%e8%88%aa%e7%8f%ad%e8%b3%87%e8%a8%8a/" withEn "https://hkkf.com.hk/en/timetables/"
)

val fortuneFerryInfo: RouteNoticeExternal = RouteNoticeExternal(
    title = "富裕小輪資訊" withEn "Fortune Ferry Info",
    co = Operator.FORTUNEFERRY,
    importance = RouteNoticeImportance.NORMAL,
    url = "https://www.fortuneferry.com.hk/zh/route-and-fare" withEn "https://www.fortuneferry.com.hk/en/route-and-fare"
)

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
        it.add(mtrLineStatus)
        it.add(mtrRouteMapNotice)
        it.add(mtrRouteInfo)
    }
    this[Operator.LRT] = {
        it.add(lrtLineStatus)
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

fun TrafficNewsEntry.getTitle(): BilingualText {
    return BilingualText(
        zh = incidentDetailCN.takeIf { it.isNotBlank() }?: incidentHeadingCN,
        en = incidentDetailEN.takeIf { it.isNotBlank() }?: incidentHeadingEN
    )
}

fun TrafficNewsEntry.getBody(instance: AppContext): BilingualText {
    val time = instance.formatDateTime(LocalDateTime.parse(announcementDate), true)
    return BilingualText(
        zh = "$contentCN\n\n更新時間: $time",
        en = "$contentEN\n\nLast Updated: $time"
    )
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

fun SpecialTrafficNewsEntry.getTitle(): BilingualText {
    return BilingualText(
        zh = chinText.split("\\R".toRegex()).firstOrNull()?: "特別安排",
        en = engText.split("\\R".toRegex()).firstOrNull()?: "Special Arrangement"
    )
}

fun SpecialTrafficNewsEntry.getBody(instance: AppContext): BilingualText {
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
    return BilingualText(
        zh = "$chinText\n\n更新時間: $time",
        en = "$engText\n\nLast Updated: $time"
    )
}

private val ctbNoticePattern = "onclick=\\\"javascript:window\\.open\\('([^']+)'\\).*?<br>(.*?)</td>".toRegex()

private val operatorNoticeFetchers: Map<Operator, suspend Route.(MutableList<RouteNotice>) -> Unit> = buildMap {
    this[Operator.KMB] = { list ->
        val data = getJSONResponse<JsonObject>("https://search.kmb.hk/KMBWebSite/Function/FunctionRequest.ashx?action=getAnnounce&route=${routeNumber}&bound=${if (bound[Operator.KMB] == "O") 1 else 2}")!!
        val notices = data.optJsonArray("data")!!
        for ((index, obj) in notices.withIndex()) {
            val notice = obj.jsonObject
            val url = "https://search.kmb.hk/KMBWebSite/AnnouncementPicture.ashx?url=${notice.optString("kpi_noticeimageurl")}"
            val titleZh = notice.optString("kpi_title_chi")
            val titleEn = notice.optString("kpi_title")
            list.add(RouteNoticeExternal(
                title = titleZh withEn titleEn,
                co = Operator.KMB,
                importance = RouteNoticeImportance.IMPORTANT,
                url = url.asBilingualText(),
                sort = index,
                possibleTwoWaySectionFareInfo = titleEn.lowercase().contains("section fare") || titleZh.contains("分段收費")
            ))
        }
    }
    this[Operator.CTB] = { list ->
        val dataZh = getTextResponse("https://mobile.citybus.com.hk/nwp3/getnotice.php?id=${routeNumber}&l=0")!!.string()
        val dataEn = getTextResponse("https://mobile.citybus.com.hk/nwp3/getnotice.php?id=${routeNumber}&l=1")!!.string()
        val matchesZh = ctbNoticePattern.findAll(dataZh.remove("\\n|\\t".toRegex())).toList()
        val matchesEn = ctbNoticePattern.findAll(dataEn.remove("\\n|\\t".toRegex())).toList()
        for ((index, matchZh) in matchesZh.withIndex()) {
            val matchEn = matchesEn.getOrNull(index)
            list.add(RouteNoticeExternal(
                title = matchZh.groupValues[2] withEn (matchEn?.groupValues[2]?: matchZh.groupValues[2]),
                co = Operator.CTB,
                importance = RouteNoticeImportance.IMPORTANT,
                url = matchZh.groupValues[1] withEn (matchEn?.groupValues[1]?: matchZh.groupValues[1]),
                sort = index
            ))
        }
    }
    this[Operator.LRT] = { list ->
        val data = getJSONResponse<JsonObject>("https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=001")!!
        val titleZh = data.optString("red_alert_message_ch").ifBlank { null }
        val titleEn = data.optString("red_alert_message_en").ifBlank { null }
        val urlZh = data.optString("red_alert_url_ch").ifBlank { null }
        val urlEn = data.optString("red_alert_url_en").ifBlank { null }
        if (titleZh != null && titleEn != null) {
            if (urlZh == null || urlEn == null) {
                list.add(RouteNoticeText(
                    title = titleZh withEn titleEn,
                    co = Operator.LRT,
                    importance = RouteNoticeImportance.IMPORTANT,
                    content = "".asBilingualText(),
                    isRealTitle = true,
                    sort = 0
                ))
            } else {
                list.add(RouteNoticeExternal(
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