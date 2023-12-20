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

package com.loohp.hkbuseta.common.utils

data class FormattedText(
    val content: MutableList<FormattedTextContent>
) : CharSequence {

    val string: String = content.joinToString { it.string }

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

interface FormattingTextContentStyle

object SmallContentStyle : FormattingTextContentStyle
object BigContentStyle : FormattingTextContentStyle
object BoldContentStyle : FormattingTextContentStyle
data class ColorContentStyle(val color: Long) : FormattingTextContentStyle
data class InlineImageStyle(val image: InlineImage): FormattingTextContentStyle

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