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

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.synchronize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes


enum class CacheType {
    FUNCTION
}

data class CacheKey(
    val id: String,
    val type: CacheType,
    val keys: List<Any?>,
    val time: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CacheKey) return false

        if (id != other.id) return false
        if (type != other.type) return false
        if (keys != other.keys) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + keys.hashCode()
        return result
    }
}

object CacheStore {
    val cacheLifetime = 10.minutes.inWholeMilliseconds
    var globalCache: ConcurrentMutableMap<CacheKey, Any?>? = ConcurrentMutableMap()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(cacheLifetime / 2)
                val now = currentTimeMillis()
                val cache = globalCache?: continue
                val keys = cache.synchronize { cache.keys.toSet() }
                for (key in keys) {
                    if (now - key.time > cacheLifetime) {
                        cache.remove(key)
                    }
                }
            }
        }
    }
}

fun toggleCache(cache: Boolean) {
    if (cache) {
        if (CacheStore.globalCache == null) {
            CacheStore.globalCache = ConcurrentMutableMap()
        }
    } else {
        CacheStore.globalCache = null
    }
}

inline fun clearGlobalCache() = CacheStore.globalCache?.clear()

inline fun <reified T> cache(function: String, vararg keys: Any?, block: () -> T): T {
    return CacheStore.globalCache?.run {
        val key = CacheKey(function, CacheType.FUNCTION, keys.toList(), currentTimeMillis())
        val cachedValue = this[key]
        (if (cachedValue == null || cachedValue !is T) block.invoke() else cachedValue).also { this[key] = it }
    }?: block.invoke()
}