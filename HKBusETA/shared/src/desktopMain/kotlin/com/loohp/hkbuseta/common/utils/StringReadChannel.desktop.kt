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

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.pool.ByteArrayPool
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream


@Stable
actual interface StringReadChannel {
    val inputStream: InputStream
    actual val charset: Charset
    actual suspend fun string(): String
    suspend fun transferTo(out: OutputStream)
}

@Immutable
actual class StringBackedStringReadChannel actual constructor(
    private val backingString: String,
    override val charset: Charset,
): StringReadChannel {
    override val inputStream: InputStream by lazy { ByteArrayInputStream(backingString.toByteArray(charset)) }
    override suspend fun string(): String = backingString
    override suspend fun transferTo(out: OutputStream) {
        withContext(Dispatchers.IO) { out.write(backingString.toByteArray(charset)) }
    }
}

@Stable
class InputStreamBackedStringReadChannel(
    override val inputStream: InputStream,
    override val charset: Charset,
): StringReadChannel, AutoCloseable {
    override suspend fun string(): String {
        return inputStream.bufferedReader(charset).useLines { it.joinToString("") }
    }
    override suspend fun transferTo(out: OutputStream) {
        inputStream.use { input ->
            val buffer = ByteArrayPool.borrow()
            try {
                val bufferSize = buffer.size
                var read: Int
                while (input.read(buffer, 0, bufferSize).also { read = it } >= 0) {
                    out.write(buffer, 0, read)
                }
            } finally {
                ByteArrayPool.recycle(buffer)
            }
        }
    }
    override fun close() {
        ignoreExceptions { inputStream.close() }
    }
}

@Stable
class ByteReadChannelBackedStringReadChannel(
    private val channel: ByteReadChannel,
    override val charset: Charset,
): StringReadChannel {
    override val inputStream: InputStream by lazy { channel.toInputStream() }
    override suspend fun string(): String {
        return channel.readRemaining().use { it.readText(charset) }
    }
    override suspend fun transferTo(out: OutputStream) {
        channel.copyTo(out)
    }
}

fun InputStream.toStringReadChannel(charset: Charset = Charsets.UTF_8): StringReadChannel {
    return InputStreamBackedStringReadChannel(this, charset)
}

fun ByteReadChannel.toStringReadChannel(charset: Charset = Charsets.UTF_8): StringReadChannel {
    return ByteReadChannelBackedStringReadChannel(this, charset)
}

@OptIn(ExperimentalSerializationApi::class)
actual suspend inline fun <reified T> Json.decodeFromStringReadChannel(channel: StringReadChannel): T {
    return if (channel is StringBackedStringReadChannel) {
        decodeFromString(channel.string())
    } else {
        channel.inputStream.use { decodeFromStream(it) }
    }
}