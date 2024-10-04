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

package com.loohp.hkbuseta.appcontext

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.benasher44.uuid.uuid4
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppIntentResult
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.AppShortcutIcon
import com.loohp.hkbuseta.common.appcontext.FormFactor
import com.loohp.hkbuseta.common.appcontext.HapticFeedback
import com.loohp.hkbuseta.common.appcontext.withGlobalWritingFilesCounter
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.StringReadChannel
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.common.utils.normalizeUrlScheme
import com.loohp.hkbuseta.common.utils.pad
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import com.loohp.hkbuseta.utils.awaitCallback
import com.loohp.hkbuseta.utils.copyToClipboard
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import io.ktor.util.toByteArray
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt


external fun writeToIndexedDB(key: String, data: String)
external fun readFromIndexedDB(key: String, callback: (String) -> Unit)
external fun listAllKeysInIndexedDB(callback: (String) -> Unit)
external fun deleteFromIndexedDB(key: String, callback: (Boolean) -> Unit)
external fun readFile(callback: (String) -> Unit)
external fun writeFile(fileName: String, fileContent: String)
external fun logFirebase(name: String, keyValues: String)
external fun shareUrlMenu(url: String, title: String?): Boolean


private var versionImpl: () -> Triple<String, String, Long> = { Triple("Unknown", "Unknown", -1) }

fun setVersionImpl(handler: () -> Triple<String, String, Long>) {
    versionImpl = handler
}

@Stable
open class AppContextComposeWeb internal constructor() : AppContextCompose {

    override val packageName: String
        get() = versionImpl.invoke().first

    override val versionName: String
        get() = versionImpl.invoke().second

    override val versionCode: Long
        get() = versionImpl.invoke().third

    override val screenWidth: Int
        get() = (window.innerWidth * density).roundToInt()

    override val screenHeight: Int
        get() = (window.innerHeight * density).roundToInt()

    override val minScreenSize: Int
        get() = screenWidth.coerceAtMost(screenHeight)

    override val screenScale: Float
        get() = minScreenSize / 250F

    override val screenWidthScale: Float
        get() = screenWidth / 250F

    override val density: Float
        get() = window.devicePixelRatio.toFloat()

    override val scaledDensity: Float
        get() = density

    override val formFactor: FormFactor = FormFactor.NORMAL

    override suspend fun readTextFile(fileName: String, charset: Charset): StringReadChannel {
        return awaitCallback { readFromIndexedDB(fileName) { complete(it) } }.toStringReadChannel(charset)
    }

    override suspend fun writeTextFile(fileName: String, writeText: () -> StringReadChannel) {
        withGlobalWritingFilesCounter {
            writeToIndexedDB(fileName, writeText.invoke().string())
        }
    }

    override suspend fun readRawFile(fileName: String): ByteReadChannel {
        return ByteReadChannel(readTextFile(fileName).string().decodeBase64Bytes())
    }

    override suspend fun writeRawFile(fileName: String, writeBytes: () -> ByteReadChannel) {
        withGlobalWritingFilesCounter {
            val string = writeBytes.invoke().toByteArray().encodeBase64()
            writeTextFile(fileName) { string.toStringReadChannel() }
        }
    }

    override suspend fun listFiles(): List<String> {
        return awaitCallback { listAllKeysInIndexedDB { complete(it.split("\u0000")) } }
    }

    override suspend fun deleteFile(fileName: String): Boolean {
        return awaitCallback { deleteFromIndexedDB(fileName) { complete(it) } }
    }

    override fun syncPreference(preferences: Preferences) {
        //do nothing
    }

    override fun requestPreferencesIfPossible(): Deferred<Boolean> {
        return CompletableDeferred(false)
    }

    override fun hasConnection(): Boolean {
        return window.navigator.onLine
    }

    override fun currentBackgroundRestrictions(): BackgroundRestrictionType {
        return BackgroundRestrictionType.NONE
    }

    override fun logFirebaseEvent(title: String, values: AppBundle) {
        logFirebase(title, values.data.entries.joinToString("\u0000") { (k, v) -> "$k\u0000$v" })
    }

