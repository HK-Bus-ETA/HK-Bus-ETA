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
import io.ktor.utils.io.charsets.Charsets.UTF_8
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readLong
import kotlinx.io.Sink
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put


@Immutable
open class FormattedText(
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

    constructor(formattedText: FormattedText): this(formattedText.content)

    val string: String by lazy { content.joinToString("") { it.string } }

    override val length: Int by lazy { content.sumOf { it.string.length } }

    operator fun plus(other: FormattedText): FormattedText {
        return buildFormattedString { append(this@FormattedText); append(other) }
    }

    operator fun plus(other: String): FormattedText {
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

    fun trim(): FormattedText {
        return when (content.size) {
            0 -> this
            1 -> {
                val first = content[0]
                FormattedText(listOf(element = FormattedTextContent(first.string.trim(), first.style)))
            }
            else -> {
                val newContent = content.toMutableList()
                val first = newContent[0]
                val end = newContent[newContent.lastIndex]
                val firstFormatted = FormattedTextContent(first.string.trimStart(), first.style)
                val lastFormatted = FormattedTextContent(end.string.trimEnd(), end.style)
                newContent[0] = firstFormatted
                newContent[newContent.lastIndex] = lastFormatted
                FormattedText(newContent)
            }
        }
    }

    fun trim(predicate: (Char) -> Boolean): FormattedText {
        return when (content.size) {
            0 -> this
            1 -> {
                val first = content[0]
                FormattedText(listOf(element = FormattedTextContent(first.string.trim(predicate), first.style)))
            }
            else -> {
                val newContent = content.toMutableList()
                val first = newContent[0]
                val end = newContent[newContent.lastIndex]
                val firstFormatted = FormattedTextContent(first.string.trimStart(predicate), first.style)
                val lastFormatted = FormattedTextContent(end.string.trimEnd(predicate), end.style)
                newContent[0] = firstFormatted
                newContent[newContent.lastIndex] = lastFormatted
                FormattedText(newContent)
            }
        }
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

    override fun serialize(out: Sink) {
        out.writeInt(content.size)
        for (formattedTextContent in content) {
            out.writeString(formattedTextContent.string, UTF_8)
            out.writeInt(formattedTextContent.style.size)
            for (style in formattedTextContent.style) {
                style.serialize(out)
            }
        }
    }

    override fun toString(): String {
        return string
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FormattedText) return false

        return content == other.content
    }

    override fun hashCode(): Int {
        return content.hashCode()
    }

}

inline infix fun FormattedText.strEq(other: String): Boolean {
    return string == other
}

private val urlRegex: Regex = "<a +href=\\\"([^\\\"]*)\\\">([^>]*)<\\/a>|((?:https?:\\/\\/)?(?:www.)?[a-z0-9-]+\\.[a-z]+[0-9a-zA-Z&=+\\-\$~`*@|?\\\\#_%/,.:;]*)".toRegex()

class FormattedTextBuilder(
    private val extractUrls: Boolean
): CharSequence {

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
        append(string, emptyList())
    }

    fun append(string: String, vararg style: FormattingTextContentStyle) {
        append(string, style.asList())
    }

    fun append(string: String, style: List<FormattingTextContentStyle>) {
        val styleImmutable = style.toList()
        if (extractUrls) {
            val matcher = urlRegex.findAll(string)
            var lastPosition = 0
            for (match in matcher) {
                val start = match.range.first
                val displayText = match.groupValues[2].takeIf { it.isNotEmpty() }?: match.groupValues[3]
                val url = match.groupValues[1].takeIf { it.isNotEmpty() }?: displayText
                content.add(FormattedTextContent(string.substring(lastPosition, start), styleImmutable))
                lastPosition = match.range.last + 1
                content.add(FormattedTextContent(displayText, styleImmutable.toMutableList().apply {
                    add(URLContentStyle(url))
                    if (style.none { it is ColorContentStyle }) {
                        add(ColorContentStyle(0xFF009DFF))
                    }
                }))
            }
            if (lastPosition < string.length) {
                content.add(FormattedTextContent(string.substring(lastPosition), styleImmutable))
            }
        } else {
            content.add(FormattedTextContent(string, styleImmutable))
        }
    }

    fun appendLineBreak(count: Int = 1) {
        append("\n".repeat(count))
    }

    fun appendInlineContent(image: InlineImage, alternativeText: String) {
        append(alternativeText, InlineImageStyle(image))
    }

    override val length: Int get() = content.sumOf { it.string.length }

    override fun get(index: Int): Char {
        return content.asSequence()
            .flatMap { it.string.asSequence() }
            .drop(index)
            .first()
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return content.asSequence()
            .flatMap { it.string.asSequence() }
            .drop(startIndex)
            .take(endIndex - startIndex)
            .joinToString()
    }

    fun build(): FormattedText {
        return FormattedText(content)
    }

}

