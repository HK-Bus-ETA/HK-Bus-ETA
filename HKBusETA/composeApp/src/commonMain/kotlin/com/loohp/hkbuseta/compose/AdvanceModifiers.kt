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

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.ScaleToBounds
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
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

inline fun Modifier.onHardwareKeyboardEnter(crossinline action: () -> Boolean): Modifier {
    return onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) {
            action.invoke()
        } else {
            false
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalSharedAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.localSharedElement(
    key: Any,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val sharedAnimatedVisibilityScope = LocalSharedAnimatedVisibilityScope.current
    return applyIf(sharedTransitionScope != null && sharedAnimatedVisibilityScope != null) {
        with(sharedTransitionScope!!) {
            sharedElement(
                state = rememberSharedContentState(key),
                animatedVisibilityScope = sharedAnimatedVisibilityScope!!,
                boundsTransform = boundsTransform,
                placeHolderSize = placeHolderSize,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                zIndexInOverlay = zIndexInOverlay,
                clipInOverlayDuringTransition = clipInOverlayDuringTransition
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.localSharedBounds(
    key: Any,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    resizeMode: ResizeMode = ScaleToBounds(ContentScale.FillWidth, Center),
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val sharedAnimatedVisibilityScope = LocalSharedAnimatedVisibilityScope.current
    return applyIf(sharedTransitionScope != null && sharedAnimatedVisibilityScope != null) {
        with(sharedTransitionScope!!) {
            sharedBounds(
                sharedContentState = rememberSharedContentState(key),
                animatedVisibilityScope = sharedAnimatedVisibilityScope!!,
                enter = enter,
                exit = exit,
                boundsTransform = boundsTransform,
                resizeMode = resizeMode,
                placeHolderSize = placeHolderSize,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                zIndexInOverlay = zIndexInOverlay,
                clipInOverlayDuringTransition = clipInOverlayDuringTransition
            )
        }
    }
}

private val DefaultSpring = spring(
    stiffness = StiffnessMediumLow,
    visibilityThreshold = Rect.VisibilityThreshold
)

@ExperimentalSharedTransitionApi
private val DefaultBoundsTransform = BoundsTransform { _, _ -> DefaultSpring }

@ExperimentalSharedTransitionApi
private val ParentClip: OverlayClip =
    object : OverlayClip {
        override fun getClipPath(
            state: SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density
        ): Path? {
            return state.parentSharedContentState?.clipPathInOverlay
        }
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
    state: TooltipState = rememberTooltipState(isPersistent = true),
    enableUserInput: Boolean = true
): Modifier = plainTooltip(
    tooltip = tooltip.asAnnotatedString(),
    state = state,
    enableUserInput = enableUserInput,
)

interface TooltipScope {
    fun Modifier.drawCaret(draw: CacheDrawScope.(LayoutCoordinates?) -> DrawResult): Modifier
}

internal class TooltipScopeImpl(val getAnchorBounds: () -> LayoutCoordinates?): TooltipScope {
    override fun Modifier.drawCaret(
        draw: CacheDrawScope.(LayoutCoordinates?) -> DrawResult
    ): Modifier = this.drawWithCache { draw(getAnchorBounds()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Modifier.plainTooltip(
    tooltip: AnnotatedString,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    enableUserInput: Boolean = true
): Modifier {
    val anchorBounds: MutableState<LayoutCoordinates?> = remember { mutableStateOf(null) }
    val transition = rememberTransition(state.transition, label = "tooltip transition")
    val scope = rememberCoroutineScope()
    val tooltipScope = remember { TooltipScopeImpl { anchorBounds.value } }
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

    return onGloballyPositioned { anchorBounds.value = it }
        .handleTooltipGestures(enableUserInput, state)
        .composed { apply {
            if (state.isVisible) {
                val positionProvider = rememberPlainTooltipPositionProvider { anchorBounds.value }
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
    spacingBetweenTooltipAndAnchor: Dp = 4.dp,
    getAnchorBounds: () -> LayoutCoordinates?
): PopupPositionProvider {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val tooltipAnchorSpacing = with(density) { spacingBetweenTooltipAndAnchor.roundToPx() }
    val safeDrawingTop = WindowInsets.safeDrawing.getTop(density)
    val safeDrawingBottom = WindowInsets.safeDrawing.getBottom(density)
    val safeDrawingLeft = WindowInsets.safeDrawing.getLeft(density, layoutDirection)
    val safeDrawingRight = WindowInsets.safeDrawing.getRight(density, layoutDirection)
    val topInsert = WindowInsets.safeContent.getTop(density) - safeDrawingTop
    return remember(tooltipAnchorSpacing) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val layout = getAnchorBounds.invoke()?: return IntOffset.Zero

                val offset = -layout.windowToLocal(anchorBounds.topLeft.toOffset()).round()
                val contentWidth = layout.size.width
                val heightInset = tooltipAnchorSpacing + topInsert

                var x = anchorBounds.left + offset.x + contentWidth / 2 - popupContentSize.width / 2 - safeDrawingLeft
                if (x < -safeDrawingLeft) {
                    x = -safeDrawingLeft
                } else if (x + popupContentSize.width > windowSize.width + safeDrawingRight) {
                    x = windowSize.width + safeDrawingRight - popupContentSize.width
                }

                var y = anchorBounds.top + offset.y - popupContentSize.height - tooltipAnchorSpacing - safeDrawingTop
                if (y < heightInset - safeDrawingTop) {
                    y = anchorBounds.bottom + offset.y + tooltipAnchorSpacing - safeDrawingTop
                } else if (y + popupContentSize.height > windowSize.height - heightInset + safeDrawingBottom) {
                    y = windowSize.height - heightInset - popupContentSize.height + safeDrawingBottom
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
    state: TooltipState
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

@Composable
@ExperimentalMaterial3Api
fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretSize: DpSize = DpSize.Unspecified,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val drawCaretModifier =
        if (caretSize.isSpecified) {
            val density = LocalDensity.current
            val windowContainerWidthInPx = currentLocalWindowSize.width
            Modifier.drawCaret { anchorLayoutCoordinates ->
                drawCaretWithPath(
                    density,
                    windowContainerWidthInPx,
                    containerColor,
                    caretSize,
                    anchorLayoutCoordinates
                )
            }.then(modifier)
        } else modifier

    Surface(
        modifier = drawCaretModifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Box(modifier = Modifier
            .sizeIn(
                minWidth = 40.dp,
                maxWidth = 200.dp,
                minHeight = 24.dp
            )
            .padding(PaddingValues(8.dp, 4.dp))
        ) {
            val textStyle = MaterialTheme.typography.bodySmall

            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides textStyle,
                content = content
            )
        }
    }
}

@ExperimentalMaterial3Api
private fun CacheDrawScope.drawCaretWithPath(
    density: Density,
    windowContainerWidthInPx: Int,
    containerColor: Color,
    caretSize: DpSize,
    anchorLayoutCoordinates: LayoutCoordinates?
): DrawResult {
    val path = Path()

    if (anchorLayoutCoordinates != null) {
        val caretHeightPx: Int
        val caretWidthPx: Int
        val tooltipAnchorSpacing: Int
        with(density) {
            caretHeightPx = caretSize.height.roundToPx()
            caretWidthPx = caretSize.width.roundToPx()
            tooltipAnchorSpacing = 4.dp.roundToPx()
        }
        val anchorBounds = anchorLayoutCoordinates.boundsInWindow()
        val anchorLeft = anchorBounds.left
        val anchorRight = anchorBounds.right
        val anchorTop = anchorBounds.top
        val anchorMid = (anchorRight + anchorLeft) / 2
        val anchorWidth = anchorRight - anchorLeft
        val tooltipWidth = this.size.width
        val tooltipHeight = this.size.height
        val isCaretTop = anchorTop - tooltipHeight - tooltipAnchorSpacing < 0
        val caretY = if (isCaretTop) { 0f } else { tooltipHeight }

        val position =
            if (anchorMid + tooltipWidth / 2 > windowContainerWidthInPx) {
                val anchorMidFromRightScreenEdge =
                    windowContainerWidthInPx - anchorMid
                val caretX = tooltipWidth - anchorMidFromRightScreenEdge
                Offset(caretX, caretY)
            } else {
                val tooltipLeft =
                    anchorLeft - (this.size.width / 2 - anchorWidth / 2)
                val caretX = anchorMid - maxOf(tooltipLeft, 0f)
                Offset(caretX, caretY)
            }

        if (isCaretTop) {
            path.apply {
                moveTo(x = position.x, y = position.y)
                lineTo(x = position.x + caretWidthPx / 2, y = position.y)
                lineTo(x = position.x, y = position.y - caretHeightPx)
                lineTo(x = position.x - caretWidthPx / 2, y = position.y)
                close()
            }
        } else {
            path.apply {
                moveTo(x = position.x, y = position.y)
                lineTo(x = position.x + caretWidthPx / 2, y = position.y)
                lineTo(x = position.x, y = position.y + caretHeightPx.toFloat())
                lineTo(x = position.x - caretWidthPx / 2, y = position.y)
                close()
            }
        }
    }

    return onDrawWithContent {
        if (anchorLayoutCoordinates != null) {
            drawContent()
            drawPath(
                path = path,
                color = containerColor
            )
        }
    }
}