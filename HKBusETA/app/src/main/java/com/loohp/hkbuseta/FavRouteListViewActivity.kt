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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.objects.Coordinates
import com.loohp.hkbuseta.objects.RouteListType
import com.loohp.hkbuseta.objects.RouteSearchResultEntry
import com.loohp.hkbuseta.objects.StopInfo
import com.loohp.hkbuseta.objects.getRouteKey
import com.loohp.hkbuseta.objects.resolveStop
import com.loohp.hkbuseta.objects.uniqueKey
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.LocationUtils
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.ifFalse
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.toJsonArray
import kotlinx.coroutines.delay
import java.util.concurrent.ForkJoinPool


@Stable
class FavRouteListViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(this).ifFalse { return }
        Shared.setDefaultExceptionHandler(this)

        val usingGps = intent.extras!!.getBoolean("usingGps")

        setContent {
            FavRouteListViewElement(usingGps, this)
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
fun FavRouteListViewElement(usingGps: Boolean, instance: FavRouteListViewActivity) {
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
            MainElement(usingGps, instance)
        }
    }
}

@Composable
fun WaitingElement(state: MutableState<Boolean>, instance: FavRouteListViewActivity) {
    var enableSkip by remember { mutableStateOf(false) }

    val alpha by remember { derivedStateOf { if (enableSkip) 1F else 0F } }
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = TweenSpec(durationMillis = 400, easing = LinearEasing),
        label = ""
    )

    LaunchedEffect (Unit) {
        delay(2000)
        enableSkip = true
    }

    Box (
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = 17F.scaledSize(instance).sp,
            text = if (Shared.language == "en") "Locating..." else "正在讀取你的位置..."
        )
        Button(
            onClick = {
                state.value = true
            },
            modifier = Modifier
                .padding(20.dp, 0.dp)
                .offset(0.dp, 27F.scaledSize(instance).sp.dp + 10.dp)
                .fillMaxWidth()
                .height(35.scaledSize(instance).dp)
                .alpha(animatedAlpha),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = Color(0xFFFFFFFF)
            ),
            enabled = enableSkip,
            content = {
                Text(
                    modifier = Modifier.fillMaxWidth(0.9F),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                    text = if (Shared.language == "en") "Skip sort by distance" else "略過按距離排序"
                )
            }
        )
    }
}

@Composable
fun MainElement(usingGps: Boolean, instance: FavRouteListViewActivity) {
    val hasFavRequireLocation = remember { Shared.favoriteRouteStops.values.any { it.favouriteStopMode.isRequiresLocation } }
    val needLocation = remember { usingGps || hasFavRequireLocation }

    val state = remember { mutableStateOf(!needLocation) }
    var location: Coordinates? by remember { mutableStateOf(null) }

    LaunchedEffect (Unit) {
        ForkJoinPool.commonPool().execute {
            if (needLocation) {
                val locationResult = LocationUtils.getGPSLocation(instance).get()
                if (locationResult.isSuccess) {
                    location = locationResult.location
                }
                state.value = true
            }
        }
    }

    EvaluatedElement(state, if (hasFavRequireLocation) location else null, if (usingGps) location else null, instance)
}

@Composable
fun EvaluatedElement(state: MutableState<Boolean>, origin: Coordinates?, location: Coordinates?, instance: FavRouteListViewActivity) {
    if (state.value) {
        val intent = Intent(instance, ListRoutesActivity::class.java)
        intent.putExtra("result", Shared.favoriteRouteStops.entries.asSequence()
            .sortedBy { it.key }
            .map { (_, fav) ->
                val (_, stopId, stop, route) = fav.resolveStop(instance) { origin }
                val routeEntry = RouteSearchResultEntry(route.getRouteKey(instance)!!, route, fav.co, StopInfo(stopId, stop, 0.0, fav.co), null, false)
                routeEntry.strip()
                routeEntry
            }
            .distinctBy { routeEntry -> routeEntry.uniqueKey }
            .map { routeEntry -> routeEntry.serialize() }
            .toJsonArray().toString()
        )
        intent.putExtra("showEta", true)
        if (location != null) {
            intent.putExtra("proximitySortOrigin", doubleArrayOf(location.lat, location.lng))
        }
        intent.putExtra("listType", RouteListType.FAVOURITE.name)
        intent.putExtra("allowAmbient", true)
        instance.startActivity(intent)
        instance.finish()
    } else {
        WaitingElement(state, instance)
    }
}