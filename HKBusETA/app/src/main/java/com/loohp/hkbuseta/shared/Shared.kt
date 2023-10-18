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
import com.loohp.hkbuseta.FatalErrorActivity
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.isEqualTo
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap


data class CurrentActivityData(val cls: Class<Activity>, val extras: Bundle?) {

    fun isEqualTo(other: Any?): Boolean {
        return if (other is CurrentActivityData) {
            this.cls == other.cls && ((this.extras == null && other.extras == null) || (this.extras != null && this.extras.isEqualTo(other.extras)))
        } else {
            false
        }
    }

}

data class LastLookupRoute(val routeNumber: String, val co: String, val gtfsId: String) {

    companion object {

        fun deserialize(json: JSONObject): LastLookupRoute {
            val routeNumber = json.optString("r")
            val co = json.optString("c")
            val gtfsId = if (co == "gmb") json.optString("g") else ""
            return LastLookupRoute(routeNumber, co, gtfsId)
        }

    }

    fun isValid(): Boolean {
        if (routeNumber.isBlank() || co.isBlank()) {
            return false
        }
        if (co == "gmb" && gtfsId.isBlank()) {
            return false
        }
        return true
    }

    fun serialize(): JSONObject {
        val json = JSONObject()
        json.put("r", routeNumber)
        json.put("c", co)
        if (co == "gmb") json.put("g", gtfsId)
        return json
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LastLookupRoute

        if (routeNumber != other.routeNumber) return false
        if (co != other.co) return false
        if (co == "gmb" && gtfsId != other.gtfsId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = routeNumber.hashCode()
        result = 31 * result + co.hashCode()
        if (co == "gmb") result = 31 * result + gtfsId.hashCode()
        return result
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
                        var stacktrace = sw.toString()
                        if (stacktrace.length > 459000) {
                            stacktrace = stacktrace.substring(0, 459000).plus("...")
                        }
                        val intent = Intent(context, FatalErrorActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra("exception", stacktrace)
                        context.startActivity(intent)
                    }
                } finally {
                    defaultHandler?.uncaughtException(thread, throwable)
                    throw throwable
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
                    text = if (language == "en") "Might take a moment" else "更新需時 請稍等"
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

        fun getMtrLineSortingIndex(lineName: String): Int {
            return when (lineName) {
                "AEL" -> 8
                "TCL" -> 7
                "TML" -> 6
                "TKL" -> 3
                "EAL" -> 5
                "SIL" -> 4
                "TWL" -> 1
                "ISL" -> 2
                "KTL" -> 0
                "DRL" -> 9
                else -> 10
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

        fun isLWBRoute(routeNumber: String): Boolean {
            val routeNumberFiltered = if (routeNumber.startsWith("N")) routeNumber.substring(1) else routeNumber
            if (routeNumberFiltered.startsWith("A") || routeNumberFiltered.startsWith("E") || routeNumberFiltered.startsWith("S")) {
                return true
            }
            return when (routeNumber) {
                "N30", "N31", "N42", "N42A", "N64", "R8", "R33", "R42", "X1", "X33", "X34", "X40", "X43", "X47" -> true
                else -> false
            }
        }

        var language = "zh"

        val favoriteRouteStops: Map<Int, JSONObject> = ConcurrentHashMap()

        private const val LAST_LOOKUP_ROUTES_MEM_SIZE = 50
        private val lastLookupRoutes: LinkedList<LastLookupRoute> = LinkedList()

        fun addLookupRoute(routeNumber: String, co: String, gtfsId: String) {
            addLookupRoute(LastLookupRoute(routeNumber, co, gtfsId))
        }

        fun addLookupRoute(data: LastLookupRoute) {
            synchronized(lastLookupRoutes) {
                lastLookupRoutes.removeIf { it == data }
                lastLookupRoutes.add(data)
                while (lastLookupRoutes.size > LAST_LOOKUP_ROUTES_MEM_SIZE) {
                    lastLookupRoutes.removeFirst()
                }
            }
        }

        fun clearLookupRoute() {
            lastLookupRoutes.clear()
        }

        fun getLookupRoutes(): List<LastLookupRoute> {
            synchronized(lastLookupRoutes) {
                return ArrayList(lastLookupRoutes)
            }
        }

        fun getFavoriteAndLookupRouteIndex(routeNumber: String, co: String, gtfsId: String): Int {
            for ((index, route) in favoriteRouteStops) {
                val routeData = route.optJSONObject("route")!!
                if (routeData.optString("route") == routeNumber && route.optString("co") == co && (co != "gmb" || routeData.optString("gtfsId") == gtfsId)) {
                    return index
                }
            }
            synchronized(lastLookupRoutes) {
                for ((index, data) in lastLookupRoutes.withIndex()) {
                    val (lookupRouteNumber, lookupCo, lookupGtfsId) = data
                    if (lookupRouteNumber == routeNumber && lookupCo == co && (co != "gmb" || gtfsId == lookupGtfsId)) {
                        return (lastLookupRoutes.size - index) + 8
                    }
                }
            }
            return Int.MAX_VALUE
        }

        fun hasFavoriteAndLookupRoute(): Boolean {
            return favoriteRouteStops.isNotEmpty() || lastLookupRoutes.isNotEmpty()
        }

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