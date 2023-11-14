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

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.getColor
import com.loohp.hkbuseta.shared.Registry.MTRInterchangeData
import com.loohp.hkbuseta.shared.Registry.StopData


@Composable
fun MTRLineSection(sectionData: MTRStopSectionData) {
    val (mainLine, spurLine, _, _, co, color, isLrtCircular, interchangeData, hasOutOfStation, stopByBranchId, _, context) = sectionData
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        val leftShift = if (hasOutOfStation) StringUtils.scaledSize((22F / density).sp.toPx(), context) else 0
        val horizontalCenter = width / 2F - leftShift.toFloat()
        val horizontalPartition = width / 10F
        val horizontalCenterPrimary = if (stopByBranchId.size == 1) horizontalCenter else horizontalPartition * 3F
        val horizontalCenterSecondary = horizontalPartition * 7F
        val verticalCenter = height / 2F
        val lineWidth = StringUtils.scaledSize((11F / density).sp.toPx(), context)
        val lineOffset = StringUtils.scaledSize((8F / density).sp.toPx(), context)
        val dashEffect = floatArrayOf(StringUtils.scaledSize((14F / density).sp.toPx(), context), StringUtils.scaledSize((7F / density).sp.toPx(), context))

        var useSpurStopCircle = false
        if (mainLine != null) {
            if (mainLine.isFirstStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterPrimary, verticalCenter),
                    end = Offset(horizontalCenterPrimary, height),
                    strokeWidth = lineWidth
                )
                if (isLrtCircular) {
                    drawLine(
                        brush = Brush.linearGradient(
                            0F to color.withAlpha(0),
                            1F to color,
                            start = Offset(horizontalCenterPrimary, -verticalCenter / 2),
                            end = Offset(horizontalCenterPrimary, verticalCenter)
                        ),
                        start = Offset(horizontalCenterPrimary, -verticalCenter),
                        end = Offset(horizontalCenterPrimary, verticalCenter),
                        strokeWidth = lineWidth
                    )
                }
            } else if (mainLine.isLastStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterPrimary, 0F),
                    end = Offset(horizontalCenterPrimary, verticalCenter),
                    strokeWidth = lineWidth
                )
                if (isLrtCircular) {
                    drawLine(
                        brush = Brush.linearGradient(
                            0F to color,
                            1F to color.withAlpha(0),
                            start = Offset(horizontalCenterPrimary, verticalCenter),
                            end = Offset(horizontalCenterPrimary, height + verticalCenter / 2)
                        ),
                        start = Offset(horizontalCenterPrimary, verticalCenter),
                        end = Offset(horizontalCenterPrimary, height + verticalCenter),
                        strokeWidth = lineWidth
                    )
                }
            } else {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterPrimary, 0F),
                    end = Offset(horizontalCenterPrimary, height),
                    strokeWidth = lineWidth
                )
            }
            if (mainLine.hasOtherParallelBranches) {
                val path = Path()
                path.moveTo(horizontalCenterPrimary, lineOffset)
                path.lineTo(horizontalCenter, lineOffset)
                path.arcTo(Rect(horizontalCenterPrimary, lineOffset, horizontalCenterSecondary, (horizontalCenterSecondary - horizontalCenter) * 2F + lineOffset), -90F, 90F, true)
                path.lineTo(horizontalCenterSecondary, height)
                drawPath(
                    color = color,
                    path = path,
                    style = Stroke(
                        width = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                    )
                )
            }
            when (mainLine.sideSpurLineType) {
                SideSpurLineType.COMBINE -> {
                    useSpurStopCircle = true
                    val path = Path()
                    path.moveTo(horizontalCenterPrimary, verticalCenter)
                    path.lineTo(horizontalCenter, verticalCenter)
                    path.arcTo(Rect(horizontalCenterPrimary, verticalCenter - (horizontalCenterSecondary - horizontalCenter) * 2F, horizontalCenterSecondary, verticalCenter), 90F, -90F, true)
                    path.lineTo(horizontalCenterSecondary, 0F)
                    drawPath(
                        color = color,
                        path = path,
                        style = Stroke(
                            width = lineWidth
                        )
                    )
                }
                SideSpurLineType.DIVERGE -> {
                    useSpurStopCircle = true
                    val path = Path()
                    path.moveTo(horizontalCenterPrimary, verticalCenter)
                    path.lineTo(horizontalCenter, verticalCenter)
                    path.arcTo(Rect(horizontalCenterPrimary, verticalCenter, horizontalCenterSecondary, verticalCenter + (horizontalCenterSecondary - horizontalCenter) * 2F), -90F, 90F, true)
                    path.lineTo(horizontalCenterSecondary, height)
                    drawPath(
                        color = color,
                        path = path,
                        style = Stroke(
                            width = lineWidth
                        )
                    )
                }
                else -> {}
            }
        } else if (spurLine != null) {
            if (spurLine.hasParallelMainLine) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterPrimary, 0F),
                    end = Offset(horizontalCenterPrimary, height),
                    strokeWidth = lineWidth
                )
            }
            val dashLineResult = spurLine.dashLineResult
            if (dashLineResult.value) {
                if (dashLineResult.isStartOfSpur) {
                    val path = Path()
                    path.moveTo(horizontalCenterPrimary, lineOffset)
                    path.lineTo(horizontalCenter, lineOffset)
                    path.arcTo(Rect(horizontalCenterPrimary, lineOffset, horizontalCenterSecondary, (horizontalCenterSecondary - horizontalCenter) * 2F + lineOffset), -90F, 90F, true)
                    path.lineTo(horizontalCenterSecondary, height)
                    drawPath(
                        color = color,
                        path = path,
                        style = Stroke(
                            width = lineWidth,
                            pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                        )
                    )
                } else if (dashLineResult.isEndOfSpur) {
                    val path = Path()
                    path.moveTo(horizontalCenterPrimary, height - lineOffset)
                    path.lineTo(horizontalCenter, height - lineOffset)
                    path.arcTo(Rect(horizontalCenterPrimary, height - (horizontalCenterSecondary - horizontalCenter) * 2F - lineOffset, horizontalCenterSecondary, height - lineOffset), 90F, -90F, true)
                    path.lineTo(horizontalCenterSecondary, 0F)
                    drawPath(
                        color = color,
                        path = path,
                        style = Stroke(
                            width = lineWidth,
                            pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                        )
                    )
                } else {
                    drawLine(
                        color = color,
                        start = Offset(horizontalCenterSecondary, 0F),
                        end = Offset(horizontalCenterSecondary, height),
                        strokeWidth = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                    )
                }
            } else if (spurLine.isFirstStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, verticalCenter),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth
                )
            } else if (spurLine.isLastStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, verticalCenter),
                    strokeWidth = lineWidth
                )
            } else {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth
                )
            }
        }
        val interchangeLineWidth = StringUtils.scaledSize((14F / density).sp.toPx(), context) * 2F
        val interchangeLineHeight = StringUtils.scaledSize((6F / density).sp.toPx(), context) * 2F
        val interchangeLineSpacing = interchangeLineHeight * 1.5F
        if (interchangeData.isHasLightRail && co != Operator.LRT) {
            drawRoundRect(
                color = Color(0xFFD3A809),
                topLeft = Offset(horizontalCenterPrimary - interchangeLineWidth, verticalCenter - interchangeLineHeight / 2F),
                size = Size(interchangeLineWidth, interchangeLineHeight),
                cornerRadius = CornerRadius(interchangeLineHeight / 2F)
            )
        } else if (interchangeData.lines.isNotEmpty()) {
            var leftCorner = Offset(horizontalCenterPrimary - interchangeLineWidth, verticalCenter - ((interchangeData.lines.size - 1) * interchangeLineSpacing / 2F) - interchangeLineHeight / 2F)
            for (interchange in interchangeData.lines) {
                drawRoundRect(
                    color = if (interchange == "HighSpeed") Color(0xFF9C948B) else Operator.MTR.getColor(interchange, Color.White),
                    topLeft = leftCorner,
                    size = Size(interchangeLineWidth, interchangeLineHeight),
                    cornerRadius = CornerRadius(interchangeLineHeight / 2F)
                )
                leftCorner += Offset(0F, interchangeLineSpacing)
            }
        }

        val circleWidth = StringUtils.scaledSize((20F / density).sp.toPx(), context)

        if (interchangeData.outOfStationLines.isNotEmpty()) {
            val otherStationHorizontalCenter = horizontalCenterPrimary + circleWidth * 2F
            val connectionLineWidth = StringUtils.scaledSize((5F / density).sp.toPx(), context)
            if (interchangeData.isOutOfStationPaid) {
                drawLine(
                    color = Color(0xFF003180),
                    start = Offset(horizontalCenterPrimary, verticalCenter),
                    end = Offset(otherStationHorizontalCenter, verticalCenter),
                    strokeWidth = connectionLineWidth
                )
            } else {
                drawLine(
                    color = Color(0xFF003180),
                    start = Offset(horizontalCenterPrimary, verticalCenter),
                    end = Offset(otherStationHorizontalCenter, verticalCenter),
                    strokeWidth = connectionLineWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(StringUtils.scaledSize((4F / density).sp.toPx(), context), StringUtils.scaledSize((2F / density).sp.toPx(), context)), 0F)
                )
            }
            var leftCorner = Offset(otherStationHorizontalCenter, verticalCenter - ((interchangeData.outOfStationLines.size - 1) * interchangeLineSpacing / 2F) - interchangeLineHeight / 2F)
            for (interchange in interchangeData.outOfStationLines) {
                drawRoundRect(
                    color = if (interchange == "HighSpeed") Color(0xFF9C948B) else Operator.MTR.getColor(interchange, Color.White),
                    topLeft = leftCorner,
                    size = Size(interchangeLineWidth, interchangeLineHeight),
                    cornerRadius = CornerRadius(interchangeLineHeight / 2F)
                )
                leftCorner += Offset(0F, interchangeLineSpacing)
            }

            val circleCenter = Offset(otherStationHorizontalCenter, verticalCenter)
            val heightExpand = ((interchangeData.outOfStationLines.size - 1) * interchangeLineSpacing).coerceAtLeast(0F)
            drawRoundRect(
                color = Color(0xFF003180),
                topLeft = circleCenter - Offset(circleWidth / 1.4F, circleWidth / 1.4F + heightExpand / 2F),
                size = Size((circleWidth / 1.4F * 2F), circleWidth / 1.4F * 2F + heightExpand),
                cornerRadius = CornerRadius(circleWidth / 1.4F)
            )
            drawRoundRect(
                color = Color(0xFFFFFFFF),
                topLeft = circleCenter - Offset(circleWidth / 2F, circleWidth / 2F + heightExpand / 2F),
                size = Size(circleWidth, circleWidth + heightExpand),
                cornerRadius = CornerRadius(circleWidth / 2F)
            )
        }

        val circleCenter = Offset(if (mainLine != null) horizontalCenterPrimary else horizontalCenterSecondary, verticalCenter)
        val widthExpand = if (useSpurStopCircle) lineWidth else 0F
        val heightExpand = ((interchangeData.lines.size - 1) * interchangeLineSpacing).coerceAtLeast(0F)
        drawRoundRect(
            color = Color(0xFF003180),
            topLeft = circleCenter - Offset(circleWidth / 1.4F, circleWidth / 1.4F + heightExpand / 2F),
            size = Size((circleWidth / 1.4F * 2F) + widthExpand, circleWidth / 1.4F * 2F + heightExpand),
            cornerRadius = CornerRadius(circleWidth / 1.4F)
        )
        drawRoundRect(
            color = Color(0xFFFFFFFF),
            topLeft = circleCenter - Offset(circleWidth / 2F, circleWidth / 2F + heightExpand / 2F),
            size = Size(circleWidth + widthExpand, circleWidth + heightExpand),
            cornerRadius = CornerRadius(circleWidth / 2F)
        )
    }
}

