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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.FavouriteRouteGroup
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.FavouriteStopMode
import com.loohp.hkbuseta.common.objects.HKBusAppStopInfo
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.add
import com.loohp.hkbuseta.common.objects.findSame
import com.loohp.hkbuseta.common.objects.getByName
import com.loohp.hkbuseta.common.objects.getDeepLink
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getHKBusAppLink
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.indexOfName
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.remove
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.objects.resolvedDestWithBranch
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.currentBranchStatus
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.indexOf
import com.loohp.hkbuseta.common.utils.indexesOf
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.absoluteValue


@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun EtaMenuElement(stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: AppActiveContext) {
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val scroll = rememberLazyListState()

        val favouriteRouteStops by Shared.favoriteRouteStops.collectAsStateWithLifecycle()
        val selectedGroupState = remember { mutableStateOf(favouriteRouteStops.first().name) }

        val routeNumber = route.routeNumber
        val lat = stop.location.lat
        val lng = stop.location.lng

        val stopList = remember { Registry.getInstance(instance).getAllStops(routeNumber, route.idBound(co), co, route.gmbRegion).asImmutableList() }
        val stopData = remember { stopList.getOrNull(stopList.indexesOf { it.stopId == stopId }.minByOrNull { (it - index).absoluteValue }?: -1) }
        val branches = remember { Registry.getInstance(instance).getAllBranchRoutes(routeNumber, route.idBound(co), co, route.gmbRegion) }
        val currentBranch = remember { branches.currentBranchStatus(currentLocalDateTime(), instance, false).asSequence().sortedByDescending { it.value.activeness }.first().key }
        val resolvedDestName = remember {
            if (co.isTrain) {
                Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route, true)
            } else if (stopData?.branchIds?.contains(currentBranch) != false) {
                route.resolvedDestWithBranch(true, currentBranch, index, stopId, instance)
            } else {
                route.resolvedDest(true)
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
                SubTitle(resolvedDestName, routeNumber, co, instance)
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
                OpenOnMapsButton(stop.name, lat, lng, instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                OpenOnHKBusAppButton(route, stopId, index, instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                OpenMobileAppButton(route, stopId, index, instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                FavHeader(instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                SwitchGroupButton(favouriteRouteStops, selectedGroupState, instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                AddThisStopFavButton(favouriteRouteStops.asImmutableList(), selectedGroupState.value, stopId, co, index, stop, route, instance)
            }
            item {
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                AddAnyStopFavButton(favouriteRouteStops.asImmutableList(), selectedGroupState.value, stopId, co, index, stop, route, instance)
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
fun OpenMobileAppButton(route: Route, stopId: String, index: Int, instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .heightIn(min = 50.scaledSize(instance).sp.dp),
        shape = RoundedCornerShape(25.scaledSize(instance).dp),
        onClick = instance.handleWebpages(route.getDeepLink(instance, stopId, index), false, haptic.common),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Image(
                modifier = Modifier.matchParentSize(),
                painter = painterResource(R.mipmap.open_mobile_background),
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
                        painter = painterResource(R.drawable.baseline_phone_android_24),
                        tint = Color(0xFFFF0000),
                        contentDescription = if (Shared.language == "en") "Open on Mobile" else "在手機上開啟"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = 14F.scaledSize(instance).sp,
                    text = if (Shared.language == "en") "Open on Mobile" else "在手機上開啟"
                )
            }
        }
    )
}

@Composable
fun OpenOnHKBusAppButton(route: Route, stopId: String, index: Int, instance: AppActiveContext) {
    val haptics = LocalHapticFeedback.current
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .heightIn(min = 50.scaledSize(instance).sp.dp),
        shape = RoundedCornerShape(25.scaledSize(instance).dp),
        onClick = instance.handleWebpages(route.getHKBusAppLink(instance, HKBusAppStopInfo(stopId, index)), false, haptics.common),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Image(
                modifier = Modifier.matchParentSize(),
                painter = painterResource(R.mipmap.hkbusapp_background),
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
                        painter = painterResource(R.drawable.baseline_phone_android_24),
                        tint = Color(0xFFFFE15E),
                        contentDescription = if (Shared.language == "en") "Open on hkbus.app" else "在hkbus.app上開啟"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = 14F.scaledSize(instance).sp,
                    text = if (Shared.language == "en") "Open on hkbus.app" else "在hkbus.app上開啟"
                )
            }
        }
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
            val intent = AppIntent(instance, AppScreen.NEARBY)
            intent.putExtra("interchangeSearch", true)
            intent.putExtra("lat", stop.location.lat)
            intent.putExtra("lng", stop.location.lng)
            intent.putExtra("exclude", listOf(element = route.routeNumber))
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
            max = 17F.scaledSize(instance).sp
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
                max = 12F.scaledSize(instance).sp
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
            max = 11F.scaledSize(instance).sp
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
        fontSize = 14F.scaledSize(instance).sp,
        text = if (Shared.language == "en") "Set Favourite Routes" else "設置最喜愛路線"
    )
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 10F.scaledSize(instance).sp,
        text = if (Shared.language == "en") {
            "Route stops can be used in Tiles"
        } else {
            "最喜愛路線可在資訊方塊中顯示"
        }
    )
}

