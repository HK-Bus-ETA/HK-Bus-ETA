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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Html
import android.text.Layout
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.core.text.HtmlCompat
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import com.aghajari.compose.text.ContentAnnotatedString
import com.aghajari.compose.text.InlineContent
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


@Immutable
class ResourceImageGetter(private val context: Context, private val heightSp: Float) : Html.ImageGetter {

    companion object {

        private val types = listOf("mipmap", "drawable", "raw")

    }

    private val height = heightSp.spToPixels(context)

    @SuppressLint("DiscouragedApi")
    override fun getDrawable(source: String): Drawable? {
        return types.asSequence()
            .map { context.resources.getIdentifier(source, it, context.packageName) }.filter { it != 0 }
            .firstOrNull()?.let outer@ {
                val drawable = ResourcesCompat.getDrawable(context.resources, it, null)
                drawable?.let {
                    if (drawable.intrinsicHeight >= 0) {
                        val height = height.roundToInt()
                        val width = ((drawable.intrinsicWidth / drawable.intrinsicHeight.toFloat()) * height).roundToInt()
                        return@outer drawable.toBitmap().scale(width, height).toDrawable(context.resources)
                    }
                }
                return@outer drawable
            }
    }

}

fun CharSequence.toSpanned(context: Context, imageHeightSp: Float = 0F): Spanned {
    return if (imageHeightSp <= 0F) {
        HtmlCompat.fromHtml(this.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT)
    } else {
        HtmlCompat.fromHtml(this.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT, ResourceImageGetter(context, imageHeightSp), null)
    }
}


data class CharacterData(val style: List<AnnotatedString.Range<SpanStyle>>, val inline: List<InlineContent>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharacterData

        if (style != other.style) return false
        return inline == other.inline
    }

    override fun hashCode(): Int {
        var result = style.hashCode()
        result = 31 * result + inline.hashCode()
        return result
    }
}

private val layoutFontValues = setOf(LayoutElementBuilders.FONT_WEIGHT_NORMAL, LayoutElementBuilders.FONT_WEIGHT_BOLD)

fun FontWeight?.snapToClosestLayoutWeight(): Int {
    if (this == null) {
        return LayoutElementBuilders.FONT_WEIGHT_NORMAL
    }
    val weight = this.weight.coerceIn(1, 1000)
    return layoutFontValues.minBy { (it - weight).absoluteValue }
}

fun LayoutElementBuilders.Spannable.Builder.addContentAnnotatedString(context: Context, contentAnnotatedString: ContentAnnotatedString, defaultFontSp: Float, defaultFontStyle: (LayoutElementBuilders.FontStyle.Builder) -> Unit = {}, inlineImageHandler: ((ByteArray, Int, Int) -> String)? = null): LayoutElementBuilders.Spannable.Builder {
    if (contentAnnotatedString.isEmpty()) {
        val span = LayoutElementBuilders.SpanText.Builder().setText("")
        val fontStyleBuilder = LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(defaultFontSp))
        defaultFontStyle.invoke(fontStyleBuilder)
        return this.addSpan(span.setFontStyle(fontStyleBuilder.build()).build())
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
    var currentLength = 0
    var jumpTo = 0
    for ((s, d) in mergedData) {
        val str = s.substring((jumpTo - currentLength).coerceAtLeast(0))
        val fontStyleBuilder = LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(defaultFontSp))
        defaultFontStyle.invoke(fontStyleBuilder)
        d.style.forEach {
            val spanStyle = it.item
            if (spanStyle.fontSize != TextUnit.Unspecified) fontStyleBuilder.setSize(DimensionBuilders.sp(if (spanStyle.fontSize.isEm) (spanStyle.fontSize.value * defaultFontSp) else spanStyle.fontSize.value))
            spanStyle.fontStyle?.let { fontStyle -> if (fontStyle == FontStyle.Italic) fontStyleBuilder.setItalic(true) }
            spanStyle.textDecoration?.let { textDecoration -> if (textDecoration.contains(TextDecoration.Underline)) fontStyleBuilder.setUnderline(true) }
            if (spanStyle.color != Color.Unspecified) fontStyleBuilder.setColor(ColorBuilders.ColorProp.Builder(spanStyle.color.toArgb()).build())
            spanStyle.fontWeight?.let { fontWeight -> fontStyleBuilder.setWeight(fontWeight.snapToClosestLayoutWeight()) }
            if (spanStyle.letterSpacing != TextUnit.Unspecified) fontStyleBuilder.setLetterSpacing(DimensionBuilders.em(spanStyle.letterSpacing.value))
        }
        val fontStyle = fontStyleBuilder.build()
        val imageEndPos = if (inlineImageHandler != null) {
            d.inline.filter { it.span is ImageSpan }.minByOrNull { it.start }?.let {
                val span = it.span as ImageSpan
                val bitmap = span.drawable.toBitmap()
                val data = bitmap.compressToByteArray(Bitmap.CompressFormat.PNG, 100)
                val key = inlineImageHandler.invoke(data, bitmap.width, bitmap.height)
                LayoutElementBuilders.SPAN_VERTICAL_ALIGN_TEXT_BASELINE
                this.addSpan(LayoutElementBuilders.SpanImage.Builder()
                    .setResourceId(key)
                    .setAlignment(when (span.verticalAlignment) {
                        DynamicDrawableSpan.ALIGN_BOTTOM -> LayoutElementBuilders.SPAN_VERTICAL_ALIGN_BOTTOM
                        DynamicDrawableSpan.ALIGN_BASELINE -> LayoutElementBuilders.SPAN_VERTICAL_ALIGN_TEXT_BASELINE
                        else -> LayoutElementBuilders.SPAN_VERTICAL_ALIGN_UNDEFINED
                    })
                    .setWidth(DimensionBuilders.dp(bitmap.width.toFloat().pixelsToDp(context)))
                    .setHeight(DimensionBuilders.dp(bitmap.height.toFloat().pixelsToDp(context)))
                    .build()
                )
                it.end
            }?: -1
        } else {
            -1
        }
        if (imageEndPos >= 0) {
            jumpTo = imageEndPos
        } else {
            this.addSpan(LayoutElementBuilders.SpanText.Builder().setText(str).setFontStyle(fontStyle).build())
        }
        currentLength += text.length
    }
    return this
}

