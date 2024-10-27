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

package com.loohp.hkbuseta.glance

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.theme.DarkColors
import com.loohp.hkbuseta.theme.LightColors
import com.loohp.hkbuseta.utils.adjustAlpha
import com.materialkolor.dynamicColorScheme


fun resolveWidgetColorScheme(context: Context): ColorProviders {
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val lightColors = Shared.color
        ?.let { dynamicColorScheme(Color(it), isDark = false, isAmoled = true) }
        ?: if (dynamicColor) dynamicLightColorScheme(context) else LightColors
    val darkColors = Shared.color
        ?.let { dynamicColorScheme(Color(it), isDark = true, isAmoled = true) }
        ?: if (dynamicColor) dynamicDarkColorScheme(context) else DarkColors
    return ColorProviders(
        light = lightColors.forGlance(),
        dark = darkColors.forGlance(),
    )
}

@Suppress("NOTHING_TO_INLINE")
inline fun ColorScheme.forGlance(): ColorScheme {
    return copy(background = background.adjustAlpha(0.925F))
}

@Suppress("NOTHING_TO_INLINE")
inline fun Color.forGlance(): ColorProvider {
    return ColorProvider(this)
}

@Suppress("NOTHING_TO_INLINE")
inline fun ColorProviders.isDark(context: Context): Boolean {
    return background.getColor(context).luminance() < 0.5F
}

@Composable
fun GlanceWidgetTheme(context: Context, content: @Composable () -> Unit) {
    GlanceTheme(colors = resolveWidgetColorScheme(context)) {
        content.invoke()
    }
}