@Composable
fun MTRLineSectionExtension(sectionData: MTRStopSectionData) {
    val (mainLine, spurLine, _, _, _, color, _, _, hasOutOfStation, stopByBranchId, requireExtension, context) = sectionData
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!requireExtension) {
            return@Canvas
        }

        val width = size.width
        val height = size.height

        val leftShift = if (hasOutOfStation) StringUtils.scaledSize((22F / density).sp.toPx(), context) else 0
        val horizontalCenter = width / 2F - leftShift.toFloat()
        val horizontalPartition = width / 10F
        val horizontalCenterPrimary = if (stopByBranchId.size == 1) horizontalCenter else horizontalPartition * 3F
        val horizontalCenterSecondary = horizontalPartition * 7F
        val lineWidth = StringUtils.scaledSize((11F / density).sp.toPx(), context)
        val dashEffect = floatArrayOf(StringUtils.scaledSize((14F / density).sp.toPx(), context), StringUtils.scaledSize((7F / density).sp.toPx(), context))

        if (mainLine != null) {
            drawLine(
                color = color,
                start = Offset(horizontalCenterPrimary, 0F),
                end = Offset(horizontalCenterPrimary, height),
                strokeWidth = lineWidth
            )
            if (mainLine.hasOtherParallelBranches) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth,
                    pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                )
            }
            when (mainLine.sideSpurLineType) {
                SideSpurLineType.DIVERGE -> {
                    drawLine(
                        color = color,
                        start = Offset(horizontalCenterSecondary, 0F),
                        end = Offset(horizontalCenterSecondary, height),
                        strokeWidth = lineWidth
                    )
                }
                else -> {}
            }
        } else if (spurLine != null) {
            drawLine(
                color = color,
                start = Offset(horizontalCenterPrimary, 0F),
                end = Offset(horizontalCenterPrimary, height),
                strokeWidth = lineWidth
            )
            val dashLineResult = spurLine.dashLineResult
            if (dashLineResult.value) {
                if (dashLineResult.isStartOfSpur) {
                    drawLine(
                        color = color,
                        start = Offset(horizontalCenterSecondary, 0F),
                        end = Offset(horizontalCenterSecondary, height),
                        strokeWidth = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                    )
                } else if (!dashLineResult.isEndOfSpur) {
                    drawLine(
                        color = color,
                        start = Offset(horizontalCenterSecondary, 0F),
                        end = Offset(horizontalCenterSecondary, height),
                        strokeWidth = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                    )
                }
            } else if (spurLine.isFirstStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth
                )
            } else if (!spurLine.isLastStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth
                )
            }
        }
    }
}

