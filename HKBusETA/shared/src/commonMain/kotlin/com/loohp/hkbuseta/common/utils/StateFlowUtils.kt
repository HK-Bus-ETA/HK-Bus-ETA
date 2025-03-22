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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun interface Closeable {
    fun close()
}

data class Optional<T: Any>(val value: T?)

class MutableNullableStateFlow<T: Any>(
    handle: MutableStateFlow<Optional<T>>
): MutableNonNullStateFlow<Optional<T>>(handle) {
    constructor(value: T?): this(MutableNonNullStateFlow(Optional(value)))
    @Deprecated("Use valueNullable Instead")
    override var value: Optional<T>
        get() = handle.value
        set(value) { handle.value = value }
    var valueNullable: T?
        get() = handle.value.value
        set(value) { handle.value = Optional(value) }
    override fun watch(block: (Optional<T>) -> Unit): Closeable {
        return super.watch(block)
    }
}

class MutableNonNullStateFlowList<T>(
    handle: MutableStateFlow<List<T>>
): MutableNonNullStateFlow<List<T>>(handle) {
    constructor(value: List<T>): this(MutableNonNullStateFlow(value))
    override var value: List<T>
        get() = handle.value
        set(value) { handle.value = value }
    override fun watch(block: (List<T>) -> Unit): Closeable {
        return super.watch(block)
    }
}

open class MutableNonNullStateFlow<T: Any>(
    internal val handle: MutableStateFlow<T>
): MutableStateFlow<T> {
    constructor(value: T): this(MutableStateFlow(value))
    override val replayCache: List<T> get() = handle.replayCache
    override val subscriptionCount: StateFlow<Int> get() = handle.subscriptionCount
    override var value: T
        get() = handle.value
        set(value) { handle.value = value }
    override suspend fun collect(collector: FlowCollector<T>): Nothing = handle.collect(collector)
    override fun compareAndSet(expect: T, update: T): Boolean = handle.compareAndSet(expect, update)
    @ExperimentalCoroutinesApi
    override fun resetReplayCache() = handle.resetReplayCache()
    override fun tryEmit(value: T): Boolean = handle.tryEmit(value)
    override suspend fun emit(value: T) = handle.emit(value)
    open fun watch(block: (T) -> Unit): Closeable {
        val job = Job()
        onEach {
            block(it)
        }.launchIn(CoroutineScope(Dispatchers.Main + job))
        return Closeable { job.cancel() }
    }
}

fun <T: Any> MutableStateFlow<T>.wrap(): MutableNonNullStateFlow<T> = MutableNonNullStateFlow(this)

fun <T: Any> MutableStateFlow<T?>.wrapNullable(): MutableNullableStateFlow<T> = MutableNullableStateFlow(this.value)

fun <T: Any> MutableStateFlow<List<T>>.wrapList(): MutableNonNullStateFlowList<T> = MutableNonNullStateFlowList(this)