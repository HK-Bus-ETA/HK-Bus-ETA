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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.FavouriteRouteState
import com.loohp.hkbuseta.common.objects.FavouriteStopMode
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.TileUseState
import com.loohp.hkbuseta.common.shared.Tiles
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.services.AlightReminderService
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.checkBackgroundLocationPermission
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.checkNotificationPermission
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.sp


@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun EtaMenuElement(stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: AppActiveContext) {
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val scroll = rememberLazyListState()

        val maxFavItems by Shared.suggestedMaxFavouriteRouteStopState.collectAsStateWithLifecycle()

        val routeNumber = route.routeNumber
        val lat = stop.location.lat
        val lng = stop.location.lng

        val scrollTo = remember { mutableIntStateOf(0) }

        LaunchedEffect (scrollTo.intValue) {
            val value = scrollTo.intValue
            if (value > 0) {
                scroll.animateScrollToItem(value + 8, -instance.screenHeight / 3)
                scrollTo.intValue = 0
            }
        }

        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalLazyScrollbar(
                    state = scroll,
                    context = instance
                )
                .rotaryScroll(scroll, focusRequester),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = scroll
        ) {
            item {
                Spacer(modifier = Modifier.size(20.scaledSize(instance).dp))
            }
            item {
                Title(index, stop.name, stop.remark, routeNumber, co, instance)
                SubTitle(Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route, true), routeNumber, co, instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                MoreInfoHeader(instance)
            }
            item {
                stop.kmbBbiId?.let {
                    Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                    OpenOpenKmbBbiMapButton(it, instance)
                }
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                SearchNearbyButton(stop, route, instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                AlightReminderButton(stopId, index, stop, route, co, instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                OpenOnMapsButton(stop.name, lat, lng, instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                FavHeader(instance)
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                NextFavButton(scrollTo, stopId, co, index, stop, route, instance)
            }
            items(maxFavItems) {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                FavButton(it + 1, stopId, co, index, stop, route, instance)
            }
            item {
                Spacer(modifier = Modifier.size(40.scaledSize(instance).dp))
            }
        }
    }
}

@Composable
fun MoreInfoHeader(instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
        text = if (Shared.language == "en") "More Info & Actions" else "更多資訊及功能"
    )
}

@Composable
fun SearchNearbyButton(stop: Stop, route: Route, instance: AppActiveContext) {
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .heightIn(min = 50.scaledSize(instance).sp.dp),
        shape = RoundedCornerShape(25.scaledSize(instance).dp),
        onClick = {
            instance.runOnUiThread {
                val text = if (Shared.language == "en") {
                    "Nearby Interchange Routes of ".plus(stop.name.en)
                } else {
                    "".plus(stop.name.zh).plus(" 附近轉乘路線")
                }
                instance.showToastText(text, ToastDuration.LONG)
            }
            val intent = AppIntent(instance, AppScreen.NEARBY)
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
            Image(
                modifier = Modifier.matchParentSize(),
                painter = painterResource(R.mipmap.interchange_background),
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(Color(0xC1000000), BlendMode.Multiply),
                contentDescription = null
            )
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .height(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(17F.scaledSize(instance).sp.dp),
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
                    fontSize = 14F.scaledSize(instance).sp,
                    text = if (Shared.language == "en") "Find Nearby Interchanges" else "尋找附近轉乘路線"
                )
            }
        }
    )
}

