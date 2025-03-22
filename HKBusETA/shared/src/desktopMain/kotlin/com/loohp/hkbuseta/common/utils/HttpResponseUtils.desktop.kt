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
import io.ktor.http.charset
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.GZIPInputStream

actual suspend fun HttpResponse.gzipBodyAsText(fallbackCharset: Charset): String {
    return withContext(Dispatchers.IO) {
        GZIPInputStream(bodyAsChannel().toInputStream())
            .bufferedReader(charset()?: fallbackCharset)
            .useLines { it.joinToString("") }
    }
}

actual suspend fun HttpResponse.bodyAsStringReadChannel(fallbackCharset: Charset): StringReadChannel {
    return bodyAsChannel().toStringReadChannel(charset()?: fallbackCharset)
}

actual suspend fun HttpResponse.gzipBodyAsStringReadChannel(fallbackCharset: Charset): StringReadChannel {
    return withContext(Dispatchers.IO) {
        GZIPInputStream(bodyAsChannel().toInputStream())
            .toStringReadChannel(charset()?: fallbackCharset)
    }
}

actual fun gzipSupported(): Boolean {
    return true
}