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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import com.loohp.hkbuseta.appcontext.ScreenState
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.isFerry
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.Stable
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.compose.ChangedEffect
import com.loohp.hkbuseta.compose.LanguageDarkModeChangeEffect
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.platformBackgroundColor
import com.loohp.hkbuseta.shared.ComposeShared
import com.loohp.hkbuseta.utils.closenessTo
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.toHexString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch


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
    val shouldHide by ScreenState.hasInterruptElement.collectAsStateMultiplatform()

    val stopNames by remember(sections, alternateStopNameShowing) { derivedStateOf { sections.map { s -> s.waypoints.stops.mapIndexed { index, stop -> index to stop }.joinToString("\u0000") { (index, stop) ->
        val resolvedStop = s.alternateStopNames?.takeIf { alternateStopNameShowing }?.get(index)?.stop?: stop
        "<b>" + resolvedStop.name[Shared.language] + "</b>" + (resolvedStop.remark?.let { r -> "<br><small>${r[Shared.language]}</small>" }?: "")
    } } } }
    val stopsJsArrays by remember(sections) { derivedStateOf { sections.map { s -> s.waypoints.stops.joinToString("\u0000") { "${it.location.lat}\u0000${it.location.lng}" } } } }
    val pathsJsArrays by remember(sections) { derivedStateOf { sections.map { s -> s.waypoints.paths.joinToString("\u0000") { path -> path.joinToString(separator = "|") { "${it.lat}|${it.lng}" } } } } }
    val pathColors by ComposeShared.rememberOperatorColors(sections.map { s -> s.waypoints.co.getLineColor(s.waypoints.routeNumber, Color.Red) to Operator.CTB.getOperatorColor(Color.Yellow).takeIf { s.waypoints.isKmbCtbJoint } }.asImmutableList())
    val iconFiles = remember { sections.map { s -> when (s.waypoints.co) {
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
    } } }
    val anchors = remember { sections.map { s -> if (s.waypoints.co.isTrain) Offset(0.5F, 0.5F) else Offset(0.5F, 1.0F) } }
    var selectedStop by selectedStopState
    var selectedSection by selectedSectionState
    val indexMap by remember(sections) { derivedStateOf { sections.map { s -> s.waypoints.buildStopListMapping(instance, s.stops) } } }
    val scope = rememberCoroutineScope()
    val backgroundColor = platformBackgroundColor
    var sizeToggle by sizeToggleState

    val webMap = rememberWebMap(
        language = Shared.language,
        darkMode = Shared.theme.isDarkMode,
        backgroundColor = backgroundColor.toArgb(),
        sizeToggleCallback = { sizeToggle = it }
    )

    LaunchedEffect (useSizeToggle, sizeToggle) {
        webMap.setUseSizeToggleContainer(useSizeToggle, sizeToggle)
    }

    LaunchedEffect (sections, stopNames) {
        webMap.show()
        webMap.clearMarkings()
        for ((index, section) in sections.withIndex()) {
            val colorHex = pathColors[index].toHexString()
            val clearness = pathColors[index].closenessTo(Color(0xFFFDE293))
            val (outlineHex, outlineOpacity) = if (clearness > 0.8F) { Color.Blue.toHexString() to ((clearness - 0.8) / 0.05).toFloat() } else null to 0F
            webMap.addMarkings(stopsJsArrays[index], stopNames[index], pathsJsArrays[index], colorHex, 1F, outlineHex, outlineOpacity, "assets/${iconFiles[index]}", anchors[index].x, anchors[index].y, indexMap[index].joinToString(separator = ","), !section.waypoints.co.run { isTrain || isFerry }) {
                scope.launch {
                    selectedSection = index
                    selectedStop = indexMap[index][it] + 1
                }
            }
        }
    }
    LaunchedEffect (pathColors) {
        for ((index, pathColor) in pathColors.withIndex()) {
            val colorHex = pathColor.toHexString()
            val clearness = pathColor.closenessTo(Color(0xFFFDE293))
            val (outlineHex, outlineOpacity) = if (clearness > 0.8F) { Color.Blue.toHexString() to ((clearness - 0.8) / 0.05).toFloat() } else null to 0F
            webMap.updateLineColor(index, colorHex, 1F, outlineHex, outlineOpacity)
        }
    }
    LaunchedEffect (selectedSection, selectedStop) {
        webMap.show()
        val location = sections[selectedSection].stops[selectedStop - 1].stop.location
        webMap.mapFlyTo(location.lat, location.lng)
    }
    DisposableEffect (Unit) {
        onDispose { webMap.remove() }
    }
    LaunchedEffect (shouldHide) {
        if (shouldHide) {
            webMap.hide()
        } else {
            webMap.show()
        }
    }
    LanguageDarkModeChangeEffect { language, darkMode ->
        webMap.reloadTiles(language, darkMode, backgroundColor.toArgb())
    }
    ChangedEffect (selectedSection, selectedStop) {
        val index = indexMap[selectedSection].indexOf(selectedStop - 1)
        if (index >= 0) {
            webMap.showMarker(selectedSection, index)
        }
    }

    WebMapContainer(webMap)
}

