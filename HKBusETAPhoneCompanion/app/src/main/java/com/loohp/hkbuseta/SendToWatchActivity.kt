package com.loohp.hkbuseta

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
import com.loohp.hkbuseta.utils.RemoteActivityUtils
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
val HKBUSAPP_URL_PATTERN: Pattern = Pattern.compile("https://hkbus\\.app/.+/route/([^/]*)")


class SendToWatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HKBusETATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1A1A1A)
                ) {
                    DisplayElements(intent, this)
                }
            }
        }
    }
}

fun sendPayload(instance: SendToWatchActivity, payload: JSONObject) {
    RemoteActivityUtils.dataToWatch(instance, START_ACTIVITY_PATH, payload, {
        instance.runOnUiThread {
            Toast.makeText(instance, R.string.send_no_watch, Toast.LENGTH_LONG)
                .show()
            instance.finish()
        }
    }, {
        instance.runOnUiThread {
            Toast.makeText(instance, R.string.send_failed, Toast.LENGTH_LONG).show()
            instance.finish()
        }
    }, {
        instance.runOnUiThread {
            Toast.makeText(instance, R.string.send_success, Toast.LENGTH_LONG)
                .show()
            instance.finish()
        }
    })
}

@Composable
fun DisplayElements(intent: Intent, instance: SendToWatchActivity) {
    LaunchedEffect (Unit) {
        ForkJoinPool.commonPool().execute {
            val action = intent.action
            val type = intent.type

            var matcher: Matcher
            if (action == "android.intent.action.SEND" && type != null && type == "text/plain") {
                val url = intent.getStringExtra("android.intent.extra.TEXT")!!.replace("\n", "")
                Log.d("a", url)
                if (url.startsWith(KMB_URL_STARTS_WITH) || url.startsWith(KMB_DIRECT_URL_STARTS_WITH)) {
                    val realUrl = if (url.startsWith(KMB_URL_STARTS_WITH)) HTTPRequestUtils.getMovedRedirect(url) else url
                    val urlDecoded = URLDecoder.decode(realUrl, StandardCharsets.UTF_8.name())
                    val parameter = urlDecoded.substring("https://m4.kmb.hk/kmb-ws/share.php?parameter=".length)
                    val data = JSONObject(String(Base64.decode(parameter, Base64.DEFAULT)))
                    val route = data.optString("r")
                    val bound = if (data.optString("b") == "1") "O" else "I"

                    val payload = JSONObject()
                    payload.put("r", route)
                    payload.put("b", bound)
                    payload.put("c", "kmb")

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

                    sendPayload(instance, payload)
                } else {
                    instance.runOnUiThread {
                        Toast.makeText(instance, R.string.send_malformed, Toast.LENGTH_LONG).show()
                        instance.finish()
                    }
                }
            } else {
                instance.runOnUiThread {
                    Toast.makeText(instance, R.string.send_malformed, Toast.LENGTH_LONG).show()
                    instance.finish()
                }
            }
        }
    }
}