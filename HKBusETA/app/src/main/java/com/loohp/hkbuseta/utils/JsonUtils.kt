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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


fun jsonArrayOf(vararg items: Any?): JSONArray {
    val array = JSONArray()
    for (item in items) {
        array.put(item)
    }
    return array
}

fun JSONObject.clone(): JSONObject {
    return try {
        JSONObject(toString())
    } catch (e: JSONException) {
        throw RuntimeException(e)
    }
}

fun JSONArray.clone(): JSONArray {
    return try {
        JSONArray(toString())
    } catch (e: JSONException) {
        throw RuntimeException(e)
    }
}

fun JSONArray.indexOf(obj: Any?): Int {
    for (i in 0 until length()) {
        if (opt(i) == obj) {
            return i
        }
    }
    return -1
}

inline fun <T> JSONArray.mapToList(mapping: (Any?) -> T): MutableList<T> {
    val list: MutableList<T> = ArrayList(length())
    for (i in 0 until length()) {
        list.add(mapping.invoke(opt(i)))
    }
    return list
}

fun JSONArray.contains(obj: Any?): Boolean {
    return indexOf(obj) >= 0
}

@Suppress("UNCHECKED_CAST")
fun <T> JSONArray.toList(type: Class<T>): MutableList<T> {
    val list: MutableList<T> = ArrayList(length())
    for (i in 0 until length()) {
        list.add(opt(i) as T)
    }
    return list
}

@Suppress("UNCHECKED_CAST")
fun <T> JSONArray.toSet(type: Class<T>): MutableSet<T> {
    val set: MutableSet<T> = LinkedHashSet()
    for (i in 0 until length()) {
        set.add(opt(i) as T)
    }
    return set
}

inline fun <K, V> JSONObject.toMap(keyDeserializer: (String) -> K, valueDeserializer: (Any?) -> V): MutableMap<K, V> {
    val map: MutableMap<K, V> = LinkedHashMap()
    val itr = keys()
    while (itr.hasNext()) {
        val key = itr.next()
        map[keyDeserializer.invoke(key)] = valueDeserializer.invoke(opt(key))
    }
    return map
}

inline fun <V> JSONObject.toMap(valueDeserializer: (Any?) -> V): MutableMap<String, V> {
    return toMap({ it }, valueDeserializer)
}

fun <T> Collection<T>.toJSONArray(): JSONArray {
    val array = JSONArray()
    for (t in this) {
        array.put(t)
    }
    return array
}

fun <T> Sequence<T>.toJSONArray(): JSONArray {
    val array = JSONArray()
    forEach { array.put(it) }
    return array
}

inline fun <V> Map<*, V>.toJSONObject(valueSerializer: (V) -> Any?): JSONObject {
    val json = JSONObject()
    for ((key, value) in this) {
        json.put(key.toString(), valueSerializer.invoke(value))
    }
    return json
}
