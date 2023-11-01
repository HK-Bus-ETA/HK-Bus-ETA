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

package com.loohp.hkbuseta

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.objects.BilingualText
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.objects.Stop
import com.loohp.hkbuseta.objects.getColor
import com.loohp.hkbuseta.objects.getDisplayName
import com.loohp.hkbuseta.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.objects.name
import com.loohp.hkbuseta.objects.resolvedDest
import com.loohp.hkbuseta.services.AlightReminderService
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Registry.StopData
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.DistanceUtils
import com.loohp.hkbuseta.utils.ImmutableState
import com.loohp.hkbuseta.utils.LocationUtils
import com.loohp.hkbuseta.utils.ScreenSizeUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.UnitUtils
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.asImmutableState
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.eitherContains
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.formatDecimalSeparator
import com.loohp.hkbuseta.utils.sp
import com.loohp.hkbuseta.utils.toImmutableList
import com.loohp.hkbuseta.utils.withAlpha
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


data class StopEntry(
    val stopIndex: Int,
    val stopName: String,
    val stopData: StopData,
    val lat: Double,
    val lng: Double,
    var distance: Double = Double.MAX_VALUE
)

data class OriginData(
    val lat: Double,
    val lng: Double,
    val onlyInRange: Boolean = false
)

@Stable
class ListStopsActivity : ComponentActivity() {

    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 4)
    private val etaUpdatesMap: MutableMap<Int, Pair<ScheduledFuture<*>?, () -> Unit>> = LinkedHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shared.setDefaultExceptionHandler(this)
        val route = intent.extras!!.getString("route")?.let { RouteSearchResultEntry.deserialize(JSONObject(it)) }?: throw RuntimeException()
        val scrollToStop = intent.extras!!.getString("scrollToStop")
        val showEta = intent.extras!!.getBoolean("showEta", true)
        val isAlightReminder = intent.extras!!.getBoolean("isAlightReminder", false)

        setContent {
            MainElement(this, route, showEta, scrollToStop, isAlightReminder) { isAdd, index, task ->
                synchronized(etaUpdatesMap) {
                    if (isAdd) {
                        etaUpdatesMap.computeIfAbsent(index) { executor.scheduleWithFixedDelay(task, 0, 30, TimeUnit.SECONDS) to task!! }
                    } else {
                        etaUpdatesMap.remove(index)?.first?.cancel(true)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Shared.setSelfAsCurrentActivity(this)
    }

    override fun onResume() {
        super.onResume()
        synchronized(etaUpdatesMap) {
            etaUpdatesMap.replaceAll { _, value ->
                value.first?.cancel(true)
                executor.scheduleWithFixedDelay(value.second, 0, 30, TimeUnit.SECONDS) to value.second
            }
        }
    }

    override fun onPause() {
        super.onPause()
        synchronized(etaUpdatesMap) {
            etaUpdatesMap.forEach { it.value.first?.cancel(true) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            executor.shutdownNow()
            Shared.removeSelfFromCurrentActivity(this)
        }
    }

}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainElement(instance: ListStopsActivity, route: RouteSearchResultEntry, showEta: Boolean, scrollToStop: String?, isAlightReminder: Boolean, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = remember { FocusRequester() }
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val padding by remember { derivedStateOf { StringUtils.scaledSize(7.5F, instance) } }

        val kmbCtbJoint = remember { route.route.isKmbCtbJoint }
        val routeNumber = remember { route.route.routeNumber }
        val co = remember { route.co }
        val bound = remember { if (co.equals(Operator.NLB)) route.route.nlbId else route.route.bound[co] }
        val gmbRegion = remember { route.route.gmbRegion }
        val interchangeSearch = remember { route.isInterchangeSearch }
        val origName = remember { route.route.orig }
        val destName = remember { route.route.dest }
        val resolvedDestName = remember { route.route.resolvedDest(true) }
        val specialOrigsDests = remember { Registry.getInstance(instance).getAllOriginsAndDestinations(routeNumber, bound, co, gmbRegion) }
        val specialOrigs = remember { specialOrigsDests.first.stream().filter { !it.zh.eitherContains(origName.zh) }.toImmutableList() }
        val specialDests = remember { specialOrigsDests.second.stream().filter { !it.zh.eitherContains(destName.zh) }.toImmutableList() }

        val coColor = remember { co.getColor(routeNumber, Color.White) }

        val stopsList = remember { Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gmbRegion).toImmutableList() }
        val lowestServiceType = remember { stopsList.minOf { it.serviceType } }
        val mtrStopsInterchange = remember { if (co == Operator.MTR || co == Operator.LRT) {
            stopsList.stream().map { Registry.getInstance(instance).getMtrStationInterchange(it.stopId, routeNumber) }.toImmutableList()
        } else persistentListOf() }
        val isLrtCircular = remember { route.route.lrtCircular != null }
        val mtrLineCreator = remember { if (co == Operator.MTR || co == Operator.LRT) generateMTRLine(co,
            if (co == Operator.LRT) when (routeNumber) {
                "505" -> Color(0xFFDA2127)
                "507" -> Color(0xFF00A652)
                "610" -> Color(0xFF551C15)
                "614" -> Color(0xFF00BFF3)
                "614P" -> Color(0xFFF4858E)
                "615" -> Color(0xFFFFDD00)
                "615P" -> Color(0xFF016682)
                "705" -> Color(0xFF73BF43)
                "706" -> Color(0xFFB47AB5)
                "751" -> Color(0xFFF48221)
                "761P" -> Color(0xFF6F2D91)
                else -> coColor
            } else coColor, stopsList, mtrStopsInterchange, isLrtCircular, instance
        ) else null }

        val distances: MutableMap<Int, Double> = remember { ConcurrentHashMap() }

        val etaTextWidth by remember { derivedStateOf { StringUtils.findTextLengthDp(instance, "99", StringUtils.scaledSize(16F, instance)) + 1F } }

        val etaUpdateTimes = remember { ConcurrentHashMap<Int, Long>().asImmutableState() }
        val etaResults = remember { ConcurrentHashMap<Int, Registry.ETAQueryResult>().asImmutableState() }

        val rawAlightReminderData by remember { AlightReminderService.getCurrentState() }.collectAsStateWithLifecycle()
        val alightReminderData by remember { derivedStateOf { if (isAlightReminder) rawAlightReminderData else null } }

        var targetStop: Stop? by remember { mutableStateOf(null) }
        val targetStopIndex by remember { derivedStateOf { targetStop?.let { target -> stopsList.indexOfFirst { it.stop == target } + 1 }?: -1 } }
        var isTargetActive by remember { mutableStateOf(isAlightReminder) }

        var closestIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect (Unit) {
            focusRequester.requestFocus()

            val scrollTask: (OriginData?, String?) -> Unit = { origin, stopId ->
                if (stopId != null) {
                    stopsList.withIndex().find { it.value.stopId == stopId }?.let {
                        scope.launch {
                            scroll.animateScrollToItem(it.index + 2, (-ScreenSizeUtils.getScreenHeight(instance) / 2) - UnitUtils.spToPixels(instance, StringUtils.scaledSize(15F, instance)).roundToInt())
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
                        it.distance = DistanceUtils.findDistance(origin.lat, origin.lng, it.lat, it.lng)
                        distances[it.stopIndex] = it.distance
                    }.minBy {
                        it.distance
                    }.let {
                        if (!origin.onlyInRange || it.distance <= 0.3) {
                            closestIndex = it.stopIndex
                            scope.launch {
                                scroll.animateScrollToItem(it.stopIndex + 1, (-ScreenSizeUtils.getScreenHeight(instance) / 2) - UnitUtils.spToPixels(instance, StringUtils.scaledSize(15F, instance)).roundToInt())
                            }
                        }
                    }
                }
            }

            if (scrollToStop != null) {
                scrollTask.invoke(null, scrollToStop)
            } else if (route.origin != null) {
                val origin = route.origin
                scrollTask.invoke(OriginData(origin.lat, origin.lng), null)
            } else {
                LocationUtils.checkLocationPermission(instance) {
                    if (it) {
                        val future = LocationUtils.getGPSLocation(instance)
                        Thread {
                            try {
                                val locationResult = future.get()
                                if (locationResult.isSuccess) {
                                    val location = locationResult.location
                                    scrollTask.invoke(OriginData(location.lat, location.lng, true), null)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.start()
                    }
                }
            }
        }

        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalLazyScrollbar(
                    state = scroll
                )
                .rotaryScroll(scroll, focusRequester)
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
                                    val location = alightReminderData!!.currentLocation.location
                                    closestIndex = (stopsList.withIndex().minBy {
                                        DistanceUtils.findDistance(it.value.stop.location.lat, it.value.stop.location.lng, location.lat, location.lng)
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
                HeaderElement(routeNumber, kmbCtbJoint, co, coColor, resolvedDestName, specialOrigs, specialDests, instance)
            }
            for ((index, entry) in stopsList.withIndex()) {
                item {
                    val stopNumber = index + 1
                    val isClosest = closestIndex == stopNumber
                    val isTargetStop = targetStopIndex == stopNumber
                    val stopId = entry.stopId
                    val stop = entry.stop
                    val brightness = if (entry.serviceType == lowestServiceType) 1F else 0.65F
                    val rawColor = (if (isClosest) coColor else Color.White).adjustBrightness(brightness)
                    val stopStr = stop.name[Shared.language]

                    Column (
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    val intent = Intent(instance, EtaActivity::class.java)
                                    intent.putExtra("stopId", stopId)
                                    intent.putExtra("co", co.name)
                                    intent.putExtra("index", stopNumber)
                                    intent.putExtra("stop", stop.serialize().toString())
                                    intent.putExtra("route", entry.route.serialize().toString())
                                    instance.startActivity(intent)
                                },
                                onLongClick = {
                                    instance.runOnUiThread {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        instance.runOnUiThread {
                                            val prefix =
                                                if (co == Operator.MTR || co == Operator.LRT) "" else stopNumber
                                                    .toString()
                                                    .plus(". ")
                                            val text = if (isClosest && !isAlightReminder) {
                                                prefix
                                                    .plus(stopStr)
                                                    .plus("\n")
                                                    .plus(if (interchangeSearch) (if (Shared.language == "en") "Interchange " else "轉乘") else (if (Shared.language == "en") "Nearby " else "附近"))
                                                    .plus(((distances[stopNumber] ?: Double.NaN) * 1000).roundToInt().formatDecimalSeparator())
                                                    .plus(if (Shared.language == "en") "m" else "米")
                                            } else {
                                                prefix.plus(stopStr)
                                            }
                                            Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                    ) {
                        val (mtrLineSection, bottomExtensionSection) = mtrLineCreator?.get(index)?: (null to null)
                        StopRowElement(stopNumber, stopId, co, showEta, isAlightReminder, isClosest, isTargetStop, kmbCtbJoint, mtrStopsInterchange, rawColor, brightness, padding, stopStr, stopsList, mtrLineSection, route, etaTextWidth, etaResults, etaUpdateTimes, instance, schedule)
                        if (isAlightReminder && isTargetStop) {
                            var height by remember { mutableIntStateOf(0) }
                            Box (
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged {
                                        height = it.height
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if ((co == Operator.MTR || co == Operator.LRT) && bottomExtensionSection != null) {
                                    Box(
                                        modifier = Modifier
                                            .padding(25.dp, 0.dp)
                                            .requiredWidth((if (stopsList.map { it.serviceType }.distinct().size > 1 || mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }) 50 else 35).sp.dp)
                                            .requiredHeight(height.toFloat().equivalentDp)
                                            .align(Alignment.CenterStart)
                                    ) {
                                        bottomExtensionSection.invoke()
                                    }
                                }
                                Column {
                                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                                    if (isTargetActive) {
                                        TerminateAlightReminderButton(instance)
                                    } else {
                                        AlightReminderCompleted(instance)
                                    }
                                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                                }
                            }
                        }
                    }
                    Spacer(
                        modifier = Modifier
                            .padding(25.dp, 0.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF333333))
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(40, instance).dp))
            }
        }
    }
}

@Composable
fun AlightReminderCompleted(instance: ListStopsActivity) {
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(40, instance).sp.clamp(min = 40.dp).dp),
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
                        .width(StringUtils.scaledSize(20, instance).sp.clamp(max = 20.dp).dp)
                        .height(StringUtils.scaledSize(20, instance).sp.clamp(max = 20.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(17F, instance).sp.dp),
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
                    fontSize = StringUtils.scaledSize(14F, instance).sp,
                    text = if (Shared.language == "en") "Arrived" else "已到達"
                )
            }
        }
    )
}

@Composable
fun TerminateAlightReminderButton(instance: ListStopsActivity) {
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(40, instance).sp.clamp(min = 40.dp).dp),
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
                        .width(StringUtils.scaledSize(20, instance).sp.clamp(max = 20.dp).dp)
                        .height(StringUtils.scaledSize(20, instance).sp.clamp(max = 20.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(17F, instance).sp.dp),
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
                    fontSize = StringUtils.scaledSize(14F, instance).sp,
                    text = if (Shared.language == "en") "Disable Reminder" else "關閉落車提示"
                )
            }
        }
    )
}

