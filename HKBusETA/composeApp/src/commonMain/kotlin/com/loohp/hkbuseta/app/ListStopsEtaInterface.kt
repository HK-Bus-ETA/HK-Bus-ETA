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

package com.loohp.hkbuseta.app

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.SubcomposeAsyncImage
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.ScreenState
import com.loohp.hkbuseta.appcontext.applicationAppContext
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.appcontext.sendToWatch
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.ETADisplayMode
import com.loohp.hkbuseta.common.objects.FavouriteRouteGroup
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.FavouriteStop
import com.loohp.hkbuseta.common.objects.FavouriteStopMode
import com.loohp.hkbuseta.common.objects.GMBRegion
import com.loohp.hkbuseta.common.objects.NextBusTextDisplayMode
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.OriginData
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RemarkType
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.RouteWaypoints
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.TemporaryPinItem
import com.loohp.hkbuseta.common.objects.TrafficSnapshotPoint
import com.loohp.hkbuseta.common.objects.WearableConnectionState
import com.loohp.hkbuseta.common.objects.add
import com.loohp.hkbuseta.common.objects.anyEquals
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.bilingualToPrefix
import com.loohp.hkbuseta.common.objects.component3
import com.loohp.hkbuseta.common.objects.findSame
import com.loohp.hkbuseta.common.objects.getDeepLink
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getDisplayText
import com.loohp.hkbuseta.common.objects.getMTRStationLayoutUrl
import com.loohp.hkbuseta.common.objects.getMTRStationStreetMapUrl
import com.loohp.hkbuseta.common.objects.getOperatorName
import com.loohp.hkbuseta.common.objects.hasStop
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.indexOfName
import com.loohp.hkbuseta.common.objects.isBus
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.mapTrafficSnapshots
import com.loohp.hkbuseta.common.objects.remove
import com.loohp.hkbuseta.common.objects.removeStop
import com.loohp.hkbuseta.common.objects.resolveSpecialRemark
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.objects.shouldPrependTo
import com.loohp.hkbuseta.common.objects.toStopIndexed
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.services.AlightReminderService
import com.loohp.hkbuseta.common.services.AlightReminderServiceState
import com.loohp.hkbuseta.common.services.StopIdIndexed
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Shared.getResolvedText
import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.MTRStopSectionData
import com.loohp.hkbuseta.common.utils.Optional
import com.loohp.hkbuseta.common.utils.RouteBranchStatus
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.buildImmutableList
import com.loohp.hkbuseta.common.utils.createMTRLineSectionData
import com.loohp.hkbuseta.common.utils.currentBranchStatus
import com.loohp.hkbuseta.common.utils.currentEpochSeconds
import com.loohp.hkbuseta.common.utils.currentMinuteState
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.floorToInt
import com.loohp.hkbuseta.common.utils.isNotNullAndNotEmpty
import com.loohp.hkbuseta.compose.Add
import com.loohp.hkbuseta.compose.ArrowDropDown
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.Code
import com.loohp.hkbuseta.compose.DeleteDialog
import com.loohp.hkbuseta.compose.DeleteForever
import com.loohp.hkbuseta.compose.EmojiFlags
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.Fullscreen
import com.loohp.hkbuseta.compose.LocationOn
import com.loohp.hkbuseta.compose.Map
import com.loohp.hkbuseta.compose.MoreHoriz
import com.loohp.hkbuseta.compose.MoreVert
import com.loohp.hkbuseta.compose.NotificationsActive
import com.loohp.hkbuseta.compose.NotificationsOff
import com.loohp.hkbuseta.compose.PhotoCamera
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformCircularProgressIndicator
import com.loohp.hkbuseta.compose.PlatformFilledTonalIconButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformModalBottomSheet
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.PushPin
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.Share
import com.loohp.hkbuseta.compose.Star
import com.loohp.hkbuseta.compose.StarOutline
import com.loohp.hkbuseta.compose.Streetview
import com.loohp.hkbuseta.compose.SwipeRightAlt
import com.loohp.hkbuseta.compose.SyncAlt
import com.loohp.hkbuseta.compose.TextInputDialog
import com.loohp.hkbuseta.compose.Train
import com.loohp.hkbuseta.compose.TransferWithinAStation
import com.loohp.hkbuseta.compose.Watch
import com.loohp.hkbuseta.compose.Widget
import com.loohp.hkbuseta.compose.WidgetAdd
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.clickable
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.combinedClickable
import com.loohp.hkbuseta.compose.currentLocalWindowSize
import com.loohp.hkbuseta.compose.enterPipMode
import com.loohp.hkbuseta.compose.itemsIndexedPossiblySticky
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.compose.platformBackgroundColor
import com.loohp.hkbuseta.compose.platformHorizontalDividerShadow
import com.loohp.hkbuseta.compose.platformLargeShape
import com.loohp.hkbuseta.compose.platformLocalContentColor
import com.loohp.hkbuseta.compose.platformPrimaryContainerColor
import com.loohp.hkbuseta.compose.rememberIsInPipMode
import com.loohp.hkbuseta.compose.rememberPlatformModalBottomSheetState
import com.loohp.hkbuseta.compose.table.DataColumn
import com.loohp.hkbuseta.compose.table.DataTable
import com.loohp.hkbuseta.compose.table.TableColumnWidth
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.compose.userMarqueeMaxLines
import com.loohp.hkbuseta.compose.verticalScrollBar
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.shared.ComposeShared
import com.loohp.hkbuseta.utils.Big
import com.loohp.hkbuseta.utils.BusLineSection
import com.loohp.hkbuseta.utils.BusLineSectionExtension
import com.loohp.hkbuseta.utils.BusRouteLineData
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.MTRLineSection
import com.loohp.hkbuseta.utils.MTRLineSectionExtension
import com.loohp.hkbuseta.utils.RouteHighlightType
import com.loohp.hkbuseta.utils.RouteStopETALiveActivity
import com.loohp.hkbuseta.utils.RouteStopETASelectedRouteStop
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.checkNotificationPermission
import com.loohp.hkbuseta.utils.clearColors
import com.loohp.hkbuseta.utils.copyToClipboard
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.fontScaledDp
import com.loohp.hkbuseta.utils.generateLineTypes
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.joinToAnnotatedString
import com.loohp.hkbuseta.utils.px
import com.loohp.hkbuseta.utils.sp
import io.ktor.util.collections.ConcurrentMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import org.jetbrains.compose.resources.painterResource


@Immutable
data class KeyedNextBusPosition(
    val position: Registry.NextBusPosition,
    val branch: Route,
    val etaQueryOptions: Registry.EtaQueryOptions
)

enum class ListStopsInterfaceType(val showBranches: Boolean, val canChangeBranch: Boolean) {
    ETA(true, true),
    TIMES(false, false),
    TRAFFIC_SNAPSHOTS(false, false),
    ALIGHT_REMINDER(true, false)
}

enum class BottomSheetType(val show: Boolean = true) {
    ACTIONS, FAV, NEARBY, NONE(false)
}

val RouteBranchStatus?.indicatorColor: Color get() = when (this) {
    RouteBranchStatus.SOON_BEGIN -> Color.Green.adjustAlpha(0.2F)
    RouteBranchStatus.ACTIVE -> Color.Green
    RouteBranchStatus.HOUR_GAP -> Color(0xFFFFCC66)
    RouteBranchStatus.LAST_LEFT_TERMINUS -> Color.Yellow
    RouteBranchStatus.INACTIVE -> Color.Gray
    else -> Color.Gray
}

val RouteBranchStatus?.description: BilingualText get() = when (this) {
    RouteBranchStatus.SOON_BEGIN -> "表定班次將於60分鐘內從總站開出" withEn "Scheduled service within 60 minutes"
    RouteBranchStatus.ACTIVE -> "表定班次現正服務" withEn "Active scheduled services"
    RouteBranchStatus.HOUR_GAP -> "上一班表定班次已從總站開出 下一班表定班次將於60分鐘內從總站開出" withEn "Last scheduled service departed from terminus, next scheduled service within 60 minutes"
    RouteBranchStatus.LAST_LEFT_TERMINUS -> "表定尾班車已從總站開出" withEn "Last scheduled service departed from terminus"
    RouteBranchStatus.INACTIVE -> "目前沒有表定班次將於60分鐘內從總站開出" withEn "No scheduled services within 60 minutes"
    else -> "沒有表定班次" withEn "No scheduled services"
}

private val etaUpdateScope: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(4)

private fun generateRouteLineData(instance: AppActiveContext, routeNumber: String, co: Operator, isLrtCircular: Boolean, lineColor: Color, stopsList: ImmutableList<Registry.StopData>, selectedBranch: Route): List<Any> {
    return if (co.isTrain) {
        val mtrStopsInterchange = stopsList.asSequence().map { Registry.getInstance(instance).getMtrStationInterchange(it.stopId, routeNumber) }.toImmutableList()
        createMTRLineSectionData(
            co = co,
            color = lineColor.toArgb().toLong(),
            stopList = stopsList,
            mtrStopsInterchange = mtrStopsInterchange,
            isLrtCircular = isLrtCircular,
            context = instance
        )
    } else {
        generateLineTypes(lineColor, stopsList, selectedBranch)
    }
}

