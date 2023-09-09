package com.loohp.hkbuseta.presentation.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost


inline val Dp.sp: TextUnit @Composable get() = with (LocalDensity.current) { this@sp.toSp() }

inline val TextUnit.dp: Dp @Composable get() = with (LocalDensity.current) { this@dp.toDp() }


@Composable
fun TextUnit.clamp(min: Dp? = null, max: Dp? = null) = with (LocalDensity.current) {
    var dp = dp
    if (min != null) {
        dp = dp.coerceAtLeast(min)
    }
    if (max != null) {
        dp = dp.coerceAtMost(max)
    }
    return@with dp.sp
}


fun clampSp(context: Context, sp: Float, dpMin: Float? = null, dpMax: Float? = null): Float {
    var dp = UnitUtils.spToDp(context, sp)
    if (dpMin != null) {
        dp = dp.coerceAtLeast(dpMin)
    }
    if (dpMax != null) {
        dp = dp.coerceAtMost(dpMax)
    }
    return UnitUtils.dpToSp(context, dp)
}
