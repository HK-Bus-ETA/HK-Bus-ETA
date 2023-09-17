package com.loohp.hkbuseta.presentation

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.presentation.shared.Registry
import com.loohp.hkbuseta.presentation.shared.Shared
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.JsonUtils
import com.loohp.hkbuseta.presentation.utils.LocationUtils
import com.loohp.hkbuseta.presentation.utils.LocationUtils.LocationResult
import com.loohp.hkbuseta.presentation.utils.StringUtils
import org.json.JSONObject
import java.util.concurrent.ForkJoinPool
import kotlin.math.roundToInt


class NearbyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var location: LocationResult? = null
        var exclude: Set<String> = emptySet()
        if (intent.extras != null) {
            if (intent.extras!!.containsKey("lat") && intent.extras!!.containsKey("lng")) {
                val lat = intent.extras!!.getDouble("lat")
                val lng = intent.extras!!.getDouble("lng")
                location = LocationResult.fromLatLng(lat, lng)
            }
            if (intent.extras!!.containsKey("exclude")) {
                exclude = HashSet(intent.extras!!.getStringArrayList("exclude")!!)
            }
        }

        setContent {
            NearbyPage(location, exclude, this)
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
fun NearbyPage(location: LocationResult?, exclude: Set<String>, instance: NearbyActivity) {
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
            MainElement(location, exclude, instance)
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
        fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
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
        fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
        text = if (Shared.language == "en") "Unable to read your location" else "無法讀取你的位置"
    )
    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(14F, instance), TextUnitType.Sp),
        text = if (Shared.language == "en") "Please check whether your GPS is enabled" else "請檢查你的定位服務是否已開啟"
    )
}

@Composable
fun NoNearbyText(closestStop: JSONObject, distance: Double, instance: NearbyActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
        text = if (Shared.language == "en") "There are no nearby bus stops" else "附近沒有巴士站"
    )
    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(12.5F, instance), TextUnitType.Sp),
        text = if (Shared.language == "en")
            "Nearest Stop: ".plus(closestStop.optJSONObject("name").optString("en")).plus(" (").plus(distance.roundToInt()).plus("m)")
        else
            "最近的巴士站: ".plus(closestStop.optJSONObject("name").optString("zh")).plus(" (").plus(distance.roundToInt()).plus("米)")
    )
}

@Composable
fun MainElement(location: LocationResult?, exclude: Set<String>, instance: NearbyActivity) {
    var state by remember { mutableStateOf(false) }
    var result: Registry.NearbyRoutesResult? by remember { mutableStateOf(null) }

    LaunchedEffect (Unit) {
        ForkJoinPool.commonPool().execute {
            val locationResult = location?: LocationUtils.getGPSLocation(instance).get()
            if (locationResult.isSuccess) {
                val loc = locationResult.location
                result = Registry.getInstance(instance).getNearbyRoutes(loc!!.latitude, loc.longitude, exclude)
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
                intent.putExtra("result", JsonUtils.fromCollection(list).toString())
                instance.startActivity(intent)
                instance.finish()
            }
        }
    } else {
        WaitingText(usingGps, instance)
    }
}