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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.google.common.collect.ImmutableMap
import com.loohp.hkbuseta.compose.HapticsController
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.objects.Coordinates
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.Route
import com.loohp.hkbuseta.objects.RouteListType
import com.loohp.hkbuseta.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.objects.RouteSearchResultEntry.StopInfo
import com.loohp.hkbuseta.objects.RouteSortMode
import com.loohp.hkbuseta.objects.component1
import com.loohp.hkbuseta.objects.component2
import com.loohp.hkbuseta.objects.firstLine
import com.loohp.hkbuseta.objects.getColor
import com.loohp.hkbuseta.objects.getDisplayName
import com.loohp.hkbuseta.objects.resolvedDest
import com.loohp.hkbuseta.objects.toCoordinates
import com.loohp.hkbuseta.objects.uniqueKey
import com.loohp.hkbuseta.shared.KMBSubsidiary
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Registry.ETAQueryResult
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.DistanceUtils
import com.loohp.hkbuseta.utils.ImmutableState
import com.loohp.hkbuseta.utils.JsonUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.UnitUtils
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.asImmutableState
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.formatDecimalSeparator
import com.loohp.hkbuseta.utils.ifFalse
import com.loohp.hkbuseta.utils.px
import com.loohp.hkbuseta.utils.set
import com.loohp.hkbuseta.utils.toHexString
import com.loohp.hkbuseta.utils.toSpanned
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.set
import kotlin.math.roundToInt

enum class RecentSortMode(val enabled: Boolean, val defaultSortMode: RouteSortMode = RouteSortMode.NORMAL, val forcedMode: Boolean = false) {

    DISABLED(false), CHOICE(true), FORCED(true, RouteSortMode.RECENT, true);

}

@Stable
class StopIndexedRouteSearchResultEntry(
    routeKey: String?,
    route: Route?,
    co: Operator?,
    stopInfo: StopInfo?,
    var stopInfoIndex: Int,
    origin: Coordinates?,
    isInterchangeSearch: Boolean
) : RouteSearchResultEntry(routeKey, route, co, stopInfo, origin, isInterchangeSearch) {

    companion object {

        fun deserialize(json: JSONObject): StopIndexedRouteSearchResultEntry {
            val routeKey = json.optString("routeKey");
            val route = if (json.has("route")) Route.deserialize(json.optJSONObject("route")) else null
            val co = Operator.valueOf(json.optString("co"));
            val stop = if (json.has("stop")) StopInfo.deserialize(json.optJSONObject("stop")) else null
            val origin = if (json.has("origin")) Coordinates.deserialize(json.optJSONObject("origin")) else null
            val isInterchangeSearch = json.optBoolean("isInterchangeSearch")
            return StopIndexedRouteSearchResultEntry(routeKey, route, co, stop, 0, origin, isInterchangeSearch)
        }

    }

}

@Stable
class ListRoutesActivity : ComponentActivity() {

    private val sync: ExecutorService = Executors.newSingleThreadExecutor()
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(16)
    private val etaUpdatesMap: MutableMap<String, Pair<ScheduledFuture<*>?, () -> Unit>> = LinkedHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(this).ifFalse { return }
        Shared.setDefaultExceptionHandler(this)

