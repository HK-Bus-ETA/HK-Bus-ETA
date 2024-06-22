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

package com.loohp.hkbuseta.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamicColorScheme

@Composable
fun resolveColorScheme(useDarkTheme: Boolean, customColor: Color?): ColorScheme {
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        dynamicColor && useDarkTheme && customColor == null -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !useDarkTheme && customColor == null -> dynamicLightColorScheme(LocalContext.current)
        customColor != null -> dynamicColorScheme(customColor, useDarkTheme)
        useDarkTheme -> DarkColors
        else -> LightColors
    }
}

@Composable
actual fun AppTheme(
    useDarkTheme: Boolean,
    customColor: Color?,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = resolveColorScheme(useDarkTheme, customColor),
        typography = MaterialTheme.typography,
        content = content
    )
}