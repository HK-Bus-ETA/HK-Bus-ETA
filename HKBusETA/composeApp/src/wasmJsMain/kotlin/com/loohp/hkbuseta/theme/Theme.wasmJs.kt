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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource


@OptIn(ExperimentalResourceApi::class)
val typography: Typography @Composable get() {
    val font = FontFamily(
        Font(
            resource = FontResource("fonts/NotoSansHK-Black.ttf"),
            weight = FontWeight.Black
        ),
        Font(
            resource = FontResource("fonts/NotoSansHK-Bold.ttf"),
            weight = FontWeight.Bold
        ),
        Font(
            resource = FontResource("fonts/NotoSansHK-ExtraBold.ttf"),
            weight = FontWeight.ExtraBold
        ),
        Font(
            resource = FontResource("fonts/NotoSansHK-ExtraLight.ttf"),
            weight = FontWeight.ExtraLight
        ),
        Font(
            resource = FontResource("fonts/NotoSansHK-Light.ttf"),
            weight = FontWeight.Light
        ),
        Font(
            resource = FontResource("fonts/NotoSansHK-Medium.ttf"),
            weight = FontWeight.Medium
        ),
        Font(
            resource = FontResource("fonts/NotoSansHK-Regular.ttf"),
            weight = FontWeight.Normal
        ),
        Font(
            resource = FontResource("fonts/NotoSansHK-SemiBold.ttf"),
            weight = FontWeight.SemiBold
        ),
        Font(
            resource = FontResource("fonts/NotoSansHK-Thin.ttf"),
            weight = FontWeight.Thin
        )
    )
    return MaterialTheme.typography.run {
        Typography(
            displayLarge = displayLarge.copy(fontFamily = font),
            displayMedium = displayMedium.copy(fontFamily = font),
            displaySmall = displaySmall.copy(fontFamily = font),

            headlineLarge = headlineLarge.copy(fontFamily = font),
            headlineMedium = headlineMedium.copy(fontFamily = font),
            headlineSmall = headlineSmall.copy(fontFamily = font),

            titleLarge = titleLarge.copy(fontFamily = font),
            titleMedium = titleMedium.copy(fontFamily = font),
            titleSmall = titleSmall.copy(fontFamily = font),

            bodyLarge = bodyLarge.copy(fontFamily = font),
            bodyMedium = bodyMedium.copy(fontFamily = font),
            bodySmall = bodySmall.copy(fontFamily = font),

            labelLarge = labelLarge.copy(fontFamily = font),
            labelMedium = labelMedium.copy(fontFamily = font),
            labelSmall = labelSmall.copy(fontFamily = font)
        )
    }
}

@Composable
actual fun AppTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (!useDarkTheme) LightColors else DarkColors,
        typography = typography,
        content = content
    )
}