fun CharSequence.eitherContains(other: CharSequence): Boolean {
    return this.contains(other) || other.contains(this)
}

fun String.editDistance(other: String): Int {
    if (this == other) {
        return 0
    }
    val dp = Array(this.length + 1) {
        IntArray(
            other.length + 1
        )
    }
    for (i in 0..this.length) {
        for (j in 0..other.length) {
            if (i == 0) {
                dp[i][j] = j
            } else if (j == 0) {
                dp[i][j] = i
            } else {
                dp[i][j] = min(
                    dp[i - 1][j - 1] + costOfSubstitution(this[i - 1], other[j - 1]),
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1
                )
            }
        }
    }
    return dp[this.length][other.length]
}

private fun costOfSubstitution(a: Char, b: Char): Int {
    return if (a == b) 0 else 1
}

private fun min(vararg numbers: Int): Int {
    return numbers.minOrNull()?: Int.MAX_VALUE
}

fun Int.getCircledNumber(): String {
    if (this < 0 || this > 20) {
        return this.toString()
    }
    if (this == 0) {
        return "⓿"
    }
    return if (this > 10) {
        (9451 + (this - 11)).toChar().toString()
    } else (10102 + (this - 1)).toChar().toString()
}

fun Int.getHollowCircledNumber(): String {
    if (this < 0 || this > 10) {
        return this.toString()
    }
    return if (this == 0) {
        "⓪"
    } else (9312 + (this - 1)).toChar().toString()
}


fun Int.scaledSize(context: Context): Float {
    return this * ScreenSizeUtils.getScreenScale(context)
}

fun Float.scaledSize(context: Context): Float {
    return this * ScreenSizeUtils.getScreenScale(context)
}

fun String.findOptimalSp(context: Context, targetWidth: Int, maxLines: Int, minSp: Float, maxSp: Float): Float {
    val paint = TextPaint()
    paint.density = context.resources.displayMetrics.density
    var sp = maxSp
    while (sp >= minSp) {
        paint.textSize = sp.spToPixels(context)
        paint.setTypeface(Typeface.DEFAULT)
        val staticLayout = StaticLayout.Builder.obtain(this, 0, length, paint, targetWidth)
            .setMaxLines(Int.MAX_VALUE)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        if (staticLayout.lineCount <= maxLines) {
            return sp
        }
        sp -= 0.5f
    }
    return minSp
}

fun String.findTextLengthDp(context: Context, sp: Float): Float {
    val textView = TextView(context)
    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    return textView.paint.measureText(this).pixelsToDp(context)
}