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

package com.loohp.hkbuseta.common.external

import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.gmbRegion
import com.loohp.hkbuseta.common.objects.operator
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.getMovedRedirect
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.remove
import io.ktor.http.decodeURLPart
import io.ktor.util.decodeBase64String
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put


const val KMB_URL_STARTS_WITH = "https://app1933.page.link"
const val KMB_DIRECT_URL_STARTS_WITH = "https://m4.kmb.hk/kmb-ws/share.php?parameter="
val CTB_URL_PATTERN = "(?:城巴|Citybus) ?App: ?([0-9A-Za-z]+) ?(?:往|To) ?(.*)?http".toRegex()
val HKBUS_URL_PATTERN = "https://(?:(?:(?:watch|wear)\\.)?hkbus\\.app|(?:web|app)\\.hkbuseta\\.com)/(?:.+/)?route/([^/]*)(?:/([^/]*)(?:%2C|,)([^/]*))?".toRegex()
val HKBUS_LAUNCH_PATTERN = "https://(?:(?:(?:watch|wear)\\.)?hkbus\\.app|(?:web|app)\\.hkbuseta\\.com)".toRegex()
val HKBUS_SCREEN_PATTERN = "https://(?:(?:(?:watch|wear)\\.)?hkbus\\.app|(?:web|app)\\.hkbuseta\\.com)/(.*)".toRegex()

suspend fun String.extractShareLink(): JsonObject? {
    var matcher: MatchResult?
    if (this.startsWith(KMB_URL_STARTS_WITH) || this.startsWith(KMB_DIRECT_URL_STARTS_WITH)) {
        val realUrl = if (this.startsWith(KMB_URL_STARTS_WITH)) getMovedRedirect(this) else this
        val urlDecoded = realUrl!!.decodeURLPart(charset = Charsets.UTF_8)
        val parameter = urlDecoded.substring("https://m4.kmb.hk/kmb-ws/share.php?parameter=".length).remove("\\R".toRegex())
        val data = Json.decodeFromString<JsonObject>(parameter.decodeBase64String())
        val route = data.optString("r")
        val co = when (data.optString("c").substring(0, 2)) {
            "NL" -> "nlb"
            "GB" -> "gmb"
            else -> "kmb"
        }

        return buildJsonObject {
            put("r", route)
            put("c", co)
            if (co == "kmb") {
                put("b", if (data.optString("b") == "1") "O" else "I")
            }
        }
    } else if (CTB_URL_PATTERN.find(this).apply { matcher = this } != null) {
        val route = matcher!!.groupValues[1]
        val dest = matcher!!.groupValues[2]

        return buildJsonObject {
            put("r", route)
            put("d", dest.trim())
        }
    } else if (HKBUS_LAUNCH_PATTERN.find(this) != null) {
        if (HKBUS_URL_PATTERN.find(this).apply { matcher = this } != null) {
            val key = matcher!!.groupValues[1]

            return buildJsonObject {
                put("k", key.let { if (it.contains("%")) it.decodeURLPart() else it })
                matcher!!.groupValues.getOrNull(2)?.let { if (it.isNotBlank()) {
                    put("s", it)
                    put("sd", true)
                } }
                matcher!!.groupValues.getOrNull(3)?.let { if (it.isNotBlank()) it.toIntOrNull()?.let { i -> put("si", i) } }
            }
        } else if (HKBUS_SCREEN_PATTERN.find(this).apply { matcher = this } != null) {
            val screen = matcher!!.groupValues[1]
            return buildJsonObject {
                put("screen", screen)
            }
        }
    }
    return null
}

fun JsonObject.shareLaunch(instance: AppActiveContext, noAnimation: Boolean = false) {
    val queryKey = this["k"]?.jsonPrimitive?.content
    val queryRouteNumber = this["r"]?.jsonPrimitive?.content
    val queryBound = this["b"]?.jsonPrimitive?.content
    val queryCo = this["c"]?.jsonPrimitive?.content?.operator
    val queryDest = this["d"]?.jsonPrimitive?.content
    val queryGMBRegion = this["g"]?.jsonPrimitive?.content?.gmbRegion
    val queryStop = this["s"]?.jsonPrimitive?.content
    val queryStopIndex = this["si"]?.jsonPrimitive?.intOrNull?: 0
    val queryStopDirectLaunch = this["sd"]?.jsonPrimitive?.booleanOrNull == true
    val appScreen = this["screen"]?.jsonPrimitive?.content?.let { AppScreen.valueOfNullable(it) }

    Shared.handleLaunchOptions(instance, null, null, null, null, null, null, null, null, queryKey, queryRouteNumber, queryBound, queryCo, queryDest, queryGMBRegion, queryStop, queryStopIndex, queryStopDirectLaunch, appScreen, noAnimation) { /* do nothing */ }
}