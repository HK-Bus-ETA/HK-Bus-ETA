package com.loohp.hkbuseta.notificationserver.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream


val HttpClient = HttpClient(Java)

suspend inline fun fetchResourceAsBytes(url: String): ByteArray? {
    return try {
        HttpClient.get(url) {
            headers {
                append(HttpHeaders.UserAgent, "Mozilla/5.0")
                append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                append(HttpHeaders.Pragma, "no-cache")
            }
            timeout {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 10000
            }
        }.bodyAsBytes()
    } catch (_: Exception) {
        null
    }
}

suspend inline fun fetchResourceAsText(url: String): String? {
    return try {
        HttpClient.get(url) {
            headers {
                append(HttpHeaders.UserAgent, "Mozilla/5.0")
                append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                append(HttpHeaders.Pragma, "no-cache")
            }
            timeout {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 10000
            }
        }.bodyAsText()
    } catch (_: Exception) {
        null
    }
}

suspend inline fun fetchResourceAsChannel(url: String): ByteReadChannel? {
    return try {
        HttpClient.get(url) {
            headers {
                append(HttpHeaders.UserAgent, "Mozilla/5.0")
                append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                append(HttpHeaders.Pragma, "no-cache")
            }
            timeout {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 10000
            }
        }.bodyAsChannel()
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T: JsonElement> fetchResourceAsJson(url: String): T? {
    return try {
        Json.decodeFromStream<T>(fetchResourceAsChannel(url)!!.toInputStream())
    } catch (_: Exception) {
        null
    }
}