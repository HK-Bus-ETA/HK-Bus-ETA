/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

package com.loohp.hkbuseta.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.runtime.Immutable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.loohp.hkbuseta.objects.Coordinates;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class LocationUtils {

    public static boolean checkLocationPermission(Context context, boolean askIfNotGranted) {
        return checkLocationPermission(context, askIfNotGranted, r -> {});
    }

    public static boolean checkLocationPermission(Context context, Consumer<Boolean> callback) {
        return checkLocationPermission(context, true, callback);
    }

    private static boolean checkLocationPermission(Context context, boolean askIfNotGranted, Consumer<Boolean> callback) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            callback.accept(true);
            return true;
        }
        if (askIfNotGranted && context instanceof ComponentActivity) {
            ComponentActivity activity = (ComponentActivity) context;
            AtomicReference<ActivityResultLauncher<String>> ref = new AtomicReference<>();
            ActivityResultLauncher<String> launcher = activity.getActivityResultRegistry().register(UUID.randomUUID().toString(), new ActivityResultContracts.RequestPermission(), result -> {
                callback.accept(result);
                ref.get().unregister();
            });
            ref.set(launcher);
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        return false;
    }

    public static Future<LocationResult> getGPSLocation(Context context) {
        if (!checkLocationPermission(context, false)) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<LocationResult> future = new CompletableFuture<>();
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        ForkJoinPool.commonPool().execute(() -> {
            client.getLocationAvailability().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().isLocationAvailable()) {
                    client.getCurrentLocation(new CurrentLocationRequest.Builder()
                            .setMaxUpdateAgeMillis(2000)
                            .setDurationMillis(60000)
                            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                            .build(),
                            null
                    ).addOnCompleteListener(t -> future.complete(LocationResult.fromTask(t)));
                } else {
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, ForkJoinPool.commonPool(), loc -> future.complete(LocationResult.fromLocationNullable(loc)));
                    } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, ForkJoinPool.commonPool(), loc -> future.complete(LocationResult.fromLocationNullable(loc)));
                    } else {
                        future.complete(LocationResult.FAILED_RESULT);
                    }
                }
            });
        });
        return future;
    }

    @Immutable
    public static class LocationResult {

        public static final LocationResult FAILED_RESULT = new LocationResult(null);

        public static LocationResult fromTask(Task<Location> task) {
            if (!task.isSuccessful()) {
                return FAILED_RESULT;
            }
            return fromLocationNullable(task.getResult());
        }

        public static LocationResult fromLatLng(double lat, double lng) {
            return new LocationResult(new Coordinates(lat, lng));
        }

        public static LocationResult fromLocationNullable(Location location) {
            return new LocationResult(location == null ? null : new Coordinates(location.getLatitude(), location.getLongitude()));
        }
        
        public static LocationResult fromStopLocationNullable(Coordinates location) {
            return new LocationResult(location);
        }

        private final Coordinates location;

        private LocationResult(Coordinates location) {
            this.location = location;
        }

        public boolean isSuccess() {
            return location != null;
        }

        public Coordinates getLocation() {
            return location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LocationResult that = (LocationResult) o;
            return Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location);
        }
    }

}
