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
