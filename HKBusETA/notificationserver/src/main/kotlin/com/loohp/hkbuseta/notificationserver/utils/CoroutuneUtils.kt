package com.loohp.hkbuseta.notificationserver.utils

import kotlinx.coroutines.delay


suspend inline fun awaitUntil(
    pollInterval: Long = 50,
    onPollFailed: () -> Unit = { /* do nothing */ },
    predicate: () -> Boolean
) {
    val delay = pollInterval.coerceAtLeast(1)
    while (!predicate.invoke()) {
        onPollFailed.invoke()
        delay(delay)
    }
}

suspend inline fun <T> retryUntil(
    retry: Long = 10000,
    maxTries: Int = 50,
    block: () -> T,
    predicate: (T) -> Boolean,
    fallbackValue: T
): T {
    for (i in 0 until maxTries) {
        val value = try {
            block.invoke()
        } catch (_: Throwable) {
            null
        }
        if (value != null && predicate.invoke(value)) {
            return value
        }
        delay(retry)
    }
    return fallbackValue
}