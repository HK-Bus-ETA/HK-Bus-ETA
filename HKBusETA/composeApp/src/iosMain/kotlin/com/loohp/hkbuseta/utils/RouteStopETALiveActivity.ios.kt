package com.loohp.hkbuseta.utils

import platform.Foundation.NSProcessInfo
import platform.Foundation.lowPowerModeEnabled

actual fun isLiveNotificationBackgroundUpdateSystemAllowed(): Boolean {
    return !NSProcessInfo.processInfo.lowPowerModeEnabled
}