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

package com.loohp.hkbuseta.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Mode
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.utils.FontResource
import com.materialkolor.dynamicColorScheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.rememberResourceEnvironment


data class WebFontEntry(
    val identity: String,
    val weight: FontWeight
)

private val tcFonts = listOf(
    WebFontEntry("NotoSansHK-Regular", FontWeight.Normal),
    WebFontEntry("NotoSansHK-Bold", FontWeight.Bold),
    WebFontEntry("NotoSansHK-ExtraBold", FontWeight.ExtraBold),
    WebFontEntry("NotoSansHK-ExtraLight", FontWeight.ExtraLight),
    WebFontEntry("NotoSansHK-Light", FontWeight.Light),
    WebFontEntry("NotoSansHK-Medium", FontWeight.Medium),
    WebFontEntry("NotoSansHK-SemiBold", FontWeight.SemiBold),
    WebFontEntry("NotoSansHK-Thin", FontWeight.Thin),
)

private val scFonts = listOf(
    WebFontEntry("NotoSansSC-Regular", FontWeight.Normal),
    WebFontEntry("NotoSansSC-Bold", FontWeight.Bold),
    WebFontEntry("NotoSansSC-ExtraBold", FontWeight.ExtraBold),
    WebFontEntry("NotoSansSC-ExtraLight", FontWeight.ExtraLight),
    WebFontEntry("NotoSansSC-Light", FontWeight.Light),
    WebFontEntry("NotoSansSC-Medium", FontWeight.Medium),
    WebFontEntry("NotoSansSC-SemiBold", FontWeight.SemiBold),
    WebFontEntry("NotoSansSC-Thin", FontWeight.Thin),
)

private val emojiFont = listOf(
    WebFontEntry("NotoColorEmoji-Regular", FontWeight.Normal)
)

@Composable
private fun rememberLoadFontFamilyEmitGradually(environment: ResourceEnvironment, fontEntries: Collection<WebFontEntry>): State<FontFamily?> {
    var fonts: ImmutableList<Font> by remember { mutableStateOf(persistentListOf()) }
    val fontFamilyState: MutableState<FontFamily?> = remember { mutableStateOf(null) }
    var fontFamily by fontFamilyState

    LaunchedEffect (Unit) {
        for ((identity, weight) in fontEntries) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val font = Font(
                        identity = identity,
                        data = FontResource("fonts/$identity.ttf").readBytes(environment),
                        weight = weight
                    )
                    val newFonts = (fonts + font).asImmutableList()
                    val newFontFamily = FontFamily(newFonts)
                    withContext(Dispatchers.Main) {
                        fonts = newFonts
                        fontFamily = newFontFamily
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    return fontFamilyState
}

@Composable
private fun rememberLoadFontFamilyEmitOnComplete(environment: ResourceEnvironment, fontEntries: Collection<WebFontEntry>): State<FontFamily?> {
    val fontFamilyState: MutableState<FontFamily?> = remember { mutableStateOf(null) }
    var fontFamily by fontFamilyState

    LaunchedEffect (Unit) {
        val fonts = buildList {
            for ((identity, weight) in fontEntries) {
                add(CoroutineScope(Dispatchers.IO).async {
                    try {
                        Font(
                            identity = identity,
                            data = FontResource("fonts/$identity.ttf").readBytes(environment),
                            weight = weight
                        )
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        null
                    }
                })
            }
        }.awaitAll()
        fontFamily = FontFamily(fonts.filterNotNull())
    }

    return fontFamilyState
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

suspend inline fun FontResource.readBytes(environment: ResourceEnvironment): ByteArray {
    return getFontResourceBytes(environment, this)
}

@Composable
fun resolveColorScheme(useDarkTheme: Boolean, customColor: Color?): ColorScheme {
    return when {
        customColor != null -> dynamicColorScheme(customColor, useDarkTheme, true)
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
    val environment = rememberResourceEnvironment()
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val platformTypography = MaterialTheme.typography

    val loadedTcFont by rememberLoadFontFamilyEmitGradually(environment, tcFonts)
    val loadedEmojiFont by rememberLoadFontFamilyEmitOnComplete(environment, emojiFont)
    val loadedScFont by rememberLoadFontFamilyEmitOnComplete(environment, scFonts)

    var appliedTypography: Typography? by remember { mutableStateOf(null) }

    LaunchedEffect (loadedTcFont) {
        loadedTcFont?.apply { appliedTypography = platformTypography.applyFontFamily(this) }
    }
    LaunchedEffect (loadedEmojiFont) {
        loadedEmojiFont?.apply { fontFamilyResolver.preload(this) }
    }
    LaunchedEffect (loadedScFont) {
        loadedScFont?.apply { fontFamilyResolver.preload(this) }
    }

    appliedTypography?.let {
        MaterialTheme(
            colorScheme = resolveColorScheme(useDarkTheme, customColor),
            typography = it,
            content = {
                ProvideTextStyle(
                    value = TextStyle(lineHeightStyle = LineHeightStyle(alignment = Alignment.Proportional, trim = Trim.None, mode = Mode.Fixed)),
                    content = content
                )
            }
        )
    }
}