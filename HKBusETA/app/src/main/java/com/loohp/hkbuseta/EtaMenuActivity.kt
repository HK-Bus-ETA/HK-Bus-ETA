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
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.objects.BilingualText
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.Route
import com.loohp.hkbuseta.objects.Stop
import com.loohp.hkbuseta.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.objects.operator
import com.loohp.hkbuseta.services.AlightReminderService
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ActivityUtils
import com.loohp.hkbuseta.utils.LocationUtils
import com.loohp.hkbuseta.utils.NotificationUtils
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.ScreenSizeUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.sp
import com.loohp.hkbuseta.utils.toSpanned
import org.json.JSONObject
import java.util.stream.IntStream


enum class FavouriteRouteState {

    NOT_USED, USED_OTHER, USED_SELF

}

@Stable
class EtaMenuActivity : ComponentActivity() {

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
        setContent {
            EtaMenuElement(stopId, co, index, stop, route, this)
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
fun EtaMenuElement(stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: EtaMenuActivity) {
    HKBusETATheme {
        val focusRequester = remember { FocusRequester() }
        val scroll = rememberLazyListState()

        val maxFavItems by remember { Shared.getSuggestedMaxFavouriteRouteStopState() }

        val routeNumber = route.routeNumber
        val lat = stop.location.lat
        val lng = stop.location.lng

        val scrollTo = remember { mutableIntStateOf(0) }

        LaunchedEffect (scrollTo.intValue) {
            val value = scrollTo.intValue
            if (value > 0) {
                scroll.animateScrollToItem(value + 8, -ScreenSizeUtils.getScreenHeight(instance) / 3)
                scrollTo.intValue = 0
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
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp))
            }
            item {
                Title(index, stop.name, routeNumber, co, instance)
                SubTitle(Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route, true), routeNumber, co, instance)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                MoreInfoHeader(instance)
            }
            item {
                if (stop.kmbBbiId != null) {
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                    OpenOpenKmbBbiMapButton(stop.kmbBbiId, instance)
                }
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                SearchNearbyButton(stop, route, instance)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                AlightReminderButton(stopId, index, stop, route, co, instance)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                OpenOnMapsButton(stop.name, lat, lng, instance)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                FavHeader(instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                NextFavButton(scrollTo, stopId, co, index, stop, route, instance)
            }
            items(maxFavItems) {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                FavButton(it + 1, stopId, co, index, stop, route, instance)
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(40, instance).dp))
            }
        }
    }
}

@Composable
fun MoreInfoHeader(instance: EtaMenuActivity) {
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(14F, instance).sp.clamp(max = 14.dp),
        text = if (Shared.language == "en") "More Info & Actions" else "更多資訊及功能"
    )
}

@Composable
fun SearchNearbyButton(stop: Stop, route: Route, instance: EtaMenuActivity) {
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(50, instance).sp.clamp(min = 50.dp).dp),
        onClick = {
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
            instance.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .height(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(17F, instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_sync_alt_24),
                        tint = Color(0xFFFFE15E),
                        contentDescription = if (Shared.language == "en") "Find Nearby Interchanges" else "尋找附近轉乘路線"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = StringUtils.scaledSize(14F, instance).sp,
                    text = if (Shared.language == "en") "Find Nearby Interchanges" else "尋找附近轉乘路線"
                )
            }
        }
    )
}

