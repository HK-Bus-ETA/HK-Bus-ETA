package com.loohp.hkbuseta.common.utils

actual fun <T : Number> T.toString(decimalPlaces: Int): String {
    return toFixed(toDouble(), decimalPlaces)
}

@Suppress("unused")
private fun toFixed(value: Double, decimalPlaces: Int): String = js("value.toFixed(decimalPlaces)")