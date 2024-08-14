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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.FavouriteResolvedStop
import com.loohp.hkbuseta.common.objects.FavouriteRouteGroup
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.bilingualToPrefix
import com.loohp.hkbuseta.common.objects.getByName
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getFavouriteRouteStop
import com.loohp.hkbuseta.common.objects.getRouteKey
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.removeFavouriteRouteStop
import com.loohp.hkbuseta.common.objects.resolveStop
import com.loohp.hkbuseta.common.objects.shouldPrependTo
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Shared.getResolvedText
import com.loohp.hkbuseta.common.shared.TileUseState
import com.loohp.hkbuseta.common.shared.Tiles
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.LocationResult
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.common.utils.indexOf
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.DrawPhaseColorText
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.compose.userMarqueeMaxLines
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import java.util.concurrent.ConcurrentHashMap


@OptIn(ExperimentalWearFoundationApi::class, ExperimentalFoundationApi::class)
@Composable
fun FavElements(ambientMode: Boolean, instance: AppActiveContext, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val state = rememberLazyListState()

        val favouriteRouteStops by Shared.favoriteRouteStops.collectAsStateWithLifecycle()
        val selectedGroupState = remember { mutableStateOf(favouriteRouteStops.first().name) }
        val showRouteListViewButton by remember { derivedStateOf { Shared.shouldShowFavListRouteView } }
        val routeStops by remember(favouriteRouteStops, selectedGroupState) { derivedStateOf { favouriteRouteStops.getByName(selectedGroupState.value)!!.favouriteRouteStops } }

        val etaUpdateTimes = remember { ConcurrentHashMap<Int, Long>().asImmutableState() }
        val etaResults = remember { ConcurrentHashMap<Int, Registry.ETAQueryResult>().asImmutableState() }

        val originState: MutableStateFlow<LocationResult?> = remember { MutableStateFlow(null) }
        val origin by originState.collectAsStateWithLifecycle()

        LaunchedEffect (Unit) {
            if (favouriteRouteStops.any { it.favouriteRouteStops.any { s -> s.favouriteStopMode.isRequiresLocation } }) {
                checkLocationPermission(instance) {
                    if (it) {
                        schedule.invoke(true, -1) {
                            originState.value = runBlocking(WearOSShared.CACHED_DISPATCHER) { getGPSLocation(instance).await() }
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
                Spacer(modifier = Modifier.size(30.scaledSize(instance).dp))
            }
            item {
                FavTitle(ambientMode, instance)
                Spacer(modifier = Modifier.size(5.scaledSize(instance).dp))
                FavDescription(ambientMode, instance)
            }
            if (showRouteListViewButton) {
                item {
                    Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                    RouteListViewButton(ambientMode, instance)
                }
            } else {
                item {
                    Spacer(modifier = Modifier.size(0.dp))
                }
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                SwitchGroupButton(favouriteRouteStops.toImmutableList(), selectedGroupState, instance)
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
            }
            if (routeStops.isNotEmpty()) {
                itemsIndexed(routeStops, key = { _, r -> r.favouriteId }) { index, routeStop ->
                    Column (
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        FavButton(index + 1, routeStop, etaResults, etaUpdateTimes, origin, instance, schedule)
                        Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                    }
                }
            } else {
                item {
                    NoFavText(instance)
                    Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                }
            }
            item {
                Spacer(modifier = Modifier.size(45.scaledSize(instance).dp))
            }
        }
    }
}

@Composable
fun NoFavText(instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 17F.scaledSize(instance).sp.clamp(max = 17.dp),
        text = if (Shared.language == "en") "No favourite routes" else "沒有最喜愛路線"
    )
}

@Composable
fun SwitchGroupButton(groups: ImmutableList<FavouriteRouteGroup>, selectedState: MutableState<BilingualText>, instance: AppActiveContext) {
    Button(
        onClick = {
            val current = selectedState.value
            val index = groups.indexOf { it.name == current }
            selectedState.value = (if (index + 1 >= groups.size) groups.first() else groups[index + 1]).name
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(0.8F)
            .height(35.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color.White
        ),
        content = {
            Text(
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .userMarquee(),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                text = selectedState.value[Shared.language]
            )
        }
    )
}

@Composable
fun FavTitle(ambientMode: Boolean, instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary.adjustBrightness(if (ambientMode) 0.7F else 1F),
        fontSize = 17F.scaledSize(instance).sp,
        text = if (Shared.language == "en") "Favourite Routes" else "最喜愛路線"
    )
}

@Composable
fun FavDescription(ambientMode: Boolean, instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary.adjustBrightness(if (ambientMode) 0.7F else 1F),
        fontSize = 11F.scaledSize(instance).sp,
        text = if (Shared.language == "en") "Routes can be displayed in Tiles" else "路線可在資訊方塊中顯示"
    )
}