@Composable
fun AlightReminderButton(stopId: String, index: Int, stop: Stop, route: Route, co: Operator, instance: EtaMenuActivity) {
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(50, instance).sp.clamp(min = 50.dp).dp),
        onClick = {
            LocationUtils.checkLocationPermission(instance) { locationGranted ->
                if (locationGranted) {
                    NotificationUtils.checkNotificationPermission(instance) { notificationGranted ->
                        if (notificationGranted) {
                            Registry.getInstance(instance).findRoutes(route.routeNumber, true) { it -> it == route }.first().let {
                                val intent = Intent(instance, AlightReminderService::class.java)
                                intent.putExtra("stop", stop.serialize().toString())
                                intent.putExtra("route", route.serialize().toString())
                                intent.putExtra("index", index)
                                intent.putExtra("co", co.name())

                                val stopListIntent = Intent(instance, ListStopsActivity::class.java)
                                stopListIntent.putExtra("route", it.serialize().toString())
                                stopListIntent.putExtra("scrollToStop", stopId)
                                stopListIntent.putExtra("showEta", false)
                                stopListIntent.putExtra("isAlightReminder", true)

                                val noticeIntent = Intent(instance, DismissibleTextDisplayActivity::class.java)
                                val notice = BilingualText(
                                    "你可能需要「<b>允許背景活動</b>」讓此功能在螢幕關閉時繼續正常運作<br><br>此功能目前在<b>測試階段</b>, 運作可能不穩定",
                                    "You might need to \"<b>Allow Background Activity</b>\" for this feature to continue working while the screen is off.<br><br>This feature is currently in <b>beta</b>, which might be unstable."
                                )
                                noticeIntent.putExtra("text", notice.serialize().toString())
                                ActivityUtils.startActivity(instance, noticeIntent) { result ->
                                    if (result.resultCode == 1) {
                                        AlightReminderService.terminate()
                                        instance.startForegroundService(intent)
                                        instance.startActivity(stopListIntent)
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(instance, if (Shared.language == "en") "Notification Permission Denied" else "推送通知權限被拒絕", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(instance, if (Shared.language == "en") "Location Access Permission Denied" else "位置存取權限被拒絕", Toast.LENGTH_SHORT).show()
                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .height(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(17F, instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_notifications_active_24),
                        tint = Color(0xFFFF9800),
                        contentDescription = if (Shared.language == "en") "Enable Alight Reminder" else "開啟落車提示"
                    )
                }
                AnnotatedText(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = StringUtils.scaledSize(14F, instance).sp,
                    maxLines = 2,
                    text = (if (Shared.language == "en") "<b>Enable Alight Reminder</b> <small>Beta</small>" else "<b>開啟落車提示</b><br><small>測試版</small>").toSpanned(instance).asAnnotatedString()
                )
            }
        }
    )
}

@Composable
fun OpenOnMapsButton(stopName: BilingualText, lat: Double, lng: Double, instance: EtaMenuActivity) {
    val haptic = LocalHapticFeedback.current
    val name = if (Shared.language == "en") stopName.en else stopName.zh
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(50, instance).sp.clamp(min = 50.dp).dp),
        onClick = handleOpenMaps(lat, lng, name, instance, false, haptic),
        onLongClick = handleOpenMaps(lat, lng, name, instance, true, haptic),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .height(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(17F, instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_map_24),
                        tint = Color(0xFF4CFF00),
                        contentDescription = if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = StringUtils.scaledSize(14F, instance).sp,
                    text = if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置"
                )
            }
        }
    )
}

fun handleOpenMaps(lat: Double, lng: Double, label: String, instance: EtaMenuActivity, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
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

@Composable
fun OpenOpenKmbBbiMapButton(kmbBbiId: String, instance: EtaMenuActivity) {
    val haptic = LocalHapticFeedback.current
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(50, instance).sp.clamp(min = 50.dp).dp),
        onClick = handleOpenKmbBbiMap(kmbBbiId, instance, false, haptic),
        onLongClick = handleOpenKmbBbiMap(kmbBbiId, instance, true, haptic),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .height(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(17F, instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_transfer_within_a_station_24),
                        tint = Color(0xFFFF0000),
                        contentDescription = if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = StringUtils.scaledSize(14F, instance).sp,
                    text = if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"
                )
            }
        }
    )
}

