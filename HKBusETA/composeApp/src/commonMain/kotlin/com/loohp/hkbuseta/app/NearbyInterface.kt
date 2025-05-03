/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.FavouriteStop
import com.loohp.hkbuseta.common.objects.RadiusCenterPosition
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.hasStop
import com.loohp.hkbuseta.common.objects.joinToBilingualText
import com.loohp.hkbuseta.common.objects.removeStop
import com.loohp.hkbuseta.common.objects.toStopIndexed
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.awaitWithTimeout
import com.loohp.hkbuseta.common.utils.formatDecimalSeparator
import com.loohp.hkbuseta.compose.DismissRequestType
import com.loohp.hkbuseta.compose.Forest
import com.loohp.hkbuseta.compose.LocationDisabled
import com.loohp.hkbuseta.compose.Map
import com.loohp.hkbuseta.compose.MyLocation
import com.loohp.hkbuseta.compose.NearMe
import com.loohp.hkbuseta.compose.NearMeDisabled
import com.loohp.hkbuseta.compose.PinDrop
import com.loohp.hkbuseta.compose.PlatformAlertDialog
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformCircularProgressIndicator
import com.loohp.hkbuseta.compose.PlatformFloatingActionButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformOutlinedTextField
import com.loohp.hkbuseta.compose.PlatformSlider
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.Route
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.Search
import com.loohp.hkbuseta.compose.Star
import com.loohp.hkbuseta.compose.StarOutline
import com.loohp.hkbuseta.compose.Sync
import com.loohp.hkbuseta.compose.TransferWithinAStation
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.clickable
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.currentLocalWindowSize
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.compose.platformLargeShape
import com.loohp.hkbuseta.compose.platformTopBarColor
import com.loohp.hkbuseta.compose.verticalScrollBar
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.appendFormatted
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.clearColors
import com.loohp.hkbuseta.utils.coordinatesNullableStateSaver
import com.loohp.hkbuseta.utils.fontScaledDp
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.lastLocation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.roundToInt


enum class LocationPermissionState(
    val waiting: Boolean = false,
    val denied: Boolean = false,
) {

    PENDING(waiting = true),
    PROMPTING(waiting = true),
    NOT_SUPPORTED(denied = true),
    DENIED(denied = true),
    ALLOWED

}

inline val LocationPermissionState.isAllowed get() = this == LocationPermissionState.ALLOWED

private val defaultCustomPosition: RadiusCenterPosition = RadiusCenterPosition(22.2940359, 114.1680971, 0.3F)

private val locationPermissionState: MutableStateFlow<LocationPermissionState> = MutableStateFlow(LocationPermissionState.PENDING)
private val customCenterPositionState: MutableStateFlow<RadiusCenterPosition?> = MutableStateFlow(Shared.lastNearbyLocation)
private val listedRoutesState: MutableStateFlow<ImmutableList<StopIndexedRouteSearchResultEntry>> = MutableStateFlow(persistentListOf())
private val nearbyRoutesResultState: MutableStateFlow<Registry.NearbyRoutesResult?> = MutableStateFlow(null)

