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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.StopInfo
import com.loohp.hkbuseta.common.objects.asStop
import com.loohp.hkbuseta.common.objects.firstCo
import com.loohp.hkbuseta.common.objects.toStopIndexed
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.compose.collectAsStateWithLifecycle
import com.loohp.hkbuseta.compose.zoomable.Zoomable
import com.loohp.hkbuseta.compose.zoomable.ZoomableState
import com.loohp.hkbuseta.compose.zoomable.rememberZoomableState
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.coordinatesNullableStateSaver
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonArray
import java.io.InputStream

enum class TrainRouteMapType {

    MTR, LRT;

    companion object {
        fun of(name: String): TrainRouteMapType? {
            return entries.firstOrNull { it.name.equals(name, true) }
        }
    }
}

@Immutable
data class RouteMapData(
    val dimension: Size,
    val stations: Map<String, Offset>
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromInputStream(input: InputStream): RouteMapData {
            val json = Json.decodeFromStream<JsonObject>(input)
            val dimension = json.optJsonObject("properties")!!.optJsonArray("dimension")!!.let { Size(it.optDouble(0).toFloat(), it.optDouble(1).toFloat()) }
            val stops = json.optJsonObject("stops")!!.mapToMutableMap { it.jsonArray.let { a -> Offset(a.optDouble(0).toFloat(), a.optDouble(1).toFloat()) } }
            return RouteMapData(dimension, stops)
        }
    }

    fun findClickedStations(offset: Offset): String? {
        return stations.entries
            .asSequence()
            .map { it to (it.value - offset).getDistanceSquared() }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= 40000 }
            ?.first?.key
    }
}

private val mtrRouteMapDataState: MutableStateFlow<RouteMapData?> = MutableStateFlow(null)
private val lightRailRouteMapDataState: MutableStateFlow<RouteMapData?> = MutableStateFlow(null)

@Composable
fun TrainRouteMapInterface(instance: AppActiveContext, ambientMode: Boolean, type: TrainRouteMapType) {
    HKBusETATheme {
        when (type) {
            TrainRouteMapType.MTR -> MTRRouteMapInterface(instance, ambientMode)
            TrainRouteMapType.LRT -> LRTRouteMapInterface(instance, ambientMode)
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            WearOSShared.MainTime()
        }
    }
}

data class ZoomState(val scale: Float, val translationX: Float, val translationY: Float)

private val mtrRouteMapZoomState: MutableStateFlow<ZoomState> = MutableStateFlow(ZoomState(6F, 0F, 0F))

