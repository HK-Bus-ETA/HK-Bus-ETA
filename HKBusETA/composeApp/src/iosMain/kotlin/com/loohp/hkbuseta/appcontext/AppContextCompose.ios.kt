/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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

@file:OptIn(ExperimentalForeignApi::class, ExperimentalForeignApi::class, ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalForeignApi::class)

package com.loohp.hkbuseta.appcontext

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppIntentResult
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.AppShortcutIcon
import com.loohp.hkbuseta.common.appcontext.FormFactor
import com.loohp.hkbuseta.common.appcontext.HapticFeedback
import com.loohp.hkbuseta.common.appcontext.applicationBaseAppContext
import com.loohp.hkbuseta.common.appcontext.withGlobalWritingFilesCounter
import com.loohp.hkbuseta.common.external.extractShareLink
import com.loohp.hkbuseta.common.external.shareLaunch
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.objects.getColor
import com.loohp.hkbuseta.common.services.AlightReminderActiveState
import com.loohp.hkbuseta.common.services.AlightReminderService
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Tiles
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.StringReadChannel
import com.loohp.hkbuseta.common.utils.normalizeUrlScheme
import com.loohp.hkbuseta.common.utils.pad
import com.loohp.hkbuseta.common.utils.provideGzipBodyAsTextImpl
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toNSDateComponents
import kotlinx.serialization.json.Json
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSBundle
import platform.Foundation.NSCalendar
import platform.Foundation.NSData
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.isLowPowerModeEnabled
import platform.Foundation.writeToURL
import platform.MapKit.MKMapItem
import platform.MapKit.MKPlacemark
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithAddress
import platform.SystemConfiguration.SCNetworkReachabilityFlags
import platform.SystemConfiguration.SCNetworkReachabilityFlagsVar
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkFlagsReachable
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationShortcutIcon
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UIScreen
import platform.UIKit.shortcutItems
import platform.WatchConnectivity.WCSession
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.AF_INET
import platform.posix.memcpy
import platform.posix.memset
import platform.posix.sleep
import platform.posix.sockaddr_in
import platform.posix.usleep
import kotlin.collections.set
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


data class ShareUrlData(val url: String, val title: String?)

data class AlightReminderData(
    val routeNumber: String,
    val stopsRemaining: String,
    val titleLeading: String,
    val titleTrailing: String,
    val content: String,
    val url: String,
    val state: Int,
    val color: Long,
    val active: Int
)

var windowWidth: Double = UIScreen.mainScreen.bounds.useContents { size.width }
var windowHeight: Double = UIScreen.mainScreen.bounds.useContents { size.height }

val statusColorState: MutableStateFlow<Color?> = MutableStateFlow(null)
val navColorState: MutableStateFlow<Color?> = MutableStateFlow(null)

private var firebaseImpl: (String, AppBundle) -> Unit = { _, _ -> }
private var fileChooserImportImpl: ((String) -> Unit) -> Unit = { /* do nothing */ }
private var fileChooserExportImpl: (String, String, () -> Unit) -> Unit = { _, _, _ -> }
private var syncPreferencesImpl: (String) -> Unit = { /* do nothing */ }
private var shareUrlDataImpl: (ShareUrlData) -> Unit = { /* do nothing */ }

fun setFirebaseLogImpl(handler: (String, AppBundle) -> Unit) {
    firebaseImpl = handler
}

fun setFileChooserImportImpl(handler: ((String) -> Unit) -> Unit) {
    fileChooserImportImpl = handler
}

fun setFileChooserExportImpl(handler: (String, String, () -> Unit) -> Unit) {
    fileChooserExportImpl = handler
}

fun setGzipBodyAsTextImpl(impl: ((NSData, String) -> String)?) {
    provideGzipBodyAsTextImpl(impl)
}

fun setSyncPreferencesImpl(handler: (String) -> Unit) {
    syncPreferencesImpl = handler
}

fun setTilesUpdateImpl(handler: () -> Unit) {
    Tiles.providePlatformUpdate(handler)
}

fun setShareUrlDataImpl(handler: (ShareUrlData) -> Unit) {
    shareUrlDataImpl = handler
}

@Stable
open class AppContextComposeIOS internal constructor() : AppContextCompose {

    override val packageName: String
        get() = NSBundle.mainBundle.bundleIdentifier?.split('.')?.subList(0, 3)?.joinToString(".") ?: "Unknown"

    override val versionName: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")?.toString()?: "Unknown"

    override val versionCode: Long
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")?.toString()?.toLong()?: -1L

    override val screenWidth: Int
        get() = (windowWidth * density).roundToInt()

