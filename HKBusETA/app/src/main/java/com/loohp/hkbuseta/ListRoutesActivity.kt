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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Registry.ETAQueryResult
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.JsonUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.UnitUtils
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class RecentSortMode(val enabled: Boolean) {

    DISABLED(false), CHOICE(true), FORCED(true)

}

class ListRoutesActivity : ComponentActivity() {

    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 4)
    private val etaUpdatesMap: MutableMap<Int, Pair<ScheduledFuture<*>?, () -> Unit>> = LinkedHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)

        val result = JsonUtils.toList(JSONArray(intent.extras!!.getString("result")!!), JSONObject::class.java).onEach {
            if (!it.has("route")) {
                it.put("route", Registry.getInstance(this).findRouteByKey(it.getString("routeKey"), null))
            }
        }
        val showEta = intent.extras!!.getBoolean("showEta", false)
        val recentSort = RecentSortMode.values()[intent.extras!!.getInt("recentSort", RecentSortMode.DISABLED.ordinal)]

        setContent {
            MainElement(this, result, showEta, recentSort) { isAdd, index, task ->
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
fun MainElement(instance: ListRoutesActivity, result: List<JSONObject>, showEta: Boolean, recentSort: RecentSortMode, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = remember { FocusRequester() }
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val padding by remember { derivedStateOf { StringUtils.scaledSize(7.5F, instance) } }
        val etaTextWidth by remember { derivedStateOf { if (showEta) StringUtils.findTextLengthDp(instance, "99", clampSp(instance, StringUtils.scaledSize(16F, instance), dpMax = 19F)) + 1F else 0F } }

        val defaultTextWidth by remember { derivedStateOf { StringUtils.findTextLengthDp(instance, "N373", clampSp(instance, StringUtils.scaledSize(20F, instance), dpMax = StringUtils.scaledSize(23F, instance))) + 1F } }
        val mtrTextWidth by remember { derivedStateOf { StringUtils.findTextLengthDp(instance, "機場快綫", clampSp(instance, StringUtils.scaledSize(16F, instance), dpMax = StringUtils.scaledSize(19F, instance))) + 1F } }

        val bottomOffset by remember { derivedStateOf { -UnitUtils.spToDp(instance, clampSp(instance, StringUtils.scaledSize(7F, instance), dpMax = StringUtils.scaledSize(7F, instance))) / 2.7F } }
        val mtrBottomOffset by remember { derivedStateOf { -UnitUtils.spToDp(instance, clampSp(instance, StringUtils.scaledSize(7F, instance), dpMax = StringUtils.scaledSize(7F, instance))) / 10.7F } }

        val etaUpdateTimes: MutableMap<Int, Long> = remember { ConcurrentHashMap() }
        val etaResults: MutableMap<Int, ETAQueryResult> = remember { ConcurrentHashMap() }

        var sortedResult: List<JSONObject> by remember { mutableStateOf(emptyList()) }

        var recentSortEnabled by remember { mutableStateOf(recentSort == RecentSortMode.FORCED) }
        val sortedResults by remember { derivedStateOf { if (recentSortEnabled) sortedResult else result } }

        RestartEffect {
            if (recentSort.enabled) {
                val newSorted = result.sortedBy {
                    val co = it.optString("co")
                    val meta = when (co) {
                        "gmb" -> it.optJSONObject("route")!!.optString("gtfsId")
                        "nlb" -> it.optJSONObject("route")!!.optString("nlbId")
                        else -> ""
                    }
                    Shared.getFavoriteAndLookupRouteIndex(it.optJSONObject("route")!!.optString("route"), co, meta)
                }
                if (newSorted != sortedResult) {
                    sortedResult = newSorted
                    if (recentSortEnabled) {
                        scope.launch { scroll.scrollToItem(0) }
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
                .rotaryScroll(scroll, focusRequester),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = scroll
        ) {
            item {
                when (recentSort) {
                    RecentSortMode.CHOICE -> {
                        Button(
                            onClick = {
                                recentSortEnabled = !recentSortEnabled
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
                                    fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp).clamp(max = 14.dp),
                                    text = if (recentSortEnabled) {
                                        if (Shared.language == "en") "Sort: Fav/Recent" else "排序: 喜歡/最近瀏覽"
                                    } else {
                                        if (Shared.language == "en") "Sort: Normal" else "排序: 正常"
                                    }
                                )
                            }
                        )
                    }
                    RecentSortMode.FORCED -> {
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
                    }
                    else -> {
                        Spacer(modifier = Modifier.size(StringUtils.scaledSize(35, instance).dp))
                    }
                }
            }
            itemsIndexed(
                items = sortedResults,
                key = { _, route -> route.optString("routeKey") }
            ) { index, route ->
                val co = route.optString("co")
                val kmbCtbJoint = route.optJSONObject("route")!!.optBoolean("kmbCtbJoint", false)
                val routeNumber = if (co == "mtr" && Shared.language != "en") {
                    Shared.getMtrLineName(route.optJSONObject("route")!!.optString("route"))
                } else {
                    route.optJSONObject("route")!!.optString("route")
                }
                val routeTextWidth = if (Shared.language != "en" && co == "mtr") mtrTextWidth else defaultTextWidth
                val rawColor = when (co) {
                    "kmb" -> if (Shared.isLWBRoute(routeNumber)) Color(0xFFF26C33) else Color(0xFFFF4747)
                    "ctb" -> Color(0xFFFFE15E)
                    "nlb" -> Color(0xFF9BFFC6)
                    "mtr-bus" -> Color(0xFFAAD4FF)
                    "gmb" -> Color(0xFF36FF42)
                    "lightRail" -> Color(0xFFD3A809)
                    "mtr" -> {
                        when (route.optJSONObject("route")!!.optString("route")) {
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
                            else -> Color.White
                        }
                    }
                    else -> Color.White
                }
                var dest = route.optJSONObject("route")!!.optJSONObject("dest")!!.optString(Shared.language)
                dest = (if (Shared.language == "en") "To " else "往").plus(dest)
                val optOrig = if (co == "nlb") {
                    if (Shared.language == "en") {
                        "From ".plus(route.optJSONObject("route")!!.optJSONObject("orig")!!.optString("en"))
                    } else {
                        "從".plus(route.optJSONObject("route")!!.optJSONObject("orig")!!.optString("zh")).plus("開出")
                    }
                } else {
                    null
                }

                Box (
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .animateItemPlacement()
                        .combinedClickable(
                            onClick = {
                                val meta = when (co) {
                                    "gmb" -> route.optJSONObject("route")!!.optString("gtfsId")
                                    "nlb" -> route.optJSONObject("route")!!.optString("nlbId")
                                    else -> ""
                                }
                                Registry.getInstance(instance).addLastLookupRoute(route.optJSONObject("route")!!.optString("route"), co, meta, instance)
                                val intent = Intent(instance, ListStopsActivity::class.java)
                                intent.putExtra("route", route.toString())
                                instance.startActivity(intent)
                            },
                            onLongClick = {
                                instance.runOnUiThread {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val text = routeNumber.plus(" ").plus(dest).plus("\n(").plus(
                                            if (Shared.language == "en") {
                                                when (route.optString("co")) {
                                                    "kmb" -> if (Shared.isLWBRoute(routeNumber)) (if (kmbCtbJoint) "LWB/CTB" else "LWB") else (if (kmbCtbJoint) "KMB/CTB" else "KMB")
                                                    "ctb" -> "CTB"
                                                    "nlb" -> "NLB"
                                                    "mtr-bus" -> "MTR-Bus"
                                                    "gmb" -> "GMB"
                                                    "lightRail" -> "LRT"
                                                    "mtr" -> "MTR"
                                                    else -> "???"
                                                }
                                            } else {
                                                when (route.optString("co")) {
                                                    "kmb" -> if (Shared.isLWBRoute(routeNumber)) (if (kmbCtbJoint) "龍運/城巴" else "龍運") else (if (kmbCtbJoint) "九巴/城巴" else "九巴")
                                                    "ctb" -> "城巴"
                                                    "nlb" -> "嶼巴"
                                                    "mtr-bus" -> "港鐵巴士"
                                                    "gmb" -> "專線小巴"
                                                    "lightRail" -> "輕鐵"
                                                    "mtr" -> "港鐵"
                                                    else -> "???"
                                                }
                                            }
                                        ).plus(")")
                                    Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                ) {
                    RouteRow(index, kmbCtbJoint, rawColor, padding, routeTextWidth, co, routeNumber, bottomOffset, mtrBottomOffset, dest, optOrig, showEta, route, etaTextWidth, etaResults, etaUpdateTimes, instance, schedule)
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
fun RouteRow(index: Int, kmbCtbJoint: Boolean, rawColor: Color, padding: Float, routeTextWidth: Float, co: String, routeNumber: String, bottomOffset: Float, mtrBottomOffset: Float, dest: String, optOrig: String?, showEta: Boolean, route: JSONObject, etaTextWidth: Float, etaResults: MutableMap<Int, ETAQueryResult>, etaUpdateTimes: MutableMap<Int, Long>, instance: ListRoutesActivity, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
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
                .requiredWidth(routeTextWidth.dp),
            textAlign = TextAlign.Start,
            fontSize = if (co == "mtr" && Shared.language != "en") {
                TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(19F, instance).dp)
            } else {
                TextUnit(StringUtils.scaledSize(20F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(23F, instance).dp)
            },
            color = color,
            maxLines = 1,
            text = routeNumber
        )
        if (optOrig == null) {
            Text(
                modifier = Modifier
                    .padding(0.dp, padding.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE)
                    .offset(0.dp, if (co == "mtr" && Shared.language != "en") mtrBottomOffset.dp else bottomOffset.dp)
                    .weight(1F),
                textAlign = TextAlign.Start,
                fontSize = if (co == "mtr" && Shared.language != "en") {
                    TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(17F, instance).dp)
                } else {
                    TextUnit(StringUtils.scaledSize(15F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(18F, instance).dp)
                },
                color = color,
                maxLines = 1,
                text = dest
            )
        } else {
            val extraHeightUsed = if (co == "mtr" && Shared.language != "en") {
                clampSp(instance, StringUtils.scaledSize(7F, instance), dpMax = 10F)
            } else {
                clampSp(instance, StringUtils.scaledSize(8F, instance), dpMax = 11F)
            }
            Column (
                modifier = Modifier
                    .padding(0.dp, (padding - extraHeightUsed.equivalentDp.value).coerceAtLeast(0F).dp)
                    .offset(0.dp, (if (co == "mtr" && Shared.language != "en") mtrBottomOffset else bottomOffset).dp)
                    .weight(1F),
            ) {
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    textAlign = TextAlign.Start,
                    fontSize = if (co == "mtr" && Shared.language != "en") {
                        TextUnit(StringUtils.scaledSize(13F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(16F, instance).dp)
                    } else {
                        TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(17F, instance).dp)
                    },
                    color = color,
                    maxLines = 1,
                    text = dest
                )
                Text(
                    modifier = Modifier
                        .basicMarquee(iterations = Int.MAX_VALUE),
                    textAlign = TextAlign.Start,
                    fontSize = if (co == "mtr" && Shared.language != "en") {
                        TextUnit(StringUtils.scaledSize(7F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(10F, instance).dp)
                    } else {
                        TextUnit(StringUtils.scaledSize(8F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(11F, instance).dp)
                    },
                    color = color.adjustBrightness(0.75F),
                    maxLines = 1,
                    text = optOrig
                )
            }
        }

        if (showEta) {
            Box (
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                ETAElement(index, route, etaTextWidth, etaResults, etaUpdateTimes, instance, schedule)
            }
        }
    }
}

@Composable
fun ETAElement(index: Int, route: JSONObject, etaTextWidth: Float, etaResults: MutableMap<Int, ETAQueryResult>, etaUpdateTimes: MutableMap<Int, Long>, instance: ListRoutesActivity, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    var eta: ETAQueryResult? by remember { mutableStateOf(etaResults[index]) }

    LaunchedEffect (Unit) {
        if (eta != null && !eta!!.isConnectionError) {
            delay(etaUpdateTimes[index]?.let { (30000 - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        schedule.invoke(true, index) {
            eta = Registry.getEta(route.optJSONObject("stop")!!.optString("stopId"), route.optString("co"), route.optJSONObject("route")!!, instance)
            etaUpdateTimes[index] = System.currentTimeMillis()
            etaResults[index] = eta!!
        }
    }
    DisposableEffect (Unit) {
        onDispose {
            schedule.invoke(false, index, null)
        }
    }

    Column (
        modifier = Modifier
            .requiredWidth(etaTextWidth.dp)
            .offset(0.dp, -TextUnit(2F, TextUnitType.Sp).clamp(max = 4.dp).dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        if (eta != null && !eta!!.isConnectionError) {
            if (eta!!.nextScheduledBus < 0 || eta!!.nextScheduledBus > 60) {
                if (eta!!.isMtrEndOfLine) {
                    Icon(
                        modifier = Modifier
                            .size(
                                TextUnit(
                                    StringUtils.scaledSize(16F, instance),
                                    TextUnitType.Sp
                                ).clamp(max = StringUtils.scaledSize(18F, instance).dp).dp
                            )
                            .offset(
                                0.dp,
                                TextUnit(
                                    StringUtils.scaledSize(3F, instance),
                                    TextUnitType.Sp
                                ).clamp(max = StringUtils.scaledSize(4F, instance).dp).dp
                            ),
                        painter = painterResource(R.drawable.baseline_line_end_circle_24),
                        contentDescription = if (Shared.language == "en") "End of Line" else "終點站",
                        tint = Color(0xFF798996),
                    )
                } else if (eta!!.isTyphoonSchedule) {
                    Image(
                        modifier = Modifier
                            .size(
                                TextUnit(
                                    StringUtils.scaledSize(16F, instance),
                                    TextUnitType.Sp
                                ).clamp(max = StringUtils.scaledSize(18F, instance).dp).dp
                            )
                            .offset(
                                0.dp,
                                TextUnit(
                                    StringUtils.scaledSize(3F, instance),
                                    TextUnitType.Sp
                                ).clamp(max = StringUtils.scaledSize(4F, instance).dp).dp
                            ),
                        painter = painterResource(R.mipmap.cyclone),
                        contentDescription = if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次"
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(
                                TextUnit(
                                    StringUtils.scaledSize(16F, instance),
                                    TextUnitType.Sp
                                ).clamp(max = StringUtils.scaledSize(18F, instance).dp).dp
                            )
                            .offset(
                                0.dp,
                                TextUnit(
                                    StringUtils.scaledSize(3F, instance),
                                    TextUnitType.Sp
                                ).clamp(max = StringUtils.scaledSize(4F, instance).dp).dp
                            ),
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
                    fontSize = TextUnit(14F, TextUnitType.Sp).clamp(max = 15.dp),
                    color = Color(0xFFAAC3D5),
                    lineHeight = TextUnit(7F, TextUnitType.Sp).clamp(max = 9.dp),
                    maxLines = 2,
                    text = SpannableString(TextUtils.concat(span1, "\n", span2)).asAnnotatedString()
                )
            }
        }
    }
}