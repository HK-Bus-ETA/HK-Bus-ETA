package com.loohp.hkbuseta.presentation.utils

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