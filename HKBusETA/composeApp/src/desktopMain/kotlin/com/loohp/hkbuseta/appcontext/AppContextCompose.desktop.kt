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

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppIntentResult
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.AppShortcutIcon
import com.loohp.hkbuseta.common.appcontext.FormFactor
import com.loohp.hkbuseta.common.appcontext.HapticFeedback
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.appcontext.withGlobalWritingFilesCounter
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.DATA_FOLDER
import com.loohp.hkbuseta.common.utils.StringReadChannel
import com.loohp.hkbuseta.common.utils.isReachable
import com.loohp.hkbuseta.common.utils.normalizeUrlScheme
import com.loohp.hkbuseta.common.utils.timeFormatLocale
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import com.loohp.hkbuseta.utils.DesktopUtils
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import javafx.application.Platform
import javafx.stage.FileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileSystemView
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream


private var versionImpl: () -> Triple<String, String, Long> = { Triple("Unknown", "Unknown", -1) }
private var widthHeightDensityImpl: () -> Triple<Int, Int, Float> = { Triple(800, 800, 1F) }
private var firebaseImpl: (String, AppBundle) -> Unit = { _, _ -> }

fun setVersionImpl(handler: () -> Triple<String, String, Long>) {
    versionImpl = handler
}

fun setWidthHeightDensityImpl(handler: () -> Triple<Int, Int, Float>) {
    widthHeightDensityImpl = handler
}

fun setFirebaseLogImpl(handler: (String, AppBundle) -> Unit) {
    firebaseImpl = handler
}

@Stable
open class AppContextComposeDesktop internal constructor() : AppContextCompose {

    override val packageName: String
        get() = versionImpl.invoke().first

    override val versionName: String
        get() = versionImpl.invoke().second

    override val versionCode: Long
        get() = versionImpl.invoke().third

    override val screenWidth: Int
        get() = (widthHeightDensityImpl.invoke().first * density).roundToInt()

    override val screenHeight: Int
        get() = (widthHeightDensityImpl.invoke().second * density).roundToInt()

    override val minScreenSize: Int
        get() = screenWidth.coerceAtMost(screenHeight)

    override val screenScale: Float
        get() = minScreenSize / 250F

    override val screenWidthScale: Float
        get() = screenWidth / 250F

    override val density: Float
        get() = widthHeightDensityImpl.invoke().third

    override val scaledDensity: Float
        get() = density

    override val formFactor: FormFactor = FormFactor.NORMAL

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

    override fun syncPreference(preferences: Preferences) {
        //do nothing
    }

    override fun requestPreferencesIfPossible(): Deferred<Boolean> {
        return CompletableDeferred(false)
    }

    override fun hasConnection(): Boolean {
        return runBlocking(Dispatchers.IO) { isReachable(Registry.checksumUrl()) }
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
            add(AppActiveContextComposeDesktop(appIntent.screen, appIntent.extras.data, appIntent.intentFlags))
        }
    }

    override fun startForegroundService(appIntent: AppIntent) {
        throw RuntimeException("Unsupported Platform Operation")
    }

    override fun formatTime(localDateTime: LocalDateTime): String {
        val dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT, timeFormatLocale)
        val pattern = (dateFormat as? SimpleDateFormat)?.toPattern()?: "HH:mm"
        return localDateTime.time.toJavaLocalTime().format(DateTimeFormatter.ofPattern(pattern))
    }

    override fun formatDateTime(localDateTime: LocalDateTime, includeTime: Boolean): String {
        val dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, timeFormatLocale)
        val pattern = (dateFormat as? SimpleDateFormat)?.toPattern()?: "dd/MM/yyyy"
        return localDateTime.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern(pattern)) + (if (includeTime) " " + formatTime(localDateTime) else "")
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

