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

package com.loohp.hkbuseta.appcontext

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.AtomicFile
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.loohp.hkbuseta.AlightReminderForegroundService
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppIntentResult
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.AppShortcutIcon
import com.loohp.hkbuseta.common.appcontext.FormFactor
import com.loohp.hkbuseta.common.appcontext.HapticFeedback
import com.loohp.hkbuseta.common.appcontext.HapticFeedbackType
import com.loohp.hkbuseta.common.appcontext.withGlobalWritingFilesCounter
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.objects.Theme
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.StringReadChannel
import com.loohp.hkbuseta.common.utils.currentBackgroundRestricted
import com.loohp.hkbuseta.common.utils.getConnectionType
import com.loohp.hkbuseta.common.utils.normalizeUrlScheme
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import com.loohp.hkbuseta.common.utils.useWriteBuffered
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.brightness
import com.loohp.hkbuseta.utils.getBitmapFromVectorDrawable
import com.loohp.hkbuseta.utils.innerBounds
import com.loohp.hkbuseta.utils.startActivity
import com.loohp.hkbuseta.utils.tint
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


var statusBarColor: Color? = null
var navBarColor: Color? = null

@Stable
open class AppContextComposeAndroid internal constructor(
    open val context: Context,
) : AppContextCompose {

    override val packageName: String
        get() = context.packageName

    override val versionName: String
        get() = context.packageManager.getPackageInfo(packageName, 0).versionName?: "Unknown"

    override val versionCode: Long
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
        }

    override val screenWidth: Int
        get() = context.resources.displayMetrics.widthPixels.absoluteValue

    override val screenHeight: Int
        get() = context.resources.displayMetrics.heightPixels.absoluteValue

    override val minScreenSize: Int
        get() = screenWidth.coerceAtMost(screenHeight)

    override val screenScale: Float
        get() = minScreenSize / 412F

    override val screenWidthScale: Float
        get() = screenWidth / 412F

    override val density: Float
        get() = context.resources.displayMetrics.density

    @Suppress("DEPRECATION")
    override val scaledDensity: Float
        get() = context.resources.displayMetrics.scaledDensity

    override val formFactor: FormFactor = FormFactor.NORMAL

    override suspend fun readTextFile(fileName: String, charset: Charset): StringReadChannel {
        return context.applicationContext.openFileInput(fileName).toStringReadChannel(charset)
    }

    override suspend fun writeTextFile(fileName: String, writeText: () -> StringReadChannel) {
        withGlobalWritingFilesCounter {
            AtomicFile(context.applicationContext.getFileStreamPath(fileName)).useWriteBuffered {
                writeText.invoke().transferTo(it)
                it.flush()
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> writeTextFile(fileName: String, json: Json, serializer: SerializationStrategy<T>, writeJson: () -> T) {
        withGlobalWritingFilesCounter {
            AtomicFile(context.applicationContext.getFileStreamPath(fileName)).useWriteBuffered {
                val value = writeJson.invoke()
                json.encodeToStream(serializer, value, it)
                it.flush()
            }
        }
    }

    override suspend fun readRawFile(fileName: String): ByteReadChannel {
        return context.applicationContext.openFileInput(fileName).toByteReadChannel()
    }

    override suspend fun writeRawFile(fileName: String, writeBytes: () -> ByteReadChannel) {
        withGlobalWritingFilesCounter {
            AtomicFile(context.applicationContext.getFileStreamPath(fileName)).useWriteBuffered {
                writeBytes.invoke().copyTo(it)
                it.flush()
            }
        }
    }

    override suspend fun listFiles(): List<String> {
        return context.applicationContext.fileList().asList()
    }

    override suspend fun deleteFile(fileName: String): Boolean {
        return context.applicationContext.deleteFile(fileName)
    }

    override fun syncPreference(preferences: Preferences) {
        RemoteActivityUtils.dataToWatch(context, Shared.SYNC_PREFERENCES_ID, preferences.serialize())
    }

    override fun requestPreferencesIfPossible(): Deferred<Boolean> {
        val defer = CompletableDeferred<Boolean>()
        CoroutineScope(Dispatchers.IO).launch {
            val result = RemoteActivityUtils.hasWatchApp(context).await()
            if (result) {
                RemoteActivityUtils.dataToWatch(context, Shared.REQUEST_PREFERENCES_ID, null,
                    noWatch = { defer.complete(false) },
                    failed = { defer.complete(false) },
                    success = { defer.complete(true) }
                )
            }
        }
        return defer
    }

    override fun hasConnection(): Boolean {
        return context.getConnectionType().hasConnection
    }

    override fun currentBackgroundRestrictions(): BackgroundRestrictionType {
        return context.currentBackgroundRestricted()
    }

    override fun logFirebaseEvent(title: String, values: AppBundle) {
        Firebase.analytics.logEvent(title, values.toAndroidBundle())
    }

    override fun getResourceString(resId: Int): String {
        return context.resources.getString(resId)
    }

    override fun isScreenRound(): Boolean {
        return context.resources.configuration.isScreenRound
    }

    override fun startActivity(appIntent: AppIntent) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            add(AppActiveContextComposeAndroid(appIntent.screen, appIntent.extras.data, appIntent.intentFlags))
        }
    }

    override fun startForegroundService(appIntent: AppIntent) {
        val intent = Intent(appIntent.context.context, appIntent.screen.foregroundServiceActivityClass)
        appIntent.intentFlags.toAndroidFlags()?.let { intent.addFlags(it) }
        appIntent.extras.toAndroidBundle()?.let { intent.putExtras(it) }
        context.startService(intent)
    }

    override fun formatTime(localDateTime: LocalDateTime): String {
        return localDateTime.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern((DateFormat.getTimeFormat(context) as? SimpleDateFormat)?.toPattern()?: "HH:mm"))
    }

    override fun formatDateTime(localDateTime: LocalDateTime, includeTime: Boolean): String {
        return localDateTime.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern((DateFormat.getDateFormat(context) as? SimpleDateFormat)?.toPattern()?: "dd/MM/yyyy")) + (if (includeTime) " " + formatTime(localDateTime) else "")
    }

    override fun setAppShortcut(id: String, shortLabel: String, longLabel: String, icon: AppShortcutIcon, tint: Long?, rank: Int, url: String) {
        val shortcut = ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(IconCompat.createWithBitmap(context.getBitmapFromVectorDrawable(icon.androidResId).let { i -> tint?.let { i.tint(it.toInt()) }?: i }))
            .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            .setRank(rank)
            .addCapabilityBinding("actions.intent.OPEN_APP_FEATURE", "feature", listOf(id))
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    override fun sendLocalNotification(id: Int, channel: String, title: String, content: String, url: String) {
        val manager = NotificationManagerCompat.from(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && manager.getNotificationChannel(channel) != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = url.toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.mipmap.icon_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(id, notification)
        }
    }

    override fun removeAppShortcut(id: String) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
    }

    override suspend fun <T> withHighBandwidthNetwork(block: suspend () -> T): T {
        return block.invoke()
    }

}

