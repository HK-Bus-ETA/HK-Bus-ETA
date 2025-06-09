package com.loohp.hkbuseta.common.utils

actual fun <T : Number> T.toString(decimalPlaces: Int): String {
    return "%.${decimalPlaces}f".format(toDouble())
}