@Composable
fun AlightReminderButton(stopId: String, index: Int, stop: Stop, route: Route, co: Operator, instance: AppActiveContext) {
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .heightIn(min = 50.scaledSize(instance).sp.dp),
        shape = RoundedCornerShape(25.scaledSize(instance).dp),
        onClick = {
            checkLocationPermission(instance) { locationGranted ->
                if (locationGranted) {
                    checkNotificationPermission(instance) { notificationGranted ->
                        if (notificationGranted) {
                            Registry.getInstance(instance).findRoutes(route.routeNumber, true) { it -> it == route }.first().let {
                                val intent = AppIntent(instance, AppScreen.ALIGHT_REMINDER_SERVICE)
                                intent.putExtra("stop", stop.serialize().toString())
                                intent.putExtra("route", route.serialize().toString())
                                intent.putExtra("index", index)
                                intent.putExtra("co", co.name)

                                val stopListIntent = AppIntent(instance, AppScreen.LIST_STOPS)
                                stopListIntent.putExtra("route", it.toByteArray())
                                stopListIntent.putExtra("scrollToStop", stopId)
                                stopListIntent.putExtra("showEta", false)
                                stopListIntent.putExtra("isAlightReminder", true)

                                val noticeIntent = AppIntent(instance, AppScreen.DISMISSIBLE_TEXT_DISPLAY)
                                noticeIntent.putExtra("specialTextIndex", 0)
                                instance.startActivity(noticeIntent) { result ->
                                    if (result.resultCode == 1) {
                                        AlightReminderService.terminate()
                                        instance.startForegroundService(intent)
                                        instance.startActivity(stopListIntent)
                                    }
                                }
                            }
                        } else {
                            instance.showToastText(if (Shared.language == "en") "Notification Permission Denied" else "推送通知權限被拒絕", ToastDuration.SHORT)
                        }
                    }
                } else {
                    instance.showToastText(if (Shared.language == "en") "Location Access Permission Denied" else "位置存取權限被拒絕", ToastDuration.SHORT)
                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Image(
                modifier = Modifier.matchParentSize(),
                painter = painterResource(R.mipmap.alight_reminder_background),
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(Color(0xC1000000), BlendMode.Multiply),
                contentDescription = null
            )
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .height(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(17F.scaledSize(instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_notifications_active_24),
                        tint = Color(0xFFFF9800),
                        contentDescription = if (Shared.language == "en") "Enable Alight Reminder" else "開啟落車提示"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = 14F.scaledSize(instance).sp,
                    maxLines = 2,
                    text = buildAnnotatedString {
                        append(if (Shared.language == "en") "Enable Alight Reminder " else "開啟落車提示\n", SpanStyle(fontWeight = FontWeight.Bold))
                        append(if (Shared.language == "en") "Beta" else "測試版", SpanStyle(fontSize = TextUnit.Small, fontWeight = FontWeight.Normal))
                    }
                )
            }
        }
    )
}

@Composable
fun OpenOnMapsButton(stopName: BilingualText, lat: Double, lng: Double, instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    val name = stopName[Shared.language]

    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .heightIn(min = 50.scaledSize(instance).sp.dp),
        shape = RoundedCornerShape(25.scaledSize(instance).dp),
        onClick = instance.handleOpenMaps(lat, lng, name, false, haptic.common),
        onLongClick = instance.handleOpenMaps(lat, lng, name, true, haptic.common),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Image(
                modifier = Modifier.matchParentSize(),
                painter = painterResource(R.mipmap.open_map_background),
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(Color(0xC1000000), BlendMode.Multiply),
                contentDescription = null
            )
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .height(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(17F.scaledSize(instance).sp.dp),
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
                    fontSize = 14F.scaledSize(instance).sp,
                    text = if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置"
                )
            }
        }
    )
}

@Composable
fun OpenOpenKmbBbiMapButton(kmbBbiId: String, instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current

    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .heightIn(min = 50.scaledSize(instance).sp.dp),
        shape = RoundedCornerShape(25.scaledSize(instance).dp),
        onClick = instance.handleWebImages("https://app.kmb.hk/app1933/BBI/map/$kmbBbiId.jpg", false, haptic.common),
        onLongClick = instance.handleWebImages("https://app.kmb.hk/app1933/BBI/map/$kmbBbiId.jpg", true, haptic.common),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Image(
                modifier = Modifier.matchParentSize(),
                painter = painterResource(R.mipmap.kmb_bbi_background),
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(Color(0xC1000000), BlendMode.Multiply),
                contentDescription = null
            )
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .height(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(17F.scaledSize(instance).sp.dp),
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
                    fontSize = 14F.scaledSize(instance).sp,
                    text = if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"
                )
            }
        }
    )
}

