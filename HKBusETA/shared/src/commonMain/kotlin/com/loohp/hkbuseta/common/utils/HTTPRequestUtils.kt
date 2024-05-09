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
import io.ktor.client.HttpClientConfig
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
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.KtorDsl
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import nl.adaptivity.xmlutil.serialization.XML


val urlHasSchemeRegex: Regex = "^[0-9a-zA-Z]+://.*".toRegex()

fun String.normalizeUrlScheme(defaultScheme: String = "http"): String {
    return if (matches(urlHasSchemeRegex)) this else "$defaultScheme://$this"
}

val httpClient: HttpClient = PlatformHttpClient {
    install(HttpTimeout)
    install(createClientPlugin("remove-utf-8") {
        on(Send) { request ->
            request.headers.remove("Accept-Charset")
            proceed(request)
        }
    })
}

@Suppress("FunctionName")
@KtorDsl
expect fun PlatformHttpClient(block: HttpClientConfig<*>.() -> Unit = { /* do nothing */ }): HttpClient

expect fun HeadersBuilder.applyPlatformHeaders()

suspend inline fun isReachable(link: String): Boolean {
    return try {
        httpClient.get(link) {
            headers {
                applyPlatformHeaders()
            }
            timeout {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 20000
            }
        }.status == HttpStatusCode.OK
    } catch (_: Exception) {
        false
    }
}

suspend inline fun getTextResponse(link: String): StringReadChannel? {
    return try {
        httpClient.get(link) {
            headers {
                applyPlatformHeaders()
            }
            timeout {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 20000
            }
        }.let {
            if (it.status == HttpStatusCode.OK) {
                it.bodyAsStringReadChannel(Charsets.UTF_8)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend inline fun getTextResponseWithPercentageCallback(link: String, customContentLength: Long, crossinline percentageCallback: (Float) -> Unit): StringReadChannel? {
    return getTextResponseWithPercentageCallback(link, customContentLength, false, percentageCallback)
}

suspend inline fun getTextResponseWithPercentageCallback(link: String, customContentLength: Long, gzip: Boolean, crossinline percentageCallback: (Float) -> Unit): StringReadChannel? {
    return try {
        httpClient.get(link) {
            headers {
                applyPlatformHeaders()
            }
            timeout {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 20000
            }
            onDownload { bytesSentTotal, rawContentLength ->
                (customContentLength.takeIf { l -> l >= 0 }?: rawContentLength)?.let {
                    percentageCallback.invoke(0f.coerceAtLeast((bytesSentTotal.toFloat() / it).coerceAtMost(1f)))
                }
            }
        }.let {
            if (it.status == HttpStatusCode.OK) {
                if (gzip) it.gzipBodyAsStringReadChannel(Charsets.UTF_8) else it.bodyAsStringReadChannel(Charsets.UTF_8)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend inline fun <reified T> getJSONResponse(link: String): T? {
    return getJSONResponse(link, false)
}

suspend inline fun <reified T> getJSONResponse(link: String, gzip: Boolean): T? {
    return try {
        httpClient.get(link) {
            headers {
                applyPlatformHeaders()
            }
            timeout {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 20000
            }
        }.let {
            if (it.status == HttpStatusCode.OK) {
                Json.decodeFromStringReadChannel<T>(if (gzip) it.gzipBodyAsStringReadChannel(Charsets.UTF_8) else it.bodyAsStringReadChannel(Charsets.UTF_8))
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend inline fun <reified I, reified T> postJSONResponse(link: String, body: I): T? {
    return try {
        httpClient.post(link) {
            headers {
                applyPlatformHeaders()
            }
            contentType(ContentType.Application.Json)
            setBody(if (body is JsonElement) body.toString() else Json.encodeToString(body))
            timeout {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 20000
            }
        }.let {
            if (it.status == HttpStatusCode.OK) {
                Json.decodeFromStringReadChannel<T>(it.bodyAsStringReadChannel(Charsets.UTF_8))
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend inline fun <reified T> getXMLResponse(link: String): T? {
    return getXMLResponse(link, false)
}

suspend inline fun <reified T: Any> getXMLResponse(link: String, gzip: Boolean): T? {
    return try {
        httpClient.get(link) {
            headers {
                applyPlatformHeaders()
            }
            timeout {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 20000
            }
        }.let {
            if (it.status == HttpStatusCode.OK) {
                XML.decodeFromString<T>(if (gzip) it.gzipBodyAsText(Charsets.UTF_8) else it.bodyAsText(Charsets.UTF_8))
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend inline fun getMovedRedirect(link: String): String? {
    return try {
        httpClient.config {
            followRedirects = false
        }.get(link) {
            headers {
                applyPlatformHeaders()
            }
            timeout {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 20000
            }
        }.let {
            if (it.status == HttpStatusCode.MovedPermanently || it.status == HttpStatusCode.Found) {
                it.headers[HttpHeaders.Location]
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}