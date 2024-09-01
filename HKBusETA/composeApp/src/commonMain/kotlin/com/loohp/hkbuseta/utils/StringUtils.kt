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

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.BigContentStyle
import com.loohp.hkbuseta.common.utils.BoldContentStyle
import com.loohp.hkbuseta.common.utils.ColorContentStyle
import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.common.utils.FormattingTextContentStyle
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.InlineImage
import com.loohp.hkbuseta.common.utils.InlineImageStyle
import com.loohp.hkbuseta.common.utils.SmallContentStyle
import com.loohp.hkbuseta.common.utils.URLContentStyle
import com.loohp.hkbuseta.common.utils.asImmutableMap
import com.loohp.hkbuseta.compose.PlatformText
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.compose.resources.painterResource

val RESOURCE_RATIO: Map<String, Float> = mapOf(
    "lrv" to 128F / 95F,
    "lrv_empty" to 128F / 95F
)

@Immutable
data class ContentAnnotatedString(val annotatedString: AnnotatedString, val inlineResources: Map<String, String>) : CharSequence {

    fun createInlineContent(imageHeight: TextUnit = 1F.em): Map<String, InlineTextContent> {
        return inlineResources.mapValues { (id, resource) ->
            InlineTextContent(
                placeholder = Placeholder(
                    width = imageHeight * (RESOURCE_RATIO[resource]?: 1F),
                    height = imageHeight,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom
                ),
                children = {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(DrawableResource(resource)),
                        contentDescription = id
                    )
                }
            )
        }
    }

    override val length: Int = annotatedString.length

    override fun get(index: Int): Char {
        return annotatedString[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return annotatedString.subSequence(startIndex, endIndex)
    }

    operator fun plus(other: ContentAnnotatedString): ContentAnnotatedString {
        val map = mutableMapOf<String, String>()
        map.putAll(inlineResources)
        map.putAll(other.inlineResources)
        return ContentAnnotatedString(annotatedString + other.annotatedString, map)
    }

}

fun AnnotatedString.asContentAnnotatedString(inlineResources: Map<String, String> = mapOf()): ContentAnnotatedString {
    return ContentAnnotatedString(this, inlineResources)
}

fun String.asContentAnnotatedString(spanStyle: SpanStyle? = null, inlineResources: Map<String, String> = mapOf()): ContentAnnotatedString {
    return ContentAnnotatedString(this.asAnnotatedString(spanStyle), inlineResources)
}

inline val TextUnit.Companion.Small: TextUnit get() = 0.8F.em

inline val TextUnit.Companion.Big: TextUnit get() = 1.25F.em

fun String.asAnnotatedString(spanStyle: SpanStyle? = null): AnnotatedString {
    return spanStyle?.let { buildAnnotatedString { append(this@asAnnotatedString, spanStyle) } }?: AnnotatedString(this)
}

inline fun AnnotatedString.Builder.append(string: String, spanStyle: SpanStyle) {
    val start = length
    append(string)
    addStyle(spanStyle, start, length)
}

@Composable
inline fun AnnotatedString.Builder.appendFormatted(string: FormattedText, spanStyle: SpanStyle? = null) {
    append(string.asContentAnnotatedString(spanStyle).annotatedString)
}

fun Collection<FormattingTextContentStyle>.toSpanStyles(isDarkTheme: Boolean, spanStyle: SpanStyle? = null): SpanStyle {
    return (spanStyle?: SpanStyle()).copy(
        fontSize = if (any { it is SmallContentStyle }) TextUnit.Small else if (any { it is BigContentStyle }) TextUnit.Big else (spanStyle?.fontSize?: TextUnit.Unspecified),
        color = firstOrNull { it is ColorContentStyle }?.let { Color((it as ColorContentStyle).color(isDarkTheme)) }?: (spanStyle?.color?: Color.Unspecified),
        fontWeight = if (any { it is BoldContentStyle }) FontWeight.Bold else spanStyle?.fontWeight,
    )
}

val InlineImage.resource: String get() = when (this) {
    InlineImage.LRV -> "lrv.png"
    InlineImage.LRV_EMPTY -> "lrv_empty.png"
}

@Composable
fun FormattedText.asContentAnnotatedString(spanStyle: SpanStyle? = null): ContentAnnotatedString {
    return asContentAnnotatedString(Shared.theme.isDarkMode, spanStyle)
}