        val result = JsonUtils.mapToList(JSONArray(intent.extras!!.getString("result")!!)) { StopIndexedRouteSearchResultEntry.deserialize(it as JSONObject) }.also { list ->
            list.removeIf {
                if (it.route == null) {
                    val route = Registry.getInstance(this).findRouteByKey(it.routeKey, null)
                    if (route == null) {
                        return@removeIf true
                    } else {
                        it.route = route
                    }
                }
                if (it.stopInfo != null && it.stopInfo.data == null) {
                    val stop = Registry.getInstance(this).getStopById(it.stopInfo.stopId)
                    if (stop == null) {
                        return@removeIf true
                    } else {
                        it.stopInfo.data = stop
                    }
                }
                return@removeIf false
            }
        }.onEach {
            val route: Route? = it.route
            val co: Operator? = it.co
            val stopInfo: StopInfo? = it.stopInfo
            if (route != null && co != null && stopInfo != null) {
                it.stopInfoIndex = Registry.getInstance(this).getAllStops(route.routeNumber, route.bound[co], co, route.gmbRegion).indexOfFirst { i -> i.stopId == stopInfo.stopId }
            }
        }.toImmutableList()
        val listType = intent.extras!!.getString("listType")?.let { RouteListType.valueOf(it) }?: RouteListType.NORMAL
        val showEta = intent.extras!!.getBoolean("showEta", false)
        val recentSort = RecentSortMode.values()[intent.extras!!.getInt("recentSort", RecentSortMode.DISABLED.ordinal)]
        val proximitySortOrigin = intent.extras!!.getDoubleArray("proximitySortOrigin")?.toCoordinates()

