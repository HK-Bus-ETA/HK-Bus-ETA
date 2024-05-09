package com.loohp.hkbuseta.compose.table

import androidx.compose.runtime.Composable

internal data class TableCellData(
    val content: @Composable TableCellScope.() -> Unit,
)