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

package com.loohp.hkbuseta.appcontext

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.AtomicFile
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Stable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.loohp.hkbuseta.AlightReminderActivity
import com.loohp.hkbuseta.DismissibleTextDisplayActivity
import com.loohp.hkbuseta.EtaActivity
import com.loohp.hkbuseta.EtaMenuActivity
import com.loohp.hkbuseta.FatalErrorActivity
import com.loohp.hkbuseta.FavActivity
import com.loohp.hkbuseta.FavRouteListViewActivity
import com.loohp.hkbuseta.ListRoutesActivity
import com.loohp.hkbuseta.ListStopsActivity
import com.loohp.hkbuseta.MainActivity
import com.loohp.hkbuseta.NearbyActivity
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.SearchActivity
import com.loohp.hkbuseta.SettingsActivity
import com.loohp.hkbuseta.TitleActivity
import com.loohp.hkbuseta.TrainRouteMapActivity
import com.loohp.hkbuseta.URLImageActivity
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
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.appcontext.withGlobalWritingFilesCounter
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.StringReadChannel
import com.loohp.hkbuseta.common.utils.currentBackgroundRestricted
import com.loohp.hkbuseta.common.utils.getConnectionType
import com.loohp.hkbuseta.common.utils.normalizeUrlScheme
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import com.loohp.hkbuseta.common.utils.useWriteBuffered
import com.loohp.hkbuseta.tiles.EtaTileConfigureActivity
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.getBitmapFromVectorDrawable
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


