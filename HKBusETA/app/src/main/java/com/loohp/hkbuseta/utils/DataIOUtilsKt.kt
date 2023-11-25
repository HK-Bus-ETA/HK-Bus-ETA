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

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.Charset


fun IOSerializable.toByteArray(): ByteArray {
    val out = ByteArrayOutputStream()
    serialize(out)
    return out.toByteArray()
}

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

@FunctionalInterface
interface NullableOutputFunction<T> { @Throws(IOException::class) fun invoke(output: DataOutputStream, value: T) }

fun <T> DataOutputStream.writeNullable(value: T?, write: NullableOutputFunction<T>) {
    value?.let { writeBoolean(true); write.invoke(this, it) }?: writeBoolean(false)
}

@FunctionalInterface
interface NullableInputFunction<T> { @Throws(IOException::class) fun invoke(input: DataInputStream): T }

fun <T> DataInputStream.readNullable(read: NullableInputFunction<T>): T? {
    return if (readBoolean()) read.invoke(this) else null
}

@FunctionalInterface
interface MapOutputFunction<K, V> { @Throws(IOException::class) fun invoke(output: DataOutputStream, key: K, value: V) }

fun <K, V> DataOutputStream.writeMap(map: Map<K, V>, write: MapOutputFunction<K, V>) {
    writeInt(map.size)
    map.entries.forEach { write.invoke(this, it.key, it.value) }
}

@FunctionalInterface
interface MapInputFunction<K, V> { @Throws(IOException::class) fun invoke(input: DataInputStream): Pair<K, V> }

fun <M: MutableMap<K, V>, K, V> DataInputStream.readMap(map: M, read: MapInputFunction<K, V>): M {
    val size = readInt()
    (0 until size).forEach { _ -> read.invoke(this).let { map[it.first] = it.second } }
    return map
}

@FunctionalInterface
interface CollectionOutputFunction<T> { @Throws(IOException::class) fun invoke(output: DataOutputStream, element: T) }

fun <T> DataOutputStream.writeCollection(collection: Collection<T>, write: CollectionOutputFunction<T>) {
    writeInt(collection.size)
    collection.forEach { write.invoke(this, it) }
}

@FunctionalInterface
interface CollectionInputFunction<T> { @Throws(IOException::class) fun invoke(input: DataInputStream): T }

fun <C: MutableCollection<T>, T> DataInputStream.readCollection(collection: C, read: CollectionInputFunction<T>): C {
    val size = readInt()
    (0 until size).forEach { _ -> read.invoke(this).let { collection.add(it) } }
    return collection
}