package com.loohp.hkbuseta.compose.table

import androidx.compose.runtime.Composable

interface CellContentProvider {
    @Composable
    fun RowCellContent(content: @Composable () -> Unit)

    @Composable
    fun HeaderCellContent(sorted: Boolean, sortAscending: Boolean, onClick: (() -> Unit)?, content: @Composable () -> Unit)
}

object DefaultCellContentProvider : CellContentProvider {

    @Composable
    override fun RowCellContent(content: @Composable () -> Unit) {
        content()
    }

    @Composable
    override fun HeaderCellContent(sorted: Boolean, sortAscending: Boolean, onClick: (() -> Unit)?, content: @Composable () -> Unit) {
        content()
    }
}