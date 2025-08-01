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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


@Immutable
data class IconPainterLayer(
    val painter: @Composable () -> Painter,
    val overrideColor: Color? = null,
    val modifier: Modifier.() -> Modifier = { this }
)

@Composable
expect fun <T> StateFlow<T>.collectAsStateMultiplatform(context: CoroutineContext = EmptyCoroutineContext): State<T>

@Composable
fun <T> MutableStateFlow<T>.collectAsStateMultiplatform(
    context: CoroutineContext = EmptyCoroutineContext,
    setValueCallback: ((T) -> Unit)? = null
): MutableState<T> {
    val delegate by (this as StateFlow<T>).collectAsStateMultiplatform(context)
    val mutableState = remember(delegate) { object : MutableState<T> {
        private var usingTempValue = false
        private var tempValue: T = delegate
        override var value: T
            get() = if (usingTempValue) tempValue else delegate
            set(value) {
                tempValue = value
                usingTempValue = true
                this@collectAsStateMultiplatform.value = value
                setValueCallback?.invoke(value)
            }
        override operator fun component1(): T = value
        override operator fun component2(): (T) -> Unit = { value = it }
    } }
    return mutableState
}

@ExperimentalMaterial3Api
@Composable
fun rememberPlatformModalBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true }
): SheetState {
    return rememberModalBottomSheetState(
        skipPartiallyExpanded = composePlatform.hasLargeScreen || skipPartiallyExpanded,
        confirmValueChange = confirmValueChange
    )
}

@Composable
fun LanguageDarkModeChangeEffect(vararg key: Any?, block: CoroutineScope.(String, Boolean) -> Unit) {
    var language by remember { mutableStateOf(Shared.language) }
    val darkMode = Shared.theme.isDarkMode
    LaunchedEffect (Unit) {
        while (true) {
            language = Shared.language
            delay(100)
        }
    }
    LaunchedEffect (language, darkMode, *key) {
        block.invoke(this, language, darkMode)
    }
}

@Composable
expect fun BackButtonHandler(enabled: Boolean = true, onBack: () -> Unit)

expect class PlatformBackEvent {
    val touchX: Float
    val touchY: Float
    val progress: Float
    val swipeEdge: Int
}

expect val allowRightSwipeBackGesture: Boolean

@Composable
expect fun PredicativeBackGestureHandler(enabled: Boolean = true, onBack: suspend (Flow<PlatformBackEvent>) -> Unit)

expect val currentLocalWindowSize: IntSize @Composable get

@Composable
expect fun rememberIsInPipMode(context: AppActiveContext): Boolean

expect fun AppActiveContext.enterPipMode(
    aspectRatio: AspectRatio = AspectRatio(16, 10),
    sourceRectHint: SourceRectHintArea? = null
)

data class AspectRatio(val numerator: Int, val denominator: Int)
data class SourceRectHintArea(val left: Int, val top: Int, val right: Int, val bottom: Int)

@Composable
expect fun calculateWindowSizeClass(): WindowSizeClass