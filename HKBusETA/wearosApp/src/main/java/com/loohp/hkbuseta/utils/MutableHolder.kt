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

package com.loohp.hkbuseta.utils


class MutableHolder<T>(value: T? = null) {

    private val lock = ReadWriteLock()

    var value: T? = value
        get() {
            lock.readLock()
            try {
                return field
            } finally {
                lock.readUnlock()
            }
        }
        set(value) {
            lock.writeLock()
            try {
                field = value
            } finally {
                lock.writeUnlock()
            }
        }

    fun computeIfAbsent(compute: () -> T?) {
        lock.writeLock()
        try {
            if (value == null) {
                value = compute.invoke()
            }
        } finally {
            lock.writeUnlock()
        }
    }

    fun <O> ifPresent(compute: (T) -> O): O? {
        lock.readLock()
        try {
            return value?.let { compute.invoke(it) }
        } finally {
            lock.readUnlock()
        }
    }

    fun replace(replace: (T) -> T?): T? {
        lock.writeLock()
        try {
            return value?.let { replace.invoke(it) }.apply { value = this }
        } finally {
            lock.writeUnlock()
        }
    }

    fun remove(): T? {
        lock.writeLock()
        try {
            return value.apply { value = null }
        } finally {
            lock.writeUnlock()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MutableHolder<*>

        return value == other.value
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

}

fun <T> T.asMutableHolder(): MutableHolder<T> {
    @Suppress("UNCHECKED_CAST")
    return if (this is MutableHolder<*>) this as MutableHolder<T> else MutableHolder(this)
}