@Composable
actual fun MapSelectInterface(
    instance: AppActiveContext,
    initialPosition: Coordinates,
    currentRadius: Float,
    onMove: (Coordinates, Float) -> Unit
) {
    val shouldHide by ScreenState.hasInterruptElement.collectAsStateMultiplatform()
    var position by remember { mutableStateOf(initialPosition) }
    var init by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val backgroundColor = platformBackgroundColor

    val webMap = rememberWebMap(
        language = Shared.language,
        darkMode = Shared.theme.isDarkMode,
        backgroundColor = backgroundColor.toArgb(),
        sizeToggleCallback = { /* do nothing */ }
    )

    LaunchedEffect (Unit) {
        webMap.show()
        webMap.startSelect(initialPosition.lat, initialPosition.lng, currentRadius) { lat, lng, zoom ->
            scope.launch {
                val pos = Coordinates(lat, lng)
                position = pos
                onMove.invoke(position, zoom.toFloat())
            }
        }
        init = true
    }
    LaunchedEffect (position, currentRadius) {
        webMap.show()
        webMap.updateSelect(position.lat, position.lng, currentRadius * 1000F)
    }
    LaunchedEffect (initialPosition) {
        if (init) {
            webMap.flyToSelect(initialPosition.lat, initialPosition.lng)
        }
    }
    DisposableEffect (Unit) {
        onDispose { webMap.remove() }
    }
    LaunchedEffect (shouldHide) {
        if (shouldHide) {
            webMap.hide()
        } else {
            webMap.show()
        }
    }
    LanguageDarkModeChangeEffect { language, darkMode ->
        webMap.reloadTiles(language, darkMode, backgroundColor.toArgb())
    }

    WebMapContainer(webMap)
}

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun rememberWebMap(
    language: String,
    darkMode: Boolean,
    backgroundColor: Int,
    noinline sizeToggleCallback: (Boolean) -> Unit
): WebMap = remember { WebMap(language, darkMode, backgroundColor, sizeToggleCallback) }

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun WebMapContainer(webMap: WebMap) {
    val density = LocalDensity.current
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                if (webMap.valid) {
                    val size = it.size
                    val pos = it.positionInRoot()
                    webMap.setMapPosition(
                        x = pos.x / density.density,
                        y = pos.y / density.density,
                        width = size.width.toFloat() / density.density,
                        height = size.height.toFloat() / density.density
                    )
                }
            }
    )
}

actual val isMapOverlayAlwaysOnTop: Boolean = true

@Stable
external class WebMap(
    language: String,
    darkMode: Boolean,
    backgroundColor: Int,
    sizeToggleCallback: (Boolean) -> Unit
): JsAny {
    val valid: Boolean
    fun getMapElementId(): String
    fun setUseSizeToggleContainer(useSizeToggle: Boolean, sizeToggleIsLarge: Boolean)
    fun reloadTiles(language: String, darkMode: Boolean, backgroundColor: Int)
    fun remove()
    fun setMapPosition(x: Float, y: Float, width: Float, height: Float)
    fun show()
    fun hide()
    fun startSelect(lat: Double, lng: Double, radius: Float, onMoveCallback: (Double, Double, Double) -> Unit)
    fun flyToSelect(lat: Double, lng: Double)
    fun updateSelect(lat: Double, lng: Double, radius: Float)
    fun clearMarkings()
    fun addMarkings(stopsJsArray: String, stopNamesJsArray: String, pathsJsArray: String, colorHex: String, opacity: Float, outlineHex: String?, outlineOpacity: Float, iconFile: String, anchorX: Float, anchorY: Float, indexMap: String, shouldShowStopIndex: Boolean, selectStopCallback: (Int) -> Unit)
    fun updateLineColor(sectionIndex: Int, colorHex: String, opacity: Float, outlineHex: String?, outlineOpacity: Float)
    fun mapFlyTo(lat: Double, lng: Double)
    fun showMarker(sectionIndex: Int, stopIndex: Int)
}