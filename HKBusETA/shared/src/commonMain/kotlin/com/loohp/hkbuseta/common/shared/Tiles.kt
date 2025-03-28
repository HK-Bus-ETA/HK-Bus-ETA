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

package com.loohp.hkbuseta.common.shared

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.getFavouriteRouteStop
import com.loohp.hkbuseta.common.objects.resolveStop


enum class TileUseState {

    PRIMARY, SECONDARY, NONE

}

object Tiles {

    private val etaTileConfigurations: Map<Int, List<Int>> = ConcurrentMutableMap()
    private val lock: Lock = Lock()

    private var platformUpdate: () -> Unit = { /* do nothing */ }

    fun providePlatformUpdate(runnable: () -> Unit) {
        platformUpdate = runnable
    }

    fun updateEtaTileConfigurations(mutation: (MutableMap<Int, List<Int>>) -> Unit) {
        lock.withLock {
            mutation.invoke(etaTileConfigurations as MutableMap)
        }
    }

    fun getSortedEtaTileConfigurationsIds(context: AppContext, originGetter: () -> Coordinates?): List<Int> {
        return etaTileConfigurations.keys.toMutableList().apply {
            val origin = originGetter.invoke()?: return@apply
            val distances = associateWith {
                id -> getEtaTileConfiguration(id).minBy {
                    Shared.favoriteRouteStops.value.getFavouriteRouteStop(it)?.resolveStop(context) { origin }?.stop?.location?.distance(origin)?: Double.MAX_VALUE
                }
            }
            sortBy { distances[it] }
        }
    }

    fun getEtaTileConfiguration(tileId: Int): List<Int> {
        return etaTileConfigurations.getOrElse(tileId) { emptyList() }
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
        platformUpdate.invoke()
    }

}