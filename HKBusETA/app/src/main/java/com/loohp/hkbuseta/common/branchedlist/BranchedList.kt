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

import kotlin.random.Random

class BranchedList<K, V> (
    private val conflictResolve: (V, V) -> V = { a, _ -> a },
    collection: Collection<BranchedListEntry<K, V>> = emptyList(),
    private val branchId: Int = Random.nextInt()
) : MutableList<BranchedListEntry<K, V>> {

    private val list: MutableList<BranchedListEntry<K, V>> = mutableListOf<BranchedListEntry<K, V>>().apply { addAll(collection) }

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

    override val size: Int get() = list.size
    override fun clear() = list.clear()
    override fun addAll(elements: Collection<BranchedListEntry<K, V>>): Boolean = list.addAll(elements)
    override fun addAll(index: Int, elements: Collection<BranchedListEntry<K, V>>): Boolean = list.addAll(index, elements)
    override fun add(index: Int, element: BranchedListEntry<K, V>) = list.add(index, element)
    override fun add(element: BranchedListEntry<K, V>) = list.add(element)
    override fun get(index: Int): BranchedListEntry<K, V> = list[index]
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun iterator(): MutableIterator<BranchedListEntry<K, V>> = list.iterator()
    override fun listIterator(): MutableListIterator<BranchedListEntry<K, V>> = list.listIterator()
    override fun listIterator(index: Int): MutableListIterator<BranchedListEntry<K, V>> = list.listIterator(index)
    override fun removeAt(index: Int): BranchedListEntry<K, V> = list.removeAt(index)
    override fun set(index: Int, element: BranchedListEntry<K, V>): BranchedListEntry<K, V> = list.set(index, element)
    override fun retainAll(elements: Collection<BranchedListEntry<K, V>>): Boolean = list.retainAll(elements)
    override fun removeAll(elements: Collection<BranchedListEntry<K, V>>): Boolean = list.removeAll(elements)
    override fun remove(element: BranchedListEntry<K, V>): Boolean = list.remove(element)
    override fun lastIndexOf(element: BranchedListEntry<K, V>): Int = list.lastIndexOf(element)
    override fun indexOf(element: BranchedListEntry<K, V>): Int = list.indexOf(element)
    override fun containsAll(elements: Collection<BranchedListEntry<K, V>>): Boolean = list.containsAll(elements)
    override fun contains(element: BranchedListEntry<K, V>): Boolean = list.contains(element)

    override fun subList(fromIndex: Int, toIndex: Int): BranchedList<K, V> {
        return BranchedList(conflictResolve, list.subList(fromIndex, toIndex), branchId)
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
        return map { it.value }
    }

    fun asSequenceWithBranchIds(): Sequence<Pair<V, Set<Int>>> {
        return asSequence().map { it.value to it.branchIds }
    }

}