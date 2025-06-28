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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.core.graphics.scale
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RouteWaypoints
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.DebugPurpose
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.compose.ChangedEffect
import com.loohp.hkbuseta.compose.ImmediateEffect
import com.loohp.hkbuseta.compose.LanguageDarkModeChangeEffect
import com.loohp.hkbuseta.compose.LocationOff
import com.loohp.hkbuseta.compose.PlatformFilledTonalIconToggleButton
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.compose.platformBackgroundColor
import com.loohp.hkbuseta.shared.ComposeShared
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.closenessTo
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.hasGooglePlayService
import com.loohp.hkbuseta.utils.isHuaweiDevice
import com.loohp.hkbuseta.utils.toHexString
import com.loohp.hkbuseta.utils.withAlpha
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt


@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun rememberGooglePlayServicesAvailable(context: AppActiveContext): Boolean {
    return rememberSaveable { !isHuaweiDevice() && hasGooglePlayService(context.context) }
}

@Composable
actual fun MapRouteInterface(
    instance: AppActiveContext,
    sections: ImmutableList<MapRouteSection>,
    selectedStopState: MutableIntState,
    selectedSectionState: MutableIntState,
    alternateStopNameShowing: Boolean
) {
    val hasGooglePlayServices = rememberGooglePlayServicesAvailable(instance)
    if (hasGooglePlayServices) {
        GoogleMapRouteInterface(instance, sections, selectedStopState, selectedSectionState, alternateStopNameShowing)
    } else {
        DefaultMapRouteInterface(instance, sections, selectedStopState, selectedSectionState, alternateStopNameShowing)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapRouteInterface(
    instance: AppActiveContext,
    sections: ImmutableList<MapRouteSection>,
    selectedStopState: MutableIntState,
    selectedSectionState: MutableIntState,
    alternateStopNameShowing: Boolean
) {
    val selectedStop by selectedStopState
    val selectedSection by selectedSectionState
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(sections[selectedSection].stops[selectedStop - 1].stop.location.toGoogleLatLng(), 15F)
    }
    val icons = remember { sections.map {
        BitmapFactory.decodeResource(
            instance.context.resources, when (it.waypoints.co) {
                Operator.KMB -> when (it.waypoints.routeNumber.getKMBSubsidiary()) {
                    KMBSubsidiary.KMB -> if (it.waypoints.isKmbCtbJoint) R.mipmap.bus_jointly_kmb else R.mipmap.bus_kmb
                    KMBSubsidiary.LWB -> if (it.waypoints.isKmbCtbJoint) R.mipmap.bus_jointly_lwb else R.mipmap.bus_lwb
                    else -> R.mipmap.bus_kmb
                }

                Operator.CTB -> R.mipmap.bus_ctb
                Operator.NLB -> R.mipmap.bus_nlb
                Operator.GMB -> R.mipmap.minibus
                Operator.MTR_BUS -> R.mipmap.bus_mtrbus
                Operator.LRT -> R.mipmap.mtr
                Operator.MTR -> R.mipmap.mtr
                Operator.HKKF -> R.mipmap.bus_nlb
                Operator.SUNFERRY -> R.mipmap.bus_nlb
                Operator.FORTUNEFERRY -> R.mipmap.bus_nlb
                else -> R.mipmap.bus_kmb
            }
        ).scale(96, 96, false) } }
    val shouldShowStopIndex = remember { sections.map { !it.waypoints.co.run { isTrain || isFerry } } }
    val anchors = remember { sections.map { if (it.waypoints.co.isTrain) Offset(0.5F, 0.5F) else Offset(0.5F, 1.0F) } }
    var init by remember { mutableLongStateOf(-1) }
    var hasLocation by remember { mutableStateOf(false) }
    var gpsEnabled by remember { mutableStateOf(false) }
    val backgroundColor = if (Shared.theme.isDarkMode) 0xFF0F0F0F.toInt() else null

    LaunchedEffect (selectedSection, selectedStop, init) {
        if (init >= 0) {
            val location = sections[selectedSection].stops[selectedStop - 1].stop.location
            if (currentTimeMillis() - init > 500) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(location.toGoogleLatLng(), 15F), 500)
            } else {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(location.toGoogleLatLng(), 15F))
            }
        }
    }
    LaunchedEffect (Unit) {
        checkLocationPermission(instance, true) { hasLocation = it }
    }

    Box {
        if (hasLocation && !gpsEnabled) {
            PlatformFilledTonalIconToggleButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(100F)
                    .padding(1.dp)
                    .plainTooltip(if (Shared.language == "en") "Enable GPS" else "顯示定位"),
                checked = gpsEnabled,
                onCheckedChange = { gpsEnabled = !gpsEnabled }
            ) {
                Icon(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center),
                    painter = PlatformIcons.Outlined.LocationOff,
                    contentDescription = if (Shared.language == "en") "Enable GPS" else "顯示定位"
                )
            }
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
            ),
            googleMapOptionsFactory = { GoogleMapOptions().backgroundColor(backgroundColor) },
            properties = MapProperties(
                isMyLocationEnabled = gpsEnabled,
                isBuildingEnabled = true,
                isIndoorEnabled = true
            ),
            cameraPositionState = cameraPositionState,
            mapColorScheme = if (Shared.theme.isDarkMode) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
            onMapLoaded = { init = currentTimeMillis() }
        ) {
            for ((index, section) in sections.withIndex()) {
                StopMarkers(
                    instance = instance,
                    waypoints = section.waypoints,
                    stops = section.stops.asImmutableList(),
                    alternateStopNames = section.alternateStopNames?.asImmutableList().asImmutableState(),
                    alternateStopNameShowing = alternateStopNameShowing,
                    icon = icons[index],
                    anchor = anchors[index],
                    selectedStopState = selectedStopState,
                    selectedSectionState = selectedSectionState,
                    sectionIndex = index,
                    shouldShowStopIndex = shouldShowStopIndex[index]
                )
                WaypointPaths(
                    waypoints = section.waypoints
                )
            }
        }
    }
}

