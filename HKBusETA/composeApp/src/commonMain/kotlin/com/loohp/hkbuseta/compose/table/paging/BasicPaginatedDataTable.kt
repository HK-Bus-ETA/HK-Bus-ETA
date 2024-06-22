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

package com.loohp.hkbuseta.compose.table.paging

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loohp.hkbuseta.compose.table.BasicDataTable
import com.loohp.hkbuseta.compose.table.CellContentProvider
import com.loohp.hkbuseta.compose.table.DataColumn
import com.loohp.hkbuseta.compose.table.DataTableScope
import com.loohp.hkbuseta.compose.table.DefaultCellContentProvider
import kotlinx.collections.immutable.ImmutableList

@Composable
fun BasicPaginatedDataTable(
    columns: ImmutableList<DataColumn>,
    modifier: Modifier = Modifier,
    separator: @Composable (rowIndex: Int) -> Unit = { },
    headerHeight: Dp = 56.dp,
    rowHeight: Dp = 52.dp,
    horizontalPadding: Dp = 16.dp,
    state: PaginatedDataTableState = rememberPaginatedDataTableState(10),
    footer: @Composable () -> Unit = { },
    cellContentProvider: CellContentProvider = DefaultCellContentProvider,
    sortColumnIndex: Int? = null,
    sortAscending: Boolean = true,
    content: DataTableScope.() -> Unit
) {
    BasicDataTable(
        columns = columns,
        modifier = modifier,
        separator = separator,
        headerHeight = headerHeight,
        rowHeight = rowHeight,
        horizontalPadding = horizontalPadding,
        footer = footer,
        cellContentProvider = cellContentProvider,
        sortColumnIndex = sortColumnIndex,
        sortAscending = sortAscending,
    ) {
        val start = state.pageIndex * state.pageSize
        val scope = PaginatedRowScope(start, start + state.pageSize, this)
        with(scope) {
            content()
        }
        if (state.count != scope.index) {
            state.count = scope.index
        }
    }
}
