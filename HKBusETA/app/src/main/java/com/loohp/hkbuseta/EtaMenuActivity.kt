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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.sp
import kotlinx.coroutines.delay
import org.json.JSONObject


class EtaMenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)

        val stopId = intent.extras!!.getString("stopId")
        val co = intent.extras!!.getString("co")
        val index = intent.extras!!.getInt("index")
        val stop = intent.extras!!.getString("stop")?.let { JSONObject(it) }
        val route = intent.extras!!.getString("route")?.let { JSONObject(it) }
        if (stopId == null || co == null || stop == null || route == null) {
            throw RuntimeException()
        }
        setContent {
            EtaMenuElement(stopId, co, index, stop, route, this)
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
fun EtaMenuElement(stopId: String, co: String, index: Int, stop: JSONObject, route: JSONObject, instance: EtaMenuActivity) {
    HKBusETATheme {
        val focusRequester = remember { FocusRequester() }
        val scroll = rememberLazyListState()

        val routeNumber = route.optString("route")
        val lat = stop.optJSONObject("location")!!.optDouble("lat")
        val lng = stop.optJSONObject("location")!!.optDouble("lng")

        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalLazyScrollbar(
                    state = scroll
                )
                .rotaryScroll(scroll, focusRequester),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = scroll
        ) {
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(20, instance).dp))
            }
            item {
                Title(index, stop.optJSONObject("name")!!, routeNumber, co, instance)
                SubTitle(Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route), routeNumber, co, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            }
            item {
                MoreInfoHeader(instance)
                Spacer(modifier = Modifier.size(10.dp))
            }
            item {
                SearchNearbyButton(stop, route, instance)
                Spacer(modifier = Modifier.size(10.dp))
            }
            item {
                OpenOnMapsButton(stop.optJSONObject("name")!!, lat, lng, instance)
                Spacer(modifier = Modifier.size(10.dp))
            }
            item {
                Spacer(modifier = Modifier.size(10.dp))
            }
            item {
                FavHeader(instance)
                Spacer(modifier = Modifier.size(10.dp))
            }
            items(8) {
                FavButton(it + 1, stopId, co, index, stop, route, instance)
                Spacer(modifier = Modifier.size(10.dp))
            }
            item {
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(30, instance).dp))
            }
        }
    }
}

@Composable
fun MoreInfoHeader(instance: EtaMenuActivity) {
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp).clamp(max = 14.dp),
        text = if (Shared.language == "en") "More Info" else "更多資訊"
    )
}

@Composable
fun SearchNearbyButton(stop: JSONObject, route: JSONObject, instance: EtaMenuActivity) {
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(50, instance).sp.clamp(min = 50.dp).dp),
        onClick = {
            instance.runOnUiThread {
                val text = if (Shared.language == "en") {
                    "Nearby Interchange Routes of ".plus(stop.optJSONObject("name")!!.optString("en"))
                } else {
                    "".plus(stop.optJSONObject("name")!!.optString("zh")).plus(" 附近轉乘路線")
                }
                Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
            }
            val intent = Intent(instance, NearbyActivity::class.java)
            intent.putExtra("interchangeSearch", true)
            intent.putExtra("lat", stop.optJSONObject("location")!!.optDouble("lat"))
            intent.putExtra("lng", stop.optJSONObject("location")!!.optDouble("lng"))
            intent.putExtra("exclude", arrayListOf(route.optString("route")))
            instance.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .height(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(17F, instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_sync_alt_24),
                        tint = Color(0xFFFF0000),
                        contentDescription = if (Shared.language == "en") "Find Nearby Interchanges" else "尋找附近轉乘路線"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp),
                    text = if (Shared.language == "en") "Find Nearby Interchanges" else "尋找附近轉乘路線"
                )
            }
        }
    )
}

@Composable
fun OpenOnMapsButton(stopName: JSONObject, lat: Double, lng: Double, instance: EtaMenuActivity) {
    val haptic = LocalHapticFeedback.current
    val name = if (Shared.language == "en") stopName.optString("en") else stopName.optString("zh")
    AdvanceButton (
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(50, instance).sp.clamp(min = 50.dp).dp),
        onClick = handleOpenMaps(lat, lng, name, instance, false, haptic),
        onLongClick = handleOpenMaps(lat, lng, name, instance, true, haptic),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Row (
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .height(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D3D3D))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(StringUtils.scaledSize(17F, instance).sp.dp),
                        painter = painterResource(R.drawable.baseline_map_24),
                        tint = Color(0xFF4CFF00),
                        contentDescription = if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置"
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.primary,
                    fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp),
                    text = if (Shared.language == "en") "Open Stop Location on Maps" else "在地圖上顯示巴士站位置"
                )
            }
        }
    )
}

