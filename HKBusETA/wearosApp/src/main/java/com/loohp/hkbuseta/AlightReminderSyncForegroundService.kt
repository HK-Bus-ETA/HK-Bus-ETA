/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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

package com.loohp.hkbuseta

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.loohp.hkbuseta.common.services.AlightReminderActiveState
import com.loohp.hkbuseta.common.services.AlightReminderRemoteData
import com.loohp.hkbuseta.common.services.AlightReminderServiceState
import com.loohp.hkbuseta.common.utils.Closeable
import com.loohp.hkbuseta.shared.WearOSShared.remoteAlightReminderService


data class NotificationData(
    val title: String,
    val content: String,
    val state: AlightReminderServiceState
)

class AlightReminderSyncForegroundService: Service() {

    private var closeable: Closeable? = null
    private var notificationData: NotificationData? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (closeable == null) {
            start()
            remoteAlightReminderService.watch { update() }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun buildNotificationData(service: AlightReminderRemoteData): NotificationData {
        val title = service.titleLeading
        val content = "${service.titleTrailing}\n${service.content}"
        val state = service.state
        return NotificationData(title, content, state)
    }

    private fun buildNotification(notificationData: NotificationData, notify: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "alight_reminder_channel")
            .setSmallIcon(R.mipmap.icon_circle)
            .setContentTitle(notificationData.title)
            .setContentText(notificationData.content)
            .setOngoing(notificationData.state != AlightReminderServiceState.ARRIVED)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(!notify)
            .build()
    }

    private fun start() {
        notificationData = null
        remoteAlightReminderService.valueNullable?.let {
            startForeground(1, buildNotification(buildNotificationData(it).apply { notificationData = this }, true))
        }?: terminate()
    }

    private fun update() {
        remoteAlightReminderService.valueNullable?.let {
            val currentNotificationData = buildNotificationData(it)
            if (currentNotificationData != notificationData) {
                val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(1, buildNotification(currentNotificationData, currentNotificationData.state != notificationData?.state))
                notificationData = currentNotificationData
            }
            if (it.active != AlightReminderActiveState.ACTIVE) {
                terminate()
            }
        }?: terminate()
    }

    private fun terminate() {
        stopForeground(if (remoteAlightReminderService.valueNullable?.active == AlightReminderActiveState.ARRIVED) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
        closeable?.close()
        closeable = null
        stopSelf()
    }

}