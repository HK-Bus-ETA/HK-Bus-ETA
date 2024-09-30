/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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

package com.loohp.hkbuseta.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.utils.asAnnotatedString
import kotlinx.coroutines.delay


@Composable
fun TextInputDialog(
    title: BilingualText,
    confirmText: BilingualText,
    initialText: String? = null,
    placeholder: BilingualText? = null,
    inputValidation: (String) -> Boolean = { true },
    onDismissRequest: (String, DismissRequestType) -> Unit,
    onConfirmation: (String) -> Unit
) {
    var textInput by remember { mutableStateOf(TextFieldValue(
        text = initialText?: "",
        selection = TextRange(initialText?.length?: 0)
    )) }
    val inputValid by remember { derivedStateOf { inputValidation.invoke(textInput.text) } }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect (Unit) {
        delay(200)
        focusRequester.requestFocus()
    }
    PlatformAlertDialog(
        title = {
            PlatformText(text = title[Shared.language], textAlign = TextAlign.Center)
        },
        text = {
            PlatformOutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = textInput,
                singleLine = true,
                onValueChange = { textInput = it },
                placeholder = placeholder?.let { {
                    PlatformText(
                        text = placeholder[Shared.language]
                    )
                } },
                isError = !inputValid
            )
        },
        onDismissRequest = { onDismissRequest.invoke(textInput.text, it) },
        confirmButton = {
            PlatformText(
                text = confirmText[Shared.language]
            )
        },
        confirmEnabled = inputValid,
        onConfirm = { onConfirmation.invoke(textInput.text) },
        dismissButton = {
            PlatformText(
                text = if (Shared.language == "en") "Cancel" else "取消"
            )
        }
    )
}

@Composable
fun AutoResizeText(
    text: String,
    fontSizeRange: FontSizeRange,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    preferSingleLine: Boolean = maxLines <= 1,
    style: TextStyle = platformLocalTextStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    AutoResizeText(
        text = text.asAnnotatedString(),
        modifier = modifier,
        color = color,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style,
        autoResizeTextState = rememberAutoResizeFontState(
            fontSizeRange = fontSizeRange,
            preferSingleLine = preferSingleLine
        ),
        onTextLayout = onTextLayout,
    )
}

@Composable
fun AutoResizeText(
    text: AnnotatedString,
    fontSizeRange: FontSizeRange,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    preferSingleLine: Boolean = maxLines <= 1,
    style: TextStyle = platformLocalTextStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    AutoResizeText(
        text = text,
        modifier = modifier,
        color = color,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style,
        autoResizeTextState = rememberAutoResizeFontState(
            fontSizeRange = fontSizeRange,
            preferSingleLine = preferSingleLine
        ),
        onTextLayout = onTextLayout,
    )
}

@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = platformLocalTextStyle,
    autoResizeTextState: MutableState<AutoResizeFontState>,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    AutoResizeText(
        text = text.asAnnotatedString(),
        modifier = modifier,
        color = color,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style,
        autoResizeTextState = autoResizeTextState,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun AutoResizeText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = platformLocalTextStyle,
    autoResizeTextState: MutableState<AutoResizeFontState>,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    val id = remember { uuid4() }
    var state by autoResizeTextState
    val density = LocalDensity.current
    val windowSize = currentLocalWindowSize
    var textRefresh by remember { mutableStateOf(true) }
    var lastTextLayoutResult: TextLayoutResult? by remember { mutableStateOf(null) }
    var first = remember { false }

    ImmediateEffect (Unit) {
        state = state.subscribe(id)
    }
    DisposableEffect (Unit) {
        onDispose { state = state.unsubscribe(id) }
    }

    LaunchedEffect (state.readyToDraw, lastTextLayoutResult, state.fontSizeValue) {
        if (state.readyToDraw) {
            lastTextLayoutResult?.apply {
                onTextLayout?.invoke(this)
            }
        }
    }
    LaunchedEffect (density.fontScale, density.density, windowSize, text, state.fontSizeRange) {
        if (state.readyToDraw(id) && textRefresh) {
            if (first) {
                first = false
            } else {
                state = state.setFontSizeValue(id, null).setReadyToDraw(id, false)
                textRefresh = false
            }
        }
    }
    LaunchedEffect (textRefresh) {
        if (!textRefresh) {
            textRefresh = true
        }
    }

    if (textRefresh) {
        PlatformText(
            text = text,
            color = color,
            maxLines = if (!state.readyToDraw && state.preferSingleLine) 1 else maxLines,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            style = style,
            fontSize = (if (state.readyToDraw(id)) state.fontSizeValue else state.fontSizeValue(id)).sp,
            onTextLayout = {
                if (it.didOverflowHeight || (state.preferSingleLine && (it.didOverflowWidth || it.lineCount > 1))) {
                    if (state.readyToDraw(id)) {
                        state = state.setReadyToDraw(id, false)
                    }
                    val nextFontSizeValue = state.fontSizeValue.coerceAtMost(state.fontSizeRange.max.value) - state.fontSizeRange.step.value
                    state = if (nextFontSizeValue <= state.fontSizeRange.min.value) {
                        // Reached minimum, set minimum font size and it's readyToDraw
                        state.setFontSizeValue(id, state.fontSizeRange.min.value).setReadyToDraw(id, true)
                    } else {
                        // Text doesn't fit yet and haven't reached minimum text range, keep decreasing
                        state.setFontSizeValue(id, nextFontSizeValue)
                    }
                } else {
                    // Text fits before reaching the minimum, it's readyToDraw
                    state = state.setReadyToDraw(id, true)
                }
                lastTextLayoutResult = it
            },
            modifier = modifier.drawWithContent { if (state.readyToDraw(id)) drawContent() }
        )
    }
}

