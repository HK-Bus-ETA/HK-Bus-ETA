package com.loohp.hkbuseta

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.debugLog
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit


class MainFirebaseMessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        debugLog("Refreshed token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = message.data
        when (payload["action"]) {
            "Alert" -> {
                runBlocking { Shared.handleAlertRemoteNotification(payload["data"]!!, appContext) }
            }
            "Refresh" -> {
                val registry = Registry.getInstance(appContext)
                while (registry.state.value.isProcessing) {
                    TimeUnit.MILLISECONDS.sleep(100)
                }
            }
        }
    }

}