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

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.charset
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

private var gzipBodyAsTextImpl: (suspend (ByteArray, Charset) -> String)? = null

fun provideGzipBodyAsTextImpl(impl: (suspend (ByteArray, Charset) -> String)?) {
    gzipBodyAsTextImpl = impl
}

actual suspend fun HttpResponse.gzipBodyAsText(fallbackCharset: Charset): String {
    return gzipBodyAsTextImpl
        ?.invoke(this.bodyAsChannel().readRemaining().readByteArray(), this.charset()?: fallbackCharset)
        ?: throw RuntimeException("Unsupported Platform Operation")
}

actual suspend fun HttpResponse.bodyAsStringReadChannel(fallbackCharset: Charset): StringReadChannel {
    return bodyAsText(fallbackCharset).toStringReadChannel(charset()?: fallbackCharset)
}

actual suspend fun HttpResponse.gzipBodyAsStringReadChannel(fallbackCharset: Charset): StringReadChannel {
    return gzipBodyAsText(fallbackCharset).toStringReadChannel(charset()?: fallbackCharset)
}

actual fun gzipSupported(): Boolean {
    return gzipBodyAsTextImpl != null
}