@Composable
fun RouteListViewButton(ambientMode: Boolean, instance: AppActiveContext) {
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
            contentColor = MaterialTheme.colors.primary.adjustBrightness(if (ambientMode) 0.7F else 1F),
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(0.9F),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary.adjustBrightness(if (ambientMode) 0.7F else 1F),
                fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                text = if (Shared.language == "en") "Route List View" else "路線一覽列表"
            )
        }
    )
}

@Composable
fun FavButton(numIndex: Int, favouriteRouteStop: FavouriteRouteStop, etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>, origin: LocationResult?, instance: AppActiveContext, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    val favouriteId by remember(favouriteRouteStop) { derivedStateOf { favouriteRouteStop.favouriteId } }
    var anyTileUses by remember { mutableStateOf(Tiles.getTileUseState(favouriteId)) }

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
        val newAnyTileUses = Tiles.getTileUseState(favouriteId)
        if (newAnyTileUses != anyTileUses) {
            anyTileUses = newAnyTileUses
        }
    }
    LaunchedEffect (favouriteId) {
        deleteAnimatable.snapTo(0F)
    }

    val shape = RoundedCornerShape(15.dp)
    AdvanceButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp)
            .let { when (anyTileUses) {
                TileUseState.PRIMARY -> it.border(2.sp.dp, Color(0x5437FF00), shape)
                TileUseState.SECONDARY -> it.border(2.sp.dp, Color(0x54FFB700), shape)
                TileUseState.NONE -> it
            } },
        onClick = {
            if (deleteState) {
                if (Shared.favoriteRouteStops.value.getFavouriteRouteStop(favouriteId) != null) {
                    Registry.getInstance(instance).setFavouriteRouteGroups(Shared.favoriteRouteStops.value.removeFavouriteRouteStop(favouriteId), instance)
                    instance.showToastText(if (Shared.language == "en") "Cleared Favourite Route ".plus(numIndex) else "已清除最喜愛路線".plus(numIndex), ToastDuration.SHORT)
                }
                anyTileUses = Tiles.getTileUseState(favouriteId)
                scope.launch { deleteAnimatable.snapTo(0F) }
            } else {
                val (index, stopId, stop, route) = favouriteRouteStop.resolveStop(instance) { origin?.location }
                val co = favouriteRouteStop.co

                route.getRouteKey(instance)?.let {
                    Registry.getInstance(instance).addLastLookupRoute(it, instance)
                }

                CoroutineScope(dispatcherIO).launch {
                    Registry.getInstance(instance).findRoutes(route.routeNumber, true) { it ->
                        val bound = it.bound
                        if (!bound.containsKey(co) || bound[co] != route.bound[co]) {
                            return@findRoutes false
                        }
                        val stops = it.stops[co]?: return@findRoutes false
                        return@findRoutes stops.contains(stopId)
                    }.firstOrNull()?.let {
                        val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                        intent.putExtra("route", it)
                        intent.putExtra("scrollToStop", stopId)
                        instance.startActivity(intent)
                    }

                    val intent = AppIntent(instance, AppScreen.ETA)
                    intent.putExtra("stopId", stopId)
                    intent.putExtra("co", co.name)
                    intent.putExtra("index", index)
                    intent.putExtra("stop", stop)
                    intent.putExtra("route", route)
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
            contentColor = if (deleteState) Color(0xFFFF0000) else Color(0xFFFFFF00),
        ),
        shape = shape,
        content = {
            var height by remember { mutableIntStateOf(0) }
            Row(
                modifier = Modifier
                    .padding(5.dp)
                    .heightIn(min = 70.scaledSize(instance).sp.dp)
                    .animateContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                BoxWithConstraints(
                    modifier = Modifier.heightIn(min = height.equivalentDp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 5.dp, bottom = 5.dp, start = 5.dp)
                            .heightIn(min = minHeight),
                        verticalArrangement = Arrangement.spacedBy(2.scaledSize(instance).dp, Alignment.Top),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box (
                            modifier = Modifier
                                .width(35.scaledSize(instance).sp.clamp(max = 35.dp).dp)
                                .height(35.scaledSize(instance).sp.clamp(max = 35.dp).dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3D3D3D))
                                .drawWithContent {
                                    if (deleteState) {
                                        drawArc(
                                            startAngle = -90F,
                                            sweepAngle = deleteTimer * 360F,
                                            useCenter = false,
                                            color = Color(0xFFFF0000),
                                            topLeft = Offset.Zero + Offset(1.sp.toPx(), 1.sp.toPx()),
                                            size = Size(
                                                size.width - 2.sp.toPx(),
                                                size.height - 2.sp.toPx()
                                            ),
                                            alpha = 1F,
                                            style = Stroke(width = 2.sp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    drawContent()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (deleteState) {
                                Icon(
                                    modifier = Modifier.size(21.scaledSize(instance).sp.dp),
                                    imageVector = Icons.Filled.Clear,
                                    tint = Color(0xFFFF0000),
                                    contentDescription = if (Shared.language == "en") "Clear Route Stop ETA ".plus(favouriteId).plus(" Tile") else "清除資訊方塊最喜愛路線預計到達時間".plus(favouriteId)
                                )
                            } else {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontSize = 17F.scaledSize(instance).sp,
                                    color = Color(0xFFFFFF00),
                                    text = numIndex.toString()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1F))
                        val resolvedStop = favouriteRouteStop.resolveStop(instance) { origin?.location }
                        ETAElement(favouriteRouteStop, resolvedStop, etaResults, etaUpdateTimes, instance, schedule)
                    }
                }

                val (index, _, stop) = favouriteRouteStop.resolveStop(instance) { origin?.location }
                val stopName = stop.name
                val route = favouriteRouteStop.route
                val kmbCtbJoint = route.isKmbCtbJoint
                val co = favouriteRouteStop.co
                val routeNumber = route.routeNumber
                val stopId = favouriteRouteStop.stopId
                val gpsStop = favouriteRouteStop.favouriteStopMode.isRequiresLocation
                val destName = Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route, false)
                val rawColor = co.getColor(routeNumber, Color.White)
                val color by WearOSShared.rememberOperatorColor(rawColor, Operator.CTB.getOperatorColor(Color.White).takeIf { kmbCtbJoint })

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

                Spacer(modifier = Modifier.size(10.dp))
                Column (
                    modifier = Modifier.onSizeChanged { height = it.height },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    DrawPhaseColorText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .userMarquee(),
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        color = { color },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16F.scaledSize(instance).sp,
                        maxLines = userMarqueeMaxLines(),
                        text = mainText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (route.shouldPrependTo()) {
                            Text(
                                modifier = Modifier.alignByBaseline(),
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11F.scaledSize(instance).sp,
                                text = bilingualToPrefix[Shared.language]
                            )
                        }
                        Text(
                            modifier = Modifier
                                .weight(1F)
                                .alignByBaseline()
                                .userMarquee(),
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14F.scaledSize(instance).sp,
                            maxLines = userMarqueeMaxLines(),
                            text = routeText
                        )
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .userMarquee(),
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colors.primary,
                        fontSize = 11F.scaledSize(instance).sp,
                        maxLines = userMarqueeMaxLines(),
                        text = subText
                    )
                }
            }
        }
    )
}

