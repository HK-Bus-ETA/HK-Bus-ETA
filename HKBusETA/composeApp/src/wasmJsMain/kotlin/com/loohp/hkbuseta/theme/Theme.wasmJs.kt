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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.loohp.hkbuseta.utils.FontResource
import com.materialkolor.dynamicColorScheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.rememberResourceEnvironment


@OptIn(ExperimentalResourceApi::class)
suspend fun loadFontFamily(environment: ResourceEnvironment): FontFamily {
    return FontFamily(
        Font(
            identity = "NotoSansHK-Black",
            data = FontResource("fonts/NotoSansHK-Black.ttf").readBytes(environment),
            weight = FontWeight.Black
        ),
        Font(
            identity = "NotoSansHK-Bold",
            data = FontResource("fonts/NotoSansHK-Bold.ttf").readBytes(environment),
            weight = FontWeight.Bold
        ),
        Font(
            identity = "NotoSansHK-ExtraBold",
            data = FontResource("fonts/NotoSansHK-ExtraBold.ttf").readBytes(environment),
            weight = FontWeight.ExtraBold
        ),
        Font(
            identity = "NotoSansHK-ExtraLight",
            data = FontResource("fonts/NotoSansHK-ExtraLight.ttf").readBytes(environment),
            weight = FontWeight.ExtraLight
        ),
        Font(
            identity = "NotoSansHK-Light",
            data = FontResource("fonts/NotoSansHK-Light.ttf").readBytes(environment),
            weight = FontWeight.Light
        ),
        Font(
            identity = "NotoSansHK-Medium",
            data = FontResource("fonts/NotoSansHK-Medium.ttf").readBytes(environment),
            weight = FontWeight.Medium
        ),
        Font(
            identity = "NotoSansHK-Regular",
            data = FontResource("fonts/NotoSansHK-Regular.ttf").readBytes(environment),
            weight = FontWeight.Normal
        ),
        Font(
            identity = "NotoSansHK-SemiBold",
            data = FontResource("fonts/NotoSansHK-SemiBold.ttf").readBytes(environment),
            weight = FontWeight.SemiBold
        ),
        Font(
            identity = "NotoSansHK-Thin",
            data = FontResource("fonts/NotoSansHK-Thin.ttf").readBytes(environment),
            weight = FontWeight.Thin
        )
    )
}

fun Typography.applyFontFamily(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),

        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),

        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),

        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),

        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily)
    )
}

@OptIn(ExperimentalResourceApi::class)
suspend inline fun FontResource.readBytes(environment: ResourceEnvironment): ByteArray {
    return getFontResourceBytes(environment, this)
}

@Composable
fun resolveColorScheme(useDarkTheme: Boolean, customColor: Color?): ColorScheme {
    return when {
        customColor != null -> dynamicColorScheme(customColor, useDarkTheme)
        useDarkTheme -> DarkColors
        else -> LightColors
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun AppTheme(
    useDarkTheme: Boolean,
    customColor: Color?,
    content: @Composable () -> Unit
) {
    val environment = rememberResourceEnvironment()
    val platformTypography = MaterialTheme.typography
    var appliedTypography: Typography? by remember { mutableStateOf(null) }

    LaunchedEffect (Unit) {
        val fontFamily = loadFontFamily(environment)
        appliedTypography = platformTypography.applyFontFamily(fontFamily)
    }

    appliedTypography?.let {
        MaterialTheme(
            colorScheme = resolveColorScheme(useDarkTheme, customColor),
            typography = it,
            content = content
        )
    }
}