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

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackEventCompat
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext


@Composable
actual fun <T> StateFlow<T>.collectAsStateMultiplatform(context: CoroutineContext): State<T> = collectAsState(context)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackButtonHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}

@OptIn(ExperimentalComposeUiApi::class)
actual typealias PlatformBackEvent = BackEventCompat

actual val allowRightSwipeBackGesture: Boolean = false

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun PredicativeBackGestureHandler(enabled: Boolean, onBack: suspend (Flow<BackEventCompat>) -> Unit) {
    PredictiveBackHandler(enabled, onBack)
}

actual inline val currentLocalWindowSize: IntSize
    @Composable get() = LocalWindowInfo.current.containerSize

@Composable
actual fun rememberIsInPipMode(context: AppActiveContext): Boolean = false

actual fun AppActiveContext.enterPipMode(aspectRatio: AspectRatio, sourceRectHint: SourceRectHintArea?) {
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    return androidx.compose.material3.windowsizeclass.calculateWindowSizeClass()
}