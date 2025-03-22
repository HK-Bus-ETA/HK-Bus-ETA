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

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.ajalt.colormath.model.RGB
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.getColor
import com.loohp.hkbuseta.common.objects.getLineColor
import com.loohp.hkbuseta.common.objects.getOperatorColor
import com.loohp.hkbuseta.compose.platformLocalContentColor
import kotlin.math.sqrt


val Color.brightness: Float get() = 0.2126F * red + 0.7152F * green + 0.0722F * blue

fun Color.adjustBrightness(percentage: Float): Color {
    if (percentage == 1F) {
        return this
    }
    return RGB(red, green, blue).toHSV().let {
        if (percentage > 1F) {
            it.copy(s = (it.s * (1F - (percentage - 1F))).coerceIn(0F, 1F))
        } else {
            it.copy(v = (it.v * percentage).coerceIn(0F, 1F))
        }
    }.toSRGB().let { Color(it.r, it.g, it.b, alpha) }
}

fun Color.adjustAlpha(percentage: Float): Color {
    val alpha = (alpha * percentage).coerceIn(0F, 1F)
    return copy(alpha = alpha)
}

fun Color.withAlpha(alpha: Int): Color {
    val value = toArgb() and 0x00FFFFFF
    val alphaShifted = (alpha shl 24) and -16777216
    return Color(value or alphaShifted)
}

@OptIn(ExperimentalStdlibApi::class)
fun Color.toHexString(): String {
    return "#${toArgb().toHexString(HexFormat.UpperCase).padStart(6, '0').takeLast(6)}"
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

fun Color.closenessTo(target: Color): Float {
    val r = red * 255
    val g = green * 255
    val b = blue * 255
    val targetR = target.red * 255
    val targetG = target.green * 255
    val targetB = target.blue * 255
    val distance = sqrt((r - targetR) * (r - targetR) + (g - targetG) * (g - targetG) + (b - targetB) * (b - targetB))
    val maxDistance = sqrt((255F * 255F) * 2)
    return 1 - (distance / maxDistance)
}

@Composable
fun ButtonDefaults.clearColors(
    containerColor: Color = Color.Transparent,
    contentColor: Color = platformLocalContentColor,
    disabledContainerColor: Color = Color.Transparent,
    disabledContentColor: Color = Color.Transparent,
): ButtonColors {
    return buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )
}