    override val screenHeight: Int
        get() = (windowHeight * density).roundToInt()

    override val minScreenSize: Int
        get() = screenWidth.coerceAtMost(screenHeight)

    override val screenScale: Float
        get() = minScreenSize / 192F

    override val screenWidthScale: Float
        get() = screenWidth / 192F

    override val density: Float
        get() = UIScreen.mainScreen.scale.toFloat()

    override val scaledDensity: Float
        get() = density

    override val formFactor: FormFactor = FormFactor.NORMAL

    override suspend fun readTextFile(fileName: String, charset: Charset): StringReadChannel {
        return NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier("group.com.loohp.hkbuseta")!!.let { dir ->
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            val text = NSString.create(contentsOfURL = fileURL)
            text.toString().toStringReadChannel(charset)
        }
    }

    override suspend fun writeTextFile(fileName: String, writeText: () -> StringReadChannel) {
        withGlobalWritingFilesCounter {
            NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier("group.com.loohp.hkbuseta")!!.let { dir ->
                val fileURL = dir.URLByAppendingPathComponent(fileName)!!
                val text = writeText.invoke().string().ns
                text.writeToURL(fileURL, atomically = true)
            }
        }
    }

    override suspend fun readRawFile(fileName: String): ByteReadChannel {
        return NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier("group.com.loohp.hkbuseta")!!.let { dir ->
            val fileURL = dir.URLByAppendingPathComponent(fileName)!!
            val data = NSData.create(contentsOfURL = fileURL)!!
            ByteReadChannel(ByteArray(data.length.toInt()).apply {
                usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
            })
        }
    }