@Composable
fun Title(index: Int, stopName: BilingualText, stopRemark: BilingualText?, routeNumber: String, co: Operator, instance: AppActiveContext) {
    val name = stopName[Shared.language]
    AutoResizeText (
        modifier = Modifier
            .fillMaxWidth()
            .padding(45.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = if (co == Operator.MTR) name else index.toString().plus(". ").plus(name),
        maxLines = 2,
        fontWeight = FontWeight.Bold,
        fontSizeRange = FontSizeRange(
            min = 1F.scaledSize(instance).dp.sp,
            max = 17F.scaledSize(instance).sp.clamp(max = 17F.scaledSize(instance).dp)
        )
    )
    if (stopRemark != null) {
        AutoResizeText (
            modifier = Modifier
                .fillMaxWidth()
                .padding(35.dp, 0.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            text = stopRemark[Shared.language],
            maxLines = 1,
            fontSizeRange = FontSizeRange(
                min = 1F.scaledSize(instance).dp.sp,
                max = 12F.scaledSize(instance).sp.clamp(max = 12F.scaledSize(instance).dp)
            )
        )
    }
}

@Composable
fun SubTitle(destName: BilingualText, routeNumber: String, co: Operator, instance: AppActiveContext) {
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
            min = 1F.scaledSize(instance).dp.sp,
            max = 11F.scaledSize(instance).sp.clamp(max = 11F.scaledSize(instance).dp)
        )
    )
}

@Composable
fun FavHeader(instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
        text = if (Shared.language == "en") "Set Favourite Routes" else "設置最喜愛路線"
    )
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 10F.scaledSize(instance).sp.clamp(max = 10.dp),
        text = if (Shared.language == "en") {
            "Section to set/clear this route stop from the corresponding indexed favourite route"
        } else {
            "以下可設置/清除對應的最喜愛路線"
        }
    )
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 10F.scaledSize(instance).sp.clamp(max = 10.dp),
        text = if (Shared.language == "en") {
            "Route stops can be used in Tiles"
        } else {
            "最喜愛路線可在資訊方塊中顯示"
        }
    )
    Spacer(modifier = Modifier.size(5.scaledSize(instance).dp))
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 10F.scaledSize(instance).sp.clamp(max = 10.dp),
        fontWeight = FontWeight.Bold,
        text = if (Shared.language == "en") {
            "Tap to set this stop\nLong press to set to display any closes stop of the route"
        } else {
            "點擊設置此站 長按設置顯示路線最近的任何站"
        }
    )
}

@Composable
fun NextFavButton(scrollTo: MutableIntState, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: AppActiveContext) {
    Button(
        onClick = {
            scrollTo.intValue = (1..30).minByOrNull {
                val favState = getFavState(it, stopId, co, index, stop, route, instance)
                favState == FavouriteRouteState.NOT_USED || favState == FavouriteRouteState.USED_SELF
            }?: 30
        },
        modifier = Modifier
            .width(50.scaledSize(instance).dp)
            .height(30.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Icon(
                modifier = Modifier
                    .padding(3.dp, 3.dp)
                    .size(25F.scaledSize(instance).sp.dp),
                imageVector = Icons.Filled.KeyboardArrowDown,
                tint = Color(0xFFFFB700),
                contentDescription = if (Shared.language == "en") "Down" else "向下"
            )
        }
    )
}

