package com.loohp.hkbuseta.utils

fun Int.formatDecimalSeparator(): String {
    return toString().reversed().chunked(3).joinToString(",").reversed()
}