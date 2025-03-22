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

import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.value
import co.touchlab.stately.concurrency.withLock
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.LocationResult
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.getCompletedOrNull
import com.loohp.hkbuseta.common.utils.getValue
import com.loohp.hkbuseta.common.utils.setValue
import kotlinx.coroutines.Deferred
import kotlin.concurrent.Volatile


data class LastLocationResult(
    val locationResult: LocationResult,
    val time: Long,
    val priority: LocationPriority
)

private val lastLocationLock: Lock = Lock()
private var lastLocationInternal: LastLocationResult? by AtomicReference(null)
val lastLocation: LocationResult? get() = lastLocationInternal?.locationResult

expect val shouldRecordLastLocation: Boolean

fun updateLocationLocation(location: LocationResult?, priority: LocationPriority = LocationPriority.ACCURATE) {
    if (shouldRecordLastLocation && location != null) {
        lastLocationLock.withLock {
            val now = currentTimeMillis()
            if (lastLocationInternal == null || lastLocationInternal?.let { it.priority <= priority } == true || now - (lastLocationInternal?.time?: 0) > 60000) {
                lastLocationInternal = LastLocationResult(location, now, priority)
            }
        }
    }
}

expect fun checkLocationPermission(appContext: AppContext, askIfNotGranted: Boolean = true, callback: (Boolean) -> Unit)

expect fun checkBackgroundLocationPermission(appContext: AppContext, askIfNotGranted: Boolean = true, callback: (Boolean) -> Unit)

expect fun getGPSLocationUnrecorded(appContext: AppContext, priority: LocationPriority = LocationPriority.ACCURATE): Deferred<LocationResult?>

expect fun getGPSLocationUnrecorded(appContext: AppContext, interval: Long, listener: (LocationResult) -> Unit): Deferred<() -> Unit>

fun getGPSLocation(appContext: AppContext, priority: LocationPriority = LocationPriority.ACCURATE): Deferred<LocationResult?> {
    return getGPSLocationUnrecorded(appContext, priority).apply {
        invokeOnCompletion { updateLocationLocation(getCompletedOrNull(), priority) }
    }
}

fun getGPSLocation(appContext: AppContext, interval: Long, listener: (LocationResult) -> Unit): Deferred<() -> Unit> {
    return getGPSLocationUnrecorded(appContext, interval) {
        listener.invoke(it)
        updateLocationLocation(it)
    }
}