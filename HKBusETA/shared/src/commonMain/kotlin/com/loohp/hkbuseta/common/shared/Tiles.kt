/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

package com.loohp.hkbuseta.common.shared

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.value
import co.touchlab.stately.concurrency.withLock


enum class TileUseState {

    PRIMARY, SECONDARY, NONE

}

object Tiles {

    private val etaTileConfigurations: Map<Int, List<Int>> = ConcurrentMutableMap()
    private val lock: Lock = Lock()

    private val platformUpdate: AtomicReference<(Int) -> Unit> = AtomicReference { }

    fun providePlatformUpdate(runnable: (Int) -> Unit) {
        platformUpdate.value = runnable
    }

    fun updateEtaTileConfigurations(mutation: (MutableMap<Int, List<Int>>) -> Unit) {
        lock.withLock {
            mutation.invoke(etaTileConfigurations as MutableMap<Int, List<Int>>)
        }
    }

    fun getEtaTileConfiguration(tileId: Int): List<Int> {
        return if (tileId in (1 or Int.MIN_VALUE)..(8 or Int.MIN_VALUE)) listOf(tileId and Int.MAX_VALUE) else etaTileConfigurations.getOrElse(tileId) { emptyList() }
    }

    fun getRawEtaTileConfigurations(): Map<Int, List<Int>> {
        return etaTileConfigurations
    }

    fun getTileUseState(index: Int): TileUseState {
        return when (etaTileConfigurations.values.minOfOrNull { it.indexOf(index).let { i -> if (i >= 0) i else Int.MAX_VALUE } }?: Int.MAX_VALUE) {
            0 -> TileUseState.PRIMARY
            Int.MAX_VALUE -> TileUseState.NONE
            else -> TileUseState.SECONDARY
        }
    }

    fun requestTileUpdate() {
        (0..8).forEach { requestTileUpdate(it) }
    }

    fun requestTileUpdate(favoriteIndex: Int) {
        platformUpdate.value.invoke(favoriteIndex)
    }

}