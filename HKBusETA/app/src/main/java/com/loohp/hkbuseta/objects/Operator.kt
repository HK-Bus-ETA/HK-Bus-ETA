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
package com.loohp.hkbuseta.objects

import androidx.compose.runtime.Immutable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Immutable
class Operator private constructor(
    val name: String,
    val ordinal: Int,
    private val stopIdPattern: Regex?,
    val isBuiltIn: Boolean
) : Comparable<Operator> {

    companion object {

        private val VALUES: MutableMap<String, Operator> = ConcurrentHashMap()
        private val COUNTER = AtomicInteger(0)

        val KMB = createBuiltIn("kmb", "^[0-9A-Z]{16}$")
        val CTB = createBuiltIn("ctb", "^[0-9]{6}$")
        val NLB = createBuiltIn("nlb", "^[0-9]{1,4}$")
        val MTR_BUS = createBuiltIn("mtr-bus", "^[A-Z]?[0-9]{1,3}[A-Z]?-[A-Z][0-9]{3}$")
        val GMB = createBuiltIn("gmb", "^[0-9]{8}$")
        val LRT = createBuiltIn("lightRail", "^LR[0-9]+$")
        val MTR = createBuiltIn("mtr", "^[A-Z]{3}$")

        private fun createBuiltIn(name: String, stopIdPattern: String): Operator {
            return VALUES.computeIfAbsent(name.lowercase()) {
                Operator(it, COUNTER.getAndIncrement(), Regex(stopIdPattern), true)
            }
        }

        fun valueOf(name: String): Operator {
            return VALUES.computeIfAbsent(name.lowercase()) {
                Operator(it, COUNTER.getAndIncrement(), null, false)
            }
        }

        fun values(): Array<Operator> {
            return VALUES.values.toTypedArray().apply { sort() }
        }
    }

    fun matchStopIdPattern(stopId: String): Boolean {
        return stopIdPattern != null && stopIdPattern.matches(stopId)
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

    override fun compareTo(other: Operator): Int {
        return ordinal - other.ordinal
    }

}
