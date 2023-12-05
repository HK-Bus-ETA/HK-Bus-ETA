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

package com.loohp.hkbuseta.utils

import com.google.common.collect.ImmutableMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.stream.Stream


fun <T> Collection<T>.indexOf(matcher: (T) -> Boolean): Int {
    for ((i, t) in withIndex()) {
        if (matcher.invoke(t)) {
            return i
        }
    }
    return -1
}

fun <T> Collection<T>.commonElementPercentage(other: Collection<T>): Float {
    return stream().mapToInt { if (other.contains(it)) 1 else 0 }.summaryStatistics().let { it.sum / it.count.toFloat() }
}

operator fun <K, V> ImmutableMap.Builder<K, V>.set(key: K & Any, value: V & Any): ImmutableMap.Builder<K, V> {
    return this.put(key, value)
}

inline fun <T, R> List<T>.parallelMap(executor: ExecutorService = ForkJoinPool.commonPool(), crossinline transform: (T) -> R): List<R> {
    return map { executor.submit(Callable { transform.invoke(it) }) }.map { it.get() }
}

inline fun <T, R> List<T>.parallelMapNotNull(executor: ExecutorService = ForkJoinPool.commonPool(), crossinline transform: (T) -> R?): List<R> {
    return map { executor.submit(Callable { transform.invoke(it) }) }.mapNotNull { it.get() }
}

fun <T> Stream<T>.toImmutableList(): ImmutableList<T> {
    val builder = persistentListOf<T>().builder()
    forEach { builder.add(it) }
    return builder.build()
}

fun <T> Stream<T>.toImmutableSet(): ImmutableSet<T> {
    val builder = persistentSetOf<T>().builder()
    forEach { builder.add(it) }
    return builder.build()
}

inline fun <T> Stream<T>.distinctBy(crossinline keyExtractor: (T) -> Any): Stream<T> {
    val seen: MutableSet<Any> = ConcurrentHashMap.newKeySet()
    return this.filter { seen.add(keyExtractor.invoke(it)) }
}