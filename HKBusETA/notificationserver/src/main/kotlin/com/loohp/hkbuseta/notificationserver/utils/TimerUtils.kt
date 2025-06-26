package com.loohp.hkbuseta.notificationserver.utils

import java.util.TimerTask


inline fun TimerTask(crossinline block: () -> Unit): TimerTask {
    return object : TimerTask() { override fun run() { block.invoke() } }
}