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

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.KtorDsl
import java.net.UnknownHostException


@Suppress("FunctionName")
@KtorDsl
actual fun PlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(Android) {
        block.invoke(this)
        install(HttpRequestRetry) {
            retryOnExceptionIf(maxRetries = 3) { _, e -> e.isRetryableCause() }
            exponentialDelay()
        }
    }
}

private fun Throwable.isRetryableCause(): Boolean {
    return this is UnknownHostException || cause?.isRetryableCause() == true
}

actual fun HeadersBuilder.applyPlatformHeaders() {
    append(HttpHeaders.UserAgent, "Mozilla/5.0")
    append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
    append(HttpHeaders.Pragma, "no-cache")
}