@Composable
fun HeaderElement(routeNumber: String, kmbCtbJoint: Boolean, co: Operator, coColor: Color, destName: BilingualText, specialOrigs: ImmutableList<BilingualText>, specialDests: ImmutableList<BilingualText>, instance: ListStopsActivity) {
    Column(
        modifier = Modifier
            .defaultMinSize(minHeight = StringUtils.scaledSize(35, instance).dp)
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
                min = StringUtils.scaledSize(1F, instance).dp.sp,
                max = StringUtils.scaledSize(17F, instance).sp.clamp(max = StringUtils.scaledSize(20F, instance).dp)
            ),
            lineHeight = StringUtils.scaledSize(17F, instance).sp.clamp(max = StringUtils.scaledSize(20F, instance).dp),
            color = color,
            maxLines = 1,
            text = co.getDisplayName(routeNumber, kmbCtbJoint, Shared.language).plus(" ").plus(co.getDisplayRouteNumber(routeNumber))
        )
        AutoResizeText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 0.dp),
            textAlign = TextAlign.Center,
            fontSizeRange = FontSizeRange(
                min = StringUtils.scaledSize(1F, instance).dp.sp,
                max = StringUtils.scaledSize(11F, instance).sp.clamp(max = StringUtils.scaledSize(14F, instance).dp)
            ),
            color = Color(0xFFFFFFFF),
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
                    min = StringUtils.scaledSize(1F, instance).dp.sp,
                    max = StringUtils.scaledSize(11F, instance).sp.clamp(max = StringUtils.scaledSize(14F, instance).dp)
                ),
                color = Color(0xFFFFFFFF).adjustBrightness(0.65F),
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
                    min = StringUtils.scaledSize(1F, instance).dp.sp,
                    max = StringUtils.scaledSize(11F, instance).sp.clamp(max = StringUtils.scaledSize(14F, instance).dp)
                ),
                color = Color(0xFFFFFFFF).adjustBrightness(0.65F),
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
fun StopRowElement(stopNumber: Int, stopId: String, co: Operator, showEta: Boolean, isAlightReminder: Boolean, isClosest: Boolean, isTargetStop: Boolean, kmbCtbJoint: Boolean, mtrStopsInterchange: ImmutableList<Registry.MTRInterchangeData>, rawColor: Color, brightness: Float, padding: Float, stopStr: String, stopList: ImmutableList<StopData>, mtrLineCreator: (@Composable () -> Unit)?, route: RouteSearchResultEntry, etaTextWidth: Float, etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>, instance: ListStopsActivity, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    var height by remember { mutableIntStateOf(0) }
    Row (
        modifier = Modifier
            .padding(25.dp, 0.dp)
            .onSizeChanged {
                height = it.height
            }
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
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

        if ((co == Operator.MTR || co == Operator.LRT) && mtrLineCreator != null) {
            Box(
                modifier = Modifier
                    .requiredWidth((if (stopList.map { it.serviceType }.distinct().size > 1 || mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }) 50 else 35).sp.dp)
                    .requiredHeight(height.toFloat().equivalentDp)
            ) {
                mtrLineCreator.invoke()
            }
        } else {
            Text(
                modifier = Modifier
                    .padding(0.dp, padding.dp)
                    .requiredWidth(30.dp),
                textAlign = TextAlign.Start,
                fontSize = StringUtils.scaledSize(15F, instance).sp,
                fontWeight = if (isClosest || isTargetStop) FontWeight.Bold else FontWeight.Normal,
                color = color,
                maxLines = 1,
                text = stopNumber.toString().plus(".")
            )
        }
        Text(
            modifier = Modifier
                .padding(0.dp, padding.dp)
                .weight(1F)
                .basicMarquee(iterations = Int.MAX_VALUE),
            textAlign = TextAlign.Start,
            fontSize = StringUtils.scaledSize(15F, instance).sp,
            fontWeight = if (isClosest || isTargetStop) FontWeight.Bold else FontWeight.Normal,
            color = color,
            maxLines = 1,
            text = stopStr
        )
        if ((isAlightReminder && isTargetStop && !isClosest) || (isAlightReminder && isClosest) || showEta) {
            Box (
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .requiredWidth(etaTextWidth.dp)
            ) {
                if (isAlightReminder && isTargetStop && !isClosest) {
                    Icon (
                        painter = painterResource(R.drawable.baseline_notifications_active_24),
                        tint = Color(0xFFFF9800),
                        contentDescription = if (Shared.language == "en") "Alight Reminder" else "落車提示"
                    )
                } else if (isAlightReminder && isClosest) {
                    Icon (
                        painter = painterResource(R.drawable.baseline_location_on_24),
                        tint = Color(0xFFFF0000),
                        contentDescription = if (Shared.language == "en") "Alight Reminder" else "落車提示"
                    )
                } else {
                    ETAElement(stopNumber, stopId, route, etaResults, etaUpdateTimes, instance, schedule)
                }
            }
        }
    }
}

