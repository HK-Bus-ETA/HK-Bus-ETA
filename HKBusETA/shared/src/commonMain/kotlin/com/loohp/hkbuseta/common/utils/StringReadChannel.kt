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

import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.json.Json


@Stable
expect interface StringReadChannel {
    val charset: Charset
    suspend fun string(): String
}

@Immutable
expect class StringBackedStringReadChannel(
    backingString: String,
    charset: Charset
): StringReadChannel

expect suspend inline fun <reified T> Json.decodeFromStringReadChannel(channel: StringReadChannel): T

fun String.toStringReadChannel(charset: Charset = Charsets.UTF_8): StringReadChannel {
    return StringBackedStringReadChannel(this, charset)
}