@Immutable
data class FontSizeRange(
    val min: TextUnit = 1.sp,
    val max: TextUnit,
    val step: TextUnit = DEFAULT_TEXT_STEP
) {
    init {
        require(min < max) { "min should be less than max, $this" }
        require(step.value > 0) { "step should be greater than 0, $this" }
    }

    companion object {
        private val DEFAULT_TEXT_STEP = 1.sp
    }
}

@Immutable
data class AutoResizeFontState(
    val fontSizeRange: FontSizeRange,
    val preferSingleLine: Boolean = false,
    private val subscribers: Set<Uuid> = emptySet(),
    private val fontSizeValueSubscribers: Map<Uuid, Float> = emptyMap(),
    private val readyToDrawSubscribers: Set<Uuid> = emptySet(),
) {
    val readyToDraw: Boolean = subscribers.all { readyToDrawSubscribers.contains(it) }
    fun readyToDraw(id: Uuid) = readyToDrawSubscribers.contains(id)
    fun subscribe(id: Uuid): AutoResizeFontState = copy(subscribers = subscribers.toMutableSet().apply { add(id) })
    fun unsubscribe(id: Uuid): AutoResizeFontState = copy(subscribers = subscribers.toMutableSet().apply { remove(id) })
    fun setReadyToDraw(id: Uuid, readyToDraw: Boolean): AutoResizeFontState = copy(readyToDrawSubscribers = readyToDrawSubscribers.toMutableSet().apply { if (readyToDraw) add(id) else remove(id) })
    val fontSizeValue: Float = fontSizeValueSubscribers.values.minOrNull()?: fontSizeRange.max.value
    fun fontSizeValue(id: Uuid): Float = fontSizeValueSubscribers[id]?: fontSizeValue
    fun setFontSizeValue(id: Uuid, fontSizeValue: Float?): AutoResizeFontState = copy(fontSizeValueSubscribers = fontSizeValueSubscribers.toMutableMap().apply { if (fontSizeValue == null) remove(id) else this[id] = fontSizeValue })
}

@Composable
fun rememberAutoResizeFontState(
    fontSizeRange: FontSizeRange,
    preferSingleLine: Boolean = false
): MutableState<AutoResizeFontState> {
    val state = remember { mutableStateOf(AutoResizeFontState(fontSizeRange, preferSingleLine)) }
    LaunchedEffect (fontSizeRange, preferSingleLine) {
        state.value = AutoResizeFontState(fontSizeRange, preferSingleLine)
    }
    return state
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.userMarquee(): Modifier {
    return if (Shared.disableMarquee) this else basicMarquee(Int.MAX_VALUE)
}

fun userMarqueeMaxLines(otherwise: Int = Int.MAX_VALUE): Int {
    return if (Shared.disableMarquee) otherwise else 1
}