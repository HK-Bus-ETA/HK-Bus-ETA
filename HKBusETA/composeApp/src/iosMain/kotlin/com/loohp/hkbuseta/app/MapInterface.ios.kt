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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.zIndex
import co.touchlab.stately.concurrency.Lock
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.common.utils.radians
import com.loohp.hkbuseta.common.utils.withLock
import com.loohp.hkbuseta.compose.LocationOff
import com.loohp.hkbuseta.compose.PlatformFilledTonalIconToggleButton
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.shared.ComposeShared
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.resize
import io.github.alexzhirkevich.cupertino.toUIColor
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.NativePlacement
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.free
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.useContents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import opensavvy.pedestal.weak.ExperimentalWeakApi
import opensavvy.pedestal.weak.WeakMap
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationDistance
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKMapCamera
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKOverlayLevelAboveRoads
import platform.MapKit.MKOverlayProtocol
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPolyline
import platform.MapKit.MKPolylineRenderer
import platform.MapKit.addOverlays
import platform.MapKit.removeOverlays
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.darwin.NSObject
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.sin

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class, ExperimentalWeakApi::class)
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
    var selectedSection by selectedSectionState
    var selectedStop by selectedStopState
    val indexMap by remember(sections) { derivedStateOf { sections.map { s -> s.waypoints.buildStopListMapping(instance, s.stops) } } }
    val pathColors by ComposeShared.rememberOperatorColors(sections.map { s -> s.waypoints.co.getLineColor(s.waypoints.routeNumber, Color.Red) to Operator.CTB.getOperatorColor(Color.Yellow).takeIf { s.waypoints.isKmbCtbJoint } }.asImmutableList())
    val iconNames = remember { sections.map { s ->
        when (s.waypoints.co) {
            Operator.KMB -> when (s.waypoints.routeNumber.getKMBSubsidiary()) {
                KMBSubsidiary.KMB -> if (s.waypoints.isKmbCtbJoint) "bus_jointly_kmb" else "bus_kmb"
                KMBSubsidiary.LWB -> if (s.waypoints.isKmbCtbJoint) "bus_jointly_lwb" else "bus_lwb"
                else -> "bus_kmb"
            }
            Operator.CTB -> "bus_ctb"
            Operator.NLB -> "bus_nlb"
            Operator.GMB -> "minibus"
            Operator.MTR_BUS -> "bus_mtrbus"
            Operator.LRT -> "mtr_stop"
            Operator.MTR -> "mtr_stop"
            Operator.HKKF -> "bus_nlb"
            Operator.SUNFERRY -> "bus_nlb"
            Operator.FORTUNEFERRY -> "bus_nlb"
            else -> "bus_kmb"
        }
    } }
    val shouldShowStopIndex = remember { sections.map { s -> !s.waypoints.co.run { isTrain || isFerry } } }
    val anchors = remember { sections.map { s -> if (s.waypoints.co.isTrain) Offset(0.5F, 0.5F) else Offset(0.5F, 1.0F) } }
    val polylineSectionIndexMap = remember { WeakMap<MKPolyline, Int>() }
    val mapDelegate = remember(indexMap) { MapDelegate(iconNames, pathColors.toMutableList(), anchors, polylineSectionIndexMap) { sectionIndex, stopIndex ->
        selectedSection = sectionIndex
        selectedStop = indexMap[sectionIndex][stopIndex] + 1
    } }
    val map = remember { MKMapView() }
    val lock = remember { Lock() }

    var camera by remember { mutableStateOf(MKMapCamera()) }

    var init by remember { mutableStateOf(false) }
    var hasLocation by remember { mutableStateOf(false) }
    var gpsEnabled by remember { mutableStateOf(false) }

    var annotations: List<MKAnnotationProtocol> by remember { mutableStateOf(emptyList()) }
    var overlays: List<MKPolyline> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect (selectedSection, selectedStop) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(200)
            lock.withLock {
                if (init) {
                    val location = sections[selectedSection].stops[selectedStop - 1].stop.location
                    camera = MKMapCamera()
                    camera.centerCoordinate = location.toAppleCoordinates()
                    camera.altitude = DefaultAltitude
                    map.setCamera(camera, true)

                    val index = indexMap[selectedSection].indexOf(selectedStop - 1)
                    if (index >= 0) {
                        map.selectAnnotation(annotations[index], true)
                    }
                } else {
                    init = true
                }
            }
        }
    }
    LaunchedEffect (gpsEnabled) {
        CoroutineScope(Dispatchers.Main).launch {
            lock.withLock {
                map.showsUserLocation = gpsEnabled
            }
        }
    }
    DisposableEffect (sections, alternateStopNameShowing) {
        val allocated = mutableListOf<CPointer<*>>()
        CoroutineScope(Dispatchers.Main).launch {
            lock.withLock {
                map.removeAnnotations(annotations)
                map.removeOverlays(overlays)
                val annotationList = mutableListOf<MapAnnotation>()
                val overlaysList = mutableListOf<MKPolyline>()
                for ((sectionIndex, section) in sections.withIndex()) {
                    for ((index, stop) in section.waypoints.stops.withIndex()) {
                        val resolvedStop = section.alternateStopNames?.takeIf { alternateStopNameShowing }?.get(index)?.stop?: stop
                        val title = resolvedStop.name[Shared.language]
                        annotationList.add(MapAnnotation(
                            title = if (shouldShowStopIndex[sectionIndex]) "${indexMap[sectionIndex][index] + 1}. $title" else title,
                            subTitle = resolvedStop.remark?.get(Shared.language),
                            sectionIndex = sectionIndex,
                            index = index,
                            location = stop.location
                        ))
                    }
                    for (path in section.waypoints.paths) {
                        val line = MKPolyline.polylineWithCoordinates(path.toAppleCoordinates(nativeHeap).apply { allocated.add(this) }, path.size.convert())
                        polylineSectionIndexMap[line] = sectionIndex
                        overlaysList.add(line)
                    }
                }
                annotations = annotationList
                overlays = overlaysList
                map.addAnnotations(annotations)
                map.addOverlays(overlays, MKOverlayLevelAboveRoads)
            }
        }
        onDispose { allocated.forEach { nativeHeap.free(it) } }
    }
    LaunchedEffect (mapDelegate) {
        CoroutineScope(Dispatchers.Main).launch {
            lock.withLock {
                map.delegate = mapDelegate
                map.removeOverlays(overlays)
                map.addOverlays(overlays, MKOverlayLevelAboveRoads)
            }
        }
    }
    LaunchedEffect (pathColors) {
        CoroutineScope(Dispatchers.Main).launch {
            lock.withLock {
                for ((index, pathColor) in pathColors.withIndex()) {
                    mapDelegate.update(index, pathColor)
                }
            }
        }
    }
    LaunchedEffect (Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            lock.withLock {
                val location = sections[selectedSection].stops[selectedStop - 1].stop.location
                camera.centerCoordinate = location.toAppleCoordinates()
                camera.altitude = DefaultAltitude
                map.setCamera(camera, false)
                checkLocationPermission(instance, true) { hasLocation = it }
            }
        }
    }

    Box {
        if (hasLocation && !gpsEnabled) {
            FilledTonalIconToggleButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(100F)
                    .padding(1.dp),
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
        UIKitView(
            factory = {
                @Suppress("USELESS_CAST")
                map as UIView
            },
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent { if (init) drawContent() },
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true
            )
        )
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
actual fun MapSelectInterface(
    instance: AppActiveContext,
    initialPosition: Coordinates,
    currentRadius: Float,
    onMove: (Coordinates, Float) -> Unit
) {
    val map = remember { MKMapView() }
    val lock = remember { Lock() }
    val camera = remember { MKMapCamera() }
    var hasLocation by remember { mutableStateOf(false) }
    var gpsEnabled by remember { mutableStateOf(false) }
    var lastLocation by remember { mutableStateOf(initialPosition) }
    var lastZoom by remember { mutableFloatStateOf(0F) }
    var init by remember { mutableStateOf(false) }

    LaunchedEffect (gpsEnabled) {
        CoroutineScope(Dispatchers.Main).launch {
            lock.withLock {
                map.showsUserLocation = gpsEnabled
            }
        }
    }
    LaunchedEffect (Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(200)
            lock.withLock {
                camera.centerCoordinate = initialPosition.toAppleCoordinates()
                camera.altitude = DefaultAltitude
                map.setCamera(camera, false)
                checkLocationPermission(instance, true) { hasLocation = it }
                init = true
            }
        }
    }
    LaunchedEffect (initialPosition) {
        CoroutineScope(Dispatchers.Main).launch {
            lock.withLock {
                if (init) {
                    camera.centerCoordinate = initialPosition.toAppleCoordinates()
                    camera.altitude = DefaultAltitude
                    map.setCamera(camera, true)
                }
            }
        }
    }
    LaunchedEffect (Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                if (init) {
                    val newLocation = map.centerCoordinate.useContents { Coordinates(latitude, longitude) }
                    val newZoom = map.zoom.toFloat()
                    if (lastLocation != newLocation || lastZoom != newZoom) {
                        onMove.invoke(newLocation, newZoom)
                        lastLocation = newLocation
                        lastZoom = newZoom
                    }
                }
                delay(250)
            }
        }
    }

    Box {
        if (hasLocation && !gpsEnabled) {
            PlatformFilledTonalIconToggleButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
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
        UIKitView(
            factory = {
                @Suppress("USELESS_CAST")
                map as UIView
            },
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent { if (init) drawContent() },
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true
            )
        )
    }
}

