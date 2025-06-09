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

import android.content.Context
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntSize
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.SizeResolver
import com.github.panpf.zoomimage.compose.rememberZoomState
import com.github.panpf.zoomimage.compose.zoom.zoom
import com.github.panpf.zoomimage.subsampling.ImageInfo
import com.github.panpf.zoomimage.subsampling.ImageSource
import com.github.panpf.zoomimage.subsampling.fromByteArray
import com.github.panpf.zoomimage.subsampling.size
import com.github.panpf.zoomimage.util.IntSizeCompat
import com.github.panpf.zoomimage.zoom.ScalesCalculator
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
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.compose.collectAsStateWithLifecycle
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.coordinatesNullableStateSaver
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.predefined
import com.loohp.hkbuseta.utils.realPointToContentPoint
import com.loohp.hkbuseta.utils.realPointToTouchPoint
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.touchPointToRealPoint
import com.loohp.hkbuseta.utils.unrestrictedBitmapSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonArray
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap


enum class TrainRouteMapType {

    MTR, LRT;

    companion object {
        fun of(name: String): TrainRouteMapType? {
            return entries.firstOrNull { it.name.equals(name, true) }
        }
    }
}

private fun loadImageRequest(resId: Int, context: Context): ImageRequest {
    return ImageRequest.Builder(context)
        .data(resId)
        .unrestrictedBitmapSize()
        .size(SizeResolver.ORIGINAL)
        .build()
        .apply { context.imageLoader.enqueue(this) }
}

private data class ImageData(
    val resId: Int,
    val imageRequest: ImageRequest
)

