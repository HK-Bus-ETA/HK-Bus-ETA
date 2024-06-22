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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import com.loohp.hkbuseta.appcontext.ScreenState
import com.loohp.hkbuseta.appcontext.isDarkMode
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
import com.loohp.hkbuseta.common.utils.Stable
import com.loohp.hkbuseta.compose.LanguageDarkModeChangeEffect
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.shared.ComposeShared
import com.loohp.hkbuseta.utils.closenessTo
import com.loohp.hkbuseta.utils.getLineColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.toHexString
import kotlinx.collections.immutable.ImmutableList


@OptIn(ExperimentalStdlibApi::class)
@Composable
actual fun MapRouteInterface(
    instance: AppActiveContext,
    waypoints: RouteWaypoints,
    stops: ImmutableList<Registry.StopData>,
    selectedStopState: MutableIntState,
    selectedBranchState: MutableState<Route>
) {
    val shouldHide by ScreenState.hasInterruptElement.collectAsStateMultiplatform()

    val stopNames by remember(waypoints) { derivedStateOf { waypoints.stops.joinToString("\u0000") { "<b>" + it.name[Shared.language] + "</b>" + (it.remark?.let { r -> "<br><small>${r[Shared.language]}</small>" }?: "") } } }
    val stopsJsArray by remember(waypoints) { derivedStateOf { waypoints.stops.joinToString("\u0000") { "${it.location.lat}\u0000${it.location.lng}" } } }
    val pathsJsArray by remember(waypoints) { derivedStateOf { waypoints.simplifiedPaths.joinToString("\u0000") { path -> path.joinToString(separator = "|") { "${it.lat}|${it.lng}" } } } }
    val pathColor by ComposeShared.rememberOperatorColor(waypoints.co.getLineColor(waypoints.routeNumber, Color.Red), Operator.CTB.getOperatorColor(Color.Yellow).takeIf { waypoints.isKmbCtbJoint })
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
    var selectedStop by selectedStopState
    val indexMap by remember(waypoints, stops) { derivedStateOf { waypoints.buildStopListMapping(stops) } }

    val webMap = rememberWebMap(Shared.language, Shared.theme.isDarkMode)

    LaunchedEffect (waypoints) {
        webMap.show()
        val colorHex = pathColor.toHexString()
        val clearness = pathColor.closenessTo(Color(0xFFFDE293))
        val (outlineHex, outlineOpacity) = if (clearness > 0.8F) { Color.Blue.toHexString() to ((clearness - 0.8) / 0.05).toFloat() } else null to 0F
        webMap.updateMarkings(stopsJsArray, stopNames, pathsJsArray, colorHex, 1F, outlineHex, outlineOpacity, "assets/$iconFile", anchor.x, anchor.y) {
            selectedStop = indexMap[it] + 1
        }
    }
    LaunchedEffect (pathColor) {
        val colorHex = pathColor.toHexString()
        val clearness = pathColor.closenessTo(Color(0xFFFDE293))
        val (outlineHex, outlineOpacity) = if (clearness > 0.8F) { Color.Blue.toHexString() to ((clearness - 0.8) / 0.05).toFloat() } else null to 0F
        webMap.updateLineColor(colorHex, 1F, outlineHex, outlineOpacity)
    }
    LaunchedEffect (selectedStop) {
        webMap.show()
        val location = stops[selectedStop - 1].stop.location
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
        webMap.reloadTiles(language, darkMode)
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

    val webMap = rememberWebMap(Shared.language, Shared.theme.isDarkMode)

    LaunchedEffect (Unit) {
        webMap.show()
        webMap.startSelect(initialPosition.lat, initialPosition.lng, currentRadius) { lat, lng, zoom ->
            val pos = Coordinates(lat, lng)
            position = pos
            onMove.invoke(position, zoom.toFloat())
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
        webMap.reloadTiles(language, darkMode)
    }

    WebMapContainer(webMap)
}

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun rememberWebMap(language: String, darkMode: Boolean): WebMap = remember { WebMap(language, darkMode) }

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

@Stable
external class WebMap(language: String, darkMode: Boolean): JsAny {
    val valid: Boolean
    fun reloadTiles(language: String, darkMode: Boolean)
    fun remove()
    fun setMapPosition(x: Float, y: Float, width: Float, height: Float)
    fun show()
    fun hide()
    fun startSelect(lat: Double, lng: Double, radius: Float, onMoveCallback: (Double, Double, Double) -> Unit)
    fun flyToSelect(lat: Double, lng: Double)
    fun updateSelect(lat: Double, lng: Double, radius: Float)
    fun updateMarkings(stopsJsArray: String, stopNamesJsArray: String, pathsJsArray: String, colorHex: String, opacity: Float, outlineHex: String?, outlineOpacity: Float, iconFile: String, anchorX: Float, anchorY: Float, selectStopCallback: (Int) -> Unit)
    fun updateLineColor(colorHex: String, opacity: Float, outlineHex: String?, outlineOpacity: Float)
    fun mapFlyTo(lat: Double, lng: Double)
}