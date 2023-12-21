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

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject


private val httpClient = HttpClient {
    install(HttpTimeout)
    install(createClientPlugin("remove-utf-8") {
        on(Send) { request ->
            request.headers.remove("Accept-Charset")
            this.proceed(request)
        }
    })
}

fun getTextResponse(link: String): String? {
    return runBlocking(Dispatchers.IO) {
        try {
            httpClient.get(link) {
                headers {
                    append(HttpHeaders.UserAgent, "Mozilla/5.0")
                    append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                    append(HttpHeaders.Pragma, "no-cache")
                }
                timeout {
                    requestTimeoutMillis = 20000
                    connectTimeoutMillis = 20000
                }
            }.let {
                if (it.status == HttpStatusCode.OK) {
                    it.bodyAsText(Charsets.UTF_8)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun getTextResponseWithPercentageCallback(link: String, customContentLength: Long, percentageCallback: (Float) -> Unit): String? {
    return getTextResponseWithPercentageCallback(link, customContentLength, false, percentageCallback)
}

fun getTextResponseWithPercentageCallback(link: String, customContentLength: Long, gzip: Boolean, percentageCallback: (Float) -> Unit): String? {
    return runBlocking(Dispatchers.IO) {
        try {
            httpClient.get(link) {
                headers {
                    append(HttpHeaders.UserAgent, "Mozilla/5.0")
                    append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                    append(HttpHeaders.Pragma, "no-cache")
                }
                timeout {
                    requestTimeoutMillis = 20000
                    connectTimeoutMillis = 20000
                }
                onDownload { bytesSentTotal, rawContentLength ->
                    (customContentLength.takeIf { l -> l >= 0 }?: rawContentLength)?.let {
                        percentageCallback.invoke(0f.coerceAtLeast((bytesSentTotal.toFloat() / it).coerceAtMost(1f)))
                    }
                }
            }.let {
                if (it.status == HttpStatusCode.OK) {
                    if (gzip) it.gzipBodyAsText(Charsets.UTF_8) else it.bodyAsText(Charsets.UTF_8)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun getJSONResponse(link: String): JsonObject? {
    return getJSONResponse(link, false)
}

fun getJSONResponse(link: String, gzip: Boolean): JsonObject? {
    return runBlocking(Dispatchers.IO) {
        try {
            httpClient.get(link) {
                headers {
                    append(HttpHeaders.UserAgent, "Mozilla/5.0")
                    append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                    append(HttpHeaders.Pragma, "no-cache")
                }
                timeout {
                    requestTimeoutMillis = 20000
                    connectTimeoutMillis = 20000
                }
            }.let {
                if (it.status == HttpStatusCode.OK) {
                    Json.decodeFromString<JsonObject>(if (gzip) it.gzipBodyAsText(Charsets.UTF_8) else it.bodyAsText(Charsets.UTF_8))
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun postJSONResponse(link: String, body: JsonObject): JsonObject? {
    return runBlocking(Dispatchers.IO) {
        try {
            httpClient.post(link) {
                headers {
                    append(HttpHeaders.UserAgent, "Mozilla/5.0")
                    append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                    append(HttpHeaders.Pragma, "no-cache")
                }
                contentType(ContentType.Application.Json)
                setBody(body)
                timeout {
                    requestTimeoutMillis = 20000
                    connectTimeoutMillis = 20000
                }
            }.let {
                if (it.status == HttpStatusCode.OK) {
                    Json.decodeFromString<JsonObject>(it.bodyAsText(Charsets.UTF_8))
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}