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

package com.loohp.hkbuseta.utils

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.LocationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred


actual val shouldRecordLastLocation: Boolean = false

actual fun checkLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    callback.invoke(false)
}

actual fun checkBackgroundLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    callback.invoke(false)
}

actual fun getGPSLocationUnrecorded(appContext: AppContext, priority: LocationPriority): Deferred<LocationResult?> {
    return CompletableDeferred(LocationResult.FAILED_RESULT)
}

actual fun getGPSLocationUnrecorded(appContext: AppContext, interval: Long, listener: (LocationResult) -> Unit): Deferred<() -> Unit> {
    return CompletableDeferred { /* do nothing */ }
}

actual fun isGPSServiceEnabled(appContext: AppContext, notifyUser: Boolean): Boolean {
    if (notifyUser) {
        appContext.showToastText(if (Shared.language == "en") "Device does not support location" else "裝置不支援定位位置", ToastDuration.LONG)
    }
    return false
}