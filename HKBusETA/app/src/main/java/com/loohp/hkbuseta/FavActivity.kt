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
import androidx.compose.foundation.gestures.ScrollableDefaults
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.fullPageVerticalScrollWithScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.JsonUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.UnitUtils
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dp
import kotlinx.coroutines.delay
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ForkJoinPool
import kotlin.math.roundToInt


class FavActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)
        setContent {
            FavElements(this)
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

@Composable
fun FavElements(instance: FavActivity) {
    HKBusETATheme {
        val focusRequester = remember { FocusRequester() }
        val scroll = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalScrollWithScrollbar(
                    state = scroll,
                    flingBehavior = ScrollableDefaults.flingBehavior()
                )
                .rotaryScroll(scroll, focusRequester),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp))
            FavTitle(instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(5, instance).dp))
            FavDescription(instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            FavButton(1, instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            FavButton(2, instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            FavButton(3, instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            FavButton(4, instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            FavButton(5, instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            FavButton(6, instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            FavButton(7, instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            FavButton(8, instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(40, instance).dp))
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
        fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp).clamp(max = 17.dp),
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
        fontSize = TextUnit(StringUtils.scaledSize(11F, instance), TextUnitType.Sp).clamp(max = 11.dp),
        text = if (Shared.language == "en") "These routes will display in their corresponding indexed Tile" else "這些路線將顯示在其相應數字的資訊方塊中"
    )
}

@Composable
fun FavButton(favoriteIndex: Int, instance: FavActivity) {
    val hasFavouriteStopRoute = remember { mutableStateOf(Shared.favoriteRouteStops[favoriteIndex] != null) }
    val deleteState = remember { mutableStateOf(false) }

    LaunchedEffect (Unit) {
        while (true) {
            delay(500)
            val newState = Shared.favoriteRouteStops[favoriteIndex] != null
            if (newState != hasFavouriteStopRoute.value) {
                hasFavouriteStopRoute.value = newState
            }
        }
    }

    FavButtonInternal(favoriteIndex, hasFavouriteStopRoute, deleteState, instance)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavButtonInternal(favoriteIndex: Int, hasFavouriteStopRoute: MutableState<Boolean>, deleteState: MutableState<Boolean>, instance: FavActivity) {
    val haptic = LocalHapticFeedback.current

    AdvanceButton(
        onClick = {
            if (deleteState.value) {
                if (Registry.getInstance(instance).hasFavouriteRouteStop(favoriteIndex)) {
                    Registry.getInstance(instance).clearFavouriteRouteStop(favoriteIndex, instance)
                    Toast.makeText(instance, if (Shared.language == "en") "Cleared Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "已清除資訊方塊路線巴士站預計到達時間".plus(favoriteIndex), Toast.LENGTH_SHORT).show()
                }
                val newState = Shared.favoriteRouteStops[favoriteIndex] != null
                if (newState != hasFavouriteStopRoute.value) {
                    hasFavouriteStopRoute.value = newState
                }
                deleteState.value = false
            } else {
                val favouriteStopRoute = Shared.favoriteRouteStops[favoriteIndex]
                if (favouriteStopRoute != null) {
                    val stopId = favouriteStopRoute.optString("stopId")
                    val co = favouriteStopRoute.optString("co")
                    val index = favouriteStopRoute.optInt("index")
                    val stop = favouriteStopRoute.optJSONObject("stop")!!
                    val route = favouriteStopRoute.optJSONObject("route")!!

                    Registry.getInstance(instance).findRoutes(route.optString("route"), true) {
                        val bound = it.optJSONObject("bound")!!
                        if (!bound.has(co) || bound.optString(co) != route.optJSONObject("bound")!!.optString(co)) {
                            return@findRoutes false
                        }
                        val stops = it.optJSONObject("stops")!!.optJSONArray(co)?: return@findRoutes false
                        return@findRoutes JsonUtils.contains(stops, stopId)
                    }.firstOrNull()?.let {
                        val intent = Intent(instance, ListStopsActivity::class.java)
                        intent.putExtra("route", it.toString())
                        instance.startActivity(intent)
                    }

                    val intent = Intent(instance, EtaActivity::class.java)
                    intent.putExtra("stopId", stopId)
                    intent.putExtra("co", co)
                    intent.putExtra("index", index)
                    intent.putExtra("stop", stop.toString())
                    intent.putExtra("route", route.toString())
                    instance.startActivity(intent)
                }
            }
        },
        onLongClick = {
            if (!deleteState.value) {
                deleteState.value = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        if (deleteState.value) {
                            deleteState.value = false
                        }
                    }
                }, 5000)

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
            backgroundColor = if (deleteState.value) Color(0xFF633A3A) else MaterialTheme.colors.secondary,
            contentColor = if (deleteState.value) Color(0xFFFF0000) else if (hasFavouriteStopRoute.value) Color(0xFFFFFF00) else Color(0xFF444444),
        ),
        shape = RoundedCornerShape(15.dp),
        enabled = hasFavouriteStopRoute.value,
        content = {
            val favouriteStopRoute = Shared.favoriteRouteStops[favoriteIndex]
            if (favouriteStopRoute != null) {
                val stopId = favouriteStopRoute.optString("stopId")
                val co = favouriteStopRoute.optString("co")
                val route = favouriteStopRoute.optJSONObject("route")!!

                var eta: Registry.ETAQueryResult? by remember { mutableStateOf(Registry.ETAQueryResult.EMPTY) }

                LaunchedEffect (Unit) {
                    while (true) {
                        ForkJoinPool.commonPool().execute { eta = Registry.getEta(stopId, co, route, instance) }
                        delay(30000)
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
                                        .size(
                                            TextUnit(
                                                StringUtils.scaledSize(16F, instance),
                                                TextUnitType.Sp
                                            ).clamp(max = StringUtils.scaledSize(18F, instance).dp).dp
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
                                textAlign = TextAlign.Start,
                                fontSize = TextUnit(14F, TextUnitType.Sp),
                                color = Color(0xFFAAC3D5),
                                lineHeight = TextUnit(7F, TextUnitType.Sp),
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
                        .background(if (hasFavouriteStopRoute.value) Color(0xFF3D3D3D) else Color(0xFF131313))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    if (deleteState.value) {
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
                            fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
                            color = if (hasFavouriteStopRoute.value) Color(0xFFFFFF00) else Color(0xFF444444),
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
                        fontSize = TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp).clamp(max = 16.dp),
                        text = if (Shared.language == "en") "No Route Selected" else "未有設置路線"
                    )
                } else {
                    val index = currentFavouriteStopRoute.optInt("index")
                    val stop = currentFavouriteStopRoute.optJSONObject("stop")!!
                    val stopName = stop.optJSONObject("name")!!
                    val route = currentFavouriteStopRoute.optJSONObject("route")!!
                    val kmbCtbJoint = route.optBoolean("kmbCtbJoint", false)
                    val co = currentFavouriteStopRoute.optString("co")
                    val routeNumber = route.optString("route")
                    val stopId = currentFavouriteStopRoute.optString("stopId")
                    val destName = Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route)
                    val rawColor = when (co) {
                        "kmb" -> if (Shared.isLWBRoute(routeNumber)) Color(0xFFF26C33) else Color(0xFFFF4747)
                        "ctb" -> Color(0xFFFFE15E)
                        "nlb" -> Color(0xFF9BFFC6)
                        "mtr-bus" -> Color(0xFFAAD4FF)
                        "gmb" -> Color(0xFF36FF42)
                        "lightRail" -> Color(0xFFD3A809)
                        "mtr" -> {
                            when (routeNumber) {
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

                    val operator = if (Shared.language == "en") {
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
                    }
                    val mainText = operator.plus(" ").plus(if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber)
                    val routeText = if (Shared.language == "en") {
                        "To ".plus(StringUtils.capitalize(destName.optString("en")))
                    } else {
                        "往".plus(destName.optString("zh"))
                    }
                    val subText = if (Shared.language == "en") {
                        index.toString().plus(". ").plus(StringUtils.capitalize(stopName.optString("en")))
                    } else {
                        index.toString().plus(". ").plus(stopName.optString("zh"))
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
                            fontSize = TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp).clamp(max = 16.dp),
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
                            fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp).clamp(max = 14.dp),
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
                            fontSize = TextUnit(StringUtils.scaledSize(11F, instance), TextUnitType.Sp).clamp(max = 11.dp),
                            maxLines = 1,
                            text = subText
                        )
                    }
                }
            }
        }
    )
}