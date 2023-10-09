package com.loohp.hkbuseta

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.clamp
import kotlinx.coroutines.delay
import java.util.Timer
import java.util.TimerTask


class FavActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)
        setContent {
            FavElements(this)
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
fun FavElements(instance: FavActivity) {
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
            FavTitle(instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FavButton(1, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                FavButton(2, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                FavButton(3, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                FavButton(4, instance)
            }
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FavButton(5, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                FavButton(6, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                FavButton(7, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
                FavButton(8, instance)
            }
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(10, instance).dp))
            FavDescription(instance)
        }
    }
}

@Composable
fun FavTitle(instance: FavActivity) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp).clamp(max = 17.dp),
        text = if (Shared.language == "en") "Favourite Routes" else "最喜愛路線"
    )
}

@Composable
fun FavDescription(instance: FavActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(11F, instance), TextUnitType.Sp).clamp(max = 11.dp),
        text = if (Shared.language == "en") "These routes will display in their corresponding indexed Tile" else "這些路線將顯示在其相應數字的資訊方塊中"
    )
}

@Composable
fun FavButton(favoriteIndex: Int, instance: FavActivity) {
    val hasFavouriteStopRoute = remember { mutableStateOf(Shared.favoriteRouteStops[favoriteIndex] != null) }
    val deleteState = remember { mutableStateOf(false) }

    LaunchedEffect (Unit) {
        while (true) {
            delay(500)
            val newState = Shared.favoriteRouteStops[favoriteIndex] != null
            if (newState != hasFavouriteStopRoute.value) {
                hasFavouriteStopRoute.value = newState
            }
        }
    }

    FavButtonInternal(favoriteIndex, hasFavouriteStopRoute, deleteState, instance)
}

@Composable
fun FavButtonInternal(favoriteIndex: Int, hasFavouriteStopRoute: MutableState<Boolean>, deleteState: MutableState<Boolean>, instance: FavActivity) {
    val haptic = LocalHapticFeedback.current

    AdvanceButton(
        onClick = {
            if (deleteState.value) {
                if (Registry.getInstance(instance).hasFavouriteRouteStop(favoriteIndex)) {
                    Registry.getInstance(instance).clearFavouriteRouteStop(favoriteIndex, instance)
                    Toast.makeText(instance, if (Shared.language == "en") "Cleared Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "已清除資訊方塊路線巴士站預計到達時間".plus(favoriteIndex), Toast.LENGTH_SHORT).show()
                }
                val newState = Shared.favoriteRouteStops[favoriteIndex] != null
                if (newState != hasFavouriteStopRoute.value) {
                    hasFavouriteStopRoute.value = newState
                }
                deleteState.value = false
            } else {
                val favouriteStopRoute = Shared.favoriteRouteStops[favoriteIndex]
                if (favouriteStopRoute != null) {
                    val stopId = favouriteStopRoute.optString("stopId")
                    val co = favouriteStopRoute.optString("co")
                    val index = favouriteStopRoute.optInt("index")
                    val stop = favouriteStopRoute.optJSONObject("stop")!!
                    val route = favouriteStopRoute.optJSONObject("route")!!

                    val intent = Intent(instance, EtaActivity::class.java)
                    intent.putExtra("stopId", stopId)
                    intent.putExtra("co", co)
                    intent.putExtra("index", index)
                    intent.putExtra("stop", stop.toString())
                    intent.putExtra("route", route.toString())
                    instance.startActivity(intent)
                }
            }
        },
        onLongClick = {
            if (!deleteState.value) {
                deleteState.value = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        if (deleteState.value) {
                            deleteState.value = false
                        }
                    }
                }, 5000)

                val currentFavouriteStopRoute = Shared.favoriteRouteStops[favoriteIndex]
                if (currentFavouriteStopRoute != null) {
                    val index = currentFavouriteStopRoute.optInt("index")
                    val stop = currentFavouriteStopRoute.optJSONObject("stop")!!
                    val stopName = stop.optJSONObject("name")!!
                    val route = currentFavouriteStopRoute.optJSONObject("route")!!
                    val kmbCtbJoint = route.optBoolean("kmbCtbJoint", false)
                    val co = currentFavouriteStopRoute.optString("co")
                    val routeNumber = route.optString("route")
                    val stopId = currentFavouriteStopRoute.optString("stopId")
                    val destName = Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route)

                    val operator = if (Shared.language == "en") {
                        when (co) {
                            "kmb" -> if (Shared.isLWBRoute(routeNumber)) (if (kmbCtbJoint) "LWB/CTB" else "LWB") else (if (kmbCtbJoint) "KMB/CTB" else "KMB")
                            "ctb" -> "CTB"
                            "nlb" -> "NLB"
                            "mtr-bus" -> "MTR-Bus"
                            "gmb" -> "GMB"
                            "lightRail" -> "LRT"
                            "mtr" -> "MTR"
                            else -> "???"
                        }
                    } else {
                        when (co) {
                            "kmb" -> if (Shared.isLWBRoute(routeNumber)) (if (kmbCtbJoint) "龍運/城巴" else "龍運") else (if (kmbCtbJoint) "九巴/城巴" else "九巴")
                            "ctb" -> "城巴"
                            "nlb" -> "嶼巴"
                            "mtr-bus" -> "港鐵巴士"
                            "gmb" -> "專線小巴"
                            "lightRail" -> "輕鐵"
                            "mtr" -> "港鐵"
                            else -> "???"
                        }
                    }
                    val routeName = if (Shared.language == "en") {
                        val routeName = if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber
                        routeName.plus(" To ").plus(StringUtils.capitalize(destName.optString("en")))
                    } else {
                        val routeName = if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber
                        routeName.plus(" 往").plus(destName.optString("zh"))
                    }
                    val text = if (Shared.language == "en") {
                        operator.plus(" ").plus(routeName).plus("\n").plus(index).plus(". ").plus(StringUtils.capitalize(stopName.optString("en")))
                    } else {
                        operator.plus(" ").plus(routeName).plus("\n").plus(index).plus(". ").plus(stopName.optString("zh"))
                    }
                    instance.runOnUiThread {
                        Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
                    }
                }
            }
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(35, instance).dp)
            .height(StringUtils.scaledSize(35, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (deleteState.value) Color(0xFF633A3A) else MaterialTheme.colors.secondary,
            contentColor = if (deleteState.value) Color(0xFFFF0000) else if (hasFavouriteStopRoute.value) Color(0xFFFFFF00) else Color(0xFF444444),
        ),
        enabled = hasFavouriteStopRoute.value,
        content = {
            if (deleteState.value) {
                Icon(
                    modifier = Modifier.size(StringUtils.scaledSize(21, instance).dp),
                    imageVector = Icons.Filled.Clear,
                    tint = Color(0xFFFF0000),
                    contentDescription = if (Shared.language == "en") "Clear Route Stop ETA ".plus(favoriteIndex).plus(" Tile") else "清除資訊方塊路線巴士站預計到達時間".plus(favoriteIndex)
                )
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
                    color = if (hasFavouriteStopRoute.value) Color(0xFFFFFF00) else Color(0xFF444444),
                    text = favoriteIndex.toString()
                )
            }
        }
    )
}