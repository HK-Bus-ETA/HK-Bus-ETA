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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.glance.appwidget.updateAll
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.componentActivityPaused
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.appcontext.setApplicationContext
import com.loohp.hkbuseta.appcontext.setComponentActivity
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.external.extractShareLink
import com.loohp.hkbuseta.common.external.shareLaunch
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Tiles
import com.loohp.hkbuseta.common.utils.remove
import com.loohp.hkbuseta.glance.FavouriteRoutesWidget
import com.loohp.hkbuseta.shared.AndroidShared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var pipModeState: PipModeState = PipModeState.Left

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = Firebase.analytics

        setApplicationContext(applicationContext)
        setComponentActivity(this)

        AndroidShared.setDefaultExceptionHandler()
        AndroidShared.scheduleBackgroundUpdateService(this)
        Shared.provideBackgroundUpdateScheduler { c, t -> AndroidShared.scheduleBackgroundUpdateService(c.context, t) }
        Tiles.providePlatformUpdate {
            CoroutineScope(Dispatchers.Main).launch {
                FavouriteRoutesWidget.updateAll(this@MainActivity)
            }
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val alightReminderChannel = NotificationChannel(
            "alight_reminder_channel",
            resources.getString(R.string.alight_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(alightReminderChannel)

        val alertChannel = NotificationChannel(
            "alert_channel",
            resources.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(alertChannel)

        val generalChannel = NotificationChannel(
            "general_channel",
            resources.getString(R.string.general_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(generalChannel)

        Firebase.messaging.subscribeToTopic("Alert")
        Firebase.messaging.subscribeToTopic("General")
        Firebase.messaging.subscribeToTopic("Refresh")

        enableEdgeToEdge()

        setContent {
            App()
        }
        CoroutineScope(Dispatchers.IO).launch {
            intent.extractUrl()?.extractShareLink()?.apply {
                val instance = HistoryStack.historyStack.value.last()
                instance.startActivity(AppIntent(instance, AppScreen.MAIN))
                instance.finishAffinity()
                delay(500)
                shareLaunch(instance, true)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CoroutineScope(Dispatchers.IO).launch {
            intent.extractUrl()?.extractShareLink()?.apply {
                val instance = HistoryStack.historyStack.value.last()
                instance.startActivity(AppIntent(instance, AppScreen.MAIN))
                instance.finishAffinity()
                delay(500)
                shareLaunch(instance, true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        componentActivityPaused = true
    }

    override fun onResume() {
        super.onResume()
        componentActivityPaused = false
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        if (isInPictureInPictureMode) {
            pipModeState = PipModeState.Entered
        } else {
            val id = Random.nextInt()
            pipModeState = PipModeState.JustLeft(id)
            CoroutineScope(Dispatchers.IO).launch {
                delay(1500)
                if (pipModeState.matchesJustLeft(id)) {
                    pipModeState = PipModeState.Left
                }
            }
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        if (pipModeState is PipModeState.Left) {
            recreate()
        }
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
    }

}

internal fun Intent.extractUrl(): String? {
    return when (action) {
        Intent.ACTION_SEND -> if (type == "text/plain") getStringExtra(Intent.EXTRA_TEXT)?.remove("\n") else null
        Intent.ACTION_VIEW -> data.toString()
        else -> null
    }
}

private sealed interface PipModeState {
    data object Entered: PipModeState
    data class JustLeft(val id: Int): PipModeState
    data object Left: PipModeState
}

private fun PipModeState.matchesJustLeft(id: Int): Boolean {
    return this is PipModeState.JustLeft && this.id == id
}