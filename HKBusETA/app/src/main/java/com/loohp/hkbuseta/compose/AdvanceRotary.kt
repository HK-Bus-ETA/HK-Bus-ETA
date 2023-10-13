package com.loohp.hkbuseta.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun Modifier.rotaryScroll(
    scroll: LazyListState,
    focusRequester: FocusRequester,
    animationSpec: AnimationSpec<Float> = TweenSpec(durationMillis = 300, easing = FastOutSlowInEasing),
): Modifier {
    var rotaryHandler: (RotaryScrollEvent) -> Boolean = { true }

    return composed {
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        var scrollCounter by remember { mutableStateOf(0) }
        val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
        val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
        var scrollMoved by remember { mutableStateOf(0) }

        val mutex by remember { mutableStateOf(Mutex()) }
        val animatedScrollValue = remember { Animatable(0F) }
        var previousScrollValue by remember { mutableStateOf(0F) }

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
            if (scrollReachedEnd && scrollMoved > 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (scrollMoved <= 0) {
                scrollMoved++
            }
        }
        LaunchedEffect (Unit) {
            focusRequester.requestFocus()

            rotaryHandler = {
                scope.launch {
                    mutex.withLock {
                        val target = it.verticalScrollPixels + animatedScrollValue.value
                        animatedScrollValue.snapTo(target)
                        previousScrollValue = target
                    }
                    animatedScrollValue.animateTo(0F, animationSpec)
                }
                true
            }
        }

        this
    }.onRotaryScrollEvent {
        rotaryHandler.invoke(it)
    }
    .focusRequester(
        focusRequester = focusRequester
    )
    .focusable()
}

fun Modifier.rotaryScroll(
    scroll: ScrollState,
    focusRequester: FocusRequester,
    animationSpec: AnimationSpec<Float> = TweenSpec(durationMillis = 300, easing = FastOutSlowInEasing),
): Modifier {
    var rotaryHandler: (RotaryScrollEvent) -> Boolean = { true }

    return composed {
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        var scrollCounter by remember { mutableStateOf(0) }
        val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
        val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
        var scrollMoved by remember { mutableStateOf(0) }

        val mutex by remember { mutableStateOf(Mutex()) }
        val animatedScrollValue = remember { Animatable(0F) }
        var previousScrollValue by remember { mutableStateOf(0F) }

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
            if (scrollReachedEnd && scrollMoved > 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (scrollMoved <= 0) {
                scrollMoved++
            }
        }
        LaunchedEffect (Unit) {
            focusRequester.requestFocus()

            rotaryHandler = {
                scope.launch {
                    mutex.withLock {
                        val target = it.verticalScrollPixels + animatedScrollValue.value
                        animatedScrollValue.snapTo(target)
                        previousScrollValue = target
                    }
                    animatedScrollValue.animateTo(0F, animationSpec)
                }
                true
            }
        }

        this
    }.onRotaryScrollEvent {
        rotaryHandler.invoke(it)
    }
    .focusRequester(
        focusRequester = focusRequester
    )
    .focusable()
}