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

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.google.android.horologist.compose.ambient.AmbientStateUpdate
import com.loohp.hkbuseta.appcontext.AppActiveContext
import com.loohp.hkbuseta.appcontext.AppIntent
import com.loohp.hkbuseta.appcontext.AppIntentFlag
import com.loohp.hkbuseta.appcontext.AppScreen
import com.loohp.hkbuseta.appcontext.ToastDuration
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.PauseEffect
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.ambientMode
import com.loohp.hkbuseta.compose.rememberIsInAmbientMode
import com.loohp.hkbuseta.objects.BilingualText
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.Route
import com.loohp.hkbuseta.objects.Stop
import com.loohp.hkbuseta.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.objects.isTrain
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Registry.ETAQueryResult
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.sameValueAs
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.sp
import com.loohp.hkbuseta.utils.toSpanned
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit


@OptIn(ExperimentalWearMaterialApi::class, ExperimentalWearFoundationApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun EtaElement(ambientStateUpdate: AmbientStateUpdate, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, offsetStart: Int, instance: AppActiveContext, schedule: (Boolean, (() -> Unit)?) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val swipe = rememberSwipeableState(initialValue = false)
    var swiping by remember { mutableStateOf(swipe.offset.value != 0F) }
    val ambientMode = rememberIsInAmbientMode(ambientStateUpdate)

    val routeNumber = route.routeNumber

    if (swipe.currentValue) {
        instance.runOnUiThread {
            val text = if (Shared.language == "en") {
                "Nearby Interchange Routes of ".plus(stop.name.en)
            } else {
                "".plus(stop.name.zh).plus(" 附近轉乘路線")
            }
            instance.showToastText(text, ToastDuration.LONG)
        }
        val intent = AppIntent(instance, AppScreen.NEARBY)
        intent.putExtra("interchangeSearch", true)
        intent.putExtra("lat", stop.location.lat)
        intent.putExtra("lng", stop.location.lng)
        intent.putExtra("exclude", arrayListOf(route.routeNumber))
        instance.startActivity(intent) {
            scope.launch {
                swipe.snapTo(false)
            }
        }
    }
    if (!swiping && !swipe.offset.value.sameValueAs(0F)) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        swiping = true
    } else if (swipe.offset.value.sameValueAs(0F)) {
        swiping = false
    }

    val stopList = remember { Registry.getInstance(instance).getAllStops(
        route.routeNumber,
        if (co == Operator.NLB) route.nlbId else route.bound[co]!!,
        co,
        route.gmbRegion
    ).toImmutableList() }

    val focusRequester = rememberActiveFocusRequester()
    var currentOffset by remember { mutableFloatStateOf(offsetStart * instance.screenHeight.toFloat()) }
    var animatedOffsetTask: (Float) -> Unit by remember { mutableStateOf({}) }
    val animatedOffset by animateFloatAsState(
        targetValue = currentOffset,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        finishedListener = animatedOffsetTask,
        label = "OffsetAnimation"
    )

    LaunchedEffect (Unit) {
        currentOffset = 0F
    }

    HKBusETATheme {
        Box (
            modifier = Modifier
                .ambientMode(ambientStateUpdate)
                .fillMaxSize()
                .composed {
                    this.offset(
                        animatedOffset.equivalentDp,
                        swipe.offset.value.coerceAtMost(0F).equivalentDp
                    )
                }
                .swipeable(
                    state = swipe,
                    anchors = mapOf(
                        0F to false,
                        -instance.screenHeight.toFloat() to true
                    ),
                    orientation = Orientation.Vertical
                )
                .onRotaryScrollEvent {
                    if (it.horizontalScrollPixels > 0) {
                        if (index < stopList.size) {
                            currentOffset = -instance.screenWidth.toFloat()
                            animatedOffsetTask = { launchOtherStop(index + 1, co, stopList, true, 1, instance) }
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    } else {
                        if (index > 1) {
                            currentOffset = instance.screenWidth.toFloat()
                            animatedOffsetTask = { launchOtherStop(index - 1, co, stopList, true, -1, instance) }
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                verticalArrangement = Arrangement.Top
            ) {
                Shared.MainTime()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val lat = stop.location.lat
                val lng = stop.location.lng

                var active by remember { mutableStateOf(true) }
                val etaStateFlow = remember { MutableStateFlow(null as ETAQueryResult?) }
                val eta by etaStateFlow.collectAsStateWithLifecycle()

                PauseEffect {
                    active = false
                }
                RestartEffect {
                    active = true
                }

                LaunchedEffect (Unit) {
                    schedule.invoke(true) {
                        val result = Registry.getInstance(instance).getEta(stopId, index, co, route, instance).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
                        if (active) {
                            etaStateFlow.value = result
                        }
                    }
                }
                DisposableEffect (Unit) {
                    onDispose {
                        schedule.invoke(false, null)
                    }
                }

                Spacer(modifier = Modifier.size(7.scaledSize(instance).dp))
                Title(ambientMode, index, stop.name, lat, lng, routeNumber, co, instance)
                SubTitle(ambientMode, Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route, true), lat, lng, routeNumber, co, instance)
                Spacer(modifier = Modifier.size(9.scaledSize(instance).dp))
                EtaText(ambientMode, eta, 1, instance)
                Spacer(modifier = Modifier.size(3.scaledSize(instance).dp))
                EtaText(ambientMode, eta, 2, instance)
                Spacer(modifier = Modifier.size(3.scaledSize(instance).dp))
                EtaText(ambientMode, eta, 3, instance)
                Spacer(modifier = Modifier.size(3.scaledSize(instance).dp))
                if (ambientMode) {
                    Spacer(modifier = Modifier.size(24.scaledSize(instance).dp))
                } else {
                    ActionBar(stopId, co, index, stop, route, stopList, instance)
                }
            }
        }
    }
}

fun launchOtherStop(newIndex: Int, co: Operator, stopList: List<Registry.StopData>, animation: Boolean, offset: Int, instance: AppActiveContext) {
    val newStopData = stopList[newIndex - 1]
    val intent = AppIntent(instance, AppScreen.ETA)
    intent.putExtra("stopId", newStopData.stopId)
    intent.putExtra("co", co.name)
    intent.putExtra("index", newIndex)
    intent.putExtra("stop", newStopData.stop.toByteArray())
    intent.putExtra("route", newStopData.route.toByteArray())
    intent.putExtra("offset", offset)
    if (!animation) {
        intent.addFlags(AppIntentFlag.NO_ANIMATION)
    }
    instance.startActivity(intent)
    instance.finish()
}

@Composable
fun ActionBar(stopId: String, co: Operator, index: Int, stop: Stop, route: Route, stopList: ImmutableList<Registry.StopData>, instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    Row (
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdvanceButton(
            onClick = {
                launchOtherStop(index - 1, co, stopList, true, -1, instance)
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                launchOtherStop(1, co, stopList, true, -1, instance)
            },
            modifier = Modifier
                .width(24.scaledSize(instance).dp)
                .height(24.scaledSize(instance).dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = if (index > 1) Color(0xFFFFFFFF) else Color(0xFF494949)
            ),
            enabled = index > 1,
            content = {
                Icon(
                    modifier = Modifier.size(16F.scaledSize(instance).sp.clamp(max = 16F.scaledSize(instance).dp).dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (Shared.language == "en") "Previous Stop" else "上一站",
                    tint = if (index > 1) Color(0xFFFFFFFF) else Color(0xFF494949),
                )
            }
        )
        Spacer(modifier = Modifier.size(2.scaledSize(instance).dp))
        AdvanceButton(
            onClick = {
                val intent = AppIntent(instance, AppScreen.ETA_MENU)
                intent.putExtra("stopId", stopId)
                intent.putExtra("co", co.name)
                intent.putExtra("index", index)
                intent.putExtra("stop", stop.toByteArray())
                intent.putExtra("route", route.toByteArray())
                instance.startActivity(intent)
            },
            modifier = Modifier
                .width(55.scaledSize(instance).dp)
                .height(24.scaledSize(instance).dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.primary
            ),
            content = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = 12F.scaledSize(instance).sp.clamp(max = 12.dp),
                    text = if (Shared.language == "en") "More" else "更多"
                )
            }
        )
        Spacer(modifier = Modifier.size(2.scaledSize(instance).dp))
        AdvanceButton(
            onClick = {
                launchOtherStop(index + 1, co, stopList, true, 1, instance)
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                launchOtherStop(stopList.size, co, stopList, true, 1, instance)
            },
            modifier = Modifier
                .width(24.scaledSize(instance).dp)
                .height(24.scaledSize(instance).dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = if (index < stopList.size) Color(0xFFFFFFFF) else Color(0xFF494949)
            ),
            enabled = index < stopList.size,
            content = {
                Icon(
                    modifier = Modifier.size(16F.scaledSize(instance).sp.clamp(max = 16F.scaledSize(instance).dp).dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = if (Shared.language == "en") "Next Stop" else "下一站",
                    tint = if (index < stopList.size) Color(0xFFFFFFFF) else Color(0xFF494949),
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Title(ambientMode: Boolean, index: Int, stopName: BilingualText, lat: Double, lng: Double, routeNumber: String, co: Operator, instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    val name = stopName[Shared.language]
    AutoResizeText (
        modifier = Modifier
            .fillMaxWidth()
            .padding(37.dp, 0.dp)
            .combinedClickable(
                onClick = instance.handleOpenMaps(lat, lng, name, false, haptic),
                onLongClick = instance.handleOpenMaps(lat, lng, name, true, haptic)
            ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary.adjustBrightness(if (ambientMode) 0.7F else 1F),
        text = if (co.isTrain) name else index.toString().plus(". ").plus(name),
        maxLines = 2,
        fontWeight = FontWeight.Bold,
        fontSizeRange = FontSizeRange(
            min = 1F.scaledSize(instance).dp.sp,
            max = 17F.scaledSize(instance).sp.clamp(max = 17F.scaledSize(instance).dp)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubTitle(ambientMode: Boolean, destName: BilingualText, lat: Double, lng: Double, routeNumber: String, co: Operator, instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    val name = co.getDisplayRouteNumber(routeNumber).plus(" ").plus(destName[Shared.language])
    AutoResizeText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp)
            .combinedClickable(
                onClick = instance.handleOpenMaps(lat, lng, name, false, haptic),
                onLongClick = instance.handleOpenMaps(lat, lng, name, true, haptic)
            ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary.adjustBrightness(if (ambientMode) 0.7F else 1F),
        text = name,
        maxLines = 1,
        fontSizeRange = FontSizeRange(
            min = 1F.scaledSize(instance).dp.sp,
            max = 11F.scaledSize(instance).sp.clamp(max = 11F.scaledSize(instance).dp)
        )
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EtaText(ambientMode: Boolean, lines: ETAQueryResult?, seq: Int, instance: AppActiveContext) {
    val textSize = 16F.scaledSize(instance).sp.clamp(max = 16F.scaledSize(instance).dp)
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = textSize.dp + 7.dp)
            .padding((if (seq == 1) 5 else 20).dp, 0.dp)
    ) {
        AnnotatedText(
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE),
            textAlign = TextAlign.Center,
            fontSize = textSize,
            color = MaterialTheme.colors.primary.adjustBrightness(if (lines == null || (ambientMode && seq > 1)) 0.7F else 1F),
            maxLines = 1,
            text = (lines?.getLine(seq)?.text?: if (seq == 1) (if (Shared.language == "en") "Updating" else "更新中") else "").toSpanned(instance, textSize.value).asAnnotatedString()
        )
    }
}