@OptIn(ExperimentalUuidApi::class)
@Stable
class AppActiveContextComposeAndroid internal constructor(
    override val screen: AppScreen,
    override val data: MutableMap<String, Any?>,
    override val flags: Set<AppIntentFlag>,
    private val finishCallback: ((AppIntentResult) -> Unit)? = null
) : AppContextComposeAndroid(componentActivity), AppActiveContextCompose {

    override val context: ComponentActivity = componentActivity

    val activeContextId = Uuid.random()
    private var result: AppIntentResult = AppIntentResult.NORMAL

    override val screenWidth: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.windowManager.currentWindowMetrics.innerBounds.width.absoluteValue
        } else {
            super.screenWidth
        }

    override val screenHeight: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.windowManager.currentWindowMetrics.innerBounds.height.absoluteValue
        } else {
            super.screenHeight
        }

    override suspend fun readTextFile(fileName: String, charset: Charset): StringReadChannel {
        return super.readTextFile(fileName, charset)
    }

    override fun runOnUiThread(runnable: () -> Unit) {
        context.runOnUiThread(runnable)
    }

    override fun startActivity(appIntent: AppIntent, callback: (AppIntentResult) -> Unit) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            add(AppActiveContextComposeAndroid(appIntent.screen, appIntent.extras.data, appIntent.intentFlags, finishCallback = callback))
        }
    }

    override fun handleOpenMaps(lat: Double, lng: Double, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return {
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("geo:0,0?q=".plus(lat).plus(",").plus(lng).plus("(").plus(label).plus(")")))
            context.startActivity(intent)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    override fun handleWebpages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return {
            buildCustomTabIntent {
                setColorScheme(Shared.theme.customTabColorScheme)
            }.launchUrl(context, Uri.parse(url.normalizeUrlScheme()))
        }
    }

    override fun handleWebImages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return handleWebpages(url, longClick, haptics)
    }

    override fun setResult(result: AppIntentResult) {
        this.result = result
    }

    override fun setStatusNavBarColor(status: Color?, nav: Color?) {
        val controller = WindowCompat.getInsetsController(context.window, context.window.decorView)
        status?.apply {
            context.window.statusBarColor = toArgb()
            statusBarColor = this
            controller.isAppearanceLightStatusBars = brightness > 0.5F
        }
        nav?.apply {
            context.window.navigationBarColor = toArgb()
            navBarColor = this
            controller.isAppearanceLightNavigationBars = brightness > 0.5F
        }
    }

    override fun completeFinishCallback() {
        finishCallback?.invoke(result)
    }

    override fun readFileFromFileChooser(fileType: String, read: suspend (StringReadChannel) -> Unit) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = fileType
        context.startActivity(intent) {
            if (it.resultCode == Activity.RESULT_OK) {
                CoroutineScope(Dispatchers.IO).launch {
                    val uri = it.data?.data?: return@launch
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        read.invoke(input.toStringReadChannel())
                    }
                }
            }
        }
    }

    override fun writeFileToFileChooser(fileType: String, fileName: String, file: String, onSuccess: () -> Unit) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = fileType
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        context.startActivity(intent) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data?: return@startActivity
                context.contentResolver.openOutputStream(uri)?.use { out -> out.writer(Charsets.UTF_8).use { writer ->
                    writer.write(file)
                    writer.flush()
                } }
                onSuccess.invoke()
            }
        }
    }

    override fun shareUrl(url: String, title: String?) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        shareIntent.putExtra(Intent.EXTRA_TITLE, title)
        shareIntent.type = "text/plain"
        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    override fun switchActivity(appIntent: AppIntent) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            val index = indexOf(this@AppActiveContextComposeAndroid)
            add(index, AppActiveContextComposeAndroid(appIntent.screen, appIntent.extras.data, appIntent.intentFlags))
        }
        finishSelfOnly()
    }

    override fun finishSelfOnly() {
        val stack = HistoryStack.historyStack.value.toMutableList()
        val removed = stack.remove(this)
        if (stack.isEmpty()) {
            stack.add(initialScreen())
        }
        HistoryStack.historyStack.value = stack
        if (removed) {
            finishCallback?.invoke(result)
        }
    }

    override fun finish() {
        val stack = HistoryStack.historyStack.value.toMutableList()
        val removed = stack.remove(this)
        if (stack.isEmpty()) {
            stack.add(initialScreen())
        }
        HistoryStack.historyStack.value = stack
        if (removed) {
            finishCallback?.invoke(result)
            val currentKey = screenGroup
            val lastScreen = stack.lastOrNull()
            if (currentKey != AppScreenGroup.MAIN && lastScreen?.screenGroup == currentKey) {
                lastScreen.finish()
            }
        }
    }

    override fun finishAffinity() {
        val stack = HistoryStack.historyStack.value.toMutableList()
        val index = stack.indexOf(this)
        if (index < 0) {
            return
        }
        (0..index).onEach { stack.removeFirstOrNull() }
        if (stack.isEmpty()) {
            stack.add(initialScreen())
        }
        HistoryStack.historyStack.value = stack
        finishCallback?.invoke(result)
        val currentKey = screenGroup
        val lastScreen = stack.lastOrNull()
        if (currentKey != AppScreenGroup.MAIN && lastScreen?.screenGroup == currentKey) {
            lastScreen.finish()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppActiveContextComposeAndroid) return false

        return activeContextId == other.activeContextId
    }

    override fun hashCode(): Int {
        return activeContextId.hashCode()
    }

}

