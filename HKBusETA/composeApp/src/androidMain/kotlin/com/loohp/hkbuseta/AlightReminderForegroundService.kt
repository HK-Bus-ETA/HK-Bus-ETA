/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.benasher44.uuid.uuid4
import com.loohp.hkbuseta.common.services.AlightReminderActiveState
import com.loohp.hkbuseta.common.services.AlightReminderRemoteData
import com.loohp.hkbuseta.common.services.AlightReminderService
import com.loohp.hkbuseta.common.services.AlightReminderServiceState
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class NotificationData(
    val title: String,
    val content: String,
    val url: String,
    val state: AlightReminderServiceState
)

class AlightReminderForegroundService: Service() {

    private val listenerId = uuid4().toString()
    private var notificationData: NotificationData? = null
    private var remoteData: AlightReminderRemoteData? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!AlightReminderService.updateSubscribes.containsKey(listenerId)) {
            start()
            AlightReminderService.updateSubscribes[listenerId] = { update() }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun buildNotificationData(service: AlightReminderService): NotificationData {
        val title = "${service.titleLeading} - ${service.titleTrailing}"
        val content = service.content
        val url = service.deepLink
        val state = service.state
        return NotificationData(title, content, url, state)
    }

    private fun buildNotification(notificationData: NotificationData, notify: Boolean): Notification {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(Uri.parse(notificationData.url))
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "alight_reminder_channel")
            .setSmallIcon(R.mipmap.icon_launcher)
            .setContentTitle(notificationData.title)
            .setContentText(notificationData.content)
            .setOngoing(notificationData.state != AlightReminderServiceState.ARRIVED)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(!notify)
            .build()
    }

    private fun start() {
        notificationData = null
        AlightReminderService.currentInstance.valueNullable?.let {
            startForeground(1, buildNotification(buildNotificationData(it).apply { notificationData = this }, true))
            RemoteActivityUtils.dataToWatch(this, Shared.RESPONSE_ALIGHT_REMINDER_ID, it.remoteData.serialize())
            remoteData = it.remoteData
        }?: terminate()
    }

    private fun update() {
        AlightReminderService.currentInstance.valueNullable?.let {
            val currentNotificationData = buildNotificationData(it)
            if (currentNotificationData != notificationData) {
                val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(1, buildNotification(currentNotificationData, currentNotificationData.state != notificationData?.state))
                notificationData = currentNotificationData
            }
            val currentRemoteData = it.remoteData
            if (remoteData != currentRemoteData) {
                RemoteActivityUtils.dataToWatch(this, if (remoteData == null) Shared.RESPONSE_ALIGHT_REMINDER_ID else Shared.UPDATE_ALIGHT_REMINDER_ID, currentRemoteData.serialize())
                remoteData = currentRemoteData
            }
            if (it.isActive == AlightReminderActiveState.STOPPED) {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    terminate()
                }
            }
        }?: terminate()
    }

    private fun terminate() {
        AlightReminderService.updateSubscribes.remove(listenerId)
        RemoteActivityUtils.dataToWatch(this, Shared.UPDATE_ALIGHT_REMINDER_ID, null)
        stopSelf()
    }

}