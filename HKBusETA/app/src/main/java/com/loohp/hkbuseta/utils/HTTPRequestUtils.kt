/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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
package com.loohp.hkbuseta.utils

import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import javax.net.ssl.HttpsURLConnection


fun getTextResponse(link: String?): String? {
    try {
        val url = URL(link)
        val connection = url.openConnection() as HttpsURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 20000
        connection.useCaches = false
        connection.defaultUseCaches = false
        connection.addRequestProperty("User-Agent", "Mozilla/5.0")
        connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
        connection.addRequestProperty("Pragma", "no-cache")
        if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                return reader.lines().collect(Collectors.joining())
            }
        } else {
            return null
        }
    } catch (e: IOException) {
        return null
    }
}

fun getTextResponseWithPercentageCallback(link: String, customContentLength: Long, percentageCallback: (Float) -> Unit): String? {
    return getTextResponseWithPercentageCallback(link, customContentLength, { it }, percentageCallback)
}

fun getTextResponseWithPercentageCallback(link: String, customContentLength: Long, inputStreamTransform: (InputStream) -> InputStream, percentageCallback: (Float) -> Unit): String? {
    try {
        val url = URL(link)
        val connection = url.openConnection() as HttpsURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 20000
        connection.useCaches = false
        connection.defaultUseCaches = false
        connection.addRequestProperty("User-Agent", "Mozilla/5.0")
        connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
        connection.addRequestProperty("Pragma", "no-cache")
        if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
            val contentLength = (if (customContentLength >= 0) customContentLength else connection.contentLengthLong).toFloat()
            inputStreamTransform.invoke(connection.inputStream).use { inputStream ->
                val buffer = ByteArrayOutputStream()
                var readTotal = 0
                var nRead: Int
                val data = ByteArray(16384)
                while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
                    readTotal += nRead
                    buffer.write(data, 0, nRead)
                    percentageCallback.invoke(0f.coerceAtLeast((readTotal / contentLength).coerceAtMost(1f)))
                }
                percentageCallback.invoke(1f)
                return String(buffer.toByteArray(), 0, buffer.size(), StandardCharsets.UTF_8)
            }
        } else {
            return null
        }
    } catch (e: IOException) {
        return null
    }
}

fun getJSONResponse(link: String): JSONObject? {
    return getJSONResponse(link) { it }
}

fun getJSONResponse(link: String, inputStreamTransform: (InputStream) -> InputStream): JSONObject? {
    try {
        val url = URL(link)
        val connection = url.openConnection() as HttpsURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 20000
        connection.useCaches = false
        connection.defaultUseCaches = false
        connection.addRequestProperty("User-Agent", "Mozilla/5.0")
        connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
        connection.addRequestProperty("Pragma", "no-cache")
        if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(inputStreamTransform.invoke(connection.inputStream))).use { reader ->
                val reply = reader.lines().collect(Collectors.joining())
                return JSONObject(reply)
            }
        } else {
            return null
        }
    } catch (e: IOException) {
        return null
    } catch (e: JSONException) {
        return null
    }
}

fun postJSONResponse(link: String, body: JSONObject): JSONObject? {
    try {
        val url = URL(link)
        val connection = url.openConnection() as HttpsURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 20000
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.useCaches = false
        connection.defaultUseCaches = false
        connection.addRequestProperty("User-Agent", "Mozilla/5.0")
        connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
        connection.addRequestProperty("Pragma", "no-cache")
        connection.outputStream.use { os ->
            val input = body.toString().toByteArray(StandardCharsets.UTF_8)
            os.write(input, 0, input.size)
        }
        if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                val reply = reader.lines().collect(Collectors.joining())
                return JSONObject(reply)
            }
        } else {
            return null
        }
    } catch (e: IOException) {
        return null
    } catch (e: JSONException) {
        return null
    }
}

fun getInputStream(link: String): InputStream {
    val connection = URL(link).openConnection()
    connection.connectTimeout = 20000
    connection.readTimeout = 20000
    connection.useCaches = false
    connection.defaultUseCaches = false
    connection.addRequestProperty("User-Agent", "Mozilla/5.0")
    connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
    connection.addRequestProperty("Pragma", "no-cache")
    return connection.getInputStream()
}

fun download(link: String): ByteArray {
    getInputStream(link).use { `is` ->
        val baos = ByteArrayOutputStream()
        val byteChunk = ByteArray(4096)
        var n: Int
        while (`is`.read(byteChunk).also { n = it } > 0) {
            baos.write(byteChunk, 0, n)
        }
        return baos.toByteArray()
    }
}

fun getContentSize(link: String): Long {
    return try {
        val connection = URL(link).openConnection()
        connection.connectTimeout = 20000
        connection.readTimeout = 20000
        connection.useCaches = false
        connection.defaultUseCaches = false
        connection.addRequestProperty("User-Agent", "Mozilla/5.0")
        connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
        connection.addRequestProperty("Pragma", "no-cache")
        if (connection is HttpURLConnection) {
            connection.requestMethod = "HEAD"
        }
        connection.contentLengthLong
    } catch (e: IOException) {
        -1
    }
}

fun getContentType(link: String): String {
    return try {
        val connection = URL(link).openConnection()
        connection.connectTimeout = 20000
        connection.readTimeout = 20000
        connection.useCaches = false
        connection.defaultUseCaches = false
        connection.addRequestProperty("User-Agent", "Mozilla/5.0")
        connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
        connection.addRequestProperty("Pragma", "no-cache")
        if (connection is HttpURLConnection) {
            connection.requestMethod = "HEAD"
        }
        connection.contentType
    } catch (e: IOException) {
        ""
    }
}
