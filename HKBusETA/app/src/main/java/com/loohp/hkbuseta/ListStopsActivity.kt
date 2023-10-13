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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Registry.StopData
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.DistanceUtils
import com.loohp.hkbuseta.utils.LocationUtils
import com.loohp.hkbuseta.utils.ScreenSizeUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.UnitUtils
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.formatDecimalSeparator
import com.loohp.hkbuseta.utils.sp
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


class ListStopsActivity : ComponentActivity() {

    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 4)
    private val etaUpdatesMap: MutableMap<Int, Pair<ScheduledFuture<*>?, () -> Unit>> = LinkedHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shared.setDefaultExceptionHandler(this)
        val route = intent.extras!!.getString("route")?.let { JSONObject(it) }?: throw RuntimeException()
        val scrollToStop = intent.extras!!.getString("scrollToStop")

        setContent {
            MainElement(this, route, scrollToStop) { isAdd, index, task ->
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
fun MainElement(instance: ListStopsActivity, route: JSONObject, scrollToStop: String?, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    HKBusETATheme {
        val focusRequester = remember { FocusRequester() }
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val padding by remember { derivedStateOf { StringUtils.scaledSize(7.5F, instance) } }

        val kmbCtbJoint = remember { route.optJSONObject("route")!!.optBoolean("kmbCtbJoint", false) }
        val routeNumber = remember { route.optJSONObject("route")!!.optString("route") }
        val co = remember { route.optString("co") }
        val bound = remember { if (co.equals("nlb")) route.optJSONObject("route")!!.optString("nlbId") else route.optJSONObject("route")!!.optJSONObject("bound")!!.optString(co) }
        val gtfsId = remember { route.optJSONObject("route")!!.optString("gtfsId") }
        val interchangeSearch = remember { route.optBoolean("interchangeSearch", false) }
        val destName = remember { route.optJSONObject("route")!!.optJSONObject("dest")!! }
        val specialDests = remember { Registry.getInstance(instance).getAllDestinations(routeNumber, bound, co, gtfsId).filter { it.optString("zh") != destName.optString("zh") } }
        val coColor = remember { when (co) {
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
        } }

        val stopsList = remember { Registry.getInstance(instance).getAllStops(routeNumber, bound, co, gtfsId) }
        val lowestServiceType = remember { stopsList.minOf { it.serviceType } }
        val mtrStopsInterchange = remember { if (co == "mtr" || co == "lightRail") {
            stopsList.map { Registry.getMtrStationInterchange(it.stopId, routeNumber) }
        } else emptyList() }
        val mtrLineCreator = remember { if (co == "mtr" || co == "lightRail") generateMTRLine(co,
            if (co == "lightRail") when (routeNumber) {
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
            } else coColor, stopsList, mtrStopsInterchange, instance
        ) else null }

        val distances: MutableMap<Int, Double> = remember { ConcurrentHashMap() }
        var closestIndex by remember { mutableStateOf(0) }

        val etaTextWidth by remember { derivedStateOf { StringUtils.findTextLengthDp(instance, "99", StringUtils.scaledSize(16F, instance)) + 1F } }

        val etaUpdateTimes: MutableMap<Int, Long> = remember { ConcurrentHashMap() }
        val etaResults: MutableMap<Int, Registry.ETAQueryResult> = remember { ConcurrentHashMap() }

        LaunchedEffect (Unit) {
            focusRequester.requestFocus()

            val scrollTask: (OriginData?, String?) -> Unit = { origin, stopId ->
                if (stopId != null) {
                    stopsList.withIndex().find { it.value.stopId == stopId }?.let {
                        scope.launch {
                            scroll.animateScrollToItem(it.index + 1, (-ScreenSizeUtils.getScreenHeight(instance) / 2) - UnitUtils.spToPixels(instance, StringUtils.scaledSize(15F, instance)).roundToInt())
                        }
                    }
                } else if (origin != null) {
                    stopsList.withIndex().map {
                        val (index, entry) = it
                        val stop = entry.stop
                        val location = stop.optJSONObject("location")!!
                        var stopStr = stop.optJSONObject("name")!!.optString(Shared.language)
                        if (Shared.language == "en") {
                            stopStr = StringUtils.capitalize(stopStr)
                        }
                        StopEntry(index + 1, stopStr, entry, location.optDouble("lat"), location.optDouble("lng"))
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
            } else if (route.has("origin")) {
                val origin = route.optJSONObject("origin")!!
                scrollTask.invoke(OriginData(origin.optDouble("lat"), origin.optDouble("lng")), null)
            } else {
                LocationUtils.checkLocationPermission(instance) {
                    if (it) {
                        val future = LocationUtils.getGPSLocation(instance)
                        Thread {
                            try {
                                val locationResult = future.get()
                                if (locationResult.isSuccess) {
                                    val location = locationResult.location
                                    scrollTask.invoke(OriginData(location.latitude, location.longitude, true), null)
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
                .rotaryScroll(scroll, focusRequester),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = scroll
        ) {
            item {
                HeaderElement(routeNumber, kmbCtbJoint, co, coColor, destName, specialDests, instance)
            }
            for ((index, entry) in stopsList.withIndex()) {
                item {
                    val stopNumber = index + 1
                    val isClosest = closestIndex == stopNumber
                    val stopId = entry.stopId
                    val stop = entry.stop
                    val brightness = if (entry.serviceType == lowestServiceType) 1F else 0.65F
                    val rawColor = (if (isClosest) coColor else Color.White).adjustBrightness(brightness)
                    var stopStr = stop.optJSONObject("name")!!.optString(Shared.language)
                    if (Shared.language == "en") {
                        stopStr = StringUtils.capitalize(stopStr)
                    }

                    Box (
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    val intent = Intent(instance, EtaActivity::class.java)
                                    intent.putExtra("stopId", stopId)
                                    intent.putExtra("co", co)
                                    intent.putExtra("index", stopNumber)
                                    intent.putExtra("stop", stop.toString())
                                    intent.putExtra("route", route.optJSONObject("route")!!.toString())
                                    instance.startActivity(intent)
                                },
                                onLongClick = {
                                    instance.runOnUiThread {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        instance.runOnUiThread {
                                            val prefix = if (co == "mtr" || co == "lightRail") "" else stopNumber.toString().plus(". ")
                                            val text = if (isClosest) {
                                                prefix.plus(stopStr).plus("\n").plus(if (interchangeSearch) (if (Shared.language == "en") "Interchange " else "轉乘") else (if (Shared.language == "en") "Nearby " else "附近"))
                                                    .plus(((distances[stopNumber]?: Double.NaN) * 1000).roundToInt().formatDecimalSeparator()).plus(if (Shared.language == "en") "m" else "米")
                                            } else {
                                                prefix.plus(stopStr)
                                            }
                                            Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                    ) {
                        StopRowElement(stopNumber, stopId, co, isClosest, kmbCtbJoint, mtrStopsInterchange, rawColor, brightness, padding, stopStr, stopsList, mtrLineCreator?.get(index), route, etaTextWidth, etaResults, etaUpdateTimes, instance, schedule)
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
            items(5) {
                Spacer(modifier = Modifier.size(7.dp))
            }
        }
    }
}

@Composable
fun HeaderElement(routeNumber: String, kmbCtbJoint: Boolean, co: String, coColor: Color, destName: JSONObject, specialDests: List<JSONObject>, instance: ListStopsActivity) {
    Column(
        modifier = Modifier
            .defaultMinSize(minHeight = 35.dp)
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
                max = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(20F, instance).dp)
            ),
            lineHeight = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp).clamp(max = StringUtils.scaledSize(20F, instance).dp),
            color = color,
            maxLines = 1,
            text = (if (Shared.language == "en") {
                when (co) {
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
                when (co) {
                    "kmb" -> if (Shared.isLWBRoute(routeNumber)) (if (kmbCtbJoint) "龍運/城巴" else "龍運") else (if (kmbCtbJoint) "九巴/城巴" else "九巴")
                    "ctb" -> "城巴"
                    "nlb" -> "嶼巴"
                    "mtr-bus" -> "港鐵巴士"
                    "gmb" -> "專線小巴"
                    "lightRail" -> "輕鐵"
                    "mtr" -> "港鐵"
                    else -> "???"
                }
            }).plus(" ").plus(if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber)
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
            text = if (Shared.language == "en") {
                "To ".plus(StringUtils.capitalize(destName.optString("en")))
            } else {
                "往".plus(destName.optString("zh"))
            }
        )
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
                    "Special To ".plus(specialDests.joinToString("/") { StringUtils.capitalize(it.optString("en")) })
                } else {
                    "特別班 往".plus(specialDests.joinToString("/") { it.optString("zh") })
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StopRowElement(stopNumber: Int, stopId: String, co: String, isClosest: Boolean, kmbCtbJoint: Boolean, mtrStopsInterchange: List<Registry.MTRInterchangeData>, rawColor: Color, brightness: Float, padding: Float, stopStr: String, stopList: List<StopData>, mtrLineCreator: (@Composable () -> Unit)?, route: JSONObject, etaTextWidth: Float, etaResults: MutableMap<Int, Registry.ETAQueryResult>, etaUpdateTimes: MutableMap<Int, Long>, instance: ListStopsActivity, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    var height by remember { mutableStateOf(0) }
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
        val color = if (isClosest && kmbCtbJoint) {
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

        if ((co == "mtr" || co == "lightRail") && mtrLineCreator != null) {
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
                fontSize = TextUnit(StringUtils.scaledSize(15F, instance), TextUnitType.Sp),
                fontWeight = if (isClosest) FontWeight.Bold else FontWeight.Normal,
                color = color,
                maxLines = 1,
                text = stopNumber.toString().plus(".")
            )
        }
        Text(
            modifier = Modifier
                .padding(0.dp, padding.dp)
                .basicMarquee(iterations = Int.MAX_VALUE)
                .weight(1F),
            textAlign = TextAlign.Start,
            fontSize = TextUnit(StringUtils.scaledSize(15F, instance), TextUnitType.Sp),
            fontWeight = if (isClosest) FontWeight.Bold else FontWeight.Normal,
            color = color,
            maxLines = 1,
            text = stopStr
        )
        Box (
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            ETAElement(stopNumber, stopId, route, etaTextWidth, etaResults, etaUpdateTimes, instance, schedule)
        }
    }
}

@Composable
fun ETAElement(index: Int, stopId: String, route: JSONObject, etaTextWidth: Float, etaResults: MutableMap<Int, Registry.ETAQueryResult>, etaUpdateTimes: MutableMap<Int, Long>, instance: ListStopsActivity, schedule: (Boolean, Int, (() -> Unit)?) -> Unit) {
    var eta: Registry.ETAQueryResult? by remember { mutableStateOf(etaResults[index]) }

    LaunchedEffect (Unit) {
        if (eta != null && !eta!!.isConnectionError) {
            delay(etaUpdateTimes[index]?.let { (30000 - (System.currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        schedule.invoke(true, index) {
            eta = Registry.getEta(stopId, route.optString("co"), route.optJSONObject("route")!!, instance)
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
            .offset(0.dp, -TextUnit(2F, TextUnitType.Sp).dp),
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
                        contentDescription = Registry.getInstance(instance).typhoonWarningTitle
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
                    fontSize = TextUnit(14F, TextUnitType.Sp),
                    color = Color(0xFFAAC3D5),
                    lineHeight = TextUnit(7F, TextUnitType.Sp),
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

fun generateMTRLine(co: String, color: Color, stopList: List<StopData>, mtrStopsInterchange: List<Registry.MTRInterchangeData>, instance: ListStopsActivity): List<@Composable () -> Unit> {
    val creators: MutableList<@Composable () -> Unit> = ArrayList(stopList.size)
    val stopByBranchId: MutableMap<Int, MutableList<StopData>> = HashMap()
    stopList.forEach { stop -> stop.branchIds.forEach { stopByBranchId.computeIfAbsent(it) { ArrayList() }.add(stop) } }
    val hasOutOfStation = mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }
    for ((index, stop) in stopList.withIndex()) {
        val isMainLine = stop.serviceType == 1
        val interchangeData = mtrStopsInterchange[index]
        creators.add {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height

                val leftShift = if (hasOutOfStation) UnitUtils.pixelsToDp(instance, 22F).sp.toPx() else 0
                val horizontalCenter = width / 2F - leftShift.toFloat()
                val horizontalPartition = width / 10F
                val horizontalCenterPrimary = if (stopByBranchId.size == 1) horizontalCenter else horizontalPartition * 3F
                val horizontalCenterSecondary = horizontalPartition * 7F
                val verticalCenter = height / 2F
                val lineWidth = UnitUtils.pixelsToDp(instance, 11F).sp.toPx()
                val lineOffset = UnitUtils.pixelsToDp(instance, 8F).sp.toPx()
                val dashEffect = floatArrayOf(UnitUtils.pixelsToDp(instance, 14F).sp.toPx(), UnitUtils.pixelsToDp(instance, 7F).sp.toPx())

                var useSpurStopCircle = false
                if (isMainLine) {
                    if (stopByBranchId.values.all { it.indexOf(stop) <= 0 }) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterPrimary, verticalCenter),
                            end = Offset(horizontalCenterPrimary, height),
                            strokeWidth = lineWidth
                        )
                    } else if (stopByBranchId.values.all { it.indexOf(stop).let { x -> x < 0 || x >= it.size - 1 } }) {
                        drawLine(
                            color = color,
                            start = Offset(horizontalCenterPrimary, 0F),
                            end = Offset(horizontalCenterPrimary, verticalCenter),
                            strokeWidth = lineWidth
                        )
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
                val interchangeLineWidth = UnitUtils.pixelsToDp(instance, 14F).sp.toPx() * 2F
                val interchangeLineHeight = UnitUtils.pixelsToDp(instance, 6F).sp.toPx() * 2F
                val interchangeLineSpacing = interchangeLineHeight * 1.5F
                if (interchangeData.isHasLightRail && co != "lightRail") {
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

                val circleWidth = UnitUtils.pixelsToDp(instance, 20F).sp.toPx()

                if (interchangeData.outOfStationLines.isNotEmpty()) {
                    val otherStationHorizontalCenter = horizontalCenterPrimary + circleWidth * 2F
                    val connectionLineWidth = UnitUtils.pixelsToDp(instance, 5F).sp.toPx()
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
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(UnitUtils.pixelsToDp(instance, 4F).sp.toPx(), UnitUtils.pixelsToDp(instance, 2F).sp.toPx()), 0F)
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
        }
    }
    return creators
}