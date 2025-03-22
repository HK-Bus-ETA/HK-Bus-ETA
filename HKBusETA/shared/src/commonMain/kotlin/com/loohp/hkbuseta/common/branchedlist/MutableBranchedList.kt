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


class MutableBranchedList<K, V, B> private constructor(
    private val delegate: MutableList<BranchedListEntry<K, V, B>>,
    val branchId: B,
    private val conflictResolve: (V, V) -> V = { a, _ -> a },
    private val equalityPredicate: (K, K) -> Boolean = { a, b -> a == b }
) : MutableList<BranchedListEntry<K, V, B>> by delegate {

    constructor(
        branchId: B,
        conflictResolve: (V, V) -> V = { a, _ -> a },
        equalityPredicate: (K, K) -> Boolean = { a, b -> a == b }
    ): this(mutableListOf(), branchId, conflictResolve, equalityPredicate)

    fun add(key: K, value: V): Boolean {
        return add(BranchedListEntry(key, value, branchId))
    }

    operator fun set(key: K & Any, value: V & Any): Boolean {
        return add(key, value)
    }

    private fun keyIndexOf(key: K, from: Int): Int {
        val itr = listIterator(from)
        while (itr.hasNext()) {
            val i = itr.nextIndex()
            if (equalityPredicate.invoke(key, itr.next().key)) {
                return i
            }
        }
        return -1
    }

    private fun match(other: MutableBranchedList<K, V, B>, searchFrom: Int): IntArray? {
        for ((i, entry) in other.withIndex()) {
            val indexOf = keyIndexOf(entry.key, searchFrom)
            if (indexOf >= 0) {
                return intArrayOf(indexOf, i)
            }
        }
        return null
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableBranchedList<K, V, B> {
        return MutableBranchedList(delegate.subList(fromIndex, toIndex), branchId, conflictResolve)
    }

    fun merge(other: MutableBranchedList<K, V, B>, mergeToFrontIfNotFound: Boolean) {
        merge(other, 0, mergeToFrontIfNotFound, false)
    }

    private fun merge(other: MutableBranchedList<K, V, B>, searchFrom: Int, mergeToFrontIfNotFound: Boolean, addToFrontIfNotFound: Boolean) {
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
            merge(newOther, selfIndex + otherIndex + 1, mergeToFrontIfNotFound, mergeToFrontIfNotFound)
        }
    }

    fun values(): List<V> {
        return map { it.value }
    }

    fun asSequenceWithBranchIds(): Sequence<Pair<V, Set<B>>> {
        return asSequence().map { it.value to it.branchIds }
    }

}