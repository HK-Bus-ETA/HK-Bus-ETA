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

package com.loohp.hkbuseta.common.utils

import androidx.compose.runtime.snapshots.SnapshotStateList
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


class AutoSortedList<E: Comparable<E>, T: MutableList<E>>(
    val backingList: T,
    private val comparator: Comparator<E> = naturalOrder(),
    sync: Boolean
): MutableList<E> by backingList {
    private val lock: Lock? = if (sync) Lock() else null
    private inline fun <T> block(block: () -> T): T {
        return if (lock == null) {
            block.invoke()
        } else {
            lock.withLock(block)
        }
    }
    init {
        block { if (backingList.size > 1) sortWith(comparator) }
    }
    override fun add(element: E): Boolean {
        block {
            var index = binarySearch(element, comparator)
            if (index < 0) {
                index = -index - 1
            } else {
                index++
                while (index in indices && comparator.compare(element, this[index]) == 0) {
                    index++
                }
            }
            backingList.add(index, element)
        }
        return true
    }
    override fun add(index: Int, element: E) {
        add(element)
    }
    override fun addAll(elements: Collection<E>): Boolean {
        block { elements.forEach { add(it) } }
        return true
    }
    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        block { elements.forEach { add(it) } }
        return true
    }
}

inline fun <E: Comparable<E>, T: MutableList<E>> T.asAutoSortedList(comparator: Comparator<E> = naturalOrder(), sync: Boolean = false): AutoSortedList<E, T> {
    return AutoSortedList(this, comparator, sync)
}

inline fun <E: Comparable<E>, T: SnapshotStateList<E>> T.asAutoSortedList(comparator: Comparator<E> = naturalOrder()): AutoSortedList<E, T> {
    return AutoSortedList(this, comparator, true)
}

inline fun <T> List<T>.sequenceSimilarity(sequence: List<T>, equality: (T, T) -> Boolean = { a, b -> a == b }): Float {
    if (sequence.isEmpty()) return 1F
    val total = sequence.size.toFloat()
    var similarity = 0F
    for (i in indices) {
        var matches = 0
        for (j in sequence.indices) {
            if (i + j + 1 >= size) break
            if (equality.invoke(this[i + j], sequence[j])) {
                matches++
            }
        }
        similarity = (matches / total).coerceAtLeast(similarity)
        if (similarity >= 1F) break
    }
    return similarity
}

inline fun <T> List<T>.indexOfSequence(sequence: List<T>): Int {
    if (sequence.isEmpty()) return 0
    if (size < sequence.size) return -1
    for (i in 0..(size - sequence.size)) {
        var matches = true
        for (j in sequence.indices) {
            if (this[i + j] != sequence[j]) {
                matches = false
                break
            }
        }
        if (matches) return i
    }
    return -1
}

inline fun <T> List<T>.containsSequence(sequence: List<T>): Boolean {
    return indexOfSequence(sequence) >= 0
}

fun <T> List<List<T>>.mergeSequences(allElements: List<T>): List<List<T>> {
    val flatten = asSequence().flatten().toSet()
    val invert = allElements.filterNot { flatten.contains(it) }
    return allElements.differenceSequence(invert)
}

inline fun <T> List<T>.differenceSequence(referenceList: List<T>): List<List<T>> {
    if (isEmpty()) return emptyList()
    if (referenceList.isEmpty()) return listOf(this)
    val result = mutableListOf<List<T>>()
    var index = -1
    var referenceIndex = -1
    while (referenceIndex < referenceList.size - 1) {
        index++
        referenceIndex++
        val item = this[index]
        var referenceItem = referenceList[referenceIndex]
        if (item != referenceItem) {
            var joinIndex = size
            while (joinIndex == size && referenceIndex < referenceList.size) {
                referenceItem = referenceList[referenceIndex]
                for (i in (index + 1) until size) {
                    if (this[i] == referenceItem) {
                        joinIndex = i
                        break
                    }
                }
                referenceIndex++
            }
            referenceIndex--
            result.add(subList(index, joinIndex))
            if (joinIndex == size) {
                break
            } else {
                index = joinIndex
            }
        }
    }
    if (index + 1 < size) {
        result.add(subList(index + 1, size))
    }
    return result
}

data class OutOfOrderSequenceEntry<T>(
    val difference: List<T>,
    val shuffled: List<T>,
    val isFirstListOutOfOrder: Boolean
)

