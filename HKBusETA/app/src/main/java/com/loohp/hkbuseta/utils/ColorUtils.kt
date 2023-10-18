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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red


fun Color.adjustBrightness(percentage: Float): Color {
    if (percentage == 1F) {
        return this
    }
    val argb = toArgb()
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(argb.red, argb.green, argb.blue, hsv)
    if (percentage > 1F) {
        hsv[1] = (hsv[1] * (1F - (percentage - 1F))).coerceAtLeast(0F).coerceAtMost(1F)
    } else {
        hsv[2] = (hsv[2] * percentage).coerceAtLeast(0F).coerceAtMost(1F)
    }
    return Color(android.graphics.Color.HSVToColor(argb.alpha, hsv))
}