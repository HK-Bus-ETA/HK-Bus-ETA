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

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.charsets.Charsets.UTF_8
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

data class FormattedText(
    val content: List<FormattedTextContent>
) : CharSequence, JSONSerializable, IOSerializable {

    companion object {

        fun deserialize(json: JsonObject): FormattedText {
            val contentArray = json.optJsonArray("content")!!
            val content: MutableList<FormattedTextContent> = mutableListOf()
            for (formattedTextContentObj in contentArray) {
                val string = formattedTextContentObj.jsonObject.optString("string")
                val styleArray = formattedTextContentObj.jsonObject.optJsonArray("style")!!
                val style: MutableList<FormattingTextContentStyle> = mutableListOf()
                for (styleObj in styleArray) {
                    style.add(FormattingTextContentStyle.deserialize(styleObj.jsonObject))
                }
                content.add(FormattedTextContent(string, style))
            }
            return FormattedText(content)
        }

        suspend fun deserialize(input: ByteReadChannel): FormattedText {
            val contentSize = input.readInt()
            val content: MutableList<FormattedTextContent> = mutableListOf()
            (0 until contentSize).forEach { _ ->
                val string = input.readString(UTF_8)
                val styleSize = input.readInt()
                val style: MutableList<FormattingTextContentStyle> = mutableListOf()
                (0 until styleSize).forEach { _ ->
                    style.add(FormattingTextContentStyle.deserialize(input))
                }
                content.add(FormattedTextContent(string, style))
            }
            return FormattedText(content)
        }
    }

    val string: String = content.joinToString("") { it.string }

    override val length: Int = string.length

    operator fun plus(other: FormattedText): FormattedText {
        return buildFormattedString { append(this@FormattedText); append(other) }
    }

    override fun get(index: Int): Char {
        return string[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return string.subSequence(startIndex, endIndex)
    }

    fun contains(compare: String): Boolean {
        return string.contains(compare)
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("content", buildJsonArray {
                for (formattedTextContent in content) {
                    add(buildJsonObject {
                        put("string", formattedTextContent.string)
                        put("style", buildJsonArray {
                            for (style in formattedTextContent.style) {
                                add(style.serialize())
                            }
                        })
                    })
                }
            })
        }
    }

    override suspend fun serialize(out: ByteWriteChannel) {
        out.writeInt(content.size)
        for (formattedTextContent in content) {
            out.writeString(formattedTextContent.string, UTF_8)
            out.writeInt(formattedTextContent.style.size)
            for (style in formattedTextContent.style) {
                style.serialize(out)
            }
        }
    }

}

infix fun FormattedText.strEq(other: String): Boolean {
    return string == other
}

class FormattedTextBuilder {

    private val content: MutableList<FormattedTextContent> = mutableListOf()

    fun append(formattedText: FormattedText) {
        content.addAll(formattedText.content)
    }

    fun append(formattedText: FormattedText, vararg style: FormattingTextContentStyle) {
        append(formattedText, style.toList())
    }

    fun append(formattedText: FormattedText, style: List<FormattingTextContentStyle>) {
        formattedText.content.map { it.string to it.style.toMutableList() }.onEach { it.second.addAll(style) }.forEach { append(it.first, it.second) }
    }

    fun append(formattedTextContent: FormattedTextContent) {
        content.add(formattedTextContent)
    }

    fun append(string: String) {
        content.add(FormattedTextContent(string))
    }

    fun append(string: String, vararg style: FormattingTextContentStyle) {
        append(string, style.toList())
    }

    fun append(string: String, style: List<FormattingTextContentStyle>) {
        content.add(FormattedTextContent(string, style))
    }

    fun appendLineBreak(count: Int = 1) {
        append("\n".repeat(count))
    }

    fun appendInlineContent(image: InlineImage, alternativeText: String) {
        append(alternativeText, InlineImageStyle(image))
    }

    fun build(): FormattedText {
        return FormattedText(content)
    }

}

inline fun buildFormattedString(builder: (FormattedTextBuilder).() -> Unit): FormattedText = FormattedTextBuilder().apply(builder).build()

data class FormattedTextContent(
    val string: String,
    val style: List<FormattingTextContentStyle> = emptyList()
)

abstract class FormattingTextContentStyle(
    private val identifier: String
) : JSONSerializable, IOSerializable {

    companion object {

        fun deserialize(json: JsonObject): FormattingTextContentStyle {
            return when (val type = json.optString("type")) {
                "small" -> SmallContentStyle
                "big" -> BigContentStyle
                "bold" -> BoldContentStyle
                "color" -> ColorContentStyle.deserializeValues(json.optJsonObject("values")!!)
                "inlineImage" -> InlineImageStyle.deserializeValues(json.optJsonObject("values")!!)
                else -> throw RuntimeException("Unknown style type $type")
            }
        }

        suspend fun deserialize(input: ByteReadChannel): FormattingTextContentStyle {
            return when (val type = input.readString(UTF_8)) {
                "small" -> SmallContentStyle
                "big" -> BigContentStyle
                "bold" -> BoldContentStyle
                "color" -> ColorContentStyle.deserializeValues(input)
                "inlineImage" -> InlineImageStyle.deserializeValues(input)
                else -> throw RuntimeException("Unknown style type $type")
            }
        }
    }

    internal open fun serializeValues(): JsonObject? = null

    internal open suspend fun serializeValues(out: ByteWriteChannel) { }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("type", identifier)
            serializeValues()?.let { put("values", it) }
        }
    }

    override suspend fun serialize(out: ByteWriteChannel) {
        out.writeString(identifier, UTF_8)
        serializeValues(out)
    }

}

object SmallContentStyle : FormattingTextContentStyle("small")
object BigContentStyle : FormattingTextContentStyle("big")
object BoldContentStyle : FormattingTextContentStyle("bold")
data class ColorContentStyle(val color: Long) : FormattingTextContentStyle("color") {

    companion object {

        fun deserializeValues(json: JsonObject): ColorContentStyle {
            return ColorContentStyle(json.optLong("color"))
        }

        suspend fun deserializeValues(input: ByteReadChannel): ColorContentStyle {
            return ColorContentStyle(input.readLong())
        }
    }

    override fun serializeValues(): JsonObject = buildJsonObject { put("color", color) }

    override suspend fun serializeValues(out: ByteWriteChannel) = out.writeLong(color)

}
data class InlineImageStyle(val image: InlineImage): FormattingTextContentStyle("inlineImage") {

    companion object {

        fun deserializeValues(json: JsonObject): InlineImageStyle {
            return InlineImageStyle(InlineImage.valueOf(json.optString("image")))
        }

        suspend fun deserializeValues(input: ByteReadChannel): InlineImageStyle {
            return InlineImageStyle(InlineImage.valueOf(input.readString(UTF_8)))
        }
    }

    override fun serializeValues(): JsonObject = buildJsonObject { put("image", image.name) }

    override suspend fun serializeValues(out: ByteWriteChannel) = out.writeString(image.name, UTF_8)

}

enum class InlineImage {

    LRV, LRV_EMPTY

}

val SmallSize: SmallContentStyle = SmallContentStyle
val BigSize: BigContentStyle = BigContentStyle
val BoldStyle: BoldContentStyle = BoldContentStyle
fun Colored(color: Long): ColorContentStyle = ColorContentStyle(color)

fun String.asFormattedText(vararg style: FormattingTextContentStyle): FormattedText {
    return asFormattedText(style.toList())
}

fun String.asFormattedText(style: List<FormattingTextContentStyle>): FormattedText {
    return buildFormattedString { append(this@asFormattedText, style.toList()) }
}