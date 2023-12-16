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
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.AtomicReference
import com.loohp.hkbuseta.FatalErrorActivity
import com.loohp.hkbuseta.MainActivity
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.AppContext
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.objects.FavouriteRouteStop
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.RouteListType
import com.loohp.hkbuseta.objects.RouteSortMode
import com.loohp.hkbuseta.objects.gmbRegion
import com.loohp.hkbuseta.objects.operator
import com.loohp.hkbuseta.utils.HongKongTimeSource
import com.loohp.hkbuseta.utils.JSONSerializable
import com.loohp.hkbuseta.utils.isEqualTo
import com.loohp.hkbuseta.utils.optString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


enum class TileUseState {

    PRIMARY, SECONDARY, NONE

}

data class CurrentActivityData(val cls: Class<Activity>, val extras: Bundle?, val shouldRelaunch: Boolean = extras?.getBoolean("shouldRelaunch", true)?: true) {

    fun isEqualTo(other: Any?): Boolean {
        return if (other is CurrentActivityData) {
            this.cls == other.cls && this.shouldRelaunch == other.shouldRelaunch && ((this.extras == null && other.extras == null) || (this.extras != null && this.extras.isEqualTo(other.extras)))
        } else {
            false
        }
    }

}

@Immutable
data class LastLookupRoute(val routeNumber: String, val co: Operator, val meta: String) : JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): LastLookupRoute {
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

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("r", routeNumber)
            put("c", co.name)
            if (co == Operator.GMB || co == Operator.NLB) put("m", meta)
        }
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

        const val ETA_UPDATE_INTERVAL: Int = 15000

        val RESOURCE_RATIO: Map<Int, Float> = mapOf(
            R.mipmap.lrv to 128F / 95F,
            R.mipmap.lrv_empty to 128F / 95F
        )

        fun setDefaultExceptionHandler(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    invalidateCache(context.appContext)
                    if (context is Activity) {
                        var stacktrace = throwable.stackTraceToString()
                        if (stacktrace.length > 459000) {
                            stacktrace = stacktrace.substring(0, 459000).plus("...")
                        }
                        val intent = Intent(context, FatalErrorActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        intent.putExtra("exception", stacktrace)
                        context.startActivity(intent)
                    }
                } finally {
                    defaultHandler?.uncaughtException(thread, throwable)
                    throw throwable
                }
            }
        }

        fun invalidateCache(context: AppContext) {
            try {
                Registry.invalidateCache(context)
            } catch (_: Throwable) {}
        }

        @Composable
        fun MainTime() {
            TimeText(
                modifier = Modifier.fillMaxWidth(),
                timeSource = HongKongTimeSource(TimeTextDefaults.timeFormat())
            )
        }

        fun getMtrLineSortingIndex(lineName: String): Int {
            return when (lineName) {
                "AEL" -> 0
                "TCL" -> 1
                "TML" -> 4
                "TKL" -> 9
                "EAL" -> 3
                "SIL" -> 5
                "TWL" -> 8
                "ISL" -> 6
                "KTL" -> 7
                "DRL" -> 2
                else -> 10
            }
        }

        fun getMtrLineName(lineName: String): String {
            return getMtrLineName(lineName, lineName)
        }

        fun getMtrLineName(lineName: String, orElse: String): String {
            return if (language == "en") when (lineName) {
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
                else -> orElse
            } else when (lineName) {
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
                else -> orElse
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

        private val suggestedMaxFavouriteRouteStop = MutableStateFlow(0)
        private val currentMaxFavouriteRouteStop = MutableStateFlow(0)
        val favoriteRouteStops: Map<Int, FavouriteRouteStop> = ConcurrentMutableMap()

        private val etaTileConfigurations: Map<Int, List<Int>> = ConcurrentMutableMap()

        fun updateEtaTileConfigurations(mutation: (MutableMap<Int, List<Int>>) -> Unit) {
            synchronized(etaTileConfigurations) {
                mutation.invoke(etaTileConfigurations as MutableMap<Int, List<Int>>)
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

        fun updateFavoriteRouteStops(mutation: (MutableMap<Int, FavouriteRouteStop>) -> Unit) {
            synchronized(favoriteRouteStops) {
                mutation.invoke(favoriteRouteStops as MutableMap<Int, FavouriteRouteStop>)
                val max = favoriteRouteStops.maxOfOrNull { it.key }?: 0
                currentMaxFavouriteRouteStop.value = max.coerceAtLeast(8)
                suggestedMaxFavouriteRouteStop.value = (max + 1).coerceIn(8, 30)
            }
        }

        fun getSuggestedMaxFavouriteRouteStopState(): StateFlow<Int> {
            return suggestedMaxFavouriteRouteStop
        }

        fun getCurrentMaxFavouriteRouteStopState(): StateFlow<Int> {
            return currentMaxFavouriteRouteStop
        }

        private const val LAST_LOOKUP_ROUTES_MEM_SIZE = 50
        private val lastLookupRoutes: ArrayDeque<LastLookupRoute> = ArrayDeque(LAST_LOOKUP_ROUTES_MEM_SIZE)

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

        val routeSortModePreference: Map<RouteListType, RouteSortMode> = ConcurrentMutableMap()

        private var currentActivity: AtomicReference<CurrentActivityData?> = AtomicReference(null)

        fun getCurrentActivity(): CurrentActivityData? {
            return currentActivity.get()
        }

        fun setSelfAsCurrentActivity(activity: Activity) {
            currentActivity.set(CurrentActivityData(activity.javaClass, activity.intent.extras))
        }

        fun removeSelfFromCurrentActivity(activity: Activity) {
            val data = CurrentActivityData(activity.javaClass, activity.intent.extras)
            currentActivity.updateAndGet { if (it != null && it.isEqualTo(data)) null else it }
        }

        fun ensureRegistryDataAvailable(activity: Activity): Boolean {
            return if (!Registry.hasInstanceCreated() || Registry.getInstanceNoUpdateCheck(activity.appContext).state.value.isProcessing) {
                val intent = Intent(activity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                activity.startActivity(intent)
                activity.finishAffinity()
                false
            } else {
                true
            }
        }

    }

}