@Composable
fun ETAElement(index: Int, stopId: String, route: RouteSearchResultEntry, etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>, instance: ListStopsActivity, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    var eta: Registry.ETAQueryResult? by remember { mutableStateOf(etaResults.value[index]) }

    LaunchedEffect (Unit) {
        if (eta != null && !eta!!.isConnectionError) {
            delay(etaUpdateTimes.value[index]?.let { (Shared.ETA_UPDATE_INTERVAL - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        schedule.invoke(true, index) {
            eta = Registry.getInstance(instance).getEta(stopId, route.co, route.route, instance)
            etaUpdateTimes.value[index] = System.currentTimeMillis()
            etaResults.value[index] = eta!!
        }
    }
    DisposableEffect (Unit) {
        onDispose {
            schedule.invoke(false, index, null)
        }
    }
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .offset(0.dp, -2F.sp.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        if (eta != null && !eta!!.isConnectionError) {
            if (eta!!.nextScheduledBus < 0 || eta!!.nextScheduledBus > 60) {
                if (eta!!.isMtrEndOfLine) {
                    Icon(
                        modifier = Modifier
                            .size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp).dp)
                            .offset(0.dp, StringUtils.scaledSize(3F, instance).sp.clamp(max = StringUtils.scaledSize(4F, instance).dp).dp),
                        painter = painterResource(R.drawable.baseline_line_end_circle_24),
                        contentDescription = if (Shared.language == "en") "End of Line" else "終點站",
                        tint = Color(0xFF798996),
                    )
                } else if (eta!!.isTyphoonSchedule) {
                    val desc by remember { derivedStateOf { Registry.getInstance(instance).currentTyphoonData.get().typhoonWarningTitle } }
                    Image(
                        modifier = Modifier
                            .size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp).dp)
                            .offset(0.dp, StringUtils.scaledSize(3F, instance).sp.clamp(max = StringUtils.scaledSize(4F, instance).dp).dp),
                        painter = painterResource(R.mipmap.cyclone),
                        contentDescription = desc
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp).dp)
                            .offset(0.dp, StringUtils.scaledSize(3F, instance).sp.clamp(max = StringUtils.scaledSize(4F, instance).dp).dp),
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        contentDescription = if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次",
                        tint = Color(0xFF798996),
                    )
                }
            } else {
                val text1 = (if (eta!!.nextScheduledBus == 0L) "-" else eta!!.nextScheduledBus.toString())
                val text2 = if (Shared.language == "en") "Min." else "分鐘"
                val span1 = SpannableString(text1)
                val size1 = UnitUtils.spToPixels(instance, clampSp(instance, StringUtils.scaledSize(14F, instance), dpMax = StringUtils.scaledSize(15F, instance))).roundToInt()
                span1.setSpan(AbsoluteSizeSpan(size1), 0, text1.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                val span2 = SpannableString(text2)
                val size2 = UnitUtils.spToPixels(instance, clampSp(instance, StringUtils.scaledSize(7F, instance), dpMax = StringUtils.scaledSize(8F, instance))).roundToInt()
                span2.setSpan(AbsoluteSizeSpan(size2), 0, text2.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                AnnotatedText(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 14F.sp,
                    color = Color(0xFFAAC3D5),
                    lineHeight = 7F.sp,
                    maxLines = 2,
                    text = SpannableString(TextUtils.concat(span1, "\n", span2)).asAnnotatedString()
                )
            }
        }
    }
}