    override suspend fun writeRawFile(fileName: String, writeBytes: () -> ByteReadChannel) {
        withGlobalWritingFilesCounter {
            NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier("group.com.loohp.hkbuseta")!!.let { dir ->
                val fileURL = dir.URLByAppendingPathComponent(fileName)!!
                val array = writeBytes.invoke().toByteArray()
                val data = memScoped { NSData.create(bytes = allocArrayOf(array), length = array.size.convert()) }
                data.writeToURL(fileURL, atomically = true)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun listFiles(): List<String> {
        val fileManager = NSFileManager.defaultManager
        return fileManager.containerURLForSecurityApplicationGroupIdentifier("group.com.loohp.hkbuseta")!!.let { dir ->
            fileManager.contentsOfDirectoryAtURL(dir, includingPropertiesForKeys = null, options = 0u, error = null)?.mapNotNull {
                it as NSURL
                it.lastPathComponent
            }?: emptyList()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun deleteFile(fileName: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        return fileManager.containerURLForSecurityApplicationGroupIdentifier("group.com.loohp.hkbuseta")!!.let { dir ->
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
        return if (WCSession.defaultSession.let { it.isWatchAppInstalled() && it.isReachable() }) {
            WCSession.defaultSession.sendMessage(mapOf("messageType" to Shared.REQUEST_PREFERENCES_ID), { /* do nothing */ }, { /* do nothing */ })
            CompletableDeferred(true)
        } else {
            CompletableDeferred(false)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun hasConnection(): Boolean {
        return memScoped {
            val zeroAddress: sockaddr_in = alloc()
            memset(zeroAddress.ptr, 0.convert(), sizeOf<sockaddr_in>().convert())
            zeroAddress.sin_len = sizeOf<sockaddr_in>().convert()
            zeroAddress.sin_family = AF_INET.convert()
            val reachability = SCNetworkReachabilityCreateWithAddress(null, zeroAddress.ptr.reinterpret())
            val flags: SCNetworkReachabilityFlagsVar = alloc()
            if (SCNetworkReachabilityGetFlags(reachability, flags.ptr)) {
                val flagZero: SCNetworkReachabilityFlags = 0.convert()
                val isReachable = (flags.value and kSCNetworkFlagsReachable) != flagZero
                val needsConnection = (flags.value and kSCNetworkFlagsConnectionRequired) != flagZero
                isReachable && !needsConnection
            } else {
                false
            }
        }
    }

    override fun currentBackgroundRestrictions(): BackgroundRestrictionType {
        return if (NSProcessInfo.processInfo.isLowPowerModeEnabled()) BackgroundRestrictionType.POWER_SAVE_MODE else BackgroundRestrictionType.NONE
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
            add(AppActiveContextComposeIOS(appIntent.screen, appIntent.extras.data, appIntent.intentFlags))
        }
    }

    override fun startForegroundService(appIntent: AppIntent) {
        when (appIntent.screen) {
            AppScreen.ALIGHT_REMINDER_SERVICE -> { /* do nothing */ }
            else -> throw RuntimeException("Unsupported Platform Operation")
        }
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
        val item = UIApplicationShortcutItem(
            type = id,
            localizedTitle = shortLabel,
            localizedSubtitle = longLabel,
            icon = UIApplicationShortcutIcon.iconWithSystemImageName(icon.iosSystemImageName),
            userInfo = mapOf(
                "url".ns to url.ns,
                "rank".ns to rank.toString().ns
            )
        )
        val existingItems = UIApplication.sharedApplication.shortcutItems
        val newItems = if (existingItems.isNullOrEmpty()) {
            listOf(item)
        } else if (existingItems.any { it is UIApplicationShortcutItem && it.type == id }) {
            existingItems.map {
                if (it is UIApplicationShortcutItem && it.type == id) item else it
            }
        } else {
            existingItems + item
        }
        UIApplication.sharedApplication.shortcutItems = newItems.sortedBy {
            (it as? UIApplicationShortcutItem)?.userInfo?.get("rank")?.toString()?.toIntOrNull()?: Int.MAX_VALUE
        }
    }

    override fun removeAppShortcut(id: String) {
        val existingItems = UIApplication.sharedApplication.shortcutItems?: return
        UIApplication.sharedApplication.shortcutItems = existingItems.filterNot { (it as? UIApplicationShortcutItem)?.type == id }
    }

    override suspend fun <T> withHighBandwidthNetwork(block: suspend () -> T): T {
        return block.invoke()
    }

}

@OptIn(ExperimentalUuidApi::class)
@Stable
class AppActiveContextComposeIOS internal constructor(
    override val screen: AppScreen,
    override val data: MutableMap<String, Any?>,
    override val flags: Set<AppIntentFlag>,
    private val finishCallback: ((AppIntentResult) -> Unit)? = null
) : AppContextComposeIOS(), AppActiveContextCompose {

    val activeContextId = Uuid.random()
    private var result: AppIntentResult = AppIntentResult.NORMAL

    override fun runOnUiThread(runnable: () -> Unit) {
        dispatch_async(dispatch_get_main_queue(), runnable)
    }

    override fun startActivity(appIntent: AppIntent, callback: (AppIntentResult) -> Unit) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            add(AppActiveContextComposeIOS(appIntent.screen, appIntent.extras.data, appIntent.intentFlags, finishCallback = callback))
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun handleOpenMaps(lat: Double, lng: Double, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return {
            if (longClick) {
                UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            }
            val location = CLLocationCoordinate2DMake(lat, lng)
            val mapItem = MKMapItem(MKPlacemark(location))
            mapItem.name = label
            mapItem.openInMapsWithLaunchOptions(null)
        }
    }

    override fun handleWebpages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return {
            if (longClick) {
                UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            }
            val nsUrl = NSURL(string = url.normalizeUrlScheme())
            UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>(), null)
        }
    }

    override fun handleWebImages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return handleWebpages(url, longClick, haptics)
    }

    override fun setResult(result: AppIntentResult) {
        this.result = result
    }

    override fun completeFinishCallback() {
        finishCallback?.invoke(result)
    }

    override fun setStatusNavBarColor(status: Color?, nav: Color?) {
        statusColorState.value = status
        navColorState.value = nav
    }

    override fun readFileFromFileChooser(fileType: String, read: (String) -> Unit) {
        fileChooserImportImpl.invoke(read)
    }

    override fun writeFileToFileChooser(fileType: String, fileName: String, file: String, onSuccess: () -> Unit) {
        fileChooserExportImpl.invoke(fileName, file, onSuccess)
    }

    override fun shareUrl(url: String, title: String?) {
        shareUrlDataImpl.invoke(ShareUrlData(url, title))
    }

    override fun switchActivity(appIntent: AppIntent) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            val index = indexOf(this@AppActiveContextComposeIOS)
            add(index, AppActiveContextComposeIOS(appIntent.screen, appIntent.extras.data, appIntent.intentFlags))
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
        if (other !is AppActiveContextComposeIOS) return false

        return activeContextId == other.activeContextId
    }

    override fun hashCode(): Int {
        return activeContextId.hashCode()
    }

}

actual val applicationAppContext: AppContextCompose = AppContextComposeIOS().apply { applicationBaseAppContext = this }
actual fun initialScreen(): AppActiveContextCompose = AppActiveContextComposeIOS(AppScreen.MAIN, mutableMapOf(), emptySet())
actual fun handleEmptyStack(stack: MutableList<AppActiveContextCompose>) { stack.add(initialScreen()) }


fun String.extractShareLinkAndLaunch() = runBlocking(Dispatchers.IO) { extractShareLink()?.apply {
    val instance = HistoryStack.historyStack.value.last()
    instance.startActivity(AppIntent(instance, AppScreen.MAIN))
    instance.finishAffinity()
    delay(500)
    shareLaunch(instance, true)
} }

fun syncPreference(context: AppContext, preferenceJson: String, sync: Boolean) {
    runBlocking(Dispatchers.IO) {
        if (Registry.isNewInstall(applicationAppContext)) {
            Registry.writeRawPreference(preferenceJson, applicationAppContext)
        } else {
            val data = Preferences.deserialize(Json.decodeFromString(preferenceJson))
            Registry.getInstance(context).syncPreference(context, data, sync)
        }
    }
}

fun getRawPreference(context: AppContext): String {
    return runBlocking(Dispatchers.IO) { Registry.getRawPreferences(context).toString() }
}

fun isNewInstall(context: AppContext): Boolean {
    return runBlocking(Dispatchers.IO) { Registry.isNewInstall(context) }
}

const val START_ACTIVITY_ID = Shared.START_ACTIVITY_ID
const val SYNC_PREFERENCES_ID = Shared.SYNC_PREFERENCES_ID
const val REQUEST_PREFERENCES_ID = Shared.REQUEST_PREFERENCES_ID
const val REQUEST_ALIGHT_REMINDER_ID = Shared.REQUEST_ALIGHT_REMINDER_ID
const val RESPONSE_ALIGHT_REMINDER_ID = Shared.RESPONSE_ALIGHT_REMINDER_ID
const val UPDATE_ALIGHT_REMINDER_ID = Shared.UPDATE_ALIGHT_REMINDER_ID
const val TERMINATE_ALIGHT_REMINDER_ID = Shared.TERMINATE_ALIGHT_REMINDER_ID
const val INVALIDATE_CACHE_ID = Shared.INVALIDATE_CACHE_ID

fun getCurrentAlightReminderData(): Pair<AlightReminderData, String?>? {
    return AlightReminderService.currentInstance.valueNullable?.let {
        AlightReminderData(
            routeNumber = it.routeNumber,
            stopsRemaining = if (Shared.language == "en") "${it.stopsRemaining} Stops Left" else "剩餘${it.stopsRemaining}站",
            titleLeading = it.titleLeading,
            titleTrailing = it.titleTrailing,
            content = it.content,
            url = it.deepLink,
            state = it.state.ordinal,
            color = it.co.getColor(it.routeNumber, 0xFFFF4747),
            active = it.isActive.ordinal
        ) to it.remoteData.takeUnless { d -> d.active == AlightReminderActiveState.STOPPED }?.serialize()?.toString()
    }
}

fun setAlightReminderHandler(handler: (AlightReminderData?, String?) -> Unit) {
    AlightReminderService.updateSubscribes["Listener"] = {
        getCurrentAlightReminderData()?.let { (d, s) -> handler.invoke(d, s) }?: handler.invoke(null, null)
    }
}

fun terminateAlightReminder() {
    AlightReminderService.kill()
}

fun provideBackgroundUpdateScheduler(handler: (AppContext, Long) -> Unit) {
    Shared.provideBackgroundUpdateScheduler(handler)
}

fun nextScheduledDataUpdateMillis(): Long {
    return com.loohp.hkbuseta.common.utils.nextScheduledDataUpdateMillis()
}

@OptIn(ExperimentalForeignApi::class)
fun runDailyUpdate(onComplete: () -> Unit) {
    val registry = Registry.getInstance(applicationAppContext)
    while (registry.state.value.isProcessing) {
        usleep(100000.convert())
    }
    sleep(2.convert())
    onComplete.invoke()
}

val String.ns get() = NSString.create(string = this)

val AppShortcutIcon.iosSystemImageName: String get() = when (this) {
    AppShortcutIcon.STAR -> "star.fill"
    AppShortcutIcon.HISTORY -> "clock.arrow.circlepath"
    AppShortcutIcon.SEARCH -> "magnifyingglass"
    AppShortcutIcon.NEAR_ME -> "location.fill"
}

fun invalidateCacheAndRestart() {
    runBlocking(Dispatchers.Main) {
        Registry.invalidateCache(applicationAppContext)
        Registry.clearInstance()
        Registry.getInstance(applicationAppContext)
        val intent = AppIntent(applicationAppContext, AppScreen.MAIN)
        intent.addFlags(AppIntentFlag.NEW_TASK)
        applicationAppContext.startActivity(intent)
    }
}