        setContent {
            MainElement(this, result, listType, showEta, recentSort, proximitySortOrigin) { isAdd, key, task ->
                sync.execute {
                    if (isAdd) {
                        etaUpdatesMap.computeIfAbsent(key) { executor.scheduleWithFixedDelay(task, 0, Shared.ETA_UPDATE_INTERVAL, TimeUnit.MILLISECONDS) to task!! }
                    } else {
                        etaUpdatesMap.remove(key)?.first?.cancel(true)
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
        sync.execute {
            etaUpdatesMap.replaceAll { _, value ->
                value.first?.cancel(true)
                executor.scheduleWithFixedDelay(value.second, 0, Shared.ETA_UPDATE_INTERVAL, TimeUnit.MILLISECONDS) to value.second
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sync.execute {
            etaUpdatesMap.forEach { it.value.first?.cancel(true) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            executor.shutdownNow()
            sync.shutdownNow()
            Shared.removeSelfFromCurrentActivity(this)
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainElement(instance: ListRoutesActivity, result: ImmutableList<StopIndexedRouteSearchResultEntry>, listType: RouteListType, showEta: Boolean, recentSort: RecentSortMode, proximitySortOrigin: Coordinates?, schedule: (Boolean, String, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = remember { FocusRequester() }
        val hapticsController = remember { HapticsController() }
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val padding by remember { derivedStateOf { StringUtils.scaledSize(7.5F, instance) } }
        val etaTextWidth by remember { derivedStateOf { if (showEta) StringUtils.findTextLengthDp(instance, "99", clampSp(instance, StringUtils.scaledSize(16F, instance), dpMax = 19F)) + 1F else 0F } }

        val defaultTextWidth by remember { derivedStateOf { StringUtils.findTextLengthDp(instance, "N373", clampSp(instance, StringUtils.scaledSize(20F, instance), dpMax = StringUtils.scaledSize(23F, instance))) + 1F } }
        val mtrTextWidth by remember { derivedStateOf { StringUtils.findTextLengthDp(instance, "機場快綫", clampSp(instance, StringUtils.scaledSize(16F, instance), dpMax = StringUtils.scaledSize(19F, instance))) + 1F } }

        val bottomOffset by remember { derivedStateOf { -UnitUtils.spToDp(instance, clampSp(instance, StringUtils.scaledSize(7F, instance), dpMax = StringUtils.scaledSize(7F, instance))) / 2.7F } }
        val mtrBottomOffset by remember { derivedStateOf { -UnitUtils.spToDp(instance, clampSp(instance, StringUtils.scaledSize(7F, instance), dpMax = StringUtils.scaledSize(7F, instance))) / 10.7F } }

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
            val map: ImmutableMap.Builder<RouteSortMode, List<StopIndexedRouteSearchResultEntry>> = ImmutableMap.builder()
            map[RouteSortMode.NORMAL] = result
            if (recentSort.enabled) {
                map[RouteSortMode.RECENT] = result.sortedBy {
                    val co = it.co
                    val meta = when (co) {
                        Operator.GMB -> it.route.gmbRegion.name
                        Operator.NLB -> it.route.nlbId
                        else -> ""
                    }
                    Shared.getFavoriteAndLookupRouteIndex(it.route.routeNumber, co, meta)
                }
            }
            if (proximitySortOrigin != null) {
                if (recentSort.enabled) {
                    map[RouteSortMode.PROXIMITY] = result.sortedWith(compareBy({
                        val location = it.stopInfo.data.location
                        DistanceUtils.findDistance(proximitySortOrigin.lat, proximitySortOrigin.lng, location.lat, location.lng)
                    }, {
                        val co = it.co
                        val meta = when (co) {
                            Operator.GMB -> it.route.gmbRegion.name
                            Operator.NLB -> it.route.nlbId
                            else -> ""
                        }
                        Shared.getFavoriteAndLookupRouteIndex(it.route.routeNumber, co, meta)
                    }))
                } else {
                    map[RouteSortMode.PROXIMITY] = result.sortedBy {
                        val location = it.stopInfo.data.location
                        DistanceUtils.findDistance(proximitySortOrigin.lat, proximitySortOrigin.lng, location.lat, location.lng)
                    }
                }
            }
            map.build()
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

        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalLazyScrollbar(
                    state = scroll
                )
                .rotaryScroll(scroll, focusRequester, hapticsController)
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
                if (recentSort == RecentSortMode.FORCED) {
                    Button(
                        onClick = {
                            Registry.getInstance(instance).clearLastLookupRoutes(instance)
                            instance.finish()
                        },
                        modifier = Modifier
                            .padding(20.dp, 15.dp, 20.dp, 0.dp)
                            .width(StringUtils.scaledSize(35, instance).dp)
                            .height(StringUtils.scaledSize(35, instance).dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = Color(0xFFFF0000)
                        ),
                        content = {
                            Icon(
                                modifier = Modifier.size(StringUtils.scaledSize(17F, instance).sp.clamp(max = 17.dp).dp),
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
                            .height(StringUtils.scaledSize(35, instance).dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = Color(0xFFFFFFFF)
                        ),
                        content = {
                            Text(
                                modifier = Modifier.fillMaxWidth(0.9F),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.primary,
                                fontSize = StringUtils.scaledSize(14F, instance).sp.clamp(max = 14.dp),
                                text = when (activeSortMode) {
                                    RouteSortMode.PROXIMITY -> if (Shared.language == "en") "Sort: Proximity" else "排序: 巴士站距離"
                                    RouteSortMode.RECENT -> if (Shared.language == "en") "Sort: Fav/Recent" else "排序: 喜歡/最近瀏覽"
                                    else -> if (Shared.language == "en") "Sort: Normal" else "排序: 正常"
                                }
                            )
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(35, instance).dp))
                }
            }
            items(
                items = sortedResults,
                key = { route -> route.uniqueKey }
            ) { route ->
                val co = route.co
                val kmbCtbJoint = route.route.isKmbCtbJoint
                val routeNumber = if (co == Operator.MTR && Shared.language != "en") {
                    Shared.getMtrLineName(route.route.routeNumber)
                } else {
                    route.route.routeNumber
                }
                val routeTextWidth = if (Shared.language != "en" && co == Operator.MTR) mtrTextWidth else defaultTextWidth
                val rawColor = co.getColor(route.route.routeNumber, Color.White)
                val dest = route.route.resolvedDest(true)[Shared.language]

                val secondLine: MutableList<String> = ArrayList()
                if (route.stopInfo != null) {
                    val stop = route.stopInfo.data
                    secondLine.add(if (Shared.language == "en") stop.name.en else stop.name.zh)
                }
                if (co == Operator.NLB) {
                    secondLine.add("<span style=\"color: ${rawColor.adjustBrightness(0.75F).toHexString()}\">".plus(if (Shared.language == "en") {
                        "From ".plus(route.route.orig.en)
                    } else {
                        "從".plus(route.route.orig.zh).plus("開出")
                    }).plus("</span>"))
                } else if (co == Operator.KMB && Shared.getKMBSubsidiary(routeNumber) == KMBSubsidiary.SUNB) {
                    secondLine.add("<span style=\"color: ${rawColor.adjustBrightness(0.75F).toHexString()}\">".plus(if (Shared.language == "en") {
                        "Sun Bus (NR$routeNumber)"
                    } else {
                        "陽光巴士 (NR$routeNumber)"
                    }).plus("</span>"))
                }

                Box (
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .animateItemPlacement()
                        .combinedClickable(
                            onClick = {
                                val meta = when (co) {
                                    Operator.GMB -> route.route.gmbRegion.name
                                    Operator.NLB -> route.route.nlbId
                                    else -> ""
                                }
                                Registry.getInstance(instance).addLastLookupRoute(route.route.routeNumber, co, meta, instance)
                                val intent = Intent(instance, ListStopsActivity::class.java)
                                intent.putExtra("route", route.serialize().toString())
                                instance.startActivity(intent)
                            },
                            onLongClick = {
                                instance.runOnUiThread {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    var text = routeNumber.plus(" ").plus(dest).plus("\n(").plus(route.co.getDisplayName(routeNumber, kmbCtbJoint, Shared.language)).plus(")")
                                    if (proximitySortOrigin != null && route.stopInfo != null) {
                                        val location = route.stopInfo.data.location
                                        val distance = DistanceUtils.findDistance(proximitySortOrigin.lat, proximitySortOrigin.lng, location.lat, location.lng)
                                        text = text.plus(" - ").plus((distance * 1000).roundToInt().formatDecimalSeparator()).plus(if (Shared.language == "en") "m" else "米")
                                    }
                                    Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
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
                        .background(Color(0xFF333333))
                )
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(40, instance).dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RouteRow(key: String, kmbCtbJoint: Boolean, rawColor: Color, padding: Float, routeTextWidth: Float, co: Operator, routeNumber: String, bottomOffset: Float, mtrBottomOffset: Float, dest: String, secondLine: ImmutableList<String>, showEta: Boolean, route: StopIndexedRouteSearchResultEntry, etaTextWidth: Float, etaResults: ImmutableState<out MutableMap<String, ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>, instance: ListRoutesActivity, schedule: (Boolean, String, (() -> Unit)?) -> Unit) {
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
                StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(19F, instance).dp)
            } else {
                StringUtils.scaledSize(20F, instance).sp.clamp(max = StringUtils.scaledSize(23F, instance).dp)
            },
            color = color,
            maxLines = 1,
            text = routeNumber
        )
        if (secondLine.isEmpty()) {
            val fontSize = if (co == Operator.MTR && Shared.language != "en") {
                StringUtils.scaledSize(14F, instance).sp.clamp(max = StringUtils.scaledSize(17F, instance).dp)
            } else {
                StringUtils.scaledSize(15F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp)
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
            val extraHeightPadding = (padding - UnitUtils.spToDp(instance, if (co == Operator.MTR && Shared.language != "en") {
                clampSp(instance, StringUtils.scaledSize(4.5F, instance), dpMax = 6F)
            } else {
                clampSp(instance, StringUtils.scaledSize(5F, instance), dpMax = 6.5F)
            })).coerceAtLeast(0F)
            Column (
                modifier = Modifier
                    .padding(0.dp, extraHeightPadding.dp)
                    .weight(1F),
            ) {
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    textAlign = TextAlign.Start,
                    fontSize = if (co == Operator.MTR && Shared.language != "en") {
                        StringUtils.scaledSize(14F, instance).sp.clamp(max = StringUtils.scaledSize(17F, instance).dp)
                    } else {
                        StringUtils.scaledSize(15F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp)
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
                    AnnotatedText(
                        modifier = Modifier
                            .basicMarquee(iterations = Int.MAX_VALUE),
                        textAlign = TextAlign.Start,
                        fontSize = if (co == Operator.MTR && Shared.language != "en") {
                            StringUtils.scaledSize(9F, instance).sp.clamp(max = StringUtils.scaledSize(12F, instance).dp)
                        } else {
                            StringUtils.scaledSize(10F, instance).sp.clamp(max = StringUtils.scaledSize(13F, instance).dp)
                        },
                        color = Color(0xFFFFFFFF).adjustBrightness(0.75F),
                        maxLines = 1,
                        text = secondLine[it].toSpanned(instance).asAnnotatedString()
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
fun ETAElement(key: String, route: StopIndexedRouteSearchResultEntry, etaTextWidth: Float, etaResults: ImmutableState<out MutableMap<String, ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>, instance: ListRoutesActivity, schedule: (Boolean, String, (() -> Unit)?) -> Unit) {
    val etaStateFlow = remember { MutableStateFlow(etaResults.value[key]) }

    LaunchedEffect (Unit) {
        val eta = etaStateFlow.value
        if (eta != null && !eta.isConnectionError) {
            delay(etaUpdateTimes.value[key]?.let { (Shared.ETA_UPDATE_INTERVAL - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        schedule.invoke(true, key) {
            val result = Registry.getInstance(instance).getEta(route.stopInfo.stopId, route.stopInfoIndex, route.co, route.route, instance).get(Shared.ETA_UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
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
                            .size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp).dp)
                            .offset(0.dp, -StringUtils.scaledSize(2.5F, instance).sp.clamp(max = StringUtils.scaledSize(3.5F, instance).dp).dp),
                        painter = painterResource(R.drawable.baseline_line_end_circle_24),
                        contentDescription = if (Shared.language == "en") "End of Line" else "終點站",
                        tint = Color(0xFF798996),
                    )
                } else if (eta.isTyphoonSchedule) {
                    val typhoonInfo by remember { Registry.getInstance(instance).cachedTyphoonDataState }.collectAsStateWithLifecycle()
                    Image(
                        modifier = Modifier
                            .size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp).dp)
                            .offset(0.dp, -StringUtils.scaledSize(2.5F, instance).sp.clamp(max = StringUtils.scaledSize(3.5F, instance).dp).dp),
                        painter = painterResource(R.mipmap.cyclone),
                        contentDescription = typhoonInfo.typhoonWarningTitle
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp).dp)
                            .offset(0.dp, -StringUtils.scaledSize(2.5F, instance).sp.clamp(max = StringUtils.scaledSize(3.5F, instance).dp).dp),
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        contentDescription = if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次",
                        tint = Color(0xFF798996),
                    )
                }
            } else {
                val (text1, text2) = eta.firstLine.shortText
                val span1 = SpannableString(text1)
                val size1 = UnitUtils.spToPixels(instance, clampSp(instance, StringUtils.scaledSize(14F, instance), dpMax = StringUtils.scaledSize(15F, instance))).roundToInt()
                span1.setSpan(AbsoluteSizeSpan(size1), 0, text1.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                val span2 = SpannableString(text2)
                val size2 = UnitUtils.spToPixels(instance, clampSp(instance, StringUtils.scaledSize(7F, instance), dpMax = StringUtils.scaledSize(8F, instance))).roundToInt()
                span2.setSpan(AbsoluteSizeSpan(size2), 0, text2.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                AnnotatedText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(0.dp, StringUtils.scaledSize(1F, instance).sp.clamp(max = StringUtils.scaledSize(2F, instance).dp).dp),
                    textAlign = TextAlign.End,
                    fontSize = 14F.sp.clamp(max = 15.dp),
                    color = Color(0xFFAAC3D5),
                    lineHeight = 7F.sp.clamp(max = 9.dp),
                    maxLines = 2,
                    text = SpannableString(TextUtils.concat(span1, "\n", span2)).asAnnotatedString()
                )
            }
        }
    }
}