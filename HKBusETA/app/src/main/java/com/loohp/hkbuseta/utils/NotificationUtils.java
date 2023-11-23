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
import android.os.Build;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class NotificationUtils {

    public static boolean checkNotificationPermission(Context context, boolean askIfNotGranted) {
        return checkNotificationPermission(context, askIfNotGranted, r -> {});
    }

    public static boolean checkNotificationPermission(Context context, Consumer<Boolean> callback) {
        return checkNotificationPermission(context, true, callback);
    }

    private static boolean checkNotificationPermission(Context context, boolean askIfNotGranted, Consumer<Boolean> callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            callback.accept(true);
            return true;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            callback.accept(true);
            return true;
        }
        if (askIfNotGranted && context instanceof ComponentActivity) {
            ComponentActivity activity = (ComponentActivity) context;
            AtomicReference<ActivityResultLauncher<String>> ref = new AtomicReference<>();
            ActivityResultLauncher<String> launcher = activity.getActivityResultRegistry().register(UUID.randomUUID().toString(), new ActivityResultContracts.RequestPermission(), result -> {
                Bundle bundle = new Bundle();
                bundle.putBoolean("value", result);
                FirebaseAnalytics.getInstance(context).logEvent("notification_request_result", bundle);
                callback.accept(result);
                ref.get().unregister();
            });
            ref.set(launcher);
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        return false;
    }

}