@Composable
@GoogleMapComposable
fun StopMarkers(
    instance: AppActiveContext,
    waypoints: RouteWaypoints,
    stops: ImmutableList<Registry.StopData>,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>,
    alternateStopNameShowing: Boolean,
    icon: Bitmap,
    anchor: Offset,
    selectedStopState: MutableIntState,
    selectedSectionState: MutableIntState,
    sectionIndex: Int,
    shouldShowStopIndex: Boolean
) {
    key(waypoints, stops) {
        val indexMap = remember { waypoints.buildStopListMapping(instance, stops) }
        var selectedStop by selectedStopState
        var selectedSection by selectedSectionState
        for ((i, stop) in waypoints.stops.withIndex()) {
            val stopIndex = { indexMap[i] + 1 }.logPossibleStopMarkerIndexMapException(instance, waypoints)?: continue
            val title = (alternateStopNames.value?.takeIf { alternateStopNameShowing }?.getOrNull(stopIndex - 1)?.stop?: stop).name[Shared.language]
            val markerState = rememberStopMarkerState(stop)
            ChangedEffect (selectedSection, selectedStop) {
                if (selectedSection == sectionIndex && selectedStop == stopIndex) {
                    markerState.showInfoWindow()
                }
            }
            Marker(
                state = markerState,
                title = if (shouldShowStopIndex) "${stopIndex}. $title" else title,
                snippet = stop.remark?.get(Shared.language),
                icon = BitmapDescriptorFactory.fromBitmap(icon),
                anchor = anchor,
                onClick = {
                    selectedSection = sectionIndex
                    selectedStop = stopIndex
                    false
                },
                zIndex = 3F
            )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
@DebugPurpose
inline fun (() -> Int).logPossibleStopMarkerIndexMapException(
    instance: AppActiveContext,
    waypoints: RouteWaypoints
): Int? {
    return try {
        invoke()
    } catch (e: Throwable) {
        instance.logFirebaseEvent("stop_marker_crash_v2_${waypoints.co.name}_${waypoints.routeNumber}", AppBundle())
        null
    }
}

@Composable
@GoogleMapComposable
fun WaypointPaths(waypoints: RouteWaypoints) {
    val pathColor by ComposeShared.rememberOperatorColor(waypoints.co.getLineColor(waypoints.routeNumber, Color.Red), Operator.CTB.getOperatorColor(Color.Yellow).takeIf { waypoints.isKmbCtbJoint })
    val closeness by remember { derivedStateOf { max(pathColor.closenessTo(Color(0xFFFDE293)), pathColor.closenessTo(Color(0xFFAAD4FF))) } }
    for (lines in waypoints.paths) {
        val clearness = if (Shared.theme.isDarkMode) 0F else closeness
        if (clearness > 0.8F) {
            Polyline(
                points = lines.toGoogleLatLng(),
                color = Color.Blue.withAlpha((((clearness - 0.8) / 0.05) * 255).roundToInt().coerceIn(0, 255)),
                width = 14F,
                zIndex = 1F,
            )
        }
        Polyline(
            points = lines.toGoogleLatLng(),
            color = pathColor,
            width = 10F,
            zIndex = 2F
        )
    }
}

@Composable
fun rememberStopMarkerState(stop: Stop): MarkerState {
    return remember(stop) { MarkerState(stop.location.toGoogleLatLng()) }
}

const val baseHtml: String = """
<!DOCTYPE html>
<html>
<head>
    <title>Route Map</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
          integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
          crossorigin=""/>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
            integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo="
            crossorigin=""></script>
    <style>
        #map { 
            position: absolute; top: 0; bottom: 0; left: 0; right: 0; 
        }
        
        .leaflet-dark-theme.leaflet-layer {
            filter: brightness(0.6) invert(1) contrast(3) hue-rotate(200deg) saturate(0.3) brightness(0.7);
        }
        
        .leaflet-dark-theme.leaflet-control-attribution {
            background: #111111 !important;
            color: #AAAAAA;
            filter: brightness(1.4);
        }
        
        .leaflet-dark-theme.leaflet-control-zoom {
            filter: brightness(0.6) invert(1) contrast(3);
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map').setView([22.32267, 144.17504], 13);
        var mapElement = document.getElementById("map");

        var tileLayers = L.layerGroup();
        map.addLayer(tileLayers);

        var layer = L.layerGroup();
        map.addLayer(layer);
        
        var stopMarkers = [];
        
        var polylines = [];
        var polylinesOutline = [];
    </script>
</body>
</html>
"""

@Composable
fun rememberLeafletScript(
    sections: ImmutableList<MapRouteSection>,
    alternateStopNameShowing: Boolean,
    indexMap: ImmutableList<ImmutableList<Int>>
): State<String> {
    val stopNames by remember(sections, alternateStopNameShowing) { derivedStateOf {
        sections.joinToString(prefix = "[[", separator = "]],[[", postfix = "]]") { s ->
            s.waypoints.stops.mapIndexed { index, stop -> index to stop }
                .joinToString(",") { (index, stop) ->
                    val resolvedStop = s.alternateStopNames?.takeIf { alternateStopNameShowing }
                        ?.get(index)?.stop ?: stop
                    "\"<b>" + resolvedStop.name[Shared.language] + "</b>" + (resolvedStop.remark?.let { r -> "<br><small>${r[Shared.language]}</small>" }
                        ?: "") + "\""
                }
        }
    } }
    val stopsJsArray by remember(sections) { derivedStateOf {
        sections.joinToString(prefix = "[", separator = "],[", postfix = "]") {
            s -> s.waypoints.stops.joinToString(",") { "[${it.location.lat}, ${it.location.lng}]" }
        }
    } }
    val pathsJsArray by remember(sections) { derivedStateOf {
        sections.joinToString(prefix = "[", separator = "],[", postfix = "]") { s ->
            s.waypoints.paths.joinToString(",") { path -> "[" + path.joinToString(separator = ",") { "[${it.lat},${it.lng}]" } + "]" }
        }
    } }
    val pathColors = remember { sections.map { s -> s.waypoints.co.getLineColor(s.waypoints.routeNumber, Color.Red) } }
    val colorHexes = remember {
        pathColors.joinToString(prefix = "[\"", separator = "\"],[\"", postfix = "\"]") { it.toHexString() }
    }
    val iconFiles = remember {
        sections.joinToString(prefix = "[\"", separator = "\"],[\"", postfix = "\"]") { s ->
            when (s.waypoints.co) {
                Operator.KMB -> when (s.waypoints.routeNumber.getKMBSubsidiary()) {
                    KMBSubsidiary.KMB -> if (s.waypoints.isKmbCtbJoint) "bus_jointly_kmb.svg" else "bus_kmb.svg"
                    KMBSubsidiary.LWB -> if (s.waypoints.isKmbCtbJoint) "bus_jointly_lwb.svg" else "bus_lwb.svg"
                    else -> "bus_kmb.svg"
                }
                Operator.CTB -> "bus_ctb.svg"
                Operator.NLB -> "bus_nlb.svg"
                Operator.GMB -> "minibus.svg"
                Operator.MTR_BUS -> "bus_mtr-bus.svg"
                Operator.LRT -> "mtr.svg"
                Operator.MTR -> "mtr.svg"
                Operator.HKKF -> "bus_nlb.svg"
                Operator.SUNFERRY -> "bus_nlb.svg"
                Operator.FORTUNEFERRY -> "bus_nlb.svg"
                else -> "bus_kmb.svg"
            }
        }
    }
    val anchors = remember { sections.asSequence()
        .map { s -> if (s.waypoints.co.isTrain) Offset(0.5F, 0.5F) else Offset(0.5F, 1.0F) }
        .map { a -> "[${a.x * 30F}, ${a.y * 30F}]" }
        .joinToString(prefix = "[", separator = "],[", postfix = "]")
    }
    val clearnesses = remember { pathColors.map { it.closenessTo(Color(0xFFFDE293)) } }
    val outlineHexOpacity = remember { clearnesses.asSequence()
        .map { if (it > 0.8F) { "[\"${Color.Blue.toHexString()}\", ${((it - 0.8) / 0.05).toFloat()}]" } else "[null, 0]" }
        .joinToString(prefix = "[", separator = "],[", postfix = "]")
    }
    val shouldShowStopIndex = remember(sections) {
        sections.joinToString(prefix = "[", separator = "],[", postfix = "]") {
            (!it.waypoints.co.run { isTrain || isFerry }).toString()
        }
    }
    val indexMapStr = remember(indexMap) { indexMap.joinToString(prefix = "[", separator = "],[", postfix = "]") { it.joinToString(prefix = "[", separator = ",", postfix = "]") } }

    return remember(sections, stopNames, stopsJsArray, pathsJsArray) { derivedStateOf { """
        layer.clearLayers();
        
        polylinesList = [];
        polylinesOutlineList = [];
        stopMarkersList = [];

        var stops = $stopsJsArray;
        var stopNames = $stopNames;
        var indexMap = $indexMapStr;
        var colorHexes = $colorHexes;
        var iconFiles = $iconFiles;
        var anchors = $anchors;
        var outlineHexOpacity = $outlineHexOpacity;
        var shouldShowStopIndex = $shouldShowStopIndex;
        var paths = [$pathsJsArray];
        
        for (var i = 0; i < ${sections.size}; i++) {
            var sectionIndex = i;
            var stopIcon = L.icon({
                iconUrl: 'file:///android_asset/' + iconFiles[sectionIndex],
                iconSize: [30, 30],
                iconAnchor: anchors[sectionIndex]
            });
    
            var stopMarkers = stops.map(function(point, index) {
                var title;
                if (shouldShowStopIndex[sectionIndex]) {
                    title = "<div style='text-align: center;'><b>" + (indexMap[sectionIndex][index] + 1) + ". </b>" + stopNames[sectionIndex][index] + "<div>";
                } else {
                    title = "<div style='text-align: center;'>" + stopNames[sectionIndex][index] + "<div>";
                }
                return L.marker(point, {icon: stopIcon})
                    .addTo(layer)
                    .bindPopup(title, { offset: L.point(0, -22), closeButton: false })
                    .on('click', () => window.kmpJsBridge.callNative("SelectStop", sectionIndex + "," + index, null));
            });
            
            var polylines = [];
            var polylinesOutline = [];
            paths[sectionIndex].forEach(function(path) {
                polylinesOutline.push(L.polyline(path, { color: outlineHexOpacity[sectionIndex][0], opacity: outlineHexOpacity[sectionIndex][1], weight: 5 }).addTo(layer));
            });
            paths[sectionIndex].forEach(function(path) {
                polylines.push(L.polyline(path, { color: colorHexes[sectionIndex], opacity: 1.0, weight: 4 }).addTo(layer));
            });
            
            stopMarkersList.push(stopMarkers);
            polylinesList.push(polylines);
            polylinesOutlineList.push(polylinesOutline);
        }
    """.trimIndent() } }
}

@Composable
fun DefaultMapRouteInterface(
    instance: AppActiveContext,
    sections: ImmutableList<MapRouteSection>,
    selectedStopState: MutableIntState,
    selectedSectionState: MutableIntState,
    alternateStopNameShowing: Boolean
) {
    val windowSize = LocalWindowInfo.current.containerSize
    var isInWindow by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.onGloballyPositioned { isInWindow = it.isVisible(windowSize) }
    ) {
        key(isInWindow) {
            val scope = rememberCoroutineScope()
            val webViewState = rememberWebViewStateWithHTMLData(baseHtml)
            val webViewNavigator = rememberWebViewNavigator()
            val webViewJsBridge = rememberWebViewJsBridge()
            var selectedStop by selectedStopState
            var selectedSection by selectedSectionState
            val indexMap by remember(sections) { derivedStateOf { sections.map { it.waypoints.buildStopListMapping(instance, it.stops) }.asImmutableList() } }
            val script by rememberLeafletScript(sections, alternateStopNameShowing, indexMap)
            val pathColors by ComposeShared.rememberOperatorColors(sections.map { s -> s.waypoints.co.getLineColor(s.waypoints.routeNumber, Color.Red) to Operator.CTB.getOperatorColor(Color.Yellow).takeIf { s.waypoints.isKmbCtbJoint } }.asImmutableList())
            val background = platformBackgroundColor
            val haptics = LocalHapticFeedback.current.common

            ImmediateEffect (Unit) {
                webViewState.webSettings.backgroundColor = background
            }
            LaunchedEffect (script, webViewState.loadingState) {
                if (webViewState.loadingState == LoadingState.Finished) {
                    webViewNavigator.evaluateJavaScript(script)
                }
            }
            LaunchedEffect (pathColors, webViewState.loadingState) {
                if (webViewState.loadingState == LoadingState.Finished) {
                    for ((index, pathColor) in pathColors.withIndex()) {
                        val colorHex = pathColor.toHexString()
                        val clearness = pathColor.closenessTo(Color(0xFFFDE293))
                        val (outlineHex, outlineOpacity) = if (clearness > 0.8F) { Color.Blue.toHexString() to ((clearness - 0.8) / 0.05).toFloat() } else null to 0F
                        webViewNavigator.evaluateJavaScript("""
                            if (polylinesList[$index] || polylinesOutlineList[$index]) {
                                polylinesOutlineList[$index].forEach(function(polyline) {
                                    polyline.setStyle({ color: '$outlineHex', opacity: $outlineOpacity });
                                });
                                polylinesList[$index].forEach(function(polyline) {
                                    polyline.setStyle({ color: '$colorHex', opacity: 1.0 });
                                });
                            }
                        """.trimIndent())
                    }
                }
            }
            LaunchedEffect (selectedSection, selectedStop, webViewState.loadingState) {
                if (webViewState.loadingState == LoadingState.Finished) {
                    val location = sections[selectedSection].stops[selectedStop - 1].stop.location
                    webViewNavigator.evaluateJavaScript("""
                        map.flyTo([${location.lat},${location.lng}], 15, {animate: true, duration: 0.5});
                    """.trimIndent())
                }
            }
            LaunchedEffect (Unit) {
                webViewJsBridge.register(object : IJsMessageHandler {
                    override fun methodName(): String = "SelectStop"
                    override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
                        val (sectionIndex, stopIndex) = message.params.split(",").map { it.toIntOrNull() }
                        if (sectionIndex != null && stopIndex != null) {
                            scope.launch {
                                selectedSection = sectionIndex
                                selectedStop = indexMap[sectionIndex][stopIndex] + 1
                            }
                        }
                    }
                })
            }
            LanguageDarkModeChangeEffect (webViewState.loadingState) { language, darkMode ->
                if (webViewState.loadingState == LoadingState.Finished) {
                    webViewNavigator.evaluateJavaScript("""
                        tileLayers.clearLayers();
                            
                        const argb = Number(${background.toArgb()});
                        const alpha = (argb >> 24) & 0xFF;
                        const red = (argb >> 16) & 0xFF;
                        const green = (argb >> 8) & 0xFF;
                        const blue = argb & 0xFF;
                        const alphaCss = alpha / 255;
                        mapElement.style.backgroundColor = "rgba(" + red + ", " + green + ", " + blue + ", " + alphaCss + ")";
                        
                        L.tileLayer('$darkMode' === 'true' ? 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_nolabels/{z}/{x}/{y}.png' : 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/rastertiles/voyager_nolabels/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a> &copy; <a href="https://api.portal.hkmapservice.gov.hk/disclaimer">HKSAR Gov</a>'
                        }).addTo(tileLayers);
                        L.tileLayer('https://mapapi.geodata.gov.hk/gs/api/v1.0.0/xyz/label/hk/{lang}/WGS84/{z}/{x}/{y}.png'.replace("{lang}", "$language" === "en" ? "en" : "tc"), {
                            maxZoom: 19,
                        }).addTo(tileLayers);
                        
                        const mapComponents = document.querySelectorAll('.leaflet-layer, .leaflet-control-zoom, .leaflet-control-attribution');
                        if ('$darkMode' === 'true') {
                            mapComponents.forEach(element => element.classList.add('leaflet-dark-theme'));
                        } else {
                            mapComponents.forEach(element => element.classList.remove('leaflet-dark-theme'));
                        }
                    """.trimIndent())
                }
            }
            ChangedEffect (selectedSection, selectedStop) {
                val index = indexMap[selectedSection].indexOf(selectedStop - 1)
                if (index >= 0) {
                    webViewNavigator.evaluateJavaScript("""
                    stopMarkersList[$selectedSection][$index].openPopup();
                """.trimIndent())
                }
            }
            LaunchedEffect (webViewState.lastLoadedUrl) {
                val url = webViewState.lastLoadedUrl
                if (url != null && url != "about:blank") {
                    instance.handleWebpages(url, false, haptics).invoke()
                    isInWindow = !isInWindow
                }
            }

            WebView(
                modifier = Modifier.fillMaxSize(),
                state = webViewState,
                navigator = webViewNavigator,
                webViewJsBridge = webViewJsBridge,
                captureBackPresses = false
            )
        }
    }
}

@Composable
actual fun MapSelectInterface(
    instance: AppActiveContext,
    initialPosition: Coordinates,
    currentRadius: Float,
    onMove: (Coordinates, Float) -> Unit
) {
    val hasGooglePlayServices = rememberGooglePlayServicesAvailable(instance)
    if (hasGooglePlayServices) {
        GoogleMapSelectInterface(instance, initialPosition, onMove)
    } else {
        DefaultMapSelectInterface(instance, initialPosition, currentRadius, onMove)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapSelectInterface(
    instance: AppActiveContext,
    initialPosition: Coordinates,
    onMove: (Coordinates, Float) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition.toGoogleLatLng(), 15F)
    }
    var hasLocation by remember { mutableStateOf(false) }
    var gpsEnabled by remember { mutableStateOf(false) }
    val backgroundColor = if (Shared.theme.isDarkMode) 0xFF0F0F0F.toInt() else null
    var init by remember { mutableLongStateOf(-1) }

    LaunchedEffect (Unit) {
        checkLocationPermission(instance, true) { hasLocation = it }
    }
    LaunchedEffect (initialPosition) {
        if (init >= 0) {
            if (currentTimeMillis() - init > 500) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(initialPosition.toGoogleLatLng(), 15F), 500)
            } else {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(initialPosition.toGoogleLatLng(), 15F))
            }
        }
    }
    LaunchedEffect (cameraPositionState.position.target) {
        onMove.invoke(cameraPositionState.position.target.let { Coordinates(it.latitude, it.longitude) }, cameraPositionState.position.zoom)
    }

    Box {
        if (hasLocation && !gpsEnabled) {
            PlatformFilledTonalIconToggleButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(100F)
                    .padding(1.dp)
                    .plainTooltip(if (Shared.language == "en") "Enable GPS" else "顯示定位"),
                checked = gpsEnabled,
                onCheckedChange = { gpsEnabled = !gpsEnabled }
            ) {
                Icon(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center),
                    painter = PlatformIcons.Outlined.LocationOff,
                    contentDescription = if (Shared.language == "en") "Enable GPS" else "顯示定位"
                )
            }
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false
            ),
            googleMapOptionsFactory = { GoogleMapOptions().backgroundColor(backgroundColor) },
            properties = MapProperties(
                isMyLocationEnabled = gpsEnabled,
                isBuildingEnabled = true,
                isIndoorEnabled = true
            ),
            cameraPositionState = cameraPositionState,
            mapColorScheme = if (Shared.theme.isDarkMode) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
            onMapLoaded = { init = currentTimeMillis() }
        ) { /* do nothing */ }
    }
}

