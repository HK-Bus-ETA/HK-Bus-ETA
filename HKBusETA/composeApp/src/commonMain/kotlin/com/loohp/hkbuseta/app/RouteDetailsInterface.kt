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

package com.loohp.hkbuseta.app

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.loohp.hkbuseta.appcontext.AppScreenGroup
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.ScreenState
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.appcontext.newScreenGroup
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ReduceDataOmitted
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.OriginData
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteNotice
import com.loohp.hkbuseta.common.objects.RouteNoticeImportance
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.RouteWaypoints
import com.loohp.hkbuseta.common.objects.SpecialRouteAlerts
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.TrafficSnapshotPoint
import com.loohp.hkbuseta.common.objects.asOriginData
import com.loohp.hkbuseta.common.objects.bilingualToPrefix
import com.loohp.hkbuseta.common.objects.calculateServiceTimeCategory
import com.loohp.hkbuseta.common.objects.defaultWaypoints
import com.loohp.hkbuseta.common.objects.findReverse
import com.loohp.hkbuseta.common.objects.findSimilarRoutes
import com.loohp.hkbuseta.common.objects.getDeepLink
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getListDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getSpecialRouteAlerts
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.isBus
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.objects.resolvedDestWithBranchFormatted
import com.loohp.hkbuseta.common.objects.shouldPrependTo
import com.loohp.hkbuseta.common.objects.toRouteSearchResult
import com.loohp.hkbuseta.common.objects.toStopIndexed
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.services.AlightReminderService
import com.loohp.hkbuseta.common.shared.BASE_URL
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.ServiceTimeCategory
import com.loohp.hkbuseta.common.utils.any
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.awaitWithTimeout
import com.loohp.hkbuseta.common.utils.createTimetable
import com.loohp.hkbuseta.common.utils.currentBranchStatus
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.getCircledNumber
import com.loohp.hkbuseta.common.utils.getServiceTimeCategory
import com.loohp.hkbuseta.common.utils.indexesOf
import com.loohp.hkbuseta.compose.AdaptiveTopBottomLayout
import com.loohp.hkbuseta.compose.AltRoute
import com.loohp.hkbuseta.compose.ArrowBack
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.CalendarClock
import com.loohp.hkbuseta.compose.ChangedEffect
import com.loohp.hkbuseta.compose.ConditionalComposable
import com.loohp.hkbuseta.compose.DepartureBoard
import com.loohp.hkbuseta.compose.DirectionsBoat
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.Fullscreen
import com.loohp.hkbuseta.compose.FullscreenExit
import com.loohp.hkbuseta.compose.NotificationImportant
import com.loohp.hkbuseta.compose.PhotoCamera
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformCircularProgressIndicator
import com.loohp.hkbuseta.compose.PlatformFilledTonalIconToggleButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformModalBottomSheet
import com.loohp.hkbuseta.compose.PlatformTab
import com.loohp.hkbuseta.compose.PlatformTabRow
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.PlatformTopAppBar
import com.loohp.hkbuseta.compose.RightToLeftRow
import com.loohp.hkbuseta.compose.Timer
import com.loohp.hkbuseta.compose.UTurnRight
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.currentLocalWindowSize
import com.loohp.hkbuseta.compose.isNarrow
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.compose.platformHorizontalDividerShadow
import com.loohp.hkbuseta.compose.platformLocalContentColor
import com.loohp.hkbuseta.compose.platformPrimaryContainerColor
import com.loohp.hkbuseta.compose.platformSurfaceContainerColor
import com.loohp.hkbuseta.compose.rememberAutoResizeFontState
import com.loohp.hkbuseta.compose.rememberIsInPipMode
import com.loohp.hkbuseta.compose.rememberPlatformModalBottomSheetState
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clearColors
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.pixelsToDp
import com.loohp.hkbuseta.utils.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue
import kotlin.math.min


enum class StopsTabItemType {
    STOPS, TIMES, TRAFFIC_SNAPSHOTS, NOTICES, TIMETABLE
}

@Immutable
data class ListStopsTabItem(
    val title: BilingualText,
    val icon: @Composable () -> Painter,
    val color: Color? = null,
    val type: StopsTabItemType
)

