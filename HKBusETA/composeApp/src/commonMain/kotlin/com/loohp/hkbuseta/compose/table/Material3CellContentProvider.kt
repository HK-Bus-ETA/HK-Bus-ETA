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