fun FormattedText.asContentAnnotatedString(isDarkTheme: Boolean, spanStyle: SpanStyle? = null): ContentAnnotatedString {
    val inlineContents: MutableMap<String, String> = mutableMapOf()
    return buildAnnotatedString {
        content.forEach { (string, style) ->
            val url = style.firstNotNullOfOrNull { it as? URLContentStyle }
            url?.let { pushStringAnnotation("url", it.url) }
            style.firstNotNullOfOrNull { it as? InlineImageStyle }?.let {
                inlineContents[it.image.name] = it.image.resource
                appendInlineContent(it.image.name, string)
            }?: append(string, style.toSpanStyles(isDarkTheme, spanStyle))
            url?.let { pop() }
        }
    }.asContentAnnotatedString(inlineContents)
}

@Composable
fun ImmutableCollection<String>.renderedSizes(fontSize: TextUnit, maxLines: Int = 1, spanStyle: SpanStyle? = null): State<ImmutableMap<String, IntSize?>> {
    val result: MutableState<ImmutableMap<String, IntSize?>> = remember { mutableStateOf(persistentMapOf()) }
    LaunchedEffect (this) {
        result.value = result.value.toMutableMap().apply { keys.retainAll(this@renderedSizes) }.asImmutableMap()
    }
    for (string in this) {
        val stringResult by string.renderedSize(fontSize, maxLines, spanStyle)
        LaunchedEffect (string, stringResult) {
            result.value = result.value.toMutableMap().apply { this[string] = stringResult }.asImmutableMap()
        }
    }
    return result
}

@Composable
fun ImmutableCollection<AnnotatedString>.renderedSizes(fontSize: TextUnit, maxLines: Int = 1): State<ImmutableMap<AnnotatedString, IntSize?>> {
    val result: MutableState<ImmutableMap<AnnotatedString, IntSize?>> = remember { mutableStateOf(persistentMapOf()) }
    LaunchedEffect (this) {
        result.value = result.value.toMutableMap().apply { keys.retainAll(this@renderedSizes) }.asImmutableMap()
    }
    for (string in this) {
        val stringResult by string.renderedSize(fontSize, maxLines)
        LaunchedEffect (string, stringResult) {
            result.value = result.value.toMutableMap().apply { this[string] = stringResult }.asImmutableMap()
        }
    }
    return result
}

@Composable
fun String.renderedSize(fontSize: TextUnit, maxLines: Int = 1, spanStyle: SpanStyle? = null): State<IntSize?> {
    return asAnnotatedString(spanStyle).renderedSize(fontSize, maxLines)
}

@Composable
fun AnnotatedString.renderedSize(fontSize: TextUnit, maxLines: Int = 1): State<IntSize?> {
    val result: MutableState<IntSize?> = remember(this, fontSize) { mutableStateOf(null) }
    val density = LocalDensity.current
    var first by remember { mutableStateOf(true) }
    LaunchedEffect (density.density, density.fontScale) {
        if (first) {
            first = false
        } else {
            result.value = null
        }
    }
    if (result.value == null) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            PlatformText(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    result.value = IntSize(placeable.width, placeable.height)
                    layout(0, 0) { /* do nothing */ }
                },
                text = this@renderedSize,
                maxLines = maxLines,
                fontSize = fontSize,
                overflow = TextOverflow.Visible
            )
        }
    }
    return result
}

fun <T> Iterable<T>.joinToAnnotatedString(separator: AnnotatedString = ", ".asAnnotatedString(), prefix: AnnotatedString = "".asAnnotatedString(), postfix: AnnotatedString = "".asAnnotatedString(), limit: Int = -1, truncated: AnnotatedString = "...".asAnnotatedString(), transform: ((T) -> AnnotatedString)? = null): AnnotatedString {
    return AnnotatedString.Builder().apply {
        append(prefix)
        var count = 0
        for (element in this@joinToAnnotatedString) {
            if (++count > 1) append(separator)
            if (limit < 0 || count <= limit) {
                when {
                    transform != null -> append(transform.invoke(element))
                    element is AnnotatedString -> append(element)
                    element is CharSequence? -> append(element)
                    element is Char -> append(element)
                    else -> append(element.toString())
                }
            } else break
        }
        if (limit in 0 until count) {
            append(truncated)
        }
        append(postfix)
    }.toAnnotatedString()
}