private fun jointStopId(stopId: String, route: Route, context: AppContext): List<Pair<String, Operator?>> {
    if (!route.isKmbCtbJoint) return listOf(element = stopId to null)
    return listOf(
        stopId to Operator.KMB,
        Registry.getInstance(context).findJointAlternateStops(listOf(element = stopId), route.routeNumber).first().stopId to Operator.CTB
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListStopsEtaInterface(
    instance: AppActiveContext,
    type: ListStopsInterfaceType,
    location: OriginData?,
    listRoute: RouteSearchResultEntry,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    alternateStopNamesShowingState: MutableState<Boolean>,
    possibleBidirectionalSectionFare: Boolean = false,
    alertCheckRoute: Boolean = false,
    floatingActions: (@Composable BoxScope.(LazyListState) -> Unit)? = null,
    timesStartIndexState: MutableIntState = remember { mutableIntStateOf(1) },
    timesInitState: MutableState<Boolean> = remember { mutableStateOf(false) },
    waypoints: RouteWaypoints? = null,
    trafficSnapshots: Array<out List<TrafficSnapshotPoint>>? = null
) {
    val routeNumber by remember(listRoute) { derivedStateOf { listRoute.route!!.routeNumber } }
    val co by remember(listRoute) { derivedStateOf { listRoute.co } }
    val bound by remember(listRoute) { derivedStateOf { listRoute.route!!.idBound(co) } }
    val gmbRegion by remember(listRoute) { derivedStateOf { listRoute.route!!.gmbRegion } }
    val isKmbCtbJoint by remember(listRoute) { derivedStateOf { listRoute.route!!.isKmbCtbJoint } }

    val routeBranches by remember(listRoute) { derivedStateOf { Registry.getInstance(instance).getAllBranchRoutes(routeNumber, bound, co, gmbRegion).asImmutableList() } }
    val selectedBranch by selectedBranchState
    var selectedStop by selectedStopState
    val allStops by remember(listRoute) { derivedStateOf { Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gmbRegion).asImmutableList() } }

    val rawLineColor by remember { derivedStateOf { co.getLineColor(routeNumber, Color.Red) } }
    val lineColor by ComposeShared.rememberOperatorColor(rawLineColor, Operator.CTB.getOperatorColor(Color.Yellow).takeIf { isKmbCtbJoint })

    val mtrStopsInterchange by remember { derivedStateOf { if (co.isTrain) { allStops.asSequence().map { Registry.getInstance(instance).getMtrStationInterchange(it.stopId, routeNumber) }.toImmutableList() } else persistentListOf() } }
    val routeLineData by remember(listRoute) { derivedStateOf { generateRouteLineData(instance, routeNumber, co, listRoute.route!!.lrtCircular != null, lineColor, allStops, selectedBranch).asImmutableState() } }

    val etaResults = remember { ConcurrentMap<Int, Registry.ETAQueryResult>().asImmutableState() }
    val etaUpdateTimes = remember { ConcurrentMap<Int, Long>().asImmutableState() }
    val nextBusPositions = remember { ConcurrentMap<Int, Optional<KeyedNextBusPosition>>().asImmutableState() }

    val times: SnapshotStateMap<Int, Registry.TimeBetweenStopResult> = remember { mutableStateMapOf() }
    var timesStartIndex by timesStartIndexState
    var timesInit by timesInitState

    val lrtDirectionModeState = remember { mutableStateOf(Shared.lrtDirectionMode) }
    var lrtDirectionMode by lrtDirectionModeState

    val optAlightReminderService by AlightReminderService.currentInstance.collectAsStateMultiplatform()
    val alightReminderService by remember { derivedStateOf { optAlightReminderService.value } }
    val isActiveReminderService by remember { derivedStateOf { alightReminderService?.selectedRoute?.let { routeBranches.contains(it) } == true } }
    val alightReminderHighlightBlinkState = remember { mutableStateOf(false) }
    val alightReminderCurrentStopState: MutableState<StopIdIndexed?> = remember { mutableStateOf(null) }
    val alightReminderStateState: MutableState<AlightReminderServiceState?> = remember { mutableStateOf(null) }
    val alightReminderTimeLeftState = remember { mutableIntStateOf(-1) }
    val alightReminderStopsLeftState = remember { mutableIntStateOf(-1) }
    var alightReminderHighlightBlink by alightReminderHighlightBlinkState
    var alightReminderCurrentStop by alightReminderCurrentStopState
    var alightReminderState by alightReminderStateState
    var alightReminderTimeLeft by alightReminderTimeLeftState
    var alightReminderStopsLeft by alightReminderStopsLeftState

    val togglingAlightReminderState = remember { mutableStateOf(false) }

    val scroll = rememberLazyListState()
    val sheetState = rememberPlatformModalBottomSheetState()
    val sheetTypeState = rememberSaveable { mutableStateOf(BottomSheetType.NONE) }
    val sheetType by sheetTypeState

    val alternateStopNames by remember(listRoute) { derivedStateOf { if (isKmbCtbJoint) {
        Registry.getInstance(instance).findJointAlternateStops(allStops.map { it.stopId }, routeNumber).asImmutableList()
    } else {
        null
    }.asImmutableState() } }

    val trafficSnapshotsByAllStops by remember(waypoints, trafficSnapshots) { derivedStateOf { allStops.mapTrafficSnapshots(waypoints, trafficSnapshots) } }

    val pipMode = rememberIsInPipMode(instance)

    LaunchedEffect (isActiveReminderService) {
        if (isActiveReminderService) {
            while (true) {
                alightReminderService?.let {
                    alightReminderCurrentStop = it.currentStop
                    alightReminderState = it.state
                }
                delay(1000)
            }
        }
    }
    LaunchedEffect (alightReminderService, alightReminderCurrentStop, alightReminderState) {
        alightReminderService?.let { service -> alightReminderCurrentStop?.let { currentStop ->
            alightReminderStopsLeft = service.stopsRemaining
            alightReminderTimeLeft = Registry.getInstance(instance).getTimeBetweenStop(allStops.map { it.stopId to it.branchIds.contains(selectedBranch) }, currentStop.index - 1, service.destinationStopId.index - 1, false).await().averageInterval
        } }
    }
    LaunchedEffect (sheetType) {
        ScreenState.hasInterruptElement.value = sheetType.show
        if (sheetType.show) {
            sheetState.show()
        }
    }
    LaunchedEffect (location) {
        if (location != null) {
            val (index, _, distance) = allStops.asSequence()
                .mapIndexed { i, s -> Triple(i, s, location.distance(s.stop.location)) }
                .minBy { it.third }
            when (type) {
                ListStopsInterfaceType.ETA -> {
                    if (!location.onlyInRange || distance < 0.3) {
                        scroll.scrollToItem(index)
                        selectedStop = index + 1
                    }
                }
                ListStopsInterfaceType.TIMES -> {
                    if (!timesInit) {
                        scroll.scrollToItem(index)
                        if (type == ListStopsInterfaceType.TIMES) {
                            timesStartIndex = if (index >= allStops.size - 1) index else index + 1
                        }
                        timesInit = true
                    }
                }
                else -> { /* do nothing */ }
            }
        }
    }
    RestartEffect {
        lrtDirectionMode = Shared.lrtDirectionMode
    }
    LaunchedEffect (timesStartIndex) {
        if (type == ListStopsInterfaceType.TIMES) {
            times.clear()
            for (i in 1..allStops.size) {
                times[i] = Registry.TimeBetweenStopResult.LOADING
            }
            for (i in allStops.size downTo timesStartIndex) {
                times[i] = Registry.getInstance(instance).getTimeBetweenStop(allStops.map { it.stopId to it.branchIds.contains(selectedBranch) }.toList(), timesStartIndex - 1, i - 1, false).await()
            }
            for (i in allStops.size downTo timesStartIndex) {
                val time = times[i]
                if (time != null && time.currentHourlyInterval == null) {
                    times[i] = Registry.getInstance(instance).getTimeBetweenStop(allStops.map { it.stopId to it.branchIds.contains(selectedBranch) }.toList(), timesStartIndex - 1, i - 1, true).await()
                }
            }
        }
    }
    LaunchedEffect (selectedBranch) {
        if (!pipMode && !allStops[selectedStop - 1].branchIds.contains(selectedBranch)) {
            val firstBranchStopIndex = allStops.indexOfFirst { it.branchIds.contains(selectedBranch) }
            if (firstBranchStopIndex >= 0 && scroll.firstVisibleItemIndex < firstBranchStopIndex) {
                selectedStop = firstBranchStopIndex + 1
            }
        }
    }
    LaunchedEffect (selectedStop) {
        if (!pipMode) {
            if (type == ListStopsInterfaceType.ALIGHT_REMINDER || type == ListStopsInterfaceType.ETA || scroll.layoutInfo.visibleItemsInfo.lastOrNull()?.let { it.index == selectedStop - 1 } == true) {
                delay(200)
                scroll.animateScrollToItem(selectedStop - 1)
            }
        }
    }
    LaunchedEffect (Unit) {
        while (true) {
            alightReminderHighlightBlink = !alightReminderHighlightBlink
            delay(1000)
        }
    }

    if (pipMode) {
        PipModeInterface(
            index = selectedStop,
            route = selectedBranch,
            co = co,
            isKmbCtbJoint = isKmbCtbJoint,
            options = Registry.EtaQueryOptions(lrtDirectionMode),
            stopData = allStops[selectedStop - 1],
            etaResults = etaResults,
            etaUpdateTimes = etaUpdateTimes,
            instance = instance
        )
    } else {
        Scaffold(
            content = { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    RouteBranchBar(
                        instance = instance,
                        type = type,
                        co = co,
                        routeBranches = routeBranches,
                        selectedBranchState = selectedBranchState
                    )
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScrollBar(
                                    state = scroll,
                                    scrollbarConfig = ScrollBarConfig(
                                        indicatorThickness = 6.dp,
                                        padding = PaddingValues(0.dp, 2.dp, 0.dp, 2.dp)
                                    )
                                ),
                            state = scroll
                        ) {
                            itemsIndexedPossiblySticky(
                                items = allStops,
                                key = { i, _ -> i + 1 },
                                sticky = { i, _ -> type == ListStopsInterfaceType.TIMES && i + 1 == timesStartIndex }
                            ) { i, stopData, sticky ->
                                StopEntry(
                                    instance = instance,
                                    type = type,
                                    selectedStopState = selectedStopState,
                                    selectedBranchState = selectedBranchState,
                                    possibleBidirectionalSectionFare = possibleBidirectionalSectionFare,
                                    alertCheckRoute = alertCheckRoute,
                                    alternateStopNames = alternateStopNames,
                                    index = i + 1,
                                    allStops = allStops,
                                    stopData = stopData,
                                    routeBranches = routeBranches,
                                    routeLineData = routeLineData,
                                    mtrStopsInterchange = mtrStopsInterchange,
                                    routeNumber = routeNumber,
                                    co = co,
                                    gmbRegion = gmbRegion,
                                    isKmbCtbJoint = isKmbCtbJoint,
                                    timesStartIndexState = timesStartIndexState,
                                    times = times,
                                    alightReminderHighlightBlinkState = alightReminderHighlightBlinkState,
                                    alightReminderStateState = alightReminderStateState,
                                    alightReminderTimeLeftState = alightReminderTimeLeftState,
                                    alightReminderStopsLeftState = alightReminderStopsLeftState,
                                    lrtDirectionModeState = lrtDirectionModeState,
                                    etaResultsState = etaResults,
                                    etaUpdateTimesState = etaUpdateTimes,
                                    nextBusPositionState = nextBusPositions,
                                    sheetTypeState = sheetTypeState,
                                    togglingAlightReminderState = togglingAlightReminderState,
                                    alternateStopNamesShowingState = alternateStopNamesShowingState,
                                    trafficSnapshots = trafficSnapshotsByAllStops?.getOrNull(i),
                                    sticky = sticky
                                )
                                if (sticky) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = scroll.firstVisibleItemIndex >= i
                                    ) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                            if (selectedStop >= allStops.size) {
                                item { HorizontalDivider() }
                            }
                        }
                        floatingActions?.invoke(this, scroll)
                    }
                }
                if (sheetType.show) {
                    ListStopsBottomSheet(
                        instance = instance,
                        listRoute = listRoute,
                        routeBranches = routeBranches,
                        selectedStopState = selectedStopState,
                        selectedBranchState = selectedBranchState,
                        allStops = allStops,
                        routeNumber = routeNumber,
                        co = co,
                        isKmbCtbJoint = isKmbCtbJoint,
                        sheetTypeState = sheetTypeState,
                        togglingAlightReminderState = togglingAlightReminderState,
                        sheetState = sheetState
                    )
                }
            }
        )
    }
}

