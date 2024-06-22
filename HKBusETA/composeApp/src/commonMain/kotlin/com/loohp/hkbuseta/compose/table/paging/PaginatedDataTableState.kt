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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class PaginatedDataTableState(
    pageSize: Int,
    pageIndex: Int,
) {
    var pageSize by mutableStateOf(pageSize)
    var pageIndex by mutableStateOf(pageIndex)
    var count by mutableStateOf(0)

    companion object {
        val Saver: Saver<PaginatedDataTableState, *> = listSaver(
            save = { listOf(it.pageSize, it.pageIndex) },
            restore = {
                PaginatedDataTableState(it[0], it[1])
            }
        )
    }
}

@Composable
fun rememberPaginatedDataTableState(
    initialPageSize: Int,
    initialPageIndex: Int = 0,
): PaginatedDataTableState {
    return rememberSaveable(saver = PaginatedDataTableState.Saver) {
        PaginatedDataTableState(initialPageSize, initialPageIndex)
    }
}
