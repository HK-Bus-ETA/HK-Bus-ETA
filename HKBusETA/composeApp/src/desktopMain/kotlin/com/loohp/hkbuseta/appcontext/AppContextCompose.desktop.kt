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
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.StringReadChannel
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.common.utils.isReachable
import com.loohp.hkbuseta.common.utils.normalizeUrlScheme
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import io.ktor.utils.io.charsets.Charset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.UIManager
import kotlin.math.roundToInt


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
        return File(fileName).inputStream().toStringReadChannel(charset)
    }

    override suspend fun writeTextFile(fileName: String, writeText: () -> StringReadChannel) {
        File(fileName).outputStream().use {
            writeText.invoke().transferTo(it)
            it.flush()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> writeTextFile(fileName: String, json: Json, writeJson: () -> Pair<SerializationStrategy<T>, T>) {
        File(fileName).outputStream().use {
            val (serializer, value) = writeJson.invoke()
            json.encodeToStream(serializer, value, it)
            it.flush()
        }
    }

    override suspend fun listFiles(): List<String> {
        return File(".").listFiles()?.map { it.name }?: emptyList()
    }

    override suspend fun deleteFile(fileName: String): Boolean {
        return File(fileName).delete()
    }

    override fun syncPreference(preferences: Preferences) {
        //do nothing
    }

    override fun requestPreferencesIfPossible(): Deferred<Boolean> {
        return CompletableDeferred(false)
    }

    override fun hasConnection(): Boolean {
        return runBlocking(dispatcherIO) { isReachable(Registry.checksumUrl()) }
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

    override fun showToastText(text: String, duration: ToastDuration) {
        ToastTextState.toastState.value = ToastTextData(text, duration)
    }

    override fun formatTime(localDateTime: LocalDateTime): String {
        val dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
        val pattern = (dateFormat as? SimpleDateFormat)?.toPattern()?: "HH:mm"
        return localDateTime.time.toJavaLocalTime().format(DateTimeFormatter.ofPattern(pattern))
    }

    override fun formatDateTime(localDateTime: LocalDateTime, includeTime: Boolean): String {
        val dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT)
        val pattern = (dateFormat as? SimpleDateFormat)?.toPattern()?: "dd/MM/yyyy"
        return localDateTime.toJavaLocalDateTime().format(DateTimeFormatter.ofPattern(pattern)) + (if (includeTime) " " + formatTime(localDateTime) else "")
    }

    override fun setAppShortcut(id: String, shortLabel: String, longLabel: String, icon: AppShortcutIcon, tint: Long?, rank: Int, url: String) {
        //do nothing
    }

}

@Stable
class AppActiveContextComposeDesktop internal constructor(
    override val screen: AppScreen,
    override val data: Map<String, Any?>,
    override val flags: Set<AppIntentFlag>,
    private val finishCallback: ((AppIntentResult) -> Unit)? = null
) : AppContextComposeDesktop(), AppActiveContextCompose {

    val activeContextId = uuid4()
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
        return {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI(url.normalizeUrlScheme()))
                }
            }
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
        //do nothing
    }

    override fun readFileFromFileChooser(fileType: String, read: (String) -> Unit) {
        CoroutineScope(dispatcherIO).launch {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = if (Shared.language == "en") "Import" else "匯人"
            fileChooser.dialogType = JFileChooser.OPEN_DIALOG
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedFile = fileChooser.selectedFile
                read.invoke(selectedFile.readLines(Charsets.UTF_8).joinToString(""))
            }
        }
    }

    override fun writeFileToFileChooser(fileType: String, fileName: String, file: String, onSuccess: () -> Unit) {
        CoroutineScope(dispatcherIO).launch {
            val fileChooser = JFileChooser()
            fileChooser.selectedFile = File(fileName)
            fileChooser.dialogTitle = if (Shared.language == "en") "Export" else "匯出"
            fileChooser.dialogType = JFileChooser.SAVE_DIALOG
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedFile = fileChooser.selectedFile
                if (selectedFile.exists()) {
                    val response = JOptionPane.showConfirmDialog(null,
                        if (Shared.language == "en") "The file already exists. Do you want to overwrite the existing file?" else "這個文件已經存在。你想覆蓋已有的文件嗎？",
                        if (Shared.language == "en") "File Exists" else "文件已經存在", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
                    )
                    if (response != JOptionPane.YES_OPTION) return@launch
                }
                selectedFile.printWriter(Charsets.UTF_8).use { writer ->
                    writer.write(file)
                    writer.flush()
                }
                onSuccess.invoke()
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
        if (other !is AppActiveContextComposeDesktop) return false

        return activeContextId == other.activeContextId
    }

    override fun hashCode(): Int {
        return activeContextId.hashCode()
    }

}

actual val applicationAppContext: AppContextCompose = AppContextComposeDesktop()
actual fun initialScreen(): AppActiveContextCompose = AppActiveContextComposeDesktop(AppScreen.MAIN, emptyMap(), emptySet())
actual fun handleEmptyStack(stack: MutableList<AppActiveContextCompose>) { stack.add(initialScreen()) }