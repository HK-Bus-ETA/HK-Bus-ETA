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

package com.loohp.hkbuseta.shared

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.TimeText
import com.loohp.hkbuseta.FatalErrorActivity
import com.loohp.hkbuseta.objects.FavouriteRouteStop
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.RouteListType
import com.loohp.hkbuseta.objects.RouteSortMode
import com.loohp.hkbuseta.objects.gmbRegion
import com.loohp.hkbuseta.objects.operator
import com.loohp.hkbuseta.utils.isEqualTo
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer


enum class TileUseState {

    PRIMARY, SECONDARY, NONE

}

data class CurrentActivityData(val cls: Class<Activity>, val extras: Bundle?) {

    fun isEqualTo(other: Any?): Boolean {
        return if (other is CurrentActivityData) {
            this.cls == other.cls && ((this.extras == null && other.extras == null) || (this.extras != null && this.extras.isEqualTo(other.extras)))
        } else {
            false
        }
    }

}

@Immutable
data class LastLookupRoute(val routeNumber: String, val co: Operator, val meta: String) {

    companion object {

        fun deserialize(json: JSONObject): LastLookupRoute {
            val routeNumber = json.optString("r")
            val co = json.optString("c").operator
            val meta = if (co == Operator.GMB || co == Operator.NLB) json.optString("m") else ""
            return LastLookupRoute(routeNumber, co, meta)
        }

    }

    fun isValid(): Boolean {
        if (routeNumber.isBlank() || !co.isBuiltIn) {
            return false
        }
        if ((co == Operator.GMB || co == Operator.NLB) && meta.isBlank()) {
            return false
        }
        if (co == Operator.GMB && meta.gmbRegion == null) {
            return false
        }
        return true
    }

    fun serialize(): JSONObject {
        val json = JSONObject()
        json.put("r", routeNumber)
        json.put("c", co)
        if (co == Operator.GMB || co == Operator.NLB) json.put("m", meta)
        return json
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LastLookupRoute

        if (routeNumber != other.routeNumber) return false
        if (co != other.co) return false
        if ((co == Operator.GMB || co == Operator.NLB) && meta != other.meta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = routeNumber.hashCode()
        result = 31 * result + co.hashCode()
        if (co == Operator.GMB || co == Operator.NLB) result = 31 * result + meta.hashCode()
        return result
    }

}

enum class KMBSubsidiary {

    KMB, LWB, SUNB

}

class Shared {

    @Stable
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

        fun getKMBSubsidiary(routeNumber: String): KMBSubsidiary {
            val routeNumberFiltered = if (routeNumber.startsWith("N")) routeNumber.substring(1) else routeNumber
            if (routeNumberFiltered.startsWith("A") || routeNumberFiltered.startsWith("E") || routeNumberFiltered.startsWith("S")) {
                return KMBSubsidiary.LWB
            }
            return when (routeNumber) {
                "N30", "N31", "N42", "N42A", "N64", "R8", "R33", "R42", "X1", "X33", "X34", "X40", "X43", "X47" -> KMBSubsidiary.LWB
                "331", "331S", "917", "918", "945" -> KMBSubsidiary.SUNB
                else -> KMBSubsidiary.KMB
            }

        }

        var language = "zh"

        private val suggestedMaxFavouriteRouteStop = mutableIntStateOf(0)
        private val currentMaxFavouriteRouteStop = mutableIntStateOf(0)
        val favoriteRouteStops: Map<Int, FavouriteRouteStop> = ConcurrentHashMap()

        private val etaTileConfigurations: Map<Int, List<Int>> = ConcurrentHashMap()

        fun updateEtaTileConfigurations(mutation: Consumer<Map<Int, List<Int>>>) {
            synchronized(etaTileConfigurations) {
                mutation.accept(etaTileConfigurations)
            }
        }

        fun getEtaTileConfiguration(tileId: Int): List<Int> {
            return if (tileId in (1 or Int.MIN_VALUE)..(8 or Int.MIN_VALUE)) listOf(tileId and Int.MAX_VALUE) else etaTileConfigurations.getOrElse(tileId) { emptyList() }
        }

        fun getRawEtaTileConfigurations(): Map<Int, List<Int>> {
            return etaTileConfigurations
        }

        fun getTileUseState(index: Int): TileUseState {
            return when (etaTileConfigurations.values.minOfOrNull { it.indexOf(index).let { i -> if (i >= 0) i else Int.MAX_VALUE } }?: Int.MAX_VALUE) {
                0 -> TileUseState.PRIMARY
                Int.MAX_VALUE -> TileUseState.NONE
                else -> TileUseState.SECONDARY
            }
        }

        fun updateFavoriteRouteStops(mutation: Consumer<Map<Int, FavouriteRouteStop>>) {
            synchronized(favoriteRouteStops) {
                mutation.accept(favoriteRouteStops)
                val max = favoriteRouteStops.maxOfOrNull { it.key }?: 0
                currentMaxFavouriteRouteStop.intValue = max.coerceAtLeast(8)
                suggestedMaxFavouriteRouteStop.intValue = (max + 1).coerceIn(8, 30)
            }
        }

        fun getSuggestedMaxFavouriteRouteStopState(): State<Int> {
            return suggestedMaxFavouriteRouteStop
        }

        fun getCurrentMaxFavouriteRouteStopState(): State<Int> {
            return currentMaxFavouriteRouteStop
        }

        private const val LAST_LOOKUP_ROUTES_MEM_SIZE = 50
        private val lastLookupRoutes: LinkedList<LastLookupRoute> = LinkedList()

        fun addLookupRoute(routeNumber: String, co: Operator, meta: String) {
            addLookupRoute(LastLookupRoute(routeNumber, co, meta))
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

        fun getFavoriteAndLookupRouteIndex(routeNumber: String, co: Operator, meta: String): Int {
            for ((index, route) in favoriteRouteStops) {
                val routeData = route.route
                if (routeData.routeNumber == routeNumber && route.co == co && (co != Operator.GMB || routeData.gmbRegion == meta.gmbRegion) && (co != Operator.NLB || routeData.nlbId == meta)) {
                    return index
                }
            }
            synchronized(lastLookupRoutes) {
                for ((index, data) in lastLookupRoutes.withIndex()) {
                    val (lookupRouteNumber, lookupCo, lookupMeta) = data
                    if (lookupRouteNumber == routeNumber && lookupCo == co && ((co != Operator.GMB && co != Operator.NLB) || meta == lookupMeta)) {
                        return (lastLookupRoutes.size - index) + 8
                    }
                }
            }
            return Int.MAX_VALUE
        }

        fun hasFavoriteAndLookupRoute(): Boolean {
            return favoriteRouteStops.isNotEmpty() || lastLookupRoutes.isNotEmpty()
        }

        val routeSortModePreference: Map<RouteListType, RouteSortMode> = ConcurrentHashMap()

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