package com.loohp.hkbuseta.notificationserver.notices

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.ANRouteId
import com.loohp.hkbuseta.common.objects.ANRouteIdUrl
import com.loohp.hkbuseta.common.objects.AlertNotification
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteNotice
import com.loohp.hkbuseta.common.objects.RouteNoticeExternal
import com.loohp.hkbuseta.common.objects.RouteNoticeImportance
import com.loohp.hkbuseta.common.objects.RouteNoticeText
import com.loohp.hkbuseta.common.objects.SpecialTrafficNews
import com.loohp.hkbuseta.common.objects.TrafficNews
import com.loohp.hkbuseta.common.objects.TrainServiceStatusType
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.defaultOperatorNotices
import com.loohp.hkbuseta.common.objects.getBody
import com.loohp.hkbuseta.common.objects.getDeepLink
import com.loohp.hkbuseta.common.objects.getOperators
import com.loohp.hkbuseta.common.objects.getTitle
import com.loohp.hkbuseta.common.objects.lrtLineStatus
import com.loohp.hkbuseta.common.objects.merge
import com.loohp.hkbuseta.common.objects.mtrLineStatus
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.BASE_URL
import com.loohp.hkbuseta.common.utils.JsonIgnoreUnknownKeys
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.decodeFromStringReadChannel
import com.loohp.hkbuseta.common.utils.getJSONResponse
import com.loohp.hkbuseta.common.utils.getXMLResponse
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.pad
import com.loohp.hkbuseta.notificationserver.ServerAppContext
import com.loohp.hkbuseta.notificationserver.registry
import com.loohp.hkbuseta.notificationserver.utils.retryUntil
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.random.Random


@Serializable
data class SeenNotices(
    val routeNotices: List<RouteNotices>,
    val operatorNotices: List<OperatorNotices>
) {
    val routeNoticesMapView by lazy { routeNotices.associate { it.id to it.notices } }
    val operatorNoticesMapView by lazy { operatorNotices.associate { it.co to it.notices } }
}

@Serializable
data class RouteNotices(
    val id: ANRouteId,
    val notices: Set<RouteNotice>
)

@Serializable
data class OperatorNotices(
    val co: Operator,
    val notices: Set<RouteNotice>
)

private val executor = Executors.newFixedThreadPool(8)

suspend fun checkNoticeUpdates(): List<AlertNotification> {
    val registry = registry()
    val idToRoute = ConcurrentHashMap<ANRouteId, Route>()
    val routeNotices = ConcurrentHashMap<ANRouteId, MutableSet<RouteNotice>>()
    registry.getRouteList().values.distinctBy { it.toANRouteId() }.map { route ->
        executor.submit {
            runBlocking {
                val notices = getRouteNotices(route, ServerAppContext)
                val id = route.toANRouteId()
                routeNotices.computeIfAbsent(id) { ConcurrentHashMap.newKeySet() }.addAll(notices)
                idToRoute.put(id, route)
            }
        }
    }.forEach { it.get() }
    val operatorNotices = ConcurrentHashMap<Operator, MutableSet<RouteNotice>>()
    Operator.values().asSequence().forEach { operator ->
        val notices = getOperatorNotices(setOf(operator), ServerAppContext)
        operatorNotices.computeIfAbsent(operator) { ConcurrentHashMap.newKeySet() }.addAll(notices)
        delay(1000)
    }
    val previousSeenNotices = previousSeenNotices()
    val notifications = mutableListOf<AlertNotification>()
    val now = currentLocalDateTime()
    val timeLabel = "(${now.hour.pad(2)}:${now.minute.pad(2)}) ".asBilingualText()
    if (previousSeenNotices != null) {
        for ((id, notices) in routeNotices) {
            val previousNotices = previousSeenNotices.routeNoticesMapView[id].orEmpty()
            for (notice in notices) {
                if (previousNotices.none { it.title == notice.title } && notice.importance == RouteNoticeImportance.IMPORTANT) {
                    val url = idToRoute[id]!!.getDeepLink(ServerAppContext, null, null).asBilingualText()
                    val notification = AlertNotification(
                        id = Random.nextInt(),
                        routes = listOf(ANRouteIdUrl(id, url)),
                        title = timeLabel + notice.title,
                        content = (notice as? RouteNoticeText)?.content?: "".asBilingualText(),
                        fallbackUrl = url
                    )
                    notifications.add(notification)
                }
            }
        }
        for ((co, notices) in operatorNotices) {
            val previousNotices = previousSeenNotices.operatorNoticesMapView[co].orEmpty()
            for (notice in notices) {
                if (previousNotices.none { it.title == notice.title } && notice.importance == RouteNoticeImportance.IMPORTANT) {
                    val notification = AlertNotification(
                        id = Random.nextInt(),
                        routes = emptyList(),
                        title = timeLabel + notice.title,
                        content = (notice as? RouteNoticeText)?.content?: "".asBilingualText(),
                        fallbackUrl = BASE_URL.asBilingualText()
                    )
                    notifications.add(notification)
                }
            }
        }
    }
    val seenNotices = SeenNotices(
        routeNotices = routeNotices.map { (k, v) -> RouteNotices(k, v) },
        operatorNotices = operatorNotices.map { (k, v) -> OperatorNotices(k, v) }
    )
    saveSeenNotices(seenNotices)
    return notifications.merge()
}

