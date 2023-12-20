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

package com.loohp.hkbuseta.common.utils


fun CharSequence.eitherContains(other: CharSequence): Boolean {
    return this.contains(other) || other.contains(this)
}

fun String.editDistance(other: String): Int {
    if (this == other) {
        return 0
    }
    val dp = Array(this.length + 1) {
        IntArray(
            other.length + 1
        )
    }
    for (i in 0..this.length) {
        for (j in 0..other.length) {
            if (i == 0) {
                dp[i][j] = j
            } else if (j == 0) {
                dp[i][j] = i
            } else {
                dp[i][j] = min(
                    dp[i - 1][j - 1] + costOfSubstitution(this[i - 1], other[j - 1]),
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1
                )
            }
        }
    }
    return dp[this.length][other.length]
}

private fun costOfSubstitution(a: Char, b: Char): Int {
    return if (a == b) 0 else 1
}

private fun min(vararg numbers: Int): Int {
    return numbers.minOrNull()?: Int.MAX_VALUE
}

fun Int.getCircledNumber(): String {
    if (this < 0 || this > 20) {
        return this.toString()
    }
    if (this == 0) {
        return "⓿"
    }
    return if (this > 10) {
        (9451 + (this - 11)).toChar().toString()
    } else (10102 + (this - 1)).toChar().toString()
}

fun Int.getHollowCircledNumber(): String {
    if (this < 0 || this > 10) {
        return this.toString()
    }
    return if (this == 0) {
        "⓪"
    } else (9312 + (this - 1)).toChar().toString()
}