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

package com.loohp.hkbuseta.compose.table.paging

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.loohp.hkbuseta.compose.table.DataTableScope
import com.loohp.hkbuseta.compose.table.TableRowScope

internal class PaginatedRowScope(
    private val from: Int,
    private val to: Int,
    private val parentScope: DataTableScope,
) : DataTableScope {
    var index: Int = 0

    override fun row(onClick: (() -> Unit)?, background: @Composable (BoxScope.() -> Unit)?, content: TableRowScope.() -> Unit) {
        if (index in from until to) {
            parentScope.row(onClick, background, content)
        }
        index++
    }

    override fun rows(count: Int, background: @Composable (BoxScope.(Int) -> Unit)?, content: TableRowScope.(Int) -> Unit) {
        for (i in 0 until count) {
            row(
                onClick = null,
                background = background?.let { { it.invoke(this, i) } },
                content = { content.invoke(this, i) }
            )
        }
    }
}