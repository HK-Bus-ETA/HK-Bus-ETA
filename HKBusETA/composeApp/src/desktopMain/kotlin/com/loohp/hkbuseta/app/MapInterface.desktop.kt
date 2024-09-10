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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.ScreenState
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteWaypoints
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.compose.LanguageDarkModeChangeEffect
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.shared.ComposeShared
import com.loohp.hkbuseta.utils.closenessTo
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.toHexString
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import dev.datlag.kcef.KCEF
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

enum class KCEFLoadState {
    INITIALIZED, DOWNLOADING, RESTART_REQUIRED, ERROR, NOT_INITIALIZED
}

@Composable
inline fun KCEFWebContainer(crossinline content: @Composable () -> Unit) {
    var kcefLoadState by rememberSaveable { mutableStateOf(KCEFLoadState.NOT_INITIALIZED) }
    var downloadingProgress by rememberSaveable { mutableFloatStateOf(0F) }

    LaunchedEffect (Unit) {
        withContext(dispatcherIO) {
            KCEF.init(
                builder = {
                    installDir(File("kcef-bundle"))
                    progress {
                        onDownloading {
                            kcefLoadState = KCEFLoadState.DOWNLOADING
                            downloadingProgress = max(it / 100F, 0F)
                        }
                        onInitialized {
                            kcefLoadState = KCEFLoadState.INITIALIZED
                        }
                    }
                    settings {
                        cachePath = File("cache").absolutePath
                    }
                },
                onError = {
                    kcefLoadState = KCEFLoadState.ERROR
                    it?.printStackTrace()
                },
                onRestartRequired = {
                    kcefLoadState = KCEFLoadState.RESTART_REQUIRED
                }
            )
        }
    }

    Surface (
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (kcefLoadState) {
                KCEFLoadState.NOT_INITIALIZED -> Text(
                    fontSize = 25.sp,
                    textAlign = TextAlign.Center,
                    text = if (Shared.language == "en") "Loading Map..." else "地圖載入中..."
                )
                KCEFLoadState.RESTART_REQUIRED -> Text(
                    fontSize = 25.sp,
                    textAlign = TextAlign.Center,
                    text = if (Shared.language == "en") "Please relaunch app" else "請重新開啟應用程式"
                )
                KCEFLoadState.DOWNLOADING -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val progressAnimation by animateFloatAsState(
                        targetValue = downloadingProgress,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        label = "LoadingProgressAnimation"
                    )
                    Text(
                        fontSize = 25.sp,
                        textAlign = TextAlign.Center,
                        text = if (Shared.language == "en") "Downloading data for map display" else "正在下載用於地圖顯示的資料"
                    )
                    LinearProgressIndicator(
                        progress = { progressAnimation },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(25.dp, 0.dp),
                        color = Color(0xFFF9DE09),
                        trackColor = Color(0xFF797979),
                    )
                }
                KCEFLoadState.ERROR -> Text(
                    fontSize = 25.sp,
                    textAlign = TextAlign.Center,
                    text = if (Shared.language == "en") "Error occurred while displaying map" else "顯示地圖時發生錯誤"
                )
                KCEFLoadState.INITIALIZED -> content.invoke()
            }
        }
    }
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
fun rememberLeafletScript(waypoints: RouteWaypoints, alternateStopNameShowing: Boolean, alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>): State<String> {
    val stopNames by remember(waypoints) { derivedStateOf { waypoints.stops.mapIndexed { index, stop -> index to stop }.joinToString(",") { (index, stop) ->
        val resolvedStop = alternateStopNames.value?.takeIf { alternateStopNameShowing }?.get(index)?.stop?: stop
        "\"<b>" + resolvedStop.name[Shared.language] + "</b>" + (resolvedStop.remark?.let { r -> "<br><small>${r[Shared.language]}</small>" }?: "") + "\""
    } } }
    val stopsJsArray by remember(waypoints) { derivedStateOf { waypoints.stops.joinToString(",") { "[${it.location.lat}, ${it.location.lng}]" } } }
    val pathsJsArray by remember(waypoints) { derivedStateOf { waypoints.simplifiedPaths.joinToString(",") { path -> "[" + path.joinToString(separator = ",") { "[${it.lat},${it.lng}]" } + "]" } } }
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
        else -> "bus_kmb.svg"
    } }
    val anchor = remember { if (waypoints.co.isTrain) Offset(0.5F, 0.5F) else Offset(0.5F, 1.0F) }
    val clearness = remember { pathColor.closenessTo(Color(0xFFFDE293)) }
    val (outlineHex, outlineOpacity) = remember { if (clearness > 0.8F) { Color.Blue.toHexString() to ((clearness - 0.8) / 0.05).toFloat() } else null to 0F }

    return remember(waypoints, stopNames, stopsJsArray, pathsJsArray) { derivedStateOf { """
        layer.clearLayers();
        
        var stopIcon = L.icon({
            iconUrl: 'https://data.hkbuseta.com/img/$iconFile',
            iconSize: [30, 30],
            iconAnchor: [${anchor.x * 30F}, ${anchor.y * 30F}]
        });

        var stops = [$stopsJsArray];
        var stopNames = [$stopNames];

        stops.forEach(function(point, index) {
            L.marker(point, {icon: stopIcon})
                .addTo(layer)
                .bindPopup("<div style='text-align: center;'>" + stopNames[index] + "<div>", { offset: L.point(0, -22), closeButton: false })
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
actual fun MapRouteInterface(
    instance: AppActiveContext,
    waypoints: RouteWaypoints,
    stops: ImmutableList<Registry.StopData>,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>,
    alternateStopNameShowing: Boolean,
    alternateStopNames: ImmutableState<ImmutableList<Registry.NearbyStopSearchResult>?>
) {
    KCEFWebContainer {
        val webViewState = rememberWebViewStateWithHTMLData(baseHtml)
        val webViewNavigator = rememberWebViewNavigator()
        val webViewJsBridge = rememberWebViewJsBridge()
        var selectedStop by selectedStopState
        val indexMap by remember(waypoints, stops) { derivedStateOf { waypoints.buildStopListMapping(stops) } }
        val script by rememberLeafletScript(waypoints, alternateStopNameShowing, alternateStopNames)
        val pathColor by ComposeShared.rememberOperatorColor(waypoints.co.getLineColor(waypoints.routeNumber, Color.Red), Operator.CTB.getOperatorColor(Color.Yellow).takeIf { waypoints.isKmbCtbJoint })
        val shouldHide by ScreenState.hasInterruptElement.collectAsStateMultiplatform()
        val scope = rememberCoroutineScope()

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
                    message.params.toIntOrNull()?.apply { scope.launch { selectedStop = indexMap[this@apply] + 1 } }
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
}

@Composable
actual fun MapSelectInterface(
    instance: AppActiveContext,
    initialPosition: Coordinates,
    currentRadius: Float,
    onMove: (Coordinates, Float) -> Unit
) {
    KCEFWebContainer {
        val webViewState = rememberWebViewStateWithHTMLData(baseHtml)
        val webViewNavigator = rememberWebViewNavigator()
        val webViewJsBridge = rememberWebViewJsBridge()
        val shouldHide by ScreenState.hasInterruptElement.collectAsStateMultiplatform()
        var position by remember { mutableStateOf(initialPosition) }
        var init by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect (Unit) {
            webViewJsBridge.register(object : IJsMessageHandler {
                override fun methodName(): String = "MoveCenter"
                override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
                    val parts = message.params.split(",")
                    val pos = Coordinates(parts[0].toDouble(), parts[1].toDouble())
                    scope.launch {
                        position = pos
                        onMove.invoke(pos, parts[2].toFloat())
                    }
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
}