val AppContext.context: Context get() = (this as AppContextComposeAndroid).context
val AppActiveContext.context: ComponentActivity get() = (this as AppActiveContextComposeAndroid).context

val AppContextCompose.context: Context get() = (this as AppContextComposeAndroid).context
val AppActiveContextCompose.context: ComponentActivity get() = (this as AppActiveContextComposeAndroid).context

internal var applicationAppContextInternal: AppContextCompose? = null

fun setApplicationContext(context: Context) {
    applicationAppContextInternal = AppContextComposeAndroid(context)
}

actual val applicationAppContext: AppContextCompose get() = applicationAppContextInternal?: throw RuntimeException()

internal var componentActivityInternal: ComponentActivity? = null
internal var componentActivityPaused: Boolean? = null

fun setComponentActivity(activity: ComponentActivity) {
    componentActivityInternal = activity
}

val Context.nonActiveAppContext: AppContextComposeAndroid get() = AppContextComposeAndroid(this)

val componentActivity: ComponentActivity get() = componentActivityInternal?: throw RuntimeException()
actual fun handleEmptyStack(stack: MutableList<AppActiveContextCompose>) { componentActivityInternal?.finish()?.apply { componentActivityInternal = null } }
actual fun initialScreen(): AppActiveContextCompose = AppActiveContextComposeAndroid(AppScreen.MAIN, mutableMapOf(), emptySet())

