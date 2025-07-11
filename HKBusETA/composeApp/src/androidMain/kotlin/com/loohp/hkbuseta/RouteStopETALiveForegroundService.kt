package com.loohp.hkbuseta

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.loohp.hkbuseta.common.utils.getValue
import com.loohp.hkbuseta.common.utils.setValue
import com.loohp.hkbuseta.utils.RouteStopETAData
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

private data class RouteStopETANotificationData(
    val title: String,
    val content: String,
    val url: String,
    val shortText: String,
    val color: Int
)

class RouteStopETALiveForegroundService: Service() {

    companion object {
        var INSTANCE by AtomicReference<RouteStopETALiveForegroundService?>(null)

        fun updateOrStart(data: RouteStopETAData?, context: Context) {
            INSTANCE?.run { update(data) }?: run {
                if (data != null) {
                    val intent = Intent(context, RouteStopETALiveForegroundService::class.java)
                    intent.putExtra("data", Json.encodeToString(data))
                    context.startService(intent)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        INSTANCE = this
        val data = Json.decodeFromString<RouteStopETAData>(intent!!.getStringExtra("data")!!)
        start(data)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun buildNotificationData(data: RouteStopETAData): RouteStopETANotificationData {
        val title = "${data.routeNumber} ${data.destination}"
        val content: String
        val shortText: String
        if (data.hasEta) {
            content = data.eta.joinToString(prefix = "${data.stop}\n", separator = " / ") { it }
            shortText = data.eta.first()
        } else {
            content = "${data.stop}\n${data.remark}"
            shortText = "\uD83D\uDD52"
        }
        val url = data.url
        val color = Color(data.color).toArgb()
        return RouteStopETANotificationData(title, content, url, shortText, color)
    }

    private fun buildNotification(notificationData: RouteStopETANotificationData): Notification {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(notificationData.url.toUri())
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "route_stop_eta_channel")
            .setSmallIcon(R.mipmap.icon_launcher)
            .setContentTitle(notificationData.title)
            .setContentText(notificationData.content)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(notificationData.shortText)
            .setColor(notificationData.color)
            .build()
    }

    private fun start(data: RouteStopETAData) {
        startForeground(1, buildNotification(buildNotificationData(data)))
    }

    private fun update(data: RouteStopETAData?) {
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (data == null) {
            stopSelf()
            mNotificationManager.cancel(1)
            INSTANCE = null
        } else {
            val notificationData = buildNotificationData(data)
            mNotificationManager.notify(1, buildNotification(notificationData))
        }
    }

}