@Composable
fun ETAElement(favouriteRouteStop: FavouriteRouteStop, resolvedStop: FavouriteResolvedStop, etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>, instance: AppActiveContext, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    val favoriteId = favouriteRouteStop.favouriteId
    val (index, stopId, _, route) = resolvedStop
    val etaStateFlow = remember { MutableStateFlow(etaResults.value[favoriteId]) }

    LaunchedEffect (Unit) {
        val eta = etaStateFlow.value
        if (eta != null && !eta.isConnectionError) {
            delay(etaUpdateTimes.value[favoriteId]?.let { (Shared.ETA_UPDATE_INTERVAL - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        schedule.invoke(true, favoriteId) {
            val result = runBlocking(dispatcherIO) { Registry.getInstance(instance).getEta(stopId, index, favouriteRouteStop.co, route, instance).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND) }
            etaStateFlow.value = result
            etaUpdateTimes.value[favoriteId] = System.currentTimeMillis()
            etaResults.value[favoriteId] = result
        }
    }
    LaunchedEffect (resolvedStop) {
        if (favouriteRouteStop.favouriteStopMode.isRequiresLocation) {
            schedule.invoke(true, favoriteId) {
                val result = runBlocking(dispatcherIO) { Registry.getInstance(instance).getEta(stopId, index, favouriteRouteStop.co, route, instance).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND) }
                etaStateFlow.value = result
                etaUpdateTimes.value[favoriteId] = System.currentTimeMillis()
                etaResults.value[favoriteId] = result
            }
        }
    }
    DisposableEffect (Unit) {
        onDispose {
            schedule.invoke(false, favoriteId, null)
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
                    val typhoonInfo by remember { Registry.getInstance(instance).typhoonInfo }.collectAsStateWithLifecycle()
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
                val (text, lineHeight) = if (Shared.etaDisplayMode.wearableShortTextClockTime) {
                    val text1 = eta.getResolvedText(1, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                    val fontSize = 13F.scaledSize(instance).sp
                    text1.replace("\\s+".toRegex(), "\n").asAnnotatedString(SpanStyle(fontSize = fontSize)) to fontSize
                } else {
                    val (text1, text2) = eta.firstLine.shortText
                    buildAnnotatedString {
                        append(text1, SpanStyle(fontSize = 14F.scaledSize(instance).sp))
                        append(text2, SpanStyle(fontSize = 7F.scaledSize(instance).sp))
                    } to 7F.sp
                }
                Text(
                    textAlign = TextAlign.Start,
                    fontSize = 14F.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFAAC3D5),
                    lineHeight = lineHeight,
                    text = text
                )
            }
        }
    }
}