@Composable
fun MTRRouteMapInterface(instance: AppActiveContext, ambientMode: Boolean) {
    var zoomState by mtrRouteMapZoomState.collectAsStateWithLifecycle()
    val state = rememberZoomableState(
        initialScale = zoomState.scale,
        maxScale = 8.5F,
        initialTranslationX = zoomState.translationX,
        initialTranslationY = zoomState.translationY,
        doubleTapOutScale = 6F,
        doubleTapScale = 7F
    )
    val imageSizeState = remember { mutableStateOf(IntSize(0, 0)) }
    val imageSize by imageSizeState
    var mtrRouteMapData by mtrRouteMapDataState.collectAsStateWithLifecycle()
    var allStops by remember { mutableStateOf(mtrRouteMapData?.let { it.stations.keys.associateWith { s -> s.asStop(instance) } }?: emptyMap()) }
    val closestStopState: MutableState<Map.Entry<String, Stop?>?> = remember { mutableStateOf(null) }
    var closestStop by closestStopState

    var location by rememberSaveable(saver = coordinatesNullableStateSaver) { mutableStateOf(null) }
    var locationJumped by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect (Unit) {
        while (true) {
            val result = getGPSLocation(instance).await()
            if (result?.isSuccess == true) {
                location = result.location!!
            }
            delay(Shared.ETA_UPDATE_INTERVAL.toLong())
        }
    }

    LaunchedEffect (state.scale, state.translationX, state.translationY) {
        zoomState = ZoomState(state.scale, state.translationX, state.translationY)
    }
    LaunchedEffect (Unit) {
        if (mtrRouteMapData == null) {
            instance.context.resources.openRawResource(R.raw.mtr_system_map).use { input ->
                val data = RouteMapData.fromInputStream(input)
                mtrRouteMapData = data
                allStops = data.stations.keys.associateWith { it.asStop(instance) }
            }
        }
    }
    LaunchedEffect (allStops, location) {
        if (location != null && allStops.isNotEmpty()) {
            val stop = allStops.entries.asSequence().filter { it.value != null }.minByOrNull { it.value!!.location.distance(location!!) }
            closestStop = stop
            if (stop != null) {
                if (!locationJumped) {
                    mtrRouteMapData?.let { data ->
                        locationJumped = true
                        val position = data.stations[stop.key]!!
                        val scaleX = data.dimension.width / imageSize.width
                        val scaleY = data.dimension.height / imageSize.height
                        val offset = Offset((position.x / scaleX) - (imageSize.width / 2), (position.y / scaleY) - (imageSize.height / 2))
                        state.animateTranslateTo(-offset)
                    }
                }
            }
        }
    }

    MTRRouteMapMapInterface(instance, state, imageSizeState, closestStopState, ambientMode)
}

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun MTRRouteMapMapInterface(
    instance: AppActiveContext,
    state: ZoomableState,
    imageSizeState: MutableState<IntSize>,
    closestStopState: MutableState<Map.Entry<String, Stop?>?>,
    ambientMode: Boolean
) {
    var imageSize by imageSizeState
    val scope = rememberCoroutineScope()
    val focusRequester = rememberActiveFocusRequester()
    val infiniteTransition = rememberInfiniteTransition(label = "ClosestStationIndicator")
    val animatedClosestStationRadius by infiniteTransition.animateFloat(
        initialValue = 70F,
        targetValue = 90F,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ClosestStationIndicator"
    )
    val mtrRouteMapData by mtrRouteMapDataState.collectAsStateWithLifecycle()
    val typhoonInfo by Registry.getInstance(instance).typhoonInfo.collectAsStateWithLifecycle()
    val closestStop by closestStopState
    val painters = mapOf(
        false to rememberAsyncImagePainter(if (typhoonInfo.isAboveTyphoonSignalNine) R.mipmap.mtr_system_map_watch_typhoon else R.mipmap.mtr_system_map_watch),
        true to rememberAsyncImagePainter(if (typhoonInfo.isAboveTyphoonSignalNine) R.mipmap.mtr_system_map_watch_typhoon_ambient else R.mipmap.mtr_system_map_watch_ambient)
    )
    val transition = updateTransition(
        targetState = ambientMode to painters[ambientMode]!!,
        label = "MTRRouteMapAmbientCrossfade"
    )
    transition.Crossfade(
        modifier = Modifier
            .onRotaryScrollEvent {
                scope.launch {
                    scope.launch {
                        val scale = state.scale + it.verticalScrollPixels / 96
                        if (scale in state.minScale..state.maxScale) {
                            state.animateScaleTo(scale)
                        }
                    }
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        contentKey = { (a) -> a },
    ) { (_, painter) ->
        Box(
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = painter.state is AsyncImagePainter.State.Loading
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(27.scaledSize(instance).dp),
                    color = Color(0xFFF9DE09),
                    strokeWidth = 3.dp,
                    trackColor = Color(0xFF797979),
                    strokeCap = StrokeCap.Round,
                )
            }
            Zoomable(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .drawWithContent { if (painter.state !is AsyncImagePainter.State.Loading) drawContent() },
                state = state
            ) {
                Image(
                    modifier = Modifier
                        .aspectRatio(mtrRouteMapData?.dimension?.run { width / height }?: 1F)
                        .fillMaxSize()
                        .onSizeChanged { imageSize = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                val downTime = currentTimeMillis()
                                val tapTimeout = viewConfiguration.longPressTimeoutMillis
                                val tapPosition = down.position
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val currentTime = currentTimeMillis()
                                    if (event.changes.size != 1) break
                                    if (currentTime - downTime >= tapTimeout) break
                                    val change = event.changes[0]
                                    if ((change.position - tapPosition).getDistance() > viewConfiguration.touchSlop) break
                                    if (change.id == down.id && !change.pressed) {
                                        val offset = change.position
                                        mtrRouteMapData?.let { data ->
                                            val scaleX = data.dimension.width / imageSize.width
                                            val scaleY = data.dimension.height / imageSize.height
                                            val clickedPos = Offset(offset.x * scaleX, offset.y * scaleY)
                                            val stopId = data.findClickedStations(clickedPos)
                                            if (stopId != null) {
                                                change.consume()
                                                scope.launch {
                                                    val stop = stopId.asStop(instance)
                                                    val result = Registry.getInstance(instance).findRoutes("", false) { r ->
                                                        Shared.MTR_ROUTE_FILTER.invoke(r) && r.stops[Operator.MTR]?.contains(stopId) == true
                                                    }.onEach {
                                                        it.stopInfo = StopInfo(stopId, stop, 0.0, Operator.MTR)
                                                    }.toStopIndexed(instance)
                                                    val intent = AppIntent(instance, AppScreen.LIST_ROUTES)
                                                    intent.putExtra("result", result.map { it.strip(); it.serialize() }.toJsonArray().toString())
                                                    intent.putExtra("listType", RouteListType.NORMAL.name)
                                                    intent.putExtra("showEta", true)
                                                    intent.putExtra("mtrSearch", stopId)
                                                    instance.startActivity(intent)
                                                }
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.id == down.id && it.pressed })
                            }
                        }
                        .drawWithContent {
                            drawContent()
                            mtrRouteMapData?.let { data ->
                                val scaleX = data.dimension.width / imageSize.width
                                val scaleY = data.dimension.height / imageSize.height
                                closestStop?.let { closest ->
                                    val stopId = closest.key
                                    val position = data.stations[stopId]
                                    if (position != null) {
                                        val center = Offset(position.x / scaleX, position.y / scaleY)
                                        drawCircle(
                                            color = Color(0xff199fff),
                                            radius = animatedClosestStationRadius,
                                            center = center,
                                            alpha = 0.3F,
                                            style = Fill
                                        )
                                        drawCircle(
                                            color = Color(0xff199fff),
                                            radius = animatedClosestStationRadius,
                                            center = center,
                                            style = Stroke(
                                                width = 3.dp.toPx()
                                            )
                                        )
                                    }
                                }
                            }
                        },
                    painter = painter,
                    contentDescription = if (Shared.language == "en") "MTR System Map" else "港鐵路綫圖"
                )
            }
        }
    }
}

