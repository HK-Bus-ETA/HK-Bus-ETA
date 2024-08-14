/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.appcontext

import com.benasher44.uuid.uuid4
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
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.common.services.AlightReminderActiveState
import com.loohp.hkbuseta.common.services.AlightReminderRemoteData
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.MutableNonNullStateFlow
import com.loohp.hkbuseta.common.utils.MutableNonNullStateFlowList
import com.loohp.hkbuseta.common.utils.MutableNullableStateFlow
import com.loohp.hkbuseta.common.utils.StringReadChannel
import com.loohp.hkbuseta.common.utils.isReachable
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.normalizeUrlScheme
import com.loohp.hkbuseta.common.utils.optDouble
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.pad
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import com.loohp.hkbuseta.common.utils.wrap
import com.loohp.hkbuseta.common.utils.wrapList
import io.ktor.util.toByteArray
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toNSDateComponents
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import platform.Foundation.NSBundle
import platform.Foundation.NSCalendar
import platform.Foundation.NSData
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.URLByAppendingPathComponent
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.lastPathComponent
import platform.Foundation.writeToURL
import platform.WatchConnectivity.WCSession
import platform.WatchKit.WKHapticType
import platform.WatchKit.WKInterfaceDevice
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy


object HistoryStack {

    val historyStack: MutableNonNullStateFlowList<AppActiveContextWatchOS> = MutableStateFlow(listOf(AppActiveContextWatchOS.INIT_ENTRY)).wrapList()

    fun popHistoryStack() {
        val stack = historyStack.value.toMutableList()
        val last = stack.removeLastOrNull()
        if (stack.isEmpty()) {
            stack.add(AppActiveContextWatchOS.DEFAULT_ENTRY)
        }
        historyStack.value = stack
        last?.finishCallback?.invoke(last.result)
    }

}

data class ToastTextData(val text: String, val duration: ToastDuration) {

    companion object {

        val RESET: ToastTextData = ToastTextData("", ToastDuration.SHORT)

    }

}

object ToastTextState {

    val toastState: MutableNonNullStateFlow<ToastTextData> = MutableStateFlow(ToastTextData.RESET).wrap()

    fun resetToastState() {
        toastState.value = ToastTextData.RESET
    }

}

val applicationContext: AppContextWatchOS = AppContextWatchOS()

private var firebaseImpl: (String, AppBundle) -> Unit = { _, _ -> }
private var openMapsImpl: (Double, Double, String, Boolean, HapticFeedback) -> Unit = { _, _, _, _, _ -> }
private var openWebpagesImpl: (String, Boolean, HapticFeedback) -> Unit = { _, _, _ -> }
private var openWebImagesImpl: (String, Boolean, HapticFeedback) -> Unit = { _, _, _ -> }
private var syncPreferencesImpl: (String) -> Unit = { }

fun setFirebaseLogImpl(handler: (String, AppBundle) -> Unit) {
    firebaseImpl = handler
}

fun setOpenMapsImpl(handler: (Double, Double, String, Boolean, HapticFeedback) -> Unit) {
    openMapsImpl = handler
}

fun setOpenWebpagesImpl(handler: (String, Boolean, HapticFeedback) -> Unit) {
    openWebpagesImpl = handler
}

fun setOpenImagesImpl(handler: (String, Boolean, HapticFeedback) -> Unit) {
    openWebImagesImpl = handler
}

fun setSyncPreferencesImpl(handler: (String) -> Unit) {
    syncPreferencesImpl = handler
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class, BetaInteropApi::class)
open class AppContextWatchOS internal constructor() : AppContext {

    override val packageName: String
        get() = NSBundle.mainBundle.bundleIdentifier?.split('.')?.subList(0, 3)?.joinToString(".") ?: "Unknown"

    override val versionName: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")?.toString()?: "Unknown"

    override val versionCode: Long
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")?.toString()?.toLong()?: -1L

    override val screenWidth: Int
        get() = WKInterfaceDevice.currentDevice().screenBounds.useContents { size.width }.toInt()

    override val screenHeight: Int
        get() = WKInterfaceDevice.currentDevice().screenBounds.useContents { size.height }.toInt()

    override val minScreenSize: Int
        get() = screenWidth.coerceAtMost(screenHeight)

    override val screenScale: Float
        get() = minScreenSize / 198F

    override val density: Float
        get() = 1F

    override val scaledDensity: Float
        get() = 1F

    override val formFactor: FormFactor = FormFactor.WATCH

