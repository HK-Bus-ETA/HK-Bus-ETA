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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.resolveStop
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.shared.TileUseState
import com.loohp.hkbuseta.shared.Tiles
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ImmutableState
import com.loohp.hkbuseta.utils.LocationResult
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.asImmutableState
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.spToPixels
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt


@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun FavElements(scrollToIndex: Int, instance: AppActiveContext, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val scope = rememberCoroutineScope()
        val state = rememberLazyListState()

        val maxFavItems by Shared.getCurrentMaxFavouriteRouteStopState().collectAsStateWithLifecycle()
        val showRouteListViewButton by remember { derivedStateOf { (Shared.favoriteRouteStops.keys.maxOrNull()?: 0) > 2 } }

        val etaUpdateTimes = remember { ConcurrentHashMap<Int, Long>().asImmutableState() }
        val etaResults = remember { ConcurrentHashMap<Int, Registry.ETAQueryResult>().asImmutableState() }

        val originState: MutableStateFlow<LocationResult?> = remember { MutableStateFlow(null) }
        val origin by originState.collectAsStateWithLifecycle()

        LaunchedEffect (Unit) {
            if (scrollToIndex > 0) {
                scope.launch {
                    state.scrollToItem(scrollToIndex.coerceIn(1, maxFavItems.coerceAtLeast(1)) + 2, (-instance.screenHeight / 2) + 35F.scaledSize(instance).spToPixels(instance).roundToInt())
                }
            }
            if (Shared.favoriteRouteStops.values.any { it.favouriteStopMode.isRequiresLocation }) {
                checkLocationPermission(instance) {
                    if (it) {
                        schedule.invoke(true, -1) {
                            originState.value = runBlocking { getGPSLocation(instance).await() }
                        }
                    }
                }
            }
        }

        DisposableEffect (Unit) {
            onDispose {
                schedule.invoke(false, -1, null)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalLazyScrollbar(
                    state = state,
                    context = instance
                )
                .rotaryScroll(state, focusRequester),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = state
        ) {
            item {
                Spacer(modifier = Modifier.size(20.scaledSize(instance).dp))
            }
            item {
                FavTitle(instance)
                Spacer(modifier = Modifier.size(5.scaledSize(instance).dp))
                FavDescription(instance)
            }
            if (showRouteListViewButton) {
                item {
                    Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                    RouteListViewButton(instance)
                }
            } else {
                item {
                    Spacer(modifier = Modifier.size(0.dp))
                }
            }
            items(maxFavItems) {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                FavButton(it + 1, etaResults, etaUpdateTimes, origin, instance, schedule)
            }
            item {
                Spacer(modifier = Modifier.size(45.scaledSize(instance).dp))
            }
        }
    }
}

@Composable
fun FavTitle(instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 17F.scaledSize(instance).sp.clamp(max = 17.dp),
        text = if (Shared.language == "en") "Favourite Routes" else "最喜愛路線"
    )
}

@Composable
fun FavDescription(instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 11F.scaledSize(instance).sp.clamp(max = 11.dp),
        text = if (Shared.language == "en") "Routes can be displayed in Tiles" else "路線可在資訊方塊中顯示"
    )
}

