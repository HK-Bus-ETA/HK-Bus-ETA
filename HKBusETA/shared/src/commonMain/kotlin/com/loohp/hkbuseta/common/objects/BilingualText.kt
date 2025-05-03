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
package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.readString
import com.loohp.hkbuseta.common.utils.writeString
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.io.Sink
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
@Immutable
class BilingualText(
    val zh: String,
    val en: String
) : JSONSerializable, IOSerializable {

    companion object {

        val EMPTY = BilingualText("", "")

        fun deserialize(json: JsonObject): BilingualText {
            val zh = json.optString("zh")
            val en = json.optString("en")
            return BilingualText(zh, en)
        }

        suspend fun deserialize(input: ByteReadChannel): BilingualText {
            val zh = input.readString(Charsets.UTF_8)
            val en = input.readString(Charsets.UTF_8)
            return BilingualText(zh, en)
        }

    }

    operator fun get(language: String): String {
        return if (language == "en") en else zh
    }

    operator fun component1(): String {
        return this.zh
    }

    operator fun component2(): String {
        return this.en
    }

    operator fun plus(other: BilingualText): BilingualText {
        return BilingualText(this.zh + other.zh, this.en + other.en)
    }

    operator fun plus(other: String): BilingualText {
        return BilingualText(this.zh + other, this.en + other)
    }

    inline infix fun anyEquals(other: BilingualText): Boolean {
        return zh == other.zh || en == other.en
    }

    override fun toString(): String {
        return "$zh $en"
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("zh", zh)
            put("en", en)
        }
    }

    override fun serialize(out: Sink) {
        out.writeString(zh, Charsets.UTF_8)
        out.writeString(en, Charsets.UTF_8)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BilingualText

        if (zh != other.zh) return false
        return en == other.en
    }

    override fun hashCode(): Int {
        var result = zh.hashCode()
        result = 31 * result + en.hashCode()
        return result
    }

}

inline fun BilingualText.trim(): BilingualText {
    return BilingualText(zh.trim(), en.trim())
}

inline fun BilingualText.isEmpty(): Boolean {
    return !isNotEmpty()
}

inline fun BilingualText.isBlank(): Boolean {
    return !isNotBlank()
}

inline fun BilingualText.isNotEmpty(): Boolean {
    return zh.isNotEmpty() || en.isNotEmpty()
}

inline fun BilingualText.isNotBlank(): Boolean {
    return zh.isNotBlank() || en.isNotBlank()
}

inline infix fun String.withEn(en: String): BilingualText {
    return BilingualText(this, en)
}

inline infix fun String.withZh(zh: String): BilingualText {
    return BilingualText(zh, this)
}

inline fun String.asBilingualText(): BilingualText {
    return this withEn this
}

inline infix fun String.anyEquals(other: BilingualText): Boolean {
    return this == other.zh || this == other.en
}

fun <T> Iterable<T>.joinToBilingualText(separator: BilingualText = ", ".asBilingualText(), prefix: BilingualText = "".asBilingualText(), postfix: BilingualText = "".asBilingualText(), limit: Int = -1, truncated: BilingualText = "...".asBilingualText(), transform: ((T) -> BilingualText)? = null): BilingualText {
    return joinToString(separator.zh, prefix.zh, postfix.zh, limit, truncated.zh, transform?.let { f -> { f.invoke(it).zh } }) withEn joinToString(separator.en, prefix.en, postfix.en, limit, truncated.en, transform?.let { f -> { f.invoke(it).en } })
}

fun <T> Sequence<T>.joinToBilingualText(separator: BilingualText = ", ".asBilingualText(), prefix: BilingualText = "".asBilingualText(), postfix: BilingualText = "".asBilingualText(), limit: Int = -1, truncated: BilingualText = "...".asBilingualText(), transform: ((T) -> BilingualText)? = null): BilingualText {
    return joinToString(separator.zh, prefix.zh, postfix.zh, limit, truncated.zh, transform?.let { f -> { f.invoke(it).zh } }) withEn joinToString(separator.en, prefix.en, postfix.en, limit, truncated.en, transform?.let { f -> { f.invoke(it).en } })
}

fun Iterable<BilingualText>.joinBilingualText(separator: BilingualText = ", ".asBilingualText(), prefix: BilingualText = "".asBilingualText(), postfix: BilingualText = "".asBilingualText(), limit: Int = -1, truncated: BilingualText = "...".asBilingualText(), transform: ((BilingualText) -> BilingualText)? = null): BilingualText {
    val zh = mutableListOf<String>()
    val en = mutableListOf<String>()
    for ((index, text) in withIndex()) {
        val transformed = transform?.takeIf { limit < 0 || index < limit }?.invoke(text)?: text
        zh.add(transformed.zh)
        en.add(transformed.en)
    }
    return zh.joinToString(separator.zh, prefix.zh, postfix.zh, limit, truncated.zh) withEn en.joinToString(separator.en, prefix.en, postfix.en, limit, truncated.en)
}

fun Sequence<BilingualText>.joinBilingualText(separator: BilingualText = ", ".asBilingualText(), prefix: BilingualText = "".asBilingualText(), postfix: BilingualText = "".asBilingualText(), limit: Int = -1, truncated: BilingualText = "...".asBilingualText(), transform: ((BilingualText) -> BilingualText)? = null): BilingualText {
    val zh = mutableListOf<String>()
    val en = mutableListOf<String>()
    for ((index, text) in withIndex()) {
        val transformed = transform?.takeIf { limit < 0 || index < limit }?.invoke(text)?: text
        zh.add(transformed.zh)
        en.add(transformed.en)
    }
    return zh.joinToString(separator.zh, prefix.zh, postfix.zh, limit, truncated.zh) withEn en.joinToString(separator.en, prefix.en, postfix.en, limit, truncated.en)
}