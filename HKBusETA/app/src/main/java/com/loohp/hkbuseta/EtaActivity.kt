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
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.objects.BilingualText
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.Route
import com.loohp.hkbuseta.objects.Stop
import com.loohp.hkbuseta.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.objects.name
import com.loohp.hkbuseta.objects.operator
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Registry.ETAQueryResult
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ActivityUtils
import com.loohp.hkbuseta.utils.MutableHolder
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.ScreenSizeUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.sameValueAs
import com.loohp.hkbuseta.utils.sp
import com.loohp.hkbuseta.utils.toSpanned
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


@Stable
class EtaActivity : ComponentActivity() {

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val etaUpdatesMap: MutableHolder<Pair<ScheduledFuture<*>?, () -> Unit>> = MutableHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)

        val stopId = intent.extras!!.getString("stopId")
        val co = intent.extras!!.getString("co")?.operator
        val index = intent.extras!!.getInt("index")
        val stop = intent.extras!!.getString("stop")?.let { Stop.deserialize(JSONObject(it)) }
        val route = intent.extras!!.getString("route")?.let { Route.deserialize(JSONObject(it)) }
        if (stopId == null || co == null || stop == null || route == null) {
            throw RuntimeException()
        }
        val offsetStart = intent.extras!!.getInt("offset", 0)

        setContent {
            EtaElement(stopId, co, index, stop, route, offsetStart, this) { isAdd, task ->
                synchronized(etaUpdatesMap) {
                    if (isAdd) {
                        etaUpdatesMap.computeIfAbsent { executor.scheduleWithFixedDelay(task, 0, 30, TimeUnit.SECONDS) to task!! }
                    } else {
                        etaUpdatesMap.remove()?.first?.cancel(true)
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
            etaUpdatesMap.replace { value ->
                value.first?.cancel(true)
                executor.scheduleWithFixedDelay(value.second, 0, 30, TimeUnit.SECONDS) to value.second
            }
        }
    }

    override fun onPause() {
        super.onPause()
        synchronized(etaUpdatesMap) {
            etaUpdatesMap.ifPresent { it.first?.cancel(true) }
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

@OptIn(ExperimentalWearMaterialApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun EtaElement(stopId: String, co: Operator, index: Int, stop: Stop, route: Route, offsetStart: Int, instance: EtaActivity, schedule: (Boolean, (() -> Unit)?) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val swipe = rememberSwipeableState(initialValue = false)
    var swiping by remember { mutableStateOf(swipe.offset.value != 0F) }

    val routeNumber = route.routeNumber

    if (swipe.currentValue) {
        instance.runOnUiThread {
            val text = if (Shared.language == "en") {
                "Nearby Interchange Routes of ".plus(stop.name.en)
            } else {
                "".plus(stop.name.zh).plus(" 附近轉乘路線")
            }
            Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
        }
        val intent = Intent(instance, NearbyActivity::class.java)
        intent.putExtra("interchangeSearch", true)
        intent.putExtra("lat", stop.location.lat)
        intent.putExtra("lng", stop.location.lng)
        intent.putExtra("exclude", arrayListOf(route.routeNumber))
        ActivityUtils.startActivity(instance, intent) { _ ->
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
        if (co == Operator.NLB) route.nlbId else route.bound[co],
        co,
        route.gtfsId
    ).toImmutableList() }

    val focusRequester = remember { FocusRequester() }
    var currentOffset by remember { mutableStateOf(offsetStart * ScreenSizeUtils.getScreenHeight(instance).toFloat()) }
    var animatedOffsetTask: (Float) -> Unit by remember { mutableStateOf({}) }
    val animatedOffset by animateFloatAsState(
        targetValue = currentOffset,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        finishedListener = animatedOffsetTask,
        label = "OffsetAnimation"
    )

    LaunchedEffect (Unit) {
        focusRequester.requestFocus()
        currentOffset = 0F
    }

    HKBusETATheme {
        Box (
            modifier = Modifier
                .fillMaxSize()
                .composed {
                    this.offset(animatedOffset.equivalentDp, swipe.offset.value.coerceAtMost(0F).equivalentDp)
                }
                .swipeable(
                    state = swipe,
                    anchors = mapOf(0F to false, -ScreenSizeUtils.getScreenHeight(instance).toFloat() to true),
                    orientation = Orientation.Vertical
                )
                .onRotaryScrollEvent {
                    if (it.horizontalScrollPixels > 0) {
                        if (index < stopList.size) {
                            currentOffset = -ScreenSizeUtils.getScreenWidth(instance).toFloat()
                            animatedOffsetTask = { launchOtherStop(index + 1, co, stopList, true, 1, instance) }
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    } else {
                        if (index > 1) {
                            currentOffset = ScreenSizeUtils.getScreenWidth(instance).toFloat()
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

                var eta: ETAQueryResult? by remember { mutableStateOf(null) }

                LaunchedEffect (Unit) {
                    if (eta != null && !eta!!.isConnectionError) {
                        delay(Shared.ETA_UPDATE_INTERVAL)
                    }
                    schedule.invoke(true) { eta = Registry.getEta(stopId, co, route, instance) }
                }
                DisposableEffect (Unit) {
                    onDispose {
                        schedule.invoke(false, null)
                    }
                }

                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                Title(index, stop.name, lat, lng, routeNumber, co, instance)
                SubTitle(Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route), lat, lng, routeNumber, co, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(9, instance).dp))
                EtaText(eta, 1, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(3, instance).dp))
                EtaText(eta, 2, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(3, instance).dp))
                EtaText(eta, 3, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(3, instance).dp))
                ActionBar(stopId, co, index, stop, route, stopList, instance)
            }
        }
    }
}

fun launchOtherStop(newIndex: Int, co: Operator, stopList: List<Registry.StopData>, animation: Boolean, offset: Int, instance: EtaActivity) {
    val newStopData = stopList[newIndex - 1]
    val intent = Intent(instance, EtaActivity::class.java)
    intent.putExtra("stopId", newStopData.stopId)
    intent.putExtra("co", co.name)
    intent.putExtra("index", newIndex)
    intent.putExtra("stop", newStopData.stop.serialize().toString())
    intent.putExtra("route", newStopData.route.serialize().toString())
    intent.putExtra("offset", offset)
    if (!animation) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
    instance.startActivity(intent)
    instance.finish()
}

@Composable
fun ActionBar(stopId: String, co: Operator, index: Int, stop: Stop, route: Route, stopList: ImmutableList<Registry.StopData>, instance: EtaActivity) {
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
                .width(StringUtils.scaledSize(24, instance).dp)
                .height(StringUtils.scaledSize(24, instance).dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = if (index > 1) Color(0xFFFFFFFF) else Color(0xFF494949)
            ),
            enabled = index > 1,
            content = {
                Icon(
                    modifier = Modifier.size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(16F, instance).dp).dp),
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = if (Shared.language == "en") "Previous Stop" else "上一站",
                    tint = if (index > 1) Color(0xFFFFFFFF) else Color(0xFF494949),
                )
            }
        )
        Spacer(modifier = Modifier.size(StringUtils.scaledSize(2, instance).dp))
        AdvanceButton(
            onClick = {
                val intent = Intent(instance, EtaMenuActivity::class.java)
                intent.putExtra("stopId", stopId)
                intent.putExtra("co", co.name)
                intent.putExtra("index", index)
                intent.putExtra("stop", stop.serialize().toString())
                intent.putExtra("route", route.serialize().toString())
                instance.startActivity(intent)
            },
            modifier = Modifier
                .width(StringUtils.scaledSize(55, instance).dp)
                .height(StringUtils.scaledSize(24, instance).dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.primary
            ),
            content = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = StringUtils.scaledSize(12F, instance).sp.clamp(max = 12.dp),
                    text = if (Shared.language == "en") "More" else "更多"
                )
            }
        )
        Spacer(modifier = Modifier.size(StringUtils.scaledSize(2, instance).dp))
        AdvanceButton(
            onClick = {
                launchOtherStop(index + 1, co, stopList, true, 1, instance)
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                launchOtherStop(stopList.size, co, stopList, true, 1, instance)
            },
            modifier = Modifier
                .width(StringUtils.scaledSize(24, instance).dp)
                .height(StringUtils.scaledSize(24, instance).dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = if (index < stopList.size) Color(0xFFFFFFFF) else Color(0xFF494949)
            ),
            enabled = index < stopList.size,
            content = {
                Icon(
                    modifier = Modifier.size(StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(16F, instance).dp).dp),
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = if (Shared.language == "en") "Next Stop" else "下一站",
                    tint = if (index < stopList.size) Color(0xFFFFFFFF) else Color(0xFF494949),
                )
            }
        )
    }
}

