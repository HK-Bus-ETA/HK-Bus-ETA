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
    var value = hsv[2]
    value *= percentage
    hsv[2] = value.coerceAtLeast(0F).coerceAtMost(1F)
    return Color(android.graphics.Color.HSVToColor(argb.alpha, hsv))
}