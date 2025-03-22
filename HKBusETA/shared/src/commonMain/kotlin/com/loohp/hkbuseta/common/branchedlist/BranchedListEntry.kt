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
package com.loohp.hkbuseta.common.branchedlist

import com.loohp.hkbuseta.common.utils.Immutable
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet


@Immutable
class BranchedListEntry<K, V, B> private constructor(
    val key: K,
    val value: V,
    val branchIds: Set<B>
) {
    constructor(key: K, value: V, vararg branchIds: B) : this(key, value, persistentSetOf(*branchIds))
    constructor(key: K, value: V, branchIds: Collection<B>) : this(key, value, branchIds.toImmutableSet())

    operator fun component1(): K {
        return key
    }

    operator fun component2(): V {
        return value
    }

    operator fun component3(): Set<B> {
        return branchIds
    }

    fun merge(value: V, vararg branchIds: B): BranchedListEntry<K, V, B> {
        return BranchedListEntry(key, value, this.branchIds + branchIds)
    }
}