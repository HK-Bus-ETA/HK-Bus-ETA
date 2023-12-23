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

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.shared.Registry.MTRInterchangeData
import com.loohp.hkbuseta.common.shared.Registry.StopData
import com.loohp.hkbuseta.common.utils.indexesOf
import kotlin.math.absoluteValue


@Composable
fun MTRLineSection(sectionData: MTRStopSectionData, ambientMode: Boolean) {
    val (mainLine, spurLine, _, _, co, color, isLrtCircular, interchangeData, hasOutOfStation, stopByBranchId, _, context) = sectionData
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        val leftShift = if (hasOutOfStation) (22F / density).sp.toPx().scaledSize(context) else 0
        val horizontalCenter = width / 2F - leftShift.toFloat()
        val horizontalPartition = width / 10F
        val horizontalCenterPrimary = if (stopByBranchId.size == 1) horizontalCenter else horizontalPartition * 3F
        val horizontalCenterSecondary = horizontalPartition * 7F
        val verticalCenter = height / 2F
        val lineWidth = (11F / density).sp.toPx().scaledSize(context)
        val outlineWidth = lineWidth * 0.6F
        val lineOffset = (8F / density).sp.toPx().scaledSize(context)
        val dashEffect = floatArrayOf((14F / density).sp.toPx().scaledSize(context), (7F / density).sp.toPx().scaledSize(context))

        var useSpurStopCircle = false
        if (mainLine != null) {
            if (mainLine.isFirstStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterPrimary, verticalCenter),
                    end = Offset(horizontalCenterPrimary, height),
                    strokeWidth = lineWidth,
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
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
                        strokeWidth = lineWidth,
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
                    )
                }
            } else if (mainLine.isLastStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterPrimary, 0F),
                    end = Offset(horizontalCenterPrimary, verticalCenter),
                    strokeWidth = lineWidth,
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
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
                        strokeWidth = lineWidth,
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
                    )
                }
            } else {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterPrimary, 0F),
                    end = Offset(horizontalCenterPrimary, height),
                    strokeWidth = lineWidth,
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
                )
            }
            if (mainLine.hasOtherParallelBranches) {
                drawPath(
                    color = color,
                    path = Path().apply {
                        moveTo(horizontalCenterPrimary, lineOffset)
                        lineTo(horizontalCenter, lineOffset)
                        arcTo(Rect(horizontalCenterPrimary, lineOffset, horizontalCenterSecondary, (horizontalCenterSecondary - horizontalCenter) * 2F + lineOffset), -90F, 90F, true)
                        lineTo(horizontalCenterSecondary, height)
                    },
                    style = Stroke(
                        width = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                    ),
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
                )
            }
            when (mainLine.sideSpurLineType) {
                SideSpurLineType.COMBINE -> {
                    useSpurStopCircle = true
                    drawPath(
                        color = color,
                        path = Path().apply {
                            moveTo(horizontalCenterPrimary, verticalCenter)
                            lineTo(horizontalCenter, verticalCenter)
                            arcTo(Rect(horizontalCenterPrimary, verticalCenter - (horizontalCenterSecondary - horizontalCenter) * 2F, horizontalCenterSecondary, verticalCenter), 90F, -90F, true)
                            lineTo(horizontalCenterSecondary, 0F)
                        },
                        style = Stroke(
                            width = lineWidth
                        ),
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
                    )
                }
                SideSpurLineType.DIVERGE -> {
                    useSpurStopCircle = true
                    drawPath(
                        color = color,
                        path = Path().apply {
                            moveTo(horizontalCenterPrimary, verticalCenter)
                            lineTo(horizontalCenter, verticalCenter)
                            arcTo(Rect(horizontalCenterPrimary, verticalCenter, horizontalCenterSecondary, verticalCenter + (horizontalCenterSecondary - horizontalCenter) * 2F), -90F, 90F, true)
                            lineTo(horizontalCenterSecondary, height)
                        },
                        style = Stroke(
                            width = lineWidth
                        ),
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
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
                    strokeWidth = lineWidth,
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
                )
            }
            val dashLineResult = spurLine.dashLineResult
            if (dashLineResult.value) {
                if (dashLineResult.isStartOfSpur) {
                    drawPath(
                        color = color,
                        path = Path().apply {
                            moveTo(horizontalCenterPrimary, lineOffset)
                            lineTo(horizontalCenter, lineOffset)
                            arcTo(Rect(horizontalCenterPrimary, lineOffset, horizontalCenterSecondary, (horizontalCenterSecondary - horizontalCenter) * 2F + lineOffset), -90F, 90F, true)
                            lineTo(horizontalCenterSecondary, height)
                        },
                        style = Stroke(
                            width = lineWidth,
                            pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                        ),
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
                    )
                } else if (dashLineResult.isEndOfSpur) {
                    drawPath(
                        color = color,
                        path = Path().apply {
                            moveTo(horizontalCenterPrimary, height - lineOffset)
                            lineTo(horizontalCenter, height - lineOffset)
                            arcTo(Rect(horizontalCenterPrimary, height - (horizontalCenterSecondary - horizontalCenter) * 2F - lineOffset, horizontalCenterSecondary, height - lineOffset), 90F, -90F, true)
                            lineTo(horizontalCenterSecondary, 0F)
                        },
                        style = Stroke(
                            width = lineWidth,
                            pathEffect = PathEffect.dashPathEffect(dashEffect, 0F)
                        ),
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
                    )
                } else {
                    drawLine(
                        color = color,
                        start = Offset(horizontalCenterSecondary, 0F),
                        end = Offset(horizontalCenterSecondary, height),
                        strokeWidth = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dashEffect, 0F),
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
                    )
                }
            } else if (spurLine.isFirstStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, verticalCenter),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth,
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
                )
            } else if (spurLine.isLastStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, verticalCenter),
                    strokeWidth = lineWidth,
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
                )
            } else {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth,
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
                )
            }
        }
        val interchangeLineWidth = (14F / density).sp.toPx().scaledSize(context) * 2F
        val interchangeLineHeight = (6F / density).sp.toPx().scaledSize(context) * 2F
        val interchangeLineSpacing = interchangeLineHeight * 1.5F
        if (interchangeData.isHasLightRail && co != Operator.LRT) {
            drawRoundRect(
                color = Color(0xFFD3A809),
                topLeft = Offset(horizontalCenterPrimary - interchangeLineWidth, verticalCenter - interchangeLineHeight / 2F),
                size = Size(interchangeLineWidth, interchangeLineHeight),
                cornerRadius = CornerRadius(interchangeLineHeight / 2F),
                outlineMode = ambientMode,
                outlineWidth = outlineWidth * 0.75F
            )
        } else if (interchangeData.lines.isNotEmpty()) {
            var leftCorner = Offset(horizontalCenterPrimary - interchangeLineWidth, verticalCenter - ((interchangeData.lines.size - 1) * interchangeLineSpacing / 2F) - interchangeLineHeight / 2F)
            for (interchange in interchangeData.lines) {
                drawRoundRect(
                    color = if (interchange == "HighSpeed") Color(0xFF9C948B) else Operator.MTR.getColor(interchange, Color.White),
                    topLeft = leftCorner,
                    size = Size(interchangeLineWidth, interchangeLineHeight),
                    cornerRadius = CornerRadius(interchangeLineHeight / 2F),
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth * 0.75F
                )
                leftCorner += Offset(0F, interchangeLineSpacing)
            }
        }

        val circleWidth = (20F / density).sp.toPx().scaledSize(context)

        if (interchangeData.outOfStationLines.isNotEmpty()) {
            val otherStationHorizontalCenter = horizontalCenterPrimary + circleWidth * 2F
            val connectionLineWidth = (5F / density).sp.toPx().scaledSize(context)
            if (interchangeData.isOutOfStationPaid) {
                drawLine(
                    color = Color(0xFF003180).adjustBrightness(if (ambientMode) 1.25F else 1F),
                    start = Offset(horizontalCenterPrimary, verticalCenter),
                    end = Offset(otherStationHorizontalCenter, verticalCenter),
                    strokeWidth = connectionLineWidth
                )
            } else {
                drawLine(
                    color = Color(0xFF003180).adjustBrightness(if (ambientMode) 1.25F else 1F),
                    start = Offset(horizontalCenterPrimary, verticalCenter),
                    end = Offset(otherStationHorizontalCenter, verticalCenter),
                    strokeWidth = connectionLineWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf((4F / density).sp.toPx().scaledSize(context), (2F / density).sp.toPx().scaledSize(context)), 0F)
                )
            }
            var leftCorner = Offset(otherStationHorizontalCenter, verticalCenter - ((interchangeData.outOfStationLines.size - 1) * interchangeLineSpacing / 2F) - interchangeLineHeight / 2F)
            for (interchange in interchangeData.outOfStationLines) {
                drawRoundRect(
                    color = if (interchange == "HighSpeed") Color(0xFF9C948B) else Operator.MTR.getColor(interchange, Color.White),
                    topLeft = leftCorner,
                    size = Size(interchangeLineWidth, interchangeLineHeight),
                    cornerRadius = CornerRadius(interchangeLineHeight / 2F),
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth * 0.75F
                )
                leftCorner += Offset(0F, interchangeLineSpacing)
            }

            val circleCenter = Offset(otherStationHorizontalCenter, verticalCenter)
            val heightExpand = ((interchangeData.outOfStationLines.size - 1) * interchangeLineSpacing).coerceAtLeast(0F)
            drawRoundRect(
                color = Color(0xFF003180).adjustBrightness(if (ambientMode) 1.25F else 1F),
                topLeft = circleCenter - Offset(circleWidth / 1.4F, circleWidth / 1.4F + heightExpand / 2F),
                size = Size((circleWidth / 1.4F * 2F), circleWidth / 1.4F * 2F + heightExpand),
                cornerRadius = CornerRadius(circleWidth / 1.4F)
            )
            drawRoundRect(
                color = if (ambientMode) Color(0xFF000000) else Color(0xFFFFFFFF),
                topLeft = circleCenter - Offset(circleWidth / 2F, circleWidth / 2F + heightExpand / 2F),
                size = Size(circleWidth, circleWidth + heightExpand),
                cornerRadius = CornerRadius(circleWidth / 2F)
            )
        }

        val circleCenter = Offset(if (mainLine != null) horizontalCenterPrimary else horizontalCenterSecondary, verticalCenter)
        val widthExpand = if (useSpurStopCircle) lineWidth else 0F
        val heightExpand = ((interchangeData.lines.size - 1) * interchangeLineSpacing).coerceAtLeast(0F)
        drawRoundRect(
            color = Color(0xFF003180).adjustBrightness(if (ambientMode) 1.25F else 1F),
            topLeft = circleCenter - Offset(circleWidth / 1.4F, circleWidth / 1.4F + heightExpand / 2F),
            size = Size((circleWidth / 1.4F * 2F) + widthExpand, circleWidth / 1.4F * 2F + heightExpand),
            cornerRadius = CornerRadius(circleWidth / 1.4F)
        )
        drawRoundRect(
            color = if (ambientMode) Color(0xFF000000) else Color(0xFFFFFFFF),
            topLeft = circleCenter - Offset(circleWidth / 2F, circleWidth / 2F + heightExpand / 2F),
            size = Size(circleWidth + widthExpand, circleWidth + heightExpand),
            cornerRadius = CornerRadius(circleWidth / 2F)
        )
    }
}

