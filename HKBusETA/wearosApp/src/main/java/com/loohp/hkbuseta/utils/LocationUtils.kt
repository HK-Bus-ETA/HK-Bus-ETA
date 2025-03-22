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
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.GPSLocation
import com.loohp.hkbuseta.common.utils.LocationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


fun checkLocationPermission(appContext: AppContext, askIfNotGranted: Boolean): Boolean {
    return checkLocationPermission(appContext, askIfNotGranted) { }
}

fun checkLocationPermission(appContext: AppContext, callback: (Boolean) -> Unit): Boolean {
    return checkLocationPermission(appContext, true, callback)
}

@OptIn(ExperimentalUuidApi::class)
private fun checkLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit): Boolean {
    val context = appContext.context
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        callback.invoke(true)
        return true
    }
    if (askIfNotGranted && context is ComponentActivity) {
        var ref: ActivityResultLauncher<String>? = null
        val launcher: ActivityResultLauncher<String> = context.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) {
            Firebase.analytics.logEvent("location_request_result", Bundle().apply {
                putBoolean("value", it)
            })
            callback.invoke(it)
            ref?.unregister()
        }
        ref = launcher
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        callback.invoke(false)
    }
    return false
}

fun checkBackgroundLocationPermission(appContext: AppContext, askIfNotGranted: Boolean): Boolean {
    return checkBackgroundLocationPermission(appContext, askIfNotGranted) { }
}

fun checkBackgroundLocationPermission(appContext: AppContext, callback: (Boolean) -> Unit): Boolean {
    return checkBackgroundLocationPermission(appContext, true, callback)
}

@OptIn(ExperimentalUuidApi::class)
private fun checkBackgroundLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit): Boolean {
    val context = appContext.context
    if ((ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        callback.invoke(true)
        return true
    }
    if (askIfNotGranted && appContext is AppActiveContext) {
        val activity = context as ComponentActivity
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            var ref0: ActivityResultLauncher<String>? = null
            val launcher0: ActivityResultLauncher<String> = activity.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) { result0: Boolean ->
                Firebase.analytics.logEvent("background_location_request_result", Bundle().apply {
                    putBoolean("value", result0)
                })
                callback.invoke(result0)
                ref0?.unregister()
            }
            ref0 = launcher0
            launcher0.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            var ref: ActivityResultLauncher<String>? = null
            val launcher: ActivityResultLauncher<String> = activity.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) { result: Boolean ->
                Firebase.analytics.logEvent("location_request_result", Bundle().apply {
                    putBoolean("value", result)
                })
                if (result) {
                    var ref0: ActivityResultLauncher<String>? = null
                    val launcher0: ActivityResultLauncher<String> = activity.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) { result0: Boolean ->
                        Firebase.analytics.logEvent("background_location_request_result", Bundle().apply {
                            putBoolean("value", result0)
                        })
                        callback.invoke(result0)
                        ref0?.unregister()
                    }
                    ref0 = launcher0
                    Thread {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1500)
                        } catch (ignore: InterruptedException) {
                        }
                        launcher0.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
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
    return false
}

@OptIn(ExperimentalUuidApi::class)
private fun checkLocationPermission(appActiveContext: AppActiveContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit): Boolean {
    val activity = appActiveContext.context
    if ((ActivityCompat.checkSelfPermission(activity,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        callback.invoke(true)
        return true
    }
    if (askIfNotGranted) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            var ref0: ActivityResultLauncher<String>? = null
            val launcher0: ActivityResultLauncher<String> = activity.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) { result0: Boolean ->
                Firebase.analytics.logEvent("background_location_request_result", Bundle().apply {
                    putBoolean("value", result0)
                })
                callback.invoke(result0)
                ref0?.unregister()
            }
            ref0 = launcher0
            launcher0.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            var ref: ActivityResultLauncher<String>? = null
            val launcher: ActivityResultLauncher<String> = activity.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) { result: Boolean ->
                Firebase.analytics.logEvent("location_request_result", Bundle().apply {
                    putBoolean("value", result)
                })
                if (result) {
                    var ref0: ActivityResultLauncher<String>? = null
                    val launcher0: ActivityResultLauncher<String> = activity.activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.RequestPermission()) { result0: Boolean ->
                        Firebase.analytics.logEvent("background_location_request_result", Bundle().apply {
                            putBoolean("value", result0)
                        })
                        callback.invoke(result0)
                        ref0?.unregister()
                    }
                    ref0 = launcher0
                    Thread {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1500)
                        } catch (ignore: InterruptedException) {
                        }
                        launcher0.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }.start()
                    ref?.unregister()
                } else {
                    callback.invoke(false)
                }
            }
            ref = launcher
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    return false
}

fun getGPSLocation(appContext: AppContext): Deferred<LocationResult?> {
    if (!checkLocationPermission(appContext, false)) {
        return CompletableDeferred(null)
    }
    val context = appContext.context
    val future = CompletableFuture<LocationResult?>()
    val client = LocationServices.getFusedLocationProviderClient(context)
    CoroutineScope(Dispatchers.IO).launch {
        client.locationAvailability.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result.isLocationAvailable) {
                client.getCurrentLocation(CurrentLocationRequest.Builder().setMaxUpdateAgeMillis(6000).setDurationMillis(60000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build(), null).addOnCompleteListener { future.complete(
                    LocationResult.fromTask(it)) }
            } else {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, Dispatchers.IO.asExecutor()) { future.complete(
                        LocationResult.fromLocationNullable(it)) }
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, Dispatchers.IO.asExecutor()) { future.complete(
                        LocationResult.fromLocationNullable(it)) }
                } else {
                    future.complete(LocationResult.FAILED_RESULT)
                }
            }
        }
    }
    return future.asDeferred()
}

inline val Location.optAltitude: Double? get() = altitude.takeIf { hasAltitude() }
inline val Location.optBearing: Float? get() = bearing.takeIf { hasBearing() }

fun LocationResult.Companion.fromTask(task: Task<Location?>): LocationResult {
    return if (!task.isSuccessful) FAILED_RESULT else fromLocationNullable(task.result)
}

fun LocationResult.Companion.fromLocationNullable(location: Location?): LocationResult {
    return LocationResult(if (location == null) null else GPSLocation(location.latitude, location.longitude, location.optAltitude, location.optBearing))
}
