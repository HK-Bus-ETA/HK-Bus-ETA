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

package com.loohp.hkbuseta.common.utils


inline fun CharSequence.remove(regex: Regex): String {
    return replace(regex, "")
}

inline fun String.remove(value: String, ignoreCase: Boolean = false): String {
    return replace(value, "", ignoreCase)
}

inline fun CharSequence.count(value: String): Int {
    var count = 0
    var index = indexOf(value)
    while (index != -1) {
        count++
        index = indexOf(value, index + value.length)
    }
    return count
}

inline fun CharSequence.eitherContains(other: CharSequence): Boolean {
    return this.contains(other) || other.contains(this)
}

fun String.editDistance(other: String): Int {
    if (this == other) return 0
    val dp = Array(length + 1) { IntArray(other.length + 1) }
    for (i in 0..length) {
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
    return dp[length][other.length]
}

private inline fun costOfSubstitution(a: Char, b: Char): Int {
    return if (a == b) 0 else 1
}

private inline fun min(vararg numbers: Int): Int {
    return numbers.minOrNull()?: Int.MAX_VALUE
}

fun Int.getCircledNumber(): String {
    return when (this) {
        0 -> "🄌"
        in 1..10 -> (10102 + (this - 1)).toChar().toString()
        in 11..20 -> (9451 + (this - 11)).toChar().toString()
        else -> toString()
    }
}

fun Int.getHollowCircledNumber(): String {
    return when (this) {
        0 -> "⓪"
        in 1..10 -> (9312 + (this - 1)).toChar().toString()
        else -> toString()
    }
}

inline fun Int.pad(characters: Int): String {
    return toString().padStart(characters, '0')
}