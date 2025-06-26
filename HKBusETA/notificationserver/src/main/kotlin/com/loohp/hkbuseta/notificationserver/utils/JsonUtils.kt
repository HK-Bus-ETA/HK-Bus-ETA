package com.loohp.hkbuseta.notificationserver.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


val EMPTY_JSON_OBJECT = JsonObject(emptyMap())
val EMPTY_JSON_ARRAY = JsonArray(emptyList())

fun emptyJsonObject(): JsonObject = EMPTY_JSON_OBJECT
fun emptyJsonArray(): JsonArray = EMPTY_JSON_ARRAY

inline fun <V> Map<*, V>.toJsonObject(valueSerializer: (V) -> Any? = { it }): JsonObject {
    return buildJsonObject {
        this@toJsonObject.forEach { (rawKey, rawValue) ->
            val key = rawKey.toString()
            when (val value = valueSerializer.invoke(rawValue)) {
                is JsonElement -> put(key, value)
                is Number -> put(key, value)
                is String -> put(key, value)
                is Boolean -> put(key, value)
                else -> throw IllegalArgumentException()
            }
        }
    }
}