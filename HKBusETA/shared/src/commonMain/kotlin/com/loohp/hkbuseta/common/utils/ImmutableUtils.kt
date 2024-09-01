/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet


@Immutable
data class ImmutableState<T>(val value: T) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImmutableState<*>) return false

        return value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode()?: 0
    }
}

inline fun <T> T.asImmutableState(): ImmutableState<T> {
    @Suppress("UNCHECKED_CAST")
    return if (this is ImmutableState<*>) this as ImmutableState<T> else ImmutableState(this)
}

@Immutable
class DelegatedImmutableCollection<E>(collection: Collection<E>): ImmutableCollection<E>, Collection<E> by collection

inline fun <T> Collection<T>.asImmutableCollection(): ImmutableCollection<T> {
    return if (this is ImmutableCollection<T>) this else DelegatedImmutableCollection(this)
}

@Immutable
class DelegatedImmutableList<E>(list: List<E>): ImmutableList<E>, List<E> by list {
    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> = super.subList(fromIndex, toIndex)
}

inline fun <T> List<T>.asImmutableList(): ImmutableList<T> {
    return if (this is ImmutableList<T>) this else DelegatedImmutableList(this)
}

inline fun <E> buildImmutableList(builderAction: MutableList<E>.() -> Unit): ImmutableList<E> {
    return buildList(builderAction).asImmutableList()
}

@Immutable
class DelegatedImmutableSet<E>(set: Set<E>): ImmutableSet<E>, Set<E> by set

inline fun <T> Set<T>.asImmutableSet(): ImmutableSet<T> {
    return if (this is ImmutableSet<T>) this else DelegatedImmutableSet(this)
}

@Immutable
class DelegatedImmutableMap<K, V>(private val map: Map<K, V>): ImmutableMap<K, V>, Map<K, V> by map {
    override val keys: ImmutableSet<K> get() = map.keys.asImmutableSet()
    override val values: ImmutableCollection<V> get() = map.values.asImmutableCollection()
    override val entries: ImmutableSet<Map.Entry<K, V>> get() = map.entries.asImmutableSet()
}

inline fun <K, V> Map<K, V>.asImmutableMap(): ImmutableMap<K, V> {
    return if (this is ImmutableMap<K, V>) this else DelegatedImmutableMap(this)
}