suspend fun previousSeenNotices(): SeenNotices? {
    return if (ServerAppContext.listFiles().contains("seen_notices.json")) {
        val channel = ServerAppContext.readTextFile("seen_notices.json")
        JsonIgnoreUnknownKeys.decodeFromStringReadChannel(channel)
    } else {
        null
    }
}

suspend fun saveSeenNotices(seenNotices: SeenNotices) {
    ServerAppContext.writeTextFile("seen_notices.json", JsonIgnoreUnknownKeys, SeenNotices.serializer()) { seenNotices }
}

fun Route.toANRouteId(): ANRouteId = ANRouteId(routeNumber, co)

suspend fun getOperatorNotices(co: Set<Operator>, context: AppContext): List<RouteNotice> {
    return buildList {
        when {
            co.contains(Operator.MTR) -> add(mtrLineStatus)
            co.contains(Operator.LRT) -> add(lrtLineStatus)
        }
        try {
            when {
                co.contains(Operator.MTR) -> {
                    val statuses = retryUntil(
                        block = { registry().getMtrLineServiceDisruption() },
                        predicate = { it != null },
                        fallbackValue = null
                    )!!
                    for (status in statuses.values) {
                        if (status.type == TrainServiceStatusType.DISRUPTION) {
                            val message = status.messages
                            if (message != null) {
                                add(RouteNoticeExternal(
                                    title = message.title,
                                    co = Operator.LRT,
                                    importance = RouteNoticeImportance.IMPORTANT,
                                    url = message.urlMobile,
                                    sort = 0
                                ))
                            }
                        }
                    }
                }
                co.contains(Operator.LRT) -> {
                    val data = retryUntil(
                        block = { getJSONResponse<JsonObject>("https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=001") },
                        predicate = { it != null },
                        fallbackValue = null
                    )!!
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
            val data = retryUntil(
                block = { getXMLResponse<TrafficNews>("https://td.gov.hk/tc/special_news/trafficnews.xml") },
                predicate = { it != null },
                fallbackValue = null
            )!!
            for (news in data.messages) {
                val operators = news.getOperators()
                val important = operators.any { co.contains(it) }
                if (operators.isEmpty() || important) {
                    add(RouteNoticeText(
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
            val data = retryUntil(
                block = { getXMLResponse<SpecialTrafficNews>("https://resource.data.one.gov.hk/td/en/specialtrafficnews.xml") },
                predicate = { it != null },
                fallbackValue = null
            )!!
            for (news in data.messages) {
                val operators = news.getOperators()
                val important = operators.any { co.contains(it) }
                if (operators.isEmpty() || important) {
                    add(RouteNoticeText(
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

suspend fun getRouteNotices(route: Route, context: AppContext): List<RouteNotice> {
    return buildList {
        route.defaultOperatorNotices(this)
        try {
            val data = getXMLResponse<TrafficNews>("https://td.gov.hk/tc/special_news/trafficnews.xml")!!
            for (news in data.messages) {
                val operators = news.getOperators()
                val important = operators.any { route.co.contains(it) }
                if (operators.isEmpty() || important) {
                    add(RouteNoticeText(
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
                val important = operators.any { route.co.contains(it) }
                if (operators.isEmpty() || important) {
                    add(RouteNoticeText(
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