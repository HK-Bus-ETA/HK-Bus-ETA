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

package com.loohp.hkbuseta.branchedlist

import com.google.common.collect.Lists
import java.util.LinkedList
import java.util.Random

class BranchedList<K, V> (
    private val conflictResolve: (V, V) -> V = { a, _ -> a },
    collection: Collection<BranchedListEntry<K, V>> = emptyList(),
    private val branchId: Int = BRANCH_ID_GENERATOR.nextInt()
) : LinkedList<BranchedListEntry<K, V>>(collection) {

    companion object {

        private val BRANCH_ID_GENERATOR = Random()

    }

    fun add(key: K, value: V): Boolean {
        return add(BranchedListEntry(key, value, branchId))
    }

    operator fun set(key: K & Any, value: V & Any): Boolean {
        return this.add(key, value)
    }

    fun keyIndexOf(key: K, from: Int): Int {
        val itr: ListIterator<BranchedListEntry<K, V>> = listIterator(from)
        while (itr.hasNext()) {
            val i = itr.nextIndex()
            if (key == itr.next().key) {
                return i
            }
        }
        return -1
    }

    fun match(other: BranchedList<K, V>, searchFrom: Int): IntArray? {
        val itr: ListIterator<BranchedListEntry<K, V>> = other.listIterator()
        while (itr.hasNext()) {
            val i = itr.nextIndex()
            val indexOf = keyIndexOf(itr.next().key, searchFrom)
            if (indexOf >= 0) {
                return intArrayOf(indexOf, i)
            }
        }
        return null
    }

    override fun subList(fromIndex: Int, toIndex: Int): BranchedList<K, V> {
        return BranchedList(conflictResolve, super.subList(fromIndex, toIndex), branchId)
    }

    fun merge(other: BranchedList<K, V>) {
        merge(other, 0, false)
    }

    fun merge(other: BranchedList<K, V>, searchFrom: Int, addToFrontIfNotFound: Boolean) {
        if (other.isEmpty()) {
            return
        }
        if (isEmpty()) {
            addAll(other)
            return
        }
        val (selfIndex, otherIndex) = match(other, searchFrom)?: let {
            if (addToFrontIfNotFound) {
                addAll(searchFrom, other)
            } else {
                addAll(other)
            }
            return
        }
        val entry = get(selfIndex)
        set(selfIndex, entry.merge(conflictResolve.invoke(entry.value, other[otherIndex].value), other.branchId))
        addAll(selfIndex, other.subList(0, otherIndex))
        val newOther = other.subList(otherIndex + 1, other.size)
        if (newOther.isNotEmpty()) {
            merge(newOther, selfIndex + 1, true)
        }
    }

    fun values(): List<V> {
        return Lists.transform(this) { it.value }
    }

    fun valuesWithBranchIds(): List<Pair<V, Set<Int>>> {
        return Lists.transform(this) { it.value to it.branchIds }
    }

}