fun handleOpenMaps(lat: Double, lng: Double, label: String, instance: EtaMenuActivity, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
    return {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("geo:0,0?q=".plus(lat).plus(",").plus(lng).plus("(").plus(label).plus(")")))
        if (longClick) {
            instance.startActivity(intent)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            RemoteActivityUtils.intentToPhone(
                instance = instance,
                intent = intent,
                noPhone = {
                    instance.startActivity(intent)
                },
                failed = {
                    instance.startActivity(intent)
                },
                success = {
                    instance.runOnUiThread {
                        Toast.makeText(instance, if (Shared.language == "en") "Please check your phone" else "請在手機上繼續", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun Title(index: Int, stopName: JSONObject, routeNumber: String, co: String, instance: EtaMenuActivity) {
    val name = if (Shared.language == "en") stopName.optString("en") else stopName.optString("zh")
    AutoResizeText (
        modifier = Modifier
            .fillMaxWidth()
            .padding(37.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = if (co == "mtr") name else index.toString().plus(". ").plus(name),
        maxLines = 2,
        fontWeight = FontWeight(900),
        fontSizeRange = FontSizeRange(
            min = StringUtils.scaledSize(1F, instance).dp.sp,
            max = StringUtils.scaledSize(17F, instance).sp.clamp(max = StringUtils.scaledSize(17F, instance).dp)
        )
    )
}

@Composable
fun SubTitle(destName: JSONObject, routeNumber: String, co: String, instance: EtaMenuActivity) {
    val name = if (Shared.language == "en") {
        val routeName = if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber
        routeName.plus(" To ").plus(destName.optString("en"))
    } else {
        val routeName = if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber
        routeName.plus(" 往").plus(destName.optString("zh"))
    }
    AutoResizeText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = name,
        maxLines = 1,
        fontSizeRange = FontSizeRange(
            min = StringUtils.scaledSize(1F, instance).dp.sp,
            max = StringUtils.scaledSize(11F, instance).sp.clamp(max = StringUtils.scaledSize(11F, instance).dp)
        )
    )
}

@Composable
fun FavHeader(instance: EtaMenuActivity) {
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp).clamp(max = 14.dp),
        text = if (Shared.language == "en") "Set Favourite Routes" else "設置最喜愛路線"
    )
    Text(
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(10F, instance), TextUnitType.Sp).clamp(max = 10.dp),
        text = if (Shared.language == "en") {
            "Click below to set/clear this route stop from the corresponding indexed favourite route"
        } else {
            "點擊可設置/清除對應的最喜愛路線"
        }
    )
}

fun getFavState(favoriteIndex: Int, stopId: String, co: String, index: Int, stop: JSONObject, route: JSONObject, instance: EtaMenuActivity): Int {
    val registry = Registry.getInstance(instance)
    if (registry.hasFavouriteRouteStop(favoriteIndex)) {
        return if (registry.isFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route)) 2 else 1
    }
    return 0
}

@Composable
fun FavButton(favoriteIndex: Int, stopId: String, co: String, index: Int, stop: JSONObject, route: JSONObject, instance: EtaMenuActivity) {
    val state = remember { mutableStateOf(getFavState(favoriteIndex, stopId, co, index, stop, route, instance)) }

    LaunchedEffect (Unit) {
        while (true) {
            delay(500)
            val newState = getFavState(favoriteIndex, stopId, co, index, stop, route, instance)
            if (newState != state.value) {
                state.value = newState
            }
        }
    }

    val haptic = LocalHapticFeedback.current
    AdvanceButton(
        onClick = {
            if (state.value == 2) {
                Registry.getInstance(instance).clearFavouriteRouteStop(favoriteIndex, instance)
                Toast.makeText(instance, if (Shared.language == "en") "Cleared Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "已清除資訊方塊路線巴士站預計到達時間".plus(favoriteIndex), Toast.LENGTH_SHORT).show()
            } else {
                Registry.getInstance(instance).setFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route, instance)
                Toast.makeText(instance, if (Shared.language == "en") "Set Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "已設置資訊方塊路線巴士站預計到達時間".plus(favoriteIndex), Toast.LENGTH_SHORT).show()
            }
            state.value = getFavState(favoriteIndex, stopId, co, index, stop, route, instance)
        },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val intent = Intent(instance, FavActivity::class.java)
            intent.putExtra("scrollToIndex", favoriteIndex)
            instance.startActivity(intent)
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(50, instance).sp.clamp(min = 50.dp).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = when (state.value) {
                0 -> Color(0xFF444444)
                1 -> Color(0xFF4E4E00)
                2 -> Color(0xFFFFFF00)
                else -> Color(0xFF444444)
            }
        ),
        content = {
            Row(
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .height(StringUtils.scaledSize(30, instance).sp.clamp(max = 30.dp).dp)
                        .clip(CircleShape)
                        .background(if (state.value == 2) Color(0xFF3D3D3D) else Color(0xFF131313))
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
                        color = when (state.value) {
                            0 -> Color(0xFF444444)
                            1 -> Color(0xFF4E4E00)
                            2 -> Color(0xFFFFFF00)
                            else -> Color(0xFF444444)
                        },
                        text = favoriteIndex.toString()
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = if (state.value == 2) Color(0xFFFFFFFF) else Color(0xFF3F3F3F),
                    fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp),
                    text = when (state.value) {
                        0 -> if (Shared.language == "en") "No Route Stop Selected" else "未有設置路線巴士站"
                        1 -> if (Shared.language == "en") "Selected by Another Route Stop" else "已設置為另一路線巴士站"
                        2 -> if (Shared.language == "en") "Selected as This Route Stop" else "已設置為本路線巴士站"
                        else -> ""
                    }
                )
            }
        }
    )
}