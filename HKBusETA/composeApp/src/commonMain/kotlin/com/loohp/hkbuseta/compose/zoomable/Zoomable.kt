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

package com.loohp.hkbuseta.compose.zoomable

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.loohp.hkbuseta.appcontext.composePlatform
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A zoomable layout that supports zooming in and out, dragging, double tap and dismiss gesture.
 *
 * @param modifier The modifier to apply to this layout.
 * @param state The state object to be used to control or observe the state.
 * @param enabled Controls the enabled state. When false, all gestures will be ignored.
 * @param onTap Will be called when a single tap is detected.
 * @param dismissGestureEnabled Whether to enable dismiss gesture detection.
 * @param onDismiss Will be called when dismiss gesture is detected. Should return a boolean
 * indicating whether the dismiss request is handled.
 * @param content The block which describes the content.
 */
@Composable
fun Zoomable(
    modifier: Modifier = Modifier,
    state: ZoomableState = rememberZoomableState(),
    enabled: Boolean = true,
    onTap: ((Offset) -> Unit)? = null,
    dismissGestureEnabled: Boolean = false,
    onDismiss: () -> Boolean = { false },
    content: @Composable () -> Unit
) {
    val dismissGestureEnabledState = rememberUpdatedState(dismissGestureEnabled)
    val onDismissState = rememberUpdatedState(onDismiss)
    val gesturesModifier = if (!enabled) Modifier else {
        LaunchedEffect(state.isGestureInProgress, state.overZoomConfig) {
            if (!state.isGestureInProgress) {
                val range = state.overZoomConfig?.range
                if (range?.contains(state.scale) == false) {
                    state.animateScaleTo(state.scale.coerceIn(range))
                }
            }
        }

        Modifier.pointerInput(state) {
            detectZoomableGestures(
                state = state,
                onTap = onTap,
                dismissGestureEnabled = dismissGestureEnabledState,
                onDismiss = onDismissState
            )
        }
    }

    Box(
        modifier = modifier
            .then(gesturesModifier)
            .layout { measurable, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val placeable = measurable.measure(
                    Constraints(
                        maxWidth = (width * state.scale).roundToInt(),
                        maxHeight = (height * state.scale).roundToInt()
                    )
                )
                state.size = IntSize(width, height)
                state.childSize = Size(
                    placeable.width / state.scale,
                    placeable.height / state.scale
                )
                layout(width, height) {
                    placeable.placeWithLayer(
                        state.translationX.roundToInt() - state.boundOffset.x,
                        state.translationY.roundToInt() - state.boundOffset.y + state.dismissDragOffsetY.roundToInt()
                    )
                }
            }
    ) {
        content()
    }
}

