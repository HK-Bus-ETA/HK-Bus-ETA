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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.multiplatform.lifecycle.LifecycleEvent
import com.multiplatform.lifecycle.LifecycleObserver
import com.multiplatform.lifecycle.LocalLifecycleTracker

@Composable
actual fun RestartEffect(onRestart: () -> Unit) {
    val lifecycleTracker = LocalLifecycleTracker.current
    DisposableEffect(Unit) {
        val listener = object : LifecycleObserver {
            override fun onEvent(event: LifecycleEvent) {
                when (event) {
                    LifecycleEvent.OnResumeEvent, LifecycleEvent.OnStartEvent -> onRestart.invoke()
                    else -> { /* do nothing */ }
                }
            }
        }
        lifecycleTracker.addObserver(listener)
        onDispose {
            lifecycleTracker.removeObserver(listener)
        }
    }
}

@Composable
actual fun PauseEffect(onPause: () -> Unit) {
    val lifecycleTracker = LocalLifecycleTracker.current
    DisposableEffect(Unit) {
        val listener = object : LifecycleObserver {
            override fun onEvent(event: LifecycleEvent) {
                when (event) {
                    LifecycleEvent.OnPauseEvent, LifecycleEvent.OnStopEvent -> onPause.invoke()
                    else -> { /* do nothing */ }
                }
            }
        }
        lifecycleTracker.addObserver(listener)
        onDispose {
            lifecycleTracker.removeObserver(listener)
        }
    }
}