@Stable
open class AppContextWearOS internal constructor(
    open val context: Context
) : AppContext {

    override val packageName: String
        get() = context.packageName

    override val versionName: String
        get() = context.packageManager.getPackageInfo(packageName, 0).versionName

    override val versionCode: Long
        get() = context.packageManager.getPackageInfo(packageName, 0).longVersionCode

    override val screenWidth: Int
        get() = context.resources.displayMetrics.widthPixels.absoluteValue

    override val screenHeight: Int
        get() = context.resources.displayMetrics.heightPixels.absoluteValue

    override val minScreenSize: Int
        get() = screenWidth.coerceAtMost(screenHeight)

    override val screenScale: Float
        get() = minScreenSize / 454f

    override val density: Float
        get() = context.resources.displayMetrics.density

    @Suppress("DEPRECATION")
    override val scaledDensity: Float
        get() = context.resources.displayMetrics.scaledDensity

    override val formFactor: FormFactor = FormFactor.WATCH

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
        RemoteActivityUtils.dataToPhone(context, Shared.SYNC_PREFERENCES_ID, preferences.serialize())
    }

    override fun requestPreferencesIfPossible(): Deferred<Boolean> {
        val defer = CompletableDeferred<Boolean>()
        CoroutineScope(Dispatchers.IO).launch {
            val result = RemoteActivityUtils.hasPhoneApp(context).await()
            if (result) {
                RemoteActivityUtils.dataToPhone(context, Shared.REQUEST_PREFERENCES_ID, null,
                    noPhone = { defer.complete(false) },
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
        val intent = Intent(appIntent.context.context, appIntent.screen.activityClass)
        appIntent.intentFlags.toAndroidFlags()?.let { intent.addFlags(it) }
        appIntent.extras.toAndroidBundle()?.let { intent.putExtras(it) }
        context.startActivity(intent)
    }

    override fun startForegroundService(appIntent: AppIntent) {
        val intent = Intent(appIntent.context.context, appIntent.screen.activityClass)
        appIntent.intentFlags.toAndroidFlags()?.let { intent.addFlags(it) }
        appIntent.extras.toAndroidBundle()?.let { intent.putExtras(it) }
        context.startService(intent)
    }

    override fun showToastText(text: String, duration: ToastDuration) {
        Toast.makeText(context, text, duration.androidValue).show()
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
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    override fun removeAppShortcut(id: String) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(element = id))
    }

    override suspend fun <T> withHighBandwidthNetwork(block: suspend () -> T): T {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectivityManager.bindProcessToNetwork(network)
            }
        }
        val bind = try {
            connectivityManager.requestNetwork(NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), callback)
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
        return try {
            block.invoke()
        } finally {
            if (bind) {
                connectivityManager.bindProcessToNetwork(null)
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }

}

@Stable
class AppActiveContextWearOS internal constructor(
    override val context: ComponentActivity
) : AppContextWearOS(context), AppActiveContext {

    override val screenWidth: Int
        get() = context.windowManager.currentWindowMetrics.bounds.width().absoluteValue

    override val screenHeight: Int
        get() = context.windowManager.currentWindowMetrics.bounds.height().absoluteValue

    override suspend fun readTextFile(fileName: String, charset: Charset): StringReadChannel {
        return super.readTextFile(fileName, charset)
    }

    override fun runOnUiThread(runnable: () -> Unit) {
        context.runOnUiThread(runnable)
    }

    override fun startActivity(appIntent: AppIntent, callback: (AppIntentResult) -> Unit) {
        val intent = Intent(appIntent.context.context, appIntent.screen.activityClass)
        appIntent.intentFlags.toAndroidFlags()?.let { intent.addFlags(it) }
        appIntent.extras.toAndroidBundle()?.let { intent.putExtras(it) }
        context.startActivity(intent) { callback.invoke(AppIntentResult(it.resultCode)) }
    }

    override fun handleOpenMaps(lat: Double, lng: Double, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return {
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("geo:0,0?q=".plus(lat).plus(",").plus(lng).plus("(").plus(label).plus(")")))
            if (longClick) {
                context.startActivity(intent)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                RemoteActivityUtils.intentToPhone(
                    instance = context,
                    intent = intent,
                    noPhone = {
                        try {
                            context.startActivity(intent)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    },
                    failed = {
                        try {
                            context.startActivity(intent)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    },
                    success = {
                        runOnUiThread {
                            showToastText(if (Shared.language == "en") "Please check your phone" else "請在手機上繼續", ToastDuration.SHORT)
                        }
                    }
                )
            }
        }
    }

    override fun handleWebpages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return handleWebpages(url, false, longClick, haptics)
    }

    fun handleWebpages(url: String, watchFirst: Boolean, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return handler@{
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(url.normalizeUrlScheme()))
            if (longClick) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (watchFirst) {
                try {
                    context.startActivity(intent)
                    return@handler
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            RemoteActivityUtils.intentToPhone(
                instance = context,
                intent = intent,
                noPhone = {
                    try {
                        context.startActivity(intent)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                },
                failed = {
                    try {
                        context.startActivity(intent)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                },
                success = {
                    runOnUiThread {
                        showToastText(if (Shared.language == "en") "Please check your phone" else "請在手機上繼續", ToastDuration.SHORT)
                    }
                }
            )
        }
    }

    override fun handleWebImages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return {
            val phoneIntent = Intent(context, AppScreen.URL_IMAGE.activityClass)
            phoneIntent.putExtra("url", url.normalizeUrlScheme())
            if (longClick) {
                context.startActivity(phoneIntent)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                val intent = Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse(url.normalizeUrlScheme()))
                RemoteActivityUtils.intentToPhone(
                    instance = context,
                    intent = intent,
                    noPhone = { context.startActivity(phoneIntent) },
                    failed = { context.startActivity(phoneIntent) },
                    success = {
                        runOnUiThread {
                            showToastText(if (Shared.language == "en") "Please check your phone" else "請在手機上繼續", ToastDuration.SHORT)
                        }
                    }
                )
            }
        }
    }

    override fun setResult(result: AppIntentResult) {
        context.setResult(result.resultCode)
    }

    override fun finish() {
        context.finish()
    }

    override fun finishAffinity() {
        context.finishAffinity()
    }

}

inline val AppContext.wear: AppContextWearOS get() = this as AppContextWearOS
inline val AppActiveContext.wear: AppActiveContextWearOS get() = this as AppActiveContextWearOS

inline val AppContext.context: Context get() = wear.context
inline val AppActiveContext.context: ComponentActivity get() = wear.context

private val appContextHolder: Cache<Context, AppContextWearOS> = CacheBuilder.newBuilder().weakKeys().build()

val ComponentActivity.appContext: AppActiveContextWearOS get() = (this as Context).appContext as AppActiveContextWearOS

val Context.appContext: AppContextWearOS get() = appContextHolder.get(this) { if (this is ComponentActivity) AppActiveContextWearOS(this) else AppContextWearOS(this) }

val AppScreen.activityClass: Class<*> get() = when (this) {
    AppScreen.DISMISSIBLE_TEXT_DISPLAY -> DismissibleTextDisplayActivity::class.java
    AppScreen.FATAL_ERROR -> FatalErrorActivity::class.java
    AppScreen.ETA -> EtaActivity::class.java
    AppScreen.ETA_MENU -> EtaMenuActivity::class.java
    AppScreen.FAV -> FavActivity::class.java
    AppScreen.FAV_ROUTE_LIST_VIEW -> FavRouteListViewActivity::class.java
    AppScreen.LIST_ROUTES -> ListRoutesActivity::class.java
    AppScreen.LIST_STOPS -> ListStopsActivity::class.java
    AppScreen.MAIN -> MainActivity::class.java
    AppScreen.NEARBY -> NearbyActivity::class.java
    AppScreen.SEARCH -> SearchActivity::class.java
    AppScreen.TITLE -> TitleActivity::class.java
    AppScreen.URL_IMAGE -> URLImageActivity::class.java
    AppScreen.SETTINGS -> SettingsActivity::class.java
    AppScreen.ALIGHT_REMINDER_SERVICE -> AlightReminderActivity::class.java
    AppScreen.ETA_TILE_CONFIGURE -> EtaTileConfigureActivity::class.java
    AppScreen.SEARCH_TRAIN -> TrainRouteMapActivity::class.java
    else -> MainActivity::class.java
}

val AppIntentFlag.androidFlag: Int @SuppressLint("WearRecents") get() = when (this) {
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

val ToastDuration.androidValue: Int get() = when (this) {
    ToastDuration.SHORT -> Toast.LENGTH_SHORT
    ToastDuration.LONG -> Toast.LENGTH_LONG
}

val androidx.compose.ui.hapticfeedback.HapticFeedbackType.common: HapticFeedbackType get() {
    return when (this) {
        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress -> HapticFeedbackType.LongPress
        androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove -> HapticFeedbackType.TextHandleMove
        else -> HapticFeedbackType.LongPress
    }
}

val HapticFeedbackType.android: androidx.compose.ui.hapticfeedback.HapticFeedbackType get() {
    return when (this) {
        HapticFeedbackType.LongPress -> androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
        HapticFeedbackType.TextHandleMove -> androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
        else -> androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
    }
}

val androidx.compose.ui.hapticfeedback.HapticFeedback.common: HapticFeedback get() {
    return object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            this@common.performHapticFeedback(hapticFeedbackType.android)
        }
    }
}

val AppShortcutIcon.androidResId: Int get() = when (this) {
    AppShortcutIcon.STAR -> R.drawable.baseline_star_24
    AppShortcutIcon.HISTORY -> R.drawable.baseline_history_24
    AppShortcutIcon.SEARCH -> R.drawable.baseline_search_24
    AppShortcutIcon.NEAR_ME -> R.drawable.baseline_near_me_24
}