    override suspend fun readTextFile(fileName: String, charset: Charset): StringReadChannel {
        return NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            val text = NSString.create(contentsOfURL = fileURL)
            text.toString().toStringReadChannel(charset)
        }
    }

    override suspend fun writeTextFile(fileName: String, writeText: () -> StringReadChannel) {
        NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            val text = NSString.create(string = writeText.invoke().string())
            text.writeToURL(fileURL, atomically = false)
        }
    }

    override suspend fun readRawFile(fileName: String): ByteReadChannel {
        return NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            val data = NSData.create(contentsOfURL = fileURL)!!
            ByteReadChannel(ByteArray(data.length.toInt()).apply {
                usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
            })
        }
    }

    override suspend fun writeRawFile(fileName: String, writeBytes: () -> ByteReadChannel) {
        NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            val array = writeBytes.invoke().toByteArray()
            val data = memScoped { NSData.create(bytes = allocArrayOf(array), length = array.size.convert()) }
            data.writeToURL(fileURL, atomically = true)
        }
    }

    override suspend fun listFiles(): List<String> {
        val fileManager = NSFileManager.defaultManager
        return fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            fileManager.contentsOfDirectoryAtURL(dir, includingPropertiesForKeys = null, options = 0u, error = null)?.mapNotNull {
                it as NSURL
                it.lastPathComponent
            }?: emptyList()
        }
    }

    override suspend fun deleteFile(fileName: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        return fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first().let { dir ->
            dir as NSURL
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            if (fileManager.fileExistsAtPath(fileURL.path!!)) {
                fileManager.removeItemAtURL(fileURL, error = null)
            } else {
                false
            }
        }
    }

    override fun syncPreference(preferences: Preferences) {
        syncPreferencesImpl.invoke(preferences.serialize().toString())
    }

    override fun requestPreferencesIfPossible(): Deferred<Boolean> {
        return if (WCSession.defaultSession.isCompanionAppInstalled()) {
            WCSession.defaultSession.sendMessage(mapOf("messageType" to Shared.REQUEST_PREFERENCES_ID), { /* do nothing */ }, { /* do nothing */ })
            CompletableDeferred(true)
        } else {
            CompletableDeferred(false)
        }
    }

    override fun hasConnection(): Boolean {
        return runBlocking(com.loohp.hkbuseta.common.utils.dispatcherIO) { isReachable(Registry.checksumUrl()) }
    }

    override fun currentBackgroundRestrictions(): BackgroundRestrictionType {
        return BackgroundRestrictionType.NONE
    }

    override fun logFirebaseEvent(title: String, values: AppBundle) {
        firebaseImpl.invoke(title, values)
    }

    override fun getResourceString(resId: Int): String {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun isScreenRound(): Boolean {
        return false
    }

    override fun startActivity(appIntent: AppIntent) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            if (appIntent.hasFlags(AppIntentFlag.NEW_TASK) || appIntent.hasFlags(AppIntentFlag.CLEAR_TASK)) {
                clear()
            }
            add(AppActiveContextWatchOS(appIntent.screen, appIntent.extras.data))
        }
    }

    override fun startForegroundService(appIntent: AppIntent) {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun showToastText(text: String, duration: ToastDuration) {
        ToastTextState.toastState.value = ToastTextData(text, duration)
    }

    override fun formatTime(localDateTime: LocalDateTime): String {
        return NSCalendar.currentCalendar.dateFromComponents(localDateTime.toNSDateComponents())?.let {
            NSDateFormatter().apply {
                locale = NSLocale.currentLocale
                dateStyle = NSDateFormatterNoStyle
                timeStyle = NSDateFormatterShortStyle
            }.stringFromDate(it)
        }?: localDateTime.let { "${it.hour.pad(2)}:${it.minute.pad(2)}" }
    }

    override fun formatDateTime(localDateTime: LocalDateTime, includeTime: Boolean): String {
        return NSCalendar.currentCalendar.dateFromComponents(localDateTime.toNSDateComponents())?.let {
            NSDateFormatter().apply {
                locale = NSLocale.currentLocale
                dateStyle = NSDateFormatterMediumStyle
                timeStyle = NSDateFormatterShortStyle
            }.stringFromDate(it)
        }?: localDateTime.let {
            "${it.dayOfMonth.pad(2)}/${it.monthNumber.pad(2)}/${it.year} ${it.hour.pad(2)}:${it.minute.pad(2)}"
        }
    }

    override fun setAppShortcut(id: String, shortLabel: String, longLabel: String, icon: AppShortcutIcon, tint: Long?, rank: Int, url: String) {
        //do nothing
    }

    override fun removeAppShortcut(id: String) {
        //do nothing
    }

}