@Composable
fun RouteBranchBar(
    instance: AppActiveContext,
    type: ListStopsInterfaceType,
    co: Operator,
    routeBranches: ImmutableList<Route>,
    selectedBranchState: MutableState<Route>,
) {
    var selectedBranch by selectedBranchState
    val now by currentMinuteState.collectAsStateMultiplatform()
    val branchStatus by remember { derivedStateOf { routeBranches.currentBranchStatus(now, instance) } }
    if (!co.isTrain && !co.isFerry && routeBranches.isNotEmpty() && type.showBranches) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        val dropdownIconDegree by animateFloatAsState(
            targetValue = if (dropdownExpanded) 180F else 0F,
            animationSpec = tween(300)
        )
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .platformHorizontalDividerShadow(5.dp)
                .background(platformPrimaryContainerColor)
                .animateContentSize()
                .zIndex(10F),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(platformPrimaryContainerColor)
                    .combinedClickable(
                        role = Role.DropdownList,
                        onClick = {
                            if (type.canChangeBranch && routeBranches.size > 1) {
                                dropdownExpanded = !dropdownExpanded && type.canChangeBranch
                            } else {
                                instance.showToastText(branchStatus[selectedBranch].description[Shared.language], ToastDuration.SHORT)
                            }
                        },
                        onLongClick = {
                            instance.showToastText(branchStatus[selectedBranch].description[Shared.language], ToastDuration.SHORT)
                        }
                    )
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start)
            ) {
                branchStatus[selectedBranch].let {
                    if (it != RouteBranchStatus.NO_TIMETABLE) {
                        Box(
                            modifier = Modifier
                                .size(width = 20.fontScaledDp, height = 29.fontScaledDp)
                                .align(Alignment.Top),
                            contentAlignment = Alignment.Center
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(14.fontScaledDp)
                                    .background(it.indicatorColor)
                                    .border(1.dp, LocalContentColor.current, CircleShape)
                            )
                        }
                    }
                }
                AutoResizeText(
                    modifier = Modifier
                        .weight(1F)
                        .userMarquee(),
                    overflow = TextOverflow.Ellipsis,
                    fontSizeRange = FontSizeRange(max = 19.sp),
                    maxLines = 2,
                    lineHeight = 1.1F.em,
                    text = selectedBranch.resolveSpecialRemark(instance, RemarkType.LABEL_ALL)[Shared.language]
                )
                if (routeBranches.size > 1 && type.canChangeBranch) {
                    PlatformIcon(
                        modifier = Modifier
                            .rotate(dropdownIconDegree)
                            .size(29.fontScaledDp)
                            .align(Alignment.Top),
                        painter = PlatformIcons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                }
            }
            if (dropdownExpanded && type.canChangeBranch) {
                val dropdownScroll = rememberScrollState()
                with(LocalDensity.current) {
                    if (dropdownScroll.value > DividerDefaults.Thickness.toPx()) {
                        HorizontalDivider()
                    }
                }
                Column(
                    modifier = Modifier
                        .verticalScrollWithScrollbar(
                            state = dropdownScroll,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            scrollbarConfig = ScrollBarConfig(
                                indicatorThickness = 4.dp
                            )
                        )
                ) {
                    for (branch in routeBranches) {
                        if (branch != selectedBranch) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(platformPrimaryContainerColor)
                                    .combinedClickable(
                                        role = Role.DropdownList,
                                        onClick = {
                                            dropdownExpanded = !dropdownExpanded
                                            selectedBranch = branch
                                        },
                                        onLongClick = {
                                            instance.showToastText(branchStatus[branch].description[Shared.language], ToastDuration.SHORT)
                                        }
                                    )
                                    .padding(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start)
                            ) {
                                branchStatus[branch].let {
                                    if (it != RouteBranchStatus.NO_TIMETABLE) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 20.fontScaledDp, height = 22.fontScaledDp)
                                                .align(Alignment.Top),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Spacer(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .size(12.fontScaledDp)
                                                    .background(it.indicatorColor)
                                                    .border(1.dp, LocalContentColor.current, CircleShape)
                                            )
                                        }
                                    }
                                }
                                PlatformText(
                                    modifier = Modifier.userMarquee(),
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 16.sp,
                                    text = branch.resolveSpecialRemark(instance, RemarkType.LABEL_ALL)[Shared.language]
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListStopsBottomSheet(
    instance: AppActiveContext,
    listRoute: RouteSearchResultEntry,
    routeBranches: ImmutableList<Route>,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    allStops: ImmutableList<Registry.StopData>,
    routeNumber: String,
    co: Operator,
    isKmbCtbJoint: Boolean,
    sheetTypeState: MutableState<BottomSheetType>,
    togglingAlightReminderState: MutableState<Boolean>,
    sheetState: SheetState
) {
    val alternateStopNamesShowing by Shared.alternateStopNamesShowingState.collectAsStateMultiplatform()

    val selectedBranch by selectedBranchState
    val selectedStop by selectedStopState

    val favouriteStops by Shared.favoriteStops.collectAsStateMultiplatform()
    val favouriteRouteStops by Shared.favoriteRouteStops.collectAsStateMultiplatform()

    val optAlightReminderService by AlightReminderService.currentInstance.collectAsStateMultiplatform()
    val alightReminderService by remember { derivedStateOf { optAlightReminderService.value } }
    val isActiveReminderService by remember { derivedStateOf { alightReminderService?.selectedRoute?.let { routeBranches.contains(it) } == true } }

    val scope = rememberCoroutineScope()
    var sheetType by sheetTypeState

    PlatformModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
                sheetType = BottomSheetType.NONE
            }
        },
        sheetState = sheetState
    ) {
        val haptic = LocalHapticFeedback.current
        val stopData = allStops[selectedStop - 1]
        val favouriteStopAlreadySet by remember { derivedStateOf { favouriteStops.hasStop(stopData.stopId) || favouriteRouteStops.hasStop(stopData.stopId, co, selectedStop, stopData.stop, stopData.route) } }
        val stopName by remember(stopData, alternateStopNamesShowing) { derivedStateOf {
            if (stopData.route.isKmbCtbJoint && alternateStopNamesShowing) {
                Registry.getInstance(instance).findJointAlternateStop(stopData.stopId, stopData.route.routeNumber).stop.name
            } else {
                stopData.stop.name
            }
        } }
        when (sheetType) {
            BottomSheetType.ACTIONS -> {
                Scaffold(
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .platformHorizontalDividerShadow(5.dp)
                                .background(platformPrimaryContainerColor)
                                .padding(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {

                            PlatformText(
                                modifier = Modifier.fillMaxWidth(),
                                text = stopName[Shared.language],
                                fontSize = 25.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    content = { padding ->
                        val actionScroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxWidth()
                                .verticalScrollWithScrollbar(
                                    state = actionScroll,
                                    flingBehavior = ScrollableDefaults.flingBehavior(),
                                    scrollbarConfig = ScrollBarConfig(
                                        indicatorThickness = 4.dp
                                    )
                                ),
                        ) {
                            stopData.stop.kmbBbiId?.let {
                                ActionRow(
                                    onClick = instance.handleWebImages("https://app.kmb.hk/app1933/BBI/map/$it.jpg", false, haptic.common),
                                    icon = PlatformIcons.Outlined.TransferWithinAStation,
                                    text = (if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖").asAnnotatedString()
                                )
                            }
                            if (co == Operator.MTR) {
                                ActionRow(
                                    onClick = instance.handleWebpages(stopData.stopId.getMTRStationLayoutUrl(), false, haptic.common),
                                    icon = PlatformIcons.Outlined.Train,
                                    text = (if (Shared.language == "en") "Open MTR Station Layout Map" else "顯示港鐵站位置圖").asAnnotatedString()
                                )
                                ActionRow(
                                    onClick = instance.handleWebpages(stopData.stopId.getMTRStationStreetMapUrl(), false, haptic.common),
                                    icon = PlatformIcons.Outlined.Streetview,
                                    text = (if (Shared.language == "en") "Open MTR Station Street Map" else "顯示港鐵站街道圖").asAnnotatedString()
                                )
                            }
                            ActionRow(
                                onClick = {
                                    scope.launch {
                                        sheetState.hide()
                                        sheetType = BottomSheetType.NEARBY
                                    }
                                },
                                icon = PlatformIcons.Outlined.SyncAlt,
                                text = (if (Shared.language == "en") "Find Nearby Interchanges" else "尋找附近轉乘路線").asAnnotatedString()
                            )
                            ActionRow(
                                onClick = instance.handleOpenMaps(stopData.stop.location.lat, stopData.stop.location.lng, stopName[Shared.language], false, haptic.common),
                                icon = PlatformIcons.Outlined.Map,
                                text = (if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置").asAnnotatedString()
                            )
                            if (composePlatform.hasBackgroundLocation) {
                                ActionRow(
                                    onClick = {
                                        scope.launch {
                                            sheetState.hide()
                                            sheetType = BottomSheetType.NONE
                                        }
                                        handleToggleAlightReminder(
                                            instance = instance,
                                            isActiveReminderService = isActiveReminderService,
                                            co = co,
                                            stopData = stopData,
                                            allStops = allStops,
                                            selectedBranch = selectedBranch,
                                            selectedStop = selectedStop,
                                            togglingAlightReminderState = togglingAlightReminderState
                                        )
                                    },
                                    icon = if (isActiveReminderService) PlatformIcons.Outlined.NotificationsOff else PlatformIcons.Outlined.NotificationsActive,
                                    text = if (isActiveReminderService) {
                                        (if (Shared.language == "en") "Disable Alight Reminder" else "關閉落車提示").asAnnotatedString()
                                    } else {
                                        (if (Shared.language == "en") "Enable Alight Reminder" else "開啟落車提示").asAnnotatedString()
                                    }
                                )
                            }
                            ActionRow(
                                onClick = {
                                    scope.launch {
                                        sheetState.hide()
                                        sheetType = BottomSheetType.FAV
                                    }
                                },
                                icon = if (favouriteStopAlreadySet) PlatformIcons.Outlined.Star else PlatformIcons.Outlined.StarOutline,
                                text = (if (Shared.language == "en") "Add to Favourites" else "設置最喜愛路線/巴士站").asAnnotatedString()
                            )
                            val pinnedItems by Shared.pinnedItems.collectAsStateMultiplatform()
                            val pinItem by remember(allStops) { derivedStateOf {
                                val stopBranches = allStops[selectedStop - 1].branchIds
                                val branch = if (stopBranches.contains(selectedBranch)) selectedBranch else stopBranches.first()
                                TemporaryPinItem(branch, co, selectedStop, routeBranches)
                            } }
                            ActionRow(
                                onClick = {
                                    scope.launch {
                                        Shared.togglePinnedItems(pinItem)
                                        sheetState.hide()
                                        sheetType = BottomSheetType.NONE
                                    }
                                },
                                icon = if (pinnedItems.any { it.key == pinItem.key }) PlatformIcons.Filled.PushPin else PlatformIcons.Outlined.PushPin,
                                text = (if (Shared.language == "en") "Pin to Top" else "置頂").asAnnotatedString()
                            )
                            ActionRow(
                                onClick = {
                                    instance.compose.shareUrl(
                                        url = selectedBranch.getDeepLink(instance, allStops[selectedStop - 1].stopId, selectedStop - 1),
                                        title = "${if (Shared.language == "en") "Share Route" else "分享路線"} - ${co.getDisplayName(
                                            routeNumber,
                                            isKmbCtbJoint,
                                            null,
                                            Shared.language
                                        )} ${co.getDisplayRouteNumber(routeNumber)} ${selectedBranch.resolvedDest(true)[Shared.language]} ${if (!co.isTrain) "${selectedStop}." else ""} ${allStops[selectedStop - 1].stop.name[Shared.language]}"
                                    )
                                    scope.launch {
                                        sheetState.hide()
                                        sheetType = BottomSheetType.NONE
                                    }
                                },
                                icon = PlatformIcons.Outlined.Share,
                                text = (if (Shared.language == "en") "Share Route" else "分享路線").asAnnotatedString()
                            )
                            if (composePlatform.mayHaveWatch) {
                                val watchStatus by rememberWearableConnected(instance)
                                ActionRow(
                                    onClick = {
                                        scope.launch {
                                            sheetState.hide()
                                            sheetType = BottomSheetType.NONE
                                        }
                                        sendToWatch(instance, listRoute.routeKey, stopData.stopId, selectedStop)
                                    },
                                    icon = PlatformIcons.Outlined.Watch,
                                    text = (if (Shared.language == "en") "Open on Watch" else "在手錶上開啟").asAnnotatedString(),
                                    subText = when (watchStatus) {
                                        WearableConnectionState.PAIRED -> if (Shared.language == "en") "Watch app must be in the foreground" else "手錶應用程式必須位於前台"
                                        WearableConnectionState.CONNECTED -> if (Shared.language == "en") "Watch App Connected ✓" else "手錶應用程式已連接 ✓"
                                        else -> if (Shared.language == "en") "No watch app paired" else "未有配對手錶應用程式"
                                    }.asAnnotatedString()
                                )
                            }
                            for ((originalStopId, branches) in stopData.mergedStopIds) {
                                for ((stopId, operator) in jointStopId(originalStopId, stopData.route, instance)) {
                                    val operatorText = if (operator == null) "" else " [${operator.getOperatorName(Shared.language)}]"
                                    val subText = if (stopData.mergedStopIds.size <= 1) {
                                        "$stopId$operatorText"
                                    } else {
                                        val branchRemark = branches.asSequence()
                                            .map { it.resolveSpecialRemark(instance, RemarkType.LABEL_MAIN_BRANCH)[Shared.language] }
                                            .joinToString(separator = "/")
                                        "$stopId ($branchRemark)$operatorText"
                                    }
                                    ActionRow(
                                        onClick = {
                                            scope.launch {
                                                val result = copyToClipboard(stopId)
                                                sheetState.hide()
                                                sheetType = BottomSheetType.NONE
                                                instance.showToastText(if (result) {
                                                    if (Shared.language == "en") "Copied to clipboard" else "已複製到剪貼簿"
                                                } else {
                                                    if (Shared.language == "en") "Failed to copy to clipboard" else "無法複製到剪貼簿"
                                                }, ToastDuration.SHORT)
                                            }
                                        },
                                        icon = PlatformIcons.Outlined.Code,
                                        text = (if (Shared.language == "en") "Copy Stop ID" else "複製 Stop ID").asAnnotatedString(),
                                        subText = subText.asAnnotatedString()
                                    )
                                }
                            }
                        }
                    }
                )
            }
            BottomSheetType.FAV -> {
                SetFavouriteInterface(instance, co, selectedStop, stopData)
            }
            BottomSheetType.NEARBY -> {
                val origin by remember { derivedStateOf { allStops[selectedStop - 1].stop.location } }
                var routes: ImmutableList<StopIndexedRouteSearchResultEntry>? by remember(origin) { mutableStateOf(null) }

                LaunchedEffect (origin) {
                    routes = Registry.getInstance(instance).getNearbyRoutes(origin, mapOf(listRoute.co to setOf(listRoute.route!!.routeNumber)), true).result.toStopIndexed(instance).asImmutableList()
                }

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
                                text = "${stopName[Shared.language]}${if (Shared.language == "en") " - Nearby Routes" else " - 附近路線"}",
                                fontSize = 25.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    content = { padding ->
                        Box(
                            modifier = Modifier.padding(padding)
                        ) {
                            ListRoutesInterface(
                                instance = instance,
                                routes = routes?: persistentListOf(),
                                checkSpecialDest = true,
                                listType = RouteListType.NEARBY,
                                showEta = true,
                                recentSort = RecentSortMode.CHOICE,
                                proximitySortOrigin = origin,
                                showEmptyText = routes != null
                            )
                        }
                    }
                )
            }
            else -> { /* do nothing */ }
        }
    }
}

@Composable
fun StopEntry(
    instance: AppActiveContext,
    type: ListStopsInterfaceType,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    possibleBidirectionalSectionFare: Boolean,
    alertCheckRoute: Boolean,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>,
    index: Int,
    allStops: ImmutableList<Registry.StopData>,
    stopData: Registry.StopData,
    mtrStopsInterchange: ImmutableList<Registry.MTRInterchangeData>,
    routeBranches: ImmutableList<Route>,
    routeLineData: ImmutableState<List<Any>>,
    routeNumber: String,
    co: Operator,
    gmbRegion: GMBRegion?,
    isKmbCtbJoint: Boolean,
    timesStartIndexState: MutableIntState,
    times: SnapshotStateMap<Int, Registry.TimeBetweenStopResult>,
    alightReminderHighlightBlinkState: MutableState<Boolean>,
    alightReminderStateState: MutableState<AlightReminderServiceState?>,
    alightReminderTimeLeftState: MutableIntState,
    alightReminderStopsLeftState: MutableIntState,
    lrtDirectionModeState: MutableState<Boolean>,
    etaResultsState: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>,
    etaUpdateTimesState: ImmutableState<out MutableMap<Int, Long>>,
    nextBusPositionState: ImmutableState<out MutableMap<Int, Optional<KeyedNextBusPosition>>>,
    sheetTypeState: MutableState<BottomSheetType>,
    togglingAlightReminderState: MutableState<Boolean>,
    alternateStopNamesShowingState: MutableState<Boolean>,
    trafficSnapshots: List<TrafficSnapshotPoint>?,
    sticky: Boolean
) {
    val selectedBranch by selectedBranchState
    val selectedStop by selectedStopState

    val timesStartIndex by timesStartIndexState

    val expanded = type == ListStopsInterfaceType.TRAFFIC_SNAPSHOTS || ((selectedStop == index || (type == ListStopsInterfaceType.TIMES && timesStartIndex == index)) && (type == ListStopsInterfaceType.ETA || co.isTrain || stopData.branchIds.contains(selectedBranch)))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .applyIf(sticky) { background(platformBackgroundColor) }
    ) {
        StopEntryCard(
            instance = instance,
            type = type,
            selectedStopState = selectedStopState,
            timesStartIndexState = timesStartIndexState,
            selectedBranchState = selectedBranchState,
            alternateStopNamesState = alternateStopNames,
            index = index,
            stopData = stopData,
            stopList = allStops,
            mtrStopsInterchange = mtrStopsInterchange,
            routeLineData = routeLineData,
            routeNumber = routeNumber,
            gmbRegion = gmbRegion,
            co = co,
            alightReminderHighlightBlinkState = alightReminderHighlightBlinkState,
            alternateStopNamesShowingState = alternateStopNamesShowingState
        )
        Box(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessHigh
                )
            )
        ) {
            if (expanded) {
                StopEntryExpansion(
                    instance = instance,
                    type = type,
                    selectedStopState = selectedStopState,
                    selectedBranchState = selectedBranchState,
                    possibleBidirectionalSectionFare = possibleBidirectionalSectionFare,
                    alertCheckRoute = alertCheckRoute,
                    index = index,
                    allStops = allStops,
                    alternateStopNames = alternateStopNames,
                    stopData = stopData,
                    routeBranches = routeBranches,
                    routeLineData = routeLineData,
                    mtrStopsInterchange = mtrStopsInterchange,
                    routeNumber = routeNumber,
                    co = co,
                    isKmbCtbJoint = isKmbCtbJoint,
                    gmbRegion = gmbRegion,
                    timesStartIndexState = timesStartIndexState,
                    times = times,
                    alightReminderHighlightBlinkState = alightReminderHighlightBlinkState,
                    alightReminderStateState = alightReminderStateState,
                    alightReminderTimeLeftState = alightReminderTimeLeftState,
                    alightReminderStopsLeftState = alightReminderStopsLeftState,
                    lrtDirectionModeState = lrtDirectionModeState,
                    etaResultsState = etaResultsState,
                    etaUpdateTimesState = etaUpdateTimesState,
                    nextBusPositionState = nextBusPositionState,
                    sheetTypeState = sheetTypeState,
                    togglingAlightReminderState = togglingAlightReminderState,
                    trafficSnapshots = trafficSnapshots
                )
            }
        }
    }
}

