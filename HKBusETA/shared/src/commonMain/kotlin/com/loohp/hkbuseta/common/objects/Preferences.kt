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
package com.loohp.hkbuseta.common.objects

import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.synchronize
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.isSystemLanguageChinese
import com.loohp.hkbuseta.common.utils.mapToMutableList
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.optBoolean
import com.loohp.hkbuseta.common.utils.optInt
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optLong
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.common.utils.toJsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class Preferences(
    var referenceChecksum: String,
    var lastSaved: Long,
    var language: String,
    var etaDisplayMode: ETADisplayMode,
    var lrtDirectionMode: Boolean,
    var theme: Theme,
    var color: Long?,
    var viewFavTab: Int,
    var disableMarquee: Boolean,
    var disableBoldDest: Boolean,
    var historyEnabled: Boolean,
    var showRouteMap: Boolean,
    var downloadSplash: Boolean,
    var alternateStopName: Boolean,
    var lastNearbyLocation: RadiusCenterPosition?,
    var disableNavBarQuickActions: Boolean,
    val favouriteStops: ConcurrentMutableList<FavouriteStop>,
    val favouriteRouteStops: ConcurrentMutableList<FavouriteRouteGroup>,
    val lastLookupRoutes: ConcurrentMutableList<LastLookupRoute>,
    val etaTileConfigurations: ConcurrentMutableMap<Int, List<Int>>,
    val routeSortModePreference: ConcurrentMutableMap<RouteListType, RouteSortPreference>
) : JSONSerializable {

    companion object {

        fun deserialize(json: JsonObject): Preferences {
            val referenceChecksum = json.optString("referenceChecksum")
            val lastSaved = json.optLong("lastSaved")
            val language = json.optString("language")
            val etaDisplayMode = if (json.containsKey("clockTimeMode")) json.optBoolean("clockTimeMode", false).etaDisplayMode else json.optString("etaDisplayMode").etaDisplayMode
            val lrtDirectionMode = json.optBoolean("lrtDirectionMode", false)
            val theme = Theme.valueOf(json.optString("theme", Theme.SYSTEM.name))
            val color = if (json.contains("color")) json.optLong("color") else null
            val viewFavTab = json.optInt("viewFavTab", 0)
            val disableMarquee = json.optBoolean("disableMarquee", false)
            val disableBoldDest = json.optBoolean("disableBoldDest", false)
            val historyEnabled = json.optBoolean("historyEnabled", true)
            val showRouteMap = json.optBoolean("showRouteMap", true)
            val downloadSplash = json.optBoolean("downloadSplash", true)
            val alternateStopName = json.optBoolean("alternateStopName", false)
            val lastNearbyLocation = if (json.containsKey("lastNearbyLocation")) RadiusCenterPosition.deserialize(json.optJsonObject("lastNearbyLocation")!!) else null
            val disableNavBarQuickActions = json.optBoolean("disableNavBarQuickActions", false)
            val favouriteStops = ConcurrentMutableList<FavouriteStop>().apply {
                if (json.optJsonArray("favouriteStops")?.getOrNull(0) is JsonPrimitive) {
                    addAll(json.optJsonArray("favouriteStops")?.mapToMutableList { FavouriteStop.fromLegacy(it.jsonPrimitive.content) }?: mutableListOf())
                } else {
                    json.optJsonArray("favouriteStops")!!.forEach { add(FavouriteStop.deserialize(it.jsonObject)) }
                }
            }
            val favouriteRouteStops = if (json["favouriteRouteStops"] is JsonArray) {
                ConcurrentMutableList<FavouriteRouteGroup>().apply { addAll(json.optJsonArray("favouriteRouteStops")!!.mapToMutableList { FavouriteRouteGroup.deserialize(it.jsonObject) }) }
            } else {
                val legacy = json.optJsonObject("favouriteRouteStops")!!.mapToMutableMap({ it.toInt() }) { FavouriteRouteStop.deserialize(it.jsonObject) }
                ConcurrentMutableList<FavouriteRouteGroup>().apply { add(FavouriteRouteGroup.fromLegacy(legacy)) }
            }
            val lastLookupRoutes = ConcurrentMutableList<LastLookupRoute>().apply { addAll(json.optJsonArray("lastLookupRoutes")!!.mapToMutableList { if (it is JsonObject) (if (it.containsKey("routeKey")) LastLookupRoute.deserialize(it) else null) else LastLookupRoute.fromLegacy(it.jsonPrimitive.content) }.filterNotNull()) }
            val etaTileConfigurations = ConcurrentMutableMap<Int, List<Int>>().apply { if (json.contains("etaTileConfigurations")) putAll(json.optJsonObject("etaTileConfigurations")!!.mapToMutableMap<Int, List<Int>>({ it.toInt() }) { it.jsonArray.mapToMutableList { e -> e.jsonPrimitive.int } }) }
            val routeSortModePreference = ConcurrentMutableMap<RouteListType, RouteSortPreference>().apply { if (json.contains("routeSortModePreference")) putAll(json.optJsonObject("routeSortModePreference")!!.mapToMutableMap({ RouteListType.valueOf(it) }, { if (it is JsonPrimitive) RouteSortPreference.fromLegacy(it) else RouteSortPreference.deserialize(it.jsonObject) })) }
            return Preferences(referenceChecksum, lastSaved, language, etaDisplayMode, lrtDirectionMode, theme, color, viewFavTab, disableMarquee, disableBoldDest, historyEnabled, showRouteMap, downloadSplash, alternateStopName, lastNearbyLocation, disableNavBarQuickActions, favouriteStops, favouriteRouteStops, lastLookupRoutes, etaTileConfigurations, routeSortModePreference)
        }

        fun createDefault(): Preferences {
            return Preferences(
                referenceChecksum = "",
                lastSaved = currentTimeMillis(),
                language = if (isSystemLanguageChinese()) "zh" else "en",
                etaDisplayMode = ETADisplayMode.COUNTDOWN,
                lrtDirectionMode = false,
                theme = Theme.SYSTEM,
                color = null,
                viewFavTab = 0,
                disableMarquee = false,
                disableBoldDest = false,
                historyEnabled = true,
                showRouteMap = true,
                downloadSplash = true,
                alternateStopName = false,
                lastNearbyLocation = null,
                disableNavBarQuickActions = false,
                favouriteStops = ConcurrentMutableList(),
                favouriteRouteStops = ConcurrentMutableList<FavouriteRouteGroup>().apply { add(FavouriteRouteGroup.DEFAULT_GROUP) },
                lastLookupRoutes = ConcurrentMutableList(),
                etaTileConfigurations = ConcurrentMutableMap(),
                routeSortModePreference = ConcurrentMutableMap()
            )
        }

    }

    fun syncWith(preferences: Preferences): Boolean {
        return if (lastSaved <= preferences.lastSaved) {
            this.lastSaved = preferences.lastSaved
            this.language = preferences.language
            this.etaDisplayMode = preferences.etaDisplayMode
            this.lrtDirectionMode = preferences.lrtDirectionMode
            this.theme = preferences.theme
            this.color = preferences.color
            this.viewFavTab = preferences.viewFavTab
            this.disableMarquee = preferences.disableMarquee
            this.disableBoldDest = preferences.disableBoldDest
            this.historyEnabled = preferences.historyEnabled
            this.showRouteMap = preferences.showRouteMap
            this.downloadSplash = preferences.downloadSplash
            this.alternateStopName = preferences.alternateStopName
            this.lastNearbyLocation = preferences.lastNearbyLocation
            this.disableNavBarQuickActions = preferences.disableNavBarQuickActions
            this.favouriteStops.apply { clear(); addAll(preferences.favouriteStops) }
            this.favouriteRouteStops.apply { clear(); addAll(preferences.favouriteRouteStops) }
            this.lastLookupRoutes.apply { clear(); addAll(preferences.lastLookupRoutes) }
            this.routeSortModePreference.apply { clear(); putAll(preferences.routeSortModePreference) }
            true
        } else {
            false
        }
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("referenceChecksum", referenceChecksum)
            put("lastSaved", lastSaved)
            put("language", language)
            put("etaDisplayMode", etaDisplayMode.name)
            put("lrtDirectionMode", lrtDirectionMode)
            put("theme", theme.name)
            color?.apply { put("color", this) }
            put("viewFavTab", viewFavTab)
            put("disableMarquee", disableMarquee)
            put("disableBoldDest", disableBoldDest)
            put("historyEnabled", historyEnabled)
            put("showRouteMap", showRouteMap)
            put("downloadSplash", downloadSplash)
            put("alternateStopName", alternateStopName)
            lastNearbyLocation?.apply { put("lastNearbyLocation", serialize()) }
            put("disableNavBarQuickActions", disableNavBarQuickActions)
            favouriteStops.synchronize { put("favouriteStops", favouriteStops.toJsonArray()) }
            favouriteRouteStops.synchronize { put("favouriteRouteStops", favouriteRouteStops.toJsonArray()) }
            lastLookupRoutes.synchronize { put("lastLookupRoutes", lastLookupRoutes.toJsonArray()) }
            etaTileConfigurations.synchronize { put("etaTileConfigurations", etaTileConfigurations.toJsonObject { it.toJsonArray() }) }
            routeSortModePreference.synchronize { put("routeSortModePreference", routeSortModePreference.toJsonObject { it.serialize() }) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Preferences) return false

        if (referenceChecksum != other.referenceChecksum) return false
        if (lastSaved != other.lastSaved) return false
        if (language != other.language) return false
        if (etaDisplayMode != other.etaDisplayMode) return false
        if (lrtDirectionMode != other.lrtDirectionMode) return false
        if (theme != other.theme) return false
        if (color != other.color) return false
        if (viewFavTab != other.viewFavTab) return false
        if (disableMarquee != other.disableMarquee) return false
        if (historyEnabled != other.historyEnabled) return false
        if (disableBoldDest != other.disableBoldDest) return false
        if (showRouteMap != other.showRouteMap) return false
        if (downloadSplash != other.downloadSplash) return false
        if (alternateStopName != other.alternateStopName) return false
        if (lastNearbyLocation != other.lastNearbyLocation) return false
        if (disableNavBarQuickActions != other.disableNavBarQuickActions) return false
        if (favouriteStops != other.favouriteStops) return false
        if (favouriteRouteStops != other.favouriteRouteStops) return false
        if (lastLookupRoutes != other.lastLookupRoutes) return false
        if (etaTileConfigurations != other.etaTileConfigurations) return false
        return routeSortModePreference == other.routeSortModePreference
    }

    override fun hashCode(): Int {
        var result = referenceChecksum.hashCode()
        result = 31 * result + lastSaved.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + etaDisplayMode.hashCode()
        result = 31 * result + lrtDirectionMode.hashCode()
        result = 31 * result + theme.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + viewFavTab
        result = 31 * result + disableMarquee.hashCode()
        result = 31 * result + disableBoldDest.hashCode()
        result = 31 * result + historyEnabled.hashCode()
        result = 31 * result + showRouteMap.hashCode()
        result = 31 * result + downloadSplash.hashCode()
        result = 31 * result + alternateStopName.hashCode()
        result = 31 * result + lastNearbyLocation.hashCode()
        result = 31 * result + disableNavBarQuickActions.hashCode()
        result = 31 * result + favouriteStops.hashCode()
        result = 31 * result + favouriteRouteStops.hashCode()
        result = 31 * result + lastLookupRoutes.hashCode()
        result = 31 * result + etaTileConfigurations.hashCode()
        result = 31 * result + routeSortModePreference.hashCode()
        return result
    }

}
