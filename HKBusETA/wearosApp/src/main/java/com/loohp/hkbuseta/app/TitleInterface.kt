/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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

package com.loohp.hkbuseta.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppBundle
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.takeOrNull
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.applyIfNotNull
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.checkLocationPermission
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.sp


@Composable
fun HKBusETAApp(instance: AppActiveContext) {
    HKBusETATheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WearOSShared.MainTime()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp, 0.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(25.scaledSize(instance).dp))
            SearchButton(instance)
            Spacer(modifier = Modifier.size(7.scaledSize(instance).dp))
            NearbyButton(instance)
            Spacer(modifier = Modifier.size(7.scaledSize(instance).dp))
            Row (
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsButton(instance)
                Spacer(modifier = Modifier.size(7.scaledSize(instance).dp))
                FavButton(instance)
            }
            Spacer(modifier = Modifier.size(7.scaledSize(instance).dp))
            BottomText(instance)
        }
    }
}

@Composable
fun SearchButton(instance: AppActiveContext) {
    Button(
        onClick = {
            instance.logFirebaseEvent("title_action", AppBundle().apply {
                putString("value", "search")
            })
            instance.startActivity(AppIntent(instance, AppScreen.SEARCH))
        },
        modifier = Modifier
            .width(220.scaledSize(instance).dp)
            .height(45.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(R.mipmap.bus_background),
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(Color(0xA3000000), BlendMode.Multiply),
                contentDescription = null
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 19F.scaledSize(instance).sp.clamp(max = 21F.scaledSize(instance).dp),
                text = if (Shared.language == "en") "Search Routes" else "搜尋路線"
            )
        }
    )
}

@Composable
fun NearbyButton(instance: AppActiveContext) {
    Button(
        onClick = {
            instance.logFirebaseEvent("title_action", AppBundle().apply {
                putString("value", "nearby")
            })
            checkLocationPermission(instance) {
                instance.runOnUiThread {
                    if (it) {
                        instance.startActivity(AppIntent(instance, AppScreen.NEARBY))
                    } else {
                        instance.showToastText(if (Shared.language == "en") "Location Access Permission Denied" else "位置存取權限被拒絕", ToastDuration.LONG)
                    }
                }
            }
        },
        modifier = Modifier
            .width(220.scaledSize(instance).dp)
            .height(45.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(R.mipmap.nearby_background),
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(Color(0xA3000000), BlendMode.Multiply),
                contentDescription = null
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 19F.scaledSize(instance).sp.clamp(max = 21F.scaledSize(instance).dp),
                text = if (Shared.language == "en") "Nearby Routes" else "附近路線"
            )
        }
    )
}

@Composable
fun SettingsButton(instance: AppActiveContext) {
    Button(
        onClick = {
            instance.logFirebaseEvent("title_action", AppBundle().apply {
                putString("value", "settings")
            })
            instance.startActivity(AppIntent(instance, AppScreen.SETTINGS))
        },
        modifier = Modifier
            .width(90.scaledSize(instance).dp)
            .height(35.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 17F.scaledSize(instance).sp.clamp(max = 19F.scaledSize(instance).dp),
                text = if (Shared.language == "en") "Settings" else "設定"
            )
        }
    )
}

@Composable
fun FavButton(instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    AdvanceButton(
        onClick = {
            instance.logFirebaseEvent("title_action", AppBundle().apply {
                putString("value", "favourite")
            })
            instance.startActivity(AppIntent(instance, AppScreen.FAV))
        },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (Shared.favoriteRouteStops.value.any { it.favouriteRouteStops.isNotEmpty() }) {
                instance.logFirebaseEvent("title_action", AppBundle().apply {
                    putString("value", "favourite_list_view")
                })
                checkLocationPermission(instance) {
                    val intent = AppIntent(instance, AppScreen.FAV_ROUTE_LIST_VIEW)
                    intent.putExtra("usingGps", it)
                    instance.startActivity(intent)
                }
            }
        },
        modifier = Modifier
            .width(35.scaledSize(instance).dp)
            .height(35.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color(0xFFFFFF00)
        ),
        content = {
            Icon(
                modifier = Modifier.size(21.scaledSize(instance).sp.dp),
                imageVector = Icons.Filled.Star,
                tint = Color(0xFFFFFF00),
                contentDescription = if (Shared.language == "en") "Favourite Routes" else "最喜愛路線"
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomText(instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    val appAlert by WearOSShared.rememberAppAlert(instance)

    appAlert?.takeOrNull()?.let { alert ->
        AutoResizeText(
            modifier = Modifier
                .padding(30.dp, 0.dp)
                .fillMaxWidth()
                .height(25.scaledSize(instance).dp)
                .applyIfNotNull(alert.url) {
                    combinedClickable(
                        onClick = instance.handleWebpages(it, false, haptic.common),
                        onLongClick = instance.handleWebpages(it, true, haptic.common)
                    )
                },
            textAlign = TextAlign.Center,
            color = Color(0xFFFF6161),
            overflow = TextOverflow.Ellipsis,
            fontSizeRange = FontSizeRange(
                max = 16F.scaledSize(instance).sp,
                min = 8F.dp.sp
            ),
            maxLines = 2,
            text = alert.content?.get(Shared.language)?: ""
        )
    }?: run {
        AutoResizeText(
            modifier = Modifier
                .fillMaxWidth()
                .height(25.scaledSize(instance).dp)
                .combinedClickable(
                    onClick = instance.handleWebpages("https://play.google.com/store/apps/details?id=com.loohp.hkbuseta", false, haptic.common),
                    onLongClick = instance.handleWebpages("https://loohpjames.com", true, haptic.common)
                ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSizeRange = FontSizeRange(
                max = 9F.scaledSize(instance).sp
            ),
            text = "${if (Shared.language == "en") "HK Bus ETA" else "香港巴士到站預報"} v${instance.versionName} (${instance.versionCode})\n@LoohpJames"
        )
    }
}