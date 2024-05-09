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

import co.touchlab.stately.collections.ConcurrentMutableList
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.utils.LocationResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject


private val objectPreferenceStore: MutableList<Any> = ConcurrentMutableList()

actual fun checkLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    when (CLLocationManager.authorizationStatus()) {
        kCLAuthorizationStatusNotDetermined -> if (askIfNotGranted) {
            val locationManager = CLLocationManager()

            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
                    if (didChangeAuthorizationStatus != kCLAuthorizationStatusNotDetermined) {
                        when (didChangeAuthorizationStatus) {
                            kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> callback.invoke(true)
                            else -> callback.invoke(false)
                        }
                        objectPreferenceStore.remove(locationManager)
                        objectPreferenceStore.remove(this)
                    }
                }

                override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                    callback.invoke(false)
                    objectPreferenceStore.remove(locationManager)
                    objectPreferenceStore.remove(this)
                }
            }

            locationManager.delegate = delegate
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
            locationManager.requestWhenInUseAuthorization()
            objectPreferenceStore.add(locationManager)
            objectPreferenceStore.add(delegate)
        } else {
            callback.invoke(false)
        }
        kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> callback.invoke(true)
        else -> callback.invoke(false)
    }
}

actual fun checkBackgroundLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    when (CLLocationManager.authorizationStatus()) {
        kCLAuthorizationStatusNotDetermined, kCLAuthorizationStatusAuthorizedWhenInUse -> if (askIfNotGranted) {
            val locationManager = CLLocationManager()

            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
                    if (didChangeAuthorizationStatus != kCLAuthorizationStatusNotDetermined) {
                        when (didChangeAuthorizationStatus) {
                            kCLAuthorizationStatusAuthorizedAlways -> callback.invoke(true)
                            else -> callback.invoke(false)
                        }
                        objectPreferenceStore.remove(locationManager)
                        objectPreferenceStore.remove(this)
                    }
                }

                override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                    callback.invoke(false)
                    objectPreferenceStore.remove(locationManager)
                    objectPreferenceStore.remove(this)
                }
            }

            locationManager.delegate = delegate
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
            locationManager.requestAlwaysAuthorization()
            objectPreferenceStore.add(locationManager)
            objectPreferenceStore.add(delegate)
        } else {
            callback.invoke(false)
        }
        kCLAuthorizationStatusAuthorizedAlways -> callback.invoke(true)
        else -> callback.invoke(false)
    }
}

actual fun getGPSLocation(appContext: AppContext): Deferred<LocationResult?> {
    val defer = CompletableDeferred<LocationResult?>()
    checkLocationPermission(appContext, false) {
        if (it) {
            when (CLLocationManager.authorizationStatus()) {
                kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> {
                    val locationManager = CLLocationManager()

                    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                            val location = didUpdateLocations.lastOrNull() as? CLLocation
                            if (location != null) {
                                defer.complete(location.toLocationResult())
                            } else {
                                defer.complete(LocationResult.FAILED_RESULT)
                            }
                            locationManager.stopUpdatingLocation()
                            objectPreferenceStore.remove(locationManager)
                            objectPreferenceStore.remove(this)
                        }

                        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                            defer.complete(null)
                            objectPreferenceStore.remove(locationManager)
                            objectPreferenceStore.remove(this)
                        }
                    }

                    locationManager.delegate = delegate
                    locationManager.desiredAccuracy = kCLLocationAccuracyBest
                    locationManager.requestLocation()
                    objectPreferenceStore.add(locationManager)
                    objectPreferenceStore.add(delegate)
                }
                else -> defer.complete(null)
            }
        } else {
            defer.complete(null)
        }
    }
    return defer
}

actual fun getGPSLocation(appContext: AppContext, interval: Long, listener: (LocationResult) -> Unit): Deferred<() -> Unit> {
    val defer = CompletableDeferred<() -> Unit>()
    checkLocationPermission(appContext, false) {
        if (it) {
            when (CLLocationManager.authorizationStatus()) {
                kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> {
                    val locationManager = CLLocationManager()

                    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                            val location = didUpdateLocations.lastOrNull() as? CLLocation
                            if (location != null) {
                                listener.invoke(location.toLocationResult())
                            }
                        }
                    }

                    locationManager.delegate = delegate
                    locationManager.desiredAccuracy = kCLLocationAccuracyBest
                    locationManager.showsBackgroundLocationIndicator = true
                    locationManager.allowsBackgroundLocationUpdates = true
                    locationManager.startUpdatingLocation()
                    objectPreferenceStore.add(locationManager)
                    objectPreferenceStore.add(delegate)

                    defer.complete {
                        locationManager.stopUpdatingLocation()
                        objectPreferenceStore.remove(locationManager)
                        objectPreferenceStore.remove(delegate)
                    }
                }
                else -> defer.complete { /* do nothing */ }
            }
        } else {
            defer.complete { /* do nothing */ }
        }
    }
    return defer
}

@OptIn(ExperimentalForeignApi::class)
fun CLLocation.toLocationResult(): LocationResult {
    val (lat, lng) = coordinate.useContents { latitude to longitude }
    return LocationResult.of(lat, lng, altitude, course.toFloat())
}