val busListStopsTabItem = listOf(
    ListStopsTabItem(
        title = "巴士站" withEn "Stops",
        icon = { PlatformIcons.Outlined.DepartureBoard },
        type = StopsTabItemType.STOPS
    ),
    ListStopsTabItem(
        title = "車程" withEn "Times",
        icon = { PlatformIcons.Outlined.Timer },
        type = StopsTabItemType.TIMES
    ),
    ListStopsTabItem(
        title = "快拍" withEn "Traffic",
        icon = { PlatformIcons.Filled.PhotoCamera },
        type = StopsTabItemType.TRAFFIC_SNAPSHOTS
    ),
    ListStopsTabItem(
        title = "公告" withEn "Notices",
        icon = { PlatformIcons.Outlined.NotificationImportant },
        type = StopsTabItemType.NOTICES
    ),
    ListStopsTabItem(
        title = "時間表" withEn "Timetable",
        icon = { PlatformIcons.Outlined.CalendarClock },
        type = StopsTabItemType.TIMETABLE
    )
)

val trainListStopsTabItem = listOf(
    ListStopsTabItem(
        title = "車站" withEn "Stations",
        icon = { painterResource(DrawableResource("mtr_vector.xml")) },
        color = Color(0xFFAC2E44),
        type = StopsTabItemType.STOPS
    ),
    ListStopsTabItem(
        title = "車程" withEn "Times",
        icon = { PlatformIcons.Outlined.Timer },
        type = StopsTabItemType.TIMES
    ),
    ListStopsTabItem(
        title = "公告" withEn "Notices",
        icon = { PlatformIcons.Outlined.NotificationImportant },
        type = StopsTabItemType.NOTICES
    ),
)

val lrtListStopsTabItem = listOf(
    ListStopsTabItem(
        title = "車站" withEn "Stops",
        icon = { painterResource(DrawableResource("lrt_vector.xml")) },
        color = Color(0xFFCDA410),
        type = StopsTabItemType.STOPS
    ),
    ListStopsTabItem(
        title = "車程" withEn "Times",
        icon = { PlatformIcons.Outlined.Timer },
        type = StopsTabItemType.TIMES
    ),
    ListStopsTabItem(
        title = "公告" withEn "Notices",
        icon = { PlatformIcons.Outlined.NotificationImportant },
        type = StopsTabItemType.NOTICES
    ),
)

val ferryListStopsTabItem = listOf(
    ListStopsTabItem(
        title = "航班" withEn "Route",
        icon = { PlatformIcons.Outlined.DirectionsBoat },
        type = StopsTabItemType.STOPS
    ),
    ListStopsTabItem(
        title = "公告" withEn "Notices",
        icon = { PlatformIcons.Outlined.NotificationImportant },
        type = StopsTabItemType.NOTICES
    ),
    ListStopsTabItem(
        title = "時間表" withEn "Timetable",
        icon = { PlatformIcons.Outlined.CalendarClock },
        type = StopsTabItemType.TIMETABLE
    )
)

fun listStopsTabItem(co: Operator): List<ListStopsTabItem> {
    return when {
        co === Operator.LRT -> lrtListStopsTabItem
        co.isTrain -> trainListStopsTabItem
        co.isFerry -> ferryListStopsTabItem
        else -> busListStopsTabItem
    }
}

