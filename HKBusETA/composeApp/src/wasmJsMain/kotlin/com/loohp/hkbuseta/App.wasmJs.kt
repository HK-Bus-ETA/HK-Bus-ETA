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

package com.loohp.hkbuseta

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.compose.calculateWindowSizeClass
import com.loohp.hkbuseta.compose.currentLocalWindowSize
import com.loohp.hkbuseta.compose.isNarrow
import com.loohp.hkbuseta.utils.pixelsToDp
import kotlin.math.min

actual fun exitApp() {
}

actual fun watchDataOverwriteWarningInitialValue(): Boolean = false

@Composable
actual fun SnackbarInterface(instance: AppActiveContext, snackbarHostState: SnackbarHostState) {
    val window = currentLocalWindowSize
    val windowSizeClass = calculateWindowSizeClass()

    when {
        windowSizeClass.isNarrow -> SnackbarHost(snackbarHostState)
        else -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            SnackbarHost(
                modifier = Modifier.width(min(412F, (window.width / 2F).pixelsToDp(instance)).dp),
                hostState = snackbarHostState
            )
        }
    }
}

@Composable
actual fun Modifier.consumePlatformWindowInsets(): Modifier = this