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

package com.loohp.hkbuseta.compose

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.loohp.hkbuseta.appcontext.AppContext


fun Modifier.fullPageVerticalScrollbar(
    state: ScrollState,
    context: AppContext,
    indicatorThickness: Dp = 8.dp,
    indicatorColor: Color = Color.LightGray,
    alpha: Float = 0.8f,
): Modifier = composed {
    var scrollOffsetViewPort by remember { mutableFloatStateOf(0F) }
    val animatedScrollOffsetViewPort by animateFloatAsState(
        targetValue = scrollOffsetViewPort,
        animationSpec = TweenSpec(durationMillis = 100, easing = LinearEasing),
        label = ""
    )

    drawWithContent {
        drawContent()

        val contentOffset = state.value
        val viewPortLength = size.height
        val contentLength = (viewPortLength + state.maxValue).coerceAtLeast(0.001f)

        if (viewPortLength < contentLength) {
            val indicatorLength = viewPortLength / contentLength
            val indicatorThicknessPx = indicatorThickness.toPx()
            val halfIndicatorThicknessPx = (indicatorThickness.value / 2F).dp.toPx()
            scrollOffsetViewPort = contentOffset / contentLength

            if (context.isScreenRound()) {
                val topLeft = Offset(halfIndicatorThicknessPx, halfIndicatorThicknessPx)
                val size = Size(context.screenWidth - indicatorThicknessPx, context.screenHeight - indicatorThicknessPx)
                val style = Stroke(width = indicatorThicknessPx, cap = StrokeCap.Round)
                drawArc(
                    startAngle = -30F,
                    sweepAngle = 60F,
                    useCenter = false,
                    color = Color.DarkGray,
                    topLeft = topLeft,
                    size = size,
                    alpha = alpha,
                    style = style
                )
                drawArc(
                    startAngle = -30F + animatedScrollOffsetViewPort * 60F,
                    sweepAngle = indicatorLength * 60F,
                    useCenter = false,
                    color = indicatorColor,
                    topLeft = topLeft,
                    size = size,
                    alpha = alpha,
                    style = style
                )
            } else {
                val cornerRadius = CornerRadius(indicatorThicknessPx / 2F)
                val topLeft = Offset(context.screenWidth - indicatorThicknessPx, viewPortLength * 0.125F)
                val size = Size(indicatorThicknessPx, viewPortLength * 0.75F)
                drawRoundRect(
                    color = Color.DarkGray,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset(topLeft.x, topLeft.y + animatedScrollOffsetViewPort * size.height),
                    size = Size(size.width, size.height * indicatorLength),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}

fun Modifier.fullPageVerticalScrollbar(
    state: LazyListState,
    context: AppContext,
    indicatorThickness: Dp = 8.dp,
    indicatorColor: Color = Color.LightGray,
    alpha: Float = 0.8f
): Modifier = composed {
    val actualItemLength: MutableMap<Int, Int> = remember { mutableMapOf() }
    var totalItemCount by remember { mutableIntStateOf(0) }
    var indicatorLength by remember { mutableFloatStateOf(0F) }
    var scrollOffsetViewPort by remember { mutableFloatStateOf(0F) }
    val animatedIndicatorLength by animateFloatAsState(
        targetValue = indicatorLength,
        animationSpec = TweenSpec(durationMillis = 300, easing = LinearEasing),
        label = ""
    )
    val animatedScrollOffsetViewPort by animateFloatAsState(
        targetValue = scrollOffsetViewPort,
        animationSpec = TweenSpec(durationMillis = 100, easing = LinearEasing),
        label = ""
    )

    drawWithContent {
        drawContent()

        val (viewPortWidth, viewPortLength) = size
        val itemsVisible = state.layoutInfo.visibleItemsInfo
        if (totalItemCount != state.layoutInfo.totalItemsCount) {
            actualItemLength.clear()
            totalItemCount = state.layoutInfo.totalItemsCount
        }
        itemsVisible.forEach { actualItemLength[it.index] = it.size }
        val knownLength = actualItemLength.entries.sumOf { it.value }
        val knownAmount = actualItemLength.values.count()
        val knownAverageItemLength = knownLength / knownAmount
        val contentOffset = (0 until state.firstVisibleItemIndex).sumOf { actualItemLength.getOrDefault(it, knownAverageItemLength) }.toFloat() + state.firstVisibleItemScrollOffset
        val contentLength = knownLength + (state.layoutInfo.totalItemsCount - knownAmount) * (knownLength / knownAmount)

        if (viewPortLength < contentLength) {
            indicatorLength = (viewPortLength / contentLength).coerceAtLeast(0.05F)
            val indicatorThicknessPx = indicatorThickness.toPx()
            val halfIndicatorThicknessPx = (indicatorThickness.value / 2F).dp.toPx()
            scrollOffsetViewPort = contentOffset / contentLength

            if (context.isScreenRound()) {
                val topLeft = Offset(halfIndicatorThicknessPx, halfIndicatorThicknessPx)
                val size = Size(viewPortWidth - indicatorThicknessPx, viewPortLength - indicatorThicknessPx)
                val style = Stroke(width = indicatorThicknessPx, cap = StrokeCap.Round)
                val startAngle = (-30F + animatedScrollOffsetViewPort * 60F).coerceIn(-30F, 30F)
                drawArc(
                    startAngle = -30F,
                    sweepAngle = 60F,
                    useCenter = false,
                    color = Color.DarkGray,
                    topLeft = topLeft,
                    size = size,
                    alpha = alpha,
                    style = style
                )
                drawArc(
                    startAngle = startAngle,
                    sweepAngle = (animatedIndicatorLength * 60F).coerceAtMost(60F - (startAngle + 30F)),
                    useCenter = false,
                    color = indicatorColor,
                    topLeft = topLeft,
                    size = size,
                    alpha = alpha,
                    style = style
                )
            } else {
                val cornerRadius = CornerRadius(indicatorThicknessPx / 2F)
                val topLeft = Offset(context.screenWidth - indicatorThicknessPx, viewPortLength * 0.125F)
                val size = Size(indicatorThicknessPx, viewPortLength * 0.75F)
                val startHeight = (topLeft.y + animatedScrollOffsetViewPort * size.height).coerceIn(topLeft.y, topLeft.y + size.height)
                drawRoundRect(
                    color = Color.DarkGray,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset(topLeft.x, startHeight),
                    size = Size(size.width, (size.height * animatedIndicatorLength).coerceAtMost(size.height - (startHeight - topLeft.y))),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}


@Immutable
data class FullPageScrollBarConfig(
    val indicatorThickness: Dp = 8.dp,
    val indicatorColor: Color = Color.LightGray,
    val alpha: Float? = null,
    val alphaAnimationSpec: AnimationSpec<Float>? = null
)


fun Modifier.fullPageVerticalLazyScrollbar(
    state: LazyListState,
    context: AppContext,
    scrollbarConfigFullPage: FullPageScrollBarConfig = FullPageScrollBarConfig()
) = this
    .fullPageVerticalScrollbar(
        state = state,
        context = context,
        indicatorThickness = scrollbarConfigFullPage.indicatorThickness,
        indicatorColor = scrollbarConfigFullPage.indicatorColor,
        alpha = scrollbarConfigFullPage.alpha ?: 0.8f
    )


fun Modifier.fullPageVerticalScrollWithScrollbar(
    state: ScrollState,
    context: AppContext,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    scrollbarConfigFullPage: FullPageScrollBarConfig = FullPageScrollBarConfig()
) = this
    .fullPageVerticalScrollbar(
        state = state,
        context = context,
        indicatorThickness = scrollbarConfigFullPage.indicatorThickness,
        indicatorColor = scrollbarConfigFullPage.indicatorColor,
        alpha = scrollbarConfigFullPage.alpha ?: 0.8f
    )
    .verticalScroll(state, enabled, flingBehavior, reverseScrolling)


fun Modifier.scrollbar(
    state: ScrollState,
    direction: Orientation,
    indicatorThickness: Dp = 8.dp,
    indicatorColor: Color = Color.LightGray,
    alpha: Float = if (state.isScrollInProgress) 0.8f else 0f,
    alphaAnimationSpec: AnimationSpec<Float> = tween(
        delayMillis = if (state.isScrollInProgress) 0 else 1500,
        durationMillis = if (state.isScrollInProgress) 150 else 500
    ),
    padding: PaddingValues = PaddingValues(all = 0.dp)
): Modifier = composed {
    val scrollbarAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = alphaAnimationSpec,
        label = ""
    )

    drawWithContent {
        drawContent()

        val showScrollBar = (state.isScrollInProgress || scrollbarAlpha > 0.0f) && (state.canScrollForward || state.canScrollBackward)

        // Draw scrollbar only if currently scrolling or if scroll animation is ongoing.
        if (showScrollBar) {
            val (topPadding, bottomPadding, startPadding, endPadding) = listOf(
                padding.calculateTopPadding().toPx(), padding.calculateBottomPadding().toPx(),
                padding.calculateStartPadding(layoutDirection).toPx(),
                padding.calculateEndPadding(layoutDirection).toPx()
            )
            val contentOffset = state.value
            val viewPortLength = if (direction == Orientation.Vertical)
                size.height else size.width
            val viewPortCrossAxisLength = if (direction == Orientation.Vertical)
                size.width else size.height
            val contentLength = (viewPortLength + state.maxValue).coerceAtLeast(0.001f)  // To prevent divide by zero error
            val indicatorLength = ((viewPortLength / contentLength) * viewPortLength) - (
                    if (direction == Orientation.Vertical) topPadding + bottomPadding
                    else startPadding + endPadding
                    )
            val indicatorThicknessPx = indicatorThickness.toPx()

            val scrollOffsetViewPort = viewPortLength * contentOffset / contentLength

            val scrollbarSizeWithoutInsets = if (direction == Orientation.Vertical)
                Size(indicatorThicknessPx, indicatorLength)
            else Size(indicatorLength, indicatorThicknessPx)

            val scrollbarPositionWithoutInsets = if (direction == Orientation.Vertical)
                Offset(
                    x = if (layoutDirection == LayoutDirection.Ltr)
                        viewPortCrossAxisLength - indicatorThicknessPx - endPadding
                    else startPadding,
                    y = scrollOffsetViewPort + topPadding
                )
            else
                Offset(
                    x = if (layoutDirection == LayoutDirection.Ltr)
                        scrollOffsetViewPort + startPadding
                    else viewPortLength - scrollOffsetViewPort - indicatorLength - endPadding,
                    y = viewPortCrossAxisLength - indicatorThicknessPx - bottomPadding
                )

            drawRoundRect(
                color = indicatorColor,
                cornerRadius = CornerRadius(
                    x = indicatorThicknessPx / 2, y = indicatorThicknessPx / 2
                ),
                topLeft = scrollbarPositionWithoutInsets,
                size = scrollbarSizeWithoutInsets,
                alpha = scrollbarAlpha
            )
        }
    }
}


@Immutable
data class ScrollBarConfig(
    val indicatorThickness: Dp = 8.dp,
    val indicatorColor: Color = Color.LightGray,
    val alpha: Float? = null,
    val alphaAnimationSpec: AnimationSpec<Float>? = null,
    val padding: PaddingValues = PaddingValues(all = 0.dp)
)


fun Modifier.verticalScrollWithScrollbar(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    scrollbarConfig: ScrollBarConfig = ScrollBarConfig()
) = this
    .scrollbar(
        state, Orientation.Vertical,
        indicatorThickness = scrollbarConfig.indicatorThickness,
        indicatorColor = scrollbarConfig.indicatorColor,
        alpha = scrollbarConfig.alpha ?: if (state.isScrollInProgress) 0.8f else 0f,
        alphaAnimationSpec = scrollbarConfig.alphaAnimationSpec ?: tween(
            delayMillis = if (state.isScrollInProgress) 0 else 1500,
            durationMillis = if (state.isScrollInProgress) 150 else 500
        ),
        padding = scrollbarConfig.padding
    )
    .verticalScroll(state, enabled, flingBehavior, reverseScrolling)


fun Modifier.horizontalScrollWithScrollbar(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    scrollbarConfig: ScrollBarConfig = ScrollBarConfig()
) = this
    .scrollbar(
        state, Orientation.Horizontal,
        indicatorThickness = scrollbarConfig.indicatorThickness,
        indicatorColor = scrollbarConfig.indicatorColor,
        alpha = scrollbarConfig.alpha ?: if (state.isScrollInProgress) 0.8f else 0f,
        alphaAnimationSpec = scrollbarConfig.alphaAnimationSpec ?: tween(
            delayMillis = if (state.isScrollInProgress) 0 else 1500,
            durationMillis = if (state.isScrollInProgress) 150 else 500
        ),
        padding = scrollbarConfig.padding
    )
    .horizontalScroll(state, enabled, flingBehavior, reverseScrolling)