@OptIn(ExperimentalUuidApi::class)
@Stable
class AppActiveContextComposeDesktop internal constructor(
    override val screen: AppScreen,
    override val data: MutableMap<String, Any?>,
    override val flags: Set<AppIntentFlag>,
    private val finishCallback: ((AppIntentResult) -> Unit)? = null
) : AppContextComposeDesktop(), AppActiveContextCompose {

    val activeContextId = Uuid.random()
    private var result: AppIntentResult = AppIntentResult.NORMAL

    override suspend fun readTextFile(fileName: String, charset: Charset): StringReadChannel {
        return super.readTextFile(fileName, charset)
    }

    override fun runOnUiThread(runnable: () -> Unit) {
        runnable.invoke()
    }

    override fun startActivity(appIntent: AppIntent, callback: (AppIntentResult) -> Unit) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            add(AppActiveContextComposeDesktop(appIntent.screen, appIntent.extras.data, appIntent.intentFlags, finishCallback = callback))
        }
    }

    override fun handleOpenMaps(lat: Double, lng: Double, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return handleWebpages("https://www.google.com/maps/search/?api=1&query=$lat%2C$lng", longClick, haptics)
    }

    override fun handleWebpages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return { DesktopUtils.browse(URI(url.normalizeUrlScheme())) }
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

    override fun readFileFromFileChooser(fileType: String, read: suspend (StringReadChannel) -> Unit) {
        Platform.runLater {
            val fileChooser = FileChooser().apply {
                title = if (Shared.language == "en") "Import" else "匯人"
                initialDirectory = FileSystemView.getFileSystemView().defaultDirectory
                extensionFilters += FileChooser.ExtensionFilter(if (Shared.language == "en") "All Files" else "所有檔案", "*.*")
            }
            val selectedFile = fileChooser.showOpenDialog(null)
            if (selectedFile != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    selectedFile.inputStream().use {
                        read.invoke(it.toStringReadChannel())
                    }
                }
            }
        }
    }

    override fun writeFileToFileChooser(fileType: String, fileName: String, file: String, onSuccess: () -> Unit) {
        Platform.runLater {
            val fileChooser = FileChooser().apply {
                title = if (Shared.language == "en") "Export" else "匯出"
                initialFileName = fileName
                initialDirectory = FileSystemView.getFileSystemView().defaultDirectory
                extensionFilters += FileChooser.ExtensionFilter(if (Shared.language == "en") "All Files" else "所有檔案", "*.*")
            }
            val selectedFile = fileChooser.showSaveDialog(null)
            if (selectedFile != null) {
                if (selectedFile.exists()) {
                    val response = JOptionPane.showConfirmDialog(null,
                        if (Shared.language == "en") "The file already exists. Do you want to overwrite the existing file?" else "此檔案已經存在，你想覆蓋已有的檔案嗎？",
                        if (Shared.language == "en") "File Exists" else "檔案已經存在", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
                    )
                    if (response != JOptionPane.YES_OPTION) return@runLater
                }
                selectedFile.printWriter(Charsets.UTF_8).use { writer ->
                    writer.write(file)
                    writer.flush()
                }
                onSuccess.invoke()
                DesktopUtils.openParentFolder(selectedFile)
            }
        }
    }

    override fun shareUrl(url: String, title: String?) {
        val stringSelection = StringSelection(url)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
        showToastText(if (Shared.language == "en") "Copied to Clipboard" else "已複製到剪貼板", ToastDuration.SHORT)
    }

    override fun switchActivity(appIntent: AppIntent) {
        HistoryStack.historyStack.value = HistoryStack.historyStack.value.toMutableList().apply {
            val index = indexOf(this@AppActiveContextComposeDesktop)
            add(index, AppActiveContextComposeDesktop(appIntent.screen, appIntent.extras.data, appIntent.intentFlags))
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
        if (other !is AppActiveContextComposeDesktop) return false

        return activeContextId == other.activeContextId
    }

    override fun hashCode(): Int {
        return activeContextId.hashCode()
    }

}

actual val applicationAppContext: AppContextCompose = AppContextComposeDesktop()
actual fun initialScreen(): AppActiveContextCompose = AppActiveContextComposeDesktop(AppScreen.MAIN, mutableMapOf(), emptySet())
actual fun handleEmptyStack(stack: MutableList<AppActiveContextCompose>) { stack.add(initialScreen()) }