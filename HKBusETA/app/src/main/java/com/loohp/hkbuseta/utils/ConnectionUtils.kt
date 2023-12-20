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

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType


private val CONNECTION_TYPE_VALUES = ConnectionType.entries.toTypedArray()

@SuppressLint("WrongConstant")
fun Context.getConnectionType(): ConnectionType {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
    if (capabilities != null) {
        for (type in CONNECTION_TYPE_VALUES) {
            if (capabilities.hasTransport(type.transportType)) {
                return type
            }
        }
    }
    return ConnectionType.NONE
}

fun Context.currentBackgroundRestricted(): BackgroundRestrictionType {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (cm.isActiveNetworkMetered && cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
        return BackgroundRestrictionType.RESTRICT_BACKGROUND_STATUS
    }
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    if (pm.isPowerSaveMode && !pm.isIgnoringBatteryOptimizations(packageName)) {
        return BackgroundRestrictionType.POWER_SAVE_MODE
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && pm.isLowPowerStandbyEnabled && (Build.VERSION.SDK_INT < 34 || !pm.isExemptFromLowPowerStandby)) {
        BackgroundRestrictionType.LOW_POWER_STANDBY
    } else BackgroundRestrictionType.NONE
}

enum class ConnectionType(val transportType: Int) {

    CELLULAR(NetworkCapabilities.TRANSPORT_CELLULAR),
    WIFI(NetworkCapabilities.TRANSPORT_WIFI),
    BLUETOOTH(NetworkCapabilities.TRANSPORT_BLUETOOTH),
    ETHERNET(NetworkCapabilities.TRANSPORT_ETHERNET),
    VPN(NetworkCapabilities.TRANSPORT_VPN),
    @SuppressLint("InlinedApi")
    USB(NetworkCapabilities.TRANSPORT_USB),
    NONE(-1);

    fun hasConnection(): Boolean {
        return transportType >= 0
    }
}
