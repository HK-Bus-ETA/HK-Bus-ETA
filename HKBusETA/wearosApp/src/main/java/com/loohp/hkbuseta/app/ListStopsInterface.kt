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

package com.loohp.hkbuseta.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.OriginData
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.StopEntry
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getLineColor
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.resolveSpecialRemark
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Shared.getResolvedText
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.MTRStopSectionData
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.createMTRLineSectionData
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.compose.AutoResizeDrawPhaseColorText
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.DrawPhaseColorText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.FullPageScrollBarConfig
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.collectAsStateWithLifecycle
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.compose.userMarqueeMaxLines
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.MTRLineSection
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.findTextLengthDp
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.spToDp
import com.loohp.hkbuseta.utils.spToPixels
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun ListStopsMainElement(ambientMode: Boolean, instance: AppActiveContext, route: RouteSearchResultEntry, showEta: Boolean, scrollToStop: String?, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()

        val padding by remember { derivedStateOf { 7.5F.scaledSize(instance) } }

        val kmbCtbJoint = remember { route.route!!.isKmbCtbJoint }
        val routeNumber = remember { route.route!!.routeNumber }
        val co = remember { route.co }
        val bound = remember { route.route!!.idBound(co) }
        val gmbRegion = remember { route.route!!.gmbRegion }
        val resolvedDestName = remember { route.route!!.resolvedDest(true) }
        val specialRoutes = remember { Registry.getInstance(instance).getAllBranchRoutes(routeNumber, bound, co, gmbRegion)
            .asSequence()
            .map { it.resolveSpecialRemark(instance) }
            .filter { it.zh.isNotBlank() }
            .toImmutableList()
        }

        val coColor = remember { co.getColor(routeNumber, Color.White) }

        val stopsList = remember { Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gmbRegion).toImmutableList() }
        val lowestServiceType = remember { stopsList.minOf { it.serviceType } }
        val mtrStopsInterchange = remember { if (co.isTrain) {
            stopsList.asSequence().map { Registry.getInstance(instance).getMtrStationInterchange(it.stopId, routeNumber) }.toImmutableList()
        } else persistentListOf() }
        val mtrLineSectionsData = remember { if (co.isTrain) createMTRLineSectionData(
            co = co,
            color = co.getLineColor(routeNumber, 0xFFFFFFFF),
            stopList = stopsList,
            mtrStopsInterchange = mtrStopsInterchange,
            isLrtCircular = route.route!!.lrtCircular != null,
            context = instance
        ).toImmutableList() else null }

        val distances: MutableMap<Int, Double> = remember { ConcurrentHashMap() }

        val etaTextWidth by remember { derivedStateOf { "99".findTextLengthDp(instance, 16F.scaledSize(instance)) + 1F } }

        val etaUpdateTimes = remember { ConcurrentHashMap<Int, Long>().asImmutableState() }
        val etaResults = remember { ConcurrentHashMap<Int, Registry.ETAQueryResult>().asImmutableState() }

        var closestIndex by remember { mutableIntStateOf(0) }

        val alternateStopNames by remember { derivedStateOf { if (kmbCtbJoint) {
            Registry.getInstance(instance).findEquivalentStops(stopsList.map { it.stopId }, Operator.CTB).toImmutableList()
        } else {
            null
        } } }

        LaunchedEffect (Unit) {
            val scrollTask: (OriginData?, String?) -> Unit = { origin, stopId ->
                if (stopId != null) {
                    stopsList.withIndex().find { it.value.stopId == stopId }?.let {
                        scope.launch {
                            scroll.animateScrollToItem(it.index + 2, (-instance.screenHeight / 2) - 15F.scaledSize(instance).spToPixels(instance).roundToInt())
                        }
                    }
                } else if (origin != null) {
                    stopsList.withIndex().map { (index, entry) ->
                        val stop = entry.stop
                        val location = stop.location
                        val stopStr = stop.name[Shared.language]
                        StopEntry(index + 1, stopStr, entry, location.lat, location.lng)
                    }.onEach {
                        it.distance = origin.distance(it)
                        distances[it.stopIndex] = it.distance
                    }.minBy {
                        it.distance
                    }.let {
                        if (!origin.onlyInRange || it.distance <= 0.3) {
                            closestIndex = it.stopIndex
                            scope.launch {
                                scroll.animateScrollToItem(it.stopIndex + 1, (-instance.screenHeight / 2) - 15F.scaledSize(instance).spToPixels(instance).roundToInt())
                            }
                        }
                    }
                }
            }

            if (scrollToStop != null) {
                scrollTask.invoke(null, scrollToStop)
            } else if (route.origin != null) {
                val origin = route.origin!!
                scrollTask.invoke(OriginData(origin.lat, origin.lng), null)
            } else {
                checkLocationPermission(instance) {
                    if (it) {
                        val future = getGPSLocation(instance)
                        CoroutineScope(dispatcherIO).launch {
                            try {
                                val locationResult = future.await()
                                if (locationResult?.isSuccess == true) {
                                    val location = locationResult.location!!
                                    scrollTask.invoke(OriginData(location.lat, location.lng, true), null)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }

        Box (
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn (
                modifier = Modifier
                    .fillMaxSize()
                    .fullPageVerticalLazyScrollbar(
                        state = scroll,
                        context = instance,
                        scrollbarConfigFullPage = FullPageScrollBarConfig(
                            alpha = if (ambientMode) 0F else null
                        )
                    )
                    .rotaryScroll(scroll, focusRequester, ambientMode = ambientMode),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = scroll
            ) {
                item {
                    HeaderElement(
                        ambientMode = ambientMode,
                        routeNumber = routeNumber,
                        kmbCtbJoint = kmbCtbJoint,
                        co = co,
                        coColor = coColor,
                        destName = resolvedDestName,
                        specialRoutes = specialRoutes,
                        alternateStopNames = alternateStopNames,
                        instance = instance
                    )
                }
                itemsIndexed(stopsList, key = { index, _ -> index }) { index, entry ->
                    StopRowElement(
                        ambientMode = ambientMode,
                        index = index,
                        entry = entry,
                        closestIndex = closestIndex,
                        co = co,
                        showEta = showEta,
                        kmbCtbJoint = kmbCtbJoint,
                        lowestServiceType = lowestServiceType,
                        coColor = coColor,
                        alternateStopNames = alternateStopNames,
                        mtrLineSectionsData = mtrLineSectionsData,
                        mtrStopsInterchange = mtrStopsInterchange,
                        padding = padding,
                        stopList = stopsList,
                        route = route,
                        etaTextWidth = etaTextWidth,
                        etaResults = etaResults,
                        etaUpdateTimes = etaUpdateTimes,
                        instance = instance,
                        schedule = schedule
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(25.dp, 0.dp),
                        color = Color(0xFF333333).adjustBrightness(if (ambientMode) 0.5F else 1F)
                    )
                }
                item {
                    Spacer(modifier = Modifier.size(40.scaledSize(instance).dp))
                }
            }
            if (ambientMode) {
                Spacer(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(35.scaledSize(instance).dp)
                        .background(
                            Brush.verticalGradient(
                                0F to Color(0xFF000000),
                                1F to Color(0x00000000),
                                startY = 23.scaledSize(instance).spToPixels(instance)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    verticalArrangement = Arrangement.Top
                ) {
                    WearOSShared.MainTime()
                }
            }
        }
    }
}

@Composable
fun HeaderElement(
    ambientMode: Boolean,
    routeNumber: String,
    kmbCtbJoint: Boolean,
    co: Operator,
    coColor: Color,
    destName: BilingualText,
    specialRoutes: ImmutableList<BilingualText>,
    alternateStopNames: ImmutableList<Registry.NearbyStopSearchResult>?,
    instance: AppActiveContext
) {
    var alternateStopNamesShowing by Shared.alternateStopNamesShowingState.collectAsStateWithLifecycle { Registry.getInstance(instance).setAlternateStopNames(it, instance) }
    Column(
        modifier = Modifier
            .defaultMinSize(minHeight = 35.scaledSize(instance).dp)
            .padding(20.dp, 20.dp, 20.dp, 5.dp)
            .fillMaxWidth()
            .applyIf(alternateStopNames != null) { clickable {
                alternateStopNamesShowing = !alternateStopNamesShowing
                val operatorName = (if (alternateStopNamesShowing) Operator.CTB else Operator.KMB).getDisplayName(routeNumber, false, Shared.language)
                val text = if (Shared.language == "en") {
                    "Displaying $operatorName stop names"
                } else {
                    "顯示${operatorName}站名"
                }
                instance.showToastText(text, ToastDuration.SHORT)
            } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val color by WearOSShared.rememberOperatorColor(coColor, Operator.CTB.getOperatorColor(Color.White).takeIf { kmbCtbJoint })

        AutoResizeDrawPhaseColorText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp, 0.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSizeRange = FontSizeRange(
                max = 17F.scaledSize(instance).sp
            ),
            lineHeight = 17F.scaledSize(instance).sp.clamp(max = 20F.scaledSize(instance).dp),
            color = { color.adjustBrightness(if (ambientMode) 0.7F else 1F) },
            maxLines = 1,
            text = co.getDisplayName(routeNumber, kmbCtbJoint, Shared.language).plus(" ").plus(co.getDisplayRouteNumber(routeNumber))
        )
        AutoResizeText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 0.dp),
            textAlign = TextAlign.Center,
            fontSizeRange = FontSizeRange(
                max = 11F.scaledSize(instance).sp
            ),
            color = Color(0xFFFFFFFF).adjustBrightness(if (ambientMode) 0.7F else 1F),
            maxLines = 2,
            text = destName[Shared.language]
        )
        if (!co.isTrain && !co.isFerry && specialRoutes.isNotEmpty()) {
            val infiniteTransition = rememberInfiniteTransition(label = "SpecialBranchesCrossFade")
            val animatedCurrentLine by infiniteTransition.animateValue(
                initialValue = 0,
                targetValue = specialRoutes.size,
                typeConverter = Int.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(3500 * specialRoutes.size, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "SpecialBranchesCrossFade"
            )
            Crossfade(
                targetState = animatedCurrentLine,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing),
                label = "SpecialBranchesCrossFade"
            ) {
                AutoResizeText(
                    modifier = Modifier
                        .padding(5.dp, 0.dp)
                        .height(38F.scaledSize(instance).sp.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSizeRange = FontSizeRange(
                        max = 11F.scaledSize(instance).sp
                    ),
                    maxLines = 2,
                    color = Color(0xFFFFFFFF).adjustBrightness(0.65F).adjustBrightness(if (ambientMode) 0.7F else 1F),
                    text = if (Shared.language == "en") {
                        "Special ${specialRoutes[it.coerceIn(specialRoutes.indices)].en}"
                    } else {
                        "特別班 ${specialRoutes[it.coerceIn(specialRoutes.indices)].zh}"
                    }
                )
            }
        }
    }
}

@Composable
fun LazyItemScope.StopRowElement(
    ambientMode: Boolean,
    index: Int,
    entry: Registry.StopData,
    closestIndex: Int,
    co: Operator,
    showEta: Boolean,
    kmbCtbJoint: Boolean,
    lowestServiceType: Int,
    coColor: Color,
    alternateStopNames: ImmutableList<Registry.NearbyStopSearchResult>?,
    mtrLineSectionsData: ImmutableList<MTRStopSectionData>?,
    mtrStopsInterchange: ImmutableList<Registry.MTRInterchangeData>,
    padding: Float,
    stopList: ImmutableList<Registry.StopData>,
    route: RouteSearchResultEntry,
    etaTextWidth: Float,
    etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>,
    etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>,
    instance: AppActiveContext,
    schedule: (Boolean, Int, (() -> Unit)?) -> Unit
) {
    val alternateStopNamesShowing by Shared.alternateStopNamesShowingState.collectAsStateWithLifecycle { Registry.getInstance(instance).setAlternateStopNames(it, instance) }

    val stopNumber = index + 1
    val isClosest = closestIndex == stopNumber
    val stopId = entry.stopId
    val stop = entry.stop
    val brightness = if (entry.serviceType == lowestServiceType || co.isTrain) 1F else 0.65F
    val rawColor = (if (isClosest) coColor else Color.White).adjustBrightness(brightness)
    val stopStr = if (alternateStopNamesShowing && alternateStopNames != null) {
        alternateStopNames[index].stop
    } else {
        stop
    }.remarkedName[Shared.language].asContentAnnotatedString().annotatedString
    val mtrLineSectionData = mtrLineSectionsData?.get(index)

    Column (
        modifier = Modifier
            .fillParentMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable {
                val intent = AppIntent(instance, AppScreen.ETA)
                intent.putExtra("stopId", stopId)
                intent.putExtra("co", co.name)
                intent.putExtra("index", stopNumber)
                intent.putExtra("stop", stop)
                intent.putExtra("route", entry.route)
                instance.startActivity(intent)
            }
    ) {
        val color by WearOSShared.rememberOperatorColor(rawColor, Operator.CTB.getOperatorColor(Color.White).takeIf { isClosest && kmbCtbJoint })
        Row (
            modifier = Modifier
                .padding(horizontal = 25.dp)
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            var lineCount by remember { mutableIntStateOf(1) }
            if (co.isTrain && mtrLineSectionData != null) {
                val width = remember { (if (stopList.map { it.serviceType }.distinct().size > 1 || mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }) 50 else 35).scaledSize(instance) }
                Box(
                    modifier = Modifier
                        .requiredWidth(width.spToDp(instance).dp)
                        .fillMaxHeight()
                        .applyIf(co.isTrain) { clickable {
                            val intent = AppIntent(instance, AppScreen.SEARCH_TRAIN)
                            intent.putExtra("type", co.name)
                            instance.startActivity(intent)
                        } }
                ) {
                    MTRLineSection(mtrLineSectionData, ambientMode)
                }
            } else {
                DrawPhaseColorText(
                    modifier = Modifier
                        .padding(0.dp, padding.dp)
                        .requiredWidth(30.dp)
                        .applyIf(lineCount > 1) { align(Alignment.CenterVertically) },
                    textAlign = TextAlign.Start,
                    fontSize = 15F.scaledSize(instance).sp,
                    fontWeight = if (isClosest) FontWeight.Bold else FontWeight.Normal,
                    color = { color.adjustBrightness(brightness) },
                    maxLines = 1,
                    text = "${stopNumber}."
                )
            }
            DrawPhaseColorText(
                modifier = Modifier
                    .padding(0.dp, padding.dp)
                    .weight(1F)
                    .userMarquee(),
                onTextLayout = { lineCount = it.lineCount },
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                fontSize = 15F.scaledSize(instance).sp,
                fontWeight = if (isClosest) FontWeight.Bold else FontWeight.Normal,
                color = { color.adjustBrightness(brightness) },
                maxLines = userMarqueeMaxLines(),
                text = stopStr
            )
            if (showEta) {
                Box (
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .requiredWidthIn(min = etaTextWidth.dp)
                ) {
                    ETAElement(stopNumber, stopId, route, etaResults, etaUpdateTimes, instance, schedule)
                }
            }
        }
    }
}

@Composable
fun ETAElement(index: Int, stopId: String, route: RouteSearchResultEntry, etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>, instance: AppActiveContext, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    val etaStateFlow = remember { MutableStateFlow(etaResults.value[index]) }

    LaunchedEffect (Unit) {
        val eta = etaStateFlow.value
        if (eta != null && !eta.isConnectionError) {
            delay(etaUpdateTimes.value[index]?.let { (Shared.ETA_UPDATE_INTERVAL - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        schedule.invoke(true, index) {
            val result = runBlocking(dispatcherIO) { Registry.getInstance(instance).getEta(stopId, index, route.co, route.route!!, instance).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND) }
            etaStateFlow.value = result
            etaUpdateTimes.value[index] = System.currentTimeMillis()
            etaResults.value[index] = result
        }
    }
    DisposableEffect (Unit) {
        onDispose {
            schedule.invoke(false, index, null)
        }
    }

    val etaState by etaStateFlow.collectAsStateWithLifecycle()

    Box (
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.CenterEnd
    ) {
        val eta = etaState
        if (eta != null && !eta.isConnectionError) {
            if (eta.nextScheduledBus !in 0..59) {
                if (eta.isMtrEndOfLine) {
                    Icon(
                        modifier = Modifier.size(18F.scaledSize(instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_line_end_circle_24),
                        contentDescription = if (Shared.language == "en") "End of Line" else "終點站",
                        tint = Color(0xFFAAC3D5),
                    )
                } else if (eta.isTyphoonSchedule) {
                    val typhoonInfo by remember { Registry.getInstance(instance).typhoonInfo }.collectAsStateWithLifecycle()
                    Image(
                        modifier = Modifier.size(18F.scaledSize(instance).sp.dp),
                        painter = painterResource(R.mipmap.cyclone),
                        contentDescription = typhoonInfo.typhoonWarningTitle
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(18F.scaledSize(instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        contentDescription = if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次",
                        tint = Color(0xFFAAC3D5),
                    )
                }
            } else {
                val (text, lineHeight) = if (Shared.etaDisplayMode.shortTextClockTime) {
                    val text1 = eta.getResolvedText(1, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                    val fontSize = 15F.scaledSize(instance).sp
                    text1.replace("\\s+".toRegex(), "\n").asAnnotatedString(SpanStyle(fontSize = fontSize)) to fontSize
                } else {
                    val (text1, text2) = eta.firstLine.shortText
                    buildAnnotatedString {
                        append(text1, SpanStyle(fontSize = 16F.scaledSize(instance).sp))
                        append("\n")
                        append(text2, SpanStyle(fontSize = 9F.scaledSize(instance).sp))
                    } to 9F.sp.clamp(max = 11.dp)
                }
                Text(
                    textAlign = TextAlign.End,
                    fontSize = 16F.sp,
                    color = Color(0xFFAAC3D5),
                    lineHeight = lineHeight,
                    text = text
                )
            }
        }
    }
}