class AppActiveContextWatchOS internal constructor(
    val screen: AppScreen,
    val data: Map<String, Any?>,
    val storage: MutableMap<String, Any?> = mutableMapOf(),
    internal val finishCallback: ((AppIntentResult) -> Unit)? = null
) : AppContextWatchOS(), AppActiveContext {

    companion object {

        val INIT_ENTRY get() = AppActiveContextWatchOS(AppScreen.DUMMY, emptyMap())

        val DEFAULT_ENTRY get() = AppActiveContextWatchOS(AppScreen.MAIN, emptyMap())

    }

    val activeContextId = uuid4()
    internal var result: AppIntentResult = AppIntentResult.NORMAL

    override fun runOnUiThread(runnable: () -> Unit) {
        dispatch_async(dispatch_get_main_queue(), runnable)
    }

    override fun startActivity(appIntent: AppIntent, callback: (AppIntentResult) -> Unit) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            if (appIntent.hasFlags(AppIntentFlag.NEW_TASK) || appIntent.hasFlags(AppIntentFlag.CLEAR_TASK)) {
                clear()
            }
            add(AppActiveContextWatchOS(appIntent.screen, appIntent.extras.data, finishCallback = callback))
        }
    }

    override fun handleOpenMaps(lat: Double, lng: Double, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return { openMapsImpl.invoke(lat, lng, label, longClick, haptics) }
    }

    override fun handleWebpages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return { openWebpagesImpl.invoke(url.normalizeUrlScheme(), longClick, haptics) }
    }

    override fun handleWebImages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return { openWebImagesImpl.invoke(url.normalizeUrlScheme(), longClick, haptics) }
    }

    override fun setResult(result: AppIntentResult) {
        this.result = result
    }

    override fun finish() {
        val stack = HistoryStack.historyStack.value.toMutableList()
        val removed = stack.remove(this)
        if (stack.isEmpty()) {
            stack.add(DEFAULT_ENTRY)
        }
        HistoryStack.historyStack.value = stack
        if (removed) {
            finishCallback?.invoke(result)
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
            stack.add(DEFAULT_ENTRY)
        }
        HistoryStack.historyStack.value = stack
        finishCallback?.invoke(result)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppActiveContextWatchOS) return false

        return activeContextId == other.activeContextId
    }

    override fun hashCode(): Int {
        return activeContextId.hashCode()
    }

}

fun createMutableAppDataContainer(): MutableMap<String, Any?> {
    return mutableMapOf()
}

fun createAppIntent(context: AppContext, screen: AppScreen, appDataContainer: MutableMap<String, Any?>): AppIntent {
    return AppIntent(context, screen).apply { extras.data.putAll(appDataContainer) }
}

fun dispatcherIO(task: () -> Unit) {
    CoroutineScope(com.loohp.hkbuseta.common.utils.dispatcherIO).launch { task.invoke() }
}

val HapticFeedbackType.native: WKHapticType get() = when (this) {
    HapticFeedbackType.LongPress -> WKHapticType.WKHapticTypeClick
    HapticFeedbackType.TextHandleMove -> WKHapticType.WKHapticTypeClick
}

val hapticFeedback: HapticFeedback = object : HapticFeedback {

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        WKInterfaceDevice.currentDevice().playHaptic(hapticFeedbackType.native)
    }

}

fun syncPreference(context: AppContext, preferenceJson: String, sync: Boolean) {
    runBlocking(com.loohp.hkbuseta.common.utils.dispatcherIO) {
        if (Registry.isNewInstall(applicationContext)) {
            Registry.writeRawPreference(preferenceJson, applicationContext)
        } else {
            val data = Preferences.deserialize(Json.decodeFromString(preferenceJson))
            Registry.getInstance(context).syncPreference(context, data, sync)
        }
    }
}

fun getRawPreference(context: AppContext): String {
    return runBlocking(com.loohp.hkbuseta.common.utils.dispatcherIO) { Registry.getRawPreferences(context).toString() }
}

fun isNewInstall(context: AppContext): Boolean {
    return runBlocking(com.loohp.hkbuseta.common.utils.dispatcherIO) { Registry.isNewInstall(context) }
}

fun invalidateCache(context: AppContext) {
    runBlocking { Shared.invalidateCache(context) }
}

val remoteAlightReminderService: MutableNullableStateFlow<AlightReminderRemoteData> = MutableNullableStateFlow(null)

