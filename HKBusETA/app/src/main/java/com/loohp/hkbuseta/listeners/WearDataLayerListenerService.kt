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
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.utils.RemoteActivityUtils.Companion.dataToPhone
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class WearDataLayerListenerService : WearableListenerService() {

    companion object {
        const val START_ACTIVITY_PATH = "/HKBusETA/Launch"
        const val IMPORT_PREFERENCE_PATH = "/HKBusETA/ImportPreference"
        const val EXPORT_PREFERENCE_PATH = "/HKBusETA/ExportPreference"
    }

    @SuppressLint("WearRecents")
    override fun onMessageReceived(event: MessageEvent) {
        super.onMessageReceived(event)
        when (event.path) {
            START_ACTIVITY_PATH -> {
                try {
                    val data = Json.decodeFromString<JsonObject>(String(event.data))
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    for ((key, value) in data) {
                        value.jsonPrimitive.apply {
                            booleanOrNull?.apply { intent.putExtra(key, this) }?:
                            intOrNull?.apply { intent.putExtra(key, this) }?:
                            longOrNull?.apply { intent.putExtra(key, this) }?:
                            floatOrNull?.apply { intent.putExtra(key, this) }?:
                            doubleOrNull?.apply { intent.putExtra(key, this) }?:
                            intent.putExtra(key, content)
                        }
                    }
                    Firebase.analytics.logEvent("remote_launch", Bundle().apply {
                        putString("value", "1")
                    })
                    startActivity(intent)
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
            IMPORT_PREFERENCE_PATH -> {
                try {
                    Registry.initInstanceWithImportedPreference(appContext, Json.decodeFromString(String(event.data)))
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
            EXPORT_PREFERENCE_PATH -> {
                try {
                    val preferences = Registry.getInstanceNoUpdateCheck(appContext).exportPreference()
                    dataToPhone(this, EXPORT_PREFERENCE_PATH, preferences)
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
