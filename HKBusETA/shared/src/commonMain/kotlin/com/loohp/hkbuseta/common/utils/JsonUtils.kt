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
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put


inline fun JsonObject.optJsonObject(key: String, default: JsonObject? = null): JsonObject? {
    return this[key]?.let { it as? JsonObject }?: default
}

inline fun JsonObject.optJsonArray(key: String, default: JsonArray? = null): JsonArray? {
    return this[key]?.let { it as? JsonArray }?: default
}

inline fun JsonObject.optString(key: String, default: String = ""): String {
    return this[key]?.let { it as? JsonPrimitive }?.content?: default
}

inline fun JsonObject.optBoolean(key: String, default: Boolean = false): Boolean {
    return this[key]?.let { it as? JsonPrimitive }?.booleanOrNull?: default
}

inline fun JsonObject.optInt(key: String, default: Int = 0): Int {
    return this[key]?.let { it as? JsonPrimitive }?.intOrNull?: default
}

inline fun JsonObject.optLong(key: String, default: Long = 0): Long {
    return this[key]?.let { it as? JsonPrimitive }?.longOrNull?: default
}

inline fun JsonObject.optDouble(key: String, default: Double = Double.NaN): Double {
    return this[key]?.let { it as? JsonPrimitive }?.doubleOrNull?: default
}

inline fun JsonArray.optJsonObject(index: Int, default: JsonObject? = null): JsonObject? {
    return this.getOrNull(index)?.let { it as? JsonObject }?: default
}

inline fun JsonArray.optJsonArray(index: Int, default: JsonArray? = null): JsonArray? {
    return this.getOrNull(index)?.let { it as? JsonArray }?: default
}

inline fun JsonArray.optString(index: Int, default: String = ""): String {
    return this.getOrNull(index)?.let { it as? JsonPrimitive }?.content?: default
}

inline fun JsonArray.optBoolean(index: Int, default: Boolean = false): Boolean {
    return this.getOrNull(index)?.let { it as? JsonPrimitive }?.booleanOrNull?: default
}

inline fun JsonArray.optInt(index: Int, default: Int = 0): Int {
    return this.getOrNull(index)?.let { it as? JsonPrimitive }?.intOrNull?: default
}

inline fun JsonArray.optLong(index: Int, default: Long = 0): Long {
    return this.getOrNull(index)?.let { it as? JsonPrimitive }?.longOrNull?: default
}

inline fun JsonArray.optDouble(index: Int, default: Double = Double.NaN): Double {
    return this.getOrNull(index)?.let { it as? JsonPrimitive }?.doubleOrNull?: default
}

inline fun <T> JsonArray.mapToMutableList(deserializer: (JsonElement) -> T): MutableList<T> {
    return ArrayList<T>(size).apply { this@mapToMutableList.forEach { this.add(deserializer.invoke(it)) } }
}

inline fun <T> JsonArray.mapToMutableSet(deserializer: (JsonElement) -> T): MutableSet<T> {
    return LinkedHashSet<T>().apply { this@mapToMutableSet.forEach { this.add(deserializer.invoke(it)) } }
}

inline fun <K, V> JsonObject.mapToMutableMap(keyDeserializer: (String) -> K, valueDeserializer: (JsonElement) -> V): MutableMap<K, V> {
    return LinkedHashMap<K, V>().apply { this@mapToMutableMap.forEach { (key, value) -> this[keyDeserializer.invoke(key)] = valueDeserializer.invoke(value) } }
}

inline fun <V> JsonObject.mapToMutableMap(valueDeserializer: (JsonElement) -> V): MutableMap<String, V> {
    return mapToMutableMap({ it }, valueDeserializer)
}

inline fun <T> Collection<T>.toJsonArray(): JsonArray {
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

inline fun <T> Sequence<T>.toJsonArray(): JsonArray {
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
