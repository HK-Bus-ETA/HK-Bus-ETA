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
package com.loohp.hkbuseta.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.benasher44.uuid.Uuid
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.appcontext.AppContextAndroid

fun checkNotificationPermission(appContext: AppContext, askIfNotGranted: Boolean): Boolean {
    return checkNotificationPermission(appContext, askIfNotGranted) { }
}

fun checkNotificationPermission(appContext: AppContext, callback: (Boolean) -> Unit): Boolean {
    return checkNotificationPermission(appContext, true, callback)
}

private fun checkNotificationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        callback.invoke(true)
        return true
    }
    val context = (appContext as AppContextAndroid).context
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        callback.invoke(true)
        return true
    }
    if (askIfNotGranted && context is ComponentActivity) {
        var ref: ActivityResultLauncher<String>? = null
        val launcher: ActivityResultLauncher<String> = context.activityResultRegistry.register(Uuid.randomUUID().toString(), ActivityResultContracts.RequestPermission()) {
            Firebase.analytics.logEvent("notification_request_result", Bundle().apply {
                putBoolean("value", it)
            })
            callback.invoke(it)
            ref?.unregister()
        }
        ref = launcher
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    return false
}