fun createMTRLineSectionData(co: Operator, color: Color, stopList: List<StopData>, mtrStopsInterchange: List<MTRInterchangeData>, isLrtCircular: Boolean, context: Context): List<MTRStopSectionData> {
    val stopByBranchId: MutableMap<Int, MutableList<StopData>> = HashMap()
    stopList.forEach { stop -> stop.branchIds.forEach { stopByBranchId.computeIfAbsent(it) { ArrayList() }.add(stop) } }
    val hasOutOfStation = mtrStopsInterchange.any { it.outOfStationLines.isNotEmpty() }
    return stopList.withIndex().map {
        val (index, stop) = it
        MTRStopSectionData.build(stop.serviceType == 1, stopByBranchId, index, stop, stopList, co, color, isLrtCircular, mtrStopsInterchange[index], hasOutOfStation, context)
    }
}

@Immutable
data class MTRStopSectionData(
    val mainLine: MTRStopSectionMainLineData?,
    val spurLine: MTRStopSectionSpurLineData?,
    val index: Int,
    val stop: StopData,
    val co: Operator,
    val color: Color,
    val isLrtCircular: Boolean,
    val interchangeData: MTRInterchangeData,
    val hasOutOfStation: Boolean,
    val stopByBranchId: MutableMap<Int, MutableList<StopData>>,
    val requireExtension: Boolean,
    val context: Context
) {
    companion object {
        fun build(isMainLine: Boolean, stopByBranchId: MutableMap<Int, MutableList<StopData>>, index: Int, stop: StopData, stopList: List<StopData>, co: Operator, color: Color, isLrtCircular: Boolean, interchangeData: MTRInterchangeData, hasOutOfStation: Boolean, context: Context): MTRStopSectionData {
            val requireExtension = index + 1 < stopList.size
            return if (isMainLine) MTRStopSectionData(MTRStopSectionMainLineData(
                isFirstStation = stopByBranchId.values.all { it.indexOf(stop) <= 0 },
                isLastStation = stopByBranchId.values.all { it.indexOf(stop).let { x -> x < 0 || x >= it.size - 1 } },
                hasOtherParallelBranches = hasOtherParallelBranches(stopList, stopByBranchId, stop),
                sideSpurLineType = getSideSpurLineType(stopList, stopByBranchId, stop)
            ), null, index, stop, co, color, isLrtCircular, interchangeData, hasOutOfStation, stopByBranchId, requireExtension, context) else MTRStopSectionData(null, MTRStopSectionSpurLineData(
                hasParallelMainLine = index > 0 && index < stopList.size - 1,
                dashLineResult = isDashLineSpur(stopList, stop),
                isFirstStation = stopByBranchId.values.all { it.indexOf(stop) <= 0 },
                isLastStation = stopByBranchId.values.all { it.indexOf(stop).let { x -> x < 0 || x >= it.size - 1 } }
            ), index, stop, co, color, isLrtCircular, interchangeData, hasOutOfStation, stopByBranchId, requireExtension, context)
        }
    }
}

