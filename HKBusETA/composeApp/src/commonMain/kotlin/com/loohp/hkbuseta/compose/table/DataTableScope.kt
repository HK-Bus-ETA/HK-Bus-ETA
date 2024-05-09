/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.loohp.hkbuseta.compose.table

import androidx.compose.foundation.layout.LayoutScopeMarker
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
    fun row(onClick: (() -> Unit)? = null, content: TableRowScope.() -> Unit)

    /**
     * Creates a new rows in the [BasicDataTable] with the specified content.
     */
    fun rows(count: Int, content: TableRowScope.(Int) -> Unit)
}

internal class DataTableScopeImpl : DataTableScope {
    val tableRows = mutableListOf<TableRowData>()

    override fun row(onClick: (() -> Unit)?, content: TableRowScope.() -> Unit) {
        tableRows += TableRowData(onClick, content)
    }

    override fun rows(count: Int, content: TableRowScope.(Int) -> Unit) {
        TODO("Not yet implemented")
    }
}
