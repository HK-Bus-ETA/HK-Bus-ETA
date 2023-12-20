/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

import kotlinx.collections.immutable.toImmutableSet

class BranchedListEntry<K, V>(val key: K, val value: V, branchIds: Collection<Int>) {

    constructor(key: K, value: V, vararg branchIds: Int) : this(key, value, branchIds.toList())

    val branchIds: Set<Int> = branchIds.toImmutableSet()

    fun merge(value: V, vararg branchIds: Int): BranchedListEntry<K, V> {
        val ids: MutableSet<Int> = HashSet(this.branchIds).apply { branchIds.forEach { this.add(it) } }
        return BranchedListEntry(key, value, ids)
    }
}