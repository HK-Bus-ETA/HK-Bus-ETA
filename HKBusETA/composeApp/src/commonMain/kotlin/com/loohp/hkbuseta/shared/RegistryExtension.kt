package com.loohp.hkbuseta.shared

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import co.touchlab.stately.collections.ConcurrentMutableMap
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteNotice
import com.loohp.hkbuseta.common.objects.RouteNoticeExternal
import com.loohp.hkbuseta.common.objects.RouteNoticeImportance
import com.loohp.hkbuseta.common.objects.RouteNoticeText
import com.loohp.hkbuseta.common.objects.SpecialTrafficNews
import com.loohp.hkbuseta.common.objects.TrafficNews
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.defaultOperatorNotices
import com.loohp.hkbuseta.common.objects.fetchOperatorNotices
import com.loohp.hkbuseta.common.objects.getBody
import com.loohp.hkbuseta.common.objects.getOperators
import com.loohp.hkbuseta.common.objects.getTitle
import com.loohp.hkbuseta.common.objects.lrtLineStatus
import com.loohp.hkbuseta.common.objects.mtrLineStatus
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.AutoSortedList
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.asAutoSortedList
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.getJSONResponse
import com.loohp.hkbuseta.common.utils.getXMLResponse
import com.loohp.hkbuseta.common.utils.optString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject


private val routeNoticeCache: MutableMap<Any, Triple<AutoSortedList<RouteNotice, SnapshotStateList<RouteNotice>>, Long, String>> = ConcurrentMutableMap()

fun Registry.getOperatorNotices(co: Set<Operator>, context: AppContext): SnapshotStateList<RouteNotice> {
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

fun Registry.getRouteNotices(route: Route, context: AppContext): SnapshotStateList<RouteNotice> {
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