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

package com.loohp.hkbuseta.common.appcontext

import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.utils.BackgroundRestrictionType
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.StringReadChannel
import com.loohp.hkbuseta.common.utils.toStringReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.Deferred
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json


@RequiresOptIn(message = "Extra care should be taken as this might be not available on some platforms.")
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class ReduceDataOmitted

enum class FormFactor(val reduceData: Boolean) {

    NORMAL(false),
    WATCH(true)

}

enum class AppShortcutIcon {
    STAR, HISTORY, SEARCH, NEAR_ME
}

var primaryThemeColor: Long? = null

@Immutable
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
    val formFactor: FormFactor

    suspend fun readTextFile(fileName: String, charset: Charset = Charsets.UTF_8): StringReadChannel

    suspend fun writeTextFile(fileName: String, writeText: () -> StringReadChannel)

    suspend fun <T> writeTextFile(fileName: String, json: Json = Json, writeJson: () -> Pair<SerializationStrategy<T>, T>) {
        writeTextFile(fileName) {
            val (serializer, value) = writeJson.invoke()
            json.encodeToString(serializer, value).toStringReadChannel(Charsets.UTF_8)
        }
    }

    suspend fun listFiles(): List<String>

    suspend fun deleteFile(fileName: String): Boolean

    fun syncPreference(preferences: Preferences)

    fun requestPreferencesIfPossible(): Deferred<Boolean>

    fun hasConnection(): Boolean

    fun currentBackgroundRestrictions(): BackgroundRestrictionType

    fun logFirebaseEvent(title: String, values: AppBundle)

    fun getResourceString(resId: Int): String

    fun isScreenRound(): Boolean

    fun startActivity(appIntent: AppIntent)

    fun startForegroundService(appIntent: AppIntent)

    fun showToastText(text: String, duration: ToastDuration)

    fun formatTime(localDateTime: LocalDateTime): String

    fun formatDateTime(localDateTime: LocalDateTime, includeTime: Boolean): String

    fun setAppShortcut(id: String, shortLabel: String, longLabel: String, icon: AppShortcutIcon, tint: Long? = null, rank: Int, url: String)

}

@Immutable
interface AppActiveContext : AppContext {

    fun runOnUiThread(runnable: () -> Unit)

    fun startActivity(appIntent: AppIntent, callback: (AppIntentResult) -> Unit)

    fun handleOpenMaps(lat: Double, lng: Double, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit

    fun handleOpenMaps(coordinates: Coordinates, label: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
        return handleOpenMaps(coordinates.lat, coordinates.lng, label, longClick, haptics)
    }

    fun handleWebpages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit

    fun handleWebImages(url: String, longClick: Boolean, haptics: HapticFeedback): () -> Unit

    fun setResult(resultCode: Int) {
        setResult(AppIntentResult(resultCode))
    }

    fun setResult(result: AppIntentResult)

    fun finish()

    fun finishAffinity()

}

interface HapticFeedback {

    fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType)

}

enum class HapticFeedbackType {
    LongPress, TextHandleMove;
}