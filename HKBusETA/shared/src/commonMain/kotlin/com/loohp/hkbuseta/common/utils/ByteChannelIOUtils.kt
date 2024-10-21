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

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readFully
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readLong
import kotlinx.io.Sink


suspend inline fun ByteReadChannel.read(size: Int): ByteArray {
    val bytes = ByteArray(size)
    readFully(bytes)
    return bytes
}

suspend inline fun ByteReadChannel.readFloat(): Float {
    return Float.fromBits(readInt())
}

inline fun Sink.writeFloat(float: Float) {
    writeInt(float.toBits())
}

suspend inline fun ByteReadChannel.readDouble(): Double {
    return Double.fromBits(readLong())
}

inline fun Sink.writeDouble(double: Double) {
    writeLong(double.toBits())
}

suspend inline fun ByteReadChannel.readBoolean(): Boolean {
    return readByte() > 0
}

inline fun Sink.writeBoolean(boolean: Boolean) {
    writeByte(if (boolean) 1 else 0)
}

inline fun Sink.writeString(string: String, charset: Charset) {
    val bytes = string.toByteArray(charset)
    writeInt(bytes.size)
    writeFully(bytes)
}

suspend inline fun ByteReadChannel.readString(charset: Charset): String {
    return read(readInt()).asString(charset)
}

inline fun ByteArray.asString(charset: Charset): String {
    return when (charset) {
        Charsets.UTF_8 -> decodeToString()
        else -> buildPacket { writeFully(this@asString, 0, size) }.readText(charset)
    }
}

inline fun <T> Sink.writeNullable(value: T?, write: (Sink, T) -> Unit) {
    value?.let { writeBoolean(true); write.invoke(this, it) }?: writeBoolean(false)
}

suspend inline fun <T> ByteReadChannel.readNullable(read: (ByteReadChannel) -> T): T? {
    return if (readBoolean()) read.invoke(this) else null
}

inline fun Sink.writeConditional(conditional: Boolean, write: (Sink) -> Unit) {
    if (conditional) { writeBoolean(true); write.invoke(this) } else { writeBoolean(false) }
}

suspend inline fun ByteReadChannel.readConditional(read: (ByteReadChannel) -> Unit) {
    if (readBoolean()) read.invoke(this)
}

inline fun <K, V> Sink.writeMap(map: Map<K, V>, write: (Sink, K, V) -> Unit) {
    writeInt(map.size)
    map.entries.forEach { write.invoke(this, it.key, it.value) }
}

suspend inline fun <M: MutableMap<K, V>, K, V> ByteReadChannel.readMap(map: M, read: (ByteReadChannel) -> Pair<K, V>): M {
    val size = readInt()
    (0 until size).forEach { _ -> read.invoke(this).let { map[it.first] = it.second } }
    return map
}

inline fun <T> Sink.writeCollection(collection: Collection<T>, write: (Sink, T) -> Unit) {
    writeInt(collection.size)
    collection.forEach { write.invoke(this, it) }
}

suspend inline fun <C: MutableCollection<T>, T> ByteReadChannel.readCollection(collection: C, read: (ByteReadChannel) -> T): C {
    val size = readInt()
    (0 until size).forEach { _ -> read.invoke(this).let { collection.add(it) } }
    return collection
}