fun handleOpenKmbBbiMap(kmbBbiId: String, instance: EtaMenuActivity, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
    return {
        val url = "https://app.kmb.hk/app1933/BBI/map/".plus(kmbBbiId).plus(".jpg")
        val phoneIntent = Intent(instance, URLImageActivity::class.java)
        phoneIntent.putExtra("url", url)
        if (longClick) {
            instance.startActivity(phoneIntent)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(url))
            RemoteActivityUtils.intentToPhone(
                instance = instance,
                intent = intent,
                noPhone = {
                    instance.startActivity(phoneIntent)
                },
                failed = {
                    instance.startActivity(phoneIntent)
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

@Composable
fun Title(index: Int, stopName: BilingualText, routeNumber: String, co: Operator, instance: EtaMenuActivity) {
    val name = if (Shared.language == "en") stopName.en else stopName.zh
    AutoResizeText (
        modifier = Modifier
            .fillMaxWidth()
            .padding(45.dp, 0.dp),
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

@Composable
fun SubTitle(destName: BilingualText, routeNumber: String, co: Operator, instance: EtaMenuActivity) {
    val name = co.getDisplayRouteNumber(routeNumber).plus(" ").plus(destName[Shared.language])
    AutoResizeText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
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

@Composable
fun FavHeader(instance: EtaMenuActivity) {
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(14F, instance).sp.clamp(max = 14.dp),
        text = if (Shared.language == "en") "Set Favourite Routes" else "設置最喜愛路線"
    )
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(10F, instance).sp.clamp(max = 10.dp),
        text = if (Shared.language == "en") {
            "Click below to set/clear this route stop from the corresponding indexed favourite route"
        } else {
            "點擊可設置/清除對應的最喜愛路線"
        }
    )
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(10F, instance).sp.clamp(max = 10.dp),
        text = if (Shared.language == "en") {
            "Route stops can be used in Tiles"
        } else {
            "最喜愛路線可在資訊方塊中顯示"
        }
    )
}

@Composable
fun NextFavButton(scrollTo: MutableIntState, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: EtaMenuActivity) {
    Button(
        onClick = {
            scrollTo.intValue = IntStream.rangeClosed(1, 30).filter {
                val favState = getFavState(it, stopId, co, index, stop, route, instance)
                favState == FavouriteRouteState.NOT_USED || favState == FavouriteRouteState.USED_SELF
            }.min().orElse(30)
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(50, instance).dp)
            .height(StringUtils.scaledSize(30, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Icon(
                modifier = Modifier
                    .padding(3.dp, 3.dp)
                    .size(StringUtils.scaledSize(25F, instance).sp.dp),
                imageVector = Icons.Filled.KeyboardArrowDown,
                tint = Color(0xFFFFB700),
                contentDescription = if (Shared.language == "en") "Down" else "向下"
            )
        }
    )
}

fun getFavState(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: EtaMenuActivity): FavouriteRouteState {
    val registry = Registry.getInstance(instance)
    if (registry.hasFavouriteRouteStop(favoriteIndex)) {
        return if (registry.isFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route)) FavouriteRouteState.USED_SELF else FavouriteRouteState.USED_OTHER
    }
    return FavouriteRouteState.NOT_USED
}

@Composable
fun FavButton(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: EtaMenuActivity) {
    var state by remember { mutableStateOf(getFavState(favoriteIndex, stopId, co, index, stop, route, instance)) }

    RestartEffect {
        val newState = getFavState(favoriteIndex, stopId, co, index, stop, route, instance)
        if (newState != state) {
            state = newState
        }
    }

    val haptic = LocalHapticFeedback.current
    AdvanceButton(
        onClick = {
            if (state == FavouriteRouteState.USED_SELF) {
                Registry.getInstance(instance).clearFavouriteRouteStop(favoriteIndex, instance)
                Toast.makeText(instance, if (Shared.language == "en") "Cleared Favourite Route ".plus(favoriteIndex) else "已清除最喜愛路線".plus(favoriteIndex), Toast.LENGTH_SHORT).show()
            } else {
                Registry.getInstance(instance).setFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route, instance)
                Toast.makeText(instance, if (Shared.language == "en") "Set Favourite Route ".plus(favoriteIndex) else "已設置最喜愛路線".plus(favoriteIndex), Toast.LENGTH_SHORT).show()
            }
            state = getFavState(favoriteIndex, stopId, co, index, stop, route, instance)
        },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val intent = Intent(instance, FavActivity::class.java)
            intent.putExtra("scrollToIndex", favoriteIndex)
            instance.startActivity(intent)
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(50, instance).sp.clamp(min = 50.dp).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = when (state) {
                FavouriteRouteState.NOT_USED -> Color(0xFF444444)
                FavouriteRouteState.USED_OTHER -> Color(0xFF4E4E00)
                FavouriteRouteState.USED_SELF -> Color(0xFFFFFF00)
            }
        ),
        content = {
            Row(
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .height(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(if (state == FavouriteRouteState.USED_SELF) Color(0xFF3D3D3D) else Color(0xFF131313))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = StringUtils.scaledSize(17F, instance).sp,
                        color = when (state) {
                            FavouriteRouteState.NOT_USED -> Color(0xFF444444)
                            FavouriteRouteState.USED_OTHER -> Color(0xFF4E4E00)
                            FavouriteRouteState.USED_SELF -> Color(0xFFFFFF00)
                        },
                        text = favoriteIndex.toString()
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = if (state == FavouriteRouteState.USED_SELF) Color(0xFFFFFFFF) else Color(0xFF3F3F3F),
                    fontSize = StringUtils.scaledSize(14F, instance).sp,
                    text = when (state) {
                        FavouriteRouteState.NOT_USED -> if (Shared.language == "en") "No Route Stop Selected" else "未有設置路線巴士站"
                        FavouriteRouteState.USED_OTHER -> if (Shared.language == "en") "Selected by Another Route Stop" else "已設置為另一路線巴士站"
                        FavouriteRouteState.USED_SELF -> if (Shared.language == "en") "Selected as This Route Stop" else "已設置為本路線巴士站"
                    }
                )
            }
        }
    )
}