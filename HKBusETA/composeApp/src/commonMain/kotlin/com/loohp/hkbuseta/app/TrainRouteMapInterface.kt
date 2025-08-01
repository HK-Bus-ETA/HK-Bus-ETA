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

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.SizeResolver
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.CoilZoomState
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.ScalesCalculator
import com.loohp.hkbuseta.appcontext.AppScreenGroup
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.appcontext.screenGroup
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ReduceDataOmitted
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.ETADisplayMode
import com.loohp.hkbuseta.common.objects.Fare
import com.loohp.hkbuseta.common.objects.FareCategory
import com.loohp.hkbuseta.common.objects.FareType
import com.loohp.hkbuseta.common.objects.FavouriteStop
import com.loohp.hkbuseta.common.objects.FirstLastTrainPath
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RouteNotice
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.TicketCategory
import com.loohp.hkbuseta.common.objects.TrainServiceStatus
import com.loohp.hkbuseta.common.objects.TrainServiceStatusMessageStatus
import com.loohp.hkbuseta.common.objects.TrainServiceStatusType
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.asStop
import com.loohp.hkbuseta.common.objects.compareRouteNumber
import com.loohp.hkbuseta.common.objects.findFare
import com.loohp.hkbuseta.common.objects.findLRTFares
import com.loohp.hkbuseta.common.objects.findLRTFirstTrain
import com.loohp.hkbuseta.common.objects.findLRTLastTrain
import com.loohp.hkbuseta.common.objects.findMTRFares
import com.loohp.hkbuseta.common.objects.findMTRFirstTrain
import com.loohp.hkbuseta.common.objects.findMTRLastTrain
import com.loohp.hkbuseta.common.objects.findMTROpeningTimes
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getMTRBarrierFreeCategories
import com.loohp.hkbuseta.common.objects.getMTRStationBarrierFree
import com.loohp.hkbuseta.common.objects.getMTRStationLayoutUrl
import com.loohp.hkbuseta.common.objects.getMTRStationStreetMapUrl
import com.loohp.hkbuseta.common.objects.getMtrLineName
import com.loohp.hkbuseta.common.objects.getMtrLineSortingIndex
import com.loohp.hkbuseta.common.objects.getOperatorName
import com.loohp.hkbuseta.common.objects.getRedirectToMTRJourneyPlannerUrl
import com.loohp.hkbuseta.common.objects.getStationBarrierFreeDetails
import com.loohp.hkbuseta.common.objects.hasStop
import com.loohp.hkbuseta.common.objects.identifyStopCo
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.mtrLineStatus
import com.loohp.hkbuseta.common.objects.mtrLines
import com.loohp.hkbuseta.common.objects.removeStop
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Shared.getResolvedText
import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.asFormattedText
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.asImmutableMap
import com.loohp.hkbuseta.common.utils.awaitWithTimeout
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.editDistance
import com.loohp.hkbuseta.common.utils.indexOf
import com.loohp.hkbuseta.common.utils.isNotNullAndNotEmpty
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.toLocalDateTime
import com.loohp.hkbuseta.compose.Accessible
import com.loohp.hkbuseta.compose.Bedtime
import com.loohp.hkbuseta.compose.ChangedEffect
import com.loohp.hkbuseta.compose.CheckCircle
import com.loohp.hkbuseta.compose.DarkMode
import com.loohp.hkbuseta.compose.Error
import com.loohp.hkbuseta.compose.Grab
import com.loohp.hkbuseta.compose.Grabbing
import com.loohp.hkbuseta.compose.IconPainterLayer
import com.loohp.hkbuseta.compose.Info
import com.loohp.hkbuseta.compose.Map
import com.loohp.hkbuseta.compose.NotificationImportant
import com.loohp.hkbuseta.compose.Paid
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformFloatingActionButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformModalBottomSheet
import com.loohp.hkbuseta.compose.PlatformTab
import com.loohp.hkbuseta.compose.PlatformTabRow
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.Route
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.Signal
import com.loohp.hkbuseta.compose.Star
import com.loohp.hkbuseta.compose.StarOutline
import com.loohp.hkbuseta.compose.Start
import com.loohp.hkbuseta.compose.Streetview
import com.loohp.hkbuseta.compose.Table
import com.loohp.hkbuseta.compose.TableColumn
import com.loohp.hkbuseta.compose.TableColumnWidth
import com.loohp.hkbuseta.compose.TableRow
import com.loohp.hkbuseta.compose.TableRowAlignment
import com.loohp.hkbuseta.compose.Train
import com.loohp.hkbuseta.compose.VerticalGrid
import com.loohp.hkbuseta.compose.applyIfNotNull
import com.loohp.hkbuseta.compose.clickable
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.dummySignal
import com.loohp.hkbuseta.compose.platformComponentBackgroundColor
import com.loohp.hkbuseta.compose.platformLocalContentColor
import com.loohp.hkbuseta.compose.platformPrimaryContainerColor
import com.loohp.hkbuseta.compose.rememberPlatformModalBottomSheetState
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.compose.userMarqueeMaxLines
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.shared.ComposeShared
import com.loohp.hkbuseta.shared.getOperatorNotices
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.coordinatesNullableStateSaver
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.fontScaledDp
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.lastLocation
import com.loohp.hkbuseta.utils.predefined
import com.loohp.hkbuseta.utils.realPointToContentPoint
import com.loohp.hkbuseta.utils.realPointToTouchPoint
import com.loohp.hkbuseta.utils.renderedSize
import com.loohp.hkbuseta.utils.touchPointToRealPoint
import com.loohp.hkbuseta.utils.unrestrictedBitmapSize
import com.loohp.hkbuseta.utils.withAlpha
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.getResourceUri
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.readResourceBytes

@Immutable
data class RouteMapData(
    val dimension: Size,
    val stations: Map<String, Offset>
) {
    companion object {
        @OptIn(InternalResourceApi::class)
        suspend fun fromFile(file: String): RouteMapData {
            val lines = readResourceBytes(file).decodeToString()
            val json = Json.decodeFromString<JsonObject>(lines)
            val dimension = json.optJsonObject("properties")!!.optJsonArray("dimension")!!.let { Size(it.optDouble(0).toFloat(), it.optDouble(1).toFloat()) }
            val stops = json.optJsonObject("stops")!!.mapToMutableMap { it.jsonArray.let { a -> Offset(a.optDouble(0).toFloat(), a.optDouble(1).toFloat()) } }
            return RouteMapData(dimension, stops)
        }
    }

    fun findClickedStations(offset: Offset): String? {
        return stations.entries
            .asSequence()
            .map { it to (it.value - offset).getDistanceSquared() }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= 40000 }
            ?.first?.key
    }
}

private val mtrRouteMapDataState: MutableStateFlow<RouteMapData?> = MutableStateFlow(null)
private val mtrRouteMapLoaded: MutableStateFlow<Boolean> = MutableStateFlow(false)

private val lightRailRouteMapDataState: MutableStateFlow<RouteMapData?> = MutableStateFlow(null)
private val lightRailRouteMapLoaded: MutableStateFlow<Boolean> = MutableStateFlow(false)

@Immutable
data class TrainRouteMapTabItem(
    val title: BilingualText,
    val iconLayers: List<IconPainterLayer>,
    val color: Color? = null,
    val shouldSaveTabPosition: Boolean
) {
    constructor(
        title: BilingualText,
        icon: @Composable () -> Painter,
        color: Color? = null,
        shouldSaveTabPosition: Boolean
    ): this(
        title = title,
        iconLayers = listOf(IconPainterLayer(icon)),
        color = color,
        shouldSaveTabPosition = shouldSaveTabPosition
    )
}

val trainRouteMapItems = listOf(
    TrainRouteMapTabItem(
        title = "港鐵" withEn "MTR",
        iconLayers = listOf(
            IconPainterLayer({ painterResource(DrawableResource("mtr_background_vector.xml")) }, Color.White) { scale(0.9F) },
            IconPainterLayer({ painterResource(DrawableResource("mtr_vector.xml")) })
        ),
        color = Color(0xFFAC2E44),
        shouldSaveTabPosition = true
    ),
    TrainRouteMapTabItem(
        title = "輕鐵" withEn "Light Rail",
        iconLayers = listOf(
            IconPainterLayer({ painterResource(DrawableResource("lrt_background_vector.xml")) }, Color.White) { scale(0.9F) },
            IconPainterLayer({ painterResource(DrawableResource("lrt_vector.xml")) })
        ),
        color = Color(0xFFCDA410),
        shouldSaveTabPosition = true
    ),
    TrainRouteMapTabItem(
        title = "公告" withEn "Notices",
        icon = { PlatformIcons.Outlined.NotificationImportant },
        shouldSaveTabPosition = false
    )
)

data class LrtRouteDisplayMapTabItems(val title: BilingualText)

val lrtRouteDisplayModeItems = listOf(
    LrtRouteDisplayMapTabItems(title = "月台" withEn "Platforms"),
    LrtRouteDisplayMapTabItems(title = "路線" withEn "Routes")
)

private val routeMapSearchSelectedTabIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
private val mtrLineServiceDisruptionState: MutableStateFlow<Map<String, TrainServiceStatus>> = MutableStateFlow(emptyMap())
private val mtrLineServiceDisruptionAvailableState: MutableStateFlow<Boolean> = MutableStateFlow(false)

