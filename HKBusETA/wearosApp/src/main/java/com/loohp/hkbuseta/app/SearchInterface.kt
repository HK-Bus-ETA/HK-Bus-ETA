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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.asBilingualText
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Registry.PossibleNextCharResult
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Immutable
data class RouteKeyboardState(
    val text: String,
    val nextCharResult: PossibleNextCharResult,
    val isLoading: Boolean,
    val showLoadingIndicator: Boolean
)

@Composable
fun SearchPage(instance: AppActiveContext) {
    HKBusETATheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top
        ) {
            WearOSShared.MainTime()
        }
        SearchMainElement(instance)
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun SearchMainElement(instance: AppActiveContext) {
    val state = remember { mutableStateOf(RouteKeyboardState("", Registry.getInstance(instance).getPossibleNextChar(""), isLoading = false, showLoadingIndicator = false)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp, 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
        val shape = RoundedCornerShape(15.scaledSize(instance).dp)
        Box(
            modifier = Modifier
                .width(140.scaledSize(instance).dp)
                .height(35.scaledSize(instance).dp)
                .border(
                    width = 3.scaledSize(instance).dp,
                    color = MaterialTheme.colors.secondaryVariant,
                    shape = shape
                )
                .background(
                    color = MaterialTheme.colors.secondary,
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = if (state.value.text.isEmpty()) {
                    if (Shared.language == "en") "Input Route" else "輸入路線"
                } else {
                    Shared.getMtrLineName(state.value.text)
                }
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            Column {
                KeyboardButton(instance, '1', state)
                KeyboardButton(instance, '4', state)
                KeyboardButton(instance, '7', state)
                KeyboardButton(instance, '<', '-', state, Color.Red, persistentListOf(Icons.Outlined.Delete.asImmutableState(), R.drawable.baseline_history_24.asImmutableState()))
            }
            Column {
                KeyboardButton(instance, '2', state)
                KeyboardButton(instance, '5', state)
                KeyboardButton(instance, '8', state)
                KeyboardButton(instance, '0', state)
            }
            Column {
                KeyboardButton(instance, '3', state)
                KeyboardButton(instance, '6', state)
                KeyboardButton(instance, '9', state)
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
                    if (currentText.isEmpty()) {
                        KeyboardButton(instance, '!', null, state, null, persistentListOf(R.mipmap.mtr.asImmutableState()))
                        KeyboardButton(instance, '~', null, state, Color(0xFF66CCFF), persistentListOf(R.drawable.baseline_directions_boat_24.asImmutableState()))
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

suspend fun handleInput(instance: AppActiveContext, state: MutableState<RouteKeyboardState>, input: Char) {
    val originalText = state.value.text
    if (input == '/' || input == '!' || input == '~' || (input == '<' && Shared.lastLookupRoutes.value.isNotEmpty() && originalText.isEmpty())) {
        state.value = state.value.copy(isLoading = true)
        val job = CoroutineScope(Dispatchers.IO).launch {
            delay(500)
            state.value = state.value.copy(showLoadingIndicator = true)
        }
        val result = when (input) {
            '!' -> Registry.getInstance(instance).findRoutes("", false, Shared.MTR_ROUTE_FILTER)
            '~' -> Registry.getInstance(instance).findRoutes("", false, Shared.FERRY_ROUTE_FILTER)
            '<' -> Registry.getInstance(instance).findRoutes("", false, Shared.RECENT_ROUTE_FILTER)
            else -> Registry.getInstance(instance).findRoutes(originalText, true)
        }
        if (result.isNotEmpty()) {
            val intent = AppIntent(instance, AppScreen.LIST_ROUTES)
            intent.putExtra("result", result.map { it.strip(); it.serialize() }.toJsonArray().toString())
            if (input == '<') {
                intent.putExtra("recentSort", RecentSortMode.FORCED.ordinal)
                intent.putExtra("listType", RouteListType.RECENT.name)
            } else {
                intent.putExtra("listType", RouteListType.NORMAL.name)
            }
            if (input == '!') {
                intent.putExtra("mtrSearch", "")
            }
            instance.startActivity(intent)
        }
        job.cancelAndJoin()
        state.value = state.value.copy(isLoading = false, showLoadingIndicator = false)
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
        state.value = state.value.copy(text = newText, nextCharResult = possibleNextChar)
    }
}

@Composable
fun KeyboardButton(instance: AppActiveContext, content: Char, state: MutableState<RouteKeyboardState>) {
    KeyboardButton(instance, content, null, state, MaterialTheme.colors.primary, persistentListOf())
}

@Composable
fun KeyboardButton(instance: AppActiveContext, content: Char, longContent: Char?, state: MutableState<RouteKeyboardState>, color: Color?, icons: ImmutableList<ImmutableState<Any>>) {
    val icon = if (icons.isEmpty()) null else icons[0].value
    val enabled = when (content) {
        '/' -> state.value.nextCharResult.hasExactMatch
        '<', '!', '~' -> true
        else -> state.value.nextCharResult.characters.contains(content)
    }
    val haptic = LocalHapticFeedback.current
    val actualColor = if (enabled) color else Color(0xFF444444)

    val lastLookupRoutes by Shared.lastLookupRoutes.collectAsStateWithLifecycle()
    val isLookupButton by remember { derivedStateOf { content == '<' && lastLookupRoutes.isNotEmpty() && state.value.text.isEmpty() && icons.size > 1 } }

    AdvanceButton(
        onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                handleInput(instance, state, content)
            }
        },
        onLongClick = {
            if (longContent != null && !isLookupButton) {
                CoroutineScope(Dispatchers.IO).launch {
                    handleInput(instance, state, longContent)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        },
        modifier = Modifier
            .width(35.scaledSize(instance).dp)
            .height((if (content.isLetter() || content == '!' || content == '~') 30 else 35).scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = actualColor?: LocalContentColor.current
        ),
        enabled = !state.value.isLoading && enabled,
        content = {
            when {
                content == '/' && state.value.showLoadingIndicator -> CircularProgressIndicator(
                    modifier = Modifier.size(17F.sp.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFFF9DE09),
                    trackColor = Color(0xFF797979)
                )
                isLookupButton -> Icon(
                    modifier = Modifier.size(19F.sp.dp),
                    painter = painterResource(icons[1].value as Int),
                    contentDescription = content.description()[Shared.language],
                    tint = Color(0xFF03A9F4),
                )
                else -> when (icon) {
                    null -> Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 17F.sp,
                        color = actualColor?: LocalContentColor.current,
                        text = content.toString()
                    )
                    is ImageVector -> Icon(
                        modifier = Modifier.size(19F.sp.dp),
                        imageVector = icon,
                        contentDescription = content.description()[Shared.language],
                        tint = actualColor?: LocalContentColor.current,
                    )
                    is Int -> Image(
                        modifier = Modifier.size(19F.sp.dp),
                        painter = painterResource(icon),
                        contentDescription = content.description()[Shared.language],
                        colorFilter = actualColor?.let { ColorFilter.tint(it) }
                    )
                }
            }
        }
    )
}

private fun Char.description(isLookupButton: Boolean = false): BilingualText {
    return when (this) {
        '!' -> "港鐵" withEn "MTR"
        '~' -> "渡輪" withEn "Ferry"
        '<' -> {
            if (isLookupButton) {
                "最近瀏覽" withEn "Recent Searches"
            } else {
                "刪除" withEn "Backspace"
            }
        }
        '-' -> "清除" withEn "Delete"
        '/' -> "完成" withEn "Done"
        else -> toString().asBilingualText()
    }
}