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
import kotlinx.serialization.json.Json


@Stable
actual interface StringReadChannel {
    actual val charset: Charset
    actual suspend fun string(): String
}

@Immutable
actual class StringBackedStringReadChannel actual constructor(
    private val backingString: String,
    override val charset: Charset,
): StringReadChannel {
    override suspend fun string(): String = backingString
}

actual suspend inline fun <reified T> Json.decodeFromStringReadChannel(channel: StringReadChannel): T {
    return decodeFromString(channel.string())
}