fun hasOtherParallelBranches(stopList: List<StopData>, stopByBranchId: MutableMap<Int, MutableList<StopData>>, stop: StopData): Boolean {
    if (stopByBranchId.size == stopByBranchId.filter { it.value.contains(stop) }.size) {
        return false
    }
    val mainIndex = stopList.indexOf(stop)
    val branchIds = stop.branchIds
    var branchStart = -1
    var branchStartStop: StopData? = null
    for (i in (mainIndex - 1) downTo 0) {
        if (stopList[i].branchIds != branchIds && stopList[i].branchIds.containsAll(branchIds)) {
            branchStart = i
            branchStartStop = stopList[i]
            break
        }
    }
    if (branchStartStop == null) {
        for (i in stopList.indices) {
            if (stopList[i].branchIds != branchIds) {
                branchStart = i
                branchStartStop = stopList[i]
                break
            }
        }
    }
    var branchEnd = stopList.size
    var branchEndStop: StopData? = null
    for (i in (mainIndex + 1) until stopList.size) {
        if (stopList[i].branchIds != branchIds && stopList[i].branchIds.containsAll(branchIds)) {
            branchEnd = i
            branchEndStop = stopList[i]
            break
        }
    }
    if (branchEndStop == null) {
        for (i in (stopList.size - 1) downTo 0) {
            if (stopList[i].branchIds != branchIds) {
                branchEnd = i
                branchEndStop = stopList[i]
                break
            }
        }
    }
    val matchingBranchStart = branchStart == mainIndex
    val matchingBranchEnd = branchEnd == mainIndex
    val isStartOfSpur = matchingBranchStart && stopByBranchId.values.none { it.indexOf(branchStartStop) <= 0 }
    val isEndOfSpur = matchingBranchEnd && stopByBranchId.values.none { it.indexOf(branchEndStop) >= (it.size - 1) }
    if (matchingBranchStart != isStartOfSpur || matchingBranchEnd != isEndOfSpur) {
        return false
    }
    return mainIndex in branchStart..branchEnd
}

