package com.loohp.hkbuseta.compose.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loohp.hkbuseta.compose.ChevronLeft
import com.loohp.hkbuseta.compose.ChevronRight
import com.loohp.hkbuseta.compose.FirstPage
import com.loohp.hkbuseta.compose.LastPage
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.table.paging.BasicPaginatedDataTable
import com.loohp.hkbuseta.compose.table.paging.PaginatedDataTableState
import com.loohp.hkbuseta.compose.table.paging.rememberPaginatedDataTableState
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.min

@Composable
fun PaginatedDataTable(
    columns: ImmutableList<DataColumn>,
    modifier: Modifier = Modifier,
    separator: @Composable (rowIndex: Int) -> Unit = { Divider() },
    headerHeight: Dp = 56.dp,
    rowHeight: Dp = 52.dp,
    horizontalPadding: Dp = 16.dp,
    state: PaginatedDataTableState = rememberPaginatedDataTableState(10),
    sortColumnIndex: Int? = null,
    sortAscending: Boolean = true,
    content: DataTableScope.() -> Unit,
) {
    BasicPaginatedDataTable(
        columns = columns,
        modifier = modifier,
        separator = separator,
        headerHeight = headerHeight,
        horizontalPadding = horizontalPadding,
        state = state,
        footer = {
            Row(
                modifier = Modifier.height(rowHeight).padding(horizontal = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val start = min(state.pageIndex * state.pageSize + 1, state.count)
                val end = min(start + state.pageSize - 1, state.count)
                val pageCount = (state.count + state.pageSize - 1) / state.pageSize
                PlatformText("$start-$end of ${state.count}")
                IconButton(
                    onClick = { state.pageIndex = 0 },
                    enabled = state.pageIndex > 0,
                ) {
                    PlatformIcon(PlatformIcons.Default.FirstPage, "First")
                }
                IconButton(
                    onClick = { state.pageIndex-- },
                    enabled = state.pageIndex > 0,
                ) {
                    PlatformIcon(PlatformIcons.Default.ChevronLeft, "Previous")
                }
                IconButton(
                    onClick = { state.pageIndex++ },
                    enabled = state.pageIndex < pageCount - 1
                ) {
                    PlatformIcon(PlatformIcons.Default.ChevronRight, "Next")
                }
                IconButton(
                    onClick = { state.pageIndex = pageCount - 1 },
                    enabled = state.pageIndex < pageCount - 1
                ) {
                    PlatformIcon(PlatformIcons.Default.LastPage, "Last")
                }
            }
        },
        cellContentProvider = Material3CellContentProvider,
        sortColumnIndex = sortColumnIndex,
        sortAscending = sortAscending,
        content = content
    )
}