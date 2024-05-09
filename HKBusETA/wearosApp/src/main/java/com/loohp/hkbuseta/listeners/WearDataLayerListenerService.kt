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
package com.loohp.hkbuseta.listeners

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.loohp.hkbuseta.MainActivity
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.services.AlightReminderActiveState
import com.loohp.hkbuseta.common.services.AlightReminderRemoteData
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class WearDataLayerListenerService : WearableListenerService() {

    @SuppressLint("WearRecents")
    override fun onMessageReceived(event: MessageEvent) {
        super.onMessageReceived(event)
        when (event.path) {
            Shared.START_ACTIVITY_ID -> {
                try {
                    val data = Json.decodeFromString<JsonObject>(String(event.data))
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    for ((key, value) in data) {
                        intent.putExtra(key, value.jsonPrimitive.content)
                    }
                    Firebase.analytics.logEvent("remote_launch", Bundle().apply {
                        putString("value", "1")
                    })
                    startActivity(intent)
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
            Shared.SYNC_PREFERENCES_ID -> {
                runBlocking(dispatcherIO) {
                    try {
                        val json = String(event.data)
                        if (Registry.isNewInstall(appContext)) {
                            Registry.writeRawPreference(json, appContext)
                        } else {
                            val preferences = Preferences.deserialize(Json.decodeFromString(json))
                            Registry.getInstanceNoUpdateCheck(appContext).syncPreference(appContext, preferences, false)
                        }
                    } catch (e: SerializationException) {
                        e.printStackTrace()
                    }
                }
            }
            Shared.REQUEST_PREFERENCES_ID -> {
                try {
                    RemoteActivityUtils.dataToPhone(this, Shared.SYNC_PREFERENCES_ID, runBlocking { Registry.getRawPreferences(appContext) })
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
            Shared.UPDATE_ALIGHT_REMINDER_ID -> {
                try {
                    if (event.data.isEmpty()) {
                        WearOSShared.remoteAlightReminderService.valueNullable = null
                    } else {
                        val json = String(event.data)
                        val remoteData = AlightReminderRemoteData.deserialize(Json.decodeFromString(json))
                        if (remoteData.active == AlightReminderActiveState.STOPPED) {
                            WearOSShared.remoteAlightReminderService.valueNullable = null
                        } else {
                            WearOSShared.remoteAlightReminderService.valueNullable = remoteData
                        }
                    }
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
            Shared.RESPONSE_ALIGHT_REMINDER_ID -> {
                try {
                    if (event.data.isEmpty()) {
                        WearOSShared.remoteAlightReminderService.valueNullable = null
                    } else {
                        val json = String(event.data)
                        val remoteData = AlightReminderRemoteData.deserialize(Json.decodeFromString(json))
                        if (remoteData.active == AlightReminderActiveState.STOPPED) {
                            WearOSShared.remoteAlightReminderService.valueNullable = null
                        } else {
                            WearOSShared.remoteAlightReminderService.valueNullable = remoteData
                            val intent = Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            intent.putExtra("alightReminder", true)
                            startActivity(intent)
                        }
                    }
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
            Shared.INVALIDATE_CACHE_ID -> {
                runBlocking(Dispatchers.Main) {
                    Registry.invalidateCache(appContext)
                    Registry.clearInstance()
                    delay(500)
                    val intent = Intent(this@WearDataLayerListenerService, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
            }
        }
    }
}
