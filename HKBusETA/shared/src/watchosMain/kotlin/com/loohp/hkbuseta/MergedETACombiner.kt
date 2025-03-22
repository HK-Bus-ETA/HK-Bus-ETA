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

import co.touchlab.stately.concurrency.Lock
import com.loohp.hkbuseta.common.objects.FavouriteResolvedStop
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.utils.MutableNonNullStateFlow
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.wrap
import com.loohp.hkbuseta.utils.withLock
import kotlinx.coroutines.flow.MutableStateFlow

class MergedETACombiner(
    private var size: Int
) {

    private val lock: Lock = Lock()
    private val results: MutableList<Pair<Pair<FavouriteResolvedStop, FavouriteRouteStop>, Registry.ETAQueryResult>> = mutableListOf()

    val mergedState: MutableNonNullStateFlow<MergedETAContainer> = MutableStateFlow(MergedETAContainer.EMPTY).wrap()

    fun reset(size: Int = this.size) {
        lock.withLock {
            this.size = size
            this.results.clear()
        }
    }

    fun addResult(result: Pair<Pair<FavouriteResolvedStop, FavouriteRouteStop>, Registry.ETAQueryResult>) {
        lock.withLock {
            if (results.size < size) {
                results.add(result)
                if (results.size >= size) {
                    mergedState.value = MergedETAContainer(Registry.MergedETAQueryResult.merge(results))
                }
            }
        }
    }

}

data class MergedETAContainer(val eta: Registry.MergedETAQueryResult<Pair<FavouriteResolvedStop, FavouriteRouteStop>>?) {

    companion object {

        val EMPTY: MergedETAContainer get() = MergedETAContainer(null)

    }

    private val generated: Long = currentTimeMillis()

    fun isEmpty(): Boolean {
        return eta == null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MergedETAContainer) return false

        if (eta != other.eta) return false
        return generated == other.generated
    }

    override fun hashCode(): Int {
        var result = eta?.hashCode() ?: 0
        result = 31 * result + generated.hashCode()
        return result
    }

}