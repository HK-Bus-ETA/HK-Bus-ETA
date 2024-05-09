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

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import com.loohp.hkbuseta.common.appcontext.AppContext
import kotlin.math.absoluteValue


inline val Dp.sp: TextUnit @Composable get() = with (LocalDensity.current) { this@sp.toSp() }

inline val TextUnit.dp: Dp @Composable get() = with (LocalDensity.current) { this@dp.toDp() }

inline val TextUnit.px: Float @Composable get() = with (LocalDensity.current) { this@px.toPx() }

inline val Float.equivalentDp: Dp @Composable get() = with (LocalDensity.current) { this@equivalentDp.toDp() }

inline val Int.equivalentDp: Dp @Composable get() = with (LocalDensity.current) { this@equivalentDp.toDp() }

inline val Float.fontScaledDp: Dp @Composable get() = (this * LocalDensity.current.fontScale).dp

inline val Int.fontScaledDp: Dp @Composable get() = (this * LocalDensity.current.fontScale).dp

@Composable
inline fun Float.fontScaledDp(factor: Float): Dp = (this * ((LocalDensity.current.fontScale - 1F) * factor + 1F)).dp

@Composable
inline fun Int.fontScaledDp(factor: Float): Dp = toFloat().fontScaledDp(factor)

fun Float.sameValueAs(other: Float) : Boolean {
    return (this - other).absoluteValue < 0.00001
}

fun Double.sameValueAs(other: Double) : Boolean {
    return (this - other).absoluteValue < 0.00001
}

@Composable
fun TextUnit.clamp(min: Dp? = null, max: Dp? = null): TextUnit {
    var dp = this.dp
    if (min != null) {
        dp = dp.coerceAtLeast(min)
    }
    if (max != null) {
        dp = dp.coerceAtMost(max)
    }
    return dp.sp
}

fun Float.clampSp(context: AppContext, dpMin: Float? = null, dpMax: Float? = null): Float {
    var dp = spToDp(context)
    if (dpMin != null) {
        dp = dp.coerceAtLeast(dpMin)
    }
    if (dpMax != null) {
        dp = dp.coerceAtMost(dpMax)
    }
    return dp.dpToSp(context)
}

fun Float.spToPixels(context: AppContext): Float {
    return this * context.scaledDensity
}

fun Float.dpToPixels(context: AppContext): Float {
    return this * context.density
}

fun Float.pixelsToDp(context: AppContext): Float {
    return this / context.density
}

fun Float.dpToSp(context: AppContext): Float {
    return dpToPixels(context) / context.scaledDensity
}

fun Float.spToDp(context: AppContext): Float {
    return (this * context.scaledDensity).pixelsToDp(context)
}
