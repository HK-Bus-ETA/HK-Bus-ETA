/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.TimeSource
import com.loohp.hkbuseta.common.utils.hongKongZoneId
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone


class HongKongTimeSource(private val timeFormat: String) : TimeSource {

    override val currentTime: String @Composable get() {
        return currentTime({ System.currentTimeMillis() }, timeFormat).value
    }

}

@Composable
private fun currentTime(time: () -> Long, timeFormat: String): State<String> {
    val calendar = remember { Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of(hongKongZoneId))) }
    var currentTime by remember { mutableLongStateOf(time.invoke()) }
    val timeText = remember { derivedStateOf { formatTime(calendar, currentTime, timeFormat) } }

    val context = LocalContext.current
    val updatedTimeLambda by rememberUpdatedState(time)

    DisposableEffect(context, updatedTimeLambda) {
        val receiver = TimeBroadcastReceiver { currentTime = updatedTimeLambda() }
        receiver.register(context)
        onDispose { receiver.unregister(context) }
    }

    return timeText
}

private class TimeBroadcastReceiver(val onTimeChanged: () -> Unit) : BroadcastReceiver() {

    private var registered = false

    override fun onReceive(context: Context, intent: Intent) {
        onTimeChanged.invoke()
    }

    fun register(context: Context) {
        if (!registered) {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_TIME_TICK)
            filter.addAction(Intent.ACTION_TIME_CHANGED)
            context.registerReceiver(this, filter)
            registered = true
        }
    }

    fun unregister(context: Context) {
        if (registered) {
            context.unregisterReceiver(this)
            registered = false
        }
    }

}

private fun formatTime(calendar: Calendar, currentTime: Long, timeFormat: String): String {
    calendar.timeInMillis = currentTime
    return DateFormat.format(timeFormat, calendar).toString()
}