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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.appcontext.AppActiveContext
import com.loohp.hkbuseta.compose.fullPageVerticalScrollWithScrollbar
import com.loohp.hkbuseta.objects.BilingualAnnotatedText
import com.loohp.hkbuseta.objects.BilingualText
import com.loohp.hkbuseta.objects.asAnnotatedText
import com.loohp.hkbuseta.objects.withEn
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


private val defaultDismissText = "確認" withEn "OK"

private val specialText = listOf(
    BilingualAnnotatedText(
        buildAnnotatedString {
            append("你可能需要「")
            append("允許背景活動", SpanStyle(fontWeight = FontWeight.Bold))
            append("」讓此功能在螢幕關閉時繼續正常運作\n\n此功能目前在")
            append("測試階段", SpanStyle(fontWeight = FontWeight.Bold))
            append(", 運作可能不穩定")

        },
        buildAnnotatedString {
            append("You might need to \"")
            append("Allow Background Activity", SpanStyle(fontWeight = FontWeight.Bold))
            append("\" for this feature to continue working while the screen is off.\n\nThis feature is currently in ")
            append("beta", SpanStyle(fontWeight = FontWeight.Bold))
            append(", which might be unstable.")
        }
    )
)

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun TextElement(specialTextIndex: Int, inputText: BilingualText, optDismissText: BilingualText?, instance: AppActiveContext) {
    val text = specialText.getOrElse(specialTextIndex) { inputText.asAnnotatedText() }
    val dismissText = optDismissText?: defaultDismissText
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val scroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        var scrollCounter by remember { mutableIntStateOf(0) }
        val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
        val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
        var scrollMoved by remember { mutableStateOf(false) }

        val mutex by remember { mutableStateOf(Mutex()) }
        val animatedScrollValue = remember { Animatable(0F) }
        var previousScrollValue by remember { mutableFloatStateOf(0F) }
        LaunchedEffect (animatedScrollValue.value) {
            val diff = previousScrollValue - animatedScrollValue.value
            scroll.scrollBy(diff)
            previousScrollValue -= diff
        }

        LaunchedEffect (scrollInProgress) {
            if (scrollInProgress) {
                scrollCounter++
            }
        }
        LaunchedEffect (scrollCounter, scrollReachedEnd) {
            delay(50)
            if (scrollReachedEnd && scrollMoved) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (!scrollMoved) {
                scrollMoved = true
            }
        }

        Column (
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalScrollWithScrollbar(
                    state = scroll,
                    context = instance,
                    flingBehavior = ScrollableDefaults.flingBehavior()
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
                            TweenSpec(durationMillis = 300, easing = FastOutSlowInEasing)
                        )
                    }
                    true
                }
                .focusRequester(
                    focusRequester = focusRequester
                )
                .focusable(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(50.scaledSize(instance).dp))
            Text(
                modifier = Modifier.fillMaxWidth(0.8F),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Normal,
                fontSize = 15F.scaledSize(instance).sp,
                text = text[Shared.language]
            )
            Spacer(modifier = Modifier.size(20.scaledSize(instance).dp))
            Button(
                onClick = {
                    instance.setResult(1)
                    instance.finish()
                },
                modifier = Modifier
                    .width(90.scaledSize(instance).dp)
                    .height(35.scaledSize(instance).dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.secondary,
                    contentColor = MaterialTheme.colors.primary
                ),
                content = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = 17F.scaledSize(instance).sp,
                        text = dismissText[Shared.language]
                    )
                }
            )
            Spacer(modifier = Modifier.size(50.scaledSize(instance).dp))
        }
    }
}