const val DefaultAltitude: CLLocationDistance = 2500.0

@OptIn(ExperimentalForeignApi::class)
inline fun Offset.asAppleMapOffset(image: UIImage): CValue<CGPoint> {
    val (width, height) = image.size.useContents { width to height }
    return CGPointMake((width * 0.5) - (width * x), (height * 0.5) - (height * y))
}

@OptIn(ExperimentalForeignApi::class)
class MapDelegate(
    private val iconName: List<String>,
    private val color: MutableList<Color>,
    private val anchor: List<Offset>,
    private val polylineSectionIndexMap: WeakMap<MKPolyline, Int>,
    private val callback: (Int, Int) -> Unit,
): NSObject(), MKMapViewDelegateProtocol {

    private val polylineRenderers = List(iconName.size) { mutableSetOf<MKPolylineRenderer>() }

    fun update(sectionIndex: Int, color: Color = this.color[sectionIndex]) {
        this.color[sectionIndex] = color
        for (renderer in polylineRenderers[sectionIndex]) {
            renderer.strokeColor = color.toUIColor()
            renderer.lineWidth = 5.0
            renderer.setNeedsDisplay()
        }
    }

    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
        if (viewForAnnotation is MapAnnotation) {
            val identifier = "StopMarker"
            val sectionIndex = viewForAnnotation.sectionIndex()
            val annotationView: MKAnnotationView
            val dequeuedAnnotationView = mapView.dequeueReusableAnnotationViewWithIdentifier(identifier)
            if (dequeuedAnnotationView == null) {
                annotationView = MKAnnotationView(viewForAnnotation, identifier)
            } else {
                annotationView = dequeuedAnnotationView
                annotationView.annotation = viewForAnnotation
            }
            annotationView.canShowCallout = true
            UIImage.imageNamed(iconName[sectionIndex])?.resize(36.0)?.apply {
                annotationView.image = this
                annotationView.centerOffset = anchor[sectionIndex].asAppleMapOffset(this)
            }
            return annotationView
        }
        return null
    }

    override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
        val annotation = (didSelectAnnotationView.annotation as? MapAnnotation)?: return
        callback.invoke(annotation.sectionIndex(), annotation.index())
    }

    override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
        val polyline = rendererForOverlay as? MKPolyline
        if (polyline != null) {
            val sectionIndex = polylineSectionIndexMap[polyline]
            if (sectionIndex != null) {
                val renderer = MKPolylineRenderer(polyline)
                renderer.strokeColor = color[sectionIndex].toUIColor()
                renderer.lineWidth = 5.0
                renderer.setNeedsDisplay()
                polylineRenderers[sectionIndex].add(renderer)
                return renderer
            }
        }
        return MKOverlayRenderer(rendererForOverlay)
    }

}

