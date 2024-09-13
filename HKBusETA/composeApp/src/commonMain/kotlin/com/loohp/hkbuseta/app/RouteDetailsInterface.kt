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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.AppScreenGroup
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.ScreenState
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.appcontext.newScreenGroup
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ReduceDataOmitted
import com.loohp.hkbuseta.common.appcontext.ReduceDataPossiblyOmitted
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.OriginData
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RemarkType
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteNotice
import com.loohp.hkbuseta.common.objects.RouteNoticeImportance
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.SpecialRouteAlerts
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.asOriginData
import com.loohp.hkbuseta.common.objects.bilingualToPrefix
import com.loohp.hkbuseta.common.objects.defaultWaypoints
import com.loohp.hkbuseta.common.objects.findReverse
import com.loohp.hkbuseta.common.objects.findSimilarRoutes
import com.loohp.hkbuseta.common.objects.getDeepLink
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getListDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getSpecialRouteAlerts
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.isCircular
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.resolveSpecialRemark
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
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.TimetableIntervalEntry
import com.loohp.hkbuseta.common.utils.TimetableSingleEntry
import com.loohp.hkbuseta.common.utils.TimetableSpecialEntry
import com.loohp.hkbuseta.common.utils.any
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.createTimetable
import com.loohp.hkbuseta.common.utils.currentBranchStatus
import com.loohp.hkbuseta.common.utils.currentEntry
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.currentMinuteState
import com.loohp.hkbuseta.common.utils.dayOfWeek
import com.loohp.hkbuseta.common.utils.dayServiceMidnight
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.common.utils.getCircledNumber
import com.loohp.hkbuseta.common.utils.isNightRoute
import com.loohp.hkbuseta.common.utils.nightServiceMidnight
import com.loohp.hkbuseta.common.utils.toDisplayText
import com.loohp.hkbuseta.compose.AdaptiveTopBottomLayout
import com.loohp.hkbuseta.compose.AltRoute
import com.loohp.hkbuseta.compose.ArrowBack
import com.loohp.hkbuseta.compose.ArrowDropDown
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.ConditionalComposable
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.Fullscreen
import com.loohp.hkbuseta.compose.FullscreenExit
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformFilledTonalIconToggleButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformModalBottomSheet
import com.loohp.hkbuseta.compose.PlatformTab
import com.loohp.hkbuseta.compose.PlatformTabRow
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.PlatformTopAppBar
import com.loohp.hkbuseta.compose.RightToLeftRow
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.UTurnRight
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.clickable
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
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
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.clearColors
import com.loohp.hkbuseta.utils.fontScaledDp
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
import kotlin.math.min


private val specialDenoteChars = listOf("*", "^", "#")

fun specialDenoteChar(index: Int): String {
    return specialDenoteChars[index % specialDenoteChars.size].repeat((index / specialDenoteChars.size) + 1)
}

enum class StopsTabItemType {
    STOPS, TIMES, NOTICES, TIMETABLE
}

@Immutable
data class ListStopsTabItem(
    val title: BilingualText,
    val type: StopsTabItemType
)

val busListStopsTabItem = listOf(
    ListStopsTabItem(
        title = "巴士站" withEn "Stops",
        type = StopsTabItemType.STOPS
    ),
    ListStopsTabItem(
        title = "車程" withEn "Times",
        type = StopsTabItemType.TIMES
    ),
    ListStopsTabItem(
        title = "公告" withEn "Notices",
        type = StopsTabItemType.NOTICES
    ),
    ListStopsTabItem(
        title = "時間表" withEn "Timetable",
        type = StopsTabItemType.TIMETABLE
    )
)

val trainListStopsTabItem = listOf(
    ListStopsTabItem(
        title = "車站" withEn "Stations",
        type = StopsTabItemType.STOPS
    ),
    ListStopsTabItem(
        title = "車程" withEn "Times",
        type = StopsTabItemType.TIMES
    ),
    ListStopsTabItem(
        title = "公告" withEn "Notices",
        type = StopsTabItemType.NOTICES
    ),
)

val ferryListStopsTabItem = listOf(
    ListStopsTabItem(
        title = "航班" withEn "Route",
        type = StopsTabItemType.STOPS
    ),
    ListStopsTabItem(
        title = "公告" withEn "Notices",
        type = StopsTabItemType.NOTICES
    ),
    ListStopsTabItem(
        title = "時間表" withEn "Timetable",
        type = StopsTabItemType.TIMETABLE
    )
)