private val lrtRouteMapZoomState: MutableStateFlow<ZoomState> = MutableStateFlow(ZoomState(5F, 0F, 0F))

@Composable
fun LRTRouteMapInterface(instance: AppActiveContext, ambientMode: Boolean) {
    var zoomState by lrtRouteMapZoomState.collectAsStateWithLifecycle()
    val state = rememberZoomableState(
        initialScale = zoomState.scale,
        maxScale = 6.5F,
        initialTranslationX = zoomState.translationX,
        initialTranslationY = zoomState.translationY,
        doubleTapOutScale = 5F,
        doubleTapScale = 6F
    )
    val imageSizeState = remember { mutableStateOf(IntSize(0, 0)) }
    val imageSize by imageSizeState
    var lrtRouteMapData by lightRailRouteMapDataState.collectAsStateWithLifecycle()
    var allStops by remember { mutableStateOf(lrtRouteMapData?.let { it.stations.keys.associateWith { s -> s.asStop(instance) } }?: emptyMap()) }
    val closestStopState: MutableState<Map.Entry<String, Stop?>?> = remember { mutableStateOf(null) }
    var closestStop by closestStopState

    var location by rememberSaveable(saver = coordinatesNullableStateSaver) { mutableStateOf(null) }
    var locationJumped by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect (Unit) {
        while (true) {
            val result = getGPSLocation(instance).await()
            if (result?.isSuccess == true) {
                location = result.location!!
            }
            delay(Shared.ETA_UPDATE_INTERVAL.toLong())
        }
    }

    LaunchedEffect (state.scale, state.translationX, state.translationY) {
        zoomState = ZoomState(state.scale, state.translationX, state.translationY)
    }
    LaunchedEffect (Unit) {
        if (lrtRouteMapData == null) {
            instance.context.resources.openRawResource(R.raw.light_rail_system_map).use { input ->
                val data = RouteMapData.fromInputStream(input)
                lrtRouteMapData = data
                allStops = data.stations.keys.associateWith { it.asStop(instance) }
            }
        }
    }
    LaunchedEffect (allStops, location) {
        if (location != null && allStops.isNotEmpty()) {
            val stop = allStops.entries.asSequence().filter { it.value != null }.minByOrNull { it.value!!.location.distance(location!!) }
            closestStop = stop
            if (stop != null) {
                if (!locationJumped) {
                    lrtRouteMapData?.let { data ->
                        locationJumped = true
                        val position = data.stations[stop.key]!!
                        val scaleX = data.dimension.width / imageSize.width
                        val scaleY = data.dimension.height / imageSize.height
                        val offset = Offset((position.x / scaleX) - (imageSize.width / 2), (position.y / scaleY) - (imageSize.height / 2))
                        state.animateTranslateTo(-offset)
                    }
                }
            }
        }
    }

    LRTRouteMapMapInterface(instance, state, imageSizeState, closestStopState, ambientMode)
}

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun LRTRouteMapMapInterface(
    instance: AppActiveContext,
    state: ZoomableState,
    imageSizeState: MutableState<IntSize>,
    closestStopState: MutableState<Map.Entry<String, Stop?>?>,
    ambientMode: Boolean
) {
    var imageSize by imageSizeState
    val scope = rememberCoroutineScope()
    val focusRequester = rememberActiveFocusRequester()
    val infiniteTransition = rememberInfiniteTransition(label = "ClosestStationIndicator")
    val animatedClosestStationRadius by infiniteTransition.animateFloat(
        initialValue = 70F,
        targetValue = 90F,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ClosestStationIndicator"
    )
    val mtrRouteMapData by lightRailRouteMapDataState.collectAsStateWithLifecycle()
    val closestStop by closestStopState
    val painters = mapOf(
        false to rememberAsyncImagePainter(R.mipmap.light_rail_system_map_watch),
        true to rememberAsyncImagePainter(R.mipmap.light_rail_system_map_watch_ambient)
    )
    val transition = updateTransition(
        targetState = ambientMode to painters[ambientMode]!!,
        label = "LRTRouteMapAmbientCrossfade"
    )
    transition.Crossfade(
        modifier = Modifier
            .onRotaryScrollEvent {
                scope.launch {
                    val scale = state.scale + it.verticalScrollPixels / 96
                    if (scale in state.minScale..state.maxScale) {
                        state.animateScaleTo(scale)
                    }
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        contentKey = { (a) -> a },
    ) { (_, painter) ->
        Box(
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = painter.state is AsyncImagePainter.State.Loading
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(27.scaledSize(instance).dp),
                    color = Color(0xFFF9DE09),
                    strokeWidth = 3.dp,
                    trackColor = Color(0xFF797979),
                    strokeCap = StrokeCap.Round,
                )
            }
            Zoomable(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .drawWithContent { if (painter.state !is AsyncImagePainter.State.Loading) drawContent() },
                state = state
            ) {
                Image(
                    modifier = Modifier
                        .aspectRatio(mtrRouteMapData?.dimension?.run { width / height }?: 1F)
                        .fillMaxSize()
                        .onSizeChanged { imageSize = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                val downTime = currentTimeMillis()
                                val tapTimeout = viewConfiguration.longPressTimeoutMillis
                                val tapPosition = down.position
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val currentTime = currentTimeMillis()
                                    if (event.changes.size != 1) break
                                    if (currentTime - downTime >= tapTimeout) break
                                    val change = event.changes[0]
                                    if ((change.position - tapPosition).getDistance() > viewConfiguration.touchSlop) break
                                    if (change.id == down.id && !change.pressed) {
                                        val offset = change.position
                                        mtrRouteMapData?.let { data ->
                                            val scaleX = data.dimension.width / imageSize.width
                                            val scaleY = data.dimension.height / imageSize.height
                                            val clickedPos = Offset(offset.x * scaleX, offset.y * scaleY)
                                            val stopId = data.findClickedStations(clickedPos)
                                            if (stopId != null) {
                                                change.consume()
                                                scope.launch {
                                                    val stop = stopId.asStop(instance)
                                                    val result = Registry.getInstance(instance).findRoutes("", false) { r ->
                                                        r.co.firstCo() == Operator.LRT && r.stops[Operator.LRT]?.contains(stopId) == true
                                                    }.onEach {
                                                        it.stopInfo = StopInfo(stopId, stop, 0.0, Operator.LRT)
                                                    }.toStopIndexed(instance)
                                                    val intent = AppIntent(instance, AppScreen.LIST_ROUTES)
                                                    intent.putExtra("result", result.map { it.strip(); it.serialize() }.toJsonArray().toString())
                                                    intent.putExtra("listType", RouteListType.NORMAL.name)
                                                    intent.putExtra("showEta", true)
                                                    intent.putExtra("mtrSearch", stopId)
                                                    instance.startActivity(intent)
                                                }
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.id == down.id && it.pressed })
                            }
                        }
                        .drawWithContent {
                            drawContent()
                            mtrRouteMapData?.let { data ->
                                val scaleX = data.dimension.width / imageSize.width
                                val scaleY = data.dimension.height / imageSize.height
                                closestStop?.let { closest ->
                                    val stopId = closest.key
                                    val position = data.stations[stopId]
                                    if (position != null) {
                                        val center = Offset(position.x / scaleX, position.y / scaleY)
                                        drawCircle(
                                            color = Color(0xff199fff),
                                            radius = animatedClosestStationRadius,
                                            center = center,
                                            alpha = 0.3F,
                                            style = Fill
                                        )
                                        drawCircle(
                                            color = Color(0xff199fff),
                                            radius = animatedClosestStationRadius,
                                            center = center,
                                            style = Stroke(
                                                width = 3.dp.toPx()
                                            )
                                        )
                                    }
                                }
                            }
                        },
                    painter = painter,
                    contentDescription = if (Shared.language == "en") "Light Rail Route Map" else "輕鐵路綫圖"
                )
            }
        }
    }
}