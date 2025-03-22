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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.WearableConnectionState
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import kotlinx.coroutines.delay

actual suspend fun invalidateWatchCache(context: AppContext) {
    if (RemoteActivityUtils.hasWatchApp(context.context).await()) {
        RemoteActivityUtils.dataToWatch(context.context, Shared.INVALIDATE_CACHE_ID, null)
    }
}

@Composable
actual fun rememberWearableConnected(context: AppContext): State<WearableConnectionState> {
    val state = remember { mutableStateOf(WearableConnectionState.NONE_DETECTED) }

    LaunchedEffect (Unit) {
        while (true) {
            state.value = if (RemoteActivityUtils.hasWatchApp(context.context).await()) {
                WearableConnectionState.CONNECTED
            } else {
                WearableConnectionState.NONE_DETECTED
            }
            delay(5000)
        }
    }

    return state
}