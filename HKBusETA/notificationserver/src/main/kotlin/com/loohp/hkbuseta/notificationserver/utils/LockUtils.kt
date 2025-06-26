package com.loohp.hkbuseta.notificationserver.utils

import java.util.concurrent.locks.Lock


inline fun <T> Lock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}