@Composable
fun SwitchGroupButton(groups: List<FavouriteRouteGroup>, selectedState: MutableState<BilingualText>, instance: AppActiveContext) {
    Button(
        onClick = {
            val current = selectedState.value
            val index = groups.indexOf { it.name == current }
            selectedState.value = (if (index + 1 >= groups.size) groups.first() else groups[index + 1]).name
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(0.8F)
            .height(35.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color.White
        ),
        content = {
            Text(
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .userMarquee(),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                text = selectedState.value[Shared.language]
            )
        }
    )
}

@Composable
fun AddThisStopFavButton(favourites: ImmutableList<FavouriteRouteGroup>, groupName: BilingualText, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: AppActiveContext) {
    val alreadySet by remember(favourites) { derivedStateOf { favourites.getByName(groupName)!!.findSame(stopId, co, index, stop, route).any { it.favouriteStopMode == FavouriteStopMode.FIXED } } }
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .heightIn(min = 50.scaledSize(instance).sp.dp),
        shape = RoundedCornerShape(25.scaledSize(instance).dp),
        onClick = {
            val fav = FavouriteRouteStop(stopId, co, index, stop, route, FavouriteStopMode.FIXED)
            val updated = Shared.favoriteRouteStops.value.toMutableList().apply {
                val groupIndex = indexOfName(groupName)
                set(groupIndex, get(groupIndex).add(fav))
            }
            Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        enabled = !alreadySet,
        content = {
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
                        imageVector = Icons.Filled.Star,
                        tint = Color(0xFFFFFF00),
                        contentDescription = if (Shared.language == "en") "Add This Route Stop" else "設置本路線巴士站"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary.adjustBrightness(if (alreadySet) 0.5F else 1F),
                    fontSize = 14F.scaledSize(instance).sp,
                    text = if (alreadySet) {
                        if (Shared.language == "en") "Added This Route Stop" else "已設置本路線巴士站"
                    } else {
                        if (Shared.language == "en") "Add This Route Stop" else "設置本路線巴士站"
                    }
                )
            }
        }
    )
}

@Composable
fun AddAnyStopFavButton(favourites: ImmutableList<FavouriteRouteGroup>, groupName: BilingualText, stopId: String, co: Operator, index: Int, stop: Stop, route: Route, instance: AppActiveContext) {
    val alreadySet by remember(favourites) { derivedStateOf { favourites.getByName(groupName)!!.findSame(stopId, co, index, stop, route).any { it.favouriteStopMode == FavouriteStopMode.CLOSEST } } }
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(220.scaledSize(instance).dp)
            .heightIn(min = 50.scaledSize(instance).sp.dp),
        shape = RoundedCornerShape(25.scaledSize(instance).dp),
        onClick = {
            val fav = FavouriteRouteStop(stopId, co, index, stop, route, FavouriteStopMode.CLOSEST)
            val updated = Shared.favoriteRouteStops.value.toMutableList().apply {
                val groupIndex = indexOfName(groupName)
                set(groupIndex, get(groupIndex).remove(fav).add(fav))
            }
            Registry.getInstance(instance).setFavouriteRouteGroups(updated, instance)
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        enabled = !alreadySet,
        content = {
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
                        imageVector = Icons.Filled.Star,
                        tint = Color(0xFFFFE496),
                        contentDescription = if (Shared.language == "en") "Add Any Closest Stop on This Route" else "設置本路線最近的任何巴士站"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary.adjustBrightness(if (alreadySet) 0.5F else 1F),
                    fontSize = 14F.scaledSize(instance).sp,
                    text = if (alreadySet) {
                        if (Shared.language == "en") "Added Any Closest Stop on This Route" else "已設置本路線最近的任何巴士站"
                    } else {
                        if (Shared.language == "en") "Add Any Closest Stop on This Route" else "設置本路線最近的任何巴士站"
                    }
                )
            }
        }
    )
}