@ExperimentalForeignApi
class MapAnnotation(
    title: String? = null,
    subTitle: String? = null,
    private val sectionIndex: Int,
    private val index: Int,
    private val location: Coordinates
): NSObject(), MKAnnotationProtocol {

    private val titleInternal = title
    private val subTitleInternal = subTitle
    private val appleCoordinates = location.toAppleCoordinates()

    fun sectionIndex(): Int {
        return sectionIndex
    }

    fun index(): Int {
        return index
    }

    override fun title(): String? {
        return titleInternal
    }

    override fun subtitle(): String? {
        return subTitleInternal
    }

    fun location(): Coordinates {
        return location
    }

    override fun coordinate(): CValue<CLLocationCoordinate2D> {
        return appleCoordinates
    }

}

@OptIn(ExperimentalForeignApi::class)
inline fun Coordinates.toAppleCoordinates(): CValue<CLLocationCoordinate2D> {
    return CLLocationCoordinate2DMake(lat, lng)
}

@OptIn(ExperimentalForeignApi::class)
inline fun List<Coordinates>.toAppleCoordinates(placement: NativePlacement): CPointer<CLLocationCoordinate2D> {
    return placement.allocArray<CLLocationCoordinate2D>(size).also { array ->
        forEachIndexed { index, coordinates -> array[index].apply {
            this.latitude = coordinates.lat
            this.longitude = coordinates.lng
        } }
    }
}

@OptIn(ExperimentalForeignApi::class)
inline val MKMapView.zoom: Double get() {
    var angleCamera = camera.heading
    if (angleCamera > 270.0) {
        angleCamera = 360.0 - angleCamera
    } else if (angleCamera > 90.0) {
        angleCamera = (angleCamera - 180.0).absoluteValue
    }
    val angleRad = angleCamera.radians
    val (width, height) = frame.useContents { size.width to size.height }
    val spanStraight = width * region.useContents { span.longitudeDelta } / (width * cos(angleRad) + height * sin(angleRad))
    return log2(360.0 * ((width / 128.0) / spanStraight)) - 1.0
}

actual val isMapOverlayAlwaysOnTop: Boolean = false
