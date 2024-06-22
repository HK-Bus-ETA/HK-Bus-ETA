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

package com.loohp.hkbuseta.compose.colorpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawHorizontalSelector(amount: Float) {
    val halfIndicatorThickness = 4.dp.toPx()
    val strokeThickness = 1.dp.toPx()

    val offset =
        Offset(
            x = amount - halfIndicatorThickness,
            y = -strokeThickness
        )

    val selectionSize = Size(halfIndicatorThickness * 2f, this.size.height + strokeThickness * 2)
    drawSelectorIndicator(
        offset = offset,
        selectionSize = selectionSize,
        strokeThicknessPx = strokeThickness
    )
}

internal fun DrawScope.drawVerticalSelector(amount: Float) {
    val halfIndicatorThickness = 4.dp.toPx()
    val strokeThickness = 1.dp.toPx()

    val offset =
        Offset(
            y = amount - halfIndicatorThickness,
            x = -strokeThickness
        )
    val selectionSize = Size(this.size.width + strokeThickness * 2, halfIndicatorThickness * 2f)
    drawSelectorIndicator(
        offset = offset,
        selectionSize = selectionSize,
        strokeThicknessPx = strokeThickness
    )
}

internal fun DrawScope.drawSelectorIndicator(
    offset: Offset,
    selectionSize: Size,
    strokeThicknessPx: Float
) {
    val selectionStyle = Stroke(strokeThicknessPx)
    drawRect(
        Color.Gray,
        topLeft = offset,
        size = selectionSize,
        style = selectionStyle
    )
    drawRect(
        Color.White,
        topLeft = offset + Offset(strokeThicknessPx, strokeThicknessPx),
        size = selectionSize.inset(2 * strokeThicknessPx),
        style = selectionStyle
    )
}

internal fun Size.inset(amount: Float): Size {
    return Size(width - amount, height - amount)
}
