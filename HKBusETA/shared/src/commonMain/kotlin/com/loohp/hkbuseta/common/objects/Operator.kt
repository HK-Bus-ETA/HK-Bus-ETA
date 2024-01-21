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
package com.loohp.hkbuseta.common.objects

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.synchronize
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.Platform
import com.loohp.hkbuseta.common.appcontext.platform
import com.loohp.hkbuseta.common.utils.Immutable

@Immutable
class Operator private constructor(
    val name: String,
    val ordinal: Int,
    private val stopIdPattern: Regex?,
    val isBuiltIn: Boolean
) : Comparable<Operator> {

    companion object {

        private val VALUES: ConcurrentMutableMap<String, Operator> = ConcurrentMutableMap()
        private val COUNTER = AtomicInt(-1)

        val KMB = createBuiltIn("kmb", "^[0-9A-Z]{16}$")
        val CTB = createBuiltIn("ctb", "^[0-9]{6}$")
        val NLB = createBuiltIn("nlb", "^[0-9]{1,4}$")
        val MTR_BUS = createBuiltIn("mtr-bus", "^[A-Z]?[0-9]{1,3}[A-Z]?-[A-Z][0-9]{3}$")
        val GMB = createBuiltIn("gmb", "^[0-9]{8}$")
        val LRT = createBuiltIn("lightRail", "^LR[0-9]+$")
        val MTR = createBuiltIn("mtr", "^[A-Z]{3}$")

        private fun createBuiltIn(name: String, stopIdPattern: String): Operator {
            return VALUES.synchronize {
                VALUES.getOrPut(name.lowercase()) {
                    Operator(name, COUNTER.incrementAndGet(), Regex(stopIdPattern), true)
                }
            }
        }

        fun valueOf(name: String): Operator {
            return VALUES.synchronize {
                VALUES.getOrPut(name.lowercase()) {
                    Operator(name, COUNTER.incrementAndGet(), null, false)
                }
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

fun AppIntent.putExtra(name: String, value: Operator): AppIntent {
    when (platform()) {
        Platform.WEAROS -> extras.putString(name, value.name)
        Platform.WATCHOS -> extras.data[name] = value
    }
    return this
}
