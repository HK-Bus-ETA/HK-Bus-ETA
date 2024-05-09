package com.loohp.hkbuseta.compose.table

internal data class TableRowData(
    val onClick: (() -> Unit)?,
    val content: TableRowScope.() -> Unit,
)