@Composable
fun RouteListViewButton(instance: AppActiveContext) {
    Button(
        onClick = {
            checkLocationPermission(instance) {
                val intent = AppIntent(instance, AppScreen.FAV_ROUTE_LIST_VIEW)
                intent.putExtra("usingGps", it)
                instance.startActivity(intent)
            }
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
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
                text = if (Shared.language == "en") "Route List View" else "路線一覽列表"
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavButton(favoriteIndex: Int, etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>, origin: LocationResult?, instance: AppActiveContext, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    var favouriteStopRoute by remember { mutableStateOf(Shared.favoriteRouteStops[favoriteIndex]) }
    var anyTileUses by remember { mutableStateOf(Tiles.getTileUseState(favoriteIndex)) }

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val deleteAnimatable = remember { Animatable(0F) }
    val deleteTimer by remember { deleteAnimatable.asState() }
    val deleteState by remember { derivedStateOf { deleteTimer > 0.001F } }

    val backgroundColor by remember { derivedStateOf { if (deleteState) Color(0xFF633A3A) else Color(0xFF1A1A1A) } }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = TweenSpec(durationMillis = 250, easing = LinearEasing),
        label = ""
    )

    RestartEffect {
        val newState = Shared.favoriteRouteStops[favoriteIndex]
        if (newState != favouriteStopRoute) {
            favouriteStopRoute = newState
        }
        val newAnyTileUses = Tiles.getTileUseState(favoriteIndex)
        if (newAnyTileUses != anyTileUses) {
            anyTileUses = newAnyTileUses
        }
    }

    val shape = RoundedCornerShape(15.dp)
    AdvanceButton(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                TweenSpec(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            .padding(20.dp, 0.dp)
            .composed {
                when (anyTileUses) {
                    TileUseState.PRIMARY -> this.border(2.sp.dp, Color(0x5437FF00), shape)
                    TileUseState.SECONDARY -> this.border(2.sp.dp, Color(0x54FFB700), shape)
                    TileUseState.NONE -> this
                }
            },
        onClick = {
            if (deleteState) {
                if (Registry.getInstance(instance).hasFavouriteRouteStop(favoriteIndex)) {
                    Registry.getInstance(instance).clearFavouriteRouteStop(favoriteIndex, instance)
                    instance.showToastText(if (Shared.language == "en") "Cleared Favourite Route ".plus(favoriteIndex) else "已清除最喜愛路線".plus(favoriteIndex), ToastDuration.SHORT)
                }
                favouriteStopRoute = Shared.favoriteRouteStops[favoriteIndex]
                anyTileUses = Tiles.getTileUseState(favoriteIndex)
                scope.launch { deleteAnimatable.snapTo(0F) }
            } else {
                val favStopRoute = Shared.favoriteRouteStops[favoriteIndex]
                if (favStopRoute != null) {
                    val (index, stopId, stop, route) = favStopRoute.resolveStop(instance) { origin?.location }
                    val co = favStopRoute.co

                    Registry.getInstance(instance).findRoutes(route.routeNumber, true) { it ->
                        val bound = it.bound
                        if (!bound.containsKey(co) || bound[co] != route.bound[co]) {
                            return@findRoutes false
                        }
                        val stops = it.stops[co]?: return@findRoutes false
                        return@findRoutes stops.contains(stopId)
                    }.firstOrNull()?.let {
                        val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                        intent.putExtra("route", it.toByteArray())
                        intent.putExtra("scrollToStop", stopId)
                        instance.startActivity(intent)
                    }

                    val intent = AppIntent(instance, AppScreen.ETA)
                    intent.putExtra("stopId", stopId)
                    intent.putExtra("co", co.name)
                    intent.putExtra("index", index)
                    intent.putExtra("stop", stop.toByteArray())
                    intent.putExtra("route", route.toByteArray())
                    instance.startActivity(intent)
                }
            }
        },
        onLongClick = {
            if (!deleteState) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val text = if (Shared.language == "en") "Click again to confirm delete" else "再次點擊確認刪除"
                instance.runOnUiThread {
                    instance.showToastText(text, ToastDuration.LONG)
                }
                scope.launch {
                    deleteAnimatable.snapTo(1F)
                    deleteAnimatable.animateTo(0F, tween(durationMillis = 5000, easing = LinearEasing))
                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = animatedBackgroundColor,
            contentColor = if (deleteState) Color(0xFFFF0000) else if (favouriteStopRoute != null) Color(0xFFFFFF00) else Color(0xFF444444),
        ),
        shape = shape,
        enabled = favouriteStopRoute != null,
        content = {
            val favStopRoute = Shared.favoriteRouteStops[favoriteIndex]
            if (favStopRoute != null) {
                Box(
                    modifier = Modifier
                        .padding(10.dp, (if (Shared.language == "en") 7.5 else 8.5).dp)
                        .align(Alignment.BottomStart),
                ) {
                    val (index, stopId, _, route) = favStopRoute.resolveStop(instance) { origin?.location }
                    ETAElement(favoriteIndex, stopId, index, favStopRoute.co, route, etaResults, etaUpdateTimes, instance, schedule)
                }
            }
            Row(
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(35.scaledSize(instance).sp.clamp(max = 35.dp).dp)
                        .height(35.scaledSize(instance).sp.clamp(max = 35.dp).dp)
                        .clip(CircleShape)
                        .background(if (favouriteStopRoute != null) Color(0xFF3D3D3D) else Color(0xFF131313))
                        .drawWithContent {
                            if (deleteState) {
                                drawArc(
                                    startAngle = -90F,
                                    sweepAngle = deleteTimer * 360F,
                                    useCenter = false,
                                    color = Color(0xFFFF0000),
                                    topLeft = Offset.Zero + Offset(1.sp.toPx(), 1.sp.toPx()),
                                    size = Size(size.width - 2.sp.toPx(), size.height - 2.sp.toPx()),
                                    alpha = 1F,
                                    style = Stroke(width = 2.sp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            drawContent()
                        }
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    if (deleteState) {
                        Icon(
                            modifier = Modifier.size(21.scaledSize(instance).dp),
                            imageVector = Icons.Filled.Clear,
                            tint = Color(0xFFFF0000),
                            contentDescription = if (Shared.language == "en") "Clear Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "清除資訊方塊最喜愛路線預計到達時間".plus(favoriteIndex)
                        )
                    } else {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 17F.scaledSize(instance).sp,
                            color = if (favouriteStopRoute != null) Color(0xFFFFFF00) else Color(0xFF444444),
                            text = favoriteIndex.toString()
                        )
                    }
                }
                val currentFavouriteStopRoute = Shared.favoriteRouteStops[favoriteIndex]
                if (currentFavouriteStopRoute == null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color(0xFF505050),
                        fontSize = 16F.scaledSize(instance).sp,
                        text = if (Shared.language == "en") "No Route Selected" else "未有設置路線"
                    )
                } else {
                    val (index, _, stop) = currentFavouriteStopRoute.resolveStop(instance) { origin?.location }
                    val stopName = stop.name
                    val route = currentFavouriteStopRoute.route
                    val kmbCtbJoint = route.isKmbCtbJoint
                    val co = currentFavouriteStopRoute.co
                    val routeNumber = route.routeNumber
                    val stopId = currentFavouriteStopRoute.stopId
                    val gpsStop = currentFavouriteStopRoute.favouriteStopMode.isRequiresLocation
                    val destName = Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route, true)
                    val rawColor = co.getColor(routeNumber, Color.White)
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

                    val operator = co.getDisplayName(routeNumber, kmbCtbJoint, Shared.language)
                    val mainText = operator.plus(" ").plus(co.getDisplayRouteNumber(routeNumber))
                    val routeText = destName[Shared.language]
                    val subText = buildAnnotatedString {
                        append(if (Shared.language == "en") {
                            (if (co.isTrain) "" else index.toString().plus(". ")).plus(stopName.en)
                        } else {
                            (if (co.isTrain) "" else index.toString().plus(". ")).plus(stopName.zh)
                        })
                        if (gpsStop) {
                            append(if (Shared.language == "en") " - Closest" else " - 最近", SpanStyle(color = Color(0xFFFFE496), fontSize = TextUnit.Small))
                        }
                    }

                    Spacer(modifier = Modifier.size(5.dp))
                    Column (
                        modifier = Modifier
                            .heightIn(
                                min = 60.scaledSize(instance).sp.dp
                            ),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(Int.MAX_VALUE),
                            textAlign = TextAlign.Start,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16F.scaledSize(instance).sp,
                            maxLines = 1,
                            text = mainText
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(Int.MAX_VALUE),
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14F.scaledSize(instance).sp,
                            maxLines = 1,
                            text = routeText
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(Int.MAX_VALUE)
                                .padding(0.dp, 0.dp, 0.dp, 3.dp),
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colors.primary,
                            fontSize = 11F.scaledSize(instance).sp,
                            maxLines = 1,
                            text = subText
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ETAElement(favoriteIndex: Int, stopId: String, stopIndex: Int, co: Operator, route: Route, etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>, instance: AppActiveContext, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    val etaStateFlow = remember { MutableStateFlow(etaResults.value[favoriteIndex]) }

    LaunchedEffect (Unit) {
        val eta = etaStateFlow.value
        if (eta != null && !eta.isConnectionError) {
            delay(etaUpdateTimes.value[favoriteIndex]?.let { (Shared.ETA_UPDATE_INTERVAL - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        schedule.invoke(true, favoriteIndex) {
            val result = Registry.getInstance(instance).getEta(stopId, stopIndex, co, route, instance).get(
                Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
            etaStateFlow.value = result
            etaUpdateTimes.value[favoriteIndex] = System.currentTimeMillis()
            etaResults.value[favoriteIndex] = result
        }
    }
    LaunchedEffect (stopId) {
        if (Shared.favoriteRouteStops[favoriteIndex]?.favouriteStopMode?.isRequiresLocation == true) {
            schedule.invoke(true, favoriteIndex) {
                val result = Registry.getInstance(instance).getEta(stopId, stopIndex, co, route, instance).get(
                    Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
                etaStateFlow.value = result
                etaUpdateTimes.value[favoriteIndex] = System.currentTimeMillis()
                etaResults.value[favoriteIndex] = result
            }
        }
    }
    DisposableEffect (Unit) {
        onDispose {
            schedule.invoke(false, favoriteIndex, null)
        }
    }

    val etaState by etaStateFlow.collectAsStateWithLifecycle()

    Box {
        val eta = etaState
        if (eta != null && !eta.isConnectionError) {
            if (eta.nextScheduledBus !in 0..59) {
                if (eta.isMtrEndOfLine) {
                    Icon(
                        modifier = Modifier
                            .size(16F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp).dp),
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
                    append(text2, SpanStyle(fontSize = 7F.scaledSize(instance).clampSp(instance, dpMax = 8F.scaledSize(instance)).sp))
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    fontSize = 14F.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFAAC3D5),
                    lineHeight = 7F.sp,
                    maxLines = 1,
                    text = text
                )
            }
        }
    }
}