enum class SideSpurLineType {

    NONE, COMBINE, DIVERGE

}

fun getSideSpurLineType(stopList: List<StopData>, stopByBranchId: MutableMap<Int, MutableList<StopData>>, stop: StopData): SideSpurLineType {
    val mainIndex = stopList.indexOf(stop)
    val branchIds = stop.branchIds
    if (mainIndex > 0) {
        if (stopList[mainIndex - 1].branchIds != branchIds) {
            if (stopByBranchId.values.all { (!it.contains(stopList[mainIndex - 1]) || it.subList(0, it.indexOf(stopList[mainIndex - 1])).none { that -> that.branchIds.containsAll(branchIds) }) && it.indexOf(stop) != 0 }) {
                return SideSpurLineType.COMBINE
            }
        }
    }
    if (mainIndex < stopList.size - 1) {
        if (stopList[mainIndex + 1].branchIds != branchIds) {
            if (stopByBranchId.values.all { (!it.contains(stopList[mainIndex + 1]) || it.subList(it.indexOf(stopList[mainIndex + 1]) + 1, it.size).none { that -> that.branchIds.containsAll(branchIds) }) && it.indexOf(stop) < it.size - 1 }) {
                return SideSpurLineType.DIVERGE
            }
        }
    }
    return SideSpurLineType.NONE
}

data class DashLineSpurResult(val value: Boolean, val isStartOfSpur: Boolean, val isEndOfSpur: Boolean) {

    companion object {

        val FALSE = DashLineSpurResult(value = false, isStartOfSpur = false, isEndOfSpur = false)

    }

}

fun isDashLineSpur(stopList: List<StopData>, stop: StopData): DashLineSpurResult {
    val mainIndex = stopList.indexOf(stop)
    val branchIds = stop.branchIds
    var possible = false
    var branchStart = false
    for (i in (mainIndex - 1) downTo 0) {
        if (stopList[i].branchIds.containsAll(branchIds)) {
            if (i + 1 == mainIndex && stopList[i].branchIds != branchIds) {
                branchStart = true
            }
            possible = true
            break
        }
    }
    if (!possible) {
        return DashLineSpurResult.FALSE
    }
    for (i in (mainIndex + 1) until stopList.size) {
        if (stopList[i].branchIds.containsAll(branchIds)) {
            return DashLineSpurResult(true, branchStart, i - 1 == mainIndex && stopList[i].branchIds != branchIds)
        }
    }
    return DashLineSpurResult.FALSE
}

