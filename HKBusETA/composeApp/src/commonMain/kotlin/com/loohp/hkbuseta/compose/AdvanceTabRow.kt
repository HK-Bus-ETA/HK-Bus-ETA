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

package com.loohp.hkbuseta.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun AdvanceTabRow(
    selectedTabIndex: Int,
    totalTabSize: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 52.dp,
    scrollable: Boolean,
    widestTabWidth: Dp,
    tabs: @Composable (Boolean) -> Unit
) {
    if (scrollable) {
        PlatformScrollableTabRow(selectedTabIndex, modifier, edgePadding, totalTabSize, widestTabWidth) { tabs.invoke(true) }
    } else {
        PlatformTabRow(selectedTabIndex, modifier) { tabs.invoke(false) }
    }
}