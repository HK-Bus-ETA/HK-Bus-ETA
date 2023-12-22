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

package com.loohp.hkbuseta.app

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.RestartEffect
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Registry.PossibleNextCharResult
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ImmutableState
import com.loohp.hkbuseta.utils.asImmutableState
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.shared.AndroidShared
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Immutable
data class RouteKeyboardState(val text: String, val nextCharResult: PossibleNextCharResult)

fun defaultText(): String {
    return if (Shared.language == "en") "Input Route" else "輸入路線"
}

@Composable
fun SearchPage(instance: AppActiveContext) {
    HKBusETATheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top
        ) {
            AndroidShared.MainTime()
        }
        SearchMainElement(instance)
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun SearchMainElement(instance: AppActiveContext) {
    val state = remember { mutableStateOf(RouteKeyboardState(defaultText(), Registry.getInstance(instance).getPossibleNextChar(""))) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp, 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
        Box(
            modifier = Modifier
                .width(140.scaledSize(instance).dp)
                .height(35.scaledSize(instance).dp)
                .border(
                    2.scaledSize(instance).dp,
                    MaterialTheme.colors.secondaryVariant,
                    RoundedCornerShape(10)
                )
                .background(MaterialTheme.colors.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                textAlign = TextAlign.Center,
                color = if (state.value.text == defaultText()) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.primary,
                text = Shared.getMtrLineName(state.value.text)
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            Column {
                KeyboardButton(instance, '7', state)
                KeyboardButton(instance, '4', state)
                KeyboardButton(instance, '1', state)
                KeyboardButton(instance, '<', '-', state, Color.Red, persistentListOf(Icons.Outlined.Delete.asImmutableState(), R.drawable.baseline_history_24.asImmutableState()))
            }
            Column {
                KeyboardButton(instance, '8', state)
                KeyboardButton(instance, '5', state)
                KeyboardButton(instance, '2', state)
                KeyboardButton(instance, '0', state)
            }
            Column {
                KeyboardButton(instance, '9', state)
                KeyboardButton(instance, '6', state)
                KeyboardButton(instance, '3', state)
                KeyboardButton(instance, '/', null, state, Color.Green, persistentListOf(Icons.Outlined.Done.asImmutableState()))
            }
            Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
            Box (
                modifier = Modifier
                    .width(35.scaledSize(instance).dp)
                    .height(135.scaledSize(instance).dp)
            ) {
                val focusRequester = rememberActiveFocusRequester()
                val scroll = rememberScrollState()
                val scope = rememberCoroutineScope()
                val haptic = LocalHapticFeedback.current
                val possibleValues by remember { derivedStateOf { state.value.nextCharResult.characters } }
                var scrollCounter by remember { mutableIntStateOf(0) }
                val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
                val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
                var scrollMoved by remember { mutableIntStateOf(0) }

                val mutex by remember { mutableStateOf(Mutex()) }
                var job: Job? = remember { null }
                val animatedScrollValue = remember { Animatable(0F) }
                var previousScrollValue by remember { mutableFloatStateOf(0F) }
                LaunchedEffect (animatedScrollValue.value) {
                    if (scrollMoved > 0) {
                        val diff = previousScrollValue - animatedScrollValue.value
                        job?.cancel()
                        job = launch { scroll.animateScrollBy(0F, TweenSpec(durationMillis = 500)) }
                        scroll.scrollBy(diff)
                        previousScrollValue -= diff
                    }
                }

                LaunchedEffect (possibleValues) {
                    scrollMoved = 1
                }
                LaunchedEffect (scrollInProgress) {
                    if (scrollInProgress) {
                        scrollCounter++
                    }
                }
                LaunchedEffect (scrollCounter, scrollReachedEnd) {
                    delay(50)
                    if (scrollReachedEnd && scrollMoved > 1) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (scrollMoved <= 1) {
                        scrollMoved++
                    }
                }

                Column (
                    modifier = Modifier
                        .verticalScrollWithScrollbar(
                            state = scroll,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            scrollbarConfig = ScrollBarConfig(
                                indicatorThickness = 2.dp,
                                padding = PaddingValues(0.dp, 2.dp, 0.dp, 2.dp)
                            )
                        )
                        .onRotaryScrollEvent {
                            scope.launch {
                                mutex.withLock {
                                    val target = it.verticalScrollPixels + animatedScrollValue.value
                                    animatedScrollValue.snapTo(target)
                                    previousScrollValue = target
                                }
                                animatedScrollValue.animateTo(
                                    0F,
                                    TweenSpec(durationMillis = 500, easing = FastOutSlowInEasing)
                                )
                            }
                            true
                        }
                        .focusRequester(
                            focusRequester = focusRequester
                        )
                        .focusable()
                ) {
                    val currentText = state.value.text
                    if (currentText.isEmpty() || currentText == defaultText()) {
                        KeyboardButton(instance, '!', null, state, Color.Red, persistentListOf(R.mipmap.mtr.asImmutableState()))
                    }
                    for (alphabet in 'A'..'Z') {
                        if (possibleValues.contains(alphabet)) {
                            KeyboardButton(instance, alphabet, state)
                        }
                    }
                }
            }
        }
    }
}

fun handleInput(instance: AppActiveContext, state: MutableState<RouteKeyboardState>, input: Char) {
    var originalText = state.value.text
    if (originalText == defaultText()) {
        originalText = ""
    }
    if (input == '/' || input == '!' || (input == '<' && Shared.hasFavoriteAndLookupRoute() && originalText.isEmpty())) {
        val result = when (input) {
            '!' -> Registry.getInstance(instance).findRoutes("", false) { r -> r.bound.containsKey(Operator.MTR) }
            '<' -> Registry.getInstance(instance).findRoutes("", false) { r, c ->
                val meta = when (c) {
                    Operator.GMB -> r.gmbRegion!!.name
                    Operator.NLB -> r.nlbId
                    else -> ""
                }
                Shared.getFavoriteAndLookupRouteIndex(r.routeNumber, c, meta) < Int.MAX_VALUE
            }
            else -> Registry.getInstance(instance).findRoutes(originalText, true)
        }
        if (result.isNotEmpty()) {
            val intent = AppIntent(instance, AppScreen.LIST_ROUTES)
            intent.putExtra("result", result.map { it.strip(); it.serialize() }.toJsonArray().toString())
            if (input == '<') {
                intent.putExtra("recentSort", RecentSortMode.FORCED.ordinal)
            }
            intent.putExtra("listType", RouteListType.RECENT.name)
            instance.startActivity(intent)
        }
    } else {
        val newText = if (input == '<') {
            if (originalText.isNotEmpty()) {
                originalText.dropLast(1)
            } else {
                originalText
            }
        } else if (input == '-') {
            ""
        } else {
            originalText + input
        }
        val possibleNextChar = Registry.getInstance(instance).getPossibleNextChar(newText)
        val text = newText.ifEmpty { defaultText() }
        state.value = RouteKeyboardState(text, possibleNextChar)
    }
}

@Composable
fun KeyboardButton(instance: AppActiveContext, content: Char, state: MutableState<RouteKeyboardState>) {
    KeyboardButton(instance, content, null, state, MaterialTheme.colors.primary, persistentListOf())
}

@Composable
fun KeyboardButton(instance: AppActiveContext, content: Char, longContent: Char?, state: MutableState<RouteKeyboardState>, color: Color, icons: ImmutableList<ImmutableState<Any>>) {
    val icon = if (icons.isEmpty()) null else icons[0].value
    val enabled = when (content) {
        '/' -> state.value.nextCharResult.hasExactMatch
        '<' -> true
        '!' -> true
        else -> state.value.nextCharResult.characters.contains(content)
    }
    val haptic = LocalHapticFeedback.current
    val actualColor = if (enabled) color else Color(0xFF444444)

    var hasHistory by remember { mutableStateOf(Shared.hasFavoriteAndLookupRoute()) }
    RestartEffect {
        hasHistory = Shared.hasFavoriteAndLookupRoute()
    }
    val isLookupButton by remember { derivedStateOf { content == '<' && hasHistory && state.value.text.let { it.isEmpty() || it == defaultText() } && icons.size > 1 } }

    AdvanceButton(
        onClick = {
            handleInput(instance, state, content)
        },
        onLongClick = {
            if (longContent != null && !isLookupButton) {
                handleInput(instance, state, longContent)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        },
        modifier = Modifier
            .width(35.scaledSize(instance).dp)
            .height((if (content.isLetter() || content == '!') 30 else 35).scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = actualColor
        ),
        enabled = enabled,
        content = {
            if (isLookupButton) {
                Icon(
                    modifier = Modifier.size(17.dp),
                    painter = painterResource(icons[1].value as Int),
                    contentDescription = content.toString(),
                    tint = Color(0xFF03A9F4),
                )
            } else {
                when (icon) {
                    null -> {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = actualColor,
                            text = content.toString()
                        )
                    }
                    is ImageVector -> {
                        Icon(
                            modifier = Modifier.size(17.dp),
                            imageVector = icon,
                            contentDescription = content.toString(),
                            tint = actualColor,
                        )
                    }
                    is Int -> {
                        Image(
                            modifier = Modifier.size(17.dp),
                            painter = painterResource(icon),
                            contentDescription = content.toString()
                        )
                    }
                }
            }
        }
    )
}