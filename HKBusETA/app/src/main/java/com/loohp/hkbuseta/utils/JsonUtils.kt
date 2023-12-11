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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put


fun JsonObject.optJsonObject(key: String, default: JsonObject? = null): JsonObject? {
    return this[key]?.let { if (it is JsonObject) it else default }?: default
}

fun JsonObject.optJsonArray(key: String, default: JsonArray? = null): JsonArray? {
    return this[key]?.let { if (it is JsonArray) it else default }?: default
}

fun JsonObject.optString(key: String, default: String = ""): String {
    return this[key]?.let { if (it is JsonPrimitive) it.content else default }?: default
}

fun JsonObject.optBoolean(key: String, default: Boolean = false): Boolean {
    return this[key]?.let { if (it is JsonPrimitive) it.booleanOrNull?: default else default }?: default
}

fun JsonObject.optInt(key: String, default: Int = 0): Int {
    return this[key]?.let { if (it is JsonPrimitive) it.intOrNull?: default else default }?: default
}

fun JsonObject.optDouble(key: String, default: Double = Double.NaN): Double {
    return this[key]?.let { if (it is JsonPrimitive) it.doubleOrNull?: default else default }?: default
}

fun JsonArray.optJsonObject(index: Int, default: JsonObject? = null): JsonObject? {
    return this.getOrNull(index)?.let { if (it is JsonObject) it else default }?: default
}

fun JsonArray.optJsonArray(index: Int, default: JsonArray? = null): JsonArray? {
    return this.getOrNull(index)?.let { if (it is JsonArray) it else default }?: default
}

fun JsonArray.optString(index: Int, default: String = ""): String {
    return this.getOrNull(index)?.let { if (it is JsonPrimitive) it.content else default }?: default
}

fun JsonArray.optBoolean(index: Int, default: Boolean = false): Boolean {
    return this.getOrNull(index)?.let { if (it is JsonPrimitive) it.booleanOrNull?: default else default }?: default
}

fun JsonArray.optInt(index: Int, default: Int = 0): Int {
    return this.getOrNull(index)?.let { if (it is JsonPrimitive) it.intOrNull?: default else default }?: default
}

fun JsonArray.optDouble(index: Int, default: Double = Double.NaN): Double {
    return this.getOrNull(index)?.let { if (it is JsonPrimitive) it.doubleOrNull?: default else default }?: default
}

fun <T> JsonArray.mapToMutableList(deserializer: (JsonElement) -> T): MutableList<T> {
    return ArrayList<T>(size).apply { this@mapToMutableList.forEach { this.add(deserializer.invoke(it)) } }
}

fun <T> JsonArray.mapToMutableSet(deserializer: (JsonElement) -> T): MutableSet<T> {
    return LinkedHashSet<T>().apply { this@mapToMutableSet.forEach { this.add(deserializer.invoke(it)) } }
}

inline fun <K, V> JsonObject.mapToMutableMap(keyDeserializer: (String) -> K, valueDeserializer: (JsonElement) -> V): MutableMap<K, V> {
    return LinkedHashMap<K, V>().apply { this@mapToMutableMap.forEach { (key, value) -> this[keyDeserializer.invoke(key)] = valueDeserializer.invoke(value) } }
}

inline fun <V> JsonObject.mapToMutableMap(valueDeserializer: (JsonElement) -> V): MutableMap<String, V> {
    return mapToMutableMap({ it }, valueDeserializer)
}

fun <T> Collection<T>.toJsonArray(): JsonArray {
    return buildJsonArray { this@toJsonArray.forEach {
        when (it) {
            is JsonElement -> add(it)
            is Number -> add(it)
            is String -> add(it)
            is Boolean -> add(it)
            else -> throw IllegalArgumentException()
        }
    } }
}

fun <T> Sequence<T>.toJsonArray(): JsonArray {
    return buildJsonArray { this@toJsonArray.forEach {
        when (it) {
            is JsonElement -> add(it)
            is Number -> add(it)
            is String -> add(it)
            is Boolean -> add(it)
            else -> throw IllegalArgumentException()
        }
    } }
}

inline fun <V> Map<*, V>.toJsonObject(valueSerializer: (V) -> Any?): JsonObject {
    return buildJsonObject { this@toJsonObject.forEach { (rawKey, rawValue) ->
        val key = rawKey.toString()
        when (val value = valueSerializer.invoke(rawValue)) {
            is JsonElement -> put(key, value)
            is Number -> put(key, value)
            is String -> put(key, value)
            is Boolean -> put(key, value)
            else -> throw IllegalArgumentException()
        }
    } }
}
