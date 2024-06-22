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

package com.loohp.hkbuseta.compose.table

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.loohp.hkbuseta.compose.ArrowDownward
import com.loohp.hkbuseta.compose.ArrowUpward
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons

object Material3CellContentProvider : CellContentProvider {
    @Composable
    override fun RowCellContent(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
            content()
        }
    }

    @Composable
    override fun HeaderCellContent(
        sorted: Boolean,
        sortAscending: Boolean,
        onClick: (() -> Unit)?,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleSmall) {
            if (onClick != null) {
                if (sorted) {
                    IconButton(
                        onClick = onClick
                    ) {
                        if (sortAscending) {
                            PlatformIcon(PlatformIcons.Default.ArrowUpward, contentDescription = null)
                        } else {
                            PlatformIcon(PlatformIcons.Default.ArrowDownward, contentDescription = null)
                        }
                    }
                }
                TextButton(onClick = onClick) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}