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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.RouteSortMode
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.asStop
import com.loohp.hkbuseta.common.objects.bilingualToPrefix
import com.loohp.hkbuseta.common.objects.bySortModes
import com.loohp.hkbuseta.common.objects.firstCo
import com.loohp.hkbuseta.common.objects.getDisplayFormattedName
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.getListDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.identifyStopCo
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.objects.shouldPrependTo
import com.loohp.hkbuseta.common.objects.uniqueKey
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Registry.ETAQueryResult
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Shared.getResolvedText
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.common.utils.toLocalDateTime
import com.loohp.hkbuseta.compose.DrawPhaseColorText
import com.loohp.hkbuseta.compose.FullPageScrollBarConfig
import com.loohp.hkbuseta.compose.HapticsController
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.compose.userMarqueeMaxLines
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.findTextLengthDp
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.px
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.spToPixels
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set


@OptIn(ExperimentalFoundationApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun ListRouteMainElement(ambientMode: Boolean, instance: AppActiveContext, result: ImmutableList<StopIndexedRouteSearchResultEntry>, listType: RouteListType, showEta: Boolean, recentSort: RecentSortMode, proximitySortOrigin: Coordinates?, mtrSearch: String?, schedule: (Boolean, String, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val hapticsController = remember { HapticsController() }
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()

        val etaTextWidth by remember { derivedStateOf { if (showEta) "99".findTextLengthDp(instance, 16F.scaledSize(instance).clampSp(instance, dpMax = 19F)) + 1F else 0F } }

        val defaultTextWidth by remember { derivedStateOf { "N373".findTextLengthDp(instance, 20F.scaledSize(instance).clampSp(instance, dpMax = 23F.scaledSize(instance))) + 1F } }
        val mtrTextWidth by remember { derivedStateOf { "機場快綫".findTextLengthDp(instance, 16F.scaledSize(instance).clampSp(instance, dpMax = 19F.scaledSize(instance))) + 1F } }

        val etaUpdateTimes = remember { ConcurrentHashMap<String, Long>().asImmutableState() }
        val etaResults = remember { ConcurrentHashMap<String, ETAQueryResult>().asImmutableState() }

        val lastLookupRoutes by if (listType == RouteListType.RECENT) Shared.lastLookupRoutes.collectAsStateWithLifecycle() else remember { mutableStateOf("") }

        var activeSortMode by remember { mutableStateOf(if (recentSort.forcedMode) {
            recentSort.defaultSortMode
        } else {
            Shared.routeSortModePreference[listType]?.let { if (it.isLegalMode(
                    recentSort == RecentSortMode.CHOICE,
                    proximitySortOrigin != null
            )) it else null }?: RouteSortMode.NORMAL
        }) }
        val sortTask = remember { { result.bySortModes(instance, recentSort, listType != RouteListType.RECENT, proximitySortOrigin).toImmutableMap() } }
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
        LaunchedEffect (lastLookupRoutes) {
            val newSorted = sortTask.invoke()
            if (newSorted != sortedByMode) {
                sortedByMode = newSorted
                if (scroll.firstVisibleItemIndex in 0..2) {
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
                                .padding(20.dp, 25.dp, 20.dp, 0.dp)
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
                                .padding(20.dp, 25.dp, 20.dp, 0.dp)
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
                                    text = activeSortMode.sortPrefixedTitle[Shared.language]
                                )
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(35.scaledSize(instance).dp))
                    }
                }
                if (mtrSearch != null) {
                    if (mtrSearch.isEmpty()) {
                        item {
                            Button(
                                onClick = {
                                    val intent = AppIntent(instance, AppScreen.SEARCH_TRAIN)
                                    intent.putExtra("type", "MTR")
                                    instance.startActivity(intent)
                                },
                                modifier = Modifier
                                    .padding(20.dp, 10.dp, 20.dp, 5.dp)
                                    .fillMaxWidth(0.8F)
                                    .height(35.scaledSize(instance).dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF001F50),
                                    contentColor = Color(0xFFFFFFFF)
                                ),
                                content = {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(0.9F),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colors.primary,
                                        fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                                        text = if (Shared.language == "en") "MTR System Map" else "港鐵路綫圖"
                                    )
                                }
                            )
                            Button(
                                onClick = {
                                    val intent = AppIntent(instance, AppScreen.SEARCH_TRAIN)
                                    intent.putExtra("type", "LRT")
                                    instance.startActivity(intent)
                                },
                                modifier = Modifier
                                    .padding(20.dp, 5.dp, 20.dp, 10.dp)
                                    .fillMaxWidth(0.8F)
                                    .height(35.scaledSize(instance).dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Operator.LRT.getOperatorColor(Color.White).adjustBrightness(0.7F),
                                    contentColor = Color(0xFFFFFFFF)
                                ),
                                content = {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(0.9F),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colors.primary,
                                        fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                                        text = if (Shared.language == "en") "LRT Route Map" else "輕鐵路綫圖"
                                    )
                                }
                            )
                        }
                    } else {
                        mtrSearch.asStop(instance)?.apply {
                            item {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 10.dp)
                                        .fillMaxWidth()
                                        .background(if (mtrSearch.identifyStopCo().firstCo() == Operator.LRT) Operator.LRT.getOperatorColor(Color.White) else Color(0xFF001F50))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colors.primary,
                                        fontSize = 17F.scaledSize(instance).sp.clamp(max = 17.dp),
                                        text = remarkedName[Shared.language].asContentAnnotatedString().annotatedString
                                    )
                                }
                            }
                        }
                    }
                }
                items(items = sortedResults, key = { route -> route.uniqueKey }) { route ->
                    RouteRow(
                        key = route.uniqueKey,
                        listType = listType,
                        showEta = showEta,
                        defaultTextWidth = defaultTextWidth,
                        mtrTextWidth = mtrTextWidth,
                        route = route,
                        mtrSearch = mtrSearch,
                        etaTextWidth = etaTextWidth,
                        etaResults = etaResults,
                        etaUpdateTimes = etaUpdateTimes,
                        instance = instance,
                        schedule = schedule
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(25.dp, 0.dp)
                            .animateItemPlacement(),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.RouteRow(
    key: String,
    listType: RouteListType,
    showEta: Boolean,
    defaultTextWidth: Float,
    mtrTextWidth: Float,
    route: StopIndexedRouteSearchResultEntry,
    etaTextWidth: Float,
    mtrSearch: String?,
    etaResults: ImmutableState<out MutableMap<String, ETAQueryResult>>,
    etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>,
    instance: AppActiveContext,
    schedule: (Boolean, String, (() -> Unit)?) -> Unit
) {
    val co = route.co
    val kmbCtbJoint = route.route!!.isKmbCtbJoint
    val routeNumber = co.getListDisplayRouteNumber(route.route!!.routeNumber, true)
    val routeTextWidth = if (Shared.language != "en" && co == Operator.MTR) mtrTextWidth else defaultTextWidth
    val rawColor = co.getColor(route.route!!.routeNumber, Color.White)
    val dest = route.route!!.resolvedDest(false)[Shared.language]
    val operatorName = remember(route, co, kmbCtbJoint) { co.getDisplayFormattedName(route.route!!.routeNumber, kmbCtbJoint, Shared.language).asContentAnnotatedString().annotatedString }

    val secondLine = remember(route, co, kmbCtbJoint, routeNumber, rawColor, listType) { buildList {
        if (listType == RouteListType.RECENT) {
            add(instance.formatDateTime((Shared.findLookupRouteTime(route.routeKey)?: 0).toLocalDateTime(), true).asAnnotatedString())
        }
        if (route.stopInfo != null && mtrSearch.isNullOrEmpty()) {
            val stop = route.stopInfo!!.data!!
            add(stop.name[Shared.language].asAnnotatedString())
        }
        if (co == Operator.NLB || co.isFerry) {
            add((if (Shared.language == "en") "From ".plus(route.route!!.orig.en) else "從".plus(route.route!!.orig.zh).plus("開出")).asAnnotatedString(SpanStyle(color = rawColor.adjustBrightness(0.75F))))
        } else if (co == Operator.KMB && routeNumber.getKMBSubsidiary() == KMBSubsidiary.SUNB) {
            add((if (Shared.language == "en") "Sun Bus (NR$routeNumber)" else "陽光巴士 (NR$routeNumber)").asAnnotatedString(SpanStyle(color = rawColor.adjustBrightness(0.75F))))
        }
    }.toImmutableList() }

    Box (
        modifier = Modifier
            .fillParentMaxWidth()
            .heightIn(min = 43F.scaledSize(instance).sp.dp)
            .animateItemPlacement()
            .clickable {
                Registry.getInstance(instance).addLastLookupRoute(route.routeKey, instance)
                if (mtrSearch.isNullOrEmpty()) {
                    val intent = AppIntent(instance, AppScreen.LIST_STOPS)
                    intent.putExtra("route", route)
                    instance.startActivity(intent)
                } else {
                    val stops = Registry.getInstance(instance).getAllStops(route.route!!.routeNumber, route.route!!.idBound(co), co, null)
                    val i = stops.indexOfFirst { it.stopId == mtrSearch }
                    val stopData = stops[i]
                    val intent = AppIntent(instance, AppScreen.ETA)
                    intent.putExtra("stopId", stopData.stopId)
                    intent.putExtra("co", co.name)
                    intent.putExtra("index", i + 1)
                    intent.putExtra("stop", stopData.stop)
                    intent.putExtra("route", stopData.route)
                    instance.startActivity(intent)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row (
            modifier = Modifier
                .padding(25.dp, 0.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RouteRowText(
                rawColor = rawColor,
                kmbCtbJoint = kmbCtbJoint,
                routeTextWidth = routeTextWidth,
                routeNumber = routeNumber,
                operatorName = operatorName,
                secondLine = secondLine,
                dest = dest,
                co = co,
                route = route,
                instance = instance
            )
            if (showEta) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .requiredWidthIn(min = etaTextWidth.dp)
                ) {
                    ETAElement(key, route, etaResults, etaUpdateTimes, instance, schedule)
                }
            }
        }
    }
}

@Composable
fun RowScope.RouteRowText(
    rawColor: Color,
    kmbCtbJoint: Boolean,
    routeTextWidth: Float,
    routeNumber: String,
    operatorName: AnnotatedString,
    secondLine: ImmutableList<AnnotatedString>,
    dest: String,
    co: Operator,
    route: StopIndexedRouteSearchResultEntry,
    instance: AppActiveContext
) {
    val color by WearOSShared.rememberOperatorColor(rawColor, Operator.CTB.getOperatorColor(Color.White).takeIf { kmbCtbJoint })
    Column {
        DrawPhaseColorText(
            modifier = Modifier.requiredWidth(routeTextWidth.dp),
            textAlign = TextAlign.Start,
            fontSize = if (co == Operator.MTR && Shared.language != "en") {
                16F.scaledSize(instance).sp.clamp(max = 19F.scaledSize(instance).dp)
            } else {
                20F.scaledSize(instance).sp.clamp(max = 23F.scaledSize(instance).dp)
            },
            color = { color },
            maxLines = 1,
            text = routeNumber
        )
        DrawPhaseColorText(
            textAlign = TextAlign.Start,
            fontSize = 8F.scaledSize(instance).sp.clamp(max = 11F.scaledSize(instance).dp),
            color = { color },
            maxLines = 1,
            text = operatorName
        )
    }
    if (secondLine.isEmpty()) {
        val fontSize = if (co == Operator.MTR && Shared.language != "en") {
            14F.scaledSize(instance).sp.clamp(max = 17F.scaledSize(instance).dp)
        } else {
            15F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp)
        }
        Row(
            modifier = Modifier.weight(1F),
            horizontalArrangement = Arrangement.Start
        ) {
            DrawPhaseColorText(
                modifier = Modifier.alignByBaseline(),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                color = { color },
                fontWeight = FontWeight.Normal,
                fontSize = 12F.scaledSize(instance).sp.clamp(max = 15F.scaledSize(instance).dp),
                text = bilingualToPrefix[Shared.language]
            )
            DrawPhaseColorText(
                modifier = Modifier
                    .weight(1F)
                    .userMarquee()
                    .alignByBaseline(),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                fontWeight = if (Shared.disableBoldDest) FontWeight.Normal else FontWeight.Bold,
                fontSize = fontSize,
                style = LocalTextStyle.current.let { if (co != Operator.MTR && Shared.language != "en") it.copy(baselineShift = BaselineShift(3F / fontSize.px)) else it },
                color = { color },
                maxLines = userMarqueeMaxLines(),
                text = dest
            )
        }
    } else {
        WithSecondLine(
            color = color,
            secondLine = secondLine,
            dest = dest,
            co = co,
            route = route,
            instance = instance
        )
    }
}

@Composable
fun RowScope.WithSecondLine(
    color: Color,
    secondLine: ImmutableList<AnnotatedString>,
    dest: String,
    co: Operator,
    route: StopIndexedRouteSearchResultEntry,
    instance: AppActiveContext
) {
    val density = LocalDensity.current
    var maxHeight by remember(density.density, density.fontScale, secondLine) { mutableIntStateOf(0) }
    Column (
        modifier = Modifier
            .weight(1F)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val height = placeable.height.coerceAtLeast(maxHeight)
                maxHeight = height
                layout(placeable.width, height) {
                    placeable.placeRelative(0, 0)
                }
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.Start
        ) {
            if (route.route!!.shouldPrependTo()) {
                DrawPhaseColorText(
                    modifier = Modifier.alignByBaseline(),
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    color = { color },
                    fontWeight = FontWeight.Normal,
                    fontSize = 12F.scaledSize(instance).sp.clamp(max = 15F.scaledSize(instance).dp),
                    text = bilingualToPrefix[Shared.language]
                )
            }
            DrawPhaseColorText(
                modifier = Modifier
                    .weight(1F)
                    .userMarquee()
                    .alignByBaseline(),
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (Shared.disableBoldDest) FontWeight.Normal else FontWeight.Bold,
                textAlign = TextAlign.Start,
                fontSize = if (co == Operator.MTR && Shared.language != "en") {
                    14F.scaledSize(instance).sp.clamp(max = 17F.scaledSize(instance).dp)
                } else {
                    15F.scaledSize(instance).sp.clamp(max = 18F.scaledSize(instance).dp)
                },
                color = { color },
                maxLines = userMarqueeMaxLines(),
                text = dest
            )
        }
        CrossfadeSecondLine(
            secondLine = secondLine,
            co = co,
            instance = instance
        )
    }
}

@Composable
fun CrossfadeSecondLine(
    secondLine: ImmutableList<AnnotatedString>,
    co: Operator,
    instance: AppActiveContext
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SecondLineCrossFade")
    val animatedCurrentLine by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = secondLine.size,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(5500 * secondLine.size, easing = LinearEasing),
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
        Text(
            modifier = Modifier.userMarquee(),
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            fontSize = if (co == Operator.MTR && Shared.language != "en") {
                9F.scaledSize(instance).sp.clamp(max = 12F.scaledSize(instance).dp)
            } else {
                10F.scaledSize(instance).sp.clamp(max = 13F.scaledSize(instance).dp)
            },
            color = Color(0xFFFFFFFF).adjustBrightness(0.75F),
            maxLines = userMarqueeMaxLines(),
            text = secondLine[it.coerceIn(secondLine.indices)]
        )
    }
}

@Composable
fun ETAElement(key: String, route: StopIndexedRouteSearchResultEntry, etaResults: ImmutableState<out MutableMap<String, ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>, instance: AppActiveContext, schedule: (Boolean, String, (() -> Unit)?) -> Unit) {
    val etaStateFlow = remember { MutableStateFlow(etaResults.value[key]) }

    LaunchedEffect (Unit) {
        val eta = etaStateFlow.value
        if (eta != null && !eta.isConnectionError) {
            delay(etaUpdateTimes.value[key]?.let { (Shared.ETA_UPDATE_INTERVAL - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        schedule.invoke(true, key) {
            val result = runBlocking(dispatcherIO) { Registry.getInstance(instance).getEta(route.stopInfo!!.stopId, route.stopInfoIndex, route.co, route.route!!, instance).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND) }
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