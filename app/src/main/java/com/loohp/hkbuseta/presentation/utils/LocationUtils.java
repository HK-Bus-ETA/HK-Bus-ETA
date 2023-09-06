package com.loohp.hkbuseta.presentation.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class LocationUtils {

    public static boolean checkLocationPermission(Activity activity, boolean askIfNotGranted) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (askIfNotGranted) {
            ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 99);
        }
        return false;
    }

    public static CompletableFuture<LocationResult> getGPSLocation(Activity activity) {
        if (!checkLocationPermission(activity, false)) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<LocationResult> future = new CompletableFuture<>();
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(activity);
        ForkJoinPool.commonPool().execute(() -> {
            client.getLocationAvailability().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().isLocationAvailable()) {
                    client.getCurrentLocation(new CurrentLocationRequest.Builder().build(), null).addOnCompleteListener(t -> future.complete(LocationResult.fromTask(t)));
                } else {
                    LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, ForkJoinPool.commonPool(), loc -> future.complete(LocationResult.ofNullable(loc)));
                    } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, ForkJoinPool.commonPool(), loc -> future.complete(LocationResult.ofNullable(loc)));
                    } else {
                        future.complete(LocationResult.FAILED_RESULT);
                    }
                }
            });
        });
        return future;
    }

    public static class LocationResult {

        public static final LocationResult FAILED_RESULT = new LocationResult(null);

        public static LocationResult fromTask(Task<Location> task) {
            if (!task.isSuccessful()) {
                return FAILED_RESULT;
            }
            return new LocationResult(task.getResult());
        }

        public static LocationResult ofNullable(Location location) {
            return new LocationResult(location);
        }

        private final Location location;

        private LocationResult(Location location) {
            this.location = location;
        }

        public boolean isSuccess() {
            return location != null;
        }

        public Location getLocation() {
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