@Composable
fun DefaultMapSelectInterface(
    instance: AppActiveContext,
    initialPosition: Coordinates,
    currentRadius: Float,
    onMove: (Coordinates, Float) -> Unit
) {
    val windowSize = LocalWindowInfo.current.containerSize
    var isInWindow by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.onGloballyPositioned { isInWindow = it.isVisible(windowSize) }
    ) {
        key(isInWindow) {
            val webViewState = rememberWebViewStateWithHTMLData(baseHtml)
            val webViewNavigator = rememberWebViewNavigator()
            val webViewJsBridge = rememberWebViewJsBridge()
            var position by remember { mutableStateOf(initialPosition) }
            var init by remember { mutableStateOf(false) }
            val background = platformBackgroundColor
            val haptics = LocalHapticFeedback.current.common

            ImmediateEffect (Unit) {
                webViewState.webSettings.backgroundColor = background
            }
            LaunchedEffect (Unit) {
                webViewJsBridge.register(object : IJsMessageHandler {
                    override fun methodName(): String = "MoveCenter"
                    override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
                        val parts = message.params.split(",")
                        val pos = Coordinates(parts[0].toDouble(), parts[1].toDouble())
                        position = pos
                        onMove.invoke(pos, parts[2].toFloat())
                    }
                })
            }
            LaunchedEffect (initialPosition, webViewState.loadingState) {
                if (webViewState.loadingState == LoadingState.Finished) {
                    if (init) {
                        webViewNavigator.evaluateJavaScript("""
                            map.flyTo([${initialPosition.lat},${initialPosition.lng}], 15, {animate: true, duration: 0.5});
                        """.trimIndent())
                            } else {
                                webViewNavigator.evaluateJavaScript("""
                            map.flyTo([${initialPosition.lat},${initialPosition.lng}], 15, {animate: false});
                            
                            function onMapMove() {
                                var center = map.getCenter();
                                var zoom = map.getZoom();
                                window.kmpJsBridge.callNative("MoveCenter", center.lat + "," + center.lng + "," + zoom, null);
                            }
                            
                            map.on('moveend', onMapMove);
                        """.trimIndent())
                        init = true
                    }
                }
            }
            LaunchedEffect (position, currentRadius, webViewState.loadingState) {
                if (webViewState.loadingState == LoadingState.Finished) {
                    webViewNavigator.evaluateJavaScript("""
                        layer.clearLayers();
                        var marker = L.marker([lat, lng]).addTo(layer);
                        var circle = L.circle([lat, lng], {
                            color: '#199fff',
                            fillColor: '#199fff',
                            fillOpacity: 0.3,
                            radius: radius
                        }).addTo(layer);
                    """.trimIndent())
                }
            }
            LanguageDarkModeChangeEffect (webViewState.loadingState) { language, darkMode ->
                if (webViewState.loadingState == LoadingState.Finished) {
                    webViewNavigator.evaluateJavaScript("""
                        tileLayers.clearLayers();
                            
                        const argb = Number(${background.toArgb()});
                        const alpha = (argb >> 24) & 0xFF;
                        const red = (argb >> 16) & 0xFF;
                        const green = (argb >> 8) & 0xFF;
                        const blue = argb & 0xFF;
                        const alphaCss = alpha / 255;
                        mapElement.style.backgroundColor = "rgba(" + red + ", " + green + ", " + blue + ", " + alphaCss + ")";
                        
                        L.tileLayer('$darkMode' === 'true' ? 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_nolabels/{z}/{x}/{y}.png' : 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/rastertiles/voyager_nolabels/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a> &copy; <a href="https://api.portal.hkmapservice.gov.hk/disclaimer">HKSAR Gov</a>'
                        }).addTo(tileLayers);
                        L.tileLayer('https://mapapi.geodata.gov.hk/gs/api/v1.0.0/xyz/label/hk/{lang}/WGS84/{z}/{x}/{y}.png'.replace("{lang}", "$language" === "en" ? "en" : "tc"), {
                            maxZoom: 19,
                        }).addTo(tileLayers);
                        
                        const mapComponents = document.querySelectorAll('.leaflet-layer, .leaflet-control-zoom, .leaflet-control-attribution');
                        if ('$darkMode' === 'true') {
                            mapComponents.forEach(element => element.classList.add('leaflet-dark-theme'));
                        } else {
                            mapComponents.forEach(element => element.classList.remove('leaflet-dark-theme'));
                        }
                    """.trimIndent())
                }
            }
            LaunchedEffect (webViewState.lastLoadedUrl) {
                val url = webViewState.lastLoadedUrl
                if (url != null && url != "about:blank") {
                    instance.handleWebpages(url, false, haptics).invoke()
                    isInWindow = !isInWindow
                }
            }

            WebView(
                modifier = Modifier.fillMaxSize(),
                state = webViewState,
                navigator = webViewNavigator,
                webViewJsBridge = webViewJsBridge,
                captureBackPresses = false
            )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Coordinates.toGoogleLatLng(): LatLng {
    return LatLng(lat, lng)
}

@Suppress("NOTHING_TO_INLINE")
inline fun Collection<Coordinates>.toGoogleLatLng(): List<LatLng> {
    return map { it.toGoogleLatLng() }
}

@Suppress("NOTHING_TO_INLINE")
inline fun LayoutCoordinates.isVisible(windowSize: IntSize): Boolean {
    val windowBound = Rect(Offset.Zero, windowSize.toSize())
    val bound = boundsInWindow()
    return windowBound.overlaps(bound)
}

actual val isMapOverlayAlwaysOnTop: Boolean = false