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
@file:Suppress("RedundantOverride")

package com.loohp.hkbuseta.common.objects

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.synchronize
import com.loohp.hkbuseta.common.utils.Immutable

@Immutable
class RouteListType private constructor(
    val name: String,
    val ordinal: Int,
    val isBuiltIn: Boolean
) : Comparable<RouteListType> {

    companion object {

        private val VALUES: ConcurrentMutableMap<String, RouteListType> = ConcurrentMutableMap()
        private val COUNTER = AtomicInt(-1)

        val NORMAL = createBuiltIn("normal")
        val NEARBY = createBuiltIn("nearby")
        val FAVOURITE = createBuiltIn("favourite")
        val RECENT = createBuiltIn("recent")

        private fun createBuiltIn(name: String): RouteListType {
            return VALUES.synchronize {
                VALUES.getOrPut(name.lowercase()) {
                    RouteListType(name, COUNTER.incrementAndGet(), true)
                }
            }
        }

        fun valueOf(name: String): RouteListType {
            return VALUES.synchronize {
                VALUES.getOrPut(name.lowercase()) {
                    RouteListType(name, COUNTER.incrementAndGet(), false)
                }
            }
        }

        fun values(): Array<RouteListType> {
            return VALUES.values.toTypedArray().apply { sort() }
        }
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    @Suppress("RedundantOverride")
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun compareTo(other: RouteListType): Int {
        return ordinal - other.ordinal
    }

}
