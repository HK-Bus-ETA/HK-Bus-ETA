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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.getColor
import com.loohp.hkbuseta.objects.getDisplayName
import com.loohp.hkbuseta.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.objects.name
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ImmutableState
import com.loohp.hkbuseta.utils.ScreenSizeUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.TimerUtils
import com.loohp.hkbuseta.utils.UnitUtils
import com.loohp.hkbuseta.utils.asImmutableState
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


@Stable
class FavActivity : ComponentActivity() {

    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 4)
    private val etaUpdatesMap: MutableMap<Int, Pair<ScheduledFuture<*>?, () -> Unit>> = LinkedHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)

        val scrollToIndex = intent.extras?.getInt("scrollToIndex")?: 0

        setContent {
            FavElements(scrollToIndex, this) { isAdd, index, task ->
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

@Composable
fun FavElements(scrollToIndex: Int, instance: FavActivity, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = remember { FocusRequester() }
        val scope = rememberCoroutineScope()
        val state = rememberLazyListState()

        val etaUpdateTimes = remember { ConcurrentHashMap<Int, Long>().asImmutableState() }
        val etaResults = remember { ConcurrentHashMap<Int, Registry.ETAQueryResult>().asImmutableState() }

        LaunchedEffect (Unit) {
            if (scrollToIndex in 1..8) {
                scope.launch {
                    state.scrollToItem(scrollToIndex + 1, (-ScreenSizeUtils.getScreenHeight(instance) / 2) + UnitUtils.spToPixels(instance, StringUtils.scaledSize(35F, instance)).roundToInt())
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalLazyScrollbar(
                    state = state
                )
                .rotaryScroll(state, focusRequester),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = state
        ) {
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp))
            }
            item {
                FavTitle(instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                FavDescription(instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            }
            items(8) {
                FavButton(it + 1, etaResults, etaUpdateTimes, instance, schedule)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(35, instance).dp))
            }
        }
    }
}

@Composable
fun FavTitle(instance: FavActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(17F, instance).sp.clamp(max = 17.dp),
        text = if (Shared.language == "en") "Favourite Routes" else "最喜愛路線"
    )
}