@Composable
fun MTRLineSectionExtension(sectionData: MTRStopSectionData, ambientMode: Boolean) {
    val (mainLine, spurLine, _, _, _, color, _, _, hasOutOfStation, stopByBranchId, requireExtension, context) = sectionData
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!requireExtension) {
            return@Canvas
        }

        val width = size.width
        val height = size.height

        val leftShift = if (hasOutOfStation) (22F / density).sp.toPx().scaledSize(context) else 0
        val horizontalCenter = width / 2F - leftShift.toFloat()
        val horizontalPartition = width / 10F
        val horizontalCenterPrimary = if (stopByBranchId.size == 1) horizontalCenter else horizontalPartition * 3F
        val horizontalCenterSecondary = horizontalPartition * 7F
        val lineWidth = (11F / density).sp.toPx().scaledSize(context)
        val outlineWidth = lineWidth * 0.6F
        val dashEffect = floatArrayOf((14F / density).sp.toPx().scaledSize(context), (7F / density).sp.toPx().scaledSize(context))

        if (mainLine != null) {
            drawLine(
                color = color,
                start = Offset(horizontalCenterPrimary, 0F),
                end = Offset(horizontalCenterPrimary, height),
                strokeWidth = lineWidth,
                outlineMode = ambientMode,
                outlineWidth = outlineWidth
            )
            if (mainLine.hasOtherParallelBranches) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth,
                    pathEffect = PathEffect.dashPathEffect(dashEffect, 0F),
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
                )
            }
            when (mainLine.sideSpurLineType) {
                SideSpurLineType.DIVERGE -> {
                    drawLine(
                        color = color,
                        start = Offset(horizontalCenterSecondary, 0F),
                        end = Offset(horizontalCenterSecondary, height),
                        strokeWidth = lineWidth,
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
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
                        pathEffect = PathEffect.dashPathEffect(dashEffect, 0F),
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
                    )
                } else if (!dashLineResult.isEndOfSpur) {
                    drawLine(
                        color = color,
                        start = Offset(horizontalCenterSecondary, 0F),
                        end = Offset(horizontalCenterSecondary, height),
                        strokeWidth = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dashEffect, 0F),
                        outlineMode = ambientMode,
                        outlineWidth = outlineWidth
                    )
                }
            } else if (spurLine.isFirstStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth,
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
                )
            } else if (!spurLine.isLastStation) {
                drawLine(
                    color = color,
                    start = Offset(horizontalCenterSecondary, 0F),
                    end = Offset(horizontalCenterSecondary, height),
                    strokeWidth = lineWidth,
                    outlineMode = ambientMode,
                    outlineWidth = outlineWidth
                )
            }
        }
    }
}

