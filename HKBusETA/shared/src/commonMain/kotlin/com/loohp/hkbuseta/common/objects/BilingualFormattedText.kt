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
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.asFormattedText


@Immutable
class BilingualFormattedText(val zh: FormattedText, val en: FormattedText) {

    companion object {

        val EMPTY = BilingualFormattedText("".asFormattedText(), "".asFormattedText())

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