fun generateMTRLine(co: Operator, color: Color, stopList: List<StopData>, mtrStopsInterchange: List<Registry.MTRInterchangeData>, isLrtCircular: Boolean, instance: ListStopsActivity): List<Pair<@Composable () -> Unit, @Composable () -> Unit>> {
    val creators: MutableList<Pair<@Composable () -> Unit, @Composable () -> Unit>> = ArrayList(stopList.size)
    val stopByBranchId: MutableMap<Int, MutableList<StopData>> = HashMap()
    stopList.forEach { stop -> stop.branchIds.forEach { stopByBranchId.computeIfAbsent(it) { ArrayList() }.add(stop) } }
    val hasOutOfStation = mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }
    for ((index, stop) in stopList.withIndex()) {
        val isMainLine = stop.serviceType == 1
        val interchangeData = mtrStopsInterchange[index]
        creators.add(@Composable {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height

                val leftShift = if (hasOutOfStation) StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 22F).sp.toPx(), instance) else 0
                val horizontalCenter = width / 2F - leftShift.toFloat()
                val horizontalPartition = width / 10F
                val horizontalCenterPrimary = if (stopByBranchId.size == 1) horizontalCenter else horizontalPartition * 3F
                val horizontalCenterSecondary = horizontalPartition * 7F
                val verticalCenter = height / 2F
                val lineWidth = StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 11F).sp.toPx(), instance)
                val lineOffset = StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 8F).sp.toPx(), instance)
                val dashEffect = floatArrayOf(StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 14F).sp.toPx(), instance), StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 7F).sp.toPx(), instance))

                var useSpurStopCircle = false
                if (isMainLine) {
                    if (stopByBranchId.values.all { it.indexOf(stop) <= 0 }) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterPrimary, verticalCenter),
                            end = Offset(horizontalCenterPrimary, height),
                            strokeWidth = lineWidth
                        )
                        if (isLrtCircular) {
                            drawLine(
                                brush = Brush.linearGradient(
                                    0F to color.withAlpha(0),
                                    1F to color,
                                    start = Offset(horizontalCenterPrimary, -verticalCenter / 2),
                                    end = Offset(horizontalCenterPrimary, verticalCenter)
                                ),
                                start = Offset(horizontalCenterPrimary, -verticalCenter),
                                end = Offset(horizontalCenterPrimary, verticalCenter),
                                strokeWidth = lineWidth
                            )
                        }
                    } else if (stopByBranchId.values.all { it.indexOf(stop).let { x -> x < 0 || x >= it.size - 1 } }) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterPrimary, 0F),
                            end = Offset(horizontalCenterPrimary, verticalCenter),
                            strokeWidth = lineWidth
                        )
                        if (isLrtCircular) {
                            drawLine(
                                brush = Brush.linearGradient(
                                    0F to color,
                                    1F to color.withAlpha(0),
                                    start = Offset(horizontalCenterPrimary, verticalCenter),
                                    end = Offset(horizontalCenterPrimary, height + verticalCenter / 2)
                                ),
                                start = Offset(horizontalCenterPrimary, verticalCenter),
                                end = Offset(horizontalCenterPrimary, height + verticalCenter),
                                strokeWidth = lineWidth
                            )
                        }
                    } else {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterPrimary, 0F),
                            end = Offset(horizontalCenterPrimary, height),
                            strokeWidth = lineWidth
                        )
                    }
                    if (hasOtherParallelBranches(stopList, stopByBranchId, stop)) {
                        val path = Path()
                        path.moveTo(horizontalCenterPrimary, lineOffset)
                        path.lineTo(horizontalCenter, lineOffset)
                        path.arcTo(Rect(horizontalCenterPrimary, lineOffset, horizontalCenterSecondary, (horizontalCenterSecondary - horizontalCenter) * 2F + lineOffset), -90F, 90F, true)
                        path.lineTo(horizontalCenterSecondary, height)
                        drawPath(
                            color = color,
                            path = path,
                            style = Stroke(
                                width = lineWidth,
                                pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                            )
                        )
                    }
                    when (getSideSpurLineType(stopList, stopByBranchId, stop)) {
                        SideSpurLineType.COMBINE -> {
                            useSpurStopCircle = true
                            val path = Path()
                            path.moveTo(horizontalCenterPrimary, verticalCenter)
                            path.lineTo(horizontalCenter, verticalCenter)
                            path.arcTo(Rect(horizontalCenterPrimary, verticalCenter - (horizontalCenterSecondary - horizontalCenter) * 2F, horizontalCenterSecondary, verticalCenter), 90F, -90F, true)
                            path.lineTo(horizontalCenterSecondary, 0F)
                            drawPath(
                                color = color,
                                path = path,
                                style = Stroke(
                                    width = lineWidth
                                )
                            )
                        }
                        SideSpurLineType.DIVERGE -> {
                            useSpurStopCircle = true
                            val path = Path()
                            path.moveTo(horizontalCenterPrimary, verticalCenter)
                            path.lineTo(horizontalCenter, verticalCenter)
                            path.arcTo(Rect(horizontalCenterPrimary, verticalCenter, horizontalCenterSecondary, verticalCenter + (horizontalCenterSecondary - horizontalCenter) * 2F), -90F, 90F, true)
                            path.lineTo(horizontalCenterSecondary, height)
                            drawPath(
                                color = color,
                                path = path,
                                style = Stroke(
                                    width = lineWidth
                                )
                            )
                        }
                        else -> {}
                    }
                } else {
                    if (index > 0 && index < stopList.size - 1) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterPrimary, 0F),
                            end = Offset(horizontalCenterPrimary, height),
                            strokeWidth = lineWidth
                        )
                    }
                    val dashLineResult = isDashLineSpur(stopList, stop)
                    if (dashLineResult.value) {
                        if (dashLineResult.isStartOfSpur) {
                            val path = Path()
                            path.moveTo(horizontalCenterPrimary, lineOffset)
                            path.lineTo(horizontalCenter, lineOffset)
                            path.arcTo(Rect(horizontalCenterPrimary, lineOffset, horizontalCenterSecondary, (horizontalCenterSecondary - horizontalCenter) * 2F + lineOffset), -90F, 90F, true)
                            path.lineTo(horizontalCenterSecondary, height)
                            drawPath(
                                color = color,
                                path = path,
                                style = Stroke(
                                    width = lineWidth,
                                    pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                                )
                            )
                        } else if (dashLineResult.isEndOfSpur) {
                            val path = Path()
                            path.moveTo(horizontalCenterPrimary, height - lineOffset)
                            path.lineTo(horizontalCenter, height - lineOffset)
                            path.arcTo(Rect(horizontalCenterPrimary, height - (horizontalCenterSecondary - horizontalCenter) * 2F - lineOffset, horizontalCenterSecondary, height - lineOffset), 90F, -90F, true)
                            path.lineTo(horizontalCenterSecondary, 0F)
                            drawPath(
                                color = color,
                                path = path,
                                style = Stroke(
                                    width = lineWidth,
                                    pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                                )
                            )
                        } else {
                            drawLine(
                                color = color,
                                start = Offset(horizontalCenterSecondary, 0F),
                                end = Offset(horizontalCenterSecondary, height),
                                strokeWidth = lineWidth,
                                pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                            )
                        }
                    } else if (stopByBranchId.values.all { it.indexOf(stop) <= 0 }) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterSecondary, verticalCenter),
                            end = Offset(horizontalCenterSecondary, height),
                            strokeWidth = lineWidth
                        )
                    } else if (stopByBranchId.values.all { it.indexOf(stop).let { x -> x < 0 || x >= it.size - 1 } }) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterSecondary, 0F),
                            end = Offset(horizontalCenterSecondary, verticalCenter),
                            strokeWidth = lineWidth
                        )
                    } else {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterSecondary, 0F),
                            end = Offset(horizontalCenterSecondary, height),
                            strokeWidth = lineWidth
                        )
                    }
                }
                val interchangeLineWidth = StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 14F).sp.toPx(), instance) * 2F
                val interchangeLineHeight = StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 6F).sp.toPx(), instance) * 2F
                val interchangeLineSpacing = interchangeLineHeight * 1.5F
                if (interchangeData.isHasLightRail && co != Operator.LRT) {
                    drawRoundRect(
                        color = Color(0xFFD3A809),
                        topLeft = Offset(horizontalCenterPrimary - interchangeLineWidth, verticalCenter - interchangeLineHeight / 2F),
                        size = Size(interchangeLineWidth, interchangeLineHeight),
                        cornerRadius = CornerRadius(interchangeLineHeight / 2F)
                    )
                } else if (interchangeData.lines.isNotEmpty()) {
                    var leftCorner = Offset(horizontalCenterPrimary - interchangeLineWidth, verticalCenter - ((interchangeData.lines.size - 1) * interchangeLineSpacing / 2F) - interchangeLineHeight / 2F)
                    for (interchange in interchangeData.lines) {
                        drawRoundRect(
                            color = when (interchange) {
                                "AEL" -> Color(0xFF00888E)
                                "TCL" -> Color(0xFFF3982D)
                                "TML" -> Color(0xFF9C2E00)
                                "TKL" -> Color(0xFF7E3C93)
                                "EAL" -> Color(0xFF5EB7E8)
                                "SIL" -> Color(0xFFCBD300)
                                "TWL" -> Color(0xFFE60012)
                                "ISL" -> Color(0xFF0075C2)
                                "KTL" -> Color(0xFF00A040)
                                "DRL" -> Color(0xFFEB6EA5)
                                "HighSpeed" -> Color(0xFFBCB0A4)
                                else -> Color.White
                            },
                            topLeft = leftCorner,
                            size = Size(interchangeLineWidth, interchangeLineHeight),
                            cornerRadius = CornerRadius(interchangeLineHeight / 2F)
                        )
                        leftCorner += Offset(0F, interchangeLineSpacing)
                    }
                }

                val circleWidth = StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 20F).sp.toPx(), instance)

                if (interchangeData.outOfStationLines.isNotEmpty()) {
                    val otherStationHorizontalCenter = horizontalCenterPrimary + circleWidth * 2F
                    val connectionLineWidth = StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 5F).sp.toPx(), instance)
                    if (interchangeData.isOutOfStationPaid) {
                        drawLine(
                            color = Color(0xFF003180),
                            start = Offset(horizontalCenterPrimary, verticalCenter),
                            end = Offset(otherStationHorizontalCenter, verticalCenter),
                            strokeWidth = connectionLineWidth
                        )
                    } else {
                        drawLine(
                            color = Color(0xFF003180),
                            start = Offset(horizontalCenterPrimary, verticalCenter),
                            end = Offset(otherStationHorizontalCenter, verticalCenter),
                            strokeWidth = connectionLineWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 4F).sp.toPx(), instance), StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 2F).sp.toPx(), instance)), 0F)
                        )
                    }
                    var leftCorner = Offset(otherStationHorizontalCenter, verticalCenter - ((interchangeData.outOfStationLines.size - 1) * interchangeLineSpacing / 2F) - interchangeLineHeight / 2F)
                    for (interchange in interchangeData.outOfStationLines) {
                        drawRoundRect(
                            color = when (interchange) {
                                "AEL" -> Color(0xFF00888E)
                                "TCL" -> Color(0xFFF3982D)
                                "TML" -> Color(0xFF9C2E00)
                                "TKL" -> Color(0xFF7E3C93)
                                "EAL" -> Color(0xFF5EB7E8)
                                "SIL" -> Color(0xFFCBD300)
                                "TWL" -> Color(0xFFE60012)
                                "ISL" -> Color(0xFF0075C2)
                                "KTL" -> Color(0xFF00A040)
                                "DRL" -> Color(0xFFEB6EA5)
                                "HighSpeed" -> Color(0xFFBCB0A4)
                                else -> Color.White
                            },
                            topLeft = leftCorner,
                            size = Size(interchangeLineWidth, interchangeLineHeight),
                            cornerRadius = CornerRadius(interchangeLineHeight / 2F)
                        )
                        leftCorner += Offset(0F, interchangeLineSpacing)
                    }

                    val circleCenter = Offset(otherStationHorizontalCenter, verticalCenter)
                    val heightExpand = ((interchangeData.outOfStationLines.size - 1) * interchangeLineSpacing).coerceAtLeast(0F)
                    drawRoundRect(
                        color = Color(0xFF003180),
                        topLeft = circleCenter - Offset(circleWidth / 1.4F, circleWidth / 1.4F + heightExpand / 2F),
                        size = Size((circleWidth / 1.4F * 2F), circleWidth / 1.4F * 2F + heightExpand),
                        cornerRadius = CornerRadius(circleWidth / 1.4F)
                    )
                    drawRoundRect(
                        color = Color(0xFFFFFFFF),
                        topLeft = circleCenter - Offset(circleWidth / 2F, circleWidth / 2F + heightExpand / 2F),
                        size = Size(circleWidth, circleWidth + heightExpand),
                        cornerRadius = CornerRadius(circleWidth / 2F)
                    )
                }

                val circleCenter = Offset(if (isMainLine) horizontalCenterPrimary else horizontalCenterSecondary, verticalCenter)
                val widthExpand = if (useSpurStopCircle) lineWidth else 0F
                val heightExpand = ((interchangeData.lines.size - 1) * interchangeLineSpacing).coerceAtLeast(0F)
                drawRoundRect(
                    color = Color(0xFF003180),
                    topLeft = circleCenter - Offset(circleWidth / 1.4F, circleWidth / 1.4F + heightExpand / 2F),
                    size = Size((circleWidth / 1.4F * 2F) + widthExpand, circleWidth / 1.4F * 2F + heightExpand),
                    cornerRadius = CornerRadius(circleWidth / 1.4F)
                )
                drawRoundRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = circleCenter - Offset(circleWidth / 2F, circleWidth / 2F + heightExpand / 2F),
                    size = Size(circleWidth + widthExpand, circleWidth + heightExpand),
                    cornerRadius = CornerRadius(circleWidth / 2F)
                )
            }
        } to @Composable {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                if (index + 1 >= stopList.size) {
                    return@Canvas
                }

                val width = size.width
                val height = size.height

                val leftShift = if (hasOutOfStation) StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 22F).sp.toPx(), instance) else 0
                val horizontalCenter = width / 2F - leftShift.toFloat()
                val horizontalPartition = width / 10F
                val horizontalCenterPrimary = if (stopByBranchId.size == 1) horizontalCenter else horizontalPartition * 3F
                val horizontalCenterSecondary = horizontalPartition * 7F
                val lineWidth = StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 11F).sp.toPx(), instance)
                val dashEffect = floatArrayOf(StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 14F).sp.toPx(), instance), StringUtils.scaledSize(UnitUtils.pixelsToDp(instance, 7F).sp.toPx(), instance))

                if (isMainLine) {
                    drawLine(
                        color = color,
                        start = Offset(horizontalCenterPrimary, 0F),
                        end = Offset(horizontalCenterPrimary, height),
                        strokeWidth = lineWidth
                    )
                    if (hasOtherParallelBranches(stopList, stopByBranchId, stop)) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterSecondary, 0F),
                            end = Offset(horizontalCenterSecondary, height),
                            strokeWidth = lineWidth,
                            pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                        )
                    }
                    when (getSideSpurLineType(stopList, stopByBranchId, stop)) {
                        SideSpurLineType.DIVERGE -> {
                            drawLine(
                                color = color,
                                start = Offset(horizontalCenterSecondary, 0F),
                                end = Offset(horizontalCenterSecondary, height),
                                strokeWidth = lineWidth
                            )
                        }
                        else -> {}
                    }
                } else {
                    drawLine(
                        color = color,
                        start = Offset(horizontalCenterPrimary, 0F),
                        end = Offset(horizontalCenterPrimary, height),
                        strokeWidth = lineWidth
                    )
                    val dashLineResult = isDashLineSpur(stopList, stop)
                    if (dashLineResult.value) {
                        if (dashLineResult.isStartOfSpur) {
                            drawLine(
                                color = color,
                                start = Offset(horizontalCenterSecondary, 0F),
                                end = Offset(horizontalCenterSecondary, height),
                                strokeWidth = lineWidth,
                                pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                            )
                        } else if (!dashLineResult.isEndOfSpur) {
                            drawLine(
                                color = color,
                                start = Offset(horizontalCenterSecondary, 0F),
                                end = Offset(horizontalCenterSecondary, height),
                                strokeWidth = lineWidth,
                                pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                            )
                        }
                    } else if (stopByBranchId.values.all { it.indexOf(stop) <= 0 }) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterSecondary, 0F),
                            end = Offset(horizontalCenterSecondary, height),
                            strokeWidth = lineWidth
                        )
                    } else if (!stopByBranchId.values.all { it.indexOf(stop).let { x -> x < 0 || x >= it.size - 1 } }) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterSecondary, 0F),
                            end = Offset(horizontalCenterSecondary, height),
                            strokeWidth = lineWidth
                        )
                    }
                }
            }
        })
    }
    return creators
}