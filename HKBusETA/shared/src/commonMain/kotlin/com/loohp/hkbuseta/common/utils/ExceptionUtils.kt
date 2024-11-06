package com.loohp.hkbuseta.common.utils


inline fun ignoreExceptions(block: () -> Unit) {
    try {
        block.invoke()
    } catch (_: Throwable) {
        //do nothing
    }
}