private fun Int.asImageData(context: Context): ImageData {
    return ImageData(this, loadImageRequest(this, context))
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

data class ZoomState(val scale: Float, val offset: Offset?)

private val mtrRouteMapZoomState: MutableStateFlow<ZoomState> = MutableStateFlow(ZoomState(1.0F, null))
private val mtrRouteMapLocationJumpedState: MutableStateFlow<Boolean> = MutableStateFlow(false)

@Composable
fun MTRRouteMapInterface(instance: AppActiveContext, ambientMode: Boolean) {
    var zoomState by mtrRouteMapZoomState.collectAsStateWithLifecycle()
    val state = rememberZoomState()
    var mtrRouteMapData by mtrRouteMapDataState.collectAsStateWithLifecycle()
    var allStops: Map<String, Stop?> by remember { mutableStateOf(mtrRouteMapData?.let { it.stations.keys.associateWith { s -> s.asStop(instance) } }.orEmpty()) }
    val closestStopState: MutableState<Map.Entry<String, Stop?>?> = remember { mutableStateOf(null) }
    var closestStop by closestStopState

    val loadedState = remember { mutableStateOf(false) }
    val loaded by loadedState

    var setScale by remember(loaded) { mutableStateOf(false) }

    var location by rememberSaveable(saver = coordinatesNullableStateSaver) { mutableStateOf(null) }
    var locationJumped by mtrRouteMapLocationJumpedState.collectAsStateWithLifecycle()

    LaunchedEffect (Unit) {
        checkLocationPermission(instance, true)
        while (true) {
            val result = getGPSLocation(instance).await()
            if (result?.isSuccess == true) {
                location = result.location!!
            }
            delay(Shared.ETA_UPDATE_INTERVAL.toLong())
        }
    }

    LaunchedEffect (Unit) {
        state.zoomable.apply {
            threeStepScale = true
            scalesCalculator = ScalesCalculator.predefined(
                minScale = 0.5F,
                mediumScale = 1.0F,
                maxScale = 1.3F
            )
        }
    }
    LaunchedEffect (loaded) {
        if (loaded) {
            state.zoomable.apply {
                delay(50)
                zoomState.offset?.let {
                    scale(zoomState.scale)
                    offset(it)
                }?: run {
                    val dimension = mtrRouteMapData?.dimension?: Size.Zero
                    val offset = state.zoomable.realPointToContentPoint(dimension.center, dimension)
                    locate(offset.round(), zoomState.scale)
                }
                setScale = true
            }
        }
    }
    LaunchedEffect (setScale, state.zoomable.transform.scaleX, state.zoomable.transform.offset) {
        if (setScale) {
            zoomState = ZoomState(state.zoomable.transform.scaleX, state.zoomable.transform.offset)
        }
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
    LaunchedEffect (setScale, allStops, location) {
        if (setScale && location != null && allStops.isNotEmpty()) {
            val stop = allStops.entries.asSequence().filter { it.value != null }.minByOrNull { it.value!!.location.distance(location!!) }
            closestStop = stop
            if (stop != null) {
                if (!locationJumped) {
                    mtrRouteMapData?.let { data ->
                        val position = data.stations[stop.key]!!
                        val offset = state.zoomable.realPointToContentPoint(position, data.dimension)
                        state.zoomable.locate(offset.round(), animated = true)
                        locationJumped = true
                    }
                }
            }
        }
    }

    MTRRouteMapMapInterface(instance, state, loadedState, closestStopState, ambientMode, setScale)
}

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun MTRRouteMapMapInterface(
    instance: AppActiveContext,
    state: com.github.panpf.zoomimage.compose.ZoomState,
    loadedState: MutableState<Boolean>,
    closestStopState: MutableState<Map.Entry<String, Stop?>?>,
    ambientMode: Boolean,
    show: Boolean
) {
    val scope = rememberCoroutineScope()
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
    var loaded by loadedState

    val platformContext = LocalPlatformContext.current
    val context = LocalContext.current

    val resources = mapOf(
        false to (if (typhoonInfo.isAboveTyphoonSignalNine) R.mipmap.mtr_system_map_watch_typhoon else R.mipmap.mtr_system_map_watch).asImageData(platformContext),
        true to (if (typhoonInfo.isAboveTyphoonSignalNine) R.mipmap.mtr_system_map_watch_typhoon_ambient else R.mipmap.mtr_system_map_watch_ambient).asImageData(platformContext)
    )

    val sizeCache = remember { ConcurrentHashMap<Int, Pair<ByteArray, IntSizeCompat>>() }
    LaunchedEffect (Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val (resId) = resources[!ambientMode]!!
            sizeCache.computeIfAbsent(resId) {
                context.resources.openRawResource(resId).use { it.readBytes() } to BitmapFactory.decodeResource(context.resources, resId).size
            }
        }
    }

    val transition = updateTransition(
        targetState = ambientMode to resources[ambientMode]!!,
        label = "MTRRouteMapAmbientCrossfade"
    )

    LaunchedEffect (transition.currentState) {
        val (resId) = transition.currentState.second
        val (bytes, size) = CoroutineScope(Dispatchers.IO).async {
            sizeCache.computeIfAbsent(resId) {
                context.resources.openRawResource(resId).use { it.readBytes() } to BitmapFactory.decodeResource(context.resources, resId).size
            }
        }.await()
        state.setSubsamplingImage(
            imageSource = ImageSource.fromByteArray(bytes),
            imageInfo = ImageInfo(size, "image/png")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent {
                scope.launch {
                    if (loaded) {
                        val scale = state.zoomable.transform.scaleX + it.verticalScrollPixels * 0.002F
                        state.zoomable.scale(scale, animated = true)
                    }
                }
                true
            }
            .requestFocusOnHierarchyActive()
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        mtrRouteMapData?.let { data ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .drawWithContent {
                        if (show) {
                            drawContent()
                            if (loaded) {
                                closestStop?.let { closest ->
                                    val stopId = closest.key
                                    val position = data.stations[stopId]
                                    if (position != null) {
                                        val center = state.zoomable.realPointToTouchPoint(position, data.dimension)
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
                        }
                    }
                    .zoom(
                        zoomable = state.zoomable,
                        userSetupContentSize = true,
                        onTap = { pos ->
                            if (loaded) {
                                val clickedPos = state.zoomable.touchPointToRealPoint(pos, data.dimension)
                                val stopId = data.findClickedStations(clickedPos)
                                if (stopId != null) {
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
                    )
            ) {
                transition.Crossfade(
                    modifier = Modifier.matchParentSize(),
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing
                    ),
                    contentKey = { (a) -> a },
                ) { (_, imageData) ->
                    AsyncImage(
                        modifier = Modifier
                            .matchParentSize()
                            .layout { measurable, _ ->
                                val (width, height) = state.zoomable.contentSize
                                val placeable = measurable.measure(Constraints.fixed(width, height))
                                layout(placeable.width, placeable.height) {
                                    placeable.place(placeable.width / 2, placeable.height / 2)
                                }
                            },
                        model = imageData.imageRequest,
                        onSuccess = {
                            scope.launch { loaded = true }
                            state.zoomable.contentSize = it.painter.intrinsicSize.roundToIntSize()
                        },
                        contentScale = ContentScale.None,
                        contentDescription = if (Shared.language == "en") "MTR System Map" else "港鐵路綫圖"
                    )
                }
            }
        }
        if (!loaded) {
            CircularProgressIndicator(
                modifier = Modifier.size(27.scaledSize(instance).dp),
                color = Color(0xFFF9DE09),
                strokeWidth = 3.dp,
                trackColor = Color(0xFF797979),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

private val lrtRouteMapZoomState: MutableStateFlow<ZoomState> = MutableStateFlow(ZoomState(0.9F, null))
private val lrtRouteMapLocationJumpedState: MutableStateFlow<Boolean> = MutableStateFlow(false)

@Composable
fun LRTRouteMapInterface(instance: AppActiveContext, ambientMode: Boolean) {
    var zoomState by lrtRouteMapZoomState.collectAsStateWithLifecycle()
    val state = rememberZoomState()
    var lrtRouteMapData by lightRailRouteMapDataState.collectAsStateWithLifecycle()
    var allStops: Map<String, Stop?> by remember { mutableStateOf(lrtRouteMapData?.let { it.stations.keys.associateWith { s -> s.asStop(instance) } }.orEmpty()) }
    val closestStopState: MutableState<Map.Entry<String, Stop?>?> = remember { mutableStateOf(null) }
    var closestStop by closestStopState

    var location by rememberSaveable(saver = coordinatesNullableStateSaver) { mutableStateOf(null) }
    var locationJumped by lrtRouteMapLocationJumpedState.collectAsStateWithLifecycle()

    val loadedState = remember { mutableStateOf(false) }
    val loaded by loadedState

    var setScale by remember(loaded) { mutableStateOf(false) }

    LaunchedEffect (Unit) {
        checkLocationPermission(instance, true)
        while (true) {
            val result = getGPSLocation(instance).await()
            if (result?.isSuccess == true) {
                location = result.location!!
            }
            delay(Shared.ETA_UPDATE_INTERVAL.toLong())
        }
    }

    LaunchedEffect (Unit) {
        state.zoomable.apply {
            threeStepScale = true
            scalesCalculator = ScalesCalculator.predefined(
                minScale = 0.5F,
                mediumScale = 0.9F,
                maxScale = 1.2F
            )
        }
    }
    LaunchedEffect (loaded) {
        if (loaded) {
            state.zoomable.apply {
                delay(50)
                zoomState.offset?.let {
                    scale(zoomState.scale)
                    offset(it)
                }?: run {
                    val dimension = lrtRouteMapData?.dimension?: Size.Zero
                    val offset = state.zoomable.realPointToContentPoint(dimension.center, dimension)
                    locate(offset.round(), zoomState.scale)
                }
                setScale = true
            }
        }
    }
    LaunchedEffect (setScale, state.zoomable.transform.scaleX, state.zoomable.transform.offset) {
        if (setScale) {
            zoomState = ZoomState(state.zoomable.transform.scaleX, state.zoomable.transform.offset)
        }
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
    LaunchedEffect (setScale, allStops, location) {
        if (setScale && location != null && allStops.isNotEmpty()) {
            val stop = allStops.entries.asSequence().filter { it.value != null }.minByOrNull { it.value!!.location.distance(location!!) }
            closestStop = stop
            if (stop != null) {
                if (!locationJumped) {
                    lrtRouteMapData?.let { data ->
                        val position = data.stations[stop.key]!!
                        val offset = state.zoomable.realPointToContentPoint(position, data.dimension)
                        state.zoomable.locate(offset.round(), animated = true)
                        locationJumped = true
                    }
                }
            }
        }
    }

    LRTRouteMapMapInterface(instance, state, loadedState, closestStopState, ambientMode, setScale)
}

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun LRTRouteMapMapInterface(
    instance: AppActiveContext,
    state: com.github.panpf.zoomimage.compose.ZoomState,
    loadedState: MutableState<Boolean>,
    closestStopState: MutableState<Map.Entry<String, Stop?>?>,
    ambientMode: Boolean,
    show: Boolean
) {
    val scope = rememberCoroutineScope()
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
    val lrtRouteMapData by lightRailRouteMapDataState.collectAsStateWithLifecycle()
    val closestStop by closestStopState
    var loaded by loadedState

    val platformContext = LocalPlatformContext.current
    val context = LocalContext.current

    val resources = mapOf(
        false to R.mipmap.light_rail_system_map_watch.asImageData(platformContext),
        true to R.mipmap.light_rail_system_map_watch_ambient.asImageData(platformContext)
    )

    val sizeCache = remember { ConcurrentHashMap<Int, Pair<ByteArray, IntSizeCompat>>() }
    LaunchedEffect (Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val (resId) = resources[!ambientMode]!!
            sizeCache.computeIfAbsent(resId) {
                context.resources.openRawResource(resId).use { it.readBytes() } to BitmapFactory.decodeResource(context.resources, resId).size
            }
        }
    }

    val transition = updateTransition(
        targetState = ambientMode to resources[ambientMode]!!,
        label = "LRTRouteMapAmbientCrossfade"
    )

    LaunchedEffect (transition.currentState) {
        val (resId) = transition.currentState.second
        val (bytes, size) = CoroutineScope(Dispatchers.IO).async {
            sizeCache.computeIfAbsent(resId) {
                context.resources.openRawResource(resId).use { it.readBytes() } to BitmapFactory.decodeResource(context.resources, resId).size
            }
        }.await()
        state.setSubsamplingImage(
            imageSource = ImageSource.fromByteArray(bytes),
            imageInfo = ImageInfo(size, "image/png")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent {
                scope.launch {
                    if (loaded) {
                        val scale = state.zoomable.transform.scaleX + it.verticalScrollPixels * 0.002F
                        state.zoomable.scale(scale, animated = true)
                    }
                }
                true
            }
            .requestFocusOnHierarchyActive()
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        lrtRouteMapData?.let { data ->
            Box(
                modifier = Modifier.fillMaxSize()
                    .clipToBounds()
                    .drawWithContent {
                        if (show) {
                            drawContent()
                            if (loaded) {
                                closestStop?.let { closest ->
                                    val stopId = closest.key
                                    val position = data.stations[stopId]
                                    if (position != null) {
                                        val center = state.zoomable.realPointToTouchPoint(
                                            position,
                                            data.dimension
                                        )
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
                        }
                    }
                    .zoom(
                        zoomable = state.zoomable,
                        userSetupContentSize = true,
                        onTap = { pos ->
                            if (loaded) {
                                val clickedPos =
                                    state.zoomable.touchPointToRealPoint(pos, data.dimension)
                                val stopId = data.findClickedStations(clickedPos)
                                if (stopId != null) {
                                    scope.launch {
                                        val stop = stopId.asStop(instance)
                                        val result = Registry.getInstance(instance)
                                            .findRoutes("", false) { r ->
                                                r.co.firstCo() == Operator.LRT && r.stops[Operator.LRT]?.contains(
                                                    stopId
                                                ) == true
                                            }.onEach {
                                            it.stopInfo = StopInfo(stopId, stop, 0.0, Operator.LRT)
                                        }.toStopIndexed(instance)
                                        val intent = AppIntent(instance, AppScreen.LIST_ROUTES)
                                        intent.putExtra(
                                            "result",
                                            result.map { it.strip(); it.serialize() }.toJsonArray()
                                                .toString()
                                        )
                                        intent.putExtra("listType", RouteListType.NORMAL.name)
                                        intent.putExtra("showEta", true)
                                        intent.putExtra("mtrSearch", stopId)
                                        instance.startActivity(intent)
                                    }
                                }
                            }
                        }
                    )
            ) {
                transition.Crossfade(
                    modifier = Modifier.matchParentSize(),
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing
                    ),
                    contentKey = { (a) -> a },
                ) { (_, imageData) ->
                    AsyncImage(
                        modifier = Modifier
                            .matchParentSize()
                            .layout { measurable, _ ->
                                val (width, height) = state.zoomable.contentSize
                                val placeable = measurable.measure(Constraints.fixed(width, height))
                                layout(placeable.width, placeable.height) {
                                    placeable.place(placeable.width / 2, placeable.height / 2)
                                }
                            },
                        model = imageData.imageRequest,
                        onSuccess = {
                            scope.launch { loaded = true }
                            state.zoomable.contentSize = it.painter.intrinsicSize.roundToIntSize()
                        },
                        contentScale = ContentScale.None,
                        contentDescription = if (Shared.language == "en") "Light Rail Route Map" else "輕鐵路綫圖"
                    )
                }
            }
        }
        if (!loaded) {
            CircularProgressIndicator(
                modifier = Modifier.size(27.scaledSize(instance).dp),
                color = Color(0xFFF9DE09),
                strokeWidth = 3.dp,
                trackColor = Color(0xFF797979),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}