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

package com.loohp.hkbuseta.utils

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.utils.LocationResult
import com.loohp.hkbuseta.common.utils.dispatcherIO
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


external fun getLocation(callback: (Double, Double) -> Unit, error: (Boolean) -> Unit)

actual fun checkLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    getLocation(
        callback = { _, _ -> callback.invoke(true) },
        error = { callback.invoke(!it) }
    )
}

actual fun checkBackgroundLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    callback.invoke(false)
}

actual fun getGPSLocation(appContext: AppContext): Deferred<LocationResult?> {
    val defer = CompletableDeferred<LocationResult?>()
    getLocation(
        callback = { lat, lng -> defer.complete(LocationResult.of(lat, lng)) },
        error = { defer.complete(if (it) null else LocationResult.FAILED_RESULT) }
    )
    return defer
}

actual fun getGPSLocation(appContext: AppContext, interval: Long, listener: (LocationResult) -> Unit): Deferred<() -> Unit> {
    val job = CoroutineScope(dispatcherIO).launch {
        while (true) {
            getLocation(
                callback = { lat, lng -> listener.invoke(LocationResult.of(lat, lng)) },
                error = { /* do nothing */ }
            )
            delay(interval)
        }
    }
    return CompletableDeferred { job.cancel() }
}