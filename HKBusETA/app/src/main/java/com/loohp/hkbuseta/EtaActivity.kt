package com.loohp.hkbuseta

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.horizontalScrollWithScrollbar
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Registry.ETAQueryResult
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ActivityUtils
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.ScreenSizeUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.StringUtilsKt
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.sameValueAs
import com.loohp.hkbuseta.utils.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject


class EtaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)

        val stopId = intent.extras!!.getString("stopId")
        val co = intent.extras!!.getString("co")
        val index = intent.extras!!.getInt("index")
        val stop = intent.extras!!.getString("stop")?.let { JSONObject(it) }
        val route = intent.extras!!.getString("route")?.let { JSONObject(it) }
        if (stopId == null || co == null || stop == null || route == null) {
            throw RuntimeException()
        }
        setContent {
            EtaElement(stopId, co, index, stop, route, this)
        }
    }

    override fun onStart() {
        super.onStart()
        Shared.setSelfAsCurrentActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Shared.removeSelfFromCurrentActivity(this)
        }
    }

}

@OptIn(ExperimentalWearMaterialApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun EtaElement(stopId: String, co: String, index: Int, stop: JSONObject, route: JSONObject, instance: EtaActivity) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val swipe = rememberSwipeableState(initialValue = false)
    var swiping by remember { mutableStateOf(swipe.offset.value != 0F) }

    if (swipe.currentValue) {
        instance.runOnUiThread {
            val text = if (Shared.language == "en") {
                "Nearby Interchange Routes of ".plus(StringUtils.capitalize(stop.optJSONObject("name")!!.optString("en")))
            } else {
                "".plus(stop.optJSONObject("name")!!.optString("zh")).plus(" 附近轉乘路線")
            }
            Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
        }
        val intent = Intent(instance, NearbyActivity::class.java)
        intent.putExtra("lat", stop.optJSONObject("location")!!.optDouble("lat"))
        intent.putExtra("lng", stop.optJSONObject("location")!!.optDouble("lng"))
        intent.putExtra("exclude", arrayListOf(route.optString("route")))
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

    HKBusETATheme {
        Box (
            modifier = Modifier
                .fillMaxSize()
                .offset(0.dp, swipe.offset.value.coerceAtMost(0F).equivalentDp)
                .swipeable(
                    state = swipe,
                    anchors = mapOf(0F to false, -ScreenSizeUtils.getScreenHeight(instance).toFloat() to true),
                    orientation = Orientation.Vertical
                )
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
                val focusRequester = remember { FocusRequester() }
                val scroll = rememberScrollState()

                var scrollCounter by remember { mutableStateOf(0) }
                val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
                val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
                var scrollMoved by remember { mutableStateOf(false) }
                LaunchedEffect (scrollInProgress) {
                    if (scrollInProgress) {
                        scrollCounter++
                    }
                }
                LaunchedEffect (scrollCounter, scrollReachedEnd) {
                    delay(50)
                    if (scrollReachedEnd && scrollMoved) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    scrollMoved = true
                }

                var eta: ETAQueryResult by remember { mutableStateOf(ETAQueryResult.EMPTY) }

                val lat = stop.optJSONObject("location")!!.optDouble("lat")
                val lng = stop.optJSONObject("location")!!.optDouble("lng")

                LaunchedEffect (Unit) {
                    focusRequester.requestFocus()
                    while (true) {
                        Thread {
                            eta = Registry.getEta(stopId, co, route, instance)
                        }.start()
                        delay(Shared.ETA_UPDATE_INTERVAL)
                    }
                }
                val routeNumber = route.optString("route")

                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                Title(index, stop.optJSONObject("name")!!, lat, lng, routeNumber, co, instance)
                SubTitle(route.optJSONObject("dest")!!, lat, lng, routeNumber, co, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(9, instance).dp))
                EtaText(eta, 1, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(3, instance).dp))
                EtaText(eta, 2, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(3, instance).dp))
                EtaText(eta, 3, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(3, instance).dp))
                Row(
                    modifier = Modifier
                        .width(StringUtils.scaledSize(113, instance).dp)
                        .horizontalScrollWithScrollbar(
                            state = scroll,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            scrollbarConfig = ScrollBarConfig(
                                indicatorThickness = 2.dp,
                                padding = PaddingValues(2.dp, (-6).dp, 2.dp, (-6).dp)
                            )
                        )
                        .onRotaryScrollEvent {
                            scope.launch {
                                scroll.animateScrollBy(
                                    it.horizontalScrollPixels,
                                    TweenSpec(durationMillis = 500, easing = FastOutSlowInEasing))
                            }
                            true
                        }
                        .focusRequester(
                            focusRequester = focusRequester
                        )
                        .focusable(),
                    horizontalArrangement = Arrangement.Center
                )  {
                    FavButton(1, stopId, co, index, stop, route, instance)
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                    FavButton(2, stopId, co, index, stop, route, instance)
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                    FavButton(3, stopId, co, index, stop, route, instance)
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                    FavButton(4, stopId, co, index, stop, route, instance)
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                    FavButton(5, stopId, co, index, stop, route, instance)
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                    FavButton(6, stopId, co, index, stop, route, instance)
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                    FavButton(7, stopId, co, index, stop, route, instance)
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
                    FavButton(8, stopId, co, index, stop, route, instance)
                }
            }
        }
    }
}

