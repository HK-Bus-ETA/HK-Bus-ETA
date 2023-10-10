package com.loohp.hkbuseta.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import com.aghajari.compose.text.ContentAnnotatedString
import com.aghajari.compose.text.InlineContent

data class CharacterData(val style: List<AnnotatedString.Range<SpanStyle>>, val inline: List<InlineContent>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharacterData

        if (style != other.style) return false
        if (inline != other.inline) return false

        return true
    }

    override fun hashCode(): Int {
        var result = style.hashCode()
        result = 31 * result + inline.hashCode()
        return result
    }
}

fun LayoutElementBuilders.Spannable.Builder.addContentAnnotatedString(contentAnnotatedString: ContentAnnotatedString, defaultFontStyle: LayoutElementBuilders.FontStyle? = null): LayoutElementBuilders.Spannable.Builder {
    if (contentAnnotatedString.isEmpty()) {
        val span = LayoutElementBuilders.SpanText.Builder().setText("")
        if (defaultFontStyle != null) {
            span.setFontStyle(defaultFontStyle)
        }
        return this.addSpan(span.build())
    }
    val text = contentAnnotatedString.annotatedString.text
    val style = contentAnnotatedString.annotatedString.spanStyles
    val inlineContent = contentAnnotatedString.inlineContents
    val characterData: MutableList<Pair<Char, CharacterData>> = ArrayList(text.length)
    for ((i, c) in text.withIndex()) {
        val s = style.filter { i >= it.start && i < it.end }.toList()
        val n = inlineContent.filter { i >= it.start && i < it.end }.toList()
        characterData.add(c to CharacterData(s, n))
    }
    val mergedData: MutableList<Pair<String, CharacterData>> = ArrayList()
    var currentString: StringBuilder = StringBuilder().append(characterData[0].first)
    var currentData: CharacterData = characterData[0].second
    for ((c, d) in characterData.drop(1)) {
        if (d == currentData) {
            currentString.append(c)
        } else {
            mergedData.add(currentString.toString() to currentData)
            currentString = StringBuilder().append(c)
            currentData = d
        }
    }
    if (currentString.isNotEmpty()) {
        mergedData.add(currentString.toString() to currentData)
    }
    for ((s, d) in mergedData) {
        val fontStyleBuilder = LayoutElementBuilders.FontStyle.Builder()
        if (defaultFontStyle != null) {
            defaultFontStyle.size?.let { fontStyleBuilder.setSize(DimensionBuilders.sp(it.value)) }
            defaultFontStyle.italic?.let { fontStyleBuilder.setItalic(it.value) }
            defaultFontStyle.underline?.let { fontStyleBuilder.setUnderline(it.value) }
            defaultFontStyle.color?.let { fontStyleBuilder.setColor(ColorBuilders.ColorProp.Builder(it.argb).build()) }
            defaultFontStyle.weight?.let { fontStyleBuilder.setWeight(it.value) }
            defaultFontStyle.letterSpacing?.let { fontStyleBuilder.setLetterSpacing(DimensionBuilders.em(it.value)) }
        }
        d.style.forEach {
            val spanStyle = it.item
            if (spanStyle.fontSize != TextUnit.Unspecified) fontStyleBuilder.setSize(DimensionBuilders.sp(if (spanStyle.fontSize.isEm) (spanStyle.fontSize.value * 14F) else spanStyle.fontSize.value))
            spanStyle.fontStyle?.let { fontStyle -> if (fontStyle == FontStyle.Italic) fontStyleBuilder.setItalic(true) }
            spanStyle.textDecoration?.let { textDecoration -> if (textDecoration.contains(TextDecoration.Underline)) fontStyleBuilder.setUnderline(true) }
            if (spanStyle.color != Color.Unspecified) fontStyleBuilder.setColor(ColorBuilders.ColorProp.Builder(spanStyle.color.toArgb()).build())
            spanStyle.fontWeight?.let { fontWeight -> fontStyleBuilder.setWeight(fontWeight.weight) }
            if (spanStyle.letterSpacing != TextUnit.Unspecified) fontStyleBuilder.setLetterSpacing(DimensionBuilders.em(spanStyle.letterSpacing.value))
        }
        this.addSpan(LayoutElementBuilders.SpanText.Builder().setText(s).setFontStyle(fontStyleBuilder.build()).build())
    }
    return this
}