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

package com.loohp.hkbuseta.common.utils

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet


@Immutable
data class ImmutableState<T>(val value: T)

inline fun <reified T> T.asImmutableState(): ImmutableState<T> {
    @Suppress("UNCHECKED_CAST")
    return if (this is ImmutableState<*> && value is T) this as ImmutableState<T> else ImmutableState(this)
}

@Immutable
class DelegatedImmutableCollection<E>(collection: Collection<E>): ImmutableCollection<E>, Collection<E> by collection

inline fun <T> Collection<T>.asImmutableCollection(): ImmutableCollection<T> {
    return (this as? ImmutableCollection<T>)?: DelegatedImmutableCollection(this)
}

@Immutable
class DelegatedImmutableList<E>(list: List<E>): ImmutableList<E>, List<E> by list {
    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> = super.subList(fromIndex, toIndex)
}

inline fun <T> List<T>.asImmutableList(): ImmutableList<T> {
    return (this as? ImmutableList<T>)?: DelegatedImmutableList(this)
}

inline fun <E> buildImmutableList(builderAction: MutableList<E>.() -> Unit): ImmutableList<E> {
    return buildList(builderAction).asImmutableList()
}

@Immutable
class DelegatedImmutableSet<E>(set: Set<E>): ImmutableSet<E>, Set<E> by set

inline fun <T> Set<T>.asImmutableSet(): ImmutableSet<T> {
    return (this as? ImmutableSet<T>)?: DelegatedImmutableSet(this)
}

@Immutable
class DelegatedImmutableMap<K, V>(private val map: Map<K, V>): ImmutableMap<K, V>, Map<K, V> by map {
    override val keys: ImmutableSet<K> get() = map.keys.asImmutableSet()
    override val values: ImmutableCollection<V> get() = map.values.asImmutableCollection()
    override val entries: ImmutableSet<Map.Entry<K, V>> get() = map.entries.asImmutableSet()
}

inline fun <K, V> Map<K, V>.asImmutableMap(): ImmutableMap<K, V> {
    return this as? ImmutableMap<K, V>?: DelegatedImmutableMap(this)
}

inline fun <K, V> buildImmutableMap(builderAction: MutableMap<K, V>.() -> Unit): ImmutableMap<K, V> {
    return buildMap(builderAction).asImmutableMap()
}