fun getFavState(favoriteIndex: Int, stopId: String, co: String, index: Int, stop: JSONObject, route: JSONObject, instance: EtaActivity): Int {
    val registry = Registry.getInstance(instance)
    if (registry.hasFavouriteRouteStop(favoriteIndex)) {
        return if (registry.isFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route)) 2 else 1
    }
    return 0
}

@Composable
fun FavButton(favoriteIndex: Int, stopId: String, co: String, index: Int, stop: JSONObject, route: JSONObject, instance: EtaActivity) {
    val state = remember { mutableStateOf(getFavState(favoriteIndex, stopId, co, index, stop, route, instance)) }
    Button(
        onClick = {
            if (state.value == 2) {
                Registry.getInstance(instance).clearFavouriteRouteStop(favoriteIndex, instance)
                Toast.makeText(instance, if (Shared.language == "en") "Cleared Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "已清除資訊方塊路線巴士站預計到達時間".plus(favoriteIndex), Toast.LENGTH_SHORT).show()
            } else {
                Registry.getInstance(instance).setFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route, instance)
                Toast.makeText(instance, if (Shared.language == "en") "Set Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "已設置資訊方塊路線巴士站預計到達時間".plus(favoriteIndex), Toast.LENGTH_SHORT).show()
            }
            state.value = getFavState(favoriteIndex, stopId, co, index, stop, route, instance)
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(24, instance).dp)
            .height(StringUtils.scaledSize(24, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = when (state.value) {
                0 -> Color(0xFF444444)
                1 -> Color(0xFF4E4E00)
                2 -> Color(0xFFFFFF00)
                else -> Color(0xFF444444)
            }
        ),
        content = {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp),
                color = when (state.value) {
                    0 -> Color(0xFF444444)
                    1 -> Color(0xFF4E4E00)
                    2 -> Color(0xFFFFFF00)
                    else -> Color(0xFF444444)
                },
                text = favoriteIndex.toString()
            )
        }
    )
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
fun Title(index: Int, stopName: JSONObject, lat: Double, lng: Double, routeNumber: String, co: String, instance: EtaActivity) {
    val haptic = LocalHapticFeedback.current
    val name = if (Shared.language == "en") StringUtils.capitalize(stopName.optString("en")) else stopName.optString("zh")
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
        text = if (co == "mtr") name else index.toString().plus(". ").plus(name),
        maxLines = 2,
        fontWeight = FontWeight(900),
        fontSizeRange = FontSizeRange(
            min = StringUtils.scaledSize(1, instance).dp.sp,
            max = StringUtils.scaledSize(17, instance).sp.clamp(max = 18.dp)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubTitle(destName: JSONObject, lat: Double, lng: Double, routeNumber: String, co: String, instance: EtaActivity) {
    val haptic = LocalHapticFeedback.current
    val name = if (Shared.language == "en") {
        val routeName = if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber
        routeName.plus(" To ").plus(StringUtils.capitalize(destName.optString("en")))
    } else {
        val routeName = if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber
        routeName.plus(" 往").plus(destName.optString("zh"))
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
            min = StringUtils.scaledSize(1, instance).dp.sp,
            max = StringUtils.scaledSize(11, instance).sp.clamp(max = 12.dp)
        )
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EtaText(lines: ETAQueryResult, seq: Int, instance: EtaActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp)
            .basicMarquee(iterations = Int.MAX_VALUE),
        textAlign = TextAlign.Center,
        fontSize = StringUtils.scaledSize(16, instance).sp.clamp(max = 17.dp),
        color = MaterialTheme.colors.primary,
        maxLines = 1,
        text = StringUtilsKt.toAnnotatedString(HtmlCompat.fromHtml(lines.lines.getOrDefault(seq, "-"), HtmlCompat.FROM_HTML_MODE_COMPACT))
    )
}