internal suspend fun PointerInputScope.detectZoomableGestures(
    state: ZoomableState,
    onTap: ((Offset) -> Unit)?,
    dismissGestureEnabled: State<Boolean>,
    onDismiss: State<() -> Boolean>
): Unit = coroutineScope {
    // 'start = CoroutineStart.UNDISPATCHED' required so handler doesn't miss first event.
    if (composePlatform.hasMouse) {
        launch(start = CoroutineStart.UNDISPATCHED) {
            awaitEachGesture {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    if (event.type == PointerEventType.Scroll) {
                        event.changes.firstOrNull { it.scrollDelta.y != 0F }?.apply {
                            consume()
                            val delta = -scrollDelta.y / 120F
                            if ((delta < 0 && state.scale > state.minScale) || (delta > 0 && state.scale < state.maxScale)) {
                                val targetScale = (state.scale + delta).coerceIn(state.minScale, state.maxScale)
                                launch {
                                    state.animateScaleTo(
                                        targetScale = targetScale
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    launch(start = CoroutineStart.UNDISPATCHED) {
        detectTapGestures(
            onTap = onTap,
            onDoubleTap = { offset ->
                launch {
                    val isZooming = state.scale >= state.doubleTapScale
                    val targetScale = if (isZooming) state.doubleTapOutScale else state.doubleTapScale
                    state.animateScaleTo(
                        targetScale = targetScale,
                        targetTranslation = state.calculateTargetTranslation(offset) * targetScale
                    )
                }
            }
        )
    }
    launch(start = CoroutineStart.UNDISPATCHED) {
        detectTransformGestures(
            onGestureStart = { state.onGestureStart() },
            onGesture = { centroid, pan, zoom ->
                if (state.dismissDragAbsoluteOffsetY == 0f) {
                    launch {
                        state.onTransform(centroid, pan, zoom)
                    }
                }
            },
            onGestureEnd = { state.onTransformEnd() }
        )
    }
    launch(start = CoroutineStart.UNDISPATCHED) {
        detectDragGestures(
            state = state,
            dismissGestureEnabled = dismissGestureEnabled,
            startDragImmediately = { state.isGestureInProgress },
            onDragStart = {
                state.onGestureStart()
                state.addPosition(it.uptimeMillis, it.position)
            },
            onDrag = { change, dragAmount ->
                if (state.isZooming) {
                    launch {
                        state.onDrag(dragAmount)
                        state.addPosition(change.uptimeMillis, change.position)
                    }
                } else {
                    state.onDismissDrag(dragAmount.y)
                }
            },
            onDragCancel = {
                if (state.isZooming) {
                    state.resetTracking()
                } else {
                    launch {
                        state.onDismissDragEnd()
                    }
                }
            },
            onDragEnd = {
                launch {
                    if (state.isZooming) {
                        state.onDragEnd()
                    } else {
                        if (!(state.shouldDismiss && onDismiss.value.invoke())) {
                            state.onDismissDragEnd()
                        }
                    }
                }
            }
        )
    }
}

private suspend fun PointerInputScope.detectDragGestures(
    state: ZoomableState,
    dismissGestureEnabled: State<Boolean>,
    startDragImmediately: () -> Boolean,
    onDragStart: (PointerInputChange) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        // We have to always call this or we'll get a crash if we do nothing
        val down = awaitFirstDown(requireUnconsumed = false)
        if (state.isZooming || dismissGestureEnabled.value) {
            var overSlop = Offset.Zero
            val drag = if (state.isZooming) {
                if (startDragImmediately()) down else {
                    val horizontalEdge = state.horizontalEdge
                    awaitTouchSlopOrCancellation(down.id) { change, over ->
                        if (horizontalEdge != HorizontalEdge.None) {
                            val offset =
                                if (over != Offset.Zero) over else change.positionChange()
                            val direction = offset.x / abs(offset.y)
                            if (horizontalEdge.isOutwards(direction) && abs(direction) > 1) {
                                return@awaitTouchSlopOrCancellation
                            }
                        }
                        change.consume()
                        overSlop = over
                    }
                }
            } else {
                awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                    change.consume()
                    overSlop = Offset(0f, over)
                }
            }
            if (drag != null) {
                onDragStart(down)
                if (overSlop != Offset.Zero) onDrag(drag, overSlop)
                if (
                    !drag(drag.id) {
                        onDrag(it, it.positionChange())
                        it.consume()
                    }
                ) {
                    onDragCancel()
                } else {
                    onDragEnd()
                }
            }
        }
    }
}

/**
 * Simplified version of [androidx.compose.foundation.gestures.detectTransformGestures] which
 * awaits two pointer downs (instead of one) and starts immediately without considering touch slop.
 */
private suspend fun PointerInputScope.detectTransformGestures(
    onGestureStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        awaitTwoDowns(requireUnconsumed = false)
        onGestureStart()
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                val centroid = event.calculateCentroid(useCurrent = false)
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    onGesture(centroid, panChange, zoomChange)
                }
                event.changes.fastForEach {
                    if (it.positionChanged()) {
                        it.consume()
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
        onGestureEnd()
    }
}

private suspend fun AwaitPointerEventScope.awaitTwoDowns(requireUnconsumed: Boolean = true) {
    var event: PointerEvent
    var firstDown: PointerId? = null
    do {
        event = awaitPointerEvent()
        var downPointers = if (firstDown != null) 1 else 0
        event.changes.fastForEach {
            val isDown =
                if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
            val isUp =
                if (requireUnconsumed) it.changedToUp() else it.changedToUpIgnoreConsumed()
            if (isUp && firstDown == it.id) {
                firstDown = null
                downPointers -= 1
            }
            if (isDown) {
                firstDown = it.id
                downPointers += 1
            }
        }
        val satisfied = downPointers > 1
    } while (!satisfied)
}