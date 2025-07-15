package com.loohp.hkbuseta.notificationserver

import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppShortcutIcon
import com.loohp.hkbuseta.common.appcontext.FormFactor
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.appcontext.withGlobalWritingFilesCounter
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.StringReadChannel
import com.loohp.hkbuseta.common.utils.hongKongZoneId
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.format.DateTimeFormatter


val DATA_FOLDER: File by lazy {
    val path = File("data").toPath()
    if (!Files.exists(path)) {
        Files.createDirectories(path)
    }
    path.toFile()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun doNothing() = Unit

object ServerAppContext: AppContext {
    override val packageName: String = "com.loohp.hkbuseta.notificationserver"
    override val versionName: String = "1.0.0"
    override val versionCode: Long = 1
    override val screenWidth: Int = 1
    override val screenHeight: Int = 1
    override val minScreenSize: Int = 1
    override val screenScale: Float = 1F
    override val density: Float = 1F
    override val scaledDensity: Float = 1F
    override val formFactor: FormFactor = FormFactor.NORMAL
    override fun syncPreference(preferences: Preferences) = doNothing()
    override fun requestPreferencesIfPossible(): Deferred<Boolean> = CompletableDeferred(false)
    override fun hasConnection(): Boolean = true
    override fun currentBackgroundRestrictions(): BackgroundRestrictionType = BackgroundRestrictionType.NONE
    override fun logFirebaseEvent(title: String, values: AppBundle) = doNothing()
    override fun getResourceString(resId: Int): String = ""
    override fun isScreenRound(): Boolean = false
    override fun startActivity(appIntent: AppIntent) = doNothing()
    override fun startForegroundService(appIntent: AppIntent) = doNothing()
    override fun showToastText(text: String, duration: ToastDuration) = doNothing()
    override fun setAppShortcut(id: String, shortLabel: String, longLabel: String, icon: AppShortcutIcon, tint: Long?, rank: Int, url: String) = doNothing()
    override fun sendLocalNotification(id: Int, channel: String, title: String, content: String, url: String) = doNothing()

    override fun removeAppShortcut(id: String) = doNothing()
    override suspend fun <T> withHighBandwidthNetwork(block: suspend () -> T): T = block.invoke()

    override suspend fun readTextFile(fileName: String, charset: Charset): StringReadChannel {
        return File(DATA_FOLDER, fileName).inputStream().toStringReadChannel(charset)
    }

    override suspend fun writeTextFile(fileName: String, writeText: () -> StringReadChannel) {
        withGlobalWritingFilesCounter {
            File(DATA_FOLDER, fileName).outputStream().use {
                writeText.invoke().transferTo(it)
                it.flush()
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> writeTextFile(fileName: String, json: Json, serializer: SerializationStrategy<T>, writeJson: () -> T) {
        withGlobalWritingFilesCounter {
            File(DATA_FOLDER, fileName).outputStream().use {
                val value = writeJson.invoke()
                json.encodeToStream(serializer, value, it)
                it.flush()
            }
        }
    }

    override suspend fun readRawFile(fileName: String): ByteReadChannel {
        return File(DATA_FOLDER, fileName).inputStream().toByteReadChannel()
    }

    override suspend fun writeRawFile(fileName: String, writeBytes: () -> ByteReadChannel) {
        withGlobalWritingFilesCounter {
            File(DATA_FOLDER, fileName).outputStream().use {
                writeBytes.invoke().copyTo(it)
                it.flush()
            }
        }
    }

    override suspend fun listFiles(): List<String> {
        return DATA_FOLDER.listFiles()?.map { it.name }.orEmpty()
    }

    override suspend fun deleteFile(fileName: String): Boolean {
        return File(DATA_FOLDER, fileName).delete()
    }

    override fun formatTime(localDateTime: LocalDateTime): String {
        return localDateTime.toJavaLocalDateTime()
            .atZone(ZoneId.of(hongKongZoneId))
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    override fun formatDateTime(localDateTime: LocalDateTime, includeTime: Boolean): String {
        val pattern = if (includeTime) "dd/MM/yyyy HH:mm:ss zzz" else "dd/MM/yyyy"
        return localDateTime.toJavaLocalDateTime()
            .atZone(ZoneId.of(hongKongZoneId))
            .format(DateTimeFormatter.ofPattern(pattern))
    }

}