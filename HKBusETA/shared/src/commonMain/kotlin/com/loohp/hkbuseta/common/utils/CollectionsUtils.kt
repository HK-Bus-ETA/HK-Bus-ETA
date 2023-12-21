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

import co.touchlab.stately.collections.ConcurrentMutableSet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking


inline fun <T, C> Collection<T>.mapToSet(mapping: (T) -> C): Set<C> {
    return buildSet { this@mapToSet.forEach { this.add(mapping.invoke(it)) } }
}

fun <T> Collection<T>.indexOf(matcher: (T) -> Boolean): Int {
    for ((i, t) in withIndex()) {
        if (matcher.invoke(t)) {
            return i
        }
    }
    return -1
}

fun <T> Collection<T>.commonElementPercentage(other: Collection<T>): Float {
    return asSequence().map { if (other.contains(it)) 1 else 0 }.sum() / other.size.toFloat()
}

inline fun <T, R> List<T>.parallelMap(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline transform: (T) -> R): List<R> {
    return map { CoroutineScope(dispatcher).async { transform.invoke(it) } }.map { runBlocking { it.await() } }
}

inline fun <T, R> List<T>.parallelMapNotNull(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline transform: (T) -> R?): List<R> {
    return map { CoroutineScope(dispatcher).async { transform.invoke(it) } }.mapNotNull { runBlocking { it.await() } }
}

fun <T> Sequence<T>.toImmutableList(): ImmutableList<T> {
    val builder = persistentListOf<T>().builder()
    forEach { builder.add(it) }
    return builder.build()
}

fun <T> Sequence<T>.toImmutableSet(): ImmutableSet<T> {
    val builder = persistentSetOf<T>().builder()
    forEach { builder.add(it) }
    return builder.build()
}

inline fun <T> Sequence<T>.distinctBy(crossinline keyExtractor: (T) -> Any): Sequence<T> {
    val seen: MutableSet<Any> = ConcurrentMutableSet()
    return this.filter { seen.add(keyExtractor.invoke(it)) }
}