/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2026. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2026. Contributors
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
import android.graphics.Point
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalDensity
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
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
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
import com.loohp.hkbuseta.utils.ProjectedRoutePoint
import com.loohp.hkbuseta.utils.ProjectedScreenBounds
import com.loohp.hkbuseta.utils.ProjectedScreenPoint
import com.loohp.hkbuseta.utils.ROUTE_ARROW_COLLISION_DISTANCE
import com.loohp.hkbuseta.utils.ROUTE_ARROW_SPACING
import com.loohp.hkbuseta.utils.ROUTE_ARROW_STOP_CLEARANCE
import com.loohp.hkbuseta.utils.RouteDirectionArrow
import com.loohp.hkbuseta.utils.calculateRouteDirectionArrows
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.closenessTo
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.hasGooglePlayService
import com.loohp.hkbuseta.utils.isHuaweiDevice
import com.loohp.hkbuseta.utils.pathsInRouteDirection
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import com.google.android.gms.maps.GoogleMap as NativeGoogleMap


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
    alternateStopNameShowing: Boolean,
    useSizeToggle: Boolean,
    sizeToggleState: MutableState<Boolean>
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
    var nativeMap by remember { mutableStateOf<NativeGoogleMap?>(null) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    var directionArrows by remember { mutableStateOf<List<List<GoogleMapRouteDirectionArrow>>>(emptyList()) }
    val density = LocalDensity.current.density
    val pathColors by ComposeShared.rememberOperatorColors(sections.map { section ->
        section.waypoints.co.getLineColor(section.waypoints.routeNumber, Color.Red) to
            Operator.CTB.getOperatorColor(Color.Yellow).takeIf { section.waypoints.isKmbCtbJoint }
    }.asImmutableList())
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
    LaunchedEffect(cameraPositionState.isMoving, sections, nativeMap, mapSize, density) {
        if (!cameraPositionState.isMoving && mapSize != IntSize.Zero) {
            directionArrows = nativeMap?.let { map ->
                runCatching { calculateGoogleMapDirectionArrows(sections, map, mapSize, density) }.getOrDefault(emptyList())
            } ?: emptyList()
        }
    }

    Box(modifier = Modifier.onGloballyPositioned { mapSize = it.size }) {
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
            @OptIn(MapsComposeExperimentalApi::class)
            MapEffect(Unit) { nativeMap = it }
            for ((index, section) in sections.withIndex()) {
                WaypointPaths(
                    waypoints = section.waypoints,
                    pathColor = pathColors[index]
                )
                RouteDirectionArrowOverlays(
                    arrows = directionArrows.getOrNull(index).orEmpty(),
                    color = pathColors[index]
                )
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
            val resolvedStop = alternateStopNames.value?.takeIf { alternateStopNameShowing }?.getOrNull(i)?.stop?: stop
            val title = resolvedStop.name[Shared.language]
            val remark = resolvedStop.remark?.get(Shared.language)
            val markerState = rememberStopMarkerState(stop)
            ChangedEffect (selectedSection, selectedStop) {
                if (selectedSection == sectionIndex && selectedStop == stopIndex) {
                    markerState.showInfoWindow()
                }
            }
            Marker(
                state = markerState,
                title = if (shouldShowStopIndex) "${stopIndex}. $title" else title,
                snippet = remark,
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
    } catch (_: Throwable) {
        instance.logFirebaseEvent("stop_marker_crash_v2_${waypoints.co.name}_${waypoints.routeNumber}", AppBundle())
        null
    }
}

@Composable
@GoogleMapComposable
fun WaypointPaths(waypoints: RouteWaypoints, pathColor: Color) {
    val outlineColor = routeContrastOutlineColor(pathColor)
    for (lines in waypoints.paths) {
        if (outlineColor != null) {
            Polyline(
                points = lines.toGoogleLatLng(),
                color = outlineColor,
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
@GoogleMapComposable
private fun RouteDirectionArrowOverlays(arrows: List<GoogleMapRouteDirectionArrow>, color: Color) {
    val outlineColor = routeContrastOutlineColor(color)
    for (arrow in arrows) {
        key(arrow.location.lat, arrow.location.lng, arrow.rotation) {
            Polygon(
                points = arrow.points,
                fillColor = color,
                strokeColor = outlineColor ?: color,
                strokeWidth = if (outlineColor == null) 1F else 2F,
                zIndex = 2.5F,
                clickable = false
            )
        }
    }
}

@Composable
private fun routeContrastOutlineColor(pathColor: Color): Color? {
    if (Shared.theme.isDarkMode) return null
    val closeness = max(
        pathColor.closenessTo(Color(0xFFFDE293)),
        pathColor.closenessTo(Color(0xFFAAD4FF))
    )
    if (closeness <= 0.8F) return null
    return Color.Blue.withAlpha(
        (((closeness - 0.8) / 0.05) * 255).roundToInt().coerceIn(0, 255)
    )
}

private data class GoogleMapRouteDirectionArrow(
    val location: Coordinates,
    val rotation: Float,
    val points: List<LatLng>
)

private fun calculateGoogleMapDirectionArrows(
    sections: ImmutableList<MapRouteSection>,
    map: NativeGoogleMap,
    mapSize: IntSize,
    density: Float
): List<List<GoogleMapRouteDirectionArrow>> {
    val projection = map.projection
    val occupied = mutableListOf<ProjectedScreenPoint>()
    val bounds =
        ProjectedScreenBounds(0.0, 0.0, mapSize.width.toDouble(), mapSize.height.toDouble())
    val allStops = sections.flatMap { section ->
        section.waypoints.stops.map { stop ->
            val point = projection.toScreenLocation(stop.location.toGoogleLatLng())
            ProjectedScreenPoint(point.x.toDouble(), point.y.toDouble())
        }
    }
    return sections.map { section ->
        val paths = section.waypoints.pathsInRouteDirection().map { path ->
            path.map { location ->
                val point = projection.toScreenLocation(location.toGoogleLatLng())
                ProjectedRoutePoint(location, point.x.toDouble(), point.y.toDouble())
            }
        }
        val arrows = calculateRouteDirectionArrows(
            paths = paths,
            stops = allStops,
            bounds = bounds,
            occupiedArrowPoints = occupied,
            spacing = ROUTE_ARROW_SPACING * density,
            collisionDistance = ROUTE_ARROW_COLLISION_DISTANCE * density,
            stopClearance = ROUTE_ARROW_STOP_CLEARANCE * density
        )
        occupied += arrows.map { ProjectedScreenPoint(it.x, it.y) }
        arrows.map { arrow ->
            GoogleMapRouteDirectionArrow(
                location = arrow.location,
                rotation = arrow.rotation,
                points = createGoogleMapDirectionArrowPoints(arrow, projection, 14F * density)
            )
        }
    }
}

private fun createGoogleMapDirectionArrowPoints(
    arrow: RouteDirectionArrow,
    projection: com.google.android.gms.maps.Projection,
    size: Float
): List<LatLng> {
    val radians = arrow.rotation * PI / 180.0
    val cosine = cos(radians)
    val sine = sin(radians)
    return listOf(
        0.0 to -0.4333,
        0.4 to 0.4333,
        0.0 to 0.2417,
        -0.4 to 0.4333
    ).map { (relativeX, relativeY) ->
        val x = relativeX * size
        val y = relativeY * size
        projection.fromScreenLocation(
            Point(
                (arrow.x + x * cosine - y * sine).roundToInt(),
                (arrow.y + x * sine + y * cosine).roundToInt()
            )
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
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
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

        .route-direction-arrow {
            background: transparent;
            border: 0;
            pointer-events: none;
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

        map.createPane('routeDirections');
        map.getPane('routeDirections').style.zIndex = 450;
        map.getPane('routeDirections').style.pointerEvents = 'none';
        
        var stopMarkers = [];
        
        var polylines = [];
        var polylinesOutline = [];
        var routeArrowSections = [];
        var routeArrowMarkers = [];
        var routeArrowUpdateTimer = null;

        function updateRouteDirectionArrows() {
            routeArrowMarkers.forEach(function(marker) { layer.removeLayer(marker); });
            routeArrowMarkers = [];
            var occupied = [];
            var allStops = [];
            routeArrowSections.forEach(function(section) {
                section.stops.forEach(function(stop) { allStops.push(map.latLngToContainerPoint(stop)); });
            });
            var size = map.getSize();

            routeArrowSections.forEach(function(section) {
                var segments = [];
                var totalLength = 0;
                section.paths.forEach(function(path) {
                    for (var i = 1; i < path.length; i++) {
                        var startPoint = map.latLngToContainerPoint(path[i - 1]);
                        var endPoint = map.latLngToContainerPoint(path[i]);
                        var length = startPoint.distanceTo(endPoint);
                        if (Number.isFinite(length) && length > 0) {
                            segments.push({ start: path[i - 1], end: path[i], startPoint: startPoint, endPoint: endPoint, length: length });
                            totalLength += length;
                        }
                    }
                });
                if (totalLength < 40) return;

                var traversed = 0;
                var nextDistance = totalLength < 96 ? totalLength / 2 : 48;
                var lastDistance = totalLength < 96 ? nextDistance : totalLength - 48;
                var visibleCount = 0;
                var opposingPhaseShifted = false;
                for (var s = 0; s < segments.length && visibleCount < 24; s++) {
                    var segment = segments[s];
                    var segmentEnd = traversed + segment.length;
                    while (nextDistance <= segmentEnd && nextDistance <= lastDistance && visibleCount < 24) {
                        var fraction = Math.max(0, Math.min(1, (nextDistance - traversed) / segment.length));
                        var x = segment.startPoint.x + (segment.endPoint.x - segment.startPoint.x) * fraction;
                        var y = segment.startPoint.y + (segment.endPoint.y - segment.startPoint.y) * fraction;
                        var rotation = (Math.atan2(segment.endPoint.y - segment.startPoint.y, segment.endPoint.x - segment.startPoint.x) * 180 / Math.PI + 450) % 360;
                        var conflicts = occupied.map(function(arrow) { return { arrow: arrow, distance: arrow.point.distanceTo([x, y]) }; }).filter(function(conflict) { return conflict.distance < 48; });
                        var hardOpposingOverlap = conflicts.some(function(conflict) { return angleDifference(conflict.arrow.rotation, rotation) >= 120 && conflict.distance < 14; });
                        if (hardOpposingOverlap && !opposingPhaseShifted && nextDistance + 48 <= lastDistance) {
                            nextDistance += 48;
                            opposingPhaseShifted = true;
                            continue;
                        }
                        var clearOfStops = allStops.every(function(point) { return point.distanceTo([x, y]) >= 24; });
                        var clearOfArrows = conflicts.every(function(conflict) { return angleDifference(conflict.arrow.rotation, rotation) >= 120 && conflict.distance >= 14; });
                        if (x >= 0 && y >= 0 && x <= size.x && y <= size.y && clearOfStops && clearOfArrows) {
                            var location = [
                                segment.start[0] + (segment.end[0] - segment.start[0]) * fraction,
                                segment.start[1] + (segment.end[1] - segment.start[1]) * fraction
                            ];
                            var icon = L.divIcon({
                                className: 'route-direction-arrow',
                                iconSize: [14, 14],
                                iconAnchor: [7, 7],
                                html: '<svg width="14" height="14" viewBox="0 0 12 12" style="transform:rotate(' + rotation + 'deg)"><path d="M6 0.8 L10.8 11.2 L6 8.9 L1.2 11.2 Z" fill="' + section.color + '" stroke="' + (section.outlineColor || section.color) + '" stroke-opacity="' + (section.outlineColor ? section.outlineOpacity : 1) + '" stroke-width="0.8" stroke-linejoin="round" paint-order="stroke fill"/></svg>'
                            });
                            routeArrowMarkers.push(L.marker(location, { icon: icon, pane: 'routeDirections', interactive: false, keyboard: false }).addTo(layer));
                            occupied.push({ point: L.point(x, y), rotation: rotation });
                            visibleCount++;
                        }
                        nextDistance += 96;
                    }
                    traversed = segmentEnd;
                }
            });
        }

        function scheduleRouteDirectionArrowUpdate() {
            clearTimeout(routeArrowUpdateTimer);
            routeArrowUpdateTimer = setTimeout(updateRouteDirectionArrows, 50);
        }

        function angleDifference(first, second) {
            var difference = Math.abs(first - second) % 360;
            return Math.min(difference, 360 - difference);
        }

        map.on('moveend zoomend', scheduleRouteDirectionArrowUpdate);
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
            s.waypoints.pathsInRouteDirection().joinToString(",") { path -> "[" + path.joinToString(separator = ",") { "[${it.lat},${it.lng}]" } + "]" }
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
        routeArrowSections = paths.map(function(sectionPaths, sectionIndex) { return { paths: sectionPaths, stops: stops, color: colorHexes[sectionIndex], outlineColor: outlineHexOpacity[sectionIndex][0], outlineOpacity: outlineHexOpacity[sectionIndex][1] }; });
        
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
        scheduleRouteDirectionArrowUpdate();
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
                            if (routeArrowSections[$index]) {
                                routeArrowSections[$index].color = '$colorHex';
                                routeArrowSections[$index].outlineColor = ${if (outlineHex == null) "null" else "'$outlineHex'"};
                                routeArrowSections[$index].outlineOpacity = $outlineOpacity;
                                scheduleRouteDirectionArrowUpdate();
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
            DisposableEffect (indexMap) {
                val handler = object : IJsMessageHandler {
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
                }
                webViewJsBridge.register(handler)
                onDispose { webViewJsBridge.unregister(handler) }
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
                        
                        L.tileLayer('$darkMode' === 'true' ? 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_nolabels/{z}/{x}/{y}.png?key=cb1_2hza_1_5548584f4b723493af41eb95' : 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/rastertiles/voyager_nolabels/{z}/{x}/{y}.png?key=cb1_2hza_1_5548584f4b723493af41eb95', {
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
                        
                        L.tileLayer('$darkMode' === 'true' ? 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_nolabels/{z}/{x}/{y}.png?key=cb1_2hza_1_5548584f4b723493af41eb95' : 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/rastertiles/voyager_nolabels/{z}/{x}/{y}.png?key=cb1_2hza_1_5548584f4b723493af41eb95', {
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
