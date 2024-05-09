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
package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.asFormattedText
import com.loohp.hkbuseta.common.utils.optJsonObject
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject


@Immutable
class BilingualFormattedText(val zh: FormattedText, val en: FormattedText) : JSONSerializable, IOSerializable {

    companion object {

        val EMPTY = BilingualFormattedText("".asFormattedText(), "".asFormattedText())

        fun deserialize(json: JsonObject): BilingualFormattedText {
            val zh = FormattedText.deserialize(json.optJsonObject("zh")!!)
            val en = FormattedText.deserialize(json.optJsonObject("en")!!)
            return BilingualFormattedText(zh, en)
        }

        suspend fun deserialize(input: ByteReadChannel): BilingualFormattedText {
            val zh = FormattedText.deserialize(input)
            val en = FormattedText.deserialize(input)
            return BilingualFormattedText(zh, en)
        }
    }

    operator fun get(language: String): FormattedText {
        return if (language == "en") en else zh
    }

    operator fun component1(): FormattedText {
        return this.zh
    }

    operator fun component2(): FormattedText {
        return this.en
    }

    operator fun plus(other: BilingualFormattedText): BilingualFormattedText {
        return BilingualFormattedText(this.zh + other.zh, this.en + other.en)
    }

    operator fun plus(other: String): BilingualFormattedText {
        val formattedOther = other.asFormattedText()
        return BilingualFormattedText(this.zh + formattedOther, this.en + formattedOther)
    }

    override fun toString(): String {
        return "${zh.string} ${en.string}"
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("zh", zh.serialize())
            put("en", en.serialize())
        }
    }

    override suspend fun serialize(out: ByteWriteChannel) {
        zh.serialize(out)
        en.serialize(out)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BilingualFormattedText

        if (zh != other.zh) return false
        return en == other.en
    }

    override fun hashCode(): Int {
        var result = zh.hashCode()
        result = 31 * result + en.hashCode()
        return result
    }

}

infix fun FormattedText.withEn(en: FormattedText): BilingualFormattedText {
    return BilingualFormattedText(this, en)
}

infix fun FormattedText.withZh(zh: FormattedText): BilingualFormattedText {
    return BilingualFormattedText(zh, this)
}

fun FormattedText.asBilingualText(): BilingualFormattedText {
    return this withEn this
}