inline fun <T> outOfOrderSequence(list1: List<T>, list2: List<T>): List<OutOfOrderSequenceEntry<T>> {
    if (!list1.containsAll(list2) || !list2.containsAll(list1)) return emptyList()
    var offset1 = -999
    var offset2 = -999
    outer@for (i in list1.indices) {
        for (u in list2.indices) {
            if (list1[i] == list2[u]) {
                offset1 = i - 1
                offset2 = u - 1
                break@outer
            }
        }
    }
    if (offset1 < -1 || offset2 < -1) return emptyList()
    val results = mutableListOf<OutOfOrderSequenceEntry<T>>()
    while (offset1 < list1.size - 1 && offset2 < list2.size - 1) {
        offset1++
        offset2++
        val item1 = list1[offset1]
        val item2 = list2[offset2]
        if (item1 != item2) {
            var list1JoinIndex = list1.size
            var list2JoinIndex = list2.size
            for (i in (offset1 + 1) until list1.size) {
                if (list1[i] == item2) {
                    list1JoinIndex = i
                    break
                }
            }
            for (i in (offset2 + 1) until list2.size) {
                if (list2[i] == item1) {
                    list2JoinIndex = i
                    break
                }
            }
            if (list1JoinIndex >= list1.size || list2JoinIndex >= list2.size) break
            if (list1JoinIndex <= list2JoinIndex) {
                results.add(OutOfOrderSequenceEntry(list1.subList(offset1, list1JoinIndex), list2.subList(offset2, list2JoinIndex), true))
                offset2 = list1JoinIndex
                offset1 = list1JoinIndex
            } else {
                results.add(OutOfOrderSequenceEntry(list2.subList(offset2, list2JoinIndex), list1.subList(offset1, list1JoinIndex), false))
                offset1 = list2JoinIndex
                offset2 = list2JoinIndex
            }
        }
    }
    return results
}

inline fun <T, C> Collection<T>.mapToSet(mapping: (T) -> C): Set<C> {
    return buildSet { this@mapToSet.forEach { this.add(mapping.invoke(it)) } }
}

inline fun <T> Collection<T>.indexOf(matcher: (T) -> Boolean): Int {
    for ((i, t) in withIndex()) {
        if (matcher.invoke(t)) {
            return i
        }
    }
    return -1
}

inline fun <T> Iterable<T>.indexOfOrNull(element: T): Int? {
    return indexOf(element).takeIf { it >= 0 }
}

inline fun <T> Iterable<T>.filterToSet(predicate: (T) -> Boolean): Set<T> {
    return filterTo(mutableSetOf(), predicate)
}

inline fun <T> Collection<T>.indexesOf(element: T): Set<Int> {
    return withIndex().asSequence().filter { it.value == element }.map { it.index }.toSet().takeIf { it.isNotEmpty() }?: setOf(-1)
}

inline fun <T> Collection<T>.indexesOf(crossinline predicate: (T) -> Boolean): Set<Int> {
    return withIndex().asSequence().filter { predicate.invoke(it.value) }.map { it.index }.toSet().takeIf { it.isNotEmpty() }?: setOf(-1)
}

inline fun <T> Collection<T>.commonElementPercentage(other: Collection<T>): Float {
    val count = asSequence().count { other.contains(it) }
    if (count == 0) return 0F
    return count / other.size.toFloat()
}

inline fun <T> Iterable<T>.any(threshold: Int): Boolean {
    if (this is Collection && isEmpty()) return false
    var count = 0
    for (element in this) {
        ++count
        if (count >= threshold) return true
    }
    return false
}

inline fun <T> Iterable<T>.any(threshold: Int, predicate: (T) -> Boolean): Boolean {
    if (this is Collection && isEmpty()) return false
    var count = 0
    for (element in this) {
        if (predicate(element)) ++count
        if (count >= threshold) return true
    }
    return false
}

inline fun <T> Sequence<T>.any(threshold: Int): Boolean {
    var count = 0
    for (element in this) {
        ++count
        if (count >= threshold) return true
    }
    return false
}

inline fun <T> Sequence<T>.any(threshold: Int, predicate: (T) -> Boolean): Boolean {
    var count = 0
    for (element in this) {
        if (predicate(element)) ++count
        if (count >= threshold) return true
    }
    return false
}

inline fun <reified R> Iterable<*>.firstIsInstanceOrNull(): R? {
    for (element in this) if (element is R) return element
    return null
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Collection<T>?.isNotNullAndNotEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullAndNotEmpty != null)
    }
    return !isNullOrEmpty()
}

@OptIn(ExperimentalContracts::class)
inline fun <K, V> Map<out K, V>?.isNotNullAndNotEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullAndNotEmpty != null)
    }
    return !isNullOrEmpty()
}