fun receiveAlightReminderRemoteData(launch: Boolean, json: String?) {
    json?.let {
        val remoteData = AlightReminderRemoteData.deserialize(Json.decodeFromString(it))
        if (remoteData.active == AlightReminderActiveState.STOPPED) {
            remoteAlightReminderService.valueNullable = null
        } else {
            remoteAlightReminderService.valueNullable = remoteData
            if (launch) {
                CoroutineScope(com.loohp.hkbuseta.common.utils.dispatcherIO).launch {
                    while (Registry.getInstance(applicationContext).state.value.isProcessing) {
                        delay(100)
                    }
                    delay(500)
                    if (HistoryStack.historyStack.value.lastOrNull()?.screen != AppScreen.ALIGHT_REMINDER_SERVICE) {
                        applicationContext.startActivity(AppIntent(applicationContext, AppScreen.ALIGHT_REMINDER_SERVICE))
                    }
                }
            }
        }
    }?: run {
        remoteAlightReminderService.valueNullable = null
    }
}

fun Registry.findRoutesBlocking(input: String, exact: Boolean): List<RouteSearchResultEntry> {
    return runBlocking(com.loohp.hkbuseta.common.utils.dispatcherIO) { findRoutes(input, exact) }
}

fun Registry.findRoutesBlocking(input: String, exact: Boolean, predicate: (Route) -> Boolean): List<RouteSearchResultEntry> {
    return runBlocking(com.loohp.hkbuseta.common.utils.dispatcherIO) { findRoutes(input, exact, predicate) }
}

fun Registry.findRoutesBlocking(input: String, exact: Boolean, coPredicate: (String, Route, Operator) -> Boolean): List<RouteSearchResultEntry> {
    return runBlocking(com.loohp.hkbuseta.common.utils.dispatcherIO) { findRoutes(input, exact, coPredicate) }
}

fun Registry.getNearbyRoutesBlocking(origin: Coordinates, excludedRouteNumbers: Set<String>, isInterchangeSearch: Boolean): Registry.NearbyRoutesResult {
    return runBlocking(com.loohp.hkbuseta.common.utils.dispatcherIO) { getNearbyRoutes(origin, 0.3, excludedRouteNumbers, isInterchangeSearch) }
}

fun Registry.getNearbyRoutesBlocking(origin: Coordinates, radius: Double, excludedRouteNumbers: Set<String>, isInterchangeSearch: Boolean): Registry.NearbyRoutesResult {
    return runBlocking(com.loohp.hkbuseta.common.utils.dispatcherIO) { getNearbyRoutes(origin, radius, excludedRouteNumbers, isInterchangeSearch) }
}

fun handleSearchInputLaunch(
    context: AppContext,
    input: Char,
    text: String,
    preRun: () -> Unit,
    loadingIndicator: () -> Unit,
    launch: (List<RouteSearchResultEntry>) -> Unit,
    complete: () -> Unit
) {
    CoroutineScope(com.loohp.hkbuseta.common.utils.dispatcherIO).launch {
        preRun.invoke()
        val job = CoroutineScope(com.loohp.hkbuseta.common.utils.dispatcherIO).launch {
            delay(500)
            loadingIndicator.invoke()
        }
        val result = when (input) {
            '!' -> Registry.getInstance(context).findRoutes("", false, Shared.MTR_ROUTE_FILTER)
            '~' -> Registry.getInstance(context).findRoutes("", false, Shared.FERRY_ROUTE_FILTER)
            '<' -> Registry.getInstance(context).findRoutes("", false, Shared.RECENT_ROUTE_FILTER)
            else -> Registry.getInstance(context).findRoutes(text, true)
        }
        if (result.isNotEmpty()) {
            launch.invoke(result)
        }
        job.cancelAndJoin()
        complete.invoke()
    }
}

@Immutable
data class RouteMapData(
    val width: Float,
    val height: Float,
    val stations: Map<String, Pair<Float, Float>>
) {
    companion object {
        fun fromString(input: String): RouteMapData {
            val json = Json.decodeFromString<JsonObject>(input)
            val dimension = json.optJsonObject("properties")!!.optJsonArray("dimension")!!
            val width = dimension.optDouble(0).toFloat()
            val height = dimension.optDouble(1).toFloat()
            val stops = json.optJsonObject("stops")!!.mapToMutableMap { it.jsonArray.let { a -> a.optDouble(0).toFloat() to a.optDouble(1).toFloat() } }
            return RouteMapData(width, height, stops)
        }
    }

    fun findClickedStations(x: Float, y: Float): String? {
        return stations.entries
            .asSequence()
            .map { it to ((it.value.first - x).sq() + (it.value.second - y).sq()) }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= 40000 }
            ?.first?.key
    }
}

private fun Float.sq(): Float = this * this