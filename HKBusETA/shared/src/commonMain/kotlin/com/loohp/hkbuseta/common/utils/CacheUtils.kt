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

import co.touchlab.stately.collections.ConcurrentMutableMap


enum class CacheType {
    FUNCTION
}

data class CacheKey(
    val id: String,
    val type: CacheType,
    val keys: List<Any?>
) {
    val hashed: String = keys.joinToString(",", "$id-$type") { it.hashCode().toString(16) }
}

val globalCache: ConcurrentMutableMap<String, Pair<CacheKey, Any?>> = ConcurrentMutableMap()

inline fun clearGlobalCache() = globalCache.clear()

inline fun <reified T> cache(function: String, vararg keys: Any?, block: () -> T): T {
    val key = CacheKey(function, CacheType.FUNCTION, keys.asList())
    globalCache[key.hashed]?.let { (k, v) -> if (key.keys == k.keys && v is T) return v }
    return block.invoke().apply { globalCache[key.hashed] = key to this }
}