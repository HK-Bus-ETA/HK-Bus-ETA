/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
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
import com.loohp.hkbuseta.appcontext.ScreenState
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteWaypoints
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.compose.ChangedEffect
import com.loohp.hkbuseta.compose.LanguageDarkModeChangeEffect
import com.loohp.hkbuseta.compose.LocationOff
import com.loohp.hkbuseta.compose.PlatformFilledTonalIconToggleButton
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.shared.ComposeShared
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.closenessTo
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.getOperatorColor
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
import kotlin.math.max
import kotlin.math.roundToInt


@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun rememberGooglePlayServicesAvailable(context: AppActiveContext): Boolean {
    return rememberSaveable {
        !isHuaweiDevice() && GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context.context) == ConnectionResult.SUCCESS
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun isHuaweiDevice(): Boolean {
    return Build.MANUFACTURER.lowercase().contains("huawei") || Build.BRAND.lowercase().contains("huawei")
}

@Composable
actual fun MapRouteInterface(
    instance: AppActiveContext,
    waypoints: RouteWaypoints,
    stops: ImmutableList<Registry.StopData>,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    alternateStopNameShowing: Boolean,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>
) {
    val hasGooglePlayServices = rememberGooglePlayServicesAvailable(instance)
    if (hasGooglePlayServices) {
        GoogleMapRouteInterface(instance, waypoints, stops, selectedStopState, alternateStopNameShowing, alternateStopNames)
    } else {
        DefaultMapRouteInterface(waypoints, stops, selectedStopState, alternateStopNameShowing, alternateStopNames)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapRouteInterface(
    instance: AppActiveContext,
    waypoints: RouteWaypoints,
    stops: ImmutableList<Registry.StopData>,
    selectedStopState: MutableIntState,
    alternateStopNameShowing: Boolean,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>
) {
    val selectedStop by selectedStopState
    val indexMap by remember(waypoints, stops) { derivedStateOf { waypoints.buildStopListMapping(stops) } }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(stops[selectedStop - 1].stop.location.toGoogleLatLng(), 15F)
    }
    val icon = remember { Bitmap.createScaledBitmap(BitmapFactory.decodeResource(instance.context.resources, when (waypoints.co) {
        Operator.KMB -> when (waypoints.routeNumber.getKMBSubsidiary()) {
            KMBSubsidiary.KMB -> if (waypoints.isKmbCtbJoint) R.mipmap.bus_jointly_kmb else R.mipmap.bus_kmb
            KMBSubsidiary.LWB -> if (waypoints.isKmbCtbJoint) R.mipmap.bus_jointly_lwb else R.mipmap.bus_lwb
            KMBSubsidiary.SUNB -> R.mipmap.bus_kmb
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
    }), 96, 96, false) }
    val shouldShowStopIndex = remember { !waypoints.co.run { isTrain || isFerry } }
    val anchor = remember { if (waypoints.co.isTrain) Offset(0.5F, 0.5F) else Offset(0.5F, 1.0F) }
    var init by remember { mutableLongStateOf(-1) }
    var hasLocation by remember { mutableStateOf(false) }
    var gpsEnabled by remember { mutableStateOf(false) }
    val backgroundColor = if (Shared.theme.isDarkMode) 0xFF0F0F0F.toInt() else null

    LaunchedEffect (selectedStop, init) {
        if (init >= 0) {
            val location = stops[selectedStop - 1].stop.location
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
            StopMarkers(waypoints, alternateStopNames, alternateStopNameShowing, icon, anchor, selectedStopState, indexMap, shouldShowStopIndex)
            WaypointPaths(waypoints)
        }
    }
}

@Composable
@GoogleMapComposable
fun StopMarkers(
    waypoints: RouteWaypoints,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>,
    alternateStopNameShowing: Boolean,
    icon: Bitmap,
    anchor: Offset,
    selectedStopState: MutableIntState,
    indexMap: ImmutableList<Int>,
    shouldShowStopIndex: Boolean
) {
    key(waypoints, indexMap) {
        var selectedStop by selectedStopState
        for ((i, stop) in waypoints.stops.withIndex()) {
            val stopIndex = indexMap[i] + 1
            val title = (alternateStopNames.value?.takeIf { alternateStopNameShowing }?.get(stopIndex - 1)?.stop?: stop).name[Shared.language]
            val markerState = rememberStopMarkerState(stop)
            ChangedEffect (selectedStop) {
                if (selectedStop == stopIndex) {
                    markerState.showInfoWindow()
                }
            }
            Marker(
                state = markerState,
                title = if (shouldShowStopIndex) "${stopIndex}. $title" else title,
                snippet = stop.remark?.get(Shared.language),
                icon = BitmapDescriptorFactory.fromBitmap(icon),
                anchor = anchor,
                onClick = { selectedStop = stopIndex; false },
                zIndex = 3F
            )
        }
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
        #map { position: absolute; top: 0; bottom: 0; left: 0; right: 0; }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map').setView([22.32267, 144.17504], 13)

        var tileLayers = L.layerGroup();
        map.addLayer(tileLayers);

        var layer = L.layerGroup();
        map.addLayer(layer);
        
        var polylines = [];
        var polylinesOutline = [];
    </script>
</body>
</html>
"""

@Composable
fun rememberLeafletScript(
    waypoints: RouteWaypoints,
    alternateStopNameShowing: Boolean,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>,
    indexMap: ImmutableList<Int>
): State<String> {
    val stopNames by remember(waypoints) { derivedStateOf { waypoints.stops.mapIndexed { index, stop -> index to stop }.joinToString(",") { (index, stop) ->
        val resolvedStop = alternateStopNames.value?.takeIf { alternateStopNameShowing }?.get(index)?.stop?: stop
        "\"<b>" + resolvedStop.name[Shared.language] + "</b>" + (resolvedStop.remark?.let { r -> "<br><small>${r[Shared.language]}</small>" }?: "") + "\""
    } } }
    val stopsJsArray by remember(waypoints) { derivedStateOf { waypoints.stops.joinToString(",") { "[${it.location.lat}, ${it.location.lng}]" } } }
    val pathsJsArray by remember(waypoints) { derivedStateOf { waypoints.paths.joinToString(",") { path -> "[" + path.joinToString(separator = ",") { "[${it.lat},${it.lng}]" } + "]" } } }
    val pathColor = remember { waypoints.co.getLineColor(waypoints.routeNumber, Color.Red) }
    val colorHex = remember { pathColor.toHexString() }
    val iconFile = remember { when (waypoints.co) {
        Operator.KMB -> when (waypoints.routeNumber.getKMBSubsidiary()) {
            KMBSubsidiary.KMB -> if (waypoints.isKmbCtbJoint) "bus_jointly_kmb.svg" else "bus_kmb.svg"
            KMBSubsidiary.LWB -> if (waypoints.isKmbCtbJoint) "bus_jointly_lwb.svg" else "bus_lwb.svg"
            KMBSubsidiary.SUNB -> "bus_kmb.svg"
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
    } }
    val anchor = remember { if (waypoints.co.isTrain) Offset(0.5F, 0.5F) else Offset(0.5F, 1.0F) }
    val clearness = remember { pathColor.closenessTo(Color(0xFFFDE293)) }
    val (outlineHex, outlineOpacity) = remember { if (clearness > 0.8F) { Color.Blue.toHexString() to ((clearness - 0.8) / 0.05).toFloat() } else null to 0F }
    val shouldShowStopIndex = remember { !waypoints.co.run { isTrain || isFerry } }

    return remember(waypoints, stopNames, stopsJsArray, pathsJsArray) { derivedStateOf { """
        layer.clearLayers();
        
        var stopIcon = L.icon({
            iconUrl: 'file:///android_asset/$iconFile',
            iconSize: [30, 30],
            iconAnchor: [${anchor.x * 30F}, ${anchor.y * 30F}]
        });

        var stops = [$stopsJsArray];
        var stopNames = [$stopNames];
        var indexMap = [${indexMap.joinToString(separator = ",")}];

        var stopMarkers = stops.map(function(point, index) {
            var title;
            if ("$shouldShowStopIndex" == "true") {
                title = "<div style='text-align: center;'><b>" + (indexMap[index] + 1) + ". </b>" + stopNames[index] + "<div>";
            } else {
                title = "<div style='text-align: center;'>" + stopNames[index] + "<div>";
            }
            return L.marker(point, {icon: stopIcon})
                .addTo(layer)
                .bindPopup(title, { offset: L.point(0, -22), closeButton: false })
                .on('click', () => window.kmpJsBridge.callNative("SelectStop", index.toString(), null));
        });
        
        var paths = [$pathsJsArray];
        
        polylines = [];
        polylinesOutline = [];
        paths.forEach(function(path) {
            polylinesOutline.push(L.polyline(path, { color: '$outlineHex', opacity: $outlineOpacity, weight: 5 }).addTo(layer));
        });
        paths.forEach(function(path) {
            polylines.push(L.polyline(path, { color: '$colorHex', opacity: 1.0, weight: 4 }).addTo(layer));
        });
    """.trimIndent() } }
}

@Composable
fun DefaultMapRouteInterface(
    waypoints: RouteWaypoints,
    stops: ImmutableList<Registry.StopData>,
    selectedStopState: MutableIntState,
    alternateStopNameShowing: Boolean,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>
) {
    val webViewState = rememberWebViewStateWithHTMLData(baseHtml)
    val webViewNavigator = rememberWebViewNavigator()
    val webViewJsBridge = rememberWebViewJsBridge()
    var selectedStop by selectedStopState
    val indexMap by remember(waypoints, stops) { derivedStateOf { waypoints.buildStopListMapping(stops) } }
    val script by rememberLeafletScript(waypoints, alternateStopNameShowing, alternateStopNames, indexMap)
    val pathColor by ComposeShared.rememberOperatorColor(waypoints.co.getLineColor(waypoints.routeNumber, Color.Red), Operator.CTB.getOperatorColor(Color.Yellow).takeIf { waypoints.isKmbCtbJoint })
    val shouldHide by ScreenState.hasInterruptElement.collectAsStateMultiplatform()

    LaunchedEffect (script, webViewState.loadingState) {
        if (webViewState.loadingState == LoadingState.Finished) {
            webViewNavigator.evaluateJavaScript(script)
        }
    }
    LaunchedEffect (pathColor, webViewState.loadingState) {
        if (webViewState.loadingState == LoadingState.Finished) {
            val colorHex = pathColor.toHexString()
            val clearness = pathColor.closenessTo(Color(0xFFFDE293))
            val (outlineHex, outlineOpacity) = if (clearness > 0.8F) { Color.Blue.toHexString() to ((clearness - 0.8) / 0.05).toFloat() } else null to 0F
            webViewNavigator.evaluateJavaScript("""
                if (polylines || polylinesOutline) {
                    polylinesOutline.forEach(function(polyline) {
                        polyline.setStyle({ color: '$outlineHex', opacity: $outlineOpacity });
                    });
                    polylines.forEach(function(polyline) {
                        polyline.setStyle({ color: '$colorHex', opacity: 1.0 });
                    });
                }
            """.trimIndent())
        }
    }
    LaunchedEffect (selectedStop, webViewState.loadingState) {
        if (webViewState.loadingState == LoadingState.Finished) {
            val location = stops[selectedStop - 1].stop.location
            webViewNavigator.evaluateJavaScript("""
                map.flyTo([${location.lat},${location.lng}], 15, {animate: true, duration: 0.5});
            """.trimIndent())
        }
    }
    LaunchedEffect (Unit) {
        webViewJsBridge.register(object : IJsMessageHandler {
            override fun methodName(): String = "SelectStop"
            override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
                message.params.toIntOrNull()?.apply { selectedStop = indexMap[this] + 1 }
            }
        })
    }
    LanguageDarkModeChangeEffect (webViewState.loadingState) { language, darkMode ->
        if (webViewState.loadingState == LoadingState.Finished) {
            webViewNavigator.evaluateJavaScript("""
                tileLayers.clearLayers();
                L.tileLayer('$darkMode' === 'true' ? 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_nolabels/{z}/{x}/{y}.png' : 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/rastertiles/voyager_nolabels/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a> &copy; <a href="https://api.portal.hkmapservice.gov.hk/disclaimer">HKSAR Gov</a>'
                }).addTo(tileLayers);
                L.tileLayer('https://mapapi.geodata.gov.hk/gs/api/v1.0.0/xyz/label/hk/{lang}/WGS84/{z}/{x}/{y}.png'.replace("{lang}", "$language" === "en" ? "en" : "tc"), {
                    maxZoom: 19,
                }).addTo(tileLayers);
            """.trimIndent())
        }
    }
    ChangedEffect (selectedStop) {
        val index = indexMap.indexOf(selectedStop - 1)
        if (index >= 0) {
            webViewNavigator.evaluateJavaScript("""
            stopMarkers[$index].openPopup()
        """.trimIndent())
        }
    }

    if (!shouldHide) {
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = webViewState,
            navigator = webViewNavigator,
            webViewJsBridge = webViewJsBridge,
            captureBackPresses = false
        )
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
        DefaultMapSelectInterface(initialPosition, currentRadius, onMove)
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
    initialPosition: Coordinates,
    currentRadius: Float,
    onMove: (Coordinates, Float) -> Unit
) {
    val webViewState = rememberWebViewStateWithHTMLData(baseHtml)
    val webViewNavigator = rememberWebViewNavigator()
    val webViewJsBridge = rememberWebViewJsBridge()
    val shouldHide by ScreenState.hasInterruptElement.collectAsStateMultiplatform()
    var position by remember { mutableStateOf(initialPosition) }
    var init by remember { mutableStateOf(false) }

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
                L.tileLayer('$darkMode' === 'true' ? 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_nolabels/{z}/{x}/{y}.png' : 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/rastertiles/voyager_nolabels/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a> &copy; <a href="https://api.portal.hkmapservice.gov.hk/disclaimer">HKSAR Gov</a>'
                }).addTo(tileLayers);
                L.tileLayer('https://mapapi.geodata.gov.hk/gs/api/v1.0.0/xyz/label/hk/{lang}/WGS84/{z}/{x}/{y}.png'.replace("{lang}", "$language" === "en" ? "en" : "tc"), {
                    maxZoom: 19,
                }).addTo(tileLayers);
            """.trimIndent())
        }
    }

    if (!shouldHide) {
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = webViewState,
            navigator = webViewNavigator,
            webViewJsBridge = webViewJsBridge,
            captureBackPresses = false
        )
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