@Composable
fun StopEntryCard(
    instance: AppActiveContext,
    type: ListStopsInterfaceType,
    selectedStopState: MutableIntState,
    timesStartIndexState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    alternateStopNamesState: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>,
    index: Int,
    stopData: Registry.StopData,
    stopList: ImmutableList<Registry.StopData>,
    mtrStopsInterchange: ImmutableList<Registry.MTRInterchangeData>,
    routeLineData: ImmutableState<List<Any>>,
    routeNumber: String,
    co: Operator,
    gmbRegion: GMBRegion?,
    alightReminderHighlightBlinkState: MutableState<Boolean>,
    alternateStopNamesShowingState: MutableState<Boolean>,
) {
    val i = index - 1
    val selectedBranch by selectedBranchState
    var selectedStop by selectedStopState
    var alternateStopNamesShowing by alternateStopNamesShowingState

    val timesStartIndex by timesStartIndexState

    val optAlightReminderService by AlightReminderService.currentInstance.collectAsStateMultiplatform()
    val alightReminderService by remember { derivedStateOf { optAlightReminderService.value } }

    val alightReminderHighlightBlink by alightReminderHighlightBlinkState

    val onSelectedBranch = stopData.branchIds.contains(selectedBranch)
    val highlightType = when (type) {
        ListStopsInterfaceType.ALIGHT_REMINDER -> {
            alightReminderService?.run {
                when {
                    currentStop.index == index -> if (alightReminderHighlightBlink) RouteHighlightType.BRIGHT else RouteHighlightType.NORMAL
                    currentStop.index > index || destinationStopId.index < index -> RouteHighlightType.DIM
                    else -> RouteHighlightType.BRIGHT
                }
            }?: RouteHighlightType.NORMAL
        }
        ListStopsInterfaceType.TIMES -> {
            if ((stopList[selectedStop - 1].branchIds.contains(selectedBranch) && index in timesStartIndex..selectedStop) || index == timesStartIndex) {
                if (index == selectedStop && index != timesStartIndex && alightReminderHighlightBlink) RouteHighlightType.NORMAL else RouteHighlightType.BRIGHT
            } else {
                RouteHighlightType.NORMAL
            }
        }
        else -> RouteHighlightType.NORMAL
    }

    val alternateStopNames = alternateStopNamesState.value

    ElevatedCard(
        modifier = Modifier.clickable {
            if (selectedStop == index && alternateStopNames != null) {
                alternateStopNamesShowing = !alternateStopNamesShowing
                val operatorName = (if (alternateStopNamesShowing) Operator.CTB else Operator.KMB).getDisplayName(routeNumber, false, gmbRegion, Shared.language)
                val text = if (Shared.language == "en") {
                    "Displaying $operatorName stop names"
                } else {
                    "顯示${operatorName}站名"
                }
                instance.showToastText(text, ToastDuration.SHORT)
            } else if (type != ListStopsInterfaceType.ALIGHT_REMINDER) {
                selectedStop = index
            }
        },
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier.height(55.fontScaledDp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (val data = routeLineData.value[i]) {
                is BusRouteLineData -> {
                    Box(
                        modifier = Modifier
                            .requiredWidth(60.dp)
                            .fillMaxHeight()
                    ) {
                        BusLineSection(data, highlightType)
                    }
                }
                is MTRStopSectionData -> {
                    val width by remember(stopList, mtrStopsInterchange) { derivedStateOf { if (stopList.map { it.serviceType }.distinct().size > 1 || mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }) 100 else 70 } }
                    Box(
                        modifier = Modifier
                            .requiredWidth(width.dp)
                            .fillMaxHeight()
                            .clickable {
                                if (HistoryStack.historyStack.value.takeIf { i -> i.size > 1 }?.let { i -> i[i.lastIndex - 1] }?.screen == AppScreen.SEARCH_TRAIN) {
                                    instance.finish()
                                } else {
                                    val intent = AppIntent(instance, AppScreen.SEARCH_TRAIN)
                                    intent.putExtra("stopLaunch", stopData.stopId)
                                    instance.startActivity(intent)
                                }
                            }
                            .padding(start = 10.dp)
                    ) {
                        MTRLineSection(data, highlightType)
                    }
                }
            }
            Row(
                modifier = Modifier.weight(1F),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var lineCount by remember { mutableIntStateOf(1) }
                if (!co.isTrain) {
                    PlatformText(
                        modifier = Modifier
                            .width(35.fontScaledDp)
                            .applyIf(lineCount <= 1) { alignByBaseline() },
                        text = "${index}.",
                        fontSize = 19.sp,
                        lineHeight = 1.1F.em,
                        color = platformLocalContentColor.adjustAlpha(if (onSelectedBranch) 1F else 0.5F)
                    )
                }
                AutoResizeText(
                    modifier = Modifier
                        .weight(1F, false)
                        .padding(end = 5.dp)
                        .alignByBaseline(),
                    onTextLayout = { lineCount = it.lineCount },
                    text = if (alternateStopNamesShowing && alternateStopNames != null) {
                        alternateStopNames[index - 1].stop
                    } else {
                        stopData.stop
                    }.remarkedName[Shared.language].asContentAnnotatedString().annotatedString,
                    fontSizeRange = FontSizeRange(max = 19.sp),
                    lineHeight = 1.1F.em,
                    color = platformLocalContentColor.adjustAlpha(if (onSelectedBranch || co.isTrain) 1F else 0.5F)
                )
            }
            when (type) {
                ListStopsInterfaceType.ALIGHT_REMINDER -> {
                    val service = alightReminderService
                    when {
                        service == null -> { /* do nothing */ }
                        service.currentStop.index == index -> {
                            PlatformIcon(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(end = 5.dp),
                                tint = Color(0xff199fff),
                                painter = PlatformIcons.Outlined.LocationOn,
                                contentDescription = if (Shared.language == "en") "Current Stop" else "本站"
                            )
                        }
                        service.destinationStopId.index == index -> {
                            PlatformIcon(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(end = 5.dp),
                                tint = Color.Red,
                                painter = PlatformIcons.Outlined.EmojiFlags,
                                contentDescription = if (Shared.language == "en") "Destination" else "目的地"
                            )
                        }
                        service.currentStop.index < index && service.destinationStopId.index >= index -> {
                            PlatformIcon(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(end = 5.dp),
                                painter = PlatformIcons.Outlined.MoreVert,
                                contentDescription = if (Shared.language == "en") "Remaining Stops" else "剩餘中途站"
                            )
                        }
                        else -> { /* do nothing*/ }
                    }
                }
                else -> { /* do nothing */ }
            }
        }
        HorizontalDivider()
    }
}

