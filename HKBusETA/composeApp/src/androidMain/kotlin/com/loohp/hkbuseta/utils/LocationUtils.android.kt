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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.GPSLocation
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.LocationResult
import com.loohp.hkbuseta.common.utils.ignoreExceptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


actual val shouldRecordLastLocation: Boolean = false

@OptIn(ExperimentalUuidApi::class)
actual fun checkLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    val context = appContext.context
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        callback.invoke(true)
        return
    }
    if (askIfNotGranted && context is ComponentActivity) {
        var ref: ActivityResultLauncher<String>? = null
        val launcher: ActivityResultLauncher<String> = context.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) {
            callback.invoke(it)
            ref?.unregister()
        }
        ref = launcher
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        callback.invoke(false)
    }
}

@OptIn(ExperimentalUuidApi::class)
actual fun checkBackgroundLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    val context = appContext.context
    if ((ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        callback.invoke(true)
        return
    }
    if (askIfNotGranted && appContext is AppActiveContext) {
        val activity = context as ComponentActivity
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            var ref0: ActivityResultLauncher<String>? = null
            val launcher0: ActivityResultLauncher<String> = activity.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) { result0: Boolean ->
                callback.invoke(result0)
                ref0?.unregister()
            }
            ref0 = launcher0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                launcher0.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                launcher0.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            var ref: ActivityResultLauncher<String>? = null
            val launcher: ActivityResultLauncher<String> = activity.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) { result: Boolean ->
                if (result) {
                    var ref0: ActivityResultLauncher<String>? = null
                    val launcher0: ActivityResultLauncher<String> = activity.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) { result0: Boolean ->
                        callback.invoke(result0)
                        ref0?.unregister()
                    }
                    ref0 = launcher0
                    Thread {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1500)
                        } catch (ignore: InterruptedException) {
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            launcher0.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            launcher0.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }.start()
                    ref?.unregister()
                } else {
                    callback.invoke(false)
                }
            }
            ref = launcher
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    } else {
        callback.invoke(false)
    }
}

actual fun getGPSLocationUnrecorded(appContext: AppContext, priority: LocationPriority): Deferred<LocationResult?> {
    val defer = CompletableDeferred<LocationResult?>()
    checkLocationPermission(appContext, false) { permission ->
        if (permission) {
            val context = appContext.context
            val client = LocationServices.getFusedLocationProviderClient(context)
            CoroutineScope(Dispatchers.IO).launch {
                val withoutGMS = {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, Dispatchers.IO.asExecutor()) { defer.complete(LocationResult.fromLocationNullable(it)) }
                        } else {
                            @Suppress("DEPRECATION")
                            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, object : LocationListener {
                                override fun onLocationChanged(location: Location) {
                                    defer.complete(LocationResult.fromLocationNullable(location))
                                }
                                @Deprecated("Deprecated in Java")
                                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                                }
                            }, null)
                        }
                    } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, Dispatchers.IO.asExecutor()) { defer.complete(LocationResult.fromLocationNullable(it)) }
                        } else {
                            @Suppress("DEPRECATION")
                            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, object : LocationListener {
                                override fun onLocationChanged(location: Location) {
                                    defer.complete(LocationResult.fromLocationNullable(location))
                                }
                                @Deprecated("Deprecated in Java")
                                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                                }
                            }, null)
                        }
                    } else {
                        defer.complete(LocationResult.FAILED_RESULT)
                    }
                }
                if (!hasGooglePlayService(context)) {
                    withoutGMS.invoke()
                } else {
                    client.locationAvailability.addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result.isLocationAvailable) {
                            client.getCurrentLocation(
                                CurrentLocationRequest.Builder()
                                    .setMaxUpdateAgeMillis(priority.toMaxUpdateAgeMillis())
                                    .setDurationMillis(60000)
                                    .setPriority(priority.toGMSPriority())
                                    .build(),
                                null
                            ).addOnCompleteListener { defer.complete(LocationResult.fromTask(it)) }
                        } else {
                            withoutGMS.invoke()
                        }
                    }
                }
            }
        } else {
            defer.complete(null)
        }
    }
    return defer
}

actual fun getGPSLocationUnrecorded(appContext: AppContext, interval: Long, listener: (LocationResult) -> Unit): Deferred<() -> Unit> {
    val defer = CompletableDeferred<() -> Unit>()
    checkLocationPermission(appContext, false) { permission ->
        if (permission) {
            val context = appContext.context
            val client = LocationServices.getFusedLocationProviderClient(context)
            CoroutineScope(Dispatchers.IO).launch {
                val callback: (Location) -> Unit = { listener.invoke(LocationResult.fromLocationNullable(it)) }
                val withoutGMS = {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LocationRequest.Builder(interval).setQuality(LocationRequest.QUALITY_HIGH_ACCURACY).setMaxUpdateDelayMillis(2000).build(), Dispatchers.IO.asExecutor(), callback)
                        } else {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0F, callback)
                        }
                        defer.complete { locationManager.removeUpdates(callback) }
                    } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LocationRequest.Builder(interval).setQuality(LocationRequest.QUALITY_HIGH_ACCURACY).setMaxUpdateDelayMillis(2000).build(), Dispatchers.IO.asExecutor(), callback)
                        } else {
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0F, callback)
                        }
                        defer.complete { locationManager.removeUpdates(callback) }
                    } else {
                        defer.complete { /* do nothing */ }
                    }
                }
                if (!hasGooglePlayService(context)) {
                    withoutGMS.invoke()
                } else {
                    client.locationAvailability.addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result.isLocationAvailable) {
                            client.requestLocationUpdates(com.google.android.gms.location.LocationRequest.Builder(interval).setMaxUpdateAgeMillis(2000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build(), Dispatchers.IO.asExecutor(), callback)
                            defer.complete { client.removeLocationUpdates(callback) }
                        } else {
                            withoutGMS.invoke()
                        }
                    }
                }
            }
        } else {
            defer.complete { /* do nothing */ }
        }
    }
    return defer
}

inline val Location.optAltitude: Double? get() = altitude.takeIf { hasAltitude() }
inline val Location.optBearing: Float? get() = bearing.takeIf { hasBearing() }

fun LocationResult.Companion.fromTask(task: Task<Location?>): LocationResult {
    return if (!task.isSuccessful) FAILED_RESULT else fromLocationNullable(task.result)
}

fun LocationResult.Companion.fromLocationNullable(location: Location?): LocationResult {
    return LocationResult(if (location == null) null else GPSLocation(location.latitude, location.longitude, location.optAltitude, location.optBearing))
}

fun LocationPriority.toMaxUpdateAgeMillis(): Long {
    return when (this) {
        LocationPriority.FASTEST -> 12000
        LocationPriority.FASTER -> 6000
        LocationPriority.ACCURATE, LocationPriority.MOST_ACCURATE -> 2000
    }
}

fun LocationPriority.toGMSPriority(): Int {
    return when (this) {
        LocationPriority.FASTEST, LocationPriority.FASTER -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        LocationPriority.ACCURATE, LocationPriority.MOST_ACCURATE -> Priority.PRIORITY_HIGH_ACCURACY
    }
}

actual fun isGPSServiceEnabled(appContext: AppContext, notifyUser: Boolean): Boolean {
    val locationManager = appContext.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    ignoreExceptions {
        val enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (enabled) {
            return true
        }
    }
    if (notifyUser) {
        appContext.showToastText(if (Shared.language == "en") "Unable to read your location" else "無法讀取你的位置", ToastDuration.LONG)
    }
    return false
}