@Composable
fun NearbyInterface(instance: AppActiveContext, visible: Boolean) {
    var permissionState by locationPermissionState.collectAsStateMultiplatform()

    SideEffect {
        if (visible && permissionState.waiting) {
            if (!composePlatform.hasLocation) {
                permissionState = LocationPermissionState.NOT_SUPPORTED
            } else if (permissionState != LocationPermissionState.PROMPTING) {
                permissionState = LocationPermissionState.PROMPTING
                checkLocationPermission(instance) {
                    permissionState = if (it) LocationPermissionState.ALLOWED else LocationPermissionState.DENIED
                }
            }
        }
    }

    if (permissionState.waiting) {
        EmptyBackgroundInterfaceProgress(
            instance = instance,
            icon = PlatformIcons.Filled.Sync,
            text = if (Shared.language == "en") "Loading" else "載入中"
        )
    } else {
        NearbyInterfaceBody(instance, visible)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyInterfaceBody(instance: AppActiveContext, visible: Boolean) {
    val permissionState by locationPermissionState.collectAsStateMultiplatform()
    val density = LocalDensity.current.density
    val window = currentLocalWindowSize
    val actualHeight by remember(density, window) { derivedStateOf { window.height / density } }

    var customCenterPosition by customCenterPositionState.collectAsStateMultiplatform()
    var choosingCustomCenterPosition by remember { mutableStateOf(false) }

    var location by rememberSaveable(saver = coordinatesNullableStateSaver) { mutableStateOf(lastLocation?.location) }

    var gpsFailed by rememberSaveable { mutableStateOf(false) }
    var nearbyRoutesResult by nearbyRoutesResultState.collectAsStateMultiplatform()
    var routes by listedRoutesState.collectAsStateMultiplatform()
    val favouriteStops by Shared.favoriteStops.collectAsStateMultiplatform()

    LaunchedEffect (permissionState) {
        if (permissionState.denied) {
            gpsFailed = true
        }
    }
    LaunchedEffect (customCenterPosition, permissionState, visible) {
        if (visible && permissionState.isAllowed) {
            val fastResult = getGPSLocation(instance, LocationPriority.FASTER).awaitWithTimeout(3000)
            if (fastResult?.isSuccess == true) {
                location = fastResult.location!!
            }
            while (true) {
                val result = getGPSLocation(instance).await()
                if (result?.isSuccess == true) {
                    location = result.location!!
                    gpsFailed = false
                } else {
                    gpsFailed = true
                }
                delay(Shared.ETA_UPDATE_INTERVAL.toLong())
            }
        }
    }
    LaunchedEffect (customCenterPosition, location) {
        location?.let { loc ->
            CoroutineScope(Dispatchers.IO).launch {
                if (customCenterPosition == null) {
                    val result = Registry.getInstance(instance).getNearbyRoutes(loc, emptyMap(), false)
                    val stopIndexed = result.result.toStopIndexed(instance).asImmutableList()
                    withContext(Dispatchers.Main) {
                        routes = stopIndexed
                        nearbyRoutesResult = result
                    }
                }
            }
        }
    }
    LaunchedEffect (customCenterPosition) {
        CoroutineScope(Dispatchers.IO).launch {
            customCenterPosition?.let {
                val result = Registry.getInstance(instance).getNearbyRoutes(it, it.radius.toDouble(), emptyMap(), false)
                val stopIndexed = result.result.toStopIndexed(instance).asImmutableList()
                withContext(Dispatchers.Main) {
                    routes = stopIndexed
                    nearbyRoutesResult = result
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (routes.isEmpty()) {
            nearbyRoutesResult?.apply {
                EmptyBackgroundInterface(
                    instance = instance,
                    icon = PlatformIcons.Filled.Forest,
                    text = if (Shared.language == "en") "There are no nearby bus stops" else "附近沒有巴士站",
                    subText = if (Shared.language == "en") {
                        "Nearest Stop: ${closestStop.name.en} (${(closestDistance * 1000).roundToInt().formatDecimalSeparator()}m)"
                    } else {
                        "最近的巴士站: ${closestStop.name.zh} (${(closestDistance * 1000).roundToInt().formatDecimalSeparator()}米)"
                    }
                )
            }?: run {
                if (gpsFailed) {
                    when (permissionState) {
                        LocationPermissionState.PENDING, LocationPermissionState.PROMPTING -> {
                            EmptyBackgroundInterfaceProgress(
                                instance = instance,
                                icon = PlatformIcons.Filled.Sync,
                                text = if (Shared.language == "en") "Loading" else "載入中"
                            )
                        }
                        LocationPermissionState.NOT_SUPPORTED -> {
                            EmptyBackgroundInterface(
                                instance = instance,
                                icon = PlatformIcons.Filled.NearMeDisabled,
                                text = if (Shared.language == "en") "Device does not support location" else "裝置不支援定位位置"
                            )
                        }
                        LocationPermissionState.DENIED -> {
                            EmptyBackgroundInterface(
                                instance = instance,
                                icon = PlatformIcons.Filled.LocationDisabled,
                                text = if (Shared.language == "en") "Location Denied" else "位置權限被拒絕"
                            )
                        }
                        LocationPermissionState.ALLOWED -> {
                            EmptyBackgroundInterface(
                                instance = instance,
                                icon = PlatformIcons.Filled.Forest,
                                text = if (Shared.language == "en") "Unable to read your location" else "無法讀取你的位置",
                                subText = if (Shared.language == "en") "Please check whether your GPS is enabled" else "請檢查你的定位服務是否已開啟"
                            )
                        }
                    }
                } else {
                    EmptyBackgroundInterface(
                        instance = instance,
                        icon = PlatformIcons.Filled.NearMe,
                        text = if (Shared.language == "en") "Locating..." else "正在讀取你的位置..."
                    )
                }
            }
        } else {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(platformTopBarColor)
                        .animateContentSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = nearbyRoutesResult != null,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(durationMillis = 300)
                        ) + expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = tween(durationMillis = 300)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(durationMillis = 300)
                        ) + shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = tween(durationMillis = 300)
                        )
                    ) {
                        val nearbyStops by remember { derivedStateOf { nearbyRoutesResult?.result?.asSequence()
                            ?.filter { r -> r.stopInfo != null }
                            ?.distinct()
                            ?.toList()?: emptyList()
                        } }
                        val nearbyStopNames by remember { derivedStateOf { nearbyStops.asSequence()
                            .distinctBy { it.stopInfo!!.data!!.name }
                            .sortedBy { it.stopInfo!!.distance }
                            .joinToBilingualText(
                                separator = "\n".asBilingualText(),
                                limit = 10,
                                truncated = "\n...".asBilingualText(),
                                transform = { it.stopInfo!!.data!!.name }
                            )
                        } }
                        nearbyRoutesResult?.let {
                            PlatformText(
                                modifier = Modifier
                                    .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                                    .plainTooltip(nearbyStopNames[Shared.language]),
                                color = ButtonDefaults.textButtonColors().contentColor,
                                fontSize = 20.sp,
                                text = buildAnnotatedString {
                                    appendFormatted(it.closestStop.remarkedName[Shared.language])
                                    val extraStopsCount = nearbyStops.size - 1
                                    if (extraStopsCount > 1) {
                                        append(" ")
                                        append("(+$extraStopsCount)", SpanStyle(fontSize = TextUnit.Small))
                                    }
                                    if (customCenterPosition != null) {
                                        append(" ")
                                        append(if (Shared.language == "en") "[Custom]" else "[自訂位置]", SpanStyle(fontSize = TextUnit.Small))
                                    }
                                }
                            )
                        }
                    }
                }
                ListRoutesInterface(
                    instance = instance,
                    routes = if (visible) routes else persistentListOf(),
                    checkSpecialDest = true,
                    listType = RouteListType.NEARBY,
                    showEta = true,
                    recentSort = RecentSortMode.CHOICE,
                    proximitySortOrigin = customCenterPosition?: location,
                    proximitySortOriginIsRealLocation = customCenterPosition == null,
                    showEmptyText = visible,
                    visible = visible,
                    bottomExtraSpace = if (actualHeight > 460) 75.dp else 0.dp,
                    extraActions = {
                        val haptic = LocalHapticFeedback.current
                        nearbyRoutesResult?.let {
                            val stop by remember(it) { derivedStateOf { it.closestStop } }
                            val stopId by remember(it) { derivedStateOf { it.closestStopId } }
                            val favouriteStopAlreadySet by remember(stopId) { derivedStateOf { favouriteStops.hasStop(stopId) } }
                            PlatformButton(
                                modifier = Modifier
                                    .width(45.dp)
                                    .fillMaxHeight()
                                    .plainTooltip(if (Shared.language == "en") "Add to Favourite Stops" else "設置為最喜愛車站"),
                                onClick = {
                                    if (favouriteStopAlreadySet) {
                                        Registry.getInstance(instance).setFavouriteStops(Shared.favoriteStops.value.toMutableList().apply { removeStop(stopId) }, instance)
                                    } else {
                                        Registry.getInstance(instance).setFavouriteStops(Shared.favoriteStops.value.toMutableList().apply { add(FavouriteStop(stopId, stop, null)) }, instance)
                                    }
                                },
                                contentPadding = PaddingValues(10.dp),
                                shape = platformLargeShape,
                                colors = ButtonDefaults.textButtonColors(),
                                content = {
                                    PlatformIcon(
                                        modifier = Modifier.size(23.dp),
                                        painter = if (favouriteStopAlreadySet) PlatformIcons.Filled.Star else PlatformIcons.Outlined.StarOutline,
                                        contentDescription = if (Shared.language == "en") "Add to Favourite Stops" else "設置為最喜愛車站"
                                    )
                                }
                            )
                            PlatformButton(
                                modifier = Modifier
                                    .width(45.dp)
                                    .fillMaxHeight()
                                    .plainTooltip(if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置"),
                                onClick = instance.handleOpenMaps(stop.location, stop.name[Shared.language], false, haptic.common),
                                contentPadding = PaddingValues(10.dp),
                                shape = platformLargeShape,
                                colors = ButtonDefaults.textButtonColors(),
                                content = {
                                    PlatformIcon(
                                        modifier = Modifier.size(23.dp),
                                        painter = PlatformIcons.Filled.Map,
                                        contentDescription = if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置"
                                    )
                                }
                            )
                        }
                        val closestKmbBbiStop by remember { derivedStateOf { nearbyRoutesResult?.result?.asSequence()
                            ?.filter { it.stopInfo?.data?.kmbBbiId != null }
                            ?.minByOrNull { it.stopInfo!!.distance }
                            ?.stopInfo
                        } }
                        closestKmbBbiStop?.let {
                            PlatformButton(
                                modifier = Modifier
                                    .width(45.dp)
                                    .fillMaxHeight()
                                    .plainTooltip(if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"),
                                onClick = instance.handleWebImages("https://app.kmb.hk/app1933/BBI/map/${it.data!!.kmbBbiId}.jpg", false, haptic.common),
                                contentPadding = PaddingValues(10.dp),
                                shape = platformLargeShape,
                                colors = ButtonDefaults.textButtonColors(),
                                content = {
                                    PlatformIcon(
                                        modifier = Modifier.size(23.dp),
                                        painter = PlatformIcons.Filled.TransferWithinAStation,
                                        contentDescription = if (Shared.language == "en") "Open KMB BBI Layout Map" else "顯示九巴轉車站位置圖"
                                    )
                                }
                            )
                        }
                    },
                    onPullToRefresh = if (visible && permissionState.isAllowed && customCenterPosition == null) ({
                        val result = getGPSLocation(instance, LocationPriority.MOST_ACCURATE).await()
                        if (result?.isSuccess == true) {
                            location = result.location!!
                            gpsFailed = false
                        } else {
                            gpsFailed = true
                        }
                        instance.showToastText(if (Shared.language == "en") "Refreshed your current location" else "你的位置已刷新", ToastDuration.SHORT)
                    }) else null
                )
            }
        }
        if (actualHeight > 460) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (Shared.JOURNEY_PLANNER_AVAILABLE) {
                    PlatformFloatingActionButton(
                        modifier = Modifier.plainTooltip(if (Shared.language == "en") "Journey Planner" else "行程規劃"),
                        onClick = {
                            val appIntent = AppIntent(instance, AppScreen.JOURNEY_PLANNER)
                            val origin: Coordinates? = customCenterPosition?: location
                            if (origin != null) {
                                appIntent.putExtra("origin", origin)
                            }
                            instance.startActivity(appIntent)
                        },
                    ) {
                        PlatformIcon(
                            modifier = Modifier.size(27.dp),
                            painter = PlatformIcons.Outlined.Route,
                            contentDescription = if (Shared.language == "en") "Journey Planner" else "行程規劃"
                        )
                    }
                }
                PlatformFloatingActionButton(
                    modifier = Modifier.plainTooltip(if (Shared.language == "en") "Custom Location" else "自訂位置"),
                    onClick = { choosingCustomCenterPosition = true },
                ) {
                    PlatformIcon(
                        modifier = Modifier.size(27.dp),
                        painter = if (customCenterPosition == null) PlatformIcons.Outlined.PinDrop else PlatformIcons.Filled.PinDrop,
                        contentDescription = if (Shared.language == "en") "Custom Location" else "自訂位置"
                    )
                }
            }
        }
    }
    if (choosingCustomCenterPosition && actualHeight > 460) {
        var initialPosition by remember { mutableStateOf(customCenterPosition?: location?.let { RadiusCenterPosition(it.lat, it.lng, 0.3F) }?: defaultCustomPosition) }
        var position by remember { mutableStateOf(initialPosition) }
        var pxPerKm by remember { mutableFloatStateOf(1F) }
        var stopsInRange by remember { mutableStateOf(emptyList<Registry.NearbyStopSearchResult>()) }
        var nearestStop: Registry.NearbyStopSearchResult? by remember { mutableStateOf(null) }
        var searchingLocation by remember { mutableStateOf(false) }
        var textInput by remember { mutableStateOf(TextFieldValue()) }
        var locations by remember { mutableStateOf(emptyList<Registry.LocationSearchEntry>()) }
        val locationListScroll = rememberLazyListState()
        var updating by remember { mutableStateOf(false) }

        LaunchedEffect (position) {
            CoroutineScope(Dispatchers.IO).launch {
                val stopsInRangeAsync = Registry.getInstance(instance).findNearbyStops(position, position.radius.toDouble())
                val nearestStopAsync = Registry.getInstance(instance).findNearestStops(position)
                withContext(Dispatchers.Main) {
                    stopsInRange = stopsInRangeAsync
                    nearestStop = nearestStopAsync
                }
            }
        }

        PlatformAlertDialog(
            iosSheetStyle = true,
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    PlatformText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .applyIf(composePlatform.applePlatform, { padding(horizontal = 50.dp) }, { padding(end = 50.dp) }),
                        textAlign = if (composePlatform.applePlatform) TextAlign.Center else TextAlign.Start,
                        text = if (Shared.language == "en") "Custom Location" else "自訂位置"
                    )
                    PlatformButton(
                        modifier = Modifier
                            .size(45.dp)
                            .align(Alignment.CenterEnd)
                            .plainTooltip(when {
                                updating -> if (Shared.language == "en") "Loading..." else "載入中..."
                                searchingLocation -> if (Shared.language == "en") "Show Map" else "顯示地圖"
                                else -> if (Shared.language == "en") "Search Location" else "搜尋位置"
                            }),
                        colors = ButtonDefaults.clearColors(),
                        contentPadding = PaddingValues(0.dp),
                        onClick = { searchingLocation = !searchingLocation },
                        enabled = !updating,
                        content = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    updating -> PlatformCircularProgressIndicator(
                                        modifier = Modifier.padding(12.dp),
                                        color = Color(0xFFF9DE09),
                                        strokeWidth = 3.dp,
                                        trackColor = Color(0xFF797979),
                                        strokeCap = StrokeCap.Round,
                                    )
                                    searchingLocation -> PlatformIcon(
                                        painter = PlatformIcons.Outlined.Map,
                                        contentDescription = if (Shared.language == "en") "Show Map" else "顯示地圖"
                                    )
                                    else -> PlatformIcon(
                                        painter = PlatformIcons.Outlined.Search,
                                        contentDescription = if (Shared.language == "en") "Search Location" else "搜尋位置"
                                    )
                                }
                            }
                        }
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (searchingLocation) {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect (Unit) {
                            delay(200)
                            focusRequester.requestFocus()
                        }
                        LaunchedEffect (textInput) {
                            updating = true
                            locations = Registry.getInstance(instance).searchLocation(textInput.text).await()
                            updating = false
                        }
                        PlatformOutlinedTextField(
                            modifier = Modifier
                                .applyIf(composePlatform.applePlatform) { padding(horizontal = 15.dp) }
                                .focusRequester(focusRequester),
                            value = textInput,
                            singleLine = true,
                            onValueChange = { textInput = it },
                            placeholder = {
                                PlatformText(
                                    text = if (Shared.language == "en") "Search Location" else "搜尋位置"
                                )
                            }
                        )
                        LazyColumn(
                            modifier = Modifier
                                .applyIf(composePlatform.applePlatform) { padding(horizontal = 15.dp) }
                                .fillMaxHeight(0.85F)
                                .verticalScrollBar(
                                    state = locationListScroll,
                                    scrollbarConfig = ScrollBarConfig(
                                        indicatorThickness = 6.dp,
                                        padding = PaddingValues(0.dp, 2.dp, 0.dp, 2.dp)
                                    )
                                ),
                            state = locationListScroll
                        ) {
                            if (locations.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .padding(5.dp)
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PlatformText(
                                            text = if (Shared.language == "en") "No Results" else "沒有結果",
                                            color = LocalContentColor.current.adjustAlpha(0.7F),
                                            fontSize = 21.sp,
                                            lineHeight = 1.1F.em
                                        )
                                    }
                                }
                            }
                            items(locations) {
                                Column(
                                    modifier = Modifier
                                        .clickable {
                                            searchingLocation = false
                                            updating = true
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val loc = it.resolveLocation().await()
                                                val newPos = RadiusCenterPosition(loc.lat, loc.lng, position.radius)
                                                withContext(Dispatchers.Main) {
                                                    initialPosition = RadiusCenterPosition(loc.lat, loc.lng, position.radius - 0.1F)
                                                    delay(750)
                                                    initialPosition = newPos
                                                    position = newPos
                                                    updating = false
                                                }
                                            }
                                        }
                                        .padding(5.dp)
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    PlatformText(
                                        text = it.name[Shared.language],
                                        fontSize = 21.sp,
                                        lineHeight = 1.1F.em
                                    )
                                    PlatformText(
                                        text = it.displayAddress[Shared.language],
                                        color = LocalContentColor.current.adjustAlpha(0.7F),
                                        fontSize = 15.sp,
                                        lineHeight = 1.1F.em
                                    )
                                }
                            }
                        }
                    } else {
                        PlatformButton(
                            modifier = Modifier
                                .height(70.fontScaledDp)
                                .fillMaxWidth(),
                            colors = ButtonDefaults.clearColors(),
                            contentPadding = PaddingValues(0.dp),
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    nearestStop?.let {
                                        val loc = it.stop.location
                                        val newPos = RadiusCenterPosition(loc.lat, loc.lng, position.radius)
                                        withContext(Dispatchers.Main) {
                                            position = newPos
                                            initialPosition = RadiusCenterPosition(loc.lat, loc.lng, position.radius - 0.1F)
                                            delay(100)
                                            initialPosition = newPos
                                        }
                                    }
                                }
                            }
                        ) {
                            PlatformText(
                                textAlign = TextAlign.Center,
                                lineHeight = 1.1F.em,
                                text = stopsInRange.firstOrNull()?.stop?.remarkedName?.get(Shared.language)?.asContentAnnotatedString()?.annotatedString?.let {
                                    it + (if (stopsInRange.size <= 1) "" else " (+${(stopsInRange.size - 1)})").asAnnotatedString()
                                }?: buildAnnotatedString {
                                    append(if (Shared.language == "en") {
                                        "No stops in the area".asAnnotatedString()
                                    } else {
                                        "此範圍沒有巴士站".asAnnotatedString()
                                    })
                                    nearestStop?.let {
                                        append("\n")
                                        append(if (Shared.language == "en") {
                                            "Nearest Stop: ${it.stop.name.en} (${(it.distance * 1000).roundToInt().formatDecimalSeparator()}m)"
                                        } else {
                                            "最近的巴士站: ${it.stop.name.zh} (${(it.distance * 1000).roundToInt().formatDecimalSeparator()}米)"
                                        }, SpanStyle(fontSize = TextUnit.Small))
                                    }
                                }
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxHeight(0.7F),
                            contentAlignment = Alignment.Center
                        ) {
                            MapSelectInterface(
                                instance = instance,
                                initialPosition = initialPosition,
                                currentRadius = position.radius,
                                onMove = { pos, zoom ->
                                    position = RadiusCenterPosition(pos.lat, pos.lng, position.radius)
                                    pxPerKm = 256F * 2F.pow(zoom) / 12742F
                                }
                            )
                            if (!isMapOverlayAlwaysOnTop) {
                                Canvas(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clipToBounds()
                                ) {
                                    val radius = pxPerKm * position.radius
                                    val maxRadius = size.maxDimension * 0.75F
                                    drawCircle(
                                        color = Color(0xff199fff),
                                        radius = radius.coerceAtMost(maxRadius),
                                        center = size.center,
                                        alpha = 0.3F,
                                        style = Fill
                                    )
                                    if (radius < maxRadius) {
                                        drawCircle(
                                            color = Color(0xff199fff),
                                            radius = radius,
                                            center = size.center,
                                            style = Stroke(
                                                width = 3.dp.toPx()
                                            )
                                        )
                                    }
                                }
                                PlatformIcon(
                                    modifier = Modifier.size(30.dp),
                                    painter = PlatformIcons.Outlined.MyLocation,
                                    tint = Color(0xff199fff),
                                    contentDescription = if (Shared.language == "en") "Custom Location" else "自訂位置"
                                )
                            }
                        }
                        PlatformSlider(
                            value = position.radius,
                            valueRange = 0.02F..3.0F,
                            onValueChange = { position = RadiusCenterPosition(position.lat, position.lng, it) }
                        )
                        PlatformText(
                            text = if (Shared.language == "en") "Search Radius: ${(position.radius * 1000).roundToInt()}m" else "搜尋半徑${(position.radius * 1000).roundToInt()}米"
                        )
                    }
                }
            },
            confirmButton = {
                PlatformText(
                    text = if (Shared.language == "en") "Confirm" else "確認"
                )
            },
            onConfirm = {
                choosingCustomCenterPosition = false
                if (customCenterPosition != position) {
                    customCenterPosition = position
                    routes = persistentListOf()
                    nearbyRoutesResult = null
                    Registry.getInstance(instance).setLastNearbyLocation(customCenterPosition, instance)
                }
            },
            dismissButton = {
                PlatformText(
                    text = if (Shared.language == "en") "Reset" else "重置"
                )
            },
            iosCloseButton = true,
            onDismissRequest = {
                choosingCustomCenterPosition = false
                if (it == DismissRequestType.BUTTON && customCenterPosition != null) {
                    customCenterPosition = null
                    routes = persistentListOf()
                    nearbyRoutesResult = null
                    Registry.getInstance(instance).setLastNearbyLocation(customCenterPosition, instance)
                }
            }
        )
    }
}