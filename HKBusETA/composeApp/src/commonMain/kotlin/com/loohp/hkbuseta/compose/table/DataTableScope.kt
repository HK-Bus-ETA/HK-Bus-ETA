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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Collects information about the children of a [BasicDataTable] when
 * its body is executed with a [DataTableScope] as argument.
 */
@LayoutScopeMarker
@Immutable
interface DataTableScope {
    /**
     * Creates a new row in the [BasicDataTable] with the specified content.
     */
    fun row(onClick: (() -> Unit)? = null, background: @Composable (BoxScope.() -> Unit)? = null, content: TableRowScope.() -> Unit)

    /**
     * Creates a new rows in the [BasicDataTable] with the specified content.
     */
    fun rows(count: Int, background: @Composable (BoxScope.(Int) -> Unit)? = null, content: TableRowScope.(Int) -> Unit)
}

internal class DataTableScopeImpl : DataTableScope {
    val tableRows = mutableListOf<TableRowData>()

    override fun row(onClick: (() -> Unit)?, background: @Composable (BoxScope.() -> Unit)?, content: TableRowScope.() -> Unit) {
        tableRows += TableRowData(onClick, background, content)
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
