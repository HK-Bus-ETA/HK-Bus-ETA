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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.loohp.hkbuseta.appcontext.setVersionImpl
import com.loohp.hkbuseta.appcontext.setWidthHeightDensityImpl
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.compose.ImmediateEffect
import com.loohp.hkbuseta.shared.DesktopShared
import java.awt.Dimension


@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    DesktopShared.setSystemFormatProviders()
    DesktopShared.setDefaultExceptionHandler()
    DesktopShared.startupJavaFX()
    setVersionImpl { Triple("HKBusETA", "2.4.17", 38) }
    Window(
        onCloseRequest = ::exitApplication,
        title = if (Shared.language == "en") "HK Bus ETA" else "香港巴士到站預報",
        icon = @Suppress("DEPRECATION") painterResource("icon_full_smaller.png"),
        state = rememberWindowState(size = DpSize(1200.dp, 800.dp)),
    ) {
        val composeWindow = LocalWindowInfo.current.containerSize
        val density = LocalDensity.current.density
        ImmediateEffect (Unit) {
            window.minimumSize = Dimension(337, 600)
        }
        ImmediateEffect (composeWindow.width, composeWindow.height, density) {
            val impl = Triple(composeWindow.width, composeWindow.height, density)
            setWidthHeightDensityImpl { impl }
        }
        App()
    }
}
