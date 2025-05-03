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

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loohp.hkbuseta.appcontext.applicationAppContext
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.shared.Registry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

actual fun exitApp() {
    exitProcess(0)
}

actual fun watchDataOverwriteWarningInitialValue(): Boolean = runBlocking(Dispatchers.IO) { Registry.isNewInstall(applicationAppContext) }

@Composable
actual fun SnackbarInterface(instance: AppActiveContext, snackbarHostState: SnackbarHostState) = SnackbarHost(snackbarHostState)

@Composable
actual fun Modifier.platformSafeAreaSidesPaddingAndConsumeWindowInsets(): Modifier = this