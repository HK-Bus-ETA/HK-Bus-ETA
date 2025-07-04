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

package com.loohp.hkbuseta

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.setVersionImpl
import com.loohp.hkbuseta.common.external.extractShareLink
import com.loohp.hkbuseta.common.external.shareLaunch
import com.loohp.hkbuseta.common.objects.Theme
import com.loohp.hkbuseta.common.shared.BASE_URL
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.URL_ROUTE
import com.loohp.hkbuseta.common.shared.URL_STACK_COUNT
import com.loohp.hkbuseta.common.utils.asString
import com.loohp.hkbuseta.common.utils.provideGzipBodyAsTextImpl
import com.loohp.hkbuseta.common.utils.remove
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.utils.awaitCallback
import io.ktor.http.decodeURLPart
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


external fun canDecodeGzip(): Boolean
external fun decodeGzip(data: String, callback: (String) -> Unit)
external fun isAppleDevice(): Boolean
external fun isMobileDevice(): Boolean
external fun isStandaloneApp(): Boolean
external fun setDownloadAppSheetVisible(isApple: Boolean, visible: Boolean, forceDarkMode: Boolean, wasmSupported: Boolean)
external fun isDownloadAppSheetVisible(): Boolean
external fun isWasmSupported(callback: (Boolean) -> Unit)
external fun setThemeColor(color: Int, useDarkTheme: Boolean)

suspend fun isWasmSupported(): Boolean = awaitCallback { isWasmSupported { complete(it) } }

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    setVersionImpl { Triple("HKBusETA", "2.5.5", 60) }
    if (canDecodeGzip()) {
        provideGzipBodyAsTextImpl { data, charset ->
            val defer = CompletableDeferred<String>()
            decodeGzip(data.encodeBase64()) {
                defer.complete(it.decodeBase64Bytes().asString(charset))
            }
            defer.await()
        }
    }
    window.onbeforeunload = {
        it.preventDefault()
        (if (Shared.language == "en") "Are you sure you want to quit the app?" else "確定退出應用程式？").apply { it.returnValue = this }
    }
    window.history.replaceState(null, "", "${BASE_URL.remove("/#")}/#$URL_ROUTE")
    window.addEventListener("popstate") {
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            if (URL_STACK_COUNT < HistoryStack.historyStack.value.size) {
                HistoryStack.historyStack.value.lastOrNull()?.finish()
            }
        }
    }
    ComposeViewport(viewportContainerId = "compose-target") {
        var language by remember { mutableStateOf(Shared.language) }
        val historyStack by HistoryStack.historyStack.collectAsStateMultiplatform()

        LaunchedEffect (Unit) {
            window.location.href.extractShareLink()?.shareLaunch(HistoryStack.historyStack.value.last(), true)
        }
        LaunchedEffect (Unit) {
            while (true) {
                language = Shared.language
                delay(500)
            }
        }
        LaunchedEffect (language) {
            window.document.title = if (language == "en") "HK Bus ETA" else "香港巴士到站預報"
            window.document.documentElement?.setAttribute("lang", language)
        }
        LaunchedEffect (historyStack) {
            delay(100)
            val url = "${BASE_URL.remove("/#")}${"/#".repeat(historyStack.size + 1)}$URL_ROUTE"
            if (window.location.href.decodeURLPart() != url.decodeURLPart()) {
                if (URL_STACK_COUNT > historyStack.size) {
                    window.history.back()
                    delay(100)
                    window.history.replaceState(null, "", url)
                } else {
                    window.history.pushState(null, "", url)
                }
            }
        }
        LaunchedEffect (Unit) {
            val wasmSupported = isWasmSupported()
            var forceDarkMode: Boolean? = null
            while (true) {
                val value = Shared.theme == Theme.DARK
                if (value != forceDarkMode) {
                    forceDarkMode = value
                    if (isDownloadAppSheetVisible()) {
                        setDownloadAppSheetVisible(isAppleDevice(), true, value, wasmSupported)
                    }
                }
                delay(100)
            }
        }

        App { window.document.getElementById("splash")?.remove() }
    }
}