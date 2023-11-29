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

package com.loohp.hkbuseta.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import com.google.android.horologist.compose.ambient.AmbientStateUpdate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


@Stable
class HapticsController(
    @Volatile var enabled: Boolean = true,
    @Volatile var invokedCallback: (HapticsController) -> Unit = {}
)

fun Modifier.rotaryScroll(
    scroll: ScrollableState,
    focusRequester: FocusRequester,
    hapticsController: HapticsController = HapticsController(),
    animationSpec: AnimationSpec<Float> = TweenSpec(durationMillis = 300, easing = FastOutSlowInEasing),
    ambientStateUpdate: AmbientStateUpdate? = null
): Modifier = composed {
    val ambientMode = ambientStateUpdate?.let { rememberIsInAmbientMode(it) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var scrollCounter by remember { mutableIntStateOf(0) }
    val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
    val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
    var scrollMoved by remember { mutableIntStateOf(0) }

    val mutex by remember { mutableStateOf(Mutex()) }
    val animatedScrollValue = remember { Animatable(0F) }
    var previousScrollValue by remember { mutableFloatStateOf(0F) }

    LaunchedEffect (ambientMode) {
        focusRequester.freeFocus()
        scope.launch { focusRequester.requestFocus() }
    }
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
        if (scrollReachedEnd && scrollMoved > 0 && hapticsController.enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hapticsController.invokedCallback.invoke(hapticsController)
        }
        if (scrollMoved <= 0) {
            scrollMoved++
        }
    }

    onRotaryScrollEvent {
        scope.launch {
            mutex.withLock {
                val target = it.verticalScrollPixels + animatedScrollValue.value
                animatedScrollValue.snapTo(target)
                previousScrollValue = target
            }
            animatedScrollValue.animateTo(0F, animationSpec)
        }
        true
    }.focusRequester(
        focusRequester = focusRequester
    ).focusable()
}