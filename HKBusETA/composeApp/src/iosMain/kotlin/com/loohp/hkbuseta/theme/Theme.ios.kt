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
import androidx.compose.runtime.Composable
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.darkColorScheme
import io.github.alexzhirkevich.cupertino.theme.lightColorScheme

@Composable
actual fun AppTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (!useDarkTheme) LightColors else DarkColors,
        typography = MaterialTheme.typography,
    ) {
        CupertinoTheme(
            colorScheme = if (!useDarkTheme) lightColorScheme() else darkColorScheme(),
            content = content
        )
    }
}