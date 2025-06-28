package com.loohp.hkbuseta.utils

import android.content.Context
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability


fun isHuaweiDevice(): Boolean {
    return Build.MANUFACTURER.lowercase().contains("huawei") || Build.BRAND.lowercase().contains("huawei")
}

fun hasGooglePlayService(context: Context): Boolean {
    return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}