@Composable
fun FavDescription(instance: FavActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(11F, instance).sp.clamp(max = 11.dp),
        text = if (Shared.language == "en") "These routes will display in their corresponding indexed Tile" else "這些路線將顯示在其相應數字的資訊方塊中"
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavButton(favoriteIndex: Int, etaResults: ImmutableState<out MutableMap<Int, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<Int, Long>>, instance: FavActivity, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    var favouriteStopRoute by remember { mutableStateOf(Shared.favoriteRouteStops[favoriteIndex]) }
    var deleteState by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    RestartEffect {
        val newState = Shared.favoriteRouteStops[favoriteIndex]
        if (newState != favouriteStopRoute) {
            favouriteStopRoute = newState
        }
    }

    AdvanceButton(
        onClick = {
            if (deleteState) {
                if (Registry.getInstance(instance).hasFavouriteRouteStop(favoriteIndex)) {
                    Registry.getInstance(instance).clearFavouriteRouteStop(favoriteIndex, instance)
                    Toast.makeText(instance, if (Shared.language == "en") "Cleared Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "已清除資訊方塊路線巴士站預計到達時間".plus(favoriteIndex), Toast.LENGTH_SHORT).show()
                }
                val newState = Shared.favoriteRouteStops[favoriteIndex]
                if (newState != favouriteStopRoute) {
                    favouriteStopRoute = newState
                }
                deleteState = false
            } else {
                val favStopRoute = Shared.favoriteRouteStops[favoriteIndex]
                if (favStopRoute != null) {
                    val stopId = favStopRoute.stopId
                    val co = favStopRoute.co
                    val index = favStopRoute.index
                    val stop = favStopRoute.stop
                    val route = favStopRoute.route

                    Registry.getInstance(instance).findRoutes(route.routeNumber, true) { it ->
                        val bound = it.bound
                        if (!bound.containsKey(co) || bound[co] != route.bound[co]) {
                            return@findRoutes false
                        }
                        val stops = it.stops[co]?: return@findRoutes false
                        return@findRoutes stops.contains(stopId)
                    }.firstOrNull()?.let {
                        val intent = Intent(instance, ListStopsActivity::class.java)
                        intent.putExtra("route", it.serialize().toString())
                        intent.putExtra("scrollToStop", stopId)
                        instance.startActivity(intent)
                    }

                    val intent = Intent(instance, EtaActivity::class.java)
                    intent.putExtra("stopId", stopId)
                    intent.putExtra("co", co.name)
                    intent.putExtra("index", index)
                    intent.putExtra("stop", stop.serialize().toString())
                    intent.putExtra("route", route.serialize().toString())
                    instance.startActivity(intent)
                }
            }
        },
        onLongClick = {
            if (!deleteState) {
                deleteState = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Timer().schedule(TimerUtils.newTimerTask { if (deleteState) deleteState = false }, 5000)
                val text = if (Shared.language == "en") "Click again to confirm delete" else "再次點擊確認刪除"
                instance.runOnUiThread {
                    Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (deleteState) Color(0xFF633A3A) else MaterialTheme.colors.secondary,
            contentColor = if (deleteState) Color(0xFFFF0000) else if (favouriteStopRoute != null) Color(0xFFFFFF00) else Color(0xFF444444),
        ),
        shape = RoundedCornerShape(15.dp),
        enabled = favouriteStopRoute != null,
        content = {
            val favStopRoute = Shared.favoriteRouteStops[favoriteIndex]
            if (favStopRoute != null) {
                val stopId = favStopRoute.stopId
                val co = favStopRoute.co
                val route = favStopRoute.route

                var eta: Registry.ETAQueryResult? by remember { mutableStateOf(etaResults.value[favoriteIndex]) }

                LaunchedEffect (Unit) {
                    if (eta != null && !eta!!.isConnectionError) {
                        delay(etaUpdateTimes.value[favoriteIndex]?.let { (Shared.ETA_UPDATE_INTERVAL - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
                    }
                    schedule.invoke(true, favoriteIndex) {
                        eta = Registry.getEta(stopId, co, route, instance)
                        etaUpdateTimes.value[favoriteIndex] = System.currentTimeMillis()
                        etaResults.value[favoriteIndex] = eta!!
                    }
                }
                DisposableEffect (Unit) {
                    onDispose {
                        schedule.invoke(false, favoriteIndex, null)
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(10.dp, 8.dp)
                        .align(Alignment.BottomStart),
                ) {
                    if (eta != null && !eta!!.isConnectionError) {
                        if (eta!!.nextScheduledBus < 0 || eta!!.nextScheduledBus > 60) {
                            if (eta!!.isMtrEndOfLine) {
                                Icon(
                                    modifier = Modifier
                                        .size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp).dp),
                                    painter = painterResource(R.drawable.baseline_line_end_circle_24),
                                    contentDescription = if (Shared.language == "en") "End of Line" else "終點站",
                                    tint = Color(0xFF798996),
                                )
                            } else if (eta!!.isTyphoonSchedule) {
                                val desc by remember { derivedStateOf { Registry.getCurrentTyphoonData().get().typhoonWarningTitle } }
                                Image(
                                    modifier = Modifier.size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp).dp),
                                    painter = painterResource(R.mipmap.cyclone),
                                    contentDescription = desc
                                )
                            } else {
                                Icon(
                                    modifier = Modifier.size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(18F, instance).dp).dp),
                                    painter = painterResource(R.drawable.baseline_schedule_24),
                                    contentDescription = if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次",
                                    tint = Color(0xFF798996),
                                )
                            }
                        } else {
                            val text1 = (if (eta!!.nextScheduledBus == 0L) "-" else eta!!.nextScheduledBus.toString())
                            val text2 = if (Shared.language == "en") " Min." else "分鐘"
                            val span1 = SpannableString(text1)
                            val size1 = UnitUtils.spToPixels(instance, clampSp(instance, StringUtils.scaledSize(14F, instance), dpMax = StringUtils.scaledSize(15F, instance))).roundToInt()
                            span1.setSpan(AbsoluteSizeSpan(size1), 0, text1.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                            val span2 = SpannableString(text2)
                            val size2 = UnitUtils.spToPixels(instance, clampSp(instance, StringUtils.scaledSize(7F, instance), dpMax = StringUtils.scaledSize(8F, instance))).roundToInt()
                            span2.setSpan(AbsoluteSizeSpan(size2), 0, text2.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                            AnnotatedText(
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                fontSize = 14F.sp,
                                color = Color(0xFFAAC3D5),
                                lineHeight = 7F.sp,
                                maxLines = 1,
                                text = SpannableString(TextUtils.concat(span1, span2)).asAnnotatedString()
                            )
                        }
                    }
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
                        .width(StringUtils.scaledSize(35, instance).sp.clamp(max = 35.dp).dp)
                        .height(StringUtils.scaledSize(35, instance).sp.clamp(max = 35.dp).dp)
                        .clip(CircleShape)
                        .background(
                            if (favouriteStopRoute != null) Color(0xFF3D3D3D) else Color(
                                0xFF131313
                            )
                        )
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    if (deleteState) {
                        Icon(
                            modifier = Modifier.size(StringUtils.scaledSize(21, instance).dp),
                            imageVector = Icons.Filled.Clear,
                            tint = Color(0xFFFF0000),
                            contentDescription = if (Shared.language == "en") "Clear Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "清除資訊方塊路線巴士站預計到達時間".plus(favoriteIndex)
                        )
                    } else {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = StringUtils.scaledSize(17F, instance).sp,
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
                        fontSize = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp),
                        text = if (Shared.language == "en") "No Route Selected" else "未有設置路線"
                    )
                } else {
                    val index = currentFavouriteStopRoute.index
                    val stop = currentFavouriteStopRoute.stop
                    val stopName = stop.name
                    val route = currentFavouriteStopRoute.route
                    val kmbCtbJoint = route.isKmbCtbJoint
                    val co = currentFavouriteStopRoute.co
                    val routeNumber = route.routeNumber
                    val stopId = currentFavouriteStopRoute.stopId
                    val destName = Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route)
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
                    val routeText = if (Shared.language == "en") {
                        "To ".plus(destName.en)
                    } else {
                        "往".plus(destName.zh)
                    }
                    val subText = if (Shared.language == "en") {
                        (if (co == Operator.MTR || co == Operator.LRT) "" else index.toString().plus(". ")).plus(stopName.en)
                    } else {
                        (if (co == Operator.MTR || co == Operator.LRT) "" else index.toString().plus(". ")).plus(stopName.zh)
                    }
                    Spacer(modifier = Modifier.size(5.dp))
                    Column {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(Int.MAX_VALUE),
                            textAlign = TextAlign.Start,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = StringUtils.scaledSize(16F, instance).sp.clamp(max = 16.dp),
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
                            fontSize = StringUtils.scaledSize(14F, instance).sp.clamp(max = 14.dp),
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
                            fontSize = StringUtils.scaledSize(11F, instance).sp.clamp(max = 11.dp),
                            maxLines = 1,
                            text = subText
                        )
                    }
                }
            }
        }
    )
}