val AppScreen.foregroundServiceActivityClass: Class<*> get() = when (this) {
    AppScreen.ALIGHT_REMINDER_SERVICE -> AlightReminderForegroundService::class.java
    else -> throw RuntimeException("Not a foreground service type")
}

val AppIntentFlag.androidFlag: Int get() = when (this) {
    AppIntentFlag.NEW_TASK -> Intent.FLAG_ACTIVITY_NEW_TASK
    AppIntentFlag.CLEAR_TASK -> Intent.FLAG_ACTIVITY_CLEAR_TASK
    AppIntentFlag.NO_ANIMATION -> Intent.FLAG_ACTIVITY_NO_ANIMATION
}

fun Collection<AppIntentFlag>.toAndroidFlags(): Int? {
    return asSequence().map { it.androidFlag }.reduceOrNull { a, b -> a or b }
}

fun AppBundle.toAndroidBundle(): Bundle? {
    if (data.isEmpty()) return null
    return Bundle().apply {
        for ((key, value) in data) {
            when (value) {
                is IOSerializable -> putByteArray(key, value.toByteArray())
                is Boolean -> putBoolean(key, value)
                is Byte -> putByte(key, value)
                is Char -> putChar(key, value)
                is Short -> putShort(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Double -> putDouble(key, value)
                is String -> putString(key, value)
                is CharSequence -> putCharSequence(key, value)
                is BooleanArray -> putBooleanArray(key, value)
                is ByteArray -> putByteArray(key, value)
                is ShortArray -> putShortArray(key, value)
                is CharArray -> putCharArray(key, value)
                is IntArray -> putIntArray(key, value)
                is LongArray -> putLongArray(key, value)
                is FloatArray -> putFloatArray(key, value)
                is DoubleArray -> putDoubleArray(key, value)
                is List<*> -> @Suppress("UNCHECKED_CAST") putStringArrayList(key, ArrayList(value as List<String>))
                is AppBundle -> putAppBundle(key, value)
            }
        }
    }
}

val AppShortcutIcon.androidResId: Int get() = when (this) {
    AppShortcutIcon.STAR -> R.drawable.baseline_star_24
    AppShortcutIcon.HISTORY -> R.drawable.baseline_history_24
    AppShortcutIcon.SEARCH -> R.drawable.baseline_search_24
    AppShortcutIcon.NEAR_ME -> R.drawable.baseline_near_me_24
}

val Theme.customTabColorScheme: Int get() = when (this) {
    Theme.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
    Theme.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
    Theme.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
}

inline fun buildCustomTabIntent(builder: CustomTabsIntent.Builder.() -> Unit): CustomTabsIntent {
    return CustomTabsIntent.Builder().apply(builder).build()
}