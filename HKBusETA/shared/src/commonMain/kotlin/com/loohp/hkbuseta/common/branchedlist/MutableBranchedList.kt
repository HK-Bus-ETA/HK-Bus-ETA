/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2026. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2026. Contributors
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
    private val delegate: MutableList<KeyedBranchedListEntry<K, V, B>>,
    val branchId: B,
    private val conflictResolve: (V, V) -> V = { a, _ -> a },
    private val equalityPredicate: (KeyedBranchedListEntry<K, V, B>, KeyedBranchedListEntry<K, V, B>) -> Boolean = { a, b -> a.key == b.key }
) : MutableList<KeyedBranchedListEntry<K, V, B>> by delegate {

    constructor(
        branchId: B,
        conflictResolve: (V, V) -> V = { a, _ -> a },
        equalityPredicate: (KeyedBranchedListEntry<K, V, B>, KeyedBranchedListEntry<K, V, B>) -> Boolean = { a, b -> a.key == b.key }
    ): this(mutableListOf(), branchId, conflictResolve, equalityPredicate)

    fun add(key: K, value: V): Boolean {
        return add(KeyedBranchedListEntry(key, value, branchId))
    }

    operator fun set(key: K & Any, value: V & Any): Boolean {
        return add(key, value)
    }

    private fun keyIndexOf(entry: KeyedBranchedListEntry<K, V, B>, from: Int): Int {
        val itr = listIterator(from)
        while (itr.hasNext()) {
            val i = itr.nextIndex()
            if (equalityPredicate.invoke(entry, itr.next())) {
                return i
            }
        }
        return -1
    }

    private fun match(other: MutableBranchedList<K, V, B>, searchFrom: Int): IntArray? {
        for ((i, entry) in other.withIndex()) {
            val indexOf = keyIndexOf(entry, searchFrom)
            if (indexOf >= 0) {
                return intArrayOf(indexOf, i)
            }
        }
        return null
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableBranchedList<K, V, B> {
        return MutableBranchedList(delegate.subList(fromIndex, toIndex), branchId, conflictResolve, equalityPredicate)
    }

    private fun addMissing(index: Int, previousMatchEnd: Int, entries: MutableList<KeyedBranchedListEntry<K, V, B>>): Int {
        while (entries.isNotEmpty() && previousMatchEnd > 0 && equalityPredicate.invoke(get(previousMatchEnd - 1), entries.first())) {
            val previous = get(previousMatchEnd - 1)
            val removed = entries.removeAt(0)
            set(previousMatchEnd - 1, previous.merge(conflictResolve.invoke(previous.value, removed.value), removed.branchIds))
        }
        addAll(index, entries)
        return entries.size
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
                addMissing(searchFrom, searchFrom, other.toMutableList())
            } else {
                addMissing(size, searchFrom, other.toMutableList())
            }
            return
        }
        val entry = get(selfIndex)
        set(selfIndex, entry.merge(conflictResolve.invoke(entry.value, other[otherIndex].value), other[otherIndex].branchIds))
        val insertedCount = addMissing(selfIndex, searchFrom, other.subList(0, otherIndex).toMutableList())
        val newOther = other.subList(otherIndex + 1, other.size)
        if (newOther.isNotEmpty()) {
            merge(newOther, selfIndex + insertedCount + 1, mergeToFrontIfNotFound, mergeToFrontIfNotFound)
        }
    }

    fun asSequenceWithBranchIds(): Sequence<BranchedListEntry<V, B>> {
        return asSequence().map { it.entry }
    }

}
