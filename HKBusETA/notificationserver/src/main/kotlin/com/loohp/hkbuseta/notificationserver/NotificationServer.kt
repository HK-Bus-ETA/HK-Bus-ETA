package com.loohp.hkbuseta.notificationserver

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.loohp.hkbuseta.common.objects.AlertNotification
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.JsonIgnoreUnknownKeys
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.debugLog
import com.loohp.hkbuseta.common.utils.hongKongZoneId
import com.loohp.hkbuseta.common.utils.toString
import com.loohp.hkbuseta.notificationserver.notices.checkNoticeUpdates
import com.loohp.hkbuseta.notificationserver.utils.TimerTask
import com.loohp.hkbuseta.notificationserver.utils.awaitUntil
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toJavaLocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Timer
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


fun main() {
    val serviceAccount = Accessor.javaClass.classLoader.getResourceAsStream("hkbuseta-firebase-adminsdk-7vwi5-ae6c215dc0.json")

    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()

    FirebaseApp.initializeApp(options)

    Timer().scheduleAtFixedRate(
        TimerTask {
            println("Resetting registry")
            runBlocking { resetRegistry() }
        },
        Date(
            currentLocalDateTime().toJavaLocalDateTime()
                .atZone(ZoneId.of(hongKongZoneId))
                .withNano(0)
                .withSecond(0)
                .withMinute(0)
                .plusHours(1)
                .toEpochSecond() * 1000
        ),
        1.hours.inWholeMilliseconds
    )

    Timer().scheduleAtFixedRate(
        TimerTask {
            println("Pushing route stop live")
            pushRouteStopLive()
        },
        Date(
            currentLocalDateTime().toJavaLocalDateTime()
                .atZone(ZoneId.of(hongKongZoneId))
                .withNano(0)
                .withSecond(0)
                .plusMinutes(1)
                .toEpochSecond() * 1000
        ),
        Shared.ETA_UPDATE_INTERVAL.toLong() + 5000
    )

    Timer().schedule(
        TimerTask {
            runBlocking {
                println("Checking notice updates")
                val notifications = checkNoticeUpdates()
                pushAlerts(notifications)
            }
        },
        Date(
            currentLocalDateTime().toJavaLocalDateTime()
                .atZone(ZoneId.of(hongKongZoneId))
                .withNano(0)
                .withSecond(0)
                .plusMinutes(1)
                .toEpochSecond() * 1000
        ),
        5.minutes.inWholeMilliseconds
    )

    Timer().scheduleAtFixedRate(
        TimerTask {
            println("Pushing daily refresh")
            pushDailyRefresh()
        },
        Date(
            currentLocalDateTime().toJavaLocalDateTime()
                .atZone(ZoneId.of(hongKongZoneId))
                .withNano(0)
                .withSecond(0)
                .withMinute(0)
                .withHour(6)
                .toEpochSecond() * 1000
        ),
        1.days.inWholeMilliseconds
    )
}

object Accessor

suspend fun registry(): Registry {
    val init = !Registry.hasInstanceCreated()
    val registry = Registry.getInstance(ServerAppContext)
    awaitUntil(
        predicate = { !registry.state.value.isProcessing },
        onPollFailed = {
            if (registry.state.value == Registry.State.UPDATING) {
                debugLog("Updating... ${(registry.updatePercentageState.value * 100F).toString(2)}%")
            }
        }
    )
    if (init) {
        registry.setLanguage("zh", ServerAppContext)
    }
    return registry
}

suspend fun resetRegistry() {
    Registry.clearInstance()
    println("Reset registry")
    registry()
}

fun pushRouteStopLive() {
    val message = Message.builder()
        .putData("action", "RouteStopLive")
        .setTopic("RouteStopLive")
        .setApnsConfig(
            ApnsConfig.builder()
                .setAps(
                    Aps.builder()
                        .setContentAvailable(true)
                        .build()
                )
                .build()
        )
        .build()

    val response = FirebaseMessaging.getInstance().send(message)
    println("Successfully pushed route stop live: $response")
}

fun pushAlerts(notifications: List<AlertNotification>) {
    val messages = notifications.map {
        Message.builder()
            .putData("action", "Alert")
            .putData("data", JsonIgnoreUnknownKeys.encodeToString(it))
            .setTopic("Alert")
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build()
            )
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setContentAvailable(true)
                            .build()
                    )
                    .build()
            )
            .build()
    }
    println("There are ${messages.size} alert messages")

    if (messages.isNotEmpty()) {
        val response = FirebaseMessaging.getInstance().sendEach(messages)
        println("Successfully sent message: $response")
    }
}

fun pushDailyRefresh() {
    val message = Message.builder()
        .putData("action", "Refresh")
        .setTopic("Refresh")
        .setAndroidConfig(
            AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build()
        )
        .setApnsConfig(
            ApnsConfig.builder()
                .setAps(
                    Aps.builder()
                        .setContentAvailable(true)
                        .build()
                )
                .build()
        )
        .build()

    val response = FirebaseMessaging.getInstance().send(message)
    println("Successfully push daily refresh: $response")
}