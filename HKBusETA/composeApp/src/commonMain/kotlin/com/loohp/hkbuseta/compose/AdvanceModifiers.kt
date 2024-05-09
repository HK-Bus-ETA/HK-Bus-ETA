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

package com.loohp.hkbuseta.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material3.BasicTooltipState
import androidx.compose.material3.CaretScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.loohp.hkbuseta.utils.asAnnotatedString
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


inline fun Modifier.applyIf(condition: Boolean, apply: Modifier.() -> Modifier): Modifier {
    return applyIf({ condition }, apply)
}

inline fun Modifier.applyIf(condition: Boolean, apply: Modifier.() -> Modifier, elseApply: Modifier.() -> Modifier): Modifier {
    return applyIf({ condition }, apply, elseApply)
}

inline fun Modifier.applyIf(predicate: () -> Boolean, apply: Modifier.() -> Modifier): Modifier {
    return if (predicate.invoke()) apply.invoke(this) else this
}

inline fun Modifier.applyIf(predicate: () -> Boolean, apply: Modifier.() -> Modifier, elseApply: Modifier.() -> Modifier): Modifier {
    return if (predicate.invoke()) apply.invoke(this) else elseApply.invoke(this)
}

inline fun <T> Modifier.applyIfNotNull(item: T?, apply: Modifier.(T) -> Modifier): Modifier {
    return item?.let { apply.invoke(this, it) }?: this
}

inline fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    noinline onClick: () -> Unit
) = clickable(
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onClick = onClick
).pointerHoverIcon(PointerIcon.Hand)

@OptIn(ExperimentalFoundationApi::class)
inline fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    noinline onLongClick: (() -> Unit)? = null,
    noinline onDoubleClick: (() -> Unit)? = null,
    noinline onClick: () -> Unit
) = combinedClickable(
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onLongClickLabel = onLongClickLabel,
    onLongClick = onLongClick,
    onDoubleClick = onDoubleClick,
    onClick = onClick
).pointerHoverIcon(PointerIcon.Hand)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Modifier.plainTooltip(
    tooltip: String,
    positionProvider: PopupPositionProvider = rememberPlainTooltipPositionProvider(),
    state: TooltipState = rememberTooltipState(isPersistent = true),
    enableUserInput: Boolean = true
): Modifier = plainTooltip(
    tooltip = tooltip.asAnnotatedString(),
    positionProvider = positionProvider,
    state = state,
    enableUserInput = enableUserInput,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Modifier.plainTooltip(
    tooltip: AnnotatedString,
    positionProvider: PopupPositionProvider = rememberPlainTooltipPositionProvider(),
    state: TooltipState = rememberTooltipState(isPersistent = true),
    enableUserInput: Boolean = true
): Modifier {
    var anchorBounds: LayoutCoordinates? by remember { mutableStateOf(null) }
    val transition = updateTransition(state.transition, label = "tooltip transition")
    val scope = rememberCoroutineScope()
    val tooltipScope = remember { object : CaretScope {
        override fun Modifier.drawCaret(draw: CacheDrawScope.(LayoutCoordinates?) -> DrawResult): Modifier {
            return drawWithCache { draw(anchorBounds) }
        }
    } }
    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                tween(durationMillis = 150, easing = LinearEasing)
            } else {
                tween(durationMillis = 75, easing = LinearEasing)
            }
        },
        label = "tooltip transition: alpha"
    ) { if (it) 1f else 0f }

    DisposableEffect (state) {
        onDispose { state.onDispose() }
    }

    return onGloballyPositioned { anchorBounds = it }
        .handleTooltipGestures(enableUserInput, state)
        .composed { apply {
            if (state.isVisible) {
                Popup(
                    popupPositionProvider = positionProvider,
                    onDismissRequest = {
                        if (state.isVisible) {
                            scope.launch { state.dismiss() }
                        }
                    },
                    properties = PopupProperties(focusable = false),
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            this.alpha = alpha
                        }
                    ) {
                        tooltipScope.PlatformPlainTooltip {
                            PlatformText(
                                text = tooltip,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } }
}

@Composable
fun rememberPlainTooltipPositionProvider(
    spacingBetweenTooltipAndAnchor: Dp = 4.dp
): PopupPositionProvider {
    val tooltipAnchorSpacing = with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
    val topInsert = WindowInsets.safeContent.getTop(LocalDensity.current)
    return remember(tooltipAnchorSpacing) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                var y = anchorBounds.top - topInsert - popupContentSize.height - tooltipAnchorSpacing
                if (y < 0) {
                    y = anchorBounds.bottom + tooltipAnchorSpacing
                } else {
                    y += topInsert
                }
                return IntOffset(x, y)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Modifier.handleTooltipGestures(
    enabled: Boolean,
    state: BasicTooltipState
): Modifier {
    var hovering by remember { mutableStateOf(false) }
    var counter by remember { mutableIntStateOf(0) }
    LaunchedEffect (state.isVisible) {
        if (state.isVisible && !hovering) {
            delay(3000)
            state.dismiss()
            counter++
        }
    }
    return applyIf(enabled) {
        pointerInput(state, counter) {
            coroutineScope {
                awaitEachGesture {
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                    val pass = PointerEventPass.Initial
                    val inputType = awaitFirstDown(pass = pass).type
                    if (inputType == PointerType.Touch || inputType == PointerType.Stylus) {
                        try {
                            withTimeout(longPressTimeout) { waitForUpOrCancellation(pass = pass) }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            launch { state.show(MutatePriority.UserInput) }
                            val changes = awaitPointerEvent(pass = pass).changes
                            for (element in changes) { element.consume() }
                        }
                    }
                }
            }
        }.pointerInput(state, counter) {
            coroutineScope {
                awaitPointerEventScope {
                    val pass = PointerEventPass.Main
                    while (true) {
                        val event = awaitPointerEvent(pass)
                        val inputType = event.changes[0].type
                        if (inputType == PointerType.Mouse) {
                            when (event.type) {
                                PointerEventType.Enter -> launch {
                                    hovering = true
                                    delay(500)
                                    state.show(MutatePriority.UserInput)
                                }
                                PointerEventType.Exit -> {
                                    hovering = false
                                    state.dismiss()
                                    counter++
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}