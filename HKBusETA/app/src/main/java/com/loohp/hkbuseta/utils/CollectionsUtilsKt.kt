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
import java.util.function.Predicate
import java.util.stream.Stream

operator fun <K, V> ImmutableMap.Builder<K, V>.set(key: K & Any, value: V & Any): ImmutableMap.Builder<K, V> {
    return this.put(key, value)
}

fun <T, C: MutableCollection<T>> C.chainedRemoveIf(predicate: Predicate<T>): C {
    removeIf(predicate)
    return this
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