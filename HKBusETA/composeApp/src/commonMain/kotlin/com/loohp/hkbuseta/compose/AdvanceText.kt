/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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

@file:OptIn(ExperimentalUuidApi::class)

package com.loohp.hkbuseta.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.utils.asAnnotatedString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


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
    val scope = rememberCoroutineScope()
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
                modifier = Modifier
                    .onHardwareKeyboardEnter {
                        scope.launch { if (inputValid) onConfirmation.invoke(textInput.text) }
                        true
                    }
                    .focusRequester(focusRequester),
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
        autoResizeTextState = rememberAutoResizeTextState(
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
        autoResizeTextState = rememberAutoResizeTextState(
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
    autoResizeTextState: MutableState<AutoResizeTextState>,
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
    autoResizeTextState: MutableState<AutoResizeTextState>,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    var state by autoResizeTextState
    val id = remember { Uuid.random() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var lastTextLayoutResult: TextLayoutResult? by remember { mutableStateOf(null) }

    ImmediateEffect (Unit) {
        state = state.subscribe(id)
    }
    DisposableEffect (Unit) {
        onDispose { state = state.unsubscribe(id) }
    }

    ChangedEffect (text) {
        state = state.reset()
    }
    LaunchedEffect (state, lastTextLayoutResult) {
        if (state.isReady()) {
            lastTextLayoutResult?.let { onTextLayout?.invoke(it) }
        }
    }

    key(state.isReady()) {
        PlatformText(
            text = text,
            color = color,
            maxLines = if (!state.isReady() && state.preferSingleLine) 1 else maxLines,
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
            fontSize = state.fontSize(density),
            onTextLayout = {
                scope.launch {
                    if (!state.isReady()) {
                        if (it.didOverflowHeight || (state.preferSingleLine && (it.didOverflowWidth || it.lineCount > 1))) {
                            with(density) {
                                val nextFontSizePx = state.fontSize(density).toPx() - state.fontSizeRange.step.toPx()
                                state = if (nextFontSizePx <= state.fontSizeRange.min.toPx()) {
                                    // Reached minimum, set minimum font size and it's readyToDraw
                                    state.fontSize(id, state.fontSizeRange.min).ready(id)
                                } else {
                                    // Text doesn't fit yet and haven't reached minimum text range, keep decreasing
                                    state.fontSize(id, nextFontSizePx.toSp())
                                }
                            }
                        } else {
                            // Text fits before reaching the minimum, it's readyToDraw
                            state = state.ready(id)
                        }
                    }
                    lastTextLayoutResult = it
                }
            },
            modifier = modifier.drawWithContent { if (state.isReady()) drawContent() }
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
data class AutoResizeTextState(
    val fontSizeRange: FontSizeRange,
    val preferSingleLine: Boolean = false,
    val subscribers: List<Uuid> = emptyList(),
    val measuredFontSize: Map<Uuid, TextUnit> = emptyMap(),
    val ready: Set<Uuid> = emptySet()
) {
    fun isReady(): Boolean {
        return ready.containsAll(subscribers)
    }
    val fontSize: TextUnit @Composable get() = fontSize(LocalDensity.current)
    fun fontSize(density: Density): TextUnit {
        return with(density) { measuredFontSize.values.minByOrNull { it.toPx() }?: fontSizeRange.max }
    }
    fun fontSize(id: Uuid, fontSize: TextUnit): AutoResizeTextState {
        return copy(measuredFontSize = measuredFontSize.toMutableMap().apply { this[id] = fontSize })
    }
    fun ready(id: Uuid): AutoResizeTextState {
        return copy(ready = ready + id)
    }
    fun subscribe(id: Uuid): AutoResizeTextState {
        return if (subscribers.contains(id)) this else copy(subscribers = subscribers + id).reset()
    }
    fun unsubscribe(id: Uuid): AutoResizeTextState {
        return copy(subscribers = subscribers.toMutableList().apply { removeAll { it == id } })
    }
    fun reset(): AutoResizeTextState {
        return copy(ready = emptySet(), measuredFontSize = emptyMap())
    }
}

@Composable
fun rememberAutoResizeTextState(
    fontSizeRange: FontSizeRange,
    preferSingleLine: Boolean = false
): MutableState<AutoResizeTextState> {
    val mutableState = remember { mutableStateOf(AutoResizeTextState(fontSizeRange, preferSingleLine)) }
    var state by mutableState

    val density = LocalDensity.current
    val windowSize = currentLocalWindowSize

    LaunchedEffect (density.fontScale, density.density, windowSize, state.fontSizeRange) {
        state = state.reset()
    }

    return mutableState
}

@Composable
fun <T> AnimatedTextTransition(
    state: T,
    transitionSpec: AnimationSpec<Int> = tween(durationMillis = 500, easing = LinearEasing),
    contentKey: (T) -> Any? = { it },
    content: @Composable (T) -> Unit
) {
    var init by remember { mutableStateOf(false) }

    val key by remember(state) { derivedStateOf { contentKey.invoke(state) } }
    var previousState by remember { mutableStateOf(state) }

    var height by remember { mutableIntStateOf(0) }

    val animatable = remember { Animatable(0, Int.VectorConverter) }
    val animatedOffset by animatable.asState()

    LaunchedEffect (key) {
        if (init) {
            animatable.snapTo(height)
            animatable.animateTo(0, transitionSpec)
            previousState = state
        } else {
            init = true
        }
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .applyIf(animatable.isRunning) { clipToBounds() }
            .onSizeChanged { height = it.height }
    ) {
        if (animatedOffset > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { translationY = (animatedOffset - height).toFloat() }
            ) {
                content.invoke(previousState)
            }
        }
        Box(
            modifier = Modifier
                .wrapContentSize()
                .graphicsLayer { translationY = animatedOffset.toFloat() }
        ) {
            content.invoke(state)
        }
    }
}

fun Modifier.userMarquee(): Modifier {
    return if (Shared.disableMarquee) this else basicMarquee(Int.MAX_VALUE)
}

fun userMarqueeMaxLines(otherwise: Int = Int.MAX_VALUE): Int {
    return if (Shared.disableMarquee) otherwise else 1
}