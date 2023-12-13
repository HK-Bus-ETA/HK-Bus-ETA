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

import androidx.compose.runtime.Stable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import com.loohp.hkbuseta.utils.BackgroundRestrictionType
import io.ktor.utils.io.charsets.Charset


@Stable
interface AppContext {

    val packageName: String
    val versionName: String
    val versionCode: Long
    val screenWidth: Int
    val screenHeight: Int
    val minScreenSize: Int
    val screenScale: Float
    val density: Float
    val scaledDensity: Float

    fun readTextFile(fileName: String, charset: Charset = Charsets.UTF_8): String {
        return readTextFileSequence(fileName, charset).joinToString()
    }

    fun readTextFileSequence(fileName: String, charset: Charset = Charsets.UTF_8): List<String>

    fun writeTextFile(fileName: String, charset: Charset = Charsets.UTF_8, writeText: () -> String) {
        writeTextFileSequence(fileName, charset) { listOf(writeText.invoke()) }
    }

    fun writeTextFileSequence(fileName: String, charset: Charset = Charsets.UTF_8, writeText: () -> List<String>)

    fun listFiles(): List<String>

    fun deleteFile(fileName: String): Boolean

    fun hasConnection(): Boolean

    fun currentBackgroundRestrictions(): BackgroundRestrictionType

    fun logFirebaseEvent(title: String, values: AppBundle)

    fun getResourceString(resId: Int): String

    fun startActivity(appIntent: AppIntent)

    fun startForegroundService(appIntent: AppIntent)

    fun showToastText(text: String, duration: ToastDuration)

}

@Stable
interface AppActiveContext : AppContext {

    fun runOnUiThread(runnable: () -> Unit)

    fun startActivity(appIntent: AppIntent, callback: (AppIntentResult) -> Unit)

    fun handleOpenMaps(lat: Double, lng: Double, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit

    fun handleWebpages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit

    fun handleWebImages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit

    fun setResult(resultCode: Int) {
        setResult(AppIntentResult(resultCode))
    }

    fun setResult(result: AppIntentResult)

    fun finish()

    fun finishAffinity()

}