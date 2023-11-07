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

package com.loohp.hkbuseta

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.objects.RouteListType
import com.loohp.hkbuseta.objects.Stop
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.JsonUtils
import com.loohp.hkbuseta.utils.LocationUtils
import com.loohp.hkbuseta.utils.LocationUtils.LocationResult
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.formatDecimalSeparator
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import java.util.concurrent.ForkJoinPool
import kotlin.math.roundToInt


@Stable
class NearbyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)
        var location: LocationResult? = null
        var exclude: ImmutableSet<String> = persistentSetOf()
        var interchangeSearch = false
        if (intent.extras != null) {
            if (intent.extras!!.containsKey("lat") && intent.extras!!.containsKey("lng")) {
                val lat = intent.extras!!.getDouble("lat")
                val lng = intent.extras!!.getDouble("lng")
                location = LocationResult.fromLatLng(lat, lng)
            }
            if (intent.extras!!.containsKey("exclude")) {
                exclude = intent.extras!!.getStringArrayList("exclude")!!.toImmutableSet()
            }
            interchangeSearch = intent.extras!!.getBoolean("interchangeSearch", false)
        }

        setContent {
            NearbyPage(location, exclude, interchangeSearch, this)
        }
    }

    override fun onStart() {
        super.onStart()
        Shared.setSelfAsCurrentActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Shared.removeSelfFromCurrentActivity(this)
        }
    }

}

@Composable
fun NearbyPage(location: LocationResult?, exclude: ImmutableSet<String>, interchangeSearch: Boolean, instance: NearbyActivity) {
    HKBusETATheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top
        ) {
            Shared.MainTime()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp, 0.dp),
            verticalArrangement = Arrangement.Center
        ) {
            MainElement(location, exclude, interchangeSearch, instance)
        }
    }
}

@Composable
fun WaitingText(usingGps: Boolean, instance: NearbyActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(17F, instance).sp,
        text = if (usingGps) {
            if (Shared.language == "en") "Locating..." else "正在讀取你的位置..."
        } else {
            if (Shared.language == "en") "Searching Nearby..." else "正在搜尋附近路線..."
        }
    )
}

@Composable
fun FailedText(instance: NearbyActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(17F, instance).sp,
        text = if (Shared.language == "en") "Unable to read your location" else "無法讀取你的位置"
    )
    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(14F, instance).sp,
        text = if (Shared.language == "en") "Please check whether your GPS is enabled" else "請檢查你的定位服務是否已開啟"
    )
}

@Composable
fun NoNearbyText(closestStop: Stop, distance: Double, instance: NearbyActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(17F, instance).sp,
        text = if (Shared.language == "en") "There are no nearby bus stops" else "附近沒有巴士站"
    )
    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = StringUtils.scaledSize(12.5F, instance).sp,
        text = if (Shared.language == "en")
            "Nearest Stop: ".plus(closestStop.name.en).plus(" (").plus((distance * 1000).roundToInt().formatDecimalSeparator()).plus("m)")
        else
            "最近的巴士站: ".plus(closestStop.name.zh).plus(" (").plus((distance * 1000).roundToInt().formatDecimalSeparator()).plus("米)")
    )
}

@Composable
fun MainElement(location: LocationResult?, exclude: ImmutableSet<String>, interchangeSearch: Boolean, instance: NearbyActivity) {
    var state by remember { mutableStateOf(false) }
    var result: Registry.NearbyRoutesResult? by remember { mutableStateOf(null) }

    LaunchedEffect (Unit) {
        ForkJoinPool.commonPool().execute {
            val locationResult = location?: LocationUtils.getGPSLocation(instance).get()
            if (locationResult.isSuccess) {
                val loc = locationResult.location
                result = Registry.getInstance(instance).getNearbyRoutes(loc.lat, loc.lng, exclude, interchangeSearch)
            }
            state = true
        }
    }

    EvaluatedElement(state, result, location == null, instance)
}

@Composable
fun EvaluatedElement(state: Boolean, result: Registry.NearbyRoutesResult?, usingGps: Boolean, instance: NearbyActivity) {
    if (state) {
        if (result == null) {
            FailedText(instance)
        } else {
            val list = result.result
            if (list.isEmpty()) {
                NoNearbyText(result.closestStop, result.closestDistance, instance)
            } else {
                val intent = Intent(instance, ListRoutesActivity::class.java)
                intent.putExtra("result", JsonUtils.fromStream(list.stream().map { it.strip(); it.serialize() }).toString())
                intent.putExtra("showEta", true)
                intent.putExtra("recentSort", RecentSortMode.CHOICE.ordinal)
                intent.putExtra("proximitySortOrigin", doubleArrayOf(result.lat, result.lng))
                intent.putExtra("listType", RouteListType.NEARBY.name())
                instance.startActivity(intent)
                instance.finish()
            }
        }
    } else {
        WaitingText(usingGps, instance)
    }
}