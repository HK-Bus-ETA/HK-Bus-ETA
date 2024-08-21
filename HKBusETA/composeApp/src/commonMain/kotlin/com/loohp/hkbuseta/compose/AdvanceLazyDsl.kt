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

package com.loohp.hkbuseta.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable


@OptIn(ExperimentalFoundationApi::class)
inline fun <T> LazyListScope.itemsIndexedPossiblySticky(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline sticky: (index: Int, item: T) -> Boolean = { _, _ -> false },
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T, sticky: Boolean) -> Unit
) {
    items.forEachIndexed { index, item ->
        if (sticky(index, item)) {
            stickyHeader(
                key = if (key != null) { key(index, items[index]) } else null,
                contentType = { contentType(index, items[index]) }
            ) {
                itemContent(index, items[index], true)
            }
        } else {
            item(
                key = if (key != null) { key(index, items[index]) } else null,
                contentType = { contentType(index, items[index]) }
            ) {
                itemContent(index, items[index], false)
            }
        }
    }
}