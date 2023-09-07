package com.loohp.hkbuseta.presentation

import android.content.Intent
import android.location.Location
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
import com.google.android.gms.tasks.Task
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
        setContent {
            NearbyPage(this)
        }
    }
}

@Composable
fun NearbyPage(instance: NearbyActivity) {
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
            MainElement(instance)
        }
    }
}

@Composable
fun WaitingText(instance: NearbyActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
        text = if (Shared.language == "en") "Locating..." else "正在讀取你的位置..."
    )
}

@Composable
fun FailedText(instance: NearbyActivity) {
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
        text = if (Shared.language == "en") "Unable to read your location" else "無法讀取你的位置"
    )
    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(2.5F, instance), TextUnitType.Em),
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
        fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
        text = if (Shared.language == "en") "There are no nearby bus stops" else "附近沒有巴士站"
    )
    Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(2.25F, instance), TextUnitType.Em),
        text = if (Shared.language == "en")
            "Nearest Stop: ".plus(closestStop.optJSONObject("name").optString("en")).plus(" (").plus(distance.roundToInt()).plus("m)")
        else
            "最近的巴士站: ".plus(closestStop.optJSONObject("name").optString("zh")).plus(" (").plus(distance.roundToInt()).plus("米)")
    )
}

@Composable
fun MainElement(instance: NearbyActivity) {
    var task: LocationResult? by remember { mutableStateOf(null) }

    LaunchedEffect (Unit) {
        ForkJoinPool.commonPool().execute {
            task = LocationUtils.getGPSLocation(instance).get()
        }
    }

    EvaluatedElement(task, instance)
}

@Composable
fun EvaluatedElement(task: LocationResult?, instance: NearbyActivity) {
    if (task == null) {
        WaitingText(instance)
    } else {
        var loc: Location? = null
        if (task.isSuccess && task.location.also { loc = it } != null) {
            val result = Registry.getInstance(instance).getNearbyRoutes(loc!!.latitude, loc!!.longitude)
            val list = result.result
            if (list.isEmpty()) {
                NoNearbyText(result.closestStop, result.cloestDistance, instance)
            } else {
                val intent = Intent(instance, ListRouteActivity::class.java)
                intent.putExtra("result", JsonUtils.fromCollection(list).toString())
                instance.startActivity(intent)
                instance.finish()
            }
        } else {
            FailedText(instance)
        }
    }
}