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

package com.loohp.hkbuseta

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.loohp.hkbuseta.appcontext.applicationAppContext
import com.loohp.hkbuseta.appcontext.componentActivity
import com.loohp.hkbuseta.appcontext.navBarColor
import com.loohp.hkbuseta.appcontext.statusBarColor
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.compose.currentLocalWindowSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

actual fun exitApp() {
    componentActivity.finishAffinity()
}

actual fun watchDataOverwriteWarningInitialValue(): Boolean = runBlocking(Dispatchers.IO) { Registry.isNewInstall(applicationAppContext) }

@Composable
actual fun SnackbarInterface(instance: AppActiveContext, snackbarHostState: SnackbarHostState) = SnackbarHost(snackbarHostState)

@Composable
actual fun Modifier.consumePlatformWindowInsets(): Modifier = this