fun handleOpenMaps(lat: Double, lng: Double, label: String, instance: EtaActivity, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
    return {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("geo:0,0?q=".plus(lat).plus(",").plus(lng).plus("(").plus(label).plus(")")))
        if (longClick) {
            instance.startActivity(intent)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            RemoteActivityUtils.intentToPhone(
                instance = instance,
                intent = intent,
                noPhone = {
                    instance.startActivity(intent)
                },
                failed = {
                    instance.startActivity(intent)
                },
                success = {
                    instance.runOnUiThread {
                        Toast.makeText(instance, if (Shared.language == "en") "Please check your phone" else "請在手機上繼續", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Title(index: Int, stopName: BilingualText, lat: Double, lng: Double, routeNumber: String, co: Operator, instance: EtaActivity) {
    val haptic = LocalHapticFeedback.current
    val name = if (Shared.language == "en") stopName.en else stopName.zh
    AutoResizeText (
        modifier = Modifier
            .fillMaxWidth()
            .padding(37.dp, 0.dp)
            .combinedClickable(
                onClick = handleOpenMaps(lat, lng, name, instance, false, haptic),
                onLongClick = handleOpenMaps(lat, lng, name, instance, true, haptic)
            ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = if (co == Operator.MTR) name else index.toString().plus(". ").plus(name),
        maxLines = 2,
        fontWeight = FontWeight(900),
        fontSizeRange = FontSizeRange(
            min = StringUtils.scaledSize(1F, instance).dp.sp,
            max = StringUtils.scaledSize(17F, instance).sp.clamp(max = StringUtils.scaledSize(17F, instance).dp)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubTitle(destName: BilingualText, lat: Double, lng: Double, routeNumber: String, co: Operator, instance: EtaActivity) {
    val haptic = LocalHapticFeedback.current
    val routeName = co.getDisplayRouteNumber(routeNumber)
    val name = if (Shared.language == "en") {
        routeName.plus(" To ").plus(destName.en)
    } else {
        routeName.plus(" 往").plus(destName.zh)
    }
    AutoResizeText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp)
            .combinedClickable(
                onClick = handleOpenMaps(lat, lng, name, instance, false, haptic),
                onLongClick = handleOpenMaps(lat, lng, name, instance, true, haptic)
            ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = name,
        maxLines = 1,
        fontSizeRange = FontSizeRange(
            min = StringUtils.scaledSize(1F, instance).dp.sp,
            max = StringUtils.scaledSize(11F, instance).sp.clamp(max = StringUtils.scaledSize(11F, instance).dp)
        )
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EtaText(lines: ETAQueryResult?, seq: Int, instance: EtaActivity) {
    val textSize = StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(16F, instance).dp)
    AnnotatedText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp)
            .basicMarquee(iterations = Int.MAX_VALUE),
        textAlign = TextAlign.Center,
        fontSize = textSize,
        color = MaterialTheme.colors.primary,
        maxLines = 1,
        text = (lines?.getLine(seq)?: if (seq == 1) (if (Shared.language == "en") "Updating" else "更新中") else "").toSpanned(instance, textSize.value).asAnnotatedString()
    )
}