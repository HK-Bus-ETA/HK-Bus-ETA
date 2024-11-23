package com.loohp.hkbuseta.common.utils


inline fun ignoreExceptions(printStackTract: Boolean = false, block: () -> Unit) {
    try {
        block.invoke()
    } catch (e: Throwable) {
        if (printStackTract) {
            e.printStackTrace()
        }
    }
}

inline fun <T> resultOrNull(printStackTract: Boolean = false, block: () -> T): T? {
    return try {
        block.invoke()
    } catch (e: Throwable) {
        if (printStackTract) {
            e.printStackTrace()
        }
        null
    }
}