@Composable
fun StopEntryExpansion(
    instance: AppActiveContext,
    type: ListStopsInterfaceType,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    possibleBidirectionalSectionFare: Boolean,
    alertCheckRoute: Boolean,
    index: Int,
    allStops: ImmutableList<Registry.StopData>,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>,
    stopData: Registry.StopData,
    routeBranches: ImmutableList<Route>,
    routeLineData: ImmutableState<List<Any>>,
    mtrStopsInterchange: ImmutableList<Registry.MTRInterchangeData>,
    routeNumber: String,
    co: Operator,
    isKmbCtbJoint: Boolean,
    gmbRegion: GMBRegion?,
    timesStartIndexState: MutableIntState,
    times: SnapshotStateMap<Int, Registry.TimeBetweenStopResult>,
    alightReminderHighlightBlinkState: MutableState<Boolean>,
    alightReminderStateState: MutableState<AlightReminderServiceState?>,
    alightReminderTimeLeftState: MutableIntState,
    alightReminderStopsLeftState: MutableIntState,
    lrtDirectionModeState: MutableState<Boolean>,
    etaResultsState: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>,
    etaUpdateTimesState: ImmutableState<out MutableMap<Int, Long>>,
    nextBusPositionState: ImmutableState<out MutableMap<Int, Optional<KeyedNextBusPosition>>>,
    sheetTypeState: MutableState<BottomSheetType>,
    togglingAlightReminderState: MutableState<Boolean>,
    trafficSnapshots: List<TrafficSnapshotPoint>?
) {
    val i = index - 1
    val density = LocalDensity.current
    val window = currentLocalWindowSize
    var height by remember(density.density, density.fontScale, window) { mutableIntStateOf(0) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { height = it.height },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (val data = routeLineData.value[i]) {
            is BusRouteLineData -> {
                Box(
                    modifier = Modifier
                        .requiredSize(60.dp, height.equivalentDp)
                ) {
                    BusLineSectionExtension(data)
                }
            }
            is MTRStopSectionData -> {
                val width by remember(allStops, mtrStopsInterchange) { derivedStateOf { if (allStops.map { it.serviceType }.distinct().size > 1 || mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }) 100 else 70 } }
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .requiredSize((width - 10).dp, height.equivalentDp)
                ) {
                    MTRLineSectionExtension(data)
                }
            }
        }
        when (type) {
            ListStopsInterfaceType.ETA -> StopEntryExpansionEta(
                instance = instance,
                selectedStopState = selectedStopState,
                selectedBranchState = selectedBranchState,
                possibleBidirectionalSectionFare = possibleBidirectionalSectionFare,
                alertCheckRoute = alertCheckRoute,
                index = index,
                allStops = allStops,
                alternateStopNames = alternateStopNames,
                stopData = stopData,
                routeBranches = routeBranches,
                routeNumber = routeNumber,
                co = co,
                isKmbCtbJoint = isKmbCtbJoint,
                gmbRegion = gmbRegion,
                lrtDirectionModeState = lrtDirectionModeState,
                etaResultsState = etaResultsState,
                etaUpdateTimesState = etaUpdateTimesState,
                nextBusPositionState = nextBusPositionState,
                sheetTypeState = sheetTypeState,
                togglingAlightReminderState = togglingAlightReminderState
            )
            ListStopsInterfaceType.TIMES -> StopEntryExpansionTimes(
                index = index,
                timesStartIndexState = timesStartIndexState,
                times = times[index]
            )
            ListStopsInterfaceType.TRAFFIC_SNAPSHOTS -> StopEntryExpansionTrafficSnapshots(
                instance = instance,
                trafficSnapshots = trafficSnapshots
            )
            ListStopsInterfaceType.ALIGHT_REMINDER -> StopEntryExpansionAlightReminder(
                instance = instance,
                selectedStopState = selectedStopState,
                selectedBranchState = selectedBranchState,
                allStops = allStops,
                stopData = stopData,
                routeBranches = routeBranches,
                routeNumber = routeNumber,
                co = co,
                isKmbCtbJoint = isKmbCtbJoint,
                gmbRegion = gmbRegion,
                sheetTypeState = sheetTypeState,
                togglingAlightReminderState = togglingAlightReminderState,
                alightReminderHighlightBlinkState = alightReminderHighlightBlinkState,
                alightReminderStateState = alightReminderStateState,
                alightReminderTimeLeftState = alightReminderTimeLeftState,
                alightReminderStopsLeftState = alightReminderStopsLeftState,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopEntryExpansionEta(
    instance: AppActiveContext,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    possibleBidirectionalSectionFare: Boolean,
    alertCheckRoute: Boolean,
    index: Int,
    allStops: ImmutableList<Registry.StopData>,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>,
    stopData: Registry.StopData,
    routeBranches: ImmutableList<Route>,
    routeNumber: String,
    co: Operator,
    isKmbCtbJoint: Boolean,
    gmbRegion: GMBRegion?,
    lrtDirectionModeState: MutableState<Boolean>,
    etaResultsState: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>,
    etaUpdateTimesState: ImmutableState<out MutableMap<Int, Long>>,
    nextBusPositionState: ImmutableState<out MutableMap<Int, Optional<KeyedNextBusPosition>>>,
    sheetTypeState: MutableState<BottomSheetType>,
    togglingAlightReminderState: MutableState<Boolean>
) {
    val selectedBranch by selectedBranchState
    val selectedStop by selectedStopState

    val favouriteStops by Shared.favoriteStops.collectAsStateMultiplatform()
    val favouriteRouteStops by Shared.favoriteRouteStops.collectAsStateMultiplatform()

    val optAlightReminderService by AlightReminderService.currentInstance.collectAsStateMultiplatform()
    val alightReminderService by remember { derivedStateOf { optAlightReminderService.value } }
    val isActiveReminderService by remember { derivedStateOf { alightReminderService?.selectedRoute?.let { routeBranches.contains(it) } == true } }

    val togglingAlightReminder by togglingAlightReminderState

    var lrtDirectionMode by lrtDirectionModeState
    val etaQueryOptions by remember { derivedStateOf { Registry.EtaQueryOptions(lrtDirectionMode) } }

    val favouriteStopAlreadySet by remember { derivedStateOf { favouriteStops.hasStop(stopData.stopId) || favouriteRouteStops.hasStop(stopData.stopId, co, selectedStop, stopData.stop, stopData.route) } }

    val etaUpdateTimes = etaUpdateTimesState.value
    val nextBusPositions = nextBusPositionState.value

    val alternateStopNameShowing by Shared.alternateStopNamesShowingState.collectAsStateMultiplatform()

    var keyedNextBusPosition: KeyedNextBusPosition? by remember(selectedStop, selectedBranch) { mutableStateOf(null) }

    var sheetType by sheetTypeState

    val pipMode = rememberIsInPipMode(instance)

    LaunchedEffect (Unit) {
        val previous = nextBusPositions[selectedStop]?.value
        if (previous?.branch == selectedBranch && previous.etaQueryOptions == etaQueryOptions) {
            val waitTime = etaUpdateTimes[index]?.let { (Shared.ETA_UPDATE_INTERVAL - (currentTimeMillis() - it)).coerceAtLeast(0) }?: 0
            if (waitTime > 1000) {
                keyedNextBusPosition = previous
            }
            etaUpdateTimes[index]?.apply {
                delay(waitTime)
            }
        }
        while (true) {
            val nextBusPosition = if (co.isBus && stopData.branchIds.contains(selectedBranch)) {
                Registry.getInstance(instance).findNextBusPosition(stopData.stopId, index, co, selectedBranch, allStops, instance, etaQueryOptions).query(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
            } else {
                null
            }
            val keyed = nextBusPosition?.let { KeyedNextBusPosition(it, selectedBranch, etaQueryOptions) }
            keyedNextBusPosition = keyed
            nextBusPositions[selectedStop] = Optional(keyed)
            delay(Shared.ETA_UPDATE_INTERVAL.toLong())
        }
    }
    LaunchedEffect (selectedBranch, etaQueryOptions) {
        val nextBusPosition = if (co.isBus && stopData.branchIds.contains(selectedBranch)) {
            Registry.getInstance(instance).findNextBusPosition(stopData.stopId, index, co, selectedBranch, allStops, instance, etaQueryOptions).query(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
        } else {
            null
        }
        val keyed = nextBusPosition?.let { KeyedNextBusPosition(it, selectedBranch, etaQueryOptions) }
        keyedNextBusPosition = keyed
        nextBusPositions[selectedStop] = Optional(keyed)
    }

    Column(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        var height by remember { mutableIntStateOf(0) }
        Column(
            modifier = Modifier
                .animateContentSize()
                .requiredHeightIn(min = height.equivalentDp)
                .onSizeChanged { height = it.height },
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stopData.fare != null) {
                    if (co.isFerry) {
                        PlatformText(
                            fontSize = 14.sp,
                            lineHeight = 1.3F.em,
                            text = "${if (Shared.language == "en") "Fare: " else "票價: "} $${stopData.fare}"
                        )
                    } else {
                        PlatformText(
                            fontSize = 14.sp,
                            lineHeight = 1.3F.em,
                            text = "${if (Shared.language == "en") "Fare: " else "車費: "} $${stopData.fare}"
                        )
                    }
                }
                if (stopData.holidayFare != null) {
                    PlatformText(
                        fontSize = 14.sp,
                        lineHeight = 1.3F.em,
                        text = "${if (Shared.language == "en") "Holiday Fare: " else "假日車費: "} $${stopData.holidayFare}"
                    )
                }
            }
            val alerts by remember(possibleBidirectionalSectionFare, alertCheckRoute) { derivedStateOf { buildList {
                if (possibleBidirectionalSectionFare) {
                    add((if (Shared.language == "en") "Possible Two-way Section Fare" else "留意雙向分段收費").asAnnotatedString())
                }
                if (alertCheckRoute) {
                    add((if (Shared.language == "en") "Check Actual Route on Bus" else "留意巴士實際路線").asAnnotatedString(SpanStyle(fontWeight = FontWeight.Bold)))
                }
                if (co.isFerry) {
                    add((if (Shared.language == "en") "Check Actual Fare on Ferry" else "檢查渡輪實際票價").asAnnotatedString(SpanStyle(fontWeight = FontWeight.Bold)))
                }
            } } }
            if (alerts.isNotEmpty()) {
                PlatformText(
                    fontSize = 14.sp,
                    lineHeight = 1.3F.em,
                    text = alerts.joinToAnnotatedString("\n".asAnnotatedString())
                )
            }
            if (co.isBus) {
                val nextBusText = keyedNextBusPosition?.position.getDisplayText(
                    allStops = allStops,
                    alternateStopNames = alternateStopNames.value,
                    alternateStopNamesShowing = alternateStopNameShowing && isKmbCtbJoint,
                    mode = NextBusTextDisplayMode.FULL,
                    context = instance,
                    language = Shared.language
                )
                if (nextBusText != null) {
                    PlatformText(
                        modifier = Modifier.padding(top = 5.dp, end = 10.dp),
                        fontSize = 14.sp,
                        lineHeight = 1.3F.em,
                        text = nextBusText.asContentAnnotatedString().annotatedString
                    )
                }
            }
        }
        if (!pipMode) {
            ETAColumn(
                modifier = Modifier
                    .height(100.fontScaledDp(0.5F))
                    .padding(end = 30.dp),
                index = index,
                co = co,
                stopData = stopData,
                etaResults = etaResultsState,
                etaUpdateTimes = etaUpdateTimesState,
                options = etaQueryOptions,
                fontSize = 16F.sp,
                lineRange = 1..4,
                dynamicLineRange = false,
                instance = instance
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
        ) {
            val haptic = LocalHapticFeedback.current
            if (co === Operator.LRT) {
                PlatformFilledTonalIconButton(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .size(42.dp, 32.dp)
                        .plainTooltip(if (Shared.language == "en") "Light Rail Display Mode" else "輕鐵路線顯示格式"),
                    onClick = {
                        Registry.getInstance(instance).setLrtDirectionMode(!lrtDirectionMode, instance)
                        lrtDirectionMode = Shared.lrtDirectionMode
                        instance.showToastText(if (lrtDirectionMode) {
                            if (Shared.language == "en") "Display all Light Rail routes in the same direction" else "顯示所有相同方向輕鐵路線"
                        } else {
                            if (Shared.language == "en") "Display only the select Light Rail route" else "只顯示該輕鐵路線"
                        }, ToastDuration.SHORT)
                    },
                    shape = RoundedCornerShape(5.dp)
                ) {
                    PlatformIcon(
                        modifier = Modifier.size(25.dp),
                        painter = PlatformIcons.Outlined.SwipeRightAlt,
                        tint = Operator.LRT.getOperatorColor(Color.White).adjustBrightness(if (lrtDirectionMode) 1F else 0.4F),
                        contentDescription = if (Shared.language == "en") "Light Rail Display Mode" else "輕鐵路線顯示格式"
                    )
                }
            }
            stopData.stop.kmbBbiId?.let {
                PlatformFilledTonalIconButton(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .size(42.dp, 32.dp)
                        .plainTooltip(if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"),
                    onClick = instance.handleWebImages("https://app.kmb.hk/app1933/BBI/map/$it.jpg", false, haptic.common),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    PlatformIcon(
                        modifier = Modifier.size(25.dp),
                        painter = PlatformIcons.Outlined.TransferWithinAStation,
                        tint = Color(0xFFC4AB48),
                        contentDescription = if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"
                    )
                }
            }
            PlatformFilledTonalIconButton(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(42.dp, 32.dp)
                    .plainTooltip(if (Shared.language == "en") "Nearby Routes" else "附近路線"),
                onClick = { sheetType = BottomSheetType.NEARBY },
                shape = RoundedCornerShape(5.dp)
            ) {
                PlatformIcon(
                    modifier = Modifier.size(25.dp),
                    painter = PlatformIcons.Outlined.SyncAlt,
                    tint = Color(0xFFC4AB48),
                    contentDescription = if (Shared.language == "en") "Nearby Routes" else "附近路線"
                )
            }
            if (composePlatform.hasBackgroundLocation) {
                PlatformFilledTonalIconButton(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .size(42.dp, 32.dp)
                        .plainTooltip(if (Shared.language == "en") "Toggle Alight Reminder" else "開啟/關閉落車提示"),
                    enabled = !togglingAlightReminder,
                    onClick = { handleToggleAlightReminder(
                        instance = instance,
                        isActiveReminderService = isActiveReminderService,
                        co = co,
                        stopData = stopData,
                        allStops = allStops,
                        selectedBranch = selectedBranch,
                        selectedStop = selectedStop,
                        togglingAlightReminderState = togglingAlightReminderState
                    ) },
                    shape = RoundedCornerShape(5.dp)
                ) {
                    PlatformIcon(
                        modifier = Modifier.size(25.dp),
                        painter = if (isActiveReminderService) PlatformIcons.Outlined.NotificationsOff else PlatformIcons.Outlined.NotificationsActive,
                        tint = Color(0xFFC4AB48),
                        contentDescription = if (Shared.language == "en") "Toggle Alight Reminder" else "開啟/關閉落車提示"
                    )
                }
            }
            PlatformFilledTonalIconButton(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(42.dp, 32.dp)
                    .plainTooltip(if (Shared.language == "en") "Add to Favourites" else "設置最喜愛路線/巴士站"),
                onClick = { sheetType = BottomSheetType.FAV },
                shape = RoundedCornerShape(5.dp)
            ) {
                PlatformIcon(
                    modifier = Modifier.size(25.dp),
                    painter = if (favouriteStopAlreadySet) PlatformIcons.Outlined.Star else PlatformIcons.Outlined.StarOutline,
                    tint = Color(0xFFC4AB48),
                    contentDescription = if (Shared.language == "en") "Add to Favourites" else "設置最喜愛路線/巴士站"
                )
            }
            if (co !== Operator.LRT) {
                val pinnedItems by Shared.pinnedItems.collectAsStateMultiplatform()
                val pinItem by remember(allStops) { derivedStateOf {
                    val pinnedStopData = allStops[selectedStop - 1]
                    val stopBranches = pinnedStopData.branchIds
                    val branch = if (stopBranches.contains(selectedBranch)) selectedBranch else (stopBranches.firstOrNull()?: pinnedStopData.route)
                    TemporaryPinItem(branch, co, selectedStop, routeBranches)
                } }
                PlatformFilledTonalIconButton(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .size(42.dp, 32.dp)
                        .plainTooltip(if (Shared.language == "en") "Pin to Top" else "置頂"),
                    onClick = { Shared.togglePinnedItems(pinItem) },
                    shape = RoundedCornerShape(5.dp)
                ) {
                    PlatformIcon(
                        modifier = Modifier.size(25.dp),
                        painter = if (pinnedItems.any { it.key == pinItem.key }) PlatformIcons.Filled.PushPin else PlatformIcons.Outlined.PushPin,
                        tint = Color(0xFFC4AB48),
                        contentDescription = if (Shared.language == "en") "Pin to Top" else "置頂"
                    )
                }
            }
            when {
                RouteStopETALiveActivity.isSupported() -> {
                    val selected = RouteStopETASelectedRouteStop(
                        stopId = stopData.stopId,
                        stopIndex = index,
                        co = co,
                        stopRoute = stopData.route,
                        stopBranches = stopData.branchIds,
                        allBranches = routeBranches.toList(),
                        context = instance,
                        etaQueryOptions = etaQueryOptions
                    )
                    var isCurrentlySelected by remember { mutableStateOf(RouteStopETALiveActivity.isCurrentSelectedStop(selected)) }
                    PlatformFilledTonalIconButton(
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(42.dp, 32.dp)
                            .plainTooltip(if (Shared.language == "en") "Dynamic Island Display Mode" else "動態島顯示模式"),
                        onClick = {
                            if (isCurrentlySelected) {
                                RouteStopETALiveActivity.clearCurrentSelectedStop()
                            } else {
                                RouteStopETALiveActivity.setCurrentSelectedStop(selected)
                            }
                            isCurrentlySelected = RouteStopETALiveActivity.isCurrentSelectedStop(selected)
                            instance.showToastText(if (isCurrentlySelected) {
                                if (Shared.language == "en") "Enabled Dynamic Island Display Mode" else "已啟用動態島顯示模式"
                            } else {
                                if (Shared.language == "en") "Disable Dynamic Island Display Mode" else "已停用動態島顯示模式"
                            }, ToastDuration.SHORT)
                        },
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        PlatformIcon(
                            modifier = Modifier.size(25.dp),
                            painter = if (isCurrentlySelected) PlatformIcons.Outlined.Widget else PlatformIcons.Outlined.WidgetAdd,
                            tint = if (isCurrentlySelected) Color(0xFFFF0000) else Color(0xFFC4AB48),
                            contentDescription = if (Shared.language == "en") "Dynamic Island Display Mode" else "動態島顯示模式"
                        )
                    }
                }
                composePlatform.supportPip -> {
                    PlatformFilledTonalIconButton(
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(42.dp, 32.dp)
                            .plainTooltip(if (Shared.language == "en") "Picture-in-picture Display Mode" else "畫中畫顯示模式"),
                        onClick = { instance.enterPipMode() },
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        PlatformIcon(
                            modifier = Modifier.size(25.dp),
                            painter = PlatformIcons.Outlined.Fullscreen,
                            tint = Color(0xFFC4AB48),
                            contentDescription = if (Shared.language == "en") "Picture-in-picture Display Mode" else "畫中畫顯示模式"
                        )
                    }
                }
                else -> {
                    PlatformFilledTonalIconButton(
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(42.dp, 32.dp)
                            .plainTooltip(if (Shared.language == "en") "Share Route" else "分享路線"),
                        onClick = { instance.compose.shareUrl(
                            url = selectedBranch.getDeepLink(instance, allStops[selectedStop - 1].stopId, selectedStop - 1),
                            title = "${if (Shared.language == "en") "Share Route" else "分享路線"} - ${co.getDisplayName(routeNumber, isKmbCtbJoint, gmbRegion, Shared.language)} ${co.getDisplayRouteNumber(routeNumber)} ${selectedBranch.resolvedDest(true)[Shared.language]} ${if (!co.isTrain) "${selectedStop}." else ""} ${allStops[selectedStop - 1].stop.name[Shared.language]}"
                        ) },
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        PlatformIcon(
                            modifier = Modifier.size(25.dp),
                            painter = PlatformIcons.Outlined.Share,
                            tint = Color(0xFFC4AB48),
                            contentDescription = if (Shared.language == "en") "Share Route" else "分享路線"
                        )
                    }
                }
            }
            PlatformFilledTonalIconButton(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(42.dp, 32.dp)
                    .plainTooltip(if (Shared.language == "en") "More" else "更多"),
                onClick = { sheetType = BottomSheetType.ACTIONS },
                shape = RoundedCornerShape(5.dp)
            ) {
                PlatformIcon(
                    modifier = Modifier.size(25.dp),
                    painter = PlatformIcons.Outlined.MoreHoriz,
                    tint = Color(0xFFC4AB48),
                    contentDescription = if (Shared.language == "en") "More" else "更多"
                )
            }
        }
    }
}

@Composable
fun StopEntryExpansionTimes(
    index: Int,
    timesStartIndexState: MutableIntState,
    times: Registry.TimeBetweenStopResult?
) {
    var timesStartIndex by timesStartIndexState

    Column(
        modifier = Modifier.padding(vertical = 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when {
            index > timesStartIndex -> {
                val timesText = times
                    ?.takeIf { it.averageInterval >= 0 }
                    ?.let {
                        if (it.isLoading) {
                            if (Shared.language == "en") "Journey Times: Loading..." else "車程: 載入中..."
                        } else {
                            buildString {
                                val interval = (it.averageInterval / 60).coerceAtLeast(1)
                                append(if (Shared.language == "en") "Journey Times: $interval Minutes (Ref. Only)" else "車程: ${interval}分鐘 (只供參考)")
                                it.currentHourlyInterval?.let { hourTime ->
                                    val intervalHour = (hourTime / 60).coerceAtLeast(1)
                                    append("\n")
                                    append(if (Shared.language == "en") "Average at this hour: $intervalHour Minutes (Experimental)" else "本小時平均: ${intervalHour}分鐘 (試驗版)")
                                }
                            }
                        }
                    }
                    ?: if (Shared.language == "en") "Unable to provide Journey Times" else "未能提供車程"
                PlatformText(
                    modifier = Modifier.fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    fontSize = 16.sp,
                    lineHeight = 1.1F.em,
                    text = timesText
                )
            }
            index == timesStartIndex -> {
                PlatformText(
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    fontSize = 16.sp,
                    lineHeight = 1.1F.em,
                    text = if (Shared.language == "en") "Set as Origin" else "已設定為起點"
                )
            }
            else -> {
                PlatformText(
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    fontSize = 16.sp,
                    lineHeight = 1.1F.em,
                    text = if (Shared.language == "en") "Origin is set at a later stop" else "起點在後面"
                )
            }
        }
        PlatformButton(
            onClick = {
                timesStartIndex = index
            },
            modifier = Modifier
                .size(200.fontScaledDp, 40.fontScaledDp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (timesStartIndex != index) Color(0xff2b87ff) else Color(0xff919191)
            ),
            enabled = timesStartIndex != index,
            shape = platformLargeShape,
            content = {
                PlatformText(
                    maxLines = 1,
                    fontSize = 16.sp,
                    lineHeight = 1.1F.em,
                    text = if (timesStartIndex == index) {
                        if (Shared.language == "en") "Set as Origin" else "已設定為起點"
                    } else {
                        if (Shared.language == "en") "Set Origin" else "設定為起點"
                    }
                )
            }
        )
    }
}

@Composable
fun StopEntryExpansionTrafficSnapshots(
    instance: AppActiveContext,
    trafficSnapshots: List<TrafficSnapshotPoint>?
) {
    if (trafficSnapshots.isNotNullAndNotEmpty()) {
        FlowRow(
            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val haptics = LocalHapticFeedback.current
            for ((key, name) in trafficSnapshots) {
                var expanded by rememberSaveable { mutableStateOf(false) }
                OutlinedCard(
                    modifier = Modifier
                        .width(400.dp)
                        .padding(5.dp)
                        .clip(platformLargeShape)
                        .clickable { expanded = true },
                    shape = platformLargeShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlatformIcon(
                            modifier = Modifier.size(20.dp),
                            painter = PlatformIcons.Filled.PhotoCamera,
                            contentDescription = if (Shared.language == "en") "Traffic Snapshot" else "交通快拍"
                        )
                        PlatformText(
                            fontSize = 16.sp,
                            lineHeight = 1.1F.em,
                            text = name[Shared.language]
                        )
                    }
                    Box(
                        modifier = Modifier.animateContentSize(
                            animationSpec = spring(
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    ) {
                        if (expanded) {
                            val url = "https://tdcctv.data.one.gov.hk/$key.JPG?_=${currentEpochSeconds / 300}"
                            SubcomposeAsyncImage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = instance.handleWebpages(url, false, haptics.common)),
                                model = url,
                                contentScale = ContentScale.FillWidth,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(4F / 3F)
                                            .background(Color.Gray.adjustAlpha(0.5F)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PlatformCircularProgressIndicator(
                                            modifier = Modifier.requiredSize(30.dp),
                                            color = Color(0xFFF9DE09),
                                            strokeWidth = 3.dp,
                                            trackColor = Color(0xFF797979),
                                            strokeCap = StrokeCap.Round,
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(4F / 3F)
                                            .background(Color.Gray.adjustAlpha(0.5F)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PlatformText(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 1.1F.em,
                                            text = if (Shared.language == "en") "Click to view" else "點擊查看"
                                        )
                                    }
                                },
                                contentDescription = name[Shared.language]
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopEntryExpansionAlightReminder(
    instance: AppActiveContext,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    allStops: ImmutableList<Registry.StopData>,
    stopData: Registry.StopData,
    routeBranches: ImmutableList<Route>,
    routeNumber: String,
    co: Operator,
    isKmbCtbJoint: Boolean,
    gmbRegion: GMBRegion?,
    alightReminderHighlightBlinkState: MutableState<Boolean>,
    alightReminderStateState: MutableState<AlightReminderServiceState?>,
    alightReminderTimeLeftState: MutableIntState,
    alightReminderStopsLeftState: MutableIntState,
    sheetTypeState: MutableState<BottomSheetType>,
    togglingAlightReminderState: MutableState<Boolean>
) {
    val selectedBranch by selectedBranchState
    val selectedStop by selectedStopState

    val favouriteStops by Shared.favoriteStops.collectAsStateMultiplatform()
    val favouriteRouteStops by Shared.favoriteRouteStops.collectAsStateMultiplatform()

    val optAlightReminderService by AlightReminderService.currentInstance.collectAsStateMultiplatform()
    val alightReminderService by remember(optAlightReminderService) { derivedStateOf { optAlightReminderService.value } }
    val isActiveReminderService by remember(alightReminderService) { derivedStateOf { alightReminderService?.selectedRoute?.let { routeBranches.contains(it) } == true } }

    val alightReminderHighlightBlink by alightReminderHighlightBlinkState
    val alightReminderState by alightReminderStateState
    val alightReminderTimeLeft by alightReminderTimeLeftState
    val alightReminderStopsLeft by alightReminderStopsLeftState

    val togglingAlightReminder by togglingAlightReminderState

    val favouriteStopAlreadySet by remember(favouriteStops, favouriteRouteStops) { derivedStateOf { favouriteStops.hasStop(stopData.stopId) || favouriteRouteStops.hasStop(stopData.stopId, co, selectedStop, stopData.stop, stopData.route) } }

    var sheetType by sheetTypeState

    Column(
        modifier = Modifier
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterVertically)
        ) {
            val timesText = alightReminderTimeLeft
                .takeIf { it >= 0 }
                ?.let { (it / 60).coerceAtLeast(1) }
                ?.let {
                    if (it > 1000) {
                        if (Shared.language == "en") "Journey Times Remaining:\nLoading..." else "剩餘車程: 載入中..."
                    } else {
                        if (Shared.language == "en") "Journey Times Remaining:\n$it Minutes (Ref. Only)" else "剩餘車程: ${it}分鐘 (只供參考)"
                    }
                }
                ?: if (Shared.language == "en") "Unable to provide\nJourney Times" else "未能提供車程"
            PlatformText(
                modifier = Modifier
                    .fillMaxWidth()
                    .userMarquee(),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                fontSize = 16.sp,
                maxLines = 2,
                text = timesText
            )
            val stopsRemainingText = if (Shared.language == "en") buildAnnotatedString {
                append("There are ")
                append(alightReminderStopsLeft.takeIf { it >= 0 }?.toString()?: "-", SpanStyle(fontSize = TextUnit.Big, fontWeight = FontWeight.Bold))
                append(" stops left")
            } else buildAnnotatedString {
                append("剩餘 ")
                append(alightReminderStopsLeft.takeIf { it >= 0 }?.toString()?: "-", SpanStyle(fontSize = TextUnit.Big, fontWeight = FontWeight.Bold))
                append(" 個站")
            }
            PlatformText(
                modifier = Modifier
                    .fillMaxWidth()
                    .userMarquee(),
                textAlign = TextAlign.Start,
                fontSize = 21.sp,
                lineHeight = 1.5F.em,
                maxLines = userMarqueeMaxLines(),
                text = stopsRemainingText
            )
            val dest = alightReminderService?.destinationStop?.stop?.name
            PlatformText(
                modifier = Modifier
                    .fillMaxWidth()
                    .userMarquee(),
                textAlign = TextAlign.Start,
                fontSize = 16.sp,
                maxLines = userMarqueeMaxLines(),
                text = when (alightReminderState) {
                    null -> ""
                    AlightReminderServiceState.ARRIVED -> {
                        if (Shared.language == "en") "Arrived at ${dest?.en}" else "已到達 ${dest?.zh}"
                    }
                    AlightReminderServiceState.ALMOST_THERE -> {
                        if (Shared.language == "en") "Arriving at ${dest?.en}" else "即將到達 ${dest?.zh}"
                    }
                    AlightReminderServiceState.TRAVELLING -> {
                        if (Shared.language == "en") "Going to ${dest?.en}" else "正在前往 ${dest?.zh}"
                    }
                }
            )
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
        ) {
            val haptic = LocalHapticFeedback.current
            stopData.stop.kmbBbiId?.let {
                PlatformFilledTonalIconButton(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .size(42.dp, 32.dp)
                        .plainTooltip(if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"),
                    onClick = instance.handleWebImages("https://app.kmb.hk/app1933/BBI/map/$it.jpg", false, haptic.common),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    PlatformIcon(
                        modifier = Modifier.size(25.dp),
                        painter = PlatformIcons.Outlined.TransferWithinAStation,
                        tint = Color(0xFFC4AB48),
                        contentDescription = if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"
                    )
                }
            }
            PlatformFilledTonalIconButton(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(42.dp, 32.dp)
                    .plainTooltip(if (Shared.language == "en") "Nearby Routes" else "附近路線"),
                onClick = { sheetType = BottomSheetType.NEARBY },
                shape = RoundedCornerShape(5.dp)
            ) {
                PlatformIcon(
                    modifier = Modifier.size(25.dp),
                    painter = PlatformIcons.Outlined.SyncAlt,
                    tint = Color(0xFFC4AB48),
                    contentDescription = if (Shared.language == "en") "Nearby Routes" else "附近路線"
                )
            }
            if (composePlatform.hasBackgroundLocation) {
                PlatformFilledTonalIconButton(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .size(42.dp, 32.dp)
                        .plainTooltip(if (Shared.language == "en") "Toggle Alight Reminder" else "開啟/關閉落車提示"),
                    enabled = !togglingAlightReminder,
                    onClick = { handleToggleAlightReminder(
                        instance = instance,
                        isActiveReminderService = isActiveReminderService,
                        co = co,
                        stopData = stopData,
                        allStops = allStops,
                        selectedBranch = selectedBranch,
                        selectedStop = selectedStop,
                        togglingAlightReminderState = togglingAlightReminderState
                    ) },
                    shape = RoundedCornerShape(5.dp)
                ) {
                    PlatformIcon(
                        modifier = Modifier.size(25.dp),
                        painter = if (isActiveReminderService) PlatformIcons.Outlined.NotificationsOff else PlatformIcons.Outlined.NotificationsActive,
                        tint = if (isActiveReminderService && alightReminderState == AlightReminderServiceState.ARRIVED && alightReminderHighlightBlink) Color(0xff52bb0a) else Color(0xFFC4AB48),
                        contentDescription = if (Shared.language == "en") "Toggle Alight Reminder" else "開啟/關閉落車提示"
                    )
                }
            }
            PlatformFilledTonalIconButton(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(42.dp, 32.dp)
                    .plainTooltip(if (Shared.language == "en") "Add to Favourites" else "設置最喜愛路線/巴士站"),
                onClick = { sheetType = BottomSheetType.FAV },
                shape = RoundedCornerShape(5.dp)
            ) {
                PlatformIcon(
                    modifier = Modifier.size(25.dp),
                    painter = if (favouriteStopAlreadySet) PlatformIcons.Outlined.Star else PlatformIcons.Outlined.StarOutline,
                    tint = Color(0xFFC4AB48),
                    contentDescription = if (Shared.language == "en") "Add to Favourites" else "設置最喜愛路線/巴士站"
                )
            }
            val pinnedItems by Shared.pinnedItems.collectAsStateMultiplatform()
            val pinItem by remember(allStops) { derivedStateOf {
                val stopBranches = allStops[selectedStop - 1].branchIds
                val branch = if (stopBranches.contains(selectedBranch)) selectedBranch else stopBranches.first()
                TemporaryPinItem(branch, co, selectedStop, routeBranches)
            } }
            PlatformFilledTonalIconButton(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(42.dp, 32.dp)
                    .plainTooltip(if (Shared.language == "en") "Pin to Top" else "置頂"),
                onClick = { Shared.togglePinnedItems(pinItem) },
                shape = RoundedCornerShape(5.dp)
            ) {
                PlatformIcon(
                    modifier = Modifier.size(25.dp),
                    painter = if (pinnedItems.any { it.key == pinItem.key }) PlatformIcons.Filled.PushPin else PlatformIcons.Outlined.PushPin,
                    tint = Color(0xFFC4AB48),
                    contentDescription = if (Shared.language == "en") "Pin to Top" else "置頂"
                )
            }
            PlatformFilledTonalIconButton(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(42.dp, 32.dp)
                    .plainTooltip(if (Shared.language == "en") "Share Route" else "分享路線"),
                onClick = { instance.compose.shareUrl(
                    url = selectedBranch.getDeepLink(instance, allStops[selectedStop - 1].stopId, selectedStop - 1),
                    title = "${if (Shared.language == "en") "Share Route" else "分享路線"} - ${co.getDisplayName(routeNumber, isKmbCtbJoint, gmbRegion, Shared.language)} ${co.getDisplayRouteNumber(routeNumber)} ${selectedBranch.resolvedDest(true)[Shared.language]} ${if (!co.isTrain) "${selectedStop}." else ""} ${allStops[selectedStop - 1].stop.name[Shared.language]}"
                ) },
                shape = RoundedCornerShape(5.dp)
            ) {
                PlatformIcon(
                    modifier = Modifier.size(25.dp),
                    painter = PlatformIcons.Outlined.Share,
                    tint = Color(0xFFC4AB48),
                    contentDescription = if (Shared.language == "en") "Share Route" else "分享路線"
                )
            }
            PlatformFilledTonalIconButton(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(42.dp, 32.dp)
                    .plainTooltip(if (Shared.language == "en") "More" else "更多"),
                onClick = { sheetType = BottomSheetType.ACTIONS },
                shape = RoundedCornerShape(5.dp)
            ) {
                PlatformIcon(
                    modifier = Modifier.size(25.dp),
                    painter = PlatformIcons.Outlined.MoreHoriz,
                    tint = Color(0xFFC4AB48),
                    contentDescription = if (Shared.language == "en") "More" else "更多"
                )
            }
        }
    }
}

@Composable
fun SetFavouriteInterface(instance: AppActiveContext, co: Operator, stopIndex: Int, stopData: Registry.StopData) {
    val alternateStopNamesShowing by Shared.alternateStopNamesShowingState.collectAsState()
    val favouriteStops by Shared.favoriteStops.collectAsStateMultiplatform()
    val favouriteRouteStops by Shared.favoriteRouteStops.collectAsStateMultiplatform()
    val favouriteStopAlreadySet by remember(favouriteStops) { derivedStateOf { favouriteStops.hasStop(stopData.stopId) } }
    var addingRouteGroup by remember { mutableStateOf(false) }
    var deletingRouteGroup: BilingualText? by remember { mutableStateOf(null) }
    val stopName by remember(stopData, alternateStopNamesShowing) { derivedStateOf {
        if (stopData.route.isKmbCtbJoint && alternateStopNamesShowing) {
            Registry.getInstance(instance).findJointAlternateStop(stopData.stopId, stopData.route.routeNumber).stop.name
        } else {
            stopData.stop.name
        }
    } }
    if (addingRouteGroup) {
        TextInputDialog(
            title = "新增分類" withEn "New Group",
            confirmText = "新增" withEn "Add",
            inputValidation = { it.isNotBlank() && favouriteRouteStops.none { g -> it anyEquals g.name } },
            placeholder = "名稱" withEn "Name",
            onDismissRequest = { _, _ -> addingRouteGroup = false },
            onConfirmation = {
                addingRouteGroup = false
                val updated = Shared.favoriteRouteStops.value.toMutableList().apply {
                    add(FavouriteRouteGroup(it.asBilingualText(), emptyList()))
                }
                Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
            }
        )
    }
    if (deletingRouteGroup != null) {
        DeleteDialog(
            icon = PlatformIcons.Filled.DeleteForever,
            title = "確認刪除分類\n${deletingRouteGroup?.zh}" withEn "Confirm Removal\n${deletingRouteGroup?.en}",
            text = "一經確認將不能復原" withEn "This action cannot be undone.",
            onDismissRequest = { deletingRouteGroup = null },
            onConfirmation = {
                val deleteName = deletingRouteGroup
                deletingRouteGroup = null
                val updated = Shared.favoriteRouteStops.value.filter { (n, _) -> n != deleteName }
                Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
            }
        )
    }
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
                    text = stopName[Shared.language],
                    fontSize = 25.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        content = { padding ->
            val scroll = rememberScrollState()
            Column (
                modifier = Modifier
                    .padding(padding)
                    .verticalScrollWithScrollbar(
                        state = scroll,
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        scrollbarConfig = ScrollBarConfig(
                            indicatorThickness = 4.dp
                        )
                    ),
            ) {
                PlatformText(
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp),
                    fontSize = 22.sp,
                    text = if (Shared.language == "en") "Favourite Stops" else "最喜愛巴士站"
                )
                HorizontalDivider()
                ActionRow(
                    onClick = {
                        if (favouriteStopAlreadySet) {
                            Registry.getInstance(instance).setFavouriteStops(Shared.favoriteStops.value.toMutableList().apply { removeStop(stopData.stopId) }, instance)
                        } else {
                            Registry.getInstance(instance).setFavouriteStops(Shared.favoriteStops.value.toMutableList().apply { add(FavouriteStop(stopData.stopId, stopData.stop, stopData.route.routeNumber.takeIf { stopData.route.isKmbCtbJoint })) }, instance)
                        }
                    },
                    icon = PlatformIcons.Outlined.Star,
                    iconColor = Color.Yellow.takeIf { favouriteStopAlreadySet },
                    text = (if (Shared.language == "en") "Add to Favourite Stops" else "設置為最喜愛巴士站").asAnnotatedString(),
                    subText = (if (Shared.language == "en") "Favourite Stops Count: ${favouriteStops.size}" else "最喜愛巴士站數量: ${favouriteStops.size}").asAnnotatedString()
                )
                HorizontalDivider(thickness = 5.dp)
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp, horizontal = 8.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    PlatformText(
                        fontSize = 22.sp,
                        text = if (Shared.language == "en") "Favourite Route Stops" else "最喜愛路線巴士站"
                    )
                    PlatformButton(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .align(Alignment.CenterEnd),
                        colors = ButtonDefaults.clearColors(),
                        contentPadding = PaddingValues(0.dp),
                        onClick = { addingRouteGroup = true }
                    ) {
                        PlatformIcon(
                            modifier = Modifier.size(22.dp),
                            painter = PlatformIcons.Outlined.Add,
                            contentDescription = if (Shared.language == "en") "New" else "新增"
                        )
                    }
                }
                HorizontalDivider()
                for ((name, routes, isDefaultGroup) in favouriteRouteStops) {
                    val same by remember(favouriteRouteStops) { derivedStateOf { routes.findSame(stopData.stopId, co, stopIndex, stopData.stop, stopData.route) } }
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp, horizontal = 8.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        PlatformText(
                            fontSize = 20.sp,
                            text = name[Shared.language]
                        )
                        if (!isDefaultGroup) {
                            PlatformButton(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .align(Alignment.CenterEnd),
                                colors = ButtonDefaults.clearColors(),
                                contentPadding = PaddingValues(0.dp),
                                onClick = { deletingRouteGroup = name }
                            ) {
                                PlatformIcon(
                                    modifier = Modifier.size(20.dp),
                                    painter = PlatformIcons.Filled.DeleteForever,
                                    tint = Color.Red,
                                    contentDescription = if (Shared.language == "en") "Delete" else "刪除"
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    ActionRow(
                        onClick = {
                            if (same.any { it.favouriteStopMode == FavouriteStopMode.FIXED }) {
                                val updated = Shared.favoriteRouteStops.value.toMutableList().apply {
                                    val groupIndex = indexOfName(name)
                                    set(groupIndex, get(groupIndex).remove(stopData.stopId, co, stopIndex, stopData.stop, stopData.route, FavouriteStopMode.FIXED))
                                }
                                Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
                            } else {
                                val fav = FavouriteRouteStop(stopData.stopId, co, stopIndex, stopData.stop, stopData.route, FavouriteStopMode.FIXED)
                                val updated = Shared.favoriteRouteStops.value.toMutableList().apply {
                                    val groupIndex = indexOfName(name)
                                    set(groupIndex, get(groupIndex).add(fav))
                                }
                                Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
                            }
                        },
                        icon = PlatformIcons.Outlined.Star,
                        iconColor = Color.Yellow.takeIf { same.any { it.favouriteStopMode == FavouriteStopMode.FIXED } },
                        text = (if (Shared.language == "en") "Add This Route Stop" else "設置本路線巴士站").asAnnotatedString(),
                        subText = (if (Shared.language == "en") "Route Stops Count: ${routes.size}" else "路線巴士站數量: ${routes.size}").asAnnotatedString()
                    )
                    ActionRow(
                        onClick = {
                            if (same.any { it.favouriteStopMode == FavouriteStopMode.CLOSEST }) {
                                val updated = Shared.favoriteRouteStops.value.toMutableList().apply {
                                    val groupIndex = indexOfName(name)
                                    set(groupIndex, get(groupIndex).remove(stopData.stopId, co, stopIndex, stopData.stop, stopData.route, FavouriteStopMode.CLOSEST))
                                }
                                Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
                            } else {
                                val fav = FavouriteRouteStop(stopData.stopId, co, stopIndex, stopData.stop, stopData.route, FavouriteStopMode.CLOSEST)
                                val updated = Shared.favoriteRouteStops.value.toMutableList().apply {
                                    val groupIndex = indexOfName(name)
                                    set(groupIndex, get(groupIndex).remove(fav).add(fav))
                                }
                                Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
                            }
                        },
                        icon = PlatformIcons.Outlined.Star,
                        iconColor = Color(0xFFFFE496).takeIf { same.any { it.favouriteStopMode == FavouriteStopMode.CLOSEST } },
                        text = (if (Shared.language == "en") "Add Any Closest Stop on This Route" else "設置本路線最近的任何站").asAnnotatedString(),
                        subText = (if (Shared.language == "en") "Route Stops Count: ${routes.size}" else "路線巴士站數量: ${routes.size}").asAnnotatedString()
                    )
                }
            }
        }
    )
}

@Composable
fun ActionRow(
    onClick: () -> Unit = { /* do nothing */ },
    onLongClick: () -> Unit = { /* do nothing */ },
    icon: Painter,
    text: AnnotatedString,
    subText: AnnotatedString? = null,
    iconColor: Color? = null,
    iconBackgroundColor: Color? = null,
    textColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlatformIcon(
            modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 10.dp)
                .size(50.dp)
                .clip(CircleShape)
                .background(iconBackgroundColor?: MaterialTheme.colorScheme.outline.adjustAlpha(0.5F))
                .padding(10.dp),
            painter = icon,
            tint = iconColor?: MaterialTheme.colorScheme.outline,
            contentDescription = text.text
        )
        Column(
            modifier = Modifier
                .weight(1F)
                .padding(end = 20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            PlatformText(
                modifier = Modifier.fillMaxWidth(),
                fontSize = 19F.sp,
                lineHeight = 21.5F.sp,
                textAlign = TextAlign.Start,
                color = textColor?: MaterialTheme.colorScheme.outline,
                text = text
            )
            if (subText != null) {
                PlatformText(
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14F.sp,
                    lineHeight = 16.5F.sp,
                    textAlign = TextAlign.Start,
                    color = textColor?: MaterialTheme.colorScheme.outline,
                    text = subText
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
fun ETAColumn(
    modifier: Modifier = Modifier,
    index: Int,
    co: Operator,
    options: Registry.EtaQueryOptions,
    stopData: Registry.StopData,
    etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>,
    etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>,
    fontSize: TextUnit,
    lineRange: IntRange,
    dynamicLineRange: Boolean,
    instance: AppActiveContext
) {
    var etaState by remember { mutableStateOf(etaResults.value[index]) }
    var errorCounter by remember { mutableIntStateOf(0) }

    val refreshEta = suspend {
        val optionsCopy = options.copy()
        val result = CoroutineScope(etaUpdateScope).async {
            Registry.getInstance(instance).buildEtaQuery(stopData.stopId, index, co, stopData.route, instance, options).query(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
        }.await()
        if (options == optionsCopy) {
            if (result.isConnectionError) {
                errorCounter++
                if (errorCounter > 1) {
                    etaState = result
                    etaResults.value[index] = result
                }
            } else {
                errorCounter = 0
                etaState = result
                etaResults.value[index] = result
            }
            etaUpdateTimes.value[index] = currentTimeMillis()
        }
    }
    LaunchedEffect (options) {
        refreshEta.invoke()
    }
    LaunchedEffect (Unit) {
        etaUpdateTimes.value[index]?.apply {
            delay(etaUpdateTimes.value[index]?.let { (Shared.ETA_UPDATE_INTERVAL - (currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        while (true) {
            refreshEta.invoke()
            delay(Shared.ETA_UPDATE_INTERVAL.toLong())
        }
    }
    RestartEffect {
        refreshEta.invoke()
    }

    ETADisplay(modifier, etaState, Shared.etaDisplayMode, co.isTrain, fontSize, lineRange, dynamicLineRange, instance)
}

@Composable
fun ETADisplay(
    modifier: Modifier, 
    lines: Registry.ETAQueryResult?,
    etaDisplayMode: ETADisplayMode,
    isTrain: Boolean,
    fontSize: TextUnit,
    lineRange: IntRange,
    dynamicLineRange: Boolean,
    instance: AppActiveContext
) {
    val resolvedText by remember(lineRange, lines, etaDisplayMode) { derivedStateOf { lineRange.associateWith { lines.getResolvedText(it, etaDisplayMode, instance) } } }
    val updating by remember(lines) { derivedStateOf { lines == null } }
    var freshness by remember { mutableStateOf(true) }

    val hasClockTime by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.clockTime.isNotEmpty() } } }
    val hasPlatform by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.platform.isNotEmpty() } } }
    val hasRouteNumber by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.routeNumber.isNotEmpty() } } }
    val hasDestination by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.destination.isNotEmpty() } } }
    val hasCarts by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.carts.isNotEmpty() } } }
    val hasTime by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.time.isNotEmpty() } } }
    val hasOperator by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.operator.isNotEmpty() } } }
    val hasRemark by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.remark.isNotEmpty() } } }

    val columns by remember(resolvedText) { derivedStateOf {
        buildImmutableList {
            if (hasClockTime && !isTrain) add(DataColumn(
                alignment = Alignment.End,
                width = TableColumnWidth.Wrap
            ) {})
            if (hasPlatform) add(DataColumn(
                width = TableColumnWidth.Wrap
            ) {})
            if (hasRouteNumber) add(DataColumn(
                width = TableColumnWidth.Wrap
            ) {})
            if (hasDestination) add(DataColumn(
                width = TableColumnWidth.Flex(1F)
            ) {})
            if (hasCarts) add(DataColumn(
                width = TableColumnWidth.Wrap
            ) {})
            if (hasClockTime && isTrain) add(DataColumn(
                alignment = Alignment.End,
                width = TableColumnWidth.Wrap
            ) {})
            if (hasTime) add(DataColumn(
                alignment = if ((lines?.nextScheduledBus?: -1) < 0) Alignment.Start else Alignment.End,
                width = TableColumnWidth.Wrap
            ) {})
            if (hasOperator) add(DataColumn(
                width = TableColumnWidth.Wrap
            ) {})
            if (hasRemark) add(DataColumn(
                width = TableColumnWidth.Flex(1F)
            ) {})
        }
    } }

    LaunchedEffect (lines) {
        while (true) {
            freshness = lines?.isOutdated() != true
            delay(500)
        }
    }

    BoxWithConstraints {
        val rowHeight = 26.fontScaledDp(0.5F) * (fontSize.px / 18.sp.px)
        val range = if (dynamicLineRange && maxHeight != Dp.Infinity) {
            lineRange.first..lineRange.last.coerceAtMost((maxHeight / rowHeight).floorToInt())
        } else {
            lineRange
        }
        DataTable(
            modifier = modifier.fillMaxWidth(),
            columns = columns,
            rowHeight = rowHeight,
            headerHeight = 0.dp,
            horizontalPadding = 0.dp,
            separator = { Spacer(modifier = Modifier.size(1.dp)) }
        ) {
            for (seq in range) {
                row {
                    if (hasClockTime && !isTrain) cell {
                        EtaText(resolvedText[seq]!!.clockTime, seq, updating, freshness, fontSize)
                    }
                    if (hasPlatform) cell {
                        EtaText(resolvedText[seq]!!.platform, seq, updating, freshness, fontSize)
                    }
                    if (hasRouteNumber) cell {
                        EtaText(resolvedText[seq]!!.routeNumber, seq, updating, freshness, fontSize)
                    }
                    if (hasDestination) cell {
                        EtaText(resolvedText[seq]!!.destination, seq, updating, freshness, fontSize)
                    }
                    if (hasCarts) cell {
                        EtaText(resolvedText[seq]!!.carts, seq, updating, freshness, fontSize)
                    }
                    if (hasClockTime && isTrain) cell {
                        EtaText(resolvedText[seq]!!.clockTime, seq, updating, freshness, fontSize)
                    }
                    if (hasTime) cell {
                        EtaText(resolvedText[seq]!!.time, seq, updating, freshness, fontSize)
                    }
                    if (hasOperator) cell {
                        EtaText(resolvedText[seq]!!.operator, seq, updating, freshness, fontSize)
                    }
                    if (hasRemark) cell {
                        EtaText(resolvedText[seq]!!.remark, seq, updating, freshness, fontSize)
                    }
                }
            }
        }
    }
}

