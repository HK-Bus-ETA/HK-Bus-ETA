package com.loohp.hkbuseta.shared

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.FatalErrorActivity
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.isEqualTo
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess


data class CurrentActivityData(val cls: Class<Activity>, val extras: Bundle?) {

    fun isEqualTo(other: Any?): Boolean {
        return if (other is CurrentActivityData) {
            this.cls == other.cls && ((this.extras == null && other.extras == null) || (this.extras != null && this.extras.isEqualTo(other.extras)))
        } else {
            false
        }
    }

}

class Shared {

    companion object {

        const val ETA_UPDATE_INTERVAL: Long = 15000

        fun setDefaultExceptionHandler(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    invalidateCache(context)
                    if (context is Activity) {
                        val sw = StringWriter()
                        val pw = PrintWriter(sw)
                        pw.use {
                            throwable.printStackTrace(it)
                        }
                        val intent = Intent(context, FatalErrorActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra("exception", sw.toString())
                        context.startActivity(intent)
                    }
                } finally {
                    defaultHandler?.uncaughtException(thread, throwable)
                    exitProcess(1)
                }
            }
        }

        fun invalidateCache(context: Context) {
            try {
                Registry.invalidateCache(context)
            } catch (_: Throwable) {}
        }

        @Composable
        fun MainTime() {
            TimeText(
                modifier = Modifier.fillMaxWidth()
            )
        }

        @Composable
        fun LoadingLabel(language: String, includeImage: Boolean, includeProgress: Boolean, instance: Context) {
            var state by remember { mutableStateOf(false) }

            LaunchedEffect (Unit) {
                while (true) {
                    delay(500)
                    if (Registry.getInstance(instance).state == Registry.State.UPDATING) {
                        state = true
                    }
                }
            }

            LoadingLabelText(state, language, includeImage, includeProgress, instance)
        }

        @Composable
        private fun LoadingLabelText(updating: Boolean, language: String, includeImage: Boolean, includeProgress: Boolean, instance: Context) {
            if (updating) {
                var currentProgress by remember { mutableStateOf(0F) }
                val progressAnimation by animateFloatAsState(
                    targetValue = currentProgress,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    label = "LoadingProgressAnimation"
                )
                if (includeProgress) {
                    LaunchedEffect (Unit) {
                        while (true) {
                            currentProgress = Registry.getInstance(instance).updatePercentage
                            delay(500)
                        }
                    }
                }

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
                    text = if (language == "en") "Updating..." else "更新數據中..."
                )
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(2, instance).dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp),
                    text = if (language == "en") "Might take a minute" else "可能需要約一分鐘"
                )
                if (includeProgress) {
                    Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(25.dp, 0.dp),
                        color = Color(0xFFF9DE09),
                        trackColor = Color(0xFF797979),
                        progress = progressAnimation
                    )
                }
            } else {
                if (includeImage) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Image(
                            modifier = Modifier
                                .size(StringUtils.scaledSize(50, instance).dp)
                                .align(Alignment.Center),
                            painter = painterResource(R.mipmap.icon_full_smaller),
                            contentDescription = instance.resources.getString(R.string.app_name)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
                    text = if (language == "en") "Loading..." else "載入中..."
                )
            }
        }

        fun getMtrLineName(lineName: String): String {
            return getMtrLineName(lineName) { lineName }
        }

        fun getMtrLineName(lineName: String, orElse: String): String {
            return getMtrLineName(lineName) { orElse }
        }

        fun getMtrLineName(lineName: String, orElse: () -> String): String {
            return if (language == "en") {
                when (lineName) {
                    "AEL" -> "Airport Express"
                    "TCL" -> "Tung Chung Line"
                    "TML" -> "Tuen Ma Line"
                    "TKL" -> "Tseung Kwan O Line"
                    "EAL" -> "East Rail Line"
                    "SIL" -> "South Island Line"
                    "TWL" -> "Tsuen Wan Line"
                    "ISL" -> "Island Line"
                    "KTL" -> "Kwun Tong Line"
                    "DRL" -> "Disneyland Resort Line"
                    else -> orElse.invoke()
                }
            } else {
                when (lineName) {
                    "AEL" -> "機場快綫"
                    "TCL" -> "東涌綫"
                    "TML" -> "屯馬綫"
                    "TKL" -> "將軍澳綫"
                    "EAL" -> "東鐵綫"
                    "SIL" -> "南港島綫"
                    "TWL" -> "荃灣綫"
                    "ISL" -> "港島綫"
                    "KTL" -> "觀塘綫"
                    "DRL" -> "迪士尼綫"
                    else -> orElse.invoke()
                }
            }
        }

        var language = "zh"

        val favoriteRouteStops: Map<Int, JSONObject> = ConcurrentHashMap()

        private val currentActivityAccessLock = Object()
        private var currentActivity: CurrentActivityData? = null

        fun getCurrentActivity(): CurrentActivityData? {
            return currentActivity
        }

        fun setSelfAsCurrentActivity(activity: Activity) {
            synchronized (currentActivityAccessLock) {
                currentActivity = CurrentActivityData(activity.javaClass, activity.intent.extras)
            }
        }

        fun removeSelfFromCurrentActivity(activity: Activity) {
            synchronized (currentActivityAccessLock) {
                if (currentActivity != null) {
                    val data = CurrentActivityData(activity.javaClass, activity.intent.extras)
                    if (currentActivity!!.isEqualTo(data)) {
                        currentActivity = null
                    }
                }
            }
        }

    }

}