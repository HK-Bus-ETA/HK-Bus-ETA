/*
 * This file is part of HKBusETA Phone Companion.
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

package com.loohp.hkbuseta

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.loohp.hkbuseta.ui.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.HTTPRequestUtils
import com.loohp.hkbuseta.utils.ImmutableState
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.asImmutableState
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ForkJoinPool
import java.util.regex.Matcher
import java.util.regex.Pattern


const val START_ACTIVITY_PATH = "/HKBusETA/Launch"
const val KMB_URL_STARTS_WITH = "https://app1933.page.link"
const val KMB_DIRECT_URL_STARTS_WITH = "https://m4.kmb.hk/kmb-ws/share.php?parameter="
val CTB_URL_PATTERN: Pattern = Pattern.compile("(?:城巴|Citybus) ?App: ?([0-9A-Za-z]+) ?(?:往|To) ?(.*)?http")
val HKBUSAPP_URL_PATTERN: Pattern = Pattern.compile("https://(?:(?:watch|wear)\\.)?hkbus\\.app/(?:.+/)?route/([^/]*)(?:/([^/]*)(?:%2C|,)([^/]*))?")


class SendToWatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HKBusETATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1A1A1A)
                ) {
                    DisplayElements(intent.extractUrl(), this)
                }
            }
        }
    }
}

fun sendPayload(instance: SendToWatchActivity, payload: JSONObject) {
    RemoteActivityUtils.dataToWatch(instance, START_ACTIVITY_PATH, payload, {
        instance.runOnUiThread {
            Toast.makeText(instance, R.string.send_no_watch, Toast.LENGTH_LONG).show()
            instance.finishAffinity()
        }
    }, {
        instance.runOnUiThread {
            Toast.makeText(instance, R.string.send_failed, Toast.LENGTH_LONG).show()
            instance.finishAffinity()
        }
    }, {
        instance.runOnUiThread {
            Toast.makeText(instance, R.string.send_success, Toast.LENGTH_LONG).show()
            instance.finishAffinity()
        }
    })
}

fun Intent.extractUrl(): String? {
    return when (action) {
        Intent.ACTION_SEND -> if (type == "text/plain") getStringExtra(Intent.EXTRA_TEXT)?.replace("\n", "") else null
        Intent.ACTION_VIEW -> data.toString()
        else -> null
    }
}

@Composable
fun DisplayElements(url: String?, instance: SendToWatchActivity) {
    LaunchedEffect (Unit) {
        ForkJoinPool.commonPool().execute {
            var matcher: Matcher
            if (url != null) {
                if (url.startsWith(KMB_URL_STARTS_WITH) || url.startsWith(KMB_DIRECT_URL_STARTS_WITH)) {
                    val realUrl = if (url.startsWith(KMB_URL_STARTS_WITH)) HTTPRequestUtils.getMovedRedirect(url) else url
                    val urlDecoded = URLDecoder.decode(realUrl, StandardCharsets.UTF_8.name())
                    val parameter = urlDecoded.substring("https://m4.kmb.hk/kmb-ws/share.php?parameter=".length)
                    val data = JSONObject(String(Base64.decode(parameter, Base64.DEFAULT)))
                    val route = data.optString("r")
                    val co = when (data.optString("c").substring(0, 2)) {
                        "NL" -> "nlb"
                        "GB" -> "gmb"
                        else -> "kmb"
                    }

                    val payload = JSONObject()
                    payload.put("r", route)
                    payload.put("c", co)
                    if (co == "kmb") {
                        payload.put("b", if (data.optString("b") == "1") "O" else "I")
                    }

                    sendPayload(instance, payload)
                } else if (CTB_URL_PATTERN.matcher(url).also { matcher = it }.find()) {
                    val route = matcher.group(1)!!
                    val dest = matcher.group(2)!!

                    val payload = JSONObject()
                    payload.put("r", route)
                    payload.put("d", dest.trim())

                    sendPayload(instance, payload)
                } else if (HKBUSAPP_URL_PATTERN.matcher(url).also { matcher = it }.find()) {
                    val key = matcher.group(1)!!

                    val payload = JSONObject()
                    payload.put("k", key)

                    matcher.group(2)?.let { if (it.isNotBlank()) payload.put("s", it) }
                    matcher.group(3)?.let { if (it.isNotBlank()) it.toIntOrNull()?.let { i -> payload.put("si", i) } }

                    sendPayload(instance, payload)
                } else {
                    instance.runOnUiThread {
                        Toast.makeText(instance, R.string.send_malformed, Toast.LENGTH_LONG).show()
                        instance.finishAffinity()
                    }
                }
            } else {
                instance.runOnUiThread {
                    Toast.makeText(instance, R.string.send_malformed, Toast.LENGTH_LONG).show()
                    instance.finishAffinity()
                }
            }
        }
    }
}