@Composable
fun EtaText(
    text: FormattedText,
    seq: Int,
    updating: Boolean,
    freshness: Boolean,
    fontSize: TextUnit
) {
    val content = text.asContentAnnotatedString()
    if (seq == 1 || !updating) {
        PlatformText(
            modifier = Modifier
                .heightIn(min = fontSize.dp)
                .basicMarquee(Int.MAX_VALUE),
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            lineHeight = fontSize * 1.1F,
            fontSize = fontSize,
            color = if (freshness) platformLocalContentColor.adjustAlpha(if (updating) 0.7F else 1F) else Color(0xFFFFB0B0),
            maxLines = 1,
            text = content.annotatedString,
            inlineContent = content.createInlineContent(fontSize)
        )
    }
}

@Composable
fun PipModeInterface(
    index: Int,
    route: Route,
    co: Operator,
    isKmbCtbJoint: Boolean,
    options: Registry.EtaQueryOptions,
    stopData: Registry.StopData,
    etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>,
    etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>,
    instance: AppActiveContext
) {
    Dialog(
        onDismissRequest = { /* do nothing */ },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val alternateStopNamesShowing by Shared.alternateStopNamesShowingState.collectAsState()
        var size by remember { mutableStateOf(IntSize(599, 336)) }
        val factor by remember(size) { derivedStateOf { size.height / 336F } }
        val darkMode = Shared.theme.isDarkMode
        val routeNumber = route.routeNumber
        val stopName by remember(stopData, alternateStopNamesShowing) { derivedStateOf {
            if (stopData.route.isKmbCtbJoint && alternateStopNamesShowing) {
                Registry.getInstance(instance).findJointAlternateStop(stopData.stopId, stopData.route.routeNumber).stop.name
            } else {
                stopData.stop.name
            }
        } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size = it },
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Column(
                modifier = Modifier
                    .applyIf(isKmbCtbJoint, {
                        background(
                            brush = Brush.horizontalGradient(
                                0.0F to co.getColor(routeNumber, Color.White).adjustBrightness(if (darkMode) 0.4F else 1.6F),
                                1.0F to Operator.CTB.getColor(routeNumber, Color.White).adjustBrightness(if (darkMode) 0.4F else 1.6F),
                            )
                        )
                    }, {
                        background(co.getColor(routeNumber, Color.White).adjustBrightness(if (darkMode) 0.4F else 1.6F))
                    })
                    .padding(top = 5.dp, bottom = 2.5F.dp, start = 10.dp, end = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PlatformText(
                        modifier = Modifier
                            .alignByBaseline()
                            .padding(end = 2.dp),
                        fontSize = 16.dp.sp * factor,
                        lineHeight = 1.1F.em,
                        fontWeight = FontWeight.Bold,
                        text = co.getDisplayRouteNumber(routeNumber, true),
                        maxLines = 1
                    )
                    AutoResizeText(
                        modifier = Modifier
                            .weight(1F)
                            .alignByBaseline(),
                        fontSizeRange = FontSizeRange(max = 13.dp.sp * factor),
                        lineHeight = 1.1F.em,
                        text = buildAnnotatedString {
                            if (route.shouldPrependTo()) {
                                append(bilingualToPrefix[Shared.language], SpanStyle(fontSize = TextUnit.Small))
                            }
                            append(route.resolvedDest(false)[Shared.language], SpanStyle(fontWeight = FontWeight.Bold))
                        },
                        maxLines = 1
                    )
                    Image(
                        modifier = Modifier
                            .requiredSize(19.dp * factor)
                            .offset(y = 5.dp)
                            .padding(start = 2.5F.dp)
                            .align(Alignment.Top),
                        painter = painterResource(DrawableResource("icon_max.png")),
                        contentDescription = "HKBusETA"
                    )
                }
                AutoResizeText(
                    fontSizeRange = FontSizeRange(max = 12.dp.sp * factor),
                    lineHeight = 1.1F.em,
                    text = "${if (co.isTrain) "" else "${index}. "}${stopName[Shared.language]}",
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .background(platformBackgroundColor)
                    .padding(top = 2.5F.dp, bottom = 10.dp, start = 10.dp, end = 10.dp),
                contentAlignment = Alignment.TopStart
            ) {
                ETAColumn(
                    modifier = Modifier.fillMaxWidth(),
                    index = index,
                    co = co,
                    stopData = stopData,
                    etaResults = etaResults,
                    etaUpdateTimes = etaUpdateTimes,
                    options = options,
                    fontSize = 12F.dp.sp * factor,
                    lineRange = 1..4,
                    dynamicLineRange = true,
                    instance = instance
                )
            }

        }
    }
}

