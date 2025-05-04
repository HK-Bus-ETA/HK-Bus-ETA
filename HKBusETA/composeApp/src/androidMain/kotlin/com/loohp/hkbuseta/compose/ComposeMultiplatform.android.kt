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

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext


@Composable
actual fun <T> StateFlow<T>.collectAsStateMultiplatform(context: CoroutineContext): State<T> = collectAsStateWithLifecycle(context = context)

@Composable
actual fun BackButtonEffect(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}

actual inline val currentLocalWindowSize: IntSize
    @Composable get() = LocalConfiguration.current.run {
        with (LocalDensity.current) { IntSize(screenWidthDp.dp.roundToPx(), screenHeightDp.dp.roundToPx()) }
    }

@Composable
actual fun rememberIsInPipMode(context: AppActiveContext): Boolean {
    val activity = LocalContext.current.findActivity()
        ?: throw IllegalStateException("Picture in picture should be called in the context of an Activity")
    var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect (activity) {
        val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
            pipMode = info.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(observer)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
    }
    return pipMode
}

internal fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

actual fun AppActiveContext.enterPipMode() {
    val activity = compose.context.findActivity()
        ?: throw IllegalStateException("Picture in picture should be called in the context of an Activity")
    if (!activity.isInPictureInPictureMode) {
        activity.enterPictureInPictureMode(PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 10))
            .build())
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    val activity = LocalContext.current.findActivity()
        ?: throw IllegalStateException("Unable to find current activity")
    return androidx.compose.material3.windowsizeclass.calculateWindowSizeClass(activity)
}