expect fun updateBrowserState(title: String, url: String)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ReduceDataOmitted::class)
@Composable
fun RouteDetailsInterface(instance: AppActiveContext) {
    val route by remember(instance) { derivedStateOf { when (val r = instance.compose.data["route"]) {
        is StopIndexedRouteSearchResultEntry -> r
        is RouteSearchResultEntry -> r.toStopIndexed(instance)
        is Route -> r.toRouteSearchResult(instance).toStopIndexed(instance)
        else -> throw RuntimeException("Missing Route")
    } } }

    var reverseRoute: RouteSearchResultEntry? by remember { mutableStateOf(null) }
    val similarRoutes: MutableMap<Route, ImmutableList<StopIndexedRouteSearchResultEntry>> = remember { mutableStateMapOf() }

    val routeNumber by remember { derivedStateOf { route.route!!.routeNumber } }
    val co by remember { derivedStateOf { route.co } }
    val bound by remember { derivedStateOf { route.route!!.idBound(co) } }
    val gmbRegion by remember { derivedStateOf { route.route!!.gmbRegion } }
    val isNightRoute by remember { derivedStateOf { co.isBus && calculateServiceTimeCategory(routeNumber, co) {
        Registry.getInstance(instance).getAllBranchRoutes(routeNumber, route.route!!.idBound(co), co, gmbRegion).createTimetable(instance).getServiceTimeCategory()
    } == ServiceTimeCategory.NIGHT } }

    var notices: List<RouteNotice>? by remember { mutableStateOf(null) }
    var ctbHasTwoWaySectionFare by remember { mutableStateOf(false) }
    val importantNoticeCount by remember { derivedStateOf { notices?.count { it.importance == RouteNoticeImportance.IMPORTANT }?: 0 } }
    val possibleBidirectionalSectionFare by remember { derivedStateOf { co == Operator.NLB || ctbHasTwoWaySectionFare || notices?.any { it.title.lowercase().contains("section fare") || it.title.contains("分段收費") } == true } }

    val alertCheckRoute by remember { derivedStateOf { route.route!!.getSpecialRouteAlerts(instance).contains(SpecialRouteAlerts.CheckRoute) } }

    val routeBranches by remember { derivedStateOf { Registry.getInstance(instance).getAllBranchRoutes(routeNumber, bound, co, gmbRegion) } }
    val selectedBranchState = remember { mutableStateOf(routeBranches.currentBranchStatus(currentLocalDateTime(), instance).asSequence().sortedByDescending { it.value.activeness }.first().key) }
    var selectedBranch by selectedBranchState
    val selectedStop = remember { mutableIntStateOf(
        ((instance.compose.data["scrollToStop"] as? String)?: (instance.compose.data["stopId"] as? String))
            ?.let { id ->
                val stopIndex = instance.compose.data["stopIndex"] as? Int?: 0
                val allStops = Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gmbRegion)
                val index = allStops.indexesOf { it.stopId == id }.minBy { (it - stopIndex).absoluteValue }
                if (index >= 0) {
                    index + 1
                } else if (route.route!!.isKmbCtbJoint) {
                    val altStops = Registry.getInstance(instance).findEquivalentStops(allStops.map { it.stopId }, Operator.CTB)
                    val ctbIndex = altStops.indexOfFirst { it.stopId == id }
                    ctbIndex.takeIf { it >= 0 }?.let { it + 1 }
                } else {
                    null
                }
            }
            ?: route.stopInfoIndex.takeIf { it >= 0 }?.let { it + 1 }?: 1
    ) }
    val allStops by remember { derivedStateOf { Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gmbRegion) } }

    val launchedWithStop = remember { route.stopInfo != null || instance.compose.data.containsKey("scrollToStop") || instance.compose.data.containsKey("stopId") }

    val listStopsTabItem by remember { derivedStateOf { listStopsTabItem(co) } }

    val pagerState = rememberPagerState(
        initialPage = (instance.compose.data["page"] as? Int?: 0).coerceAtMost(listStopsTabItem.size - 1)
    ) { listStopsTabItem.size }
    val scope = rememberCoroutineScope()
    val pagerScrollEnabled = remember { mutableStateOf(true) }

    val sheetState = rememberPlatformModalBottomSheetState()
    var showingSimilarRoutes by rememberSaveable { mutableStateOf(false) }

    var location: OriginData? by remember { mutableStateOf(null) }

    val timesStartIndexState = remember { mutableIntStateOf(1) }
    val timesInitState = remember { mutableStateOf(false) }

    val optAlightReminderService by AlightReminderService.currentInstance.collectAsStateMultiplatform()
    val alightReminderService by remember { derivedStateOf { optAlightReminderService.value } }
    val isActiveReminderService by remember { derivedStateOf { alightReminderService?.selectedRoute?.let { routeBranches.contains(it) } == true } }

    val alternateStopNamesShowing = Shared.alternateStopNamesShowingState.collectAsStateMultiplatform { Registry.getInstance(instance).setAlternateStopNames(it, instance) }

    val waypointStops by remember { derivedStateOf { (if (co.isTrain) allStops else allStops.filter { it.branchIds.contains(selectedBranch) }).map { it.stop } } }
    var waypoints by remember { mutableStateOf(if (co.isTrain) route.defaultWaypoints(instance) else selectedBranch.defaultWaypoints(instance)) }
    var trafficSnapshots: Array<out List<TrafficSnapshotPoint>>? by remember { mutableStateOf(null) }
    var trafficSnapshotsLoading by remember { mutableStateOf(true) }

    LaunchedEffect (selectedBranch, waypointStops) {
        CoroutineScope(Dispatchers.IO).launch {
            val waypointsAsync = Registry.getInstance(instance).getRouteWaypoints(selectedBranch, instance, waypointStops).await()
            withContext(Dispatchers.Main) {
                waypoints = waypointsAsync
            }
            val asyncTrafficSnapshots = Registry.getInstance(instance).getRouteTrafficSnapshots(waypointsAsync)
            withContext(Dispatchers.Main) {
                trafficSnapshots = asyncTrafficSnapshots
                trafficSnapshotsLoading = false
            }
        }
    }

    instance.compose.setStatusNavBarColor(
        status = platformPrimaryContainerColor,
        nav = platformSurfaceContainerColor
    )

    ChangedEffect (pagerState.currentPage) {
        instance.compose.data["page"] = pagerState.currentPage
    }
    LaunchedEffect (showingSimilarRoutes) {
        ScreenState.hasInterruptElement.value = showingSimilarRoutes
    }
    LaunchedEffect (isActiveReminderService) {
        if (isActiveReminderService) {
            while (true) {
                alightReminderService?.let {
                    selectedBranch = it.selectedRoute
                    selectedStop.value = it.currentStop.index
                }
                delay(1000)
            }
        }
    }
    LaunchedEffect (Unit) {
        if (!launchedWithStop) {
            val fastResult = getGPSLocation(instance, LocationPriority.FASTER).awaitWithTimeout(3000)
            if (fastResult?.isSuccess == true) {
                val pos = fastResult.location!!
                if (allStops.any(2) { it.stop.location.distance(pos) < 0.1 }) {
                    val result = getGPSLocation(instance).await()
                    if (result?.isSuccess == true) {
                        location = result.location!!.asOriginData(true)
                    }
                } else {
                    location = fastResult.location!!.asOriginData(true)
                }
            }
        }
    }
    LaunchedEffect (Unit) {
        notices = Registry.getInstance(instance).getRouteNotices(route.route!!, instance)
    }
    LaunchedEffect (Unit) {
        if (co == Operator.CTB) {
            Registry.getInstance(instance).getCtbHasTwoWaySectionFare(routeNumber) { ctbHasTwoWaySectionFare = it }
        }
    }
    LaunchedEffect (route) {
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                val reverse = route.findReverse(instance)
                withContext(Dispatchers.Main) { reverseRoute = reverse }
            }
            for (branch in routeBranches) {
                launch {
                    val result = branch.findSimilarRoutes(co, instance).takeIf { it.isNotEmpty() }?.toStopIndexed(instance)?.asImmutableList()
                    withContext(Dispatchers.Main) {
                        if (result == null) {
                            similarRoutes.remove(branch)
                        } else {
                            similarRoutes[branch] = result
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect (route, selectedBranch, selectedStop.intValue) {
        val stopIndex = selectedStop.intValue
        val stop = allStops[stopIndex - 1]
        updateBrowserState("${co.getDisplayName(routeNumber, selectedBranch.isKmbCtbJoint, gmbRegion, Shared.language)} ${co.getDisplayRouteNumber(routeNumber)} ${selectedBranch.resolvedDest(true)[Shared.language]} ${if (!co.isTrain) "${stopIndex}." else ""} ${stop.stop.name[Shared.language]}", selectedBranch.getDeepLink(instance, stop.stopId, stopIndex))
    }
    DisposableEffect (Unit) {
        onDispose { updateBrowserState("", BASE_URL) }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val darkMode = Shared.theme.isDarkMode
        PlatformTopAppBar(
            title = {
                @Suppress("RemoveExplicitTypeArguments")
                ConditionalComposable<RowScope>(
                    condition = composePlatform.applePlatform && similarRoutes[selectedBranch] != null,
                    ifTrue = { content ->
                        PlatformButton(
                            modifier = Modifier
                                .applyIf(!composePlatform.applePlatform) { weight(1F) }
                                .plainTooltip(if (Shared.language == "en") "Similar Routes" else "相近走線路線"),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.clearColors(),
                            contentPadding = PaddingValues(0.dp),
                            onClick = { showingSimilarRoutes = true },
                            content = content
                        )
                    },
                    ifFalse = { content ->
                        Row(
                            modifier = Modifier.applyIf(!composePlatform.applePlatform) { weight(1F) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            content = content
                        )
                    },
                    content = {
                        val autoResizeFontState = rememberAutoResizeFontState(FontSizeRange(max = 22.dp.sp, step = 0.5F.sp))
                        var lineCount by remember { mutableIntStateOf(1) }
                        PlatformText(
                            modifier = Modifier
                                .applyIf(lineCount == 1) { alignByBaseline() }
                                .applyIf(isNightRoute && !darkMode) {
                                    padding(horizontal = if (isNightRoute && !darkMode) 3.sp.dp else 0.dp).drawBehind {
                                        drawRoundRect(
                                            topLeft = Offset(-3.sp.toPx(), 1.sp.toPx()),
                                            size = Size(size.width + 6.sp.toPx(), size.height - (if (composePlatform.applePlatform) 2 else 3).sp.toPx()),
                                            cornerRadius = CornerRadius(4.sp.toPx()),
                                            color = Color.Black
                                        )
                                    }
                                },
                            text = co.getListDisplayRouteNumber(routeNumber, true),
                            fontSize = 30.dp.sp.clamp(max = 33.dp),
                            lineHeight = 1.1F.em,
                            color = if (isNightRoute) (if (darkMode) Color.Yellow else Color.White) else Color.Unspecified
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        if (route.route!!.shouldPrependTo()) {
                            AutoResizeText(
                                modifier = Modifier.alignByBaseline(),
                                textAlign = TextAlign.Start,
                                lineHeight = 1.1F.em,
                                autoResizeTextState = autoResizeFontState,
                                maxLines = 1,
                                text = bilingualToPrefix[Shared.language].asAnnotatedString(SpanStyle(fontSize = TextUnit.Small))
                            )
                        }
                        AutoResizeText(
                            modifier = Modifier
                                .alignByBaseline()
                                .userMarquee(),
                            overflow = TextOverflow.Ellipsis,
                            text = route.route!!.resolvedDestWithBranchFormatted(false, selectedBranch, selectedStop.intValue, allStops[selectedStop.intValue - 1].stopId, instance)[Shared.language].asContentAnnotatedString().annotatedString,
                            autoResizeTextState = autoResizeFontState,
                            lineHeight = 1.1F.em,
                            maxLines = 2,
                            onTextLayout = { lineCount = it.lineCount }
                        )
                    }
                )
            },
            navigationIcon = {
                PlatformButton(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp),
                    onClick = { instance.finish() }
                ) {
                    PlatformIcon(
                        modifier = Modifier.size(30.dp),
                        painter = PlatformIcons.AutoMirrored.Filled.ArrowBack,
                        tint = platformLocalContentColor,
                        contentDescription = if (Shared.language == "en") "Back" else "返回"
                    )
                }
            },
            actions = {
                RightToLeftRow(
                    modifier = Modifier
                        .height(45.dp)
                        .animateContentSize()
                ) {
                    if (reverseRoute != null) {
                        PlatformButton(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape)
                                .plainTooltip(if (Shared.language == "en") "Reverse Direction" else "反向"),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    reverseRoute?.let {
                                        Registry.getInstance(instance).addLastLookupRoute(it.routeKey, instance)
                                        val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                                        intent.putExtra("route", it)
                                        if (HistoryStack.historyStack.value.last().newScreenGroup() == AppScreenGroup.ROUTE_STOPS) {
                                            instance.startActivity(AppIntent(instance, AppScreen.DUMMY))
                                            delay(300)
                                        }
                                        instance.startActivity(intent)
                                        instance.compose.finishSelfOnly()
                                    }
                                }
                            }
                        ) {
                            PlatformIcon(
                                modifier = Modifier.size(30.dp),
                                painter = PlatformIcons.Outlined.UTurnRight,
                                tint = platformLocalContentColor,
                                contentDescription = if (Shared.language == "en") "Reverse Direction" else "反向"
                            )
                        }
                    }
                    if (!composePlatform.applePlatform && similarRoutes[selectedBranch] != null) {
                        PlatformButton(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape)
                                .plainTooltip(if (Shared.language == "en") "Similar Routes" else "相近走線路線"),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            onClick = { showingSimilarRoutes = true }
                        ) {
                            PlatformIcon(
                                modifier = Modifier.size(30.dp),
                                painter = PlatformIcons.AutoMirrored.Outlined.AltRoute,
                                tint = platformLocalContentColor,
                                contentDescription = if (Shared.language == "en") "Similar Routes" else "相近走線路線"
                            )
                        }
                    }
                }
            },
            iosDivider = { /* do nothing */ }
        )
        val autoFontSizeState = rememberAutoResizeFontState(
            fontSizeRange = FontSizeRange(min = 11F.dp.sp, max = 16F.sp),
            preferSingleLine = true
        )
        PlatformTabRow(
            modifier = Modifier
                .applyIf(composePlatform.applePlatform) { platformHorizontalDividerShadow() },
            selectedTabIndex = pagerState.currentPage
        ) {
            listStopsTabItem.forEachIndexed { index, (title, icon, color, type) ->
                PlatformTab(
                    selected = index == pagerState.currentPage,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    icon = {
                        PlatformIcon(
                            modifier = Modifier.size(18F.sp.dp),
                            painter = icon.invoke(),
                            tint = color,
                            contentDescription = title[Shared.language]
                        )
                    },
                    text = {
                        var height by remember { mutableIntStateOf(0) }
                        Row(
                            modifier = Modifier
                                .applyIf(composePlatform.applePlatform) { padding(horizontal = 5.dp) }
                                .onSizeChanged { height = it.height },
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AutoResizeText(
                                autoResizeTextState = autoFontSizeState,
                                lineHeight = 1.1F.em,
                                text = title[Shared.language]
                            )
                            when (type) {
                                StopsTabItemType.TRAFFIC_SNAPSHOTS -> {
                                    androidx.compose.animation.AnimatedVisibility(
                                        modifier = Modifier.requiredSizeIn(
                                            maxWidth = height.equivalentDp,
                                            maxHeight = height.equivalentDp
                                        ),
                                        visible = trafficSnapshotsLoading,
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = shrinkHorizontally() + fadeOut(),
                                    ) {
                                        PlatformCircularProgressIndicator(
                                            modifier = Modifier
                                                .size(autoFontSizeState.value.fontSizeValue.sp.dp - 1.dp)
                                                .zIndex(1F),
                                            color = Color(0xFFF9DE09),
                                            strokeWidth = autoFontSizeState.value.fontSizeValue.sp.dp / 8,
                                            trackColor = Color(0xFF797979),
                                            strokeCap = StrokeCap.Round,
                                        )
                                    }
                                }
                                StopsTabItemType.NOTICES -> {
                                    androidx.compose.animation.AnimatedVisibility(
                                        modifier = Modifier.requiredHeight(height.equivalentDp),
                                        visible = importantNoticeCount > 0,
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = shrinkHorizontally() + fadeOut(),
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AutoResizeText(
                                                modifier = Modifier.zIndex(1F),
                                                autoResizeTextState = autoFontSizeState,
                                                lineHeight = 1.1F.em,
                                                text = importantNoticeCount.getCircledNumber()
                                            )
                                        }
                                    }
                                }
                                else -> { /* do nothing */ }
                            }
                        }
                    }
                )
            }
        }
        HorizontalPager(
            modifier = Modifier.weight(1F),
            state = pagerState,
            userScrollEnabled = pagerScrollEnabled.value
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val item = listStopsTabItem[it]
                when (item.type) {
                    StopsTabItemType.STOPS -> MapStopsInterface(instance, location, route, selectedStop, selectedBranchState, waypoints, possibleBidirectionalSectionFare, alertCheckRoute, isActiveReminderService, pagerScrollEnabled, alternateStopNamesShowing)
                    StopsTabItemType.TIMES -> ListStopsEtaInterface(instance, ListStopsInterfaceType.TIMES, location, route, selectedStop, selectedBranchState, alternateStopNamesShowing, timesStartIndexState = timesStartIndexState, timesInitState = timesInitState)
                    StopsTabItemType.TRAFFIC_SNAPSHOTS -> ListStopsEtaInterface(instance, ListStopsInterfaceType.TRAFFIC_SNAPSHOTS, location, route, selectedStop, selectedBranchState, alternateStopNamesShowing, waypoints = waypoints, trafficSnapshots = trafficSnapshots)
                    StopsTabItemType.NOTICES -> NoticeInterface(instance, notices?.asImmutableList(), possibleBidirectionalSectionFare)
                    StopsTabItemType.TIMETABLE -> TimetableInterface(instance, routeBranches.asImmutableList())
                }
            }
        }
    }

    if (showingSimilarRoutes) {
        PlatformModalBottomSheet(
            onDismissRequest = { showingSimilarRoutes = false },
            sheetState = sheetState
        ) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(platformPrimaryContainerColor)
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlatformText(
                            modifier = Modifier.fillMaxWidth(),
                            text = if (Shared.language == "en") "Similar Routes" else "相近走線路線",
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                content = { padding ->
                    Box(
                        modifier = Modifier.padding(padding)
                    ) {
                        ListRoutesInterface(instance, similarRoutes[selectedBranch]?: persistentListOf(), false, RouteListType.NORMAL, false, RecentSortMode.DISABLED, null, true)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapStopsInterface(
    instance: AppActiveContext,
    location: OriginData?,
    listRoute: RouteSearchResultEntry,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    waypoints: RouteWaypoints,
    possibleBidirectionalSectionFare: Boolean,
    alertCheckRoute: Boolean,
    isActiveReminderService: Boolean,
    pagerScrollEnabled: MutableState<Boolean>,
    alternateStopNamesShowingState: MutableState<Boolean>
) {
    var mapExpanded by remember { mutableStateOf(false) }

    val window = currentLocalWindowSize

    val routeNumber by remember(listRoute) { derivedStateOf { listRoute.route!!.routeNumber } }
    val co by remember(listRoute) { derivedStateOf { listRoute.co } }
    val bound by remember(listRoute) { derivedStateOf { listRoute.route!!.idBound(co) } }
    val gmbRegion by remember(listRoute) { derivedStateOf { listRoute.route!!.gmbRegion } }
    val isKmbCtbJoint by remember(listRoute) { derivedStateOf { listRoute.route!!.isKmbCtbJoint } }
    val allStops by remember(listRoute) { derivedStateOf { Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gmbRegion) } }

    val alternateStopNamesShowing by alternateStopNamesShowingState

    val pipMode = rememberIsInPipMode(instance)

    if (Shared.showRouteMap && !pipMode) {
        val alternateStopNames by remember(listRoute, isKmbCtbJoint) { derivedStateOf { if (isKmbCtbJoint) {
            Registry.getInstance(instance).findEquivalentStops(allStops.map { it.stopId }, Operator.CTB).asImmutableList()
        } else {
            null
        }.asImmutableState() } }

        AdaptiveTopBottomLayout(
            modifier = Modifier.fillMaxWidth(),
            context = instance,
            animateSize = true,
            bottomSize = { when {
                !it.isNarrow -> min(412F, (window.width / 2F).pixelsToDp(instance)).dp
                mapExpanded -> 200.dp
                else -> (window.height / 11F * 6F).pixelsToDp(instance).dp
            } },
            top = { screenSize ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1F)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(pass = PointerEventPass.Initial)
                                    pagerScrollEnabled.value = false
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        if (event.type == PointerEventType.Release) {
                                            pagerScrollEnabled.value = true
                                            break
                                        }
                                    }
                                }
                            }
                    ) {
                        MapRouteInterface(instance, waypoints, allStops.asImmutableList(), selectedStopState, selectedBranchState, alternateStopNamesShowing, alternateStopNames)
                        if (screenSize.isNarrow) {
                            PlatformFilledTonalIconToggleButton(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .plainTooltip(if (Shared.language == "en") "Toggle Map Size" else "切換地圖大小"),
                                checked = mapExpanded,
                                onCheckedChange = { mapExpanded = !mapExpanded },
                                shape = CircleShape
                            ) {
                                PlatformIcon(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .align(Alignment.Center),
                                    painter = if (mapExpanded) PlatformIcons.Outlined.FullscreenExit else PlatformIcons.Outlined.Fullscreen,
                                    contentDescription = if (Shared.language == "en") "Toggle Map Size" else "切換地圖大小"
                                )
                            }
                        }
                    }
                    if (screenSize.isNarrow) {
                        HorizontalDivider(thickness = 10.dp)
                    }
                }
            },
            bottom = { _ ->
                ListStopsEtaInterface(instance, if (isActiveReminderService) ListStopsInterfaceType.ALIGHT_REMINDER else ListStopsInterfaceType.ETA, location, listRoute, selectedStopState, selectedBranchState, alternateStopNamesShowingState, possibleBidirectionalSectionFare, alertCheckRoute)
            }
        )
    } else {
        Box (
            modifier = Modifier.fillMaxSize()
        ) {
            ListStopsEtaInterface(instance, if (isActiveReminderService) ListStopsInterfaceType.ALIGHT_REMINDER else ListStopsInterfaceType.ETA, location, listRoute, selectedStopState, selectedBranchState, alternateStopNamesShowingState, possibleBidirectionalSectionFare, alertCheckRoute)
        }
    }
}