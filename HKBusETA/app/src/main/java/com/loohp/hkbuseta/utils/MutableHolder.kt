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

import java.util.concurrent.locks.ReentrantReadWriteLock


class MutableHolder<T>(value: T? = null) {

    private val lock = ReentrantReadWriteLock()

    var value: T? = value
        get() {
            lock.readLock().lock()
            try {
                return field
            } finally {
                lock.readLock().unlock()
            }
        }
        set(value) {
            lock.writeLock().lock()
            try {
                field = value
            } finally {
                lock.writeLock().unlock()
            }
        }

    fun computeIfAbsent(compute: () -> T?) {
        lock.writeLock().lock()
        try {
            if (value == null) {
                value = compute.invoke()
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun ifPresent(compute: (T) -> Unit) {
        lock.readLock().lock()
        try {
            value?.let { compute.invoke(it) }
        } finally {
            lock.readLock().unlock()
        }
    }

    fun replace(replace: (T) -> T?): T? {
        lock.writeLock().lock()
        try {
            value?.let { value = replace.invoke(it) }
            return value
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun remove(): T? {
        lock.writeLock().lock()
        try {
            val v = value
            value = null
            return v
        } finally {
            lock.writeLock().unlock()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MutableHolder<*>

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }
}

fun <T> T.asMutableHolder(): MutableHolder<T> {
    @Suppress("UNCHECKED_CAST")
    return if (this is MutableHolder<*>) this as MutableHolder<T> else MutableHolder(this)
}