inline fun FormattedText.withStyle(vararg style: FormattingTextContentStyle): FormattedText {
    return withStyle(style.asList())
}

inline fun FormattedText.withStyle(style: List<FormattingTextContentStyle>): FormattedText {
    return buildFormattedString { append(this@withStyle, style) }
}

inline fun buildFormattedString(
    extractUrls: Boolean = false,
    builder: (FormattedTextBuilder).() -> Unit
): FormattedText = FormattedTextBuilder(extractUrls).apply(builder).build()

data class FormattedTextContent(
    val string: String,
    val style: List<FormattingTextContentStyle> = emptyList()
)

sealed class FormattingTextContentStyle(
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
                "url" -> URLContentStyle.deserializeValues(json.optJsonObject("values")!!)
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
                "url" -> URLContentStyle.deserializeValues(input)
                else -> throw RuntimeException("Unknown style type $type")
            }
        }
    }

    internal open fun serializeValues(): JsonObject? = null

    internal open fun serializeValues(out: Sink) { }

    override fun serialize(): JsonObject = buildJsonObject {
        put("type", identifier)
        serializeValues()?.let { put("values", it) }
    }

    override fun serialize(out: Sink) {
        out.writeString(identifier, UTF_8)
        serializeValues(out)
    }

}

object SmallContentStyle : FormattingTextContentStyle("small")
object BigContentStyle : FormattingTextContentStyle("big")
object BoldContentStyle : FormattingTextContentStyle("bold")
data class ColorContentStyle(
    val color: Long,
    val darkThemeColor: Long = color
) : FormattingTextContentStyle("color") {

    companion object {

        fun deserializeValues(json: JsonObject): ColorContentStyle {
            return ColorContentStyle(json.optLong("color"), json.optLong("darkThemeColor"))
        }

        suspend fun deserializeValues(input: ByteReadChannel): ColorContentStyle {
            return ColorContentStyle(input.readLong(), input.readLong())
        }
    }

    fun color(isDarkTheme: Boolean): Long = if (isDarkTheme) darkThemeColor else color

    override fun serializeValues(): JsonObject = buildJsonObject {
        put("color", color)
        put("darkThemeColor", darkThemeColor)
    }

    override fun serializeValues(out: Sink) {
        out.writeLong(color)
        out.writeLong(darkThemeColor)
    }

}
data class InlineImageStyle(
    val image: InlineImage
): FormattingTextContentStyle("inlineImage") {

    companion object {

        fun deserializeValues(json: JsonObject): InlineImageStyle {
            return InlineImageStyle(InlineImage.valueOf(json.optString("image")))
        }

        suspend fun deserializeValues(input: ByteReadChannel): InlineImageStyle {
            return InlineImageStyle(InlineImage.valueOf(input.readString(UTF_8)))
        }
    }

    override fun serializeValues(): JsonObject = buildJsonObject {
        put("image", image.name)
    }

    override fun serializeValues(out: Sink) {
        out.writeString(image.name, UTF_8)
    }

}
data class URLContentStyle(
    val url: String
) : FormattingTextContentStyle("url") {

    companion object {

        fun deserializeValues(json: JsonObject): URLContentStyle {
            return URLContentStyle(json.optString("url"))
        }

        suspend fun deserializeValues(input: ByteReadChannel): URLContentStyle {
            return URLContentStyle(input.readString(UTF_8))
        }
    }

    override fun serializeValues(): JsonObject = buildJsonObject {
        put("url", url)
    }

    override fun serializeValues(out: Sink) {
        out.writeString(url, UTF_8)
    }

}

enum class InlineImage {

    LRV, LRV_EMPTY

}

val SmallSize: SmallContentStyle = SmallContentStyle
val BigSize: BigContentStyle = BigContentStyle
val BoldStyle: BoldContentStyle = BoldContentStyle
fun Colored(color: Long, darkThemeColor: Long = color): ColorContentStyle = ColorContentStyle(color, darkThemeColor)
fun URLLinked(url: String): URLContentStyle = URLContentStyle(url)

val EMPTY_FORMATTED_TEXT = buildFormattedString { append("") }

fun String.asFormattedText(vararg style: FormattingTextContentStyle): FormattedText {
    return asFormattedText(style.asList())
}

fun String.asFormattedText(style: List<FormattingTextContentStyle>): FormattedText {
    if (isEmpty() && style.isEmpty()) return EMPTY_FORMATTED_TEXT
    return buildFormattedString { append(this@asFormattedText, style.toList()) }
}

fun FormattedText.transformColors(map: (String, ColorContentStyle) -> ColorContentStyle): FormattedText {
    return buildFormattedString {
        for (c in content) {
            append(c.string, c.style.map { if (it is ColorContentStyle) map.invoke(c.string, it) else it })
        }
    }
}