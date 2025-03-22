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

package com.loohp.hkbuseta.utils

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.utils.MTRStopSectionData
import com.loohp.hkbuseta.common.utils.SideSpurLineType


@Composable
fun MTRLineSection(sectionData: MTRStopSectionData, ambientMode: Boolean) {
    val (mainLine, spurLine, _, _, co, rawColor, isLrtCircular, interchangeData, hasOutOfStation, stopByBranchId, _, context) = sectionData
    val color = Color(rawColor)
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
    val (mainLine, spurLine, _, _, _, rawColor, _, _, hasOutOfStation, stopByBranchId, requireExtension, context) = sectionData
    val color = Color(rawColor)
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