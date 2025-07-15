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
import com.loohp.hkbuseta.common.utils.hongKongZoneId
import com.loohp.hkbuseta.common.utils.toString
import com.loohp.hkbuseta.notificationserver.notices.checkNoticeUpdates
import com.loohp.hkbuseta.notificationserver.utils.TimerTask
import com.loohp.hkbuseta.notificationserver.utils.asLogging
import com.loohp.hkbuseta.notificationserver.utils.awaitUntil
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Timer
import java.util.zip.GZIPOutputStream
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


val START_TIME: LocalDateTime = currentLocalDateTime()

fun main() {
    initLogging()

    val serviceAccount = Accessor.javaClass.classLoader.getResourceAsStream("hkbuseta-firebase-adminsdk-7vwi5-ae6c215dc0.json")

    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()

    FirebaseApp.initializeApp(options)

    Timer().scheduleAtFixedRate(
        TimerTask {
            log("Resetting registry")
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
            log("Pushing route stop live")
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
                log("Checking notice updates")
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
            log("Pushing daily refresh")
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

fun log(vararg message: Any?) {
    val time = ServerAppContext.formatDateTime(currentLocalDateTime(), true)
    val log = message.joinToString(
        prefix = "[$time] ",
        separator = ", ",
        transform = { it.toString() }
    )
    println(log)
}

fun initLogging() {
    val logFolder = File("logs").apply { mkdirs() }
    val latestFileName = "latest.log"
    val logFile = File(logFolder, latestFileName)
    val logs = PrintStream(logFile.outputStream(append = true), true, Charsets.UTF_8)
    System.setOut(System.out.asLogging(logs))
    System.setErr(System.err.asLogging(logs))
    Runtime.getRuntime().addShutdownHook(Thread {
        logs.flush()
        logs.close()
        val logFormat = DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd'_'HH'-'mm'-'ss'_'zzz'.log.gz'")
        val fileName = START_TIME.toJavaLocalDateTime().atZone(ZoneId.of(hongKongZoneId)).format(logFormat)
        File(logFolder, fileName).outputStream().zipped().use { output ->
            logFile.inputStream().use { input -> input.copyTo(output) }
        }
        logFile.delete()
    })
}

fun File.outputStream(append: Boolean): OutputStream = FileOutputStream(this, append)
fun OutputStream.zipped(): OutputStream = GZIPOutputStream(this)

suspend fun registry(): Registry {
    val init = !Registry.hasInstanceCreated()
    val registry = Registry.getInstance(ServerAppContext)
    var wasUpdating = false
    while (true) {
        try {
            withTimeout(1.minutes.inWholeMilliseconds) {
                awaitUntil(
                    predicate = { !registry.state.value.isProcessing },
                    onPollFailed = {
                        if (registry.state.value == Registry.State.UPDATING) {
                            wasUpdating = true
                            log("Updating... ${(registry.updatePercentageState.value * 100F).toString(2)}%")
                        }
                    }
                )
            }
            break
        } catch (e: Throwable) {
            e.printStackTrace()
            resetRegistry()
        }
    }
    if (init) {
        registry.setLanguage("zh", ServerAppContext)
    }
    if (wasUpdating) {
        log("Done!")
    }
    return registry
}

suspend fun resetRegistry(): Registry {
    Registry.clearInstance()
    log("Reset registry")
    return registry()
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
    log("Successfully pushed route stop live: $response")
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
    log("There are ${messages.size} alert messages")

    if (messages.isNotEmpty()) {
        val response = FirebaseMessaging.getInstance().sendEach(messages)
        log("Successfully sent message: $response")
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
    log("Successfully push daily refresh: $response")
}