fun getFavState(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: AppActiveContext): FavouriteRouteState {
    val registry = Registry.getInstance(instance)
    if (registry.hasFavouriteRouteStop(favoriteIndex)) {
        return if (registry.isFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route)) FavouriteRouteState.USED_SELF else FavouriteRouteState.USED_OTHER
    }
    return FavouriteRouteState.NOT_USED
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavButton(favoriteIndex: Int, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: AppActiveContext) {
    var state by remember { mutableStateOf(getFavState(favoriteIndex, stopId, co, index, stop, route, instance)) }
    var anyTileUses by remember { mutableStateOf(Tiles.getTileUseState(favoriteIndex)) }

    RestartEffect {
        val newState = getFavState(favoriteIndex, stopId, co, index, stop, route, instance)
        if (newState != state) {
            state = newState
        }
        val newAnyTileUses = Tiles.getTileUseState(favoriteIndex)
        if (newAnyTileUses != anyTileUses) {
            anyTileUses = newAnyTileUses
        }
    }

    val handleClick0: (FavouriteStopMode) -> Unit = {
        if (state == FavouriteRouteState.USED_SELF) {
            Registry.getInstance(instance).clearFavouriteRouteStop(favoriteIndex, instance)
            instance.showToastText(if (Shared.language == "en") "Cleared Favourite Route ".plus(favoriteIndex) else "已清除最喜愛路線".plus(favoriteIndex), ToastDuration.SHORT)
        } else {
            Registry.getInstance(instance).setFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route, it, instance)
            instance.showToastText(if (Shared.language == "en") "Set Favourite Route ".plus(favoriteIndex) else "已設置最喜愛路線".plus(favoriteIndex), ToastDuration.SHORT)
        }
        state = getFavState(favoriteIndex, stopId, co, index, stop, route, instance)
        anyTileUses = Tiles.getTileUseState(favoriteIndex)
    }

    val handleClick: (FavouriteStopMode) -> Unit = {
        if (it.isRequiresLocation && state != FavouriteRouteState.USED_SELF && !checkBackgroundLocationPermission(instance, false)) {
            val noticeIntent = AppIntent(instance, AppScreen.DISMISSIBLE_TEXT_DISPLAY)
            val notice = BilingualText(
                "<b>設置路線任何站為最喜愛路線</b>需要在<b>背景存取定位位置的權限</b><br>" +
                        "以搜尋你<b>目前所在的地點最近的巴士站</b><br>" +
                        "<br>" +
                        "包括在程式未被打開時(用於資訊方塊中)<br>" +
                        "程式不會儲存或發送位置數據<br>" +
                        "<br>" +
                        "如出現權限請求 請分別選擇「<b>僅限使用應用程式時</b>」和「<b>一律允許</b>」",
                "<b>Setting any stop on route as favourite route</b> requires the <b>background location permission</b>.<br>" +
                        "It is used to <b>search for the closest stop to you</b>, even when the app is closed or not in use (when you look up ETA using Tiles), no location data are stored or sent.<br>" +
                        "<br>" +
                        "If prompted, please choose \"<b>While using this app</b>\" and then \"<b>All the time</b>\"."
            )
            noticeIntent.putExtra("text", notice.toByteArray())
            instance.startActivity(noticeIntent) { confirm ->
                if (confirm.resultCode == 1) {
                    checkBackgroundLocationPermission(instance) { result ->
                        if (result) {
                            handleClick0.invoke(it)
                        } else {
                            instance.showToastText(if (Shared.language == "en") "Background Location Access Permission Denied" else "背景位置存取權限被拒絕", ToastDuration.SHORT)
                        }
                    }
                }
            }
        } else {
            handleClick0.invoke(it)
        }
    }

    val shape = RoundedCornerShape(25.scaledSize(instance).dp)
    val haptic = LocalHapticFeedback.current
    AdvanceButton(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .heightIn(min = 50.scaledSize(instance).sp.dp)
            .composed {
                when (anyTileUses) {
                    TileUseState.PRIMARY -> this.border(2.sp.dp, Color(0x5437FF00), shape)
                    TileUseState.SECONDARY -> this.border(2.sp.dp, Color(0x54FFB700), shape)
                    TileUseState.NONE -> this
                }
            },
        shape = shape,
        onClick = {
            handleClick.invoke(FavouriteStopMode.FIXED)
        },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            handleClick.invoke(FavouriteStopMode.CLOSEST)
        },
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
                        .width(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .height(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(if (state == FavouriteRouteState.USED_SELF) Color(0xFF3D3D3D) else Color(0xFF131313))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 17F.scaledSize(instance).sp,
                        color = when (state) {
                            FavouriteRouteState.NOT_USED -> Color(0xFFFFFFFF)
                            FavouriteRouteState.USED_OTHER -> Color(0xFF4E4E00)
                            FavouriteRouteState.USED_SELF -> Color(0xFFFFFF00)
                        },
                        text = favoriteIndex.toString()
                    )
                }
                when (state) {
                    FavouriteRouteState.NOT_USED -> {
                        Text(
                            modifier = Modifier
                                .padding(0.dp, 0.dp, 5.dp, 0.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            color = Color(0xFFB9B9B9),
                            fontSize = 14F.scaledSize(instance).sp,
                            text = if (Shared.language == "en") "No Route Stop Selected" else "未有設置路線巴士站"
                        )
                    }
                    FavouriteRouteState.USED_OTHER -> {
                        val currentRoute = Shared.favoriteRouteStops[favoriteIndex]!!
                        val kmbCtbJoint = currentRoute.route.isKmbCtbJoint
                        val coDisplay = currentRoute.co.getDisplayName(currentRoute.route.routeNumber, kmbCtbJoint, Shared.language)
                        val routeNumberDisplay = currentRoute.co.getDisplayRouteNumber(currentRoute.route.routeNumber)
                        val stopName = if (currentRoute.favouriteStopMode == FavouriteStopMode.FIXED) {
                            if (Shared.language == "en") {
                                (if (currentRoute.co == Operator.MTR || currentRoute.co == Operator.LRT) "" else index.toString().plus(". ")).plus(currentRoute.stop.name.en)
                            } else {
                                (if (currentRoute.co == Operator.MTR || currentRoute.co == Operator.LRT) "" else index.toString().plus(". ")).plus(currentRoute.stop.name.zh)
                            }
                        } else {
                            if (Shared.language == "en") "Any" else "任何站"
                        }
                        val rawColor = currentRoute.co.getColor(currentRoute.route.routeNumber, Color.White)
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
                        Column(
                            modifier = Modifier
                                .padding(0.dp, 0.dp, 5.dp, 0.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                modifier = Modifier
                                    .basicMarquee(Int.MAX_VALUE)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                color = color.adjustBrightness(0.3F),
                                fontSize = 14F.scaledSize(instance).sp,
                                fontWeight = FontWeight.Bold,
                                text = "$coDisplay $routeNumberDisplay"
                            )
                            Text(
                                modifier = Modifier
                                    .basicMarquee(Int.MAX_VALUE)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFFFFFFFF).adjustBrightness(0.3F),
                                fontSize = 14F.scaledSize(instance).sp,
                                text = stopName
                            )
                        }
                    }
                    FavouriteRouteState.USED_SELF -> {
                        val isClosestStopMode = Shared.favoriteRouteStops[favoriteIndex]?.favouriteStopMode == FavouriteStopMode.CLOSEST
                        Text(
                            modifier = Modifier
                                .padding(0.dp, 0.dp, 5.dp, 0.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            color = if (isClosestStopMode) Color(0xFFFFE496) else Color(0xFFFFFFFF),
                            fontSize = 14F.scaledSize(instance).sp,
                            text = if (isClosestStopMode) {
                                if (Shared.language == "en") "Selected as Any Closes Stop on This Route" else "已設置為本路線最近的任何巴士站"
                            } else {
                                if (Shared.language == "en") "Selected as This Route Stop" else "已設置為本路線巴士站"
                            }
                        )
                    }
                }
            }
        }
    )
}