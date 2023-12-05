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
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.LocationUtils
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.ifFalse
import com.loohp.hkbuseta.utils.scaledSize


@Stable
class TitleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(this).ifFalse { return }
        Shared.setDefaultExceptionHandler(this)

        setContent {
            HKBusETAApp(this)
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
fun HKBusETAApp(instance: TitleActivity) {
    HKBusETATheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Shared.MainTime()
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
                LanguageButton(instance)
                Spacer(modifier = Modifier.size(7.scaledSize(instance).dp))
                FavButton(instance)
            }
            Spacer(modifier = Modifier.size(7.scaledSize(instance).dp))
            CreditVersionText(instance)
        }
    }
}

@Composable
fun SearchButton(instance: TitleActivity) {
    Button(
        onClick = {
            Firebase.analytics.logEvent("title_action", Bundle().apply {
                putString("value", "search")
            })
            instance.startActivity(Intent(instance, SearchActivity::class.java))
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
                fontSize = 17F.scaledSize(instance).sp,
                text = if (Shared.language == "en") "Input Route" else "輸入巴士路線"
            )
        }
    )
}

@Composable
fun NearbyButton(instance: TitleActivity) {
    Button(
        onClick = {
            Firebase.analytics.logEvent("title_action", Bundle().apply {
                putString("value", "nearby")
            })
            LocationUtils.checkLocationPermission(instance) {
                instance.runOnUiThread {
                    if (it) {
                        instance.startActivity(Intent(instance, NearbyActivity::class.java))
                    } else {
                        Toast.makeText(instance, if (Shared.language == "en") "Location Access Permission Denied" else "位置存取權限被拒絕", Toast.LENGTH_SHORT).show()
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
                fontSize = 17F.scaledSize(instance).sp,
                text = if (Shared.language == "en") "Search Nearby" else "附近巴士路線"
            )
        }
    )
}

@Composable
fun LanguageButton(instance: TitleActivity) {
    Button(
        onClick = {
            val newLanguage = if (Shared.language == "en") "zh" else "en"
            Registry.getInstance(instance).setLanguage(newLanguage, instance)
            Firebase.analytics.logEvent("title_action", Bundle().apply {
                putString("value", "language_$newLanguage")
            })
            instance.startActivity(Intent(instance, MainActivity::class.java))
            instance.finish()
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
                fontSize = 17F.scaledSize(instance).sp,
                text = if (Shared.language == "en") "中文" else "English"
            )
        }
    )
}

@Composable
fun FavButton(instance: TitleActivity) {
    val haptic = LocalHapticFeedback.current
    AdvanceButton(
        onClick = {
            Firebase.analytics.logEvent("title_action", Bundle().apply {
                putString("value", "favourite")
            })
            instance.startActivity(Intent(instance, FavActivity::class.java))
        },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (Shared.favoriteRouteStops.isNotEmpty()) {
                Firebase.analytics.logEvent("title_action", Bundle().apply {
                    putString("value", "favourite_list_view")
                })
                LocationUtils.checkLocationPermission(instance) {
                    val intent = Intent(instance, FavRouteListViewActivity::class.java)
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
fun CreditVersionText(instance: TitleActivity) {
    val packageInfo = instance.packageManager.getPackageInfo(instance.packageName, 0)
    val haptic = LocalHapticFeedback.current
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=com.loohp.hkbuseta"))
                    RemoteActivityUtils.intentToPhone(
                        instance = instance,
                        intent = intent,
                        noPhone = {
                            instance.startActivity(intent)
                        },
                        failed = {
                            instance.startActivity(intent)
                        }
                    )
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val intent = Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("https://loohpjames.com"))
                    RemoteActivityUtils.intentToPhone(
                        instance = instance,
                        intent = intent,
                        noPhone = {
                            instance.startActivity(intent)
                        },
                        failed = {
                            instance.startActivity(intent)
                        }
                    )
                }
            ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(1.5F.scaledSize(instance), TextUnitType.Em),
        text = instance.resources.getString(R.string.app_name).plus(" v").plus(packageInfo.versionName).plus(" (").plus(packageInfo.longVersionCode).plus(")\n@LoohpJames")
    )
}