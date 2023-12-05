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

import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.Charset


fun DataInputStream.read(size: Int): ByteArray {
    val bytes = ByteArray(size)
    read(bytes)
    return bytes
}

fun DataOutputStream.writeString(string: String, charset: Charset) {
    val bytes = string.toByteArray(charset)
    writeInt(bytes.size)
    write(bytes)
}

fun DataInputStream.readString(charset: Charset): String {
    val bytes = read(readInt())
    return String(bytes, charset)
}

fun <T> DataOutputStream.writeNullable(value: T?, write: (DataOutputStream, T) -> Unit) {
    value?.let { writeBoolean(true); write.invoke(this, it) }?: writeBoolean(false)
}

fun <T> DataInputStream.readNullable(read: (DataInputStream) -> T): T? {
    return if (readBoolean()) read.invoke(this) else null
}

fun <K, V> DataOutputStream.writeMap(map: Map<K, V>, write: (DataOutputStream, K, V) -> Unit) {
    writeInt(map.size)
    map.entries.forEach { write.invoke(this, it.key, it.value) }
}

fun <M: MutableMap<K, V>, K, V> DataInputStream.readMap(map: M, read: (DataInputStream) -> Pair<K, V>): M {
    val size = readInt()
    (0 until size).forEach { _ -> read.invoke(this).let { map[it.first] = it.second } }
    return map
}

fun <T> DataOutputStream.writeCollection(collection: Collection<T>, write: (DataOutputStream, T) -> Unit) {
    writeInt(collection.size)
    collection.forEach { write.invoke(this, it) }
}

fun <C: MutableCollection<T>, T> DataInputStream.readCollection(collection: C, read: (DataInputStream) -> T): C {
    val size = readInt()
    (0 until size).forEach { _ -> read.invoke(this).let { collection.add(it) } }
    return collection
}