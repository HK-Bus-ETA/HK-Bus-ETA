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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteSortMode
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.objects.uniqueKey
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Registry.ETAQueryResult
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.formatDecimalSeparator
import com.loohp.hkbuseta.compose.FullPageScrollBarConfig
import com.loohp.hkbuseta.compose.HapticsController
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.shared.AndroidShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ImmutableState
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.asImmutableState
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.findTextLengthDp
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.px
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.spToDp
import com.loohp.hkbuseta.utils.spToPixels
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.math.roundToInt


@OptIn(ExperimentalFoundationApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun ListRouteMainElement(ambientMode: Boolean, instance: AppActiveContext, result: ImmutableList<StopIndexedRouteSearchResultEntry>, listType: RouteListType, showEta: Boolean, recentSort: RecentSortMode, proximitySortOrigin: Coordinates?, schedule: (Boolean, String, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val hapticsController = remember { HapticsController() }
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val padding by remember { derivedStateOf { 7.5F.scaledSize(instance) } }
        val etaTextWidth by remember { derivedStateOf { if (showEta) "99".findTextLengthDp(instance, 16F.scaledSize(instance).clampSp(instance, dpMax = 19F)) + 1F else 0F } }

        val defaultTextWidth by remember { derivedStateOf { "N373".findTextLengthDp(instance, 20F.scaledSize(instance).clampSp(instance, dpMax = 23F.scaledSize(instance))) + 1F } }
        val mtrTextWidth by remember { derivedStateOf { "機場快綫".findTextLengthDp(instance, 16F.scaledSize(instance).clampSp(instance, dpMax = 19F.scaledSize(instance))) + 1F } }

        val bottomOffset by remember { derivedStateOf { -7F.scaledSize(instance).clampSp(instance, dpMax = 7F.scaledSize(instance)).spToDp(instance) / 2.7F } }
        val mtrBottomOffset by remember { derivedStateOf { -7F.scaledSize(instance).clampSp(instance, dpMax = 7F.scaledSize(instance)).spToDp(instance) / 10.7F } }

        val etaUpdateTimes = remember { ConcurrentHashMap<String, Long>().asImmutableState() }
        val etaResults = remember { ConcurrentHashMap<String, ETAQueryResult>().asImmutableState() }

        var activeSortMode by remember { mutableStateOf(if (recentSort.forcedMode) {
            recentSort.defaultSortMode
        } else {
            Shared.routeSortModePreference[listType]?.let { if (it.isLegalMode(
                    recentSort == RecentSortMode.CHOICE,
                    proximitySortOrigin != null
            )) it else null }?: RouteSortMode.NORMAL
        }) }
        val sortTask = remember { {
            buildMap {
                this[RouteSortMode.NORMAL] = result
                if (recentSort.enabled) {
                    this[RouteSortMode.RECENT] = result.sortedBy {
                        val co = it.co
                        val meta = when (co) {
                            Operator.GMB -> it.route!!.gmbRegion!!.name
                            Operator.NLB -> it.route!!.nlbId
                            else -> ""
                        }
                        Shared.getFavoriteAndLookupRouteIndex(it.route!!.routeNumber, co, meta)
                    }
                }
                if (proximitySortOrigin != null) {
                    if (recentSort.enabled) {
                        this[RouteSortMode.PROXIMITY] = result.sortedWith(compareBy({
                            val location = it.stopInfo!!.data!!.location
                            proximitySortOrigin.distance(location)
                        }, {
                            val co = it.co
                            val meta = when (co) {
                                Operator.GMB -> it.route!!.gmbRegion!!.name
                                Operator.NLB -> it.route!!.nlbId
                                else -> ""
                            }
                            Shared.getFavoriteAndLookupRouteIndex(it.route!!.routeNumber, co, meta)
                        }))
                    } else {
                        this[RouteSortMode.PROXIMITY] = result.sortedBy {
                            val location = it.stopInfo!!.data!!.location
                            proximitySortOrigin.distance(location)
                        }
                    }
                }
            }.toImmutableMap()
        } }
        @SuppressLint("MutableCollectionMutableState")
        var sortedByMode by remember { mutableStateOf(sortTask.invoke()) }
        val sortedResults by remember { derivedStateOf { sortedByMode[activeSortMode]?: result } }

        RestartEffect {
            val newSorted = sortTask.invoke()
            if (newSorted != sortedByMode) {
                sortedByMode = newSorted
                hapticsController.enabled = false
                hapticsController.invokedCallback = {
                    it.enabled = true
                    it.invokedCallback = {}
                }
                scope.launch {
                    scroll.scrollToItem(0)
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
                    .rotaryScroll(scroll, focusRequester, hapticsController, ambientMode = ambientMode)
                    .composed {
                        LaunchedEffect (activeSortMode) {
                            Shared.routeSortModePreference[listType].let {
                                if (activeSortMode != it) {
                                    Registry.getInstance(instance).setRouteSortModePreference(instance, listType, activeSortMode)
                                }
                            }
                        }
                        this
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                state = scroll
            ) {
                item {
                    if (ambientMode) {
                        Spacer(modifier = Modifier.size(35.scaledSize(instance).dp))
                    } else if (recentSort == RecentSortMode.FORCED) {
                        Button(
                            onClick = {
                                Registry.getInstance(instance).clearLastLookupRoutes(instance)
                                instance.finish()
                            },
                            modifier = Modifier
                                .padding(20.dp, 15.dp, 20.dp, 0.dp)
                                .width(35.scaledSize(instance).dp)
                                .height(35.scaledSize(instance).dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = Color(0xFFFF0000)
                            ),
                            content = {
                                Icon(
                                    modifier = Modifier.size(17F.scaledSize(instance).sp.clamp(max = 17.dp).dp),
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = if (Shared.language == "en") "Clear" else "清除",
                                    tint = Color(0xFFFF0000),
                                )
                            }
                        )
                    } else if (recentSort == RecentSortMode.CHOICE || proximitySortOrigin != null) {
                        Button(
                            onClick = {
                                activeSortMode = activeSortMode.nextMode(
                                    recentSort == RecentSortMode.CHOICE,
                                    proximitySortOrigin != null
                                )
                            },
                            modifier = Modifier
                                .padding(20.dp, 15.dp, 20.dp, 0.dp)
                                .fillMaxWidth(0.8F)
                                .height(35.scaledSize(instance).dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = Color(0xFFFFFFFF)
                            ),
                            content = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(0.9F),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colors.primary,
                                    fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                                    text = when (activeSortMode) {
                                        RouteSortMode.PROXIMITY -> if (Shared.language == "en") "Sort: Proximity" else "排序: 巴士站距離"
                                        RouteSortMode.RECENT -> if (Shared.language == "en") "Sort: Fav/Recent" else "排序: 喜歡/最近瀏覽"
                                        else -> if (Shared.language == "en") "Sort: Normal" else "排序: 正常"
                                    }
                                )
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(35.scaledSize(instance).dp))
                    }
                }
                items(
                    items = sortedResults,
                    key = { route -> route.uniqueKey }
                ) { route ->
                    val co = route.co
                    val kmbCtbJoint = route.route!!.isKmbCtbJoint
                    val routeNumber = if (co == Operator.MTR && Shared.language != "en") {
                        Shared.getMtrLineName(route.route!!.routeNumber)
                    } else {
                        route.route!!.routeNumber
                    }
                    val routeTextWidth = if (Shared.language != "en" && co == Operator.MTR) mtrTextWidth else defaultTextWidth
                    val rawColor = co.getColor(route.route!!.routeNumber, Color.White)
                    val dest = route.route!!.resolvedDest(true)[Shared.language]

                    val secondLine: MutableList<AnnotatedString> = ArrayList()
                    if (route.stopInfo != null) {
                        val stop = route.stopInfo!!.data
                        secondLine.add((if (Shared.language == "en") stop!!.name.en else stop!!.name.zh).asAnnotatedString())
                    }
                    if (co == Operator.NLB) {
                        secondLine.add((if (Shared.language == "en") "From ".plus(route.route!!.orig.en) else "從".plus(route.route!!.orig.zh).plus("開出")).asAnnotatedString(SpanStyle(color = rawColor.adjustBrightness(0.75F))))
                    } else if (co == Operator.KMB && routeNumber.getKMBSubsidiary() == KMBSubsidiary.SUNB) {
                        secondLine.add((if (Shared.language == "en") "Sun Bus (NR$routeNumber)" else "陽光巴士 (NR$routeNumber)").asAnnotatedString(SpanStyle(color = rawColor.adjustBrightness(0.75F))))
                    }

                    Box (
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .animateItemPlacement()
                            .combinedClickable(
                                onClick = {
                                    val meta = when (co) {
                                        Operator.GMB -> route.route!!.gmbRegion!!.name
                                        Operator.NLB -> route.route!!.nlbId
                                        else -> ""
                                    }
                                    Registry.getInstance(instance).addLastLookupRoute(route.route!!.routeNumber, co, meta, instance)
                                    val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                                    intent.putExtra("route", route.toByteArray())
                                    instance.startActivity(intent)
                                },
                                onLongClick = {
                                    instance.runOnUiThread {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        var text = routeNumber.plus(" ").plus(dest).plus("\n(").plus(route.co.getDisplayName(routeNumber, kmbCtbJoint, Shared.language)).plus(")")
                                        if (proximitySortOrigin != null && route.stopInfo != null) {
                                            val location = route.stopInfo!!.data!!.location
                                            val distance =proximitySortOrigin.distance(location)
                                            text = text.plus(" - ").plus((distance * 1000).roundToInt().formatDecimalSeparator()).plus(if (Shared.language == "en") "m" else "米")
                                        }
                                        instance.showToastText(text, ToastDuration.LONG)
                                    }
                                }
                            )
                    ) {
                        RouteRow(route.uniqueKey, kmbCtbJoint, rawColor, padding, routeTextWidth, co, routeNumber, bottomOffset, mtrBottomOffset, dest, secondLine.toImmutableList(), showEta, route, etaTextWidth, etaResults, etaUpdateTimes, instance, schedule)
                    }
                    Spacer(
                        modifier = Modifier
                            .padding(25.dp, 0.dp)
                            .animateItemPlacement()
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF333333).adjustBrightness(if (ambientMode) 0.5F else 1F))
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
                    AndroidShared.MainTime()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RouteRow(key: String, kmbCtbJoint: Boolean, rawColor: Color, padding: Float, routeTextWidth: Float, co: Operator, routeNumber: String, bottomOffset: Float, mtrBottomOffset: Float, dest: String, secondLine: ImmutableList<AnnotatedString>, showEta: Boolean, route: StopIndexedRouteSearchResultEntry, etaTextWidth: Float, etaResults: ImmutableState<out MutableMap<String, ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>, instance: AppActiveContext, schedule: (Boolean, String, (() -> Unit)?) -> Unit) {
    Row (
        modifier = Modifier
            .padding(25.dp, 0.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        val color = if (kmbCtbJoint) {
            val infiniteTransition = rememberInfiniteTransition(label = "JointColor")
            val animatedColor by infiniteTransition.animateColor(
                initialValue = rawColor,
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
            rawColor
        }

        Text(
            modifier = Modifier
                .padding(0.dp, padding.dp)
                .requiredWidth(routeTextWidth.dp)
                .let { if (secondLine.isEmpty()) it.alignByBaseline() else it },
            textAlign = TextAlign.Start,
            fontSize = if (co == Operator.MTR && Shared.language != "en") {
                16F.scaledSize(instance).sp.clamp(max = 19F.scaledSize(instance).dp)
            } else {
                20F.scaledSize(instance).sp.clamp(max = 23F.scaledSize(instance).dp)
            },
            color = color,
            maxLines = 1,
            text = routeNumber
        )
        if (secondLine.isEmpty()) {
            val fontSize = if (co == Operator.MTR && Shared.language != "en") {
                14F.scaledSize(instance).sp.clamp(max = 17F.scaledSize(instance).dp)
            } else {
                15F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp)
            }
            Text(
                modifier = Modifier
                    .padding(0.dp, padding.dp)
                    .weight(1F)
                    .basicMarquee(iterations = Int.MAX_VALUE)
                    .alignByBaseline(),
                textAlign = TextAlign.Start,
                fontSize = fontSize,
                style = LocalTextStyle.current.let { if (co != Operator.MTR && Shared.language != "en") it.copy(baselineShift = BaselineShift(3F / fontSize.px)) else it },
                color = color,
                maxLines = 1,
                text = dest
            )
        } else {
            val extraHeightPadding = (padding - if (co == Operator.MTR && Shared.language != "en") {
                4.5F.scaledSize(instance).clampSp(instance, dpMax = 6F)
            } else {
                5F.scaledSize(instance).clampSp(instance, dpMax = 6.5F)
            }.spToDp(instance)).coerceAtLeast(0F)
            Column (
                modifier = Modifier
                    .padding(0.dp, extraHeightPadding.dp)
                    .weight(1F),
            ) {
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    textAlign = TextAlign.Start,
                    fontSize = if (co == Operator.MTR && Shared.language != "en") {
                        14F.scaledSize(instance).sp.clamp(max = 17F.scaledSize(instance).dp)
                    } else {
                        15F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp)
                    },
                    color = color,
                    maxLines = 1,
                    text = dest
                )
                val infiniteTransition = rememberInfiniteTransition(label = "SecondLineCrossFade")
                val animatedCurrentLineFloat by infiniteTransition.animateFloat(
                    initialValue = -0.5F,
                    targetValue = secondLine.size - 0.5001F,
                    animationSpec = infiniteRepeatable(
                        animation = tween(5500 * secondLine.size, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "SecondLineCrossFade"
                )
                val animatedCurrentLine by remember { derivedStateOf { animatedCurrentLineFloat.roundToInt() } }
                Crossfade(
                    targetState = animatedCurrentLine,
                    animationSpec = tween(durationMillis = 500, easing = LinearEasing),
                    label = "SecondLineCrossFade"
                ) {
                    Text(
                        modifier = Modifier
                            .basicMarquee(iterations = Int.MAX_VALUE),
                        textAlign = TextAlign.Start,
                        fontSize = if (co == Operator.MTR && Shared.language != "en") {
                            9F.scaledSize(instance).sp.clamp(max = 12F.scaledSize(instance).dp)
                        } else {
                            10F.scaledSize(instance).sp.clamp(max = 13F.scaledSize(instance).dp)
                        },
                        color = Color(0xFFFFFFFF).adjustBrightness(0.75F),
                        maxLines = 1,
                        text = secondLine[it]
                    )
                }
            }
        }

        if (showEta) {
            Box(
                modifier = Modifier
                    .padding(0.dp, 0.dp, 0.dp, padding.dp)
                    .offset(0.dp, if (co == Operator.MTR && Shared.language != "en") mtrBottomOffset.dp else bottomOffset.dp)
            ) {
                ETAElement(key, route, etaTextWidth, etaResults, etaUpdateTimes, instance, schedule)
            }
        }
    }
}

@Composable
fun ETAElement(key: String, route: StopIndexedRouteSearchResultEntry, etaTextWidth: Float, etaResults: ImmutableState<out MutableMap<String, ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>, instance: AppActiveContext, schedule: (Boolean, String, (() -> Unit)?) -> Unit) {
    val etaStateFlow = remember { MutableStateFlow(etaResults.value[key]) }

    LaunchedEffect (Unit) {
        val eta = etaStateFlow.value
        if (eta != null && !eta.isConnectionError) {
            delay(etaUpdateTimes.value[key]?.let { (Shared.ETA_UPDATE_INTERVAL - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        schedule.invoke(true, key) {
            val result = Registry.getInstance(instance).getEta(route.stopInfo!!.stopId, route.stopInfoIndex, route.co, route.route!!, instance).get(
                Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
            etaStateFlow.value = result
            etaUpdateTimes.value[key] = System.currentTimeMillis()
            etaResults.value[key] = result
        }
    }
    DisposableEffect (Unit) {
        onDispose {
            schedule.invoke(false, key, null)
        }
    }

    val etaState by etaStateFlow.collectAsStateWithLifecycle()

    Column (
        modifier = Modifier.requiredWidth(etaTextWidth.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        val eta = etaState
        if (eta != null && !eta.isConnectionError) {
            if (eta.nextScheduledBus !in 0..59) {
                if (eta.isMtrEndOfLine) {
                    Icon(
                        modifier = Modifier
                            .size(16F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp).dp)
                            .offset(0.dp, -2.5F.scaledSize(instance).sp.clamp(max = 3.5F.scaledSize(instance).dp).dp),
                        painter = painterResource(R.drawable.baseline_line_end_circle_24),
                        contentDescription = if (Shared.language == "en") "End of Line" else "終點站",
                        tint = Color(0xFF798996),
                    )
                } else if (eta.isTyphoonSchedule) {
                    val typhoonInfo by remember { Registry.getInstance(instance).cachedTyphoonDataState }.collectAsStateWithLifecycle()
                    Image(
                        modifier = Modifier
                            .size(16F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp).dp)
                            .offset(0.dp, -2.5F.scaledSize(instance).sp.clamp(max = 3.5F.scaledSize(instance).dp).dp),
                        painter = painterResource(R.mipmap.cyclone),
                        contentDescription = typhoonInfo.typhoonWarningTitle
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(16F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp).dp)
                            .offset(0.dp, -2.5F.scaledSize(instance).sp.clamp(max = 3.5F.scaledSize(instance).dp).dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(0.dp, 1F.scaledSize(instance).sp.clamp(max = 2F.scaledSize(instance).dp).dp),
                    textAlign = TextAlign.End,
                    fontSize = 14F.sp.clamp(max = 15.dp),
                    color = Color(0xFFAAC3D5),
                    lineHeight = 7F.sp.clamp(max = 9.dp),
                    maxLines = 2,
                    text = text
                )
            }
        }
    }
}