fun handleToggleAlightReminder(
    instance: AppActiveContext,
    isActiveReminderService: Boolean,
    co: Operator,
    stopData: Registry.StopData,
    allStops: ImmutableList<Registry.StopData>,
    selectedBranch: Route,
    selectedStop: Int,
    togglingAlightReminderState: MutableState<Boolean>
) {
    when {
        isActiveReminderService -> {
            AlightReminderService.kill()
        }
        co.isTrain || stopData.branchIds.contains(selectedBranch) -> {
            checkNotificationPermission(instance, true) { result ->
                if (result) {
                    togglingAlightReminderState.value = true
                    CoroutineScope(Dispatchers.Main).launch {
                        val job = CoroutineScope(Dispatchers.IO).launch {
                            delay(500)
                            instance.showToastText(if (Shared.language == "en") "Getting alight reminder service ready..." else "正在啟動落車提示...", ToastDuration.LONG)
                        }
                        try {
                            val originStopId = getGPSLocation(instance).await()?.location?.let { location ->
                                val closestStop = allStops.asSequence().take(selectedStop).filter { it.branchIds.contains(selectedBranch) }.minBy { location.distance(it.stop.location) }
                                StopIdIndexed(closestStop.stopId, allStops.indexOf(closestStop) + 1)
                            }?: run {
                                val firstStop = selectedBranch.stops[co]!!.first()
                                val firstStopIndex = allStops.indexOfFirst { firstStop == it.stopId } + 1
                                StopIdIndexed(firstStop, firstStopIndex)
                            }
                            AlightReminderService.startNewService(
                                context = applicationAppContext,
                                locationUpdater = { a, i, c -> getGPSLocation(a, i, c) },
                                selectedRoute = selectedBranch,
                                co = co,
                                originStopId = originStopId,
                                destinationStopId = StopIdIndexed(stopData.stopId, selectedStop)
                            )
                            instance.showToastText(if (Shared.language == "en") "Alight reminder service started" else "落車提示啟動", ToastDuration.SHORT)
                        } finally {
                            job.cancelAndJoin()
                            togglingAlightReminderState.value = false
                        }
                    }
                } else {
                    instance.showToastText(if (Shared.language == "en") "Notification Permission Denied" else "推送通知權限被拒絕", ToastDuration.LONG)
                }
            }
        }
        else -> {
            instance.showToastText(if (Shared.language == "en") "Selected service branch does not stop at this stop" else "所選班次路線不停此站", ToastDuration.LONG)
        }
    }
}