@Immutable
data class MTRStopSectionMainLineData(
    val isFirstStation: Boolean,
    val isLastStation: Boolean,
    val hasOtherParallelBranches: Boolean,
    val sideSpurLineType: SideSpurLineType
)

@Immutable
data class MTRStopSectionSpurLineData(
    val hasParallelMainLine: Boolean,
    val dashLineResult: DashLineSpurResult,
    val isFirstStation: Boolean,
    val isLastStation: Boolean
)

private fun hasOtherParallelBranches(stopList: List<StopData>, stopByBranchId: MutableMap<Int, MutableList<StopData>>, stop: StopData): Boolean {
    if (stopByBranchId.size == stopByBranchId.filter { it.value.contains(stop) }.size) {
        return false
    }
    val mainIndex = stopList.indexOf(stop)
    val branchIds = stop.branchIds
    var branchStart = -1
    var branchStartStop: StopData? = null
    for (i in (mainIndex - 1) downTo 0) {
        if (stopList[i].branchIds != branchIds && stopList[i].branchIds.containsAll(branchIds)) {
            branchStart = i
            branchStartStop = stopList[i]
            break
        }
    }
    if (branchStartStop == null) {
        for (i in stopList.indices) {
            if (stopList[i].branchIds != branchIds) {
                branchStart = i
                branchStartStop = stopList[i]
                break
            }
        }
    }
    var branchEnd = stopList.size
    var branchEndStop: StopData? = null
    for (i in (mainIndex + 1) until stopList.size) {
        if (stopList[i].branchIds != branchIds && stopList[i].branchIds.containsAll(branchIds)) {
            branchEnd = i
            branchEndStop = stopList[i]
            break
        }
    }
    if (branchEndStop == null) {
        for (i in (stopList.size - 1) downTo 0) {
            if (stopList[i].branchIds != branchIds) {
                branchEnd = i
                branchEndStop = stopList[i]
                break
            }
        }
    }
    val matchingBranchStart = branchStart == mainIndex
    val matchingBranchEnd = branchEnd == mainIndex
    val isStartOfSpur = matchingBranchStart && stopByBranchId.values.none { it.indexOf(branchStartStop) <= 0 }
    val isEndOfSpur = matchingBranchEnd && stopByBranchId.values.none { it.indexOf(branchEndStop) >= (it.size - 1) }
    if (matchingBranchStart != isStartOfSpur || matchingBranchEnd != isEndOfSpur) {
        return false
    }
    return mainIndex in branchStart..branchEnd
}

