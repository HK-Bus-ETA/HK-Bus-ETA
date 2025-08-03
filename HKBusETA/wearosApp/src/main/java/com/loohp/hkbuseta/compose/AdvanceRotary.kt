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

package com.loohp.hkbuseta.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


private enum class ScrollDirection {
    FIRST, NONE, FORWARD, BACKWARD
}

@Composable
fun Modifier.rotaryScroll(
    scroll: ScrollableState,
    focusRequester: FocusRequester = remember { FocusRequester() },
    animationSpec: AnimationSpec<Float> = TweenSpec(durationMillis = 300, easing = FastOutSlowInEasing),
): Modifier {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val mutex = remember { Mutex() }
    val animatedScrollValue = remember { Animatable(0F) }
    var isScrollInProgress by remember { mutableStateOf(false) }
    var previousScrollValue by remember { mutableFloatStateOf(0F) }
    var lastScrollDirection by remember { mutableStateOf(ScrollDirection.FIRST) }

    LaunchedEffect (animatedScrollValue.value) {
        val diff = previousScrollValue - animatedScrollValue.value
        scroll.scrollBy(diff)
        previousScrollValue -= diff
    }

    LaunchedEffect (isScrollInProgress) {
        if (!isScrollInProgress) {
            when {
                lastScrollDirection == ScrollDirection.FIRST -> {
                    lastScrollDirection = ScrollDirection.NONE
                }
                !scroll.canScrollForward -> {
                    if (lastScrollDirection != ScrollDirection.FORWARD) {
                        lastScrollDirection = ScrollDirection.FORWARD
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                !scroll.canScrollBackward -> {
                    if (lastScrollDirection != ScrollDirection.BACKWARD) {
                        lastScrollDirection = ScrollDirection.BACKWARD
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                else -> {
                    lastScrollDirection = ScrollDirection.NONE
                }
            }
        }
    }

    return this
        .onRotaryScrollEvent {
            scope.launch {
                mutex.withLock {
                    val target = it.verticalScrollPixels + animatedScrollValue.value
                    animatedScrollValue.snapTo(target)
                    previousScrollValue = target
                }
                isScrollInProgress = true
                animatedScrollValue.animateTo(0F, animationSpec)
                isScrollInProgress = false
            }
            true
        }
        .hierarchicalFocusGroup(active = true)
        .requestFocusOnHierarchyActive()
        .focusRequester(focusRequester)
        .focusable()
}