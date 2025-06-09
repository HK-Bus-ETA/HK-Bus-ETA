package com.loohp.hkbuseta.common.utils

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun <T : Number> T.toString(decimalPlaces: Int): String {
    return NSString.stringWithFormat(format = "%.${decimalPlaces}f", toDouble())
}