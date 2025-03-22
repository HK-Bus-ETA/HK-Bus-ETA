/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.utils.MutableNullableStateFlow
import com.loohp.hkbuseta.common.utils.wrapNullable
import kotlinx.coroutines.flow.MutableStateFlow

class ETAResultsContainer<K> {

    private val data: MutableMap<K, Registry.ETAQueryResult> = ConcurrentMutableMap()
    private val states: MutableMap<K, ETAResultsState> = ConcurrentMutableMap()

    fun get(key: K): Registry.ETAQueryResult? {
        return data[key]
    }

    fun set(key: K, result: Registry.ETAQueryResult?) {
        result?.let { r ->
            data[key] = r
            states[key]?.let { it.state.valueNullable = r }
        }?: run {
            data.remove(key)
            states[key]?.let { it.state.valueNullable = null }
        }
    }

    fun getState(key: K): ETAResultsState {
        return states.getOrPut(key) { ETAResultsState(MutableStateFlow(get(key)).wrapNullable()) }
    }

}

class ETAResultsState(
    val state: MutableNullableStateFlow<Registry.ETAQueryResult>
)