@Composable
fun RouteMapSearchInterface(
    instance: AppActiveContext,
    visible: Boolean,
    signal: Signal = dummySignal
) {
    var routeMapSearchSelectedTabIndex by routeMapSearchSelectedTabIndexState.collectAsStateMultiplatform()
    val pagerState = rememberPagerState(
        initialPage = routeMapSearchSelectedTabIndex,
        pageCount = { trainRouteMapItems.size }
    )
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var stopLaunch: String? by remember { mutableStateOf(null) }

    LaunchedEffect (visible) {
        if (visible) {
            val stopId = instance.compose.data["stopLaunch"] as? String
            if (stopId != null) {
                val operators = stopId.identifyStopCo()
                if (operators.isNotEmpty()) {
                    val co = operators.first()
                    if (co.isTrain) {
                        when (co) {
                            Operator.MTR -> pagerState.animateScrollToPage(0)
                            Operator.LRT -> pagerState.animateScrollToPage(1)
                        }
                        stopLaunch = stopId
                        instance.compose.data.remove("stopLaunch")
                    }
                }
            }
        }
    }

    LaunchedEffect (pagerState.currentPage, pagerState.isScrollInProgress) {
        val index = pagerState.currentPage
        if (!pagerState.isScrollInProgress && trainRouteMapItems[index].shouldSaveTabPosition) {
            routeMapSearchSelectedTabIndex = index
        }
    }
    ChangedEffect (signal) {
        val index = pagerState.currentPage
        scope.launch { pagerState.animateScrollToPage(if (index == 0) 1 else 0) }
    }

    var location by rememberSaveable(saver = coordinatesNullableStateSaver) { mutableStateOf(lastLocation?.location) }

    LaunchedEffect (Unit) {
        while (true) {
            val result = getGPSLocation(instance, LocationPriority.FASTER).awaitWithTimeout(3000)
            if (result?.isSuccess == true) {
                location = result.location!!
            } else {
                val retryResult = getGPSLocation(instance).await()
                if (retryResult?.isSuccess == true) {
                    location = retryResult.location!!
                }
            }
            delay(Shared.ETA_UPDATE_INTERVAL.toLong())
        }
    }

    var mtrLineServiceDisruption by mtrLineServiceDisruptionState.collectAsStateMultiplatform()
    var mtrLineServiceDisruptionAvailable by mtrLineServiceDisruptionAvailableState.collectAsStateMultiplatform()
    var notices: List<RouteNotice>? by remember { mutableStateOf(null) }

    val mtrLineServiceMessages by remember { derivedStateOf { mtrLineServiceDisruption.asSequence().filter { (_, v) -> v.messages != null }.toImmutableList() } }

    val appAlert by ComposeShared.rememberAppAlert(instance)

    LaunchedEffect (Unit) {
        notices = Registry.getInstance(instance).getOperatorNotices(setOf(Operator.MTR, Operator.LRT), instance)
    }
    LaunchedEffect (Unit) {
        while (true) {
            val result = Registry.getInstance(instance).getMtrLineServiceDisruption()
            if (result == null) {
                mtrLineServiceDisruptionAvailable = false
            } else {
                mtrLineServiceDisruptionAvailable = true
                mtrLineServiceDisruption = result
            }
            delay(180000)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.zIndex(2F),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlatformTabRow(selectedTabIndex = pagerState.currentPage) {
                trainRouteMapItems.forEachIndexed { index, (title, iconLayers, color) ->
                    PlatformTab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = {
                            Box {
                                for ((icon, overrideColor, modifier) in iconLayers) {
                                    PlatformIcon(
                                        modifier = Modifier.size(18F.sp.dp).run(modifier),
                                        painter = icon.invoke(),
                                        tint = overrideColor?: color,
                                        contentDescription = title[Shared.language]
                                    )
                                }
                            }
                        },
                        text = {
                            PlatformText(
                                fontSize = 16F.sp,
                                lineHeight = 1.1F.em,
                                maxLines = 1,
                                text = title[Shared.language]
                            )
                        }
                    )
                }
            }
        }
        AnimatedVisibility(
            modifier = Modifier.zIndex(1F),
            visible = mtrLineServiceMessages.isNotEmpty(),
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(durationMillis = 300)
            )
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "MessageCrossfade")
            val animatedCurrentLine by infiniteTransition.animateValue(
                initialValue = 0,
                targetValue = mtrLineServiceMessages.size,
                typeConverter = Int.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(5500 * mtrLineServiceMessages.size, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "SecondLineCrossFade"
            )
            Crossfade(
                modifier = Modifier.animateContentSize(),
                targetState = animatedCurrentLine,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing),
                label = "SecondLineCrossFade"
            ) {
                val (line, status) = mtrLineServiceMessages[it.coerceIn(mtrLineServiceMessages.indices)]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .applyIfNotNull(status.messages?.url(composePlatform.isMobile)?.get(Shared.language)) { url -> clickable(
                            onClick = instance.handleWebpages(url, false, haptics.common),
                            role = Role.Button
                        ) }
                        .background(if (status.messages?.status == TrainServiceStatusMessageStatus.RED) {
                            Color(0xFFEB4034)
                        } else {
                            if (line == "LR") Operator.LRT.getOperatorColor(Color.LightGray) else Operator.MTR.getLineColor(line, Color.LightGray)
                        })
                        .padding(10.dp)
                ) {
                    PlatformText(
                        fontSize = 16.sp,
                        color = Color.White,
                        text = buildString {
                            append(if (line == "LR") Operator.LRT.getOperatorName(Shared.language) else if (Shared.language == "en") line else line.getMtrLineName().zh)
                            append(" - ")
                            append(status.messages?.title?.get(Shared.language)?: "")
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier.weight(1F),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (visible || (pagerState.currentPage == 0 && mtrRouteMapLoaded.value) || (pagerState.currentPage == 1 && lightRailRouteMapLoaded.value) || pagerState.currentPage == 2) {
                Column {
                    ComposeShared.AnimatedVisibilityColumnAppAlert(instance, appAlert, pagerState.currentPage.let { it == 0 || it == 1 })
                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        state = pagerState,
                        userScrollEnabled = true,
                        verticalAlignment = Alignment.Top,
                    ) {
                        when (it) {
                            0 -> MTRRouteMapInterface(instance, location, stopLaunch, false, visible)
                            1 -> LRTRouteMapInterface(instance, location, stopLaunch, false, visible)
                            2 -> NoticeInterface(instance, notices?.asImmutableList(), false)
                            else -> PlatformText(trainRouteMapItems[it].title[Shared.language])
                        }
                    }
                }
                val offset by animateDpAsState(
                    targetValue = if (pagerState.currentPage < 2) 0.dp else 100.dp,
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
                if (offset < 100.dp) {
                    val statusNotice = mtrLineStatus
                    val disruptedLines = mtrLineServiceDisruption.asSequence()
                        .filter { (_, v) -> v.type == TrainServiceStatusType.DISRUPTION }
                        .map { (k) -> k }
                        .sortedBy { it.getMtrLineSortingIndex() }
                        .map { it to if (it == "LR") Operator.LRT.getOperatorColor(Color.LightGray) else Operator.MTR.getLineColor(it, Color.LightGray) }
                        .toList()
                    val mtrLinesStatus = mtrLineServiceDisruption.filter { (k) -> mtrLines.contains(k) }
                    val (statusMessage, statusIcon) = when {
                        mtrLinesStatus.values.all { it.type == TrainServiceStatusType.NON_SERVICE_HOUR } -> {
                            (if (Shared.language == "en") "Non-service Hours" else "非服務時間") to PlatformIcons.Filled.DarkMode
                        }
                        mtrLinesStatus.values.all { it.type == TrainServiceStatusType.TYPHOON } -> {
                            (if (Shared.language == "en") "Typhoon Timetable" else "颱風時間表") to PlatformIcons.Filled.Error
                        }
                        else -> {
                            (if (Shared.language == "en") "Good Service" else "服務正常") to PlatformIcons.Filled.CheckCircle
                        }
                    }
                    PlatformFloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .graphicsLayer { translationY = offset.toPx() },
                        onClick = if (statusNotice.isPdf) ({
                            instance.startActivity(AppIntent(instance, AppScreen.PDF).apply {
                                putExtra("title", statusNotice.title[Shared.language])
                                putExtra("url", statusNotice.url[Shared.language])
                            })
                        }) else {
                            instance.handleWebpages(statusNotice.url[Shared.language], false, LocalHapticFeedback.current.common)
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .animateContentSize(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column {
                                PlatformText(
                                    fontSize = 17.sp,
                                    text = statusNotice.title[Shared.language],
                                    fontWeight = FontWeight.Bold
                                )
                                when {
                                    !mtrLineServiceDisruptionAvailable -> PlatformText(
                                        fontSize = 14.sp,
                                        text = if (Shared.language == "en") "Click to Check" else "點擊查看"
                                    )
                                    disruptedLines.isEmpty() -> PlatformText(
                                        fontSize = 14.sp,
                                        text = statusMessage
                                    )
                                    else -> Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlatformText(
                                            fontSize = 14.sp,
                                            text = if (Shared.language == "en") "Disruption" else "服務受阻"
                                        )
                                        PlatformIcon(
                                            modifier = Modifier.size(16.dp),
                                            painter = PlatformIcons.Filled.Error,
                                            contentDescription = if (Shared.language == "en") "Disruption" else "服務受阻"
                                        )
                                    }
                                }
                            }
                            if (mtrLineServiceDisruptionAvailable) {
                                when (disruptedLines.size) {
                                    0 -> PlatformIcon(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .align(Alignment.Bottom),
                                        painter = statusIcon,
                                        contentDescription = statusMessage
                                    )
                                    1, 2 -> Row(
                                        modifier = Modifier
                                            .align(Alignment.Bottom)
                                            .height(25.dp)
                                            .requiredWidth(22.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End)
                                    ) {
                                        for ((_, lineColor) in disruptedLines) {
                                            Spacer(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(10.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(lineColor)
                                            )
                                        }
                                    }
                                    else -> Column(
                                        modifier = Modifier
                                            .align(Alignment.Bottom)
                                            .requiredWidth(22.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        for (r in disruptedLines.indices step 2) {
                                            Row(
                                                modifier = Modifier.align(Alignment.End),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                for (i in 0..1) {
                                                    disruptedLines.getOrNull(r + i)?.let { (_, lineColor) ->
                                                        Spacer(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(RoundedCornerShape(5.dp))
                                                                .background(lineColor)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                PlatformIcon(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.Bottom),
                                    painter = PlatformIcons.Filled.Info,
                                    contentDescription = if (Shared.language == "en") "Click to Check" else "點擊查看"
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(platformComponentBackgroundColor)
                )
            }
        }
    }
}

data class ZoomState(val scale: Float, val offset: Offset?)

enum class StationInfoSheetType(
    val showing: Boolean,
    val textCloseColor: Boolean
) {
    NONE(
        showing = false,
        textCloseColor = false
    ),
    FARE(
        showing = true,
        textCloseColor = false
    ),
    OPENING_FIRST_LAST_TRAIN(
        showing = true,
        textCloseColor = true
    ),
    BARRIER_FREE(
        showing = true,
        textCloseColor = false
    )
}

private val mtrRouteMapZoomState: MutableStateFlow<ZoomState> = MutableStateFlow(ZoomState(if (composePlatform.hasLargeScreen) 0.7F else 1F, null))
private val mtrRouteMapLocationJumpedState: MutableStateFlow<Boolean> = MutableStateFlow(false)
private val mtrRouteMapSelectedStopIdState: MutableStateFlow<String?> = MutableStateFlow(null)
private val mtrRouteMapShowingSheetState: MutableStateFlow<Boolean> = MutableStateFlow(false)
private val selectedMtrStartingStationState: MutableStateFlow<String?> = MutableStateFlow(null)

var lastChosenLine: Color = Color.Transparent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MTRRouteMapInterface(
    instance: AppActiveContext,
    location: Coordinates?,
    stopLaunch: String?,
    isPreview: Boolean,
    visible: Boolean
) {
    var zoomState by mtrRouteMapZoomState.collectAsStateMultiplatform()
    val state = rememberCoilZoomState()
    var mtrRouteMapData by mtrRouteMapDataState.collectAsStateMultiplatform()
    var allStops: Map<String, Stop?> by remember { mutableStateOf(mtrRouteMapData?.let { it.stations.keys.associateWith { s -> s.asStop(instance) } }.orEmpty()) }
    val closestStopState: MutableState<Map.Entry<String, Stop?>?> = remember { mutableStateOf(null) }
    var closestStop by closestStopState

    val sheetState = rememberPlatformModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedStopId by mtrRouteMapSelectedStopIdState.collectAsStateMultiplatform()
    var showingSheet by mtrRouteMapShowingSheetState.collectAsStateMultiplatform()
    var locationJumped by mtrRouteMapLocationJumpedState.collectAsStateMultiplatform()
    var selectedMtrStartingStation by selectedMtrStartingStationState.collectAsStateMultiplatform()

    val mtrSheetInfoTypeState = rememberSaveable { mutableStateOf(StationInfoSheetType.NONE) }
    var mtrSheetInfoType by mtrSheetInfoTypeState
    val mtrSheetInfoState = rememberPlatformModalBottomSheetState()

    val loadedState = remember { mutableStateOf(false) }
    val loaded by loadedState

    var setScale by remember(loaded) { mutableStateOf(false) }

    LaunchedEffect (Unit) {
        state.zoomable.apply {
            threeStepScale = true
            mouseWheelScaleScrollDeltaConverter = { it * 0.004F }
            scalesCalculator = ScalesCalculator.predefined(
                minScale = 0.7F,
                mediumScale = 1F,
                maxScale = 1.5F
            )
        }
    }
    LaunchedEffect (loaded) {
        if (loaded) {
            state.zoomable.apply {
                delay(50)
                zoomState.offset?.let {
                    scale(zoomState.scale)
                    offset(it)
                }?: run {
                    val dimension = mtrRouteMapData?.dimension?: Size.Zero
                    val offset = state.zoomable.realPointToContentPoint(dimension.center, dimension)
                    locate(offset.round(), zoomState.scale)
                }
                setScale = true
            }
        }
    }
    LaunchedEffect (setScale, state.zoomable.transform.scaleX, state.zoomable.transform.offset) {
        if (setScale) {
            zoomState = ZoomState(state.zoomable.transform.scaleX, state.zoomable.transform.offset)
        }
    }
    LaunchedEffect (Unit) {
        mtrRouteMapLoaded.value = true
    }
    LaunchedEffect (Unit) {
        if (mtrRouteMapData == null) {
            val data = RouteMapData.fromFile("routemaps/mtr_system_map.json")
            mtrRouteMapData = data
            allStops = data.stations.keys.associateWith { it.asStop(instance) }
        }
    }
    LaunchedEffect (setScale, allStops, location) {
        if (setScale && location != null && allStops.isNotEmpty()) {
            val stop = allStops.entries.asSequence().filter { it.value != null }.minByOrNull { it.value!!.location.distance(location) }
            closestStop = stop
            if (stop != null) {
                if (selectedMtrStartingStation == null) {
                    selectedMtrStartingStation = stop.key
                }
                if (!locationJumped && stopLaunch == null) {
                    mtrRouteMapData?.let { data ->
                        val position = data.stations[stop.key]!!
                        val offset = state.zoomable.realPointToContentPoint(position, data.dimension)
                        state.zoomable.locate(offset.round(), animated = true)
                        locationJumped = true
                    }
                }
            }
        }
    }
    LaunchedEffect (setScale, stopLaunch) {
        if (setScale) {
            stopLaunch?.let { stopLaunch ->
                while (mtrRouteMapData == null) delay(10)
                mtrRouteMapData?.let { data ->
                    val position = data.stations[stopLaunch]
                    if (position != null) {
                        val offset = state.zoomable.realPointToContentPoint(position, data.dimension)
                        state.zoomable.locate(offset.round(), animated = true)
                        selectedStopId = stopLaunch
                        sheetState.hide()
                        showingSheet = true
                    }
                }
            }
        }
    }
    LaunchedEffect (showingSheet, visible) {
        if (visible) {
            delay(1000)
            if (showingSheet && sheetState.currentValue == SheetValue.Hidden) {
                showingSheet = false
            }
        }
    }

    MTRRouteMapMapInterface(instance, state, sheetState, loadedState, closestStopState, setScale)

    selectedStopId?.let { stopId ->
        val stop by remember(stopId) { derivedStateOf { stopId.asStop(instance)!! } }
        if (visible && showingSheet) {
            PlatformModalBottomSheet(
                onDismissRequest = { showingSheet = false },
                sheetState = sheetState,
                desktopCloseColor = Color.White
            ) {
                MTRETADisplayInterface(stopId, stop, mtrSheetInfoTypeState, isPreview, instance)
            }
        }
        if (visible && mtrSheetInfoType.showing) {
            PlatformModalBottomSheet(
                onDismissRequest = { mtrSheetInfoType = StationInfoSheetType.NONE },
                sheetState = mtrSheetInfoState,
                desktopCloseColor = Color.White.takeIf { mtrSheetInfoType.textCloseColor }
            ) {
                Box(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    MTRRouteMapInfoSheetInterface(stopId, stop, mtrSheetInfoType, instance)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, InternalResourceApi::class)
@Composable
fun MTRRouteMapMapInterface(
    instance: AppActiveContext,
    state: CoilZoomState,
    sheetState: SheetState,
    loadedState: MutableState<Boolean>,
    closestStopState: MutableState<Map.Entry<String, Stop?>?>,
    show: Boolean
) {
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "ClosestStationIndicator")
    val animatedClosestStationRadius by infiniteTransition.animateFloat(
        initialValue = 100F,
        targetValue = 140F,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ClosestStationIndicator"
    )
    val mtrRouteMapData by mtrRouteMapDataState.collectAsStateMultiplatform()
    var selectedStopId by mtrRouteMapSelectedStopIdState.collectAsStateMultiplatform()
    var showingSheet by mtrRouteMapShowingSheetState.collectAsStateMultiplatform()
    var selectedMtrStartingStation by selectedMtrStartingStationState.collectAsStateMultiplatform()
    val typhoonInfo by Registry.getInstance(instance).typhoonInfo.collectAsStateMultiplatform()
    val closestStop by closestStopState
    var loaded by loadedState
    var isPressing by remember { mutableStateOf(false) }

    val path = getResourceUri("routemaps/mtr_system_map${if (Shared.theme.isDarkMode) "_dark" else ""}${if (typhoonInfo.isAboveTyphoonSignalNine) "_typhoon" else ""}.png")

    mtrRouteMapData?.let { data ->
        CoilZoomAsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .composed {
                    if (loaded) {
                        var hoveringStation by remember { mutableStateOf(false) }
                        pointerInput(Unit) {
                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    when (event.type) {
                                        PointerEventType.Move -> {
                                            val change = event.changes[0]
                                            val pos = change.position
                                            val clickedPos = state.zoomable.touchPointToRealPoint(pos, data.dimension)
                                            hoveringStation = data.findClickedStations(clickedPos) != null
                                        }
                                        PointerEventType.Press -> isPressing = true
                                        PointerEventType.Release -> isPressing = false
                                    }
                                }
                            }
                        }.pointerHoverIcon(if (hoveringStation) PointerIcon.Hand else if (isPressing) PointerIcon.Grabbing else PointerIcon.Grab)
                    } else {
                        this
                    }
                }
                .drawWithContent {
                    if (show) {
                        drawContent()
                        if (loaded) {
                            selectedMtrStartingStation?.let { selected ->
                                val position = data.stations[selected]
                                if (position != null) {
                                    val center = state.zoomable.realPointToTouchPoint(position, data.dimension)
                                    drawCircle(
                                        color = Color(0xff53ff19),
                                        radius = animatedClosestStationRadius,
                                        center = center,
                                        alpha = 0.3F,
                                        style = Fill
                                    )
                                    drawCircle(
                                        color = Color(0xff53ff19),
                                        radius = animatedClosestStationRadius,
                                        center = center,
                                        style = Stroke(
                                            width = 3.dp.toPx()
                                        )
                                    )
                                }
                            }
                            closestStop?.let { closest ->
                                val stopId = closest.key
                                if (stopId != selectedMtrStartingStation) {
                                    val position = data.stations[stopId]
                                    if (position != null) {
                                        val center = state.zoomable.realPointToTouchPoint(position, data.dimension)
                                        drawCircle(
                                            color = Color(0xff199fff),
                                            radius = animatedClosestStationRadius,
                                            center = center,
                                            alpha = 0.3F,
                                            style = Fill
                                        )
                                        drawCircle(
                                            color = Color(0xff199fff),
                                            radius = animatedClosestStationRadius,
                                            center = center,
                                            style = Stroke(
                                                width = 3.dp.toPx()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(path)
                .unrestrictedBitmapSize()
                .size(SizeResolver.ORIGINAL)
                .build(),
            onSuccess = { scope.launch { loaded = true } },
            scrollBar = null,
            zoomState = state,
            onTap = { pos ->
                if (loaded) {
                    val clickedPos = state.zoomable.touchPointToRealPoint(pos, data.dimension)
                    val stopId = data.findClickedStations(clickedPos)
                    scope.launch {
                        selectedStopId = stopId
                        sheetState.hide()
                        showingSheet = true
                    }
                }
            },
            onLongPress = { pos ->
                if (loaded) {
                    val clickedPos = state.zoomable.touchPointToRealPoint(pos, data.dimension)
                    val stopId = data.findClickedStations(clickedPos)
                    if (stopId != null) {
                        scope.launch {
                            selectedMtrStartingStation = stopId
                        }
                    }
                }
            },
            contentDescription = if (Shared.language == "en") "MTR System Map" else "港鐵路綫圖"
        )
    }
}

@Composable
fun MTRETADisplayInterface(
    stopId: String,
    stop: Stop,
    sheetInfoTypeState: MutableState<StationInfoSheetType>,
    isPreview: Boolean,
    instance: AppActiveContext,
    extraActions: (@Composable RowScope.() -> Unit)? = null
) {
    var routes: List<Triple<String, Color, ImmutableList<RouteSearchResultEntry>>> by remember(stopId) { mutableStateOf(emptyList()) }
    val etaState: SnapshotStateMap<String, Registry.ETAQueryResult?> = remember { mutableStateMapOf() }
    val errorCounters: SnapshotStateMap<String, Int> = remember { mutableStateMapOf() }

    LaunchedEffect (stopId) {
        routes = Registry.getInstance(instance).findRoutes("", false) { _, r, o ->
            if (o != Operator.MTR) return@findRoutes false
            val stops = r.stops[Operator.MTR]!!
            return@findRoutes stops.contains(stopId)
        }
            .groupBy { it.route!!.routeNumber }
            .map { (k, v) -> Triple(Operator.MTR.getDisplayRouteNumber(k, false), Operator.MTR.getLineColor(k, Color.White), v.asImmutableList()) }
    }
    LaunchedEffect (routes) {
        for (route in routes.asSequence().flatMap { it.third }) {
            launch {
               while (true) {
                   val result = CoroutineScope(Dispatchers.IO).async {
                       Registry.getInstance(instance).buildEtaQuery(stopId, 0, Operator.MTR, route.route!!, instance).query(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
                   }.await()
                   if (result.isConnectionError) {
                       errorCounters[route.routeKey] = (errorCounters[route.routeKey]?: 0) + 1
                       if ((errorCounters[route.routeKey]?: 0) > 1) {
                           etaState[route.routeKey] = result
                       }
                   } else {
                       errorCounters[route.routeKey] = 0
                       etaState[route.routeKey] = result
                   }
                   delay(Shared.ETA_UPDATE_INTERVAL.toLong())
               }
            }
        }
    }

    val pagerState = rememberPagerState { routes.size.coerceAtLeast(1) }
    val scope = rememberCoroutineScope()
    val selectedLineColor = remember { Animatable(Color.Transparent) }
    var init by remember { mutableStateOf(false) }

    LaunchedEffect (routes) {
        pagerState.scrollToPage(routes.indexOf { it.second == lastChosenLine }.takeIf { it >= 0 }?: 0)
    }
    LaunchedEffect (routes, pagerState.currentPage) {
        if (init) {
            val newColor = routes.elementAtOrNull(pagerState.currentPage)?.second?.apply {
                lastChosenLine = this
            }?: Color.Transparent
            if (selectedLineColor.value == Color.Transparent) {
                selectedLineColor.snapTo(newColor)
            } else {
                selectedLineColor.animateTo(
                    targetValue = newColor,
                    animationSpec = tween(250, easing = LinearEasing)
                )
            }
        } else {
            init = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF001F50)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .matchParentSize()
                            .align(Alignment.CenterStart)
                    ) {
                        extraActions?.invoke(this)
                    }
                    PlatformText(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        color = Color(0xFFFFFFFF),
                        text = stop.remarkedName[Shared.language].asContentAnnotatedString().annotatedString,
                        fontSize = 25.sp,
                        textAlign = TextAlign.Center
                    )
                }
                HorizontalDivider(
                    modifier = Modifier
                        .zIndex(100F)
                        .drawWithContent {
                            drawContent()
                            drawCircle(
                                color = Color.White,
                                radius = 9.dp.toPx(),
                                center = center,
                                style = Fill,
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 9.dp.toPx(),
                                center = center,
                                style = Stroke(
                                    width = 3.dp.toPx()
                                )
                            )
                        },
                    thickness = 10.dp,
                    color = selectedLineColor.value
                )
                PlatformTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    iosDivider = { HorizontalDivider() }
                ) {
                    if (routes.isEmpty()) {
                        PlatformTab(
                            selected = true,
                            onClick = { /* do nothing */ },
                            text = {
                                PlatformText(
                                    fontSize = 15F.sp,
                                    lineHeight = 1.1F.em,
                                    maxLines = 1,
                                    text = "-"
                                )
                            }
                        )
                    } else {
                        routes.forEachIndexed { index, (line, color) ->
                            PlatformTab(
                                selected = index == pagerState.currentPage,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = {
                                    PlatformText(
                                        fontSize = 15F.sp,
                                        lineHeight = 1.1F.em,
                                        color = color,
                                        text = line
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        content = { padding ->
            val etaScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollWithScrollbar(
                        state = etaScroll,
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        scrollbarConfig = ScrollBarConfig(
                            indicatorThickness = 4.dp
                        )
                    )
                    .padding(padding),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState,
                    userScrollEnabled = true
                ) {
                    routes.elementAtOrNull(it)?.let { element ->
                        MTRRouteMapETAInterface(element.third, etaState, stopId, instance)
                    }
                }
                HorizontalDivider()
                if (isPreview) {
                    Spacer(modifier = Modifier.size(30.dp))
                    PreviewDetailsButton(instance, stopId)
                    Spacer(modifier = Modifier.size(30.dp))
                } else {
                    MTRRouteMapOptionsInterface(stopId, stop, sheetInfoTypeState, instance)
                }
            }
        }
    )
}

@Composable
fun MTRRouteMapETAInterface(
    routes: ImmutableList<RouteSearchResultEntry>,
    etaState: SnapshotStateMap<String, Registry.ETAQueryResult?>,
    stopId: String,
    instance: AppActiveContext
) {
    val scope = rememberCoroutineScope()
    var freshness by remember { mutableStateOf(true) }

    LaunchedEffect (etaState) {
        while (true) {
            freshness = etaState.values.all { it?.isOutdated() != true }
            delay(500)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (routes.isEmpty()) {
            val line = Registry.ETAQueryResult.NULL_VALUE.getResolvedText(1, Shared.etaDisplayMode, instance).asContentAnnotatedString().annotatedString
            ElevatedCard(
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlatformText(
                        text = "-",
                        fontSize = 21.sp
                    )
                }
                HorizontalDivider()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.fontScaledDp(0.5F))
                    .padding(5.dp),
                contentAlignment = Alignment.TopStart
            ) {
                PlatformText(
                    modifier = Modifier
                        .heightIn(min = 18F.dp)
                        .userMarquee(),
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    fontSize = 18F.sp,
                    color = platformLocalContentColor.adjustAlpha(0.7F),
                    maxLines = userMarqueeMaxLines(),
                    text = line,
                )
            }
        } else {
            for (route in routes) {
                Column(
                    modifier = Modifier.clickable {
                        scope.launch {
                            Registry.getInstance(instance).addLastLookupRoute(route.routeKey, instance)
                            val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                            intent.putExtra("route", route)
                            intent.putExtra("stopId", stopId)
                            if (HistoryStack.historyStack.value.last().screenGroup == AppScreenGroup.ROUTE_STOPS) {
                                instance.startActivity(AppIntent(instance, AppScreen.DUMMY))
                                delay(300)
                            }
                            instance.startActivity(intent)
                        }
                    }
                ) {
                    ElevatedCard(
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlatformText(
                                text = Registry.getInstance(instance).getStopSpecialDestinations(stopId, Operator.MTR, route.route!!, true)[Shared.language],
                                fontSize = 21.sp
                            )
                        }
                        HorizontalDivider()
                    }
                    etaState[route.routeKey]?.let { eta ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.fontScaledDp(0.5F))
                                .padding(5.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            TrainETADisplay(
                                modifier = Modifier,
                                lines = (1..4).map { eta[it] }.asImmutableList(),
                                time = eta.time,
                                etaDisplayMode = Shared.etaDisplayMode,
                                stopId = stopId,
                                co = Operator.MTR,
                                freshness = freshness,
                                instance = instance
                            )
                        }
                    }?: run {
                        val line = Registry.ETAQueryResult.NULL_VALUE.getResolvedText(1, Shared.etaDisplayMode, instance).asContentAnnotatedString().annotatedString
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.fontScaledDp(0.5F))
                                .padding(5.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            PlatformText(
                                modifier = Modifier
                                    .heightIn(min = 18F.dp)
                                    .userMarquee(),
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                fontSize = 18F.sp,
                                color = platformLocalContentColor.adjustAlpha(0.7F),
                                maxLines = 1,
                                text = line,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ReduceDataOmitted::class)
@Composable
fun MTRRouteMapOptionsInterface(
    stopId: String,
    stop: Stop,
    sheetInfoTypeState: MutableState<StationInfoSheetType>,
    instance: AppActiveContext
) {
    val haptic = LocalHapticFeedback.current
    var startingStation by selectedMtrStartingStationState.collectAsStateMultiplatform()
    val serviceTime by remember(stopId) { derivedStateOf { stopId.findMTROpeningTimes(instance) } }
    var sheetInfoType by sheetInfoTypeState
    Column(
        modifier = Modifier
            .padding(vertical = 25.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF001F50))
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PlatformText(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFFFFF),
                text = if (Shared.language == "en") "Service Hours" else "服務時間",
                fontSize = 25.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        PlatformText(
            fontSize = 23F.sp,
            lineHeight = 1.1F.em,
            maxLines = 1,
            text = serviceTime?.let { (open, close) ->
                "${instance.formatTime(open.toLocalDateTime())} - ${instance.formatTime(close.toLocalDateTime())}"
            }?: if (Shared.language == "en") "Depending on Schedule of the Day" else "查看當天時間表"
        )
    }
    Spacer(modifier = Modifier.size(30.dp))
    VerticalGrid(
        modifier = Modifier
            .padding(vertical = 25.dp)
            .fillMaxWidth(),
        columns = 3
    ) {
        OptionButton(
            onClick = { startingStation = stopId },
            enabled = startingStation != stopId,
            icon = PlatformIcons.Outlined.Start,
            title = if (startingStation == stopId) {
                "已設定為起點" withEn "Starting Station"
            } else {
                "設定為起點" withEn "Set Starting Station"
            }
        )
        OptionButton(
            onClick = { sheetInfoType = StationInfoSheetType.FARE },
            enabled = startingStation != null && startingStation != stopId,
            icon = PlatformIcons.Outlined.Paid,
            title = "車費" withEn "Fares",
            subTitle = startingStation?.let { startingStation ->
                ("起點: " withEn "Starting Station: ") + (startingStation.asStop(instance)?.name?: "?".asBilingualText())
            }?: ("未設定起點" withEn "Starting Station not set")
        )
        OptionButton(
            onClick = { sheetInfoType = StationInfoSheetType.OPENING_FIRST_LAST_TRAIN },
            enabled = startingStation != null && startingStation != stopId,
            icon = PlatformIcons.Outlined.Bedtime,
            title = "首/尾班車" withEn "First/Last Train",
            subTitle = startingStation?.let { startingStation ->
                ("起點: " withEn "Starting Station: ") + (startingStation.asStop(instance)?.name?: "?".asBilingualText())
            }?: ("未設定起點" withEn "Starting Station not set")

        )
        OptionButton(
            onClick = instance.handleWebpages(getRedirectToMTRJourneyPlannerUrl(startingStation, stopId, instance), false, haptic.common),
            enabled = startingStation != null,
            icon = PlatformIcons.Outlined.Route,
            title = "港鐵行程指南" withEn "MTR Journey Planner"
        )

        val favouriteStops by Shared.favoriteStops.collectAsStateMultiplatform()
        val favouriteStopAlreadySet by remember(stopId) { derivedStateOf { favouriteStops.hasStop(stopId) } }
        OptionButton(
            onClick = {
                if (favouriteStopAlreadySet) {
                    Registry.getInstance(instance).setFavouriteStops(Shared.favoriteStops.value.toMutableList().apply { removeStop(stopId) }, instance)
                } else {
                    Registry.getInstance(instance).setFavouriteStops(Shared.favoriteStops.value.toMutableList().apply { add(FavouriteStop(stopId, stop, null)) }, instance)
                }
            },
            icon = if (favouriteStopAlreadySet) PlatformIcons.Filled.Star else PlatformIcons.Outlined.StarOutline,
            iconColor = if (favouriteStopAlreadySet) Color.Yellow else null,
            title = "設置為收藏車站" withEn "Add to Favourite Stops"
        )
        OptionButton(
            onClick = {
                instance.startActivity(AppIntent(instance, AppScreen.PDF).apply {
                    putExtra("title", if (Shared.language == "en") "${stop.name.en} Station Layout Map" else "${stop.name.zh}站位置圖")
                    putExtra("url", stopId.getMTRStationLayoutUrl())
                })
            },
            icon = PlatformIcons.Outlined.Train,
            title = "港鐵站位置圖" withEn "MTR Station Layout Map"
        )
        OptionButton(
            onClick = {
                instance.startActivity(AppIntent(instance, AppScreen.PDF).apply {
                    putExtra("title", if (Shared.language == "en") "${stop.name.en} Station Street Map" else "${stop.name.zh}站街道圖")
                    putExtra("url", stopId.getMTRStationStreetMapUrl())
                })
            },
            icon = PlatformIcons.Outlined.Streetview,
            title = "港鐵站街道圖" withEn "MTR Station Street Map"
        )
        OptionButton(
            onClick = instance.handleOpenMaps(stop.location.lat, stop.location.lng, stop.name[Shared.language], false, haptic.common),
            icon = PlatformIcons.Outlined.Map,
            title = "在地圖上顯示" withEn "Open on Maps"
        )
        OptionButton(
            onClick = { sheetInfoType = StationInfoSheetType.BARRIER_FREE },
            icon = PlatformIcons.AutoMirrored.Outlined.Accessible,
            title = "無障礙設施" withEn "Barrier-free Facilities"
        )
    }
}

@OptIn(ReduceDataOmitted::class)
@Composable
fun MTRRouteMapInfoSheetInterface(
    stopId: String,
    stop: Stop,
    sheetInfoType: StationInfoSheetType,
    instance: AppActiveContext
) {
    val startingStation by selectedMtrStartingStationState.collectAsStateMultiplatform()
    when (sheetInfoType) {
        StationInfoSheetType.FARE -> {
            val actionScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(vertical = 5.dp)
                    .fillMaxWidth()
                    .verticalScrollWithScrollbar(
                        state = actionScroll,
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        scrollbarConfig = ScrollBarConfig(
                            indicatorThickness = 4.dp
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
            ) {
                startingStation?.takeIf { startingStation != stopId }?.let { startingId ->
                    val startingStop by remember(startingId) { derivedStateOf { startingId.asStop(instance)!! } }
                    val fareTable by remember(startingId, stopId) { derivedStateOf { startingId.findMTRFares(stopId, instance) } }
                    if (fareTable.containsKey(null)) {
                        PlatformText(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            text = if (Shared.language == "en") {
                                "${startingStop.name.en} to ${stop.name.en}"
                            } else {
                                "${startingStop.name.zh} 往 ${stop.name.zh}"
                            }
                        )
                        TrainFareTableDisplay(
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(platformPrimaryContainerColor)
                                .padding(3.dp),
                            fareTable = fareTable[null]!!.asImmutableMap()
                        )
                    } else {
                        for ((viaStopId, optionFareTable) in fareTable) {
                            val viaStation = viaStopId!!.asStop(instance)!!
                            PlatformText(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                text = if (Shared.language == "en") {
                                    "${startingStop.name.en} to ${stop.name.en} (Via ${viaStation.name.en})"
                                } else {
                                    "${startingStop.name.zh} 往 ${stop.name.zh} (經${viaStation.name.zh})"
                                }
                            )
                            TrainFareTableDisplay(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Operator.MTR.getLineColor("AEL", Color.White).withAlpha(100))
                                    .padding(3.dp),
                                fareTable = optionFareTable.asImmutableMap()
                            )
                        }
                    }
                    PlatformText(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        text = if (Shared.language == "en") {
                            "One Single Journey Ticket is not valid for interchange between lines at Tsim Sha Tsui / East Tsim Sha Tsui station."
                        } else {
                            "持一張單程車票不可在尖沙咀/尖東站轉綫"
                        },
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }?: run {
                    PlatformText(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        text = if (Shared.language == "en") {
                            "No starting station selected"
                        } else {
                            "未選擇起點"
                        }
                    )
                }
            }
        }
        StationInfoSheetType.OPENING_FIRST_LAST_TRAIN -> {
            startingStation?.let { startingId ->
                val startingStop by remember(startingId) { derivedStateOf { startingId.asStop(instance)!! } }
                val actionScroll = rememberScrollState()
                val firstTrain by remember(startingId, stopId) { derivedStateOf { startingId.findMTRFirstTrain(stopId, instance) } }
                val lastTrain by remember(startingId, stopId) { derivedStateOf { startingId.findMTRLastTrain(stopId, instance) } }
                val lastTrainBufferMin by remember(startingId) { derivedStateOf { if (startingId == "QUB") 7 else 5 } }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScrollWithScrollbar(
                            state = actionScroll,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            scrollbarConfig = ScrollBarConfig(
                                indicatorThickness = 4.dp
                            )
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF001F50))
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlatformText(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFFFFF),
                            text = if (Shared.language == "en") "First Train" else "首班車",
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    PlatformText(
                        modifier = Modifier.padding(10.dp),
                        fontSize = 19F.sp,
                        lineHeight = 1.1F.em,
                        text = if (Shared.language == "en") {
                            "${startingStop.name.en} to ${stop.name.en}"
                        } else {
                            "${startingStop.name.zh} 往 ${stop.name.zh}"
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp)
                            .height(90.dp)
                            .background(platformPrimaryContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        PlatformText(
                            fontSize = 25F.sp,
                            lineHeight = 1.1F.em,
                            maxLines = 1,
                            text = firstTrain?.let { instance.formatTime(it.time.toLocalDateTime()) }?: "-"
                        )
                    }
                    firstTrain?.let {
                        PlatformText(
                            modifier = Modifier.padding(10.dp),
                            fontSize = 19F.sp,
                            lineHeight = 1.1F.em,
                            text = if (Shared.language == "en") {
                                "Passengers for the first train must follow the route below."
                            } else {
                                "乘搭首班車必須使用以下轉乘路線"
                            }
                        )
                        TrainPathTableDisplay(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp),
                            path = it.path.asImmutableList(),
                            operator = Operator.MTR,
                            instance = instance
                        )
                    }
                    Spacer(modifier = Modifier.size(30.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF001F50))
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlatformText(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFFFFF),
                            text = if (Shared.language == "en") "Last Train" else "尾班車",
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    PlatformText(
                        modifier = Modifier.padding(10.dp),
                        fontSize = 19F.sp,
                        lineHeight = 1.1F.em,
                        text = if (Shared.language == "en") {
                            "${startingStop.name.en} to ${stop.name.en}"
                        } else {
                            "${startingStop.name.zh} 往 ${stop.name.zh}"
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp)
                            .height(90.dp)
                            .background(platformPrimaryContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        PlatformText(
                            fontSize = 25F.sp,
                            lineHeight = 1.1F.em,
                            maxLines = 1,
                            text = lastTrain?.let { instance.formatTime(it.time.toLocalDateTime()) }?: "-"
                        )
                    }
                    lastTrain?.let {
                        PlatformText(
                            modifier = Modifier.padding(10.dp),
                            fontSize = 19F.sp,
                            lineHeight = 1.1F.em,
                            text = if (Shared.language == "en") {
                                "Passengers for the last train must follow the route below and enter the station at least $lastTrainBufferMin minutes before the train departs."
                            } else {
                                "乘搭尾班車必須使用以下轉乘路線並須於列車開出前最少${lastTrainBufferMin}分鐘入站"
                            }
                        )
                        TrainPathTableDisplay(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp),
                            path = it.path.asImmutableList(),
                            operator = Operator.MTR,
                            instance = instance
                        )
                    }
                    Spacer(modifier = Modifier.size(30.dp))
                }
            }?: run {
                PlatformText(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    text = if (Shared.language == "en") {
                        "No starting station selected"
                    } else {
                        "未選擇起點"
                    }
                )
            }
        }
        StationInfoSheetType.BARRIER_FREE -> TrainStationBarrierFreeDisplay(
            modifier = Modifier.fillMaxWidth(),
            stopId = stopId,
            instance = instance
        )
        else -> PlatformText(sheetInfoType.name)
    }
}

private val lrtRouteMapZoomState: MutableStateFlow<ZoomState> = MutableStateFlow(ZoomState(0.8F, null))
private val lrtRouteMapLocationJumpedState: MutableStateFlow<Boolean> = MutableStateFlow(false)
private val lrtRouteMapSelectedStopIdState: MutableStateFlow<String?> = MutableStateFlow(null)
private val lrtRouteMapShowingSheetState: MutableStateFlow<Boolean> = MutableStateFlow(false)
private val selectedLrtStartingStationState: MutableStateFlow<String?> = MutableStateFlow(null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LRTRouteMapInterface(
    instance: AppActiveContext,
    location: Coordinates?,
    stopLaunch: String?,
    isPreview: Boolean,
    visible: Boolean
) {
    var zoomState by lrtRouteMapZoomState.collectAsStateMultiplatform()
    val state = rememberCoilZoomState()
    var lightRailRouteMapData by lightRailRouteMapDataState.collectAsStateMultiplatform()
    var allStops: Map<String, Stop?> by remember { mutableStateOf(lightRailRouteMapData?.let { it.stations.keys.associateWith { s -> s.asStop(instance) } }.orEmpty()) }
    val closestStopState: MutableState<Map.Entry<String, Stop?>?> = remember { mutableStateOf(null) }
    var closestStop by closestStopState

    val sheetState = rememberPlatformModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedStopId by lrtRouteMapSelectedStopIdState.collectAsStateMultiplatform()
    var showingSheet by lrtRouteMapShowingSheetState.collectAsStateMultiplatform()
    var locationJumped by lrtRouteMapLocationJumpedState.collectAsStateMultiplatform()
    var selectedLrtStartingStation by selectedLrtStartingStationState.collectAsStateMultiplatform()

    val lrtSheetInfoTypeState = rememberSaveable { mutableStateOf(StationInfoSheetType.NONE) }
    var lrtSheetInfoType by lrtSheetInfoTypeState
    val lrtSheetInfoState = rememberPlatformModalBottomSheetState()

    val loadedState = remember { mutableStateOf(false) }
    val loaded by loadedState

    var setScale by remember(loaded) { mutableStateOf(false) }

    LaunchedEffect (Unit) {
        state.zoomable.apply {
            threeStepScale = true
            mouseWheelScaleScrollDeltaConverter = { it * 0.004F }
            scalesCalculator = ScalesCalculator.predefined(
                minScale = 0.8F,
                mediumScale = 1F,
                maxScale = 1.5F
            )
        }
    }
    LaunchedEffect (loaded) {
        if (loaded) {
            state.zoomable.apply {
                delay(50)
                zoomState.offset?.let {
                    scale(zoomState.scale)
                    offset(it)
                }?: run {
                    val dimension = lightRailRouteMapData?.dimension?: Size.Zero
                    val offset = state.zoomable.realPointToContentPoint(dimension.center, dimension)
                    locate(offset.round(), zoomState.scale)
                }
                setScale = true
            }
        }
    }
    LaunchedEffect (setScale, state.zoomable.transform.scaleX, state.zoomable.transform.offset) {
        if (setScale) {
            zoomState = ZoomState(state.zoomable.transform.scaleX, state.zoomable.transform.offset)
        }
    }
    LaunchedEffect (Unit) {
        lightRailRouteMapLoaded.value = true
    }
    LaunchedEffect (Unit) {
        if (lightRailRouteMapData == null) {
            val data = RouteMapData.fromFile("routemaps/light_rail_system_map.json")
            lightRailRouteMapData = data
            allStops = data.stations.keys.associateWith { it.asStop(instance) }
        }
    }
    LaunchedEffect (setScale, allStops, location) {
        if (setScale && location != null && allStops.isNotEmpty()) {
            val stop = allStops.entries.asSequence().filter { it.value != null }.minByOrNull { it.value!!.location.distance(location) }
            closestStop = stop
            if (stop != null) {
                if (selectedLrtStartingStation == null) {
                    selectedLrtStartingStation = stop.key
                }
                if (!locationJumped && stopLaunch == null) {
                    lightRailRouteMapData?.let { data ->
                        val position = data.stations[stop.key]!!
                        val offset = state.zoomable.realPointToContentPoint(position, data.dimension)
                        state.zoomable.locate(offset.round(), animated = true)
                        locationJumped = true
                    }
                }
            }
        }
    }
    LaunchedEffect (setScale, stopLaunch) {
        if (setScale) {
            stopLaunch?.let { stopLaunch ->
                while (lightRailRouteMapData == null) delay(10)
                lightRailRouteMapData?.let { data ->
                    val position = data.stations[stopLaunch]
                    if (position != null) {
                        val offset = state.zoomable.realPointToContentPoint(position, data.dimension)
                        state.zoomable.locate(offset.round(), animated = true)
                        selectedStopId = stopLaunch
                        sheetState.hide()
                        showingSheet = true
                    }
                }
            }
        }
    }
    LaunchedEffect (showingSheet, visible) {
        if (visible) {
            delay(1000)
            if (showingSheet && sheetState.currentValue == SheetValue.Hidden) {
                showingSheet = false
            }
        }
    }

    LRTRouteMapMapInterface(state, sheetState, loadedState, closestStopState, setScale)

    selectedStopId?.let { stopId ->
        val stop by remember(stopId) { derivedStateOf { stopId.asStop(instance)!! } }
        if (visible && showingSheet) {
            PlatformModalBottomSheet(
                onDismissRequest = { showingSheet = false },
                sheetState = sheetState,
                desktopCloseColor = Color(0xFF001F50)
            ) {
                LRTETADisplayInterface(instance, stopId, stop, lrtSheetInfoTypeState, isPreview)
            }
        }
        if (visible && lrtSheetInfoType.showing) {
            PlatformModalBottomSheet(
                onDismissRequest = { lrtSheetInfoType = StationInfoSheetType.NONE },
                sheetState = lrtSheetInfoState,
                desktopCloseColor = Color(0xFF001F50).takeIf { lrtSheetInfoType.textCloseColor }
            ) {
                Box(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    LRTETADisplayInfoSheetInterface(stopId, stop, lrtSheetInfoType, instance)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, InternalResourceApi::class)
@Composable
fun LRTRouteMapMapInterface(
    state: CoilZoomState,
    sheetState: SheetState,
    loadedState: MutableState<Boolean>,
    closestStopState: MutableState<Map.Entry<String, Stop?>?>,
    show: Boolean
) {
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "ClosestStationIndicator")
    val animatedClosestStationRadius by infiniteTransition.animateFloat(
        initialValue = 100F,
        targetValue = 140F,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ClosestStationIndicator"
    )
    val lightRailRouteMapData by lightRailRouteMapDataState.collectAsStateMultiplatform()
    var selectedStopId by lrtRouteMapSelectedStopIdState.collectAsStateMultiplatform()
    var showingSheet by lrtRouteMapShowingSheetState.collectAsStateMultiplatform()
    var selectedLrtStartingStation by selectedLrtStartingStationState.collectAsStateMultiplatform()
    val closestStop by closestStopState
    var loaded by loadedState
    var isPressing by remember { mutableStateOf(false) }

    val path = getResourceUri("routemaps/light_rail_system_map${if (Shared.theme.isDarkMode) "_dark" else ""}.png")

    lightRailRouteMapData?.let { data ->
        CoilZoomAsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .composed {
                    if (loaded) {
                        var hoveringStation by remember { mutableStateOf(false) }
                        pointerInput(Unit) {
                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    when (event.type) {
                                        PointerEventType.Move -> {
                                            val change = event.changes[0]
                                            val pos = change.position
                                            val clickedPos = state.zoomable.touchPointToRealPoint(pos, data.dimension)
                                            hoveringStation = data.findClickedStations(clickedPos) != null
                                        }
                                        PointerEventType.Press -> isPressing = true
                                        PointerEventType.Release -> isPressing = false
                                    }
                                }
                            }
                        }.pointerHoverIcon(if (hoveringStation) PointerIcon.Hand else if (isPressing) PointerIcon.Grabbing else PointerIcon.Grab)
                    } else {
                        this
                    }
                }
                .drawWithContent {
                    if (show) {
                        drawContent()
                        if (loaded) {
                            selectedLrtStartingStation?.let { selected ->
                                val position = data.stations[selected]
                                if (position != null) {
                                    val center = state.zoomable.realPointToTouchPoint(position, data.dimension)
                                    drawCircle(
                                        color = Color(0xff53ff19),
                                        radius = animatedClosestStationRadius,
                                        center = center,
                                        alpha = 0.3F,
                                        style = Fill
                                    )
                                    drawCircle(
                                        color = Color(0xff53ff19),
                                        radius = animatedClosestStationRadius,
                                        center = center,
                                        style = Stroke(
                                            width = 3.dp.toPx()
                                        )
                                    )
                                }
                            }
                            closestStop?.let { closest ->
                                val stopId = closest.key
                                if (stopId != selectedLrtStartingStation) {
                                    val position = data.stations[stopId]
                                    if (position != null) {
                                        val center = state.zoomable.realPointToTouchPoint(position, data.dimension)
                                        drawCircle(
                                            color = Color(0xff199fff),
                                            radius = animatedClosestStationRadius,
                                            center = center,
                                            alpha = 0.3F,
                                            style = Fill
                                        )
                                        drawCircle(
                                            color = Color(0xff199fff),
                                            radius = animatedClosestStationRadius,
                                            center = center,
                                            style = Stroke(
                                                width = 3.dp.toPx()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(path)
                .unrestrictedBitmapSize()
                .size(SizeResolver.ORIGINAL)
                .build(),
            onSuccess = { scope.launch { loaded = true } },
            scrollBar = null,
            zoomState = state,
            onTap = { pos ->
                if (loaded) {
                    val clickedPos = state.zoomable.touchPointToRealPoint(pos, data.dimension)
                    val stopId = data.findClickedStations(clickedPos)
                    scope.launch {
                        selectedStopId = stopId
                        sheetState.hide()
                        showingSheet = true
                    }
                }
            },
            onLongPress = { pos ->
                if (loaded) {
                    val clickedPos = state.zoomable.touchPointToRealPoint(pos, data.dimension)
                    val stopId = data.findClickedStations(clickedPos)
                    if (stopId != null) {
                        scope.launch {
                            selectedLrtStartingStation = stopId
                        }
                    }
                }
            },
            contentDescription = if (Shared.language == "en") "Light Rail Route Map" else "輕鐵路綫圖"
        )
    }
}

@Composable
fun LRTETADisplayInterface(
    instance: AppActiveContext,
    stopId: String,
    stop: Stop,
    sheetInfoTypeState: MutableState<StationInfoSheetType>,
    isPreview: Boolean,
    extraActions: (@Composable RowScope.() -> Unit)? = null
) {
    var placeholderRoute: RouteSearchResultEntry? by remember(stopId) { mutableStateOf(null) }
    var etaState: Registry.ETAQueryResult? by remember { mutableStateOf(null) }
    var errorCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect (stopId) {
        placeholderRoute = Registry.getInstance(instance).findRoutes("", false) { _, r, o ->
            if (o != Operator.LRT) return@findRoutes false
            val stops = r.stops[Operator.LRT]!!
            if (!stops.contains(stopId)) return@findRoutes false
            return@findRoutes stops.last() != stopId
        }.first()
    }
    LaunchedEffect (placeholderRoute) {
        placeholderRoute?.let { placeholderRoute ->
            while (true) {
                val result = CoroutineScope(Dispatchers.IO).async {
                    Registry.getInstance(instance).buildEtaQuery(stopId, 0, Operator.LRT, placeholderRoute.route!!, instance, Registry.EtaQueryOptions(lrtAllMode = true)).query(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
                }.await()
                if (result.isConnectionError) {
                    errorCounter++
                    if (errorCounter > 1) {
                        etaState = result
                    }
                } else {
                    errorCounter = 0
                    etaState = result
                }
                delay(Shared.ETA_UPDATE_INTERVAL.toLong())
            }
        }
    }
    RestartEffect {
        placeholderRoute?.let { placeholderRoute ->
            val result = CoroutineScope(Dispatchers.IO).async {
                Registry.getInstance(instance).buildEtaQuery(stopId, 0, Operator.LRT, placeholderRoute.route!!, instance, Registry.EtaQueryOptions(lrtAllMode = true)).query(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
            }.await()
            if (result.isConnectionError) {
                errorCounter++
                if (errorCounter > 1) {
                    etaState = result
                }
            } else {
                errorCounter = 0
                etaState = result
            }
        }
    }

    val pagerState = rememberPagerState { lrtRouteDisplayModeItems.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Operator.LRT.getOperatorColor(Color.White)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .matchParentSize()
                            .align(Alignment.CenterStart)
                    ) {
                        extraActions?.invoke(this)
                    }
                    PlatformText(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        color = Color(0xFF001F50),
                        text = stop.remarkedName[Shared.language].asContentAnnotatedString().annotatedString,
                        fontSize = 25.sp,
                        textAlign = TextAlign.Center
                    )
                }
                PlatformTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    iosDivider = { HorizontalDivider() }
                ) {
                    lrtRouteDisplayModeItems.forEachIndexed { index, item ->
                        PlatformTab(
                            selected = index == pagerState.currentPage,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                PlatformText(
                                    fontSize = 15F.sp,
                                    lineHeight = 1.1F.em,
                                    maxLines = 1,
                                    text = item.title[Shared.language]
                                )
                            }
                        )
                    }
                }
            }
        },
        content = { padding ->
            val etaScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollWithScrollbar(
                        state = etaScroll,
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        scrollbarConfig = ScrollBarConfig(
                            indicatorThickness = 4.dp
                        )
                    )
                    .padding(padding),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = true
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        when (it) {
                            0 -> LRTETADisplayByPlatformInterface(instance, etaState, stopId)
                            1 -> LRTETADisplayByRouteInterface(instance, etaState, stopId)
                            else -> PlatformText(lrtRouteDisplayModeItems[it].title[Shared.language])
                        }
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.size(30.dp))
                if (isPreview) {
                    PreviewDetailsButton(instance, stopId)
                    Spacer(modifier = Modifier.size(30.dp))
                } else {
                    LRTETADisplayOptionsInterface(instance, stopId, stop, sheetInfoTypeState)
                }
            }
        }
    )
}

@Composable
fun LRTETADisplayByPlatformInterface(
    instance: AppActiveContext,
    etaState: Registry.ETAQueryResult?,
    stopId: String
) {
    val platformSorted: Map<Int, List<Registry.ETALineEntry>> by remember(etaState) { derivedStateOf {
        etaState?.let {
            val byPlatform: MutableMap<Int, MutableList<Registry.ETALineEntry>> = mutableMapOf()
            for (i in 1..Int.MAX_VALUE) {
                val entry = etaState.rawLines[i]
                if (entry == null) {
                    break
                } else if (entry.platform >= 1) {
                    byPlatform[entry.platform] = mutableListOf()
                }
            }
            for (i in 1..Int.MAX_VALUE) {
                val entry = etaState.rawLines[i]
                if (entry == null) {
                    break
                } else if (entry.platform < 1) {
                    byPlatform.keys.forEach { byPlatform[it]!!.add(entry) }
                } else if (byPlatform.containsKey(entry.platform)) {
                    byPlatform[entry.platform]!!.add(entry)
                }
            }
            byPlatform.asSequence().sortedBy { it.key }.associate { it.toPair() }
        }.orEmpty()
    } }
    var freshness by remember { mutableStateOf(true) }

    LaunchedEffect (etaState) {
        while (true) {
            freshness = etaState?.isOutdated() != true
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        if (platformSorted.isEmpty()) {
            val line = etaState.getResolvedText(1, Shared.etaDisplayMode, instance).asContentAnnotatedString().annotatedString
            ElevatedCard(
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlatformText(
                        text = if (Shared.language == "en") "All Platforms" else "所有月台",
                        fontSize = 21.sp
                    )
                }
                HorizontalDivider()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlatformText(
                    modifier = Modifier
                        .heightIn(min = 18F.dp)
                        .userMarquee(),
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    fontSize = 18F.sp,
                    color = platformLocalContentColor.adjustAlpha(if (etaState == null) 0.7F else 1F),
                    maxLines = 1,
                    text = line,
                )
            }
        } else {
            for (i in platformSorted.keys) {
                ElevatedCard(
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlatformText(
                            text = if (Shared.language == "en") {
                                "Platform $i"
                            } else {
                                "${i}號月台"
                            },
                            fontSize = 21.sp
                        )
                    }
                    HorizontalDivider()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrainETADisplay(
                        modifier = Modifier,
                        lines = platformSorted[i]!!.asImmutableList(),
                        time = etaState?.time?: currentTimeMillis(),
                        etaDisplayMode = Shared.etaDisplayMode,
                        stopId = stopId,
                        co = Operator.LRT,
                        freshness = freshness,
                        instance = instance
                    )
                }
            }
        }
    }
}

@Composable
fun LRTETADisplayByRouteInterface(
    instance: AppActiveContext,
    etaState: Registry.ETAQueryResult?,
    stopId: String
) {
    val routeSorted: Map<String, List<Registry.ETALineEntry>> by remember(etaState) { derivedStateOf {
        etaState?.let {
            val byRouteNumber: MutableMap<String, MutableList<Registry.ETALineEntry>> = mutableMapOf()
            for (i in 1..Int.MAX_VALUE) {
                val entry = etaState.rawLines[i]
                if (entry == null) {
                    break
                } else if (entry.platform >= 1) {
                    byRouteNumber[entry.routeNumber] = mutableListOf()
                }
            }
            for (i in 1..Int.MAX_VALUE) {
                val entry = etaState.rawLines[i]
                if (entry == null) {
                    break
                } else if (entry.platform < 1) {
                    byRouteNumber.keys.forEach { byRouteNumber[it]!!.add(entry) }
                } else if (byRouteNumber.containsKey(entry.routeNumber)) {
                    byRouteNumber[entry.routeNumber]!!.add(entry)
                }
            }
            byRouteNumber.asSequence()
                .sortedWith { a, b -> a.key.compareRouteNumber(b.key) }
                .associate { it.toPair() }
        }.orEmpty()
    } }
    var freshness by remember { mutableStateOf(true) }

    LaunchedEffect (etaState) {
        while (true) {
            freshness = etaState?.isOutdated() != true
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        if (routeSorted.isEmpty()) {
            val line = etaState.getResolvedText(1, Shared.etaDisplayMode, instance).asContentAnnotatedString().annotatedString
            ElevatedCard(
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlatformText(
                        text = if (Shared.language == "en") "All Routes" else "所有路線",
                        fontSize = 21.sp
                    )
                }
                HorizontalDivider()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlatformText(
                    modifier = Modifier
                        .heightIn(min = 18F.dp)
                        .userMarquee(),
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    fontSize = 18F.sp,
                    color = platformLocalContentColor.adjustAlpha(if (etaState == null) 0.7F else 1F),
                    maxLines = 1,
                    text = line,
                )
            }
        } else {
            for (routeNumber in routeSorted.keys) {
                ElevatedCard(
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.Start)
                    ) {
                        val size = "614P".renderedSize(19.sp)
                        PlatformText(
                            text = if (Shared.language == "en") "Route" else "路線",
                            fontSize = 21.sp
                        )
                        Box(
                            modifier = Modifier
                                .requiredSize(
                                    width = size.size.width.equivalentDp + 10.sp.dp,
                                    height = size.size.height.equivalentDp + 3.sp.dp,
                                )
                                .border(
                                    width = 3.sp.dp,
                                    color = Operator.LRT.getLineColor(routeNumber, Color.Unspecified),
                                    shape = RoundedCornerShape(size.size.width.equivalentDp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            PlatformText(
                                text = routeNumber,
                                fontSize = 19.sp
                            )
                        }
                    }
                    HorizontalDivider()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrainETADisplay(
                        modifier = Modifier,
                        lines = routeSorted[routeNumber]!!.asImmutableList(),
                        time = etaState?.time?: currentTimeMillis(),
                        etaDisplayMode = Shared.etaDisplayMode,
                        stopId = stopId,
                        co = Operator.LRT,
                        freshness = freshness,
                        instance = instance
                    )
                }
            }
        }
    }
}

@Composable
fun LRTETADisplayOptionsInterface(
    instance: AppActiveContext,
    stopId: String,
    stop: Stop,
    sheetInfoTypeState: MutableState<StationInfoSheetType>
) {
    val haptic = LocalHapticFeedback.current
    var startingStation by selectedLrtStartingStationState.collectAsStateMultiplatform()
    var sheetInfoType by sheetInfoTypeState

    VerticalGrid(
        modifier = Modifier
            .padding(vertical = 25.dp)
            .fillMaxWidth(),
        columns = 3
    ) {
        OptionButton(
            onClick = { startingStation = stopId },
            enabled = startingStation != stopId,
            icon = PlatformIcons.Outlined.Start,
            title = if (startingStation == stopId) {
                "已設定為起點" withEn "Starting Station"
            } else {
                "設定為起點" withEn "Set Starting Station"
            }
        )
        OptionButton(
            onClick = { sheetInfoType = StationInfoSheetType.FARE },
            enabled = startingStation != null && startingStation != stopId,
            icon = PlatformIcons.Outlined.Paid,
            title = "車費" withEn "Fares",
            subTitle = startingStation?.let { startingStation ->
                ("起點: " withEn "Starting Station: ") + (startingStation.asStop(instance)?.name?: "?".asBilingualText())
            }?: ("未設定起點" withEn "Starting Station not set")
        )
        OptionButton(
            onClick = { sheetInfoType = StationInfoSheetType.OPENING_FIRST_LAST_TRAIN },
            enabled = startingStation != null && startingStation != stopId,
            icon = PlatformIcons.Outlined.Bedtime,
            title = "首/尾班車" withEn "First/Last Train",
            subTitle = startingStation?.let { startingStation ->
                ("起點: " withEn "Starting Station: ") + (startingStation.asStop(instance)?.name?: "?".asBilingualText())
            }?: ("未設定起點" withEn "Starting Station not set")
        )
        OptionButton(
            onClick = instance.handleWebpages(getRedirectToMTRJourneyPlannerUrl(startingStation, stopId, instance), false, haptic.common),
            enabled = startingStation != null,
            icon = PlatformIcons.Outlined.Route,
            title = "港鐵行程指南" withEn "MTR Journey Planner"
        )
        val favouriteStops by Shared.favoriteStops.collectAsStateMultiplatform()
        val favouriteStopAlreadySet by remember(stopId) { derivedStateOf { favouriteStops.hasStop(stopId) } }
        OptionButton(
            onClick = {
                if (favouriteStopAlreadySet) {
                    Registry.getInstance(instance).setFavouriteStops(Shared.favoriteStops.value.toMutableList().apply { removeStop(stopId) }, instance)
                } else {
                    Registry.getInstance(instance).setFavouriteStops(Shared.favoriteStops.value.toMutableList().apply { add(FavouriteStop(stopId, stop, null)) }, instance)
                }
            },
            icon = if (favouriteStopAlreadySet) PlatformIcons.Filled.Star else PlatformIcons.Outlined.StarOutline,
            title = "設置為收藏車站" withEn "Add to Favourite Stops"
        )
        OptionButton(
            onClick = instance.handleOpenMaps(stop.location.lat, stop.location.lng, stop.name[Shared.language], false, haptic.common),
            icon = PlatformIcons.Outlined.Map,
            title = "在地圖上顯示" withEn "Open on Maps",
        )
    }
}

@OptIn(ReduceDataOmitted::class)
@Composable
fun LRTETADisplayInfoSheetInterface(
    stopId: String,
    stop: Stop,
    sheetInfoType: StationInfoSheetType,
    instance: AppActiveContext
) {
    val startingStation by selectedLrtStartingStationState.collectAsStateMultiplatform()
    when (sheetInfoType) {
        StationInfoSheetType.FARE -> {
            val actionScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(vertical = 5.dp)
                    .fillMaxWidth()
                    .verticalScrollWithScrollbar(
                        state = actionScroll,
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        scrollbarConfig = ScrollBarConfig(
                            indicatorThickness = 4.dp
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
            ) {
                startingStation?.takeIf { startingStation != stopId }?.let { startingId ->
                    val startingStop by remember(startingId) { derivedStateOf { startingId.asStop(instance)!! } }
                    val fareTable by remember(startingId, stopId) { derivedStateOf { startingId.findLRTFares(stopId, instance) } }
                    PlatformText(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        text = if (Shared.language == "en") {
                            "${startingStop.name.en} to ${stop.name.en}"
                        } else {
                            "${startingStop.name.zh} 往 ${stop.name.zh}"
                        }
                    )
                    TrainFareTableDisplay(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Operator.LRT.getOperatorColor(Color.White).withAlpha(100))
                            .padding(3.dp),
                        fareTable = fareTable.asImmutableMap()
                    )
                    PlatformText(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        text = if (Shared.language == "en") {
                            "Each Octopus or single journey ticket is valid for a single journey from the stop where the Octopus is validated or ticket is issued to one other stop in a single direction without repeating any stop.\n\n" +
                                    "A passenger is required to re-validate Octopus or buy another appropriate single journey ticket for return or another journey (including all circular routes)."
                        } else {
                            "已確認之八達通或單程車票祇適用於從確認入站或購票車站起，作單一方向乘車前往另一車站，期間不可重複車站\n\n乘客在回程或再乘車時(包括所有循環路綫)，必須重新確認八達通或另外購買合適車票"
                        },
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }?: run {
                    PlatformText(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        text = if (Shared.language == "en") {
                            "No starting station selected"
                        } else {
                            "未選擇起點"
                        }
                    )
                }
            }
        }
        StationInfoSheetType.OPENING_FIRST_LAST_TRAIN -> {
            startingStation?.let { startingId ->
                val startingStop by remember(startingId) { derivedStateOf { startingId.asStop(instance)!! } }
                val actionScroll = rememberScrollState()
                val firstTrain by remember(startingId, stopId) { derivedStateOf { startingId.findLRTFirstTrain(stopId, instance) } }
                val lastTrain by remember(startingId, stopId) { derivedStateOf { startingId.findLRTLastTrain(stopId, instance) } }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScrollWithScrollbar(
                            state = actionScroll,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            scrollbarConfig = ScrollBarConfig(
                                indicatorThickness = 4.dp
                            )
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Operator.LRT.getOperatorColor(Color.White))
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlatformText(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF001F50),
                            text = if (Shared.language == "en") "First Train" else "首班車",
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    PlatformText(
                        modifier = Modifier.padding(10.dp),
                        fontSize = 19F.sp,
                        lineHeight = 1.1F.em,
                        text = if (Shared.language == "en") {
                            "${startingStop.name.en} to ${stop.name.en}"
                        } else {
                            "${startingStop.name.zh} 往 ${stop.name.zh}"
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp)
                            .height(90.dp)
                            .background(platformPrimaryContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        PlatformText(
                            fontSize = 25F.sp,
                            lineHeight = 1.1F.em,
                            maxLines = 1,
                            text = firstTrain?.let { instance.formatTime(it.time.toLocalDateTime()) }?: "-"
                        )
                    }
                    firstTrain?.let {
                        PlatformText(
                            modifier = Modifier.padding(10.dp),
                            fontSize = 19F.sp,
                            lineHeight = 1.1F.em,
                            text = if (Shared.language == "en") {
                                "Passengers for the first train must follow the route below."
                            } else {
                                "乘搭首班車必須使用以下轉乘路線"
                            }
                        )
                        TrainPathTableDisplay(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp),
                            path = it.path.asImmutableList(),
                            operator = Operator.LRT,
                            instance = instance
                        )
                    }
                    Spacer(modifier = Modifier.size(30.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Operator.LRT.getOperatorColor(Color.White))
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlatformText(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF001F50),
                            text = if (Shared.language == "en") "Last Train" else "尾班車",
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    PlatformText(
                        modifier = Modifier.padding(10.dp),
                        fontSize = 19F.sp,
                        lineHeight = 1.1F.em,
                        text = if (Shared.language == "en") {
                            "${startingStop.name.en} to ${stop.name.en}"
                        } else {
                            "${startingStop.name.zh} 往 ${stop.name.zh}"
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 30.dp)
                            .height(90.dp)
                            .background(platformPrimaryContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        PlatformText(
                            fontSize = 25F.sp,
                            lineHeight = 1.1F.em,
                            maxLines = 1,
                            text = lastTrain?.let { instance.formatTime(it.time.toLocalDateTime()) }?: "-"
                        )
                    }
                    lastTrain?.let {
                        PlatformText(
                            modifier = Modifier.padding(10.dp),
                            fontSize = 19F.sp,
                            lineHeight = 1.1F.em,
                            text = if (Shared.language == "en") {
                                "Passengers for the last train must follow the route below."
                            } else {
                                "乘搭尾班車必須使用以下轉乘路線"
                            }
                        )
                        TrainPathTableDisplay(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp),
                            path = it.path.asImmutableList(),
                            operator = Operator.LRT,
                            instance = instance
                        )
                    }
                    Spacer(modifier = Modifier.size(30.dp))
                }
            }?: run {
                PlatformText(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    text = if (Shared.language == "en") {
                        "No starting station selected"
                    } else {
                        "未選擇起點"
                    }
                )
            }
        }
        else -> PlatformText(sheetInfoType.name)
    }
}

@OptIn(ReduceDataOmitted::class)
@Composable
fun TrainStationBarrierFreeDisplay(
    modifier: Modifier,
    stopId: String,
    instance: AppActiveContext
) {
    val items by remember(stopId) { derivedStateOf {
        stopId.getMTRStationBarrierFree(instance).entries.asSequence()
            .map { it.key.getStationBarrierFreeDetails(instance) to it.value }
            .mapNotNull { p -> p.first?.let { it to p.second } }
            .groupBy { it.first.category }
    } }
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollWithScrollbar(
                state = scroll,
                flingBehavior = ScrollableDefaults.flingBehavior(),
                scrollbarConfig = ScrollBarConfig(
                    indicatorThickness = 4.dp
                )
            ),
    ) {
        for ((id, category) in getMTRBarrierFreeCategories(instance)) {
            val categoryItems = items[id]
            if (categoryItems.isNotNullAndNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    PlatformText(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 1.1.em,
                        fontSize = 20.sp,
                        text = category.name[Shared.language]
                    )
                }
                HorizontalDivider()
                for (categoryItem in categoryItems) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlatformText(
                            modifier = Modifier.weight(1F),
                            lineHeight = 1.1.em,
                            fontSize = 17.sp,
                            text = categoryItem.first.name[Shared.language]
                        )
                        PlatformText(
                            modifier = Modifier.weight(1F),
                            lineHeight = 1.1.em,
                            fontSize = 17.sp,
                            text = ((categoryItem.second.location ?: ("可用" withEn "Available")))[Shared.language]
                        )
                    }
                    HorizontalDivider()
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun TrainFareTableDisplay(
    modifier: Modifier,
    fareTable: ImmutableMap<FareType, Fare>
) {
    val columns = remember {
        listOf(
            TableColumn(
                width = TableColumnWidth.Weight(1F),
                alignment = Alignment.Start
            ),
            TableColumn(
                width = TableColumnWidth.Wrap,
                alignment = Alignment.CenterHorizontally
            ),
            TableColumn(
                width = TableColumnWidth.Wrap,
                alignment = Alignment.CenterHorizontally
            )
        )
    }
    Table(
        modifier = modifier,
        columnsCount = 3,
        columns = { columns[it] },
        rowSpacing = 5.dp,
        columnSpacing = 5.dp,
        rowDivider = { HorizontalDivider() },
        rows = { TableRow(TableRowAlignment.Vertical(Alignment.CenterVertically)) }
    ) {
        PlatformText(
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Bold,
            lineHeight = 1.1.em,
            fontSize = 17.sp,
            text = if (Shared.language == "en") "Fares" else "車費"
        )
        PlatformText(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            lineHeight = 1.1.em,
            fontSize = 17.sp,
            text = TicketCategory.OCTO.displayName[Shared.language]
        )
        PlatformText(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            lineHeight = 1.1.em,
            fontSize = 17.sp,
            text = TicketCategory.SINGLE.displayName[Shared.language]
        )
        for (fareCategory in FareCategory.entries) {
            PlatformText(
                textAlign = TextAlign.Start,
                lineHeight = 1.1.em,
                fontSize = 17.sp,
                text = fareCategory.displayName[Shared.language]
            )
            PlatformText(
                textAlign = TextAlign.Center,
                lineHeight = 1.1.em,
                fontSize = 17.sp,
                text = "$${fareTable.findFare(fareCategory, TicketCategory.OCTO)?: "-"}"
            )
            PlatformText(
                textAlign = TextAlign.Center,
                lineHeight = 1.1.em,
                fontSize = 17.sp,
                text = "$${fareTable.findFare(fareCategory, TicketCategory.SINGLE)?: "-"}"
            )
        }
    }
}

@Composable
fun TrainETADisplay(
    modifier: Modifier,
    lines: ImmutableList<Registry.ETALineEntry>,
    time: Long,
    etaDisplayMode: ETADisplayMode,
    stopId: String,
    co: Operator,
    freshness: Boolean,
    instance: AppActiveContext
) {
    val scope = rememberCoroutineScope()
    val resolvedText by remember(lines, etaDisplayMode) { derivedStateOf { (1..lines.size).associateWith { lines.getResolvedText(it, etaDisplayMode, time, instance) } } }

    val hasPlatform by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.platform.isNotEmpty() } } }
    val hasRouteNumber by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.routeNumber.isNotEmpty() } } }
    val hasDestination by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.destination.isNotEmpty() } } }
    val hasCarts by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.carts.isNotEmpty() } } }
    val hasClockTime by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.clockTime.isNotEmpty() } } }
    val hasTime by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.time.isNotEmpty() } } }
    val hasOperator by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.operator.isNotEmpty() } } }
    val hasRemark by remember(resolvedText) { derivedStateOf { resolvedText.any { it.value.remark.isNotEmpty() } } }

    val columns by remember(resolvedText) { derivedStateOf { buildList {
        if (hasPlatform) add(TableColumn(width = TableColumnWidth.Wrap))
        if (hasRouteNumber) add(TableColumn(width = TableColumnWidth.Wrap))
        if (hasDestination) add(TableColumn(width = TableColumnWidth.Weight(1F)))
        if (hasCarts) add(TableColumn(width = TableColumnWidth.Wrap))
        if (hasClockTime) add(TableColumn(width = TableColumnWidth.Wrap, alignment = Alignment.End))
        if (hasTime) add(TableColumn(
            width = TableColumnWidth.Max(TableColumnWidth.Wrap, TableColumnWidth.Fixed(80.dp)),
            alignment = if (lines.first().platform <= 0) Alignment.Start else Alignment.End
        ))
        if (hasOperator) add(TableColumn(width = TableColumnWidth.Wrap))
        if (hasRemark) add(TableColumn(width = TableColumnWidth.Weight(1F)))
    } } }

    val minHeight = 28F.fontScaledDp(0.5F)

    val rows by remember(resolvedText) { derivedStateOf { buildList {
        for (seq in 1..lines.size) {
            val entry = resolvedText[seq]!!
            add(TableRow(
                onClick = if (co == Operator.LRT) ({
                    scope.launch {
                        Registry.getInstance(instance).findRoutes(lines[seq - 1].routeNumber, true).asSequence()
                            .filter { it.route!!.stops[Operator.LRT]?.contains(stopId) == true }
                            .minByOrNull { it.route!!.dest[Shared.language].editDistance(entry.destination.string.trim()) }
                            ?.let { route ->
                                Registry.getInstance(instance).addLastLookupRoute(route.routeKey, instance)
                                val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                                intent.putExtra("route", route)
                                intent.putExtra("stopId", stopId)
                                if (HistoryStack.historyStack.value.last().screenGroup == AppScreenGroup.ROUTE_STOPS) {
                                    instance.startActivity(AppIntent(instance, AppScreen.DUMMY))
                                    delay(300)
                                }
                                instance.startActivity(intent)
                            }
                    }
                }) else null,
                background = {
                    if (seq % 2 == 0) {
                        val color = platformPrimaryContainerColor
                        Spacer(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithCache {
                                    onDrawWithContent {
                                        drawRect(
                                            topLeft = Offset(x = -5.dp.toPx(), y = 0F),
                                            size = size.copy(width = size.width + 10.dp.toPx()),
                                            color = color.adjustAlpha(0.5F)
                                        )
                                    }
                                }
                        )
                    }
                },
                horizontalExtension = 5.dp,
                alignment = TableRowAlignment.Baseline(FirstBaseline),
                minHeight = minHeight
            ))
        }
    } } }

    Table(
        modifier = modifier.fillMaxWidth(),
        columns = { columns[it] },
        columnsCount = columns.size,
        rowDivider = { Spacer(modifier = Modifier.size(1.dp)) },
        rows = { rows[it] }
    ) {
        for (seq in 1..lines.size) {
            val entry = resolvedText[seq]!!
            val isEmpty = entry == Registry.ETALineEntryText.EMPTY
            if (hasPlatform) {
                TrainEtaText(entry.platform, freshness)
            }
            if (hasRouteNumber) {
                TrainEtaText(entry.routeNumber, freshness)
            }
            if (hasDestination) {
                TrainEtaText(entry.destination, freshness)
            }
            if (hasCarts) {
                TrainEtaText(entry.carts, freshness)
            }
            if (hasClockTime) {
                TrainEtaText(entry.clockTime, freshness)
            }
            if (hasTime) {
                TrainEtaText(if (isEmpty) "".asFormattedText() else entry.time, freshness)
            }
            if (hasOperator) {
                TrainEtaText(entry.operator, freshness)
            }
            if (hasRemark) {
                TrainEtaText(entry.remark, freshness)
            }
        }
    }
}

@Composable
fun TrainEtaText(
    text: FormattedText,
    freshness: Boolean
) {
    val content = text.asContentAnnotatedString()
    PlatformText(
        modifier = Modifier
            .heightIn(min = 24F.dp)
            .basicMarquee(Int.MAX_VALUE),
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Start,
        lineHeight = 1.1.em,
        fontSize = 18F.sp,
        color = if (freshness) platformLocalContentColor else Color(0xFFFFB0B0),
        maxLines = 1,
        text = content.annotatedString,
        inlineContent = content.createInlineContent(18F.sp)
    )
}

@Composable
fun TrainPathTableDisplay(
    modifier: Modifier,
    path: ImmutableList<FirstLastTrainPath>,
    operator: Operator,
    instance: AppActiveContext
) {
    Column(
        modifier = modifier
    ) {
        HorizontalDivider()
        for (i in path.indices) {
            val node = path[i]
            val lastNode = path.getOrNull(i - 1)
            val stop by remember(node) { derivedStateOf { node.id.asStop(instance)!! } }
            val towards by remember(node) { derivedStateOf { node.towards?.asStop(instance) } }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.fontScaledDp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(60.dp)
                ) {
                    lastNode?.line?.let { line ->
                        drawLine(
                            color = when (line) {
                                "walk_unpaid", "walk_paid" -> Color.Black
                                else -> operator.getLineColor(line, Color.Black)
                            },
                            start = Offset(center.x, 0F),
                            end = center,
                            strokeWidth = when (line) {
                                "walk_unpaid", "walk_paid" -> 4.dp.toPx()
                                else -> 10.dp.toPx()
                            },
                            pathEffect = when (line) {
                                "walk_unpaid" -> PathEffect.dashPathEffect(floatArrayOf(7F, 3F), 0F)
                                else -> null
                            }
                        )
                    }
                    node.line?.let { line ->
                        drawLine(
                            color = when (line) {
                                "walk_unpaid", "walk_paid" -> Color.Black
                                else -> operator.getLineColor(line, Color.Black)
                            },
                            start = center,
                            end = Offset(center.x, size.height),
                            strokeWidth = when (line) {
                                "walk_unpaid", "walk_paid" -> 4.dp.toPx()
                                else -> 10.dp.toPx()
                            },
                            pathEffect = when (line) {
                                "walk_unpaid" -> PathEffect.dashPathEffect(floatArrayOf(7F, 3F), 0F)
                                else -> null
                            }
                        )
                    }
                    drawCircle(
                        color = Color.White,
                        radius = 9.dp.toPx(),
                        center = center,
                        style = Fill,
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 9.dp.toPx(),
                        center = center,
                        style = Stroke(
                            width = 3.dp.toPx()
                        )
                    )
                }
                Column(
                    modifier = Modifier.weight(1F),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    PlatformText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .userMarquee(),
                        fontSize = 19F.sp,
                        lineHeight = 1.1F.em,
                        maxLines = userMarqueeMaxLines(),
                        text = stop.name[Shared.language]
                    )
                    towards?.takeIf { operator == Operator.LRT }?.let {
                        PlatformText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .userMarquee(),
                            fontSize = 16F.sp,
                            lineHeight = 1.1F.em,
                            maxLines = userMarqueeMaxLines(),
                            text = if (Shared.language == "en") "${operator.getDisplayRouteNumber(node.line!!)} to ${it.name.en}" else "${operator.getDisplayRouteNumber(node.line!!)} 往${it.name.zh}"
                        )
                    }?: run {
                        if (node.line != null) {
                            PlatformText(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .userMarquee(),
                                fontSize = 16F.sp,
                                lineHeight = 1.1F.em,
                                maxLines = userMarqueeMaxLines(),
                                text = when (node.line) {
                                    "walk_unpaid", "walk_paid" -> if (Shared.language == "en") "Walk" else "步行"
                                    else -> operator.getDisplayRouteNumber(node.line!!)
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun PreviewDetailsButton(
    instance: AppActiveContext,
    stopId: String
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        PlatformButton(
            onClick = {
                val intent = AppIntent(instance, AppScreen.SEARCH_TRAIN)
                intent.putExtra("stopLaunch", stopId)
                instance.startActivity(intent)
            },
            content = {
                PlatformIcon(
                    modifier = Modifier.padding(end = 5.dp),
                    painter = PlatformIcons.Outlined.Train,
                    contentDescription = if (Shared.language == "en") "Details" else "詳細資訊"
                )
                PlatformText(
                    fontSize = 17.sp,
                    text = if (Shared.language == "en") "Details" else "詳細資訊"
                )
            }
        )
    }
}

@Composable
fun OptionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter,
    iconColor: Color? = null,
    title: BilingualText,
    subTitle: BilingualText? = null
) {
    PlatformButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlatformIcon(
                modifier = Modifier.padding(5.dp),
                tint = iconColor,
                painter = icon,
                contentDescription = title[Shared.language]
            )
            Column(
                modifier = Modifier.weight(1F),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PlatformText(
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    text = title[Shared.language]
                )
                if (subTitle != null) {
                    PlatformText(
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        text = subTitle[Shared.language]
                    )
                }
            }
        }
    }
}