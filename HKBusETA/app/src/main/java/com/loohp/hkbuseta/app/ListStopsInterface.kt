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

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
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
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.StopEntry
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getLineColor
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Registry.StopData
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.MTRStopSectionData
import com.loohp.hkbuseta.common.utils.createMTRLineSectionData
import com.loohp.hkbuseta.common.utils.eitherContains
import com.loohp.hkbuseta.common.utils.formatDecimalSeparator
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.FullPageScrollBarConfig
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.services.AlightReminderService
import com.loohp.hkbuseta.shared.AndroidShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ImmutableState
import com.loohp.hkbuseta.utils.MTRLineSection
import com.loohp.hkbuseta.utils.MTRLineSectionExtension
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.asImmutableState
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.findTextLengthDp
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.sp
import com.loohp.hkbuseta.utils.spToPixels
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt


@OptIn(ExperimentalFoundationApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun ListStopsMainElement(ambientMode: Boolean, instance: AppActiveContext, route: RouteSearchResultEntry, showEta: Boolean, scrollToStop: String?, isAlightReminder: Boolean, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val padding by remember { derivedStateOf { 7.5F.scaledSize(instance) } }

        val kmbCtbJoint = remember { route.route!!.isKmbCtbJoint }
        val routeNumber = remember { route.route!!.routeNumber }
        val co = remember { route.co }
        val bound = remember { if (co == Operator.NLB) route.route!!.nlbId else route.route!!.bound[co]!! }
        val gmbRegion = remember { route.route!!.gmbRegion }
        val interchangeSearch = remember { route.isInterchangeSearch }
        val origName = remember { route.route!!.orig }
        val destName = remember { route.route!!.dest }
        val resolvedDestName = remember { route.route!!.resolvedDest(true) }
        val specialOrigsDests = remember { Registry.getInstance(instance).getAllOriginsAndDestinations(routeNumber, bound, co, gmbRegion) }
        val specialOrigs = remember { specialOrigsDests.first.asSequence().filter { !it.zh.eitherContains(origName.zh) }.toImmutableList() }
        val specialDests = remember { specialOrigsDests.second.asSequence().filter { !it.zh.eitherContains(destName.zh) }.toImmutableList() }

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
        ) else null }

        val distances: MutableMap<Int, Double> = remember { ConcurrentHashMap() }

        val etaTextWidth by remember { derivedStateOf { "99".findTextLengthDp(instance, 16F.scaledSize(instance)) + 1F } }

        val etaUpdateTimes = remember { ConcurrentHashMap<Int, Long>().asImmutableState() }
        val etaResults = remember { ConcurrentHashMap<Int, Registry.ETAQueryResult>().asImmutableState() }

        val rawAlightReminderData by remember { AlightReminderService.getCurrentState() }.collectAsStateWithLifecycle()
        val alightReminderData by remember { derivedStateOf { if (isAlightReminder) rawAlightReminderData else null } }

        var targetStop: Stop? by remember { mutableStateOf(null) }
        val targetStopIndex by remember { derivedStateOf { targetStop?.let { target -> stopsList.indexOfFirst { it.stop == target } + 1 }?: -1 } }
        var isTargetActive by remember { mutableStateOf(isAlightReminder) }

        var closestIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect (Unit) {
            val scrollTask: (OriginData?, String?) -> Unit = { origin, stopId ->
                if (stopId != null) {
                    stopsList.withIndex().find { it.value.stopId == stopId }?.let {
                        scope.launch {
                            scroll.animateScrollToItem(it.index + 2, (-instance.screenHeight / 2) - 15F.scaledSize(instance).spToPixels(instance).roundToInt())
                        }
                    }
                } else if (origin != null) {
                    stopsList.withIndex().map {
                        val (index, entry) = it
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
                        CoroutineScope(Dispatchers.IO).launch {
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
                    .rotaryScroll(scroll, focusRequester, ambientMode = ambientMode)
                    .composed {
                        RestartEffect {
                            if (isAlightReminder && (alightReminderData == null || !alightReminderData!!.active || route.route != alightReminderData!!.route)) {
                                instance.finish()
                            }
                        }

                        LaunchedEffect (rawAlightReminderData) {
                            delay(1000)
                            if (alightReminderData != null) {
                                targetStop = alightReminderData?.targetStop
                                if (route.route == alightReminderData!!.route) {
                                    if (!alightReminderData!!.active) {
                                        isTargetActive = false
                                    }
                                    if (alightReminderData!!.currentLocation.isSuccess) {
                                        val location = alightReminderData!!.currentLocation.location!!
                                        closestIndex = (stopsList.withIndex().minBy {
                                            it.value.stop.location.distance(location)
                                        }.index + 1).coerceAtMost(targetStopIndex)
                                    }
                                } else if (alightReminderData!!.active) {
                                    isTargetActive = false
                                    instance.finish()
                                }
                            }
                        }

                        this
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                state = scroll
            ) {
                item {
                    HeaderElement(ambientMode, routeNumber, kmbCtbJoint, co, coColor, resolvedDestName, specialOrigs, specialDests, instance)
                }
                for ((index, entry) in stopsList.withIndex()) {
                    item {
                        val stopNumber = index + 1
                        val isClosest = closestIndex == stopNumber
                        val isTargetStop = targetStopIndex == stopNumber
                        val isTargetIntermediateStop = closestIndex > 0 && stopNumber in (closestIndex + 1) until targetStopIndex
                        val stopId = entry.stopId
                        val stop = entry.stop
                        val brightness = if (entry.serviceType == lowestServiceType) 1F else 0.65F
                        val rawColor = (if (isClosest) coColor else Color.White).adjustBrightness(brightness)
                        val stopStr = stop.name[Shared.language]
                        val stopRemarkedName = remember { stop.remarkedName[Shared.language].asContentAnnotatedString().annotatedString }
                        val mtrLineSectionData = mtrLineSectionsData?.get(index)

                        Column (
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .height(IntrinsicSize.Min)
                                .combinedClickable(
                                    onClick = {
                                        val intent = AppIntent(instance, AppScreen.ETA)
                                        intent.putExtra("stopId", stopId)
                                        intent.putExtra("co", co.name)
                                        intent.putExtra("index", stopNumber)
                                        intent.putExtra("stop", stop.toByteArray())
                                        intent.putExtra("route", entry.route.toByteArray())
                                        instance.startActivity(intent)
                                    },
                                    onLongClick = {
                                        instance.runOnUiThread {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            instance.runOnUiThread {
                                                val prefix = if (co.isTrain) "" else stopNumber.toString().plus(". ")
                                                val text = if (isClosest && !isAlightReminder) {
                                                    prefix.plus(stopStr).plus("\n")
                                                        .plus(if (interchangeSearch) (if (Shared.language == "en") "Interchange " else "轉乘") else (if (Shared.language == "en") "Nearby " else "附近"))
                                                        .plus(((distances[stopNumber] ?: Double.NaN) * 1000).roundToInt().formatDecimalSeparator())
                                                        .plus(if (Shared.language == "en") "m" else "米")
                                                } else {
                                                    prefix.plus(stopStr)
                                                }
                                                instance.showToastText(text, ToastDuration.LONG)
                                            }
                                        }
                                    }
                                )
                        ) {
                            StopRowElement(ambientMode, stopNumber, stopId, co, showEta, isAlightReminder, isClosest, isTargetStop, isTargetIntermediateStop, kmbCtbJoint, mtrStopsInterchange, rawColor, brightness, padding, stopRemarkedName, stopsList, mtrLineSectionData, route, etaTextWidth, etaResults, etaUpdateTimes, instance, schedule)
                            if (isAlightReminder && isTargetStop) {
                                Box (
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (co.isTrain && mtrLineSectionData?.requireExtension == true) {
                                        Box(
                                            modifier = Modifier
                                                .padding(25.dp, 0.dp)
                                                .requiredWidth((if (stopsList.map { it.serviceType }.distinct().size > 1 || mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }) 50 else 35).sp.dp)
                                                .fillMaxHeight()
                                                .align(Alignment.CenterStart)
                                        ) {
                                            MTRLineSectionExtension(mtrLineSectionData, ambientMode)
                                        }
                                    }
                                    Column {
                                        Spacer(modifier = Modifier.size(5.scaledSize(instance).dp))
                                        if (isTargetActive) {
                                            TerminateAlightReminderButton(instance)
                                        } else {
                                            AlightReminderCompleted(instance)
                                        }
                                        Spacer(modifier = Modifier.size(5.scaledSize(instance).dp))
                                    }
                                }
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .padding(25.dp, 0.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF333333).adjustBrightness(if (ambientMode) 0.5F else 1F))
                        )
                    }
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
                    AndroidShared.MainTime()
                }
            }
        }
    }
}

@Composable
fun AlightReminderCompleted(instance: AppActiveContext) {
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .height(40.scaledSize(instance).sp.clamp(min = 40.dp).dp),
        onClick = {
            instance.finish()
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(20.scaledSize(instance).sp.clamp(max = 20.dp).dp)
                        .height(20.scaledSize(instance).sp.clamp(max = 20.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(17F.scaledSize(instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_location_on_24),
                        tint = Color(0xFFFF9800),
                        contentDescription = if (Shared.language == "en") "Arrived" else "已到達"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = 14F.scaledSize(instance).sp,
                    text = if (Shared.language == "en") "Arrived" else "已到達"
                )
            }
        }
    )
}

@Composable
fun TerminateAlightReminderButton(instance: AppActiveContext) {
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .height(40.scaledSize(instance).sp.clamp(min = 40.dp).dp),
        onClick = {
            instance.finish()
            AlightReminderService.terminate()
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(20.scaledSize(instance).sp.clamp(max = 20.dp).dp)
                        .height(20.scaledSize(instance).sp.clamp(max = 20.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(17F.scaledSize(instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_notifications_off_24),
                        tint = Color(0xFFFF0000),
                        contentDescription = if (Shared.language == "en") "Disable Reminder" else "關閉落車提示"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = 14F.scaledSize(instance).sp,
                    text = if (Shared.language == "en") "Disable Reminder" else "關閉落車提示"
                )
            }
        }
    )
}

@Composable
fun HeaderElement(ambientMode: Boolean, routeNumber: String, kmbCtbJoint: Boolean, co: Operator, coColor: Color, destName: BilingualText, specialOrigs: ImmutableList<BilingualText>, specialDests: ImmutableList<BilingualText>, instance: AppActiveContext) {
    Column(
        modifier = Modifier
            .defaultMinSize(minHeight = 35.scaledSize(instance).dp)
            .padding(20.dp, 20.dp, 20.dp, 5.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val color = if (kmbCtbJoint) {
            val infiniteTransition = rememberInfiniteTransition(label = "JointColor")
            val animatedColor by infiniteTransition.animateColor(
                initialValue = coColor,
                targetValue = Color(0xFFFFE15E),
                animationSpec = infiniteRepeatable(
                    animation = tween(5000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(1500)
                ),
                label = "JointColor"
            )
            animatedColor
        } else {
            coColor
        }

        AutoResizeText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp, 0.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSizeRange = FontSizeRange(
                min = 1F.scaledSize(instance).dp.sp,
                max = 17F.scaledSize(instance).sp.clamp(max = 20F.scaledSize(instance).dp)
            ),
            lineHeight = 17F.scaledSize(instance).sp.clamp(max = 20F.scaledSize(instance).dp),
            color = color.adjustBrightness(if (ambientMode) 0.7F else 1F),
            maxLines = 1,
            text = co.getDisplayName(routeNumber, kmbCtbJoint, Shared.language).plus(" ").plus(co.getDisplayRouteNumber(routeNumber))
        )
        AutoResizeText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 0.dp),
            textAlign = TextAlign.Center,
            fontSizeRange = FontSizeRange(
                min = 1F.scaledSize(instance).dp.sp,
                max = 11F.scaledSize(instance).sp.clamp(max = 14F.scaledSize(instance).dp)
            ),
            color = Color(0xFFFFFFFF).adjustBrightness(if (ambientMode) 0.7F else 1F),
            maxLines = 2,
            text = destName[Shared.language]
        )
        if (specialOrigs.isNotEmpty()) {
            AutoResizeText(
                modifier = Modifier
                    .padding(5.dp, 0.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSizeRange = FontSizeRange(
                    min = 1F.scaledSize(instance).dp.sp,
                    max = 11F.scaledSize(instance).sp.clamp(max = 14F.scaledSize(instance).dp)
                ),
                color = Color(0xFFFFFFFF).adjustBrightness(0.65F).adjustBrightness(if (ambientMode) 0.7F else 1F),
                maxLines = 2,
                text = if (Shared.language == "en") {
                    "Special From ".plus(specialOrigs.joinToString("/") { it.en })
                } else {
                    "特別班 從".plus(specialOrigs.joinToString("/") { it.zh }).plus("開出")
                }
            )
        }
        if (specialDests.isNotEmpty()) {
            AutoResizeText(
                modifier = Modifier
                    .padding(5.dp, 0.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSizeRange = FontSizeRange(
                    min = 1F.scaledSize(instance).dp.sp,
                    max = 11F.scaledSize(instance).sp.clamp(max = 14F.scaledSize(instance).dp)
                ),
                color = Color(0xFFFFFFFF).adjustBrightness(0.65F).adjustBrightness(if (ambientMode) 0.7F else 1F),
                maxLines = 2,
                text = if (Shared.language == "en") {
                    "Special To ".plus(specialDests.joinToString("/") { it.en })
                } else {
                    "特別班 往".plus(specialDests.joinToString("/") { it.zh })
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StopRowElement(ambientMode: Boolean, stopNumber: Int, stopId: String, co: Operator, showEta: Boolean, isAlightReminder: Boolean, isClosest: Boolean, isTargetStop: Boolean, isTargetIntermediateStop: Boolean, kmbCtbJoint: Boolean, mtrStopsInterchange: ImmutableList<Registry.MTRInterchangeData>, rawColor: Color, brightness: Float, padding: Float, stopStr: AnnotatedString, stopList: ImmutableList<StopData>, mtrLineSectionData: MTRStopSectionData?, route: RouteSearchResultEntry, etaTextWidth: Float, etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>, instance: AppActiveContext, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    val color = if (isAlightReminder) {
        if (isTargetStop) {
            Color(0xFFFF9800)
        } else if (isClosest) {
            Color(0xFFFF0000)
        } else {
            rawColor
        }
    } else {
        if (isClosest && kmbCtbJoint) {
            val infiniteTransition = rememberInfiniteTransition(label = "JointColor")
            val animatedColor by infiniteTransition.animateColor(
                initialValue = rawColor,
                targetValue = Color(0xFFFFE15E).adjustBrightness(brightness),
                animationSpec = infiniteRepeatable(
                    animation = tween(5000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(1500)
                ),
                label = "JointColor"
            )
            animatedColor
        } else {
            rawColor
        }
    }
    Row (
        modifier = Modifier
            .padding(25.dp, 0.dp)
            .height(IntrinsicSize.Min)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        if (co.isTrain && mtrLineSectionData != null) {
            val width = remember { (if (stopList.map { it.serviceType }.distinct().size > 1 || mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }) 50 else 35).sp }
            Box(
                modifier = Modifier
                    .requiredWidth(width.dp)
                    .fillMaxHeight()
            ) {
                MTRLineSection(mtrLineSectionData, ambientMode)
            }
        } else {
            Text(
                modifier = Modifier
                    .padding(0.dp, padding.dp)
                    .requiredWidth(30.dp),
                textAlign = TextAlign.Start,
                fontSize = 15F.scaledSize(instance).sp,
                fontWeight = if (isClosest || isTargetStop) FontWeight.Bold else FontWeight.Normal,
                color = color,
                maxLines = 1,
                text = stopNumber.toString().plus(".")
            )
        }
        if (isAlightReminder && ((isTargetStop && !isClosest) || isClosest || isTargetIntermediateStop)) {
            Text(
                modifier = Modifier
                    .padding(0.dp, padding.dp)
                    .weight(1F)
                    .basicMarquee(iterations = Int.MAX_VALUE),
                textAlign = TextAlign.Start,
                fontSize = 15F.scaledSize(instance).sp,
                fontWeight = if (isClosest || isTargetStop) FontWeight.Bold else FontWeight.Normal,
                color = color,
                maxLines = 1,
                text = stopStr
            )
            Box (
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .requiredWidth(etaTextWidth.dp)
            ) {
                if (isTargetStop && !isClosest) {
                    Icon (
                        painter = painterResource(R.drawable.baseline_notifications_active_24),
                        tint = color,
                        contentDescription = if (Shared.language == "en") "Alight Reminder" else "落車提示"
                    )
                } else if (isClosest) {
                    Icon (
                        painter = painterResource(R.drawable.baseline_location_on_24),
                        tint = color,
                        contentDescription = if (Shared.language == "en") "Alight Reminder" else "落車提示"
                    )
                } else {
                    Icon (
                        painter = painterResource(R.drawable.baseline_more_vert_24),
                        tint = color,
                        contentDescription = if (Shared.language == "en") "Alight Reminder" else "落車提示"
                    )
                }
            }
        } else {
            Text(
                modifier = Modifier
                    .padding(0.dp, padding.dp)
                    .weight(1F)
                    .basicMarquee(iterations = Int.MAX_VALUE),
                textAlign = TextAlign.Start,
                fontSize = 15F.scaledSize(instance).sp,
                fontWeight = if (isClosest || isTargetStop) FontWeight.Bold else FontWeight.Normal,
                color = color,
                maxLines = 1,
                text = stopStr
            )
            if (showEta) {
                Box (
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .requiredWidth(etaTextWidth.dp)
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
            val result = Registry.getInstance(instance).getEta(stopId, index, route.co, route.route!!, instance).get(
                Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
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
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .offset(0.dp, 2F.scaledSize(instance).sp.clamp(max = 3F.scaledSize(instance).dp).dp - 2F.sp.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        val eta = etaState
        if (eta != null && !eta.isConnectionError) {
            if (eta.nextScheduledBus !in 0..59) {
                if (eta.isMtrEndOfLine) {
                    Icon(
                        modifier = Modifier.size(16F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp).dp),
                        painter = painterResource(R.drawable.baseline_line_end_circle_24),
                        contentDescription = if (Shared.language == "en") "End of Line" else "終點站",
                        tint = Color(0xFF798996),
                    )
                } else if (eta.isTyphoonSchedule) {
                    val typhoonInfo by remember { Registry.getInstance(instance).cachedTyphoonDataState }.collectAsStateWithLifecycle()
                    Image(
                        modifier = Modifier.size(16F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp).dp),
                        painter = painterResource(R.mipmap.cyclone),
                        contentDescription = typhoonInfo.typhoonWarningTitle
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(16F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp).dp),
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        contentDescription = if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次",
                        tint = Color(0xFF798996),
                    )
                }
            } else {
                val (text1, text2) = eta.firstLine.shortText
                val text = buildAnnotatedString {
                    append(text1, SpanStyle(fontSize = 14F.scaledSize(instance).clampSp(instance, dpMax = 15F.scaledSize(instance)).sp))
                    append("\n")
                    append(text2, SpanStyle(fontSize = 7F.scaledSize(instance).clampSp(instance, dpMax = 8F.scaledSize(instance)).sp))
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 14F.sp,
                    color = Color(0xFFAAC3D5),
                    lineHeight = 7F.sp,
                    maxLines = 2,
                    text = text
                )
            }
        }
    }
}