fun createMTRLineSectionData(co: Operator, color: Color, stopList: List<StopData>, mtrStopsInterchange: List<MTRInterchangeData>, isLrtCircular: Boolean, context: AppContext): List<MTRStopSectionData> {
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
    val context: AppContext
) {
    companion object {
        fun build(isMainLine: Boolean, stopByBranchId: MutableMap<Int, MutableList<StopData>>, index: Int, stop: StopData, stopList: List<StopData>, co: Operator, color: Color, isLrtCircular: Boolean, interchangeData: MTRInterchangeData, hasOutOfStation: Boolean, context: AppContext): MTRStopSectionData {
            val requireExtension = index + 1 < stopList.size
            return if (isMainLine) MTRStopSectionData(MTRStopSectionMainLineData(
                isFirstStation = stopByBranchId.values.all { it.indexesOf(stop).minBy { i -> (i - index).absoluteValue } <= 0 },
                isLastStation = stopByBranchId.values.all { it.indexesOf(stop).minBy { i -> (i - index).absoluteValue }.let { x -> x < 0 || x >= it.size - 1 } },
                hasOtherParallelBranches = hasOtherParallelBranches(stopList, stopByBranchId, stop),
                sideSpurLineType = getSideSpurLineType(stopList, stopByBranchId, stop)
            ), null, index, stop, co, color, isLrtCircular, interchangeData, hasOutOfStation, stopByBranchId, requireExtension, context) else MTRStopSectionData(null, MTRStopSectionSpurLineData(
                hasParallelMainLine = index > 0 && index < stopList.size - 1,
                dashLineResult = isDashLineSpur(stopList, stop),
                isFirstStation = stopByBranchId.values.all { it.indexesOf(stop).minBy { i -> (i - index).absoluteValue } <= 0 },
                isLastStation = stopByBranchId.values.all { it.indexesOf(stop).minBy { i -> (i - index).absoluteValue }.let { x -> x < 0 || x >= it.size - 1 } }
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

@Suppress("NOTHING_TO_INLINE")
inline fun DrawScope.drawLine(
    brush: Brush,
    start: Offset,
    end: Offset,
    strokeWidth: Float = Stroke.HairlineWidth,
    cap: StrokeCap = Stroke.DefaultCap,
    pathEffect: PathEffect? = null,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
    outlineMode: Boolean,
    outlineWidth: Float
) {
    drawLine(brush, start, end, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode)
    if (outlineMode) {
        drawLine(Color(0xFF000000), start, end, strokeWidth - outlineWidth, cap, pathEffect, alpha, colorFilter, blendMode)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun DrawScope.drawLine(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float = Stroke.HairlineWidth,
    cap: StrokeCap = Stroke.DefaultCap,
    pathEffect: PathEffect? = null,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
    outlineMode: Boolean,
    outlineWidth: Float
) {
    drawLine(color, start, end, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode)
    if (outlineMode) {
        drawLine(Color(0xFF000000), start, end, strokeWidth - outlineWidth, cap, pathEffect, alpha, colorFilter, blendMode)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun DrawScope.drawPath(
    path: Path,
    color: Color,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
    outlineMode: Boolean,
    outlineWidth: Float
) {
    drawPath(path, color, alpha, style, colorFilter, blendMode)
    if (outlineMode && style is Stroke) {
        val stroke = Stroke(
            width = style.width - outlineWidth,
            miter = style.miter,
            cap = style.cap,
            join = style.join,
            pathEffect = style.pathEffect
        )
        drawPath(path, Color(0xFF000000), alpha, stroke, colorFilter, blendMode)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun DrawScope.drawRoundRect(
    color: Color,
    topLeft: Offset = Offset.Zero,
    size: Size = Size(this.size.width - topLeft.x, this.size.height - topLeft.y),
    cornerRadius: CornerRadius = CornerRadius.Zero,
    style: DrawStyle = Fill,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
    outlineMode: Boolean,
    outlineWidth: Float
) {
    drawRoundRect(color, topLeft, size, cornerRadius, style, alpha, colorFilter, blendMode)
    if (outlineMode) {
        drawRoundRect(Color(0xFF000000), topLeft + Offset(outlineWidth / 2, outlineWidth / 2), Size(size.width - outlineWidth, size.height - outlineWidth), cornerRadius, style, alpha, colorFilter, blendMode)
    }
}