fun listStopsTabItem(co: Operator): List<ListStopsTabItem> {
    return when {
        co.isTrain -> trainListStopsTabItem
        co.isFerry -> ferryListStopsTabItem
        else -> busListStopsTabItem
    }
}

expect fun updateBrowserState(title: String, url: String)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ReduceDataOmitted::class, ReduceDataPossiblyOmitted::class)
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

    val routeNumber by remember(route) { derivedStateOf { route.route!!.routeNumber } }
    val co by remember(route) { derivedStateOf { route.co } }
    val bound by remember(route) { derivedStateOf { route.route!!.idBound(co) } }
    val gmbRegion by remember(route) { derivedStateOf { route.route!!.gmbRegion } }

    var notices: List<RouteNotice>? by remember { mutableStateOf(null) }
    var ctbHasTwoWaySectionFare by remember { mutableStateOf(false) }
    val importantNoticeCount by remember(notices) { derivedStateOf { notices?.count { it.importance == RouteNoticeImportance.IMPORTANT }?: 0 } }
    val possibleBidirectionalSectionFare by remember(notices, ctbHasTwoWaySectionFare) { derivedStateOf { co == Operator.NLB || ctbHasTwoWaySectionFare || notices?.any { it.title.lowercase().contains("section fare") || it.title.contains("分段收費") } == true } }

    val alertCheckRoute by remember(route) { derivedStateOf { route.route!!.getSpecialRouteAlerts(instance).contains(SpecialRouteAlerts.CheckRoute) } }

    val routeBranches by remember(route) { derivedStateOf { Registry.getInstance(instance).getAllBranchRoutes(routeNumber, bound, co, gmbRegion) } }
    val selectedBranch = remember { mutableStateOf(routeBranches.currentBranchStatus(currentLocalDateTime(), instance).asSequence().sortedByDescending { it.value.activeness }.first().key) }
    val selectedStop = remember { mutableIntStateOf(
        ((instance.compose.data["scrollToStop"] as? String)?: (instance.compose.data["stopId"] as? String))
            ?.let { id ->
                val allStops = Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gmbRegion)
                val index = allStops.indexOfFirst { it.stopId == id }
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
    val allStops by remember(route, selectedBranch) { derivedStateOf { Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gmbRegion) } }

    val launchedWithStop = remember { route.stopInfo != null || instance.compose.data.containsKey("scrollToStop") || instance.compose.data.containsKey("stopId") }

    val listStopsTabItem by remember(route) { derivedStateOf { listStopsTabItem(co) } }

    val pagerState = rememberPagerState { listStopsTabItem.size }
    val scope = rememberCoroutineScope()
    val pagerScrollEnabled = remember { mutableStateOf(true) }

    val sheetState = rememberPlatformModalBottomSheetState()
    var showingSimilarRoutes by rememberSaveable { mutableStateOf(false) }

    var location: OriginData? by remember { mutableStateOf(null) }

    val timesStartIndexState = remember { mutableIntStateOf(1) }
    val timesInitState = remember { mutableStateOf(false) }

    val optAlightReminderService by AlightReminderService.currentInstance.collectAsStateMultiplatform()
    val alightReminderService by remember(optAlightReminderService) { derivedStateOf { optAlightReminderService.value } }
    val isActiveReminderService by remember(alightReminderService) { derivedStateOf { alightReminderService?.selectedRoute?.let { routeBranches.contains(it) } == true } }

    val alternateStopNamesShowing = Shared.alternateStopNamesShowingState.collectAsStateMultiplatform { Registry.getInstance(instance).setAlternateStopNames(it, instance) }

    instance.compose.setStatusNavBarColor(
        status = platformPrimaryContainerColor,
        nav = platformSurfaceContainerColor
    )

    LaunchedEffect (showingSimilarRoutes) {
        ScreenState.hasInterruptElement.value = showingSimilarRoutes
    }
    LaunchedEffect (isActiveReminderService) {
        if (isActiveReminderService) {
            while (true) {
                alightReminderService?.let {
                    selectedBranch.value = it.selectedRoute
                    selectedStop.value = it.currentStop.index
                }
                delay(1000)
            }
        }
    }
    LaunchedEffect (Unit) {
        if (!launchedWithStop) {
            val fastResult = getGPSLocation(instance, LocationPriority.FASTER).await()
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
        CoroutineScope(dispatcherIO).launch {
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
    LaunchedEffect (route, selectedBranch.value, selectedStop.intValue) {
        val stopIndex = selectedStop.intValue
        val stop = allStops[stopIndex - 1]
        val branch = selectedBranch.value
        updateBrowserState("${co.getDisplayName(routeNumber, branch.isKmbCtbJoint, gmbRegion, Shared.language)} ${co.getDisplayRouteNumber(routeNumber)} ${branch.resolvedDest(true)[Shared.language]} ${if (!co.isTrain) "${stopIndex}." else ""} ${stop.stop.name[Shared.language]}", branch.getDeepLink(instance, stop.stopId, stopIndex))
    }
    DisposableEffect (Unit) {
        onDispose { updateBrowserState("", BASE_URL) }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        PlatformTopAppBar(
            title = {
                @Suppress("RemoveExplicitTypeArguments")
                ConditionalComposable<RowScope>(
                    condition = composePlatform.applePlatform && similarRoutes[selectedBranch.value] != null,
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
                            modifier = Modifier.applyIf(lineCount == 1) { alignByBaseline() },
                            text = co.getListDisplayRouteNumber(routeNumber, true),
                            fontSize = 30.dp.sp,
                            lineHeight = 1.1F.em
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
                            text = route.route!!.resolvedDestWithBranchFormatted(false, selectedBranch.value, selectedStop.intValue, allStops[selectedStop.intValue - 1].stopId, instance)[Shared.language].asContentAnnotatedString().annotatedString,
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
                                CoroutineScope(dispatcherIO).launch {
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
                    if (!composePlatform.applePlatform && similarRoutes[selectedBranch.value] != null) {
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
            fontSizeRange = FontSizeRange(min = 11F.sp, max = 16F.sp),
            preferSingleLine = true
        )
        PlatformTabRow(
            modifier = Modifier
                .applyIf(composePlatform.applePlatform) { platformHorizontalDividerShadow() },
            selectedTabIndex = pagerState.currentPage
        ) {
            listStopsTabItem.forEachIndexed { index, item ->
                PlatformTab(
                    selected = index == pagerState.currentPage,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        AutoResizeText(
                            modifier = Modifier.applyIf(composePlatform.applePlatform) { padding(horizontal = 5.dp) },
                            autoResizeTextState = autoFontSizeState,
                            lineHeight = 1.1F.em,
                            text = if (index == 2 && importantNoticeCount > 0) {
                                "${item.title[Shared.language]} ${importantNoticeCount.getCircledNumber()}"
                            } else {
                                item.title[Shared.language]
                            }
                        )
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
                    StopsTabItemType.STOPS -> MapStopsInterface(instance, location, route, selectedStop, selectedBranch, possibleBidirectionalSectionFare, alertCheckRoute, isActiveReminderService, pagerScrollEnabled, alternateStopNamesShowing)
                    StopsTabItemType.TIMES -> ListStopsEtaInterface(instance, ListStopsInterfaceType.TIMES, location, route, selectedStop, selectedBranch, alternateStopNamesShowing, timesStartIndexState = timesStartIndexState, timesInitState = timesInitState)
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
                        ListRoutesInterface(instance, similarRoutes[selectedBranch.value]?: persistentListOf(), false, RouteListType.NORMAL, false, RecentSortMode.DISABLED, null, true)
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
    possibleBidirectionalSectionFare: Boolean,
    alertCheckRoute: Boolean,
    isActiveReminderService: Boolean,
    pagerScrollEnabled: MutableState<Boolean>,
    alternateStopNamesShowingState: MutableState<Boolean>
) {
    var mapExpanded by remember { mutableStateOf(false) }
    val selectedBranch by selectedBranchState

    val routeNumber by remember(listRoute) { derivedStateOf { listRoute.route!!.routeNumber } }
    val co by remember(listRoute) { derivedStateOf { listRoute.co } }
    val bound by remember(listRoute, co) { derivedStateOf { listRoute.route!!.idBound(co) } }
    val gmbRegion by remember(listRoute) { derivedStateOf { listRoute.route!!.gmbRegion } }
    val isKmbCtbJoint by remember(listRoute) { derivedStateOf { listRoute.route!!.isKmbCtbJoint } }
    val allStops by remember(routeNumber, co, bound, gmbRegion, listRoute, selectedBranch) { derivedStateOf { Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gmbRegion) } }
    val stops by remember(allStops, co, selectedBranch) { derivedStateOf { (if (co.isTrain) allStops else allStops.filter { it.branchIds.contains(selectedBranch) }).map { it.stop } } }

    val alternateStopNamesShowing by alternateStopNamesShowingState

    val pipMode = rememberIsInPipMode(instance)

    if (Shared.showRouteMap && !pipMode) {
        var waypoints by remember { mutableStateOf(if (co.isTrain) listRoute.defaultWaypoints(instance) else selectedBranch.defaultWaypoints(instance)) }

        val alternateStopNames by remember(listRoute, isKmbCtbJoint) { derivedStateOf { if (isKmbCtbJoint) {
            Registry.getInstance(instance).findEquivalentStops(allStops.map { it.stopId }, Operator.CTB).asImmutableList()
        } else {
            null
        }.asImmutableState() } }

        LaunchedEffect (selectedBranch, stops) {
            CoroutineScope(dispatcherIO).launch {
                val waypointsAsync = Registry.getInstance(instance).getRouteWaypoints(selectedBranch, instance, stops).await()
                withContext(Dispatchers.Main) {
                    waypoints = waypointsAsync
                }
            }
        }

        AdaptiveTopBottomLayout(
            modifier = Modifier.fillMaxWidth(),
            context = instance,
            animateSize = true,
            bottomSize = { when {
                !it.isNarrow -> min(412F, (instance.screenWidth / 2F).pixelsToDp(instance)).dp
                mapExpanded -> 200.dp
                else -> (instance.screenHeight.toFloat() / 11 * 6).pixelsToDp(instance).dp
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

@OptIn(ExperimentalLayoutApi::class, ReduceDataOmitted::class, ReduceDataPossiblyOmitted::class)
@Composable
fun TimetableInterface(instance: AppActiveContext, routes: ImmutableList<Route>) {
    val now by currentMinuteState.collectAsStateMultiplatform()
    val timetableData by remember(routes) { derivedStateOf { routes.createTimetable(instance) } }
    val compareMidnight by remember(timetableData) { derivedStateOf { if (timetableData.isNightRoute()) nightServiceMidnight else dayServiceMidnight } }
    val weekday by remember(now, compareMidnight) { derivedStateOf { now.dayOfWeek(Registry.getInstance(instance).getHolidays(), compareMidnight) } }
    val timetableKeysSorted by remember(timetableData) { derivedStateOf { timetableData.keys.sorted() } }
    val timetableCurrentEntries by remember(now, routes) { derivedStateOf { timetableKeysSorted.associateWith { if (it.contains(weekday)) timetableData[it]!!.currentEntry(now) else emptyList() } } }
    val currentBranches by remember(timetableData, timetableCurrentEntries) { derivedStateOf { timetableCurrentEntries.asSequence().map { (k, v) -> timetableData[k]!!.asSequence().filterIndexed { i, _ -> v.contains(i) }.map { it.route } }.flatten().toSet() } }
    val scroll = rememberScrollState()
    val hasJourneyTimeBranches by remember(routes) { derivedStateOf { routes.asSequence().filter { it.journeyTime != null }.associateWith { it.journeyTime!! } } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScrollWithScrollbar(
                state = scroll,
                flingBehavior = ScrollableDefaults.flingBehavior(),
                scrollbarConfig = ScrollBarConfig(
                    indicatorThickness = 4.dp
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column (
            modifier = Modifier.padding(top = 25.dp, bottom = 60.dp, start = 50.dp, end = 50.dp)
        ) {
            if (hasJourneyTimeBranches.isNotEmpty()) {
                val distinctTimes = hasJourneyTimeBranches.values.distinct()
                if (distinctTimes.size <= 1) {
                    val circular = if (hasJourneyTimeBranches.keys.any { it.isCircular }) (if (Shared.language == "en") " (One way)" else " (單向)") else ""
                    PlatformText(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold.takeIf { currentBranches.isNotEmpty() },
                        text = if (Shared.language == "en") "Full Journey Time: ${distinctTimes.first()} Min.$circular" else "全程車程: ${distinctTimes.first()}分鐘$circular"
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PlatformText(
                            fontSize = 16.sp,
                            text = if (Shared.language == "en") "Full Journey Times" else "全程車程"
                        )
                        PlatformText(
                            modifier = Modifier.weight(1F),
                            textAlign = TextAlign.End,
                            fontSize = 16.sp,
                            text = if (Shared.language == "en") "Times (Min.)" else "車程(分鐘)"
                        )
                    }
                    HorizontalDivider()
                    for ((route, journeyTime) in hasJourneyTimeBranches) {
                        val remark = route.resolveSpecialRemark(instance, RemarkType.LABEL_MAIN_BRANCH)
                        val weight = FontWeight.Bold.takeIf { currentBranches.contains(route) }
                        val circular = if (route.isCircular) (if (Shared.language == "en") " (One way)" else " (單向)") else ""
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PlatformText(
                                modifier = Modifier.weight(1F),
                                fontSize = 16.sp,
                                fontWeight = weight,
                                textAlign = TextAlign.Start,
                                text = "${remark[Shared.language]}$circular"
                            )
                            PlatformText(
                                modifier = Modifier.padding(start = 5.dp),
                                fontSize = 16.sp,
                                fontWeight = weight,
                                text = journeyTime.toString()
                            )
                        }
                    }
                }
            }
            var denoteIndex = 0
            val remarks = timetableData.values.asSequence()
                .flatten()
                .filter { it.specialRouteRemark != null }
                .groupBy { it.specialRouteRemark!! }
                .mapValues { it.value to denoteIndex++ }
            val onlyDaily by remember(timetableKeysSorted) { derivedStateOf { timetableKeysSorted.size <= 1 } }
            timetableKeysSorted.forEach { weekdays ->
                var dayExpanded by remember { mutableStateOf(onlyDaily || composePlatform.hasLargeScreen || weekdays.contains(weekday)) }
                val dayDropdownIconDegree by animateFloatAsState(
                    targetValue = if (dayExpanded) 180F else 0F,
                    animationSpec = tween(300)
                )
                val entries = timetableData[weekdays]!!
                val anyIntervals = entries.any { it is TimetableIntervalEntry }
                val currentIndexes = timetableCurrentEntries[weekdays]!!
                var boxWidth by remember { mutableStateOf(0) }
                var leftWidth by remember { mutableStateOf(0) }
                var rightWidth by remember { mutableStateOf(0) }
                var settled by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                        .applyIf(!onlyDaily) { clickable { dayExpanded = !dayExpanded } }
                        .onSizeChanged { boxWidth = it.width }
                        .applyIf(settled) { animateContentSize() },
                    contentAlignment = Alignment.TopStart
                ) {
                    val style = FontWeight.Bold.takeIf { weekdays.contains(weekday) && currentIndexes.isNotEmpty() }
                    Row(
                        modifier = Modifier.onSizeChanged { leftWidth = it.width },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlatformText(
                            textAlign = TextAlign.Start,
                            fontSize = 16.sp,
                            fontWeight = style,
                            text = weekdays.displayText[Shared.language]
                        )
                        if (!onlyDaily) {
                            PlatformIcon(
                                modifier = Modifier
                                    .rotate(dayDropdownIconDegree)
                                    .size(20.fontScaledDp),
                                painter = PlatformIcons.Filled.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        visible = anyIntervals && dayExpanded
                    ) {
                        PlatformText(
                            modifier = Modifier
                                .onSizeChanged { rightWidth = it.width }
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val flow = if (leftWidth + rightWidth > boxWidth) placeable.height else 0
                                    layout(placeable.width, placeable.height + flow) {
                                        placeable.placeRelative(0, flow)
                                    }.apply {
                                        settled = true
                                    }
                                },
                            textAlign = TextAlign.End,
                            fontSize = 16.sp,
                            fontWeight = style,
                            maxLines = 1,
                            text = if (Shared.language == "en") "Interval (Min.)" else "班次(分鐘)"
                        )
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .animateContentSize()
                ) {
                    if (dayExpanded) {
                        val timetableDisplayEntries = mutableListOf<TimetableDisplayEntries>()
                        for ((index, entry) in entries.withIndex()) {
                            val style = SpanStyle(fontWeight = FontWeight.Bold.takeIf { currentIndexes.contains(index) })
                            val stars = (entry.specialRouteRemark?.let { specialDenoteChar(remarks[it]!!.second) }?: "").asAnnotatedString(style)
                            when (entry) {
                                is TimetableIntervalEntry -> {
                                    val currentSubIndexes by remember(now, entry) { derivedStateOf { if (weekdays.contains(weekday)) entry.subEntries.currentEntry(now) else emptyList() } }
                                    val subDisplayEntries: List<TimetableDisplayEntries>? = if (entry.subEntries.size > 1) {
                                        buildList {
                                            for ((subIndex, subEntry) in entry.subEntries.withIndex()) {
                                                val subStyle = SpanStyle(fontWeight = FontWeight.Bold.takeIf { currentSubIndexes.contains(subIndex) })
                                                add(
                                                    TimetableDisplayEntries(
                                                        timeString = stars + subEntry.toString(Shared.language).asAnnotatedString(subStyle),
                                                        intervalString = subEntry.interval.toDisplayText().asAnnotatedString(subStyle)
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        null
                                    }
                                    timetableDisplayEntries.add(
                                        TimetableDisplayEntries(
                                            timeString = stars + entry.toString(Shared.language).asAnnotatedString(style),
                                            intervalString = entry.interval.toDisplayText().asAnnotatedString(style),
                                            subDisplayEntries = subDisplayEntries
                                        )
                                    )
                                }
                                is TimetableSingleEntry -> {
                                    if (anyIntervals || timetableDisplayEntries.lastOrNull()?.intervalString?.isNotEmpty() == true) {
                                        timetableDisplayEntries.add(
                                            TimetableDisplayEntries(
                                                timeString = stars + entry.toString(Shared.language).asAnnotatedString(style),
                                                intervalString = "".asAnnotatedString()
                                            )
                                        )
                                    } else {
                                        val lastEntry = timetableDisplayEntries.removeLastOrNull()
                                        val lastString = lastEntry?.timeString?.let { it + ", ".asAnnotatedString() }?: "".asAnnotatedString()
                                        timetableDisplayEntries.add(
                                            TimetableDisplayEntries(
                                                timeString = lastString + stars + entry.toString(Shared.language).asAnnotatedString(style),
                                                intervalString = "".asAnnotatedString()
                                            )
                                        )
                                    }
                                }
                                is TimetableSpecialEntry -> {
                                    timetableDisplayEntries.add(
                                        TimetableDisplayEntries(
                                            timeString = stars + entry.toString(Shared.language).asAnnotatedString(style),
                                            intervalString = " ".asAnnotatedString()
                                        )
                                    )
                                }
                            }
                        }
                        for (entry in timetableDisplayEntries) {
                            var expanded by remember { mutableStateOf(false) }
                            val dropdownIconDegree by animateFloatAsState(
                                targetValue = if (expanded) 180F else 0F,
                                animationSpec = tween(300)
                            )
                            Column(
                                modifier = Modifier
                                    .applyIf(entry.subDisplayEntries != null) { clickable { expanded = !expanded } }
                                    .animateContentSize()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.wrapContentSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlatformText(
                                            fontSize = 16.sp,
                                            text = entry.timeString
                                        )
                                        if (entry.subDisplayEntries != null) {
                                            PlatformIcon(
                                                modifier = Modifier
                                                    .rotate(dropdownIconDegree)
                                                    .size(20.fontScaledDp),
                                                painter = PlatformIcons.Filled.ArrowDropDown,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    PlatformText(
                                        fontSize = 16.sp,
                                        text = entry.intervalString
                                    )
                                }
                                if (expanded && entry.subDisplayEntries != null) {
                                    for (subEntry in entry.subDisplayEntries) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 20.fontScaledDp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            PlatformText(
                                                fontSize = 14.sp,
                                                text = subEntry.timeString
                                            )
                                            PlatformText(
                                                fontSize = 14.sp,
                                                text = subEntry.intervalString
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(25.dp))
            for ((remark, pair) in remarks) {
                val (entries, denote) = pair
                PlatformText(
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold.takeIf { entries.any { e -> currentBranches.contains(e.route) } },
                    text = specialDenoteChar(denote) + remark[Shared.language]
                )
            }
        }
    }
}

@Immutable
data class TimetableDisplayEntries(
    val timeString: AnnotatedString,
    val intervalString: AnnotatedString,
    val subDisplayEntries: List<TimetableDisplayEntries>? = null
)