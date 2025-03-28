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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.alpha
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.getColor
import com.loohp.hkbuseta.common.objects.getLineColor
import com.loohp.hkbuseta.common.objects.getOperatorColor


fun Color.adjustBrightness(percentage: Float): Color {
    if (percentage == 1F) {
        return this
    }
    val argb = toArgb()
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    if (percentage > 1F) {
        hsv[1] = (hsv[1] * (1F - (percentage - 1F))).coerceIn(0F, 1F)
    } else {
        hsv[2] = (hsv[2] * percentage).coerceIn(0F, 1F)
    }
    return Color(android.graphics.Color.HSVToColor(argb.alpha, hsv))
}

fun Color.withAlpha(alpha: Int): Color {
    val value = toArgb() and 0x00FFFFFF
    val alphaShifted = (alpha shl 24) and -16777216
    return Color(value or alphaShifted)
}

fun Color.adjustAlpha(percentage: Float): Color {
    val alpha = (alpha * percentage).coerceIn(0F, 1F)
    return copy(alpha = alpha)
}

fun Color.toHexString(): String {
    return "#${(this.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')}"
}

fun Operator.getOperatorColor(elseColor: Color): Color {
    return Color(getOperatorColor(elseColor.toArgb().toLong()))
}

fun Operator.getColor(routeNumber: String, elseColor: Color): Color {
    return Color(getColor(routeNumber, elseColor.toArgb().toLong()))
}

fun Operator.getLineColor(routeNumber: String, elseColor: Color): Color {
    return Color(getLineColor(routeNumber, elseColor.toArgb().toLong()))
}