enum class SideSpurLineType {

    NONE, COMBINE, DIVERGE

}

private fun getSideSpurLineType(stopList: List<StopData>, stopByBranchId: MutableMap<Int, MutableList<StopData>>, stop: StopData): SideSpurLineType {
    val mainIndex = stopList.indexOf(stop)
    val branchIds = stop.branchIds
    if (mainIndex > 0) {
        if (stopList[mainIndex - 1].branchIds != branchIds) {
            if (stopByBranchId.values.all { (!it.contains(stopList[mainIndex - 1]) || it.subList(0, it.indexOf(stopList[mainIndex - 1])).none { that -> that.branchIds.containsAll(branchIds) }) && it.indexOf(stop) != 0 }) {
                return SideSpurLineType.COMBINE
            }
        }
    }
    if (mainIndex < stopList.size - 1) {
        if (stopList[mainIndex + 1].branchIds != branchIds) {
            if (stopByBranchId.values.all { (!it.contains(stopList[mainIndex + 1]) || it.subList(it.indexOf(stopList[mainIndex + 1]) + 1, it.size).none { that -> that.branchIds.containsAll(branchIds) }) && it.indexOf(stop) < it.size - 1 }) {
                return SideSpurLineType.DIVERGE
            }
        }
    }
    return SideSpurLineType.NONE
}

data class DashLineSpurResult(val value: Boolean, val isStartOfSpur: Boolean, val isEndOfSpur: Boolean) {

    companion object {

        val FALSE = DashLineSpurResult(value = false, isStartOfSpur = false, isEndOfSpur = false)

    }

}

private fun isDashLineSpur(stopList: List<StopData>, stop: StopData): DashLineSpurResult {
    val mainIndex = stopList.indexOf(stop)
    val branchIds = stop.branchIds
    var possible = false
    var branchStart = false
    for (i in (mainIndex - 1) downTo 0) {
        if (stopList[i].branchIds.containsAll(branchIds)) {
            if (i + 1 == mainIndex && stopList[i].branchIds != branchIds) {
                branchStart = true
            }
            possible = true
            break
        }
    }
    if (!possible) {
        return DashLineSpurResult.FALSE
    }
    for (i in (mainIndex + 1) until stopList.size) {
        if (stopList[i].branchIds.containsAll(branchIds)) {
            return DashLineSpurResult(true, branchStart, i - 1 == mainIndex && stopList[i].branchIds != branchIds)
        }
    }
    return DashLineSpurResult.FALSE
}