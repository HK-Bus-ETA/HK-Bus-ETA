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

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.charset
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.readBytes
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

private var gzipBodyAsTextImpl: ((NSData, String) -> String)? = null

fun provideGzipBodyAsTextImpl(impl: ((NSData, String) -> String)?) {
    gzipBodyAsTextImpl = impl
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class, BetaInteropApi::class)
actual suspend fun HttpResponse.gzipBodyAsText(fallbackCharset: Charset): String {
    return gzipBodyAsTextImpl?.let { impl ->
        this.bodyAsChannel().readRemaining().readBytes().let {
            it.usePinned { pinned ->
                val nsData = NSData.create(bytes = pinned.addressOf(0), length = it.size.convert())
                impl.invoke(nsData, (this.charset()?: fallbackCharset).toString())
            }
        }
    }?: throw RuntimeException("Unsupported Platform Operation")
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