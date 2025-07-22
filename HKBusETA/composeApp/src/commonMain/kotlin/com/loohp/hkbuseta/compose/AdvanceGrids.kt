package com.loohp.hkbuseta.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.loohp.hkbuseta.common.utils.ceilToInt


@Composable
fun VerticalGrid(
    columns: Int,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(8.dp),
    content: @Composable () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val topPadding = paddingValues.calculateTopPadding()
    val bottomPadding = paddingValues.calculateBottomPadding()
    val startPadding = paddingValues.calculateStartPadding(layoutDirection)
    val endPadding = paddingValues.calculateEndPadding(layoutDirection)
    val verticalPadding = topPadding + bottomPadding
    val horizontalPadding = startPadding + endPadding
    Layout(
        modifier = modifier,
        content = content,
        measurePolicy = { measurables, constraints ->
            val width = constraints.maxWidth
            val individualWidth = width / columns - horizontalPadding.roundToPx()
            val individualMaxHeight = measurables.maxOf { it.minIntrinsicHeight(individualWidth) }
            val individualConstraint = constraints.copy(
                minWidth = individualWidth,
                maxWidth = individualWidth,
                minHeight = individualMaxHeight,
                maxHeight = individualMaxHeight
            )
            val placeables = measurables.map { it.measure(individualConstraint) }
            val height = (individualMaxHeight + verticalPadding.roundToPx()) * (placeables.size / columns.toFloat()).ceilToInt()
            layout(width, height) {
                for ((index, placeable) in placeables.withIndex()) {
                    val x = ((individualWidth + horizontalPadding.roundToPx()) * index) % width + startPadding.roundToPx()
                    val y = (index / columns) * (individualMaxHeight + verticalPadding.roundToPx()) + topPadding.roundToPx()
                    placeable.placeRelative(x, y)
                }
            }
        }
    )
}