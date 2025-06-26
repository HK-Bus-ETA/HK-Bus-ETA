package com.loohp.hkbuseta.notificationserver.utils

import kotlinx.coroutines.delay


suspend fun retryUntilTrue(predicate: suspend () -> Boolean) {
    while (!predicate.invoke()) {
        delay(10)
    }
}