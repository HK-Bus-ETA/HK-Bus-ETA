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