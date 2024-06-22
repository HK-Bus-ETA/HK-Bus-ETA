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
import com.loohp.hkbuseta.common.utils.SmallSize
import com.loohp.hkbuseta.common.utils.buildFormattedString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@Immutable
data class SplashEntry(
    val imageName: String,
    val description: BilingualText,
    val orientation: Orientation = Orientation.ANY
) {
    val displayText: FormattedText by lazy {
        buildFormattedString {
            append(description.zh)
            appendLineBreak()
            append(description.en, SmallSize)
        }
    }

    fun fitOrientation(width: Int, height: Int): Boolean {
        return when (orientation) {
            Orientation.PORTRAIT -> height.toFloat() / width.toFloat() > 3F / 4F
            Orientation.LANDSCAPE -> width.toFloat() / height.toFloat() > 3F / 4F
            Orientation.ANY -> true
        }
    }
}

@Serializable(with = OrientationSerializer::class)
enum class Orientation {
    PORTRAIT, LANDSCAPE, ANY
}

object OrientationSerializer : KSerializer<Orientation> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Orientation", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Orientation {
        return Orientation.valueOf(decoder.decodeString().uppercase())
    }

    override fun serialize(encoder: Encoder, value: Orientation) {
        encoder.encodeString(value.name.lowercase())
    }

}