    override fun getResourceString(resId: Int): String {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun isScreenRound(): Boolean {
        return false
    }

    override fun startActivity(appIntent: AppIntent) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            add(AppActiveContextComposeWeb(appIntent.screen, appIntent.extras.data, appIntent.intentFlags))
        }
    }

    override fun startForegroundService(appIntent: AppIntent) {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun formatTime(localDateTime: LocalDateTime): String {
        return "${localDateTime.hour.pad(2)}:${localDateTime.minute.pad(2)}"
    }

    override fun formatDateTime(localDateTime: LocalDateTime, includeTime: Boolean): String {
        val date = "${localDateTime.year}/${localDateTime.monthNumber}/${localDateTime.dayOfMonth}"
        return if (includeTime) {
            "$date ${formatTime(localDateTime)}"
        } else {
            date
        }
    }

    override fun setAppShortcut(id: String, shortLabel: String, longLabel: String, icon: AppShortcutIcon, tint: Long?, rank: Int, url: String) {
        //do nothing
    }

    override fun removeAppShortcut(id: String) {
        //do nothing
    }

    override suspend fun <T> withHighBandwidthNetwork(block: suspend () -> T): T {
        return block.invoke()
    }

}

@Stable
class AppActiveContextComposeWeb internal constructor(
    override val screen: AppScreen,
    override val data: MutableMap<String, Any?>,
    override val flags: Set<AppIntentFlag>,
    private val finishCallback: ((AppIntentResult) -> Unit)? = null
) : AppContextComposeWeb(), AppActiveContextCompose {

    val activeContextId = uuid4()
    private var result: AppIntentResult = AppIntentResult.NORMAL

    override fun runOnUiThread(runnable: () -> Unit) {
        runnable.invoke()
    }

    override fun startActivity(appIntent: AppIntent, callback: (AppIntentResult) -> Unit) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            add(AppActiveContextComposeWeb(appIntent.screen, appIntent.extras.data, appIntent.intentFlags, finishCallback = callback))
        }
    }

    override fun handleOpenMaps(lat: Double, lng: Double, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return handleWebpages("https://www.google.com/maps/search/?api=1&query=$lat%2C$lng", longClick, haptics)
    }

    override fun handleWebpages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return { window.open(url.normalizeUrlScheme(), "_blank") }
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
        //do nothing
    }

    override fun readFileFromFileChooser(fileType: String, read: (String) -> Unit) {
        readFile { read.invoke(it) }
    }

    override fun writeFileToFileChooser(fileType: String, fileName: String, file: String, onSuccess: () -> Unit) {
        writeFile(fileName, file)
        onSuccess.invoke()
    }

    override fun shareUrl(url: String, title: String?) {
        if (!shareUrlMenu(url, title)) {
            CoroutineScope(dispatcherIO).launch { copyToClipboard(url) }
        }
    }

    override fun switchActivity(appIntent: AppIntent) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            val index = indexOf(this@AppActiveContextComposeWeb)
            add(index, AppActiveContextComposeWeb(appIntent.screen, appIntent.extras.data, appIntent.intentFlags))
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
            val currentKey = newScreenGroup()
            val lastScreen = stack.lastOrNull()
            if (currentKey != AppScreenGroup.MAIN && lastScreen?.newScreenGroup() == currentKey) {
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
        val currentKey = newScreenGroup()
        val lastScreen = stack.lastOrNull()
        if (currentKey != AppScreenGroup.MAIN && lastScreen?.newScreenGroup() == currentKey) {
            lastScreen.finish()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppActiveContextComposeWeb) return false

        return activeContextId == other.activeContextId
    }

    override fun hashCode(): Int {
        return activeContextId.hashCode()
    }

}

actual val applicationAppContext: AppContextCompose = AppContextComposeWeb()
actual fun initialScreen(): AppActiveContextCompose = AppActiveContextComposeWeb(AppScreen.MAIN, mutableMapOf(), emptySet())
actual fun handleEmptyStack(stack: MutableList<AppActiveContextCompose>) { stack.add(initialScreen()) }