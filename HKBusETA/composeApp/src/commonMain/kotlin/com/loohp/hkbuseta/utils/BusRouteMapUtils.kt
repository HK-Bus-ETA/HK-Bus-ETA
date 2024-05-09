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

package com.loohp.hkbuseta.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.shared.Registry


enum class BusLineType(val hasStop: Boolean) {

    START(true),
    END(true),
    INTERMEDIATE(true),
    THROUGH(false),
    EMPTY(false);

    val extensionType: BusLineType get() = when (this) {
        START, INTERMEDIATE, THROUGH -> THROUGH
        END, EMPTY -> EMPTY
    }

}

@Immutable
data class BusRouteLineData(
    val lineType: BusLineType,
    val extensionType: BusLineType,
    val color: Color
)

fun generateLineTypes(color: Color, stopDataList: List<Registry.StopData>, branch: Route?): List<BusRouteLineData> {
    val firstIndex = stopDataList.indexOfFirst { s -> branch?.let { s.branchIds.contains(it) } != false }
    val lastIndex = stopDataList.indexOfLast { s -> branch?.let { s.branchIds.contains(it) } != false }
    return stopDataList.mapIndexed { index, stopData ->
        val currentOnBranch = branch?.let { stopData.branchIds.contains(it) } != false
        val lineType = when (index) {
            firstIndex -> BusLineType.START
            lastIndex -> BusLineType.END
            in firstIndex..lastIndex -> if (currentOnBranch) BusLineType.INTERMEDIATE else BusLineType.THROUGH
            else -> BusLineType.EMPTY
        }
        BusRouteLineData(lineType, lineType.extensionType, color)
    }
}

@Composable
private fun BusLineSection(routeLineData: BusRouteLineData, extension: Boolean, highlight: RouteHighlightType = RouteHighlightType.NORMAL) {
    val (routeLineType, extensionType, color) = routeLineData
    val lineType = if (extension) extensionType else routeLineType
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        val horizontalCenter = width / 2F
        val verticalCenter = height / 2F
        val lineWidth = 11F.dp.toPx()

        when (lineType) {
            BusLineType.START -> {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenter, verticalCenter),
                    end = Offset(horizontalCenter, height),
                    strokeWidth = lineWidth
                )
            }
            BusLineType.END -> {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenter, 0F),
                    end = Offset(horizontalCenter, verticalCenter),
                    strokeWidth = lineWidth
                )
            }
            BusLineType.INTERMEDIATE, BusLineType.THROUGH -> {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenter, 0F),
                    end = Offset(horizontalCenter, height),
                    strokeWidth = lineWidth
                )
            }
            BusLineType.EMPTY -> {

            }
        }

        if (lineType.hasStop) {
            val circleWidth = 20F.dp.toPx()
            val circleCenter = Offset(horizontalCenter, verticalCenter)
            drawRoundRect(
                color = color,
                topLeft = circleCenter - Offset(circleWidth / 1.4F, circleWidth / 1.4F),
                size = Size((circleWidth / 1.4F * 2F), circleWidth / 1.4F * 2F),
                cornerRadius = CornerRadius(circleWidth / 1.4F)
            )
            drawRoundRect(
                color = highlight.color,
                topLeft = circleCenter - Offset(circleWidth / 2F, circleWidth / 2F),
                size = Size(circleWidth, circleWidth),
                cornerRadius = CornerRadius(circleWidth / 2F)
            )
        }
    }
}

@Composable
fun BusLineSection(routeLineData: BusRouteLineData, highlight: RouteHighlightType = RouteHighlightType.NORMAL) {
    BusLineSection(routeLineData, extension = false, highlight = highlight)
}

@Composable
fun BusLineSectionExtension(routeLineData: BusRouteLineData) {
    BusLineSection(routeLineData, extension = true)
}