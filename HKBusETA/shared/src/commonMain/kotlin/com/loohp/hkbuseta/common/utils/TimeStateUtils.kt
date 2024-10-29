package com.loohp.hkbuseta.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime


private val minuteState: MutableStateFlow<LocalDateTime> = MutableStateFlow(currentLocalDateTime())
private val minuteStateJob: Job = CoroutineScope(Dispatchers.IO).launch {
    while (true) {
        minuteState.value = currentLocalDateTime().let { LocalDateTime(it.year, it.month, it.dayOfMonth, it.hour, it.minute) }
        delay(1000)
    }
}

val currentMinuteState: StateFlow<LocalDateTime> get() = minuteState