package com.loohp.hkbuseta.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.AutoResizeText
import com.loohp.hkbuseta.presentation.utils.FontSizeRange
import com.loohp.hkbuseta.presentation.utils.StringUtils
import com.loohp.hkbuseta.presentation.utils.StringUtilsKt
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.Collections


class EtaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stopId = intent.extras!!.getString("stopId")
        val co = intent.extras!!.getString("co")
        val index = intent.extras!!.getInt("index")
        val stop = intent.extras!!.getString("stop")?.let { JSONObject(it) }
        val route = intent.extras!!.getString("route")?.let { JSONObject(it) }
        if (stopId == null || co == null || stop == null || route == null) {
            throw RuntimeException()
        }
        setContent {
            EtaElement(stopId, co, index, stop, route, this)
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun EtaElement(stopId: String, co: String, index: Int, stop: JSONObject, route: JSONObject, instance: EtaActivity) {
    var eta: Map<Int, String> by remember { mutableStateOf(Collections.emptyMap()) }
    LaunchedEffect (Unit) {
        while (true) {
            Thread {
                eta = Registry.getEta(stopId, co, index, stop, route)
            }.start()
            delay(30000)
        }
    }
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(7.dp))
            Title(index, stop.optJSONObject("name"), route.optString("route"))
            SubTitle(route.optJSONObject("dest"))
            Spacer(modifier = Modifier.size(12.dp))
            EtaText(eta, 1)
            Spacer(modifier = Modifier.size(7.dp))
            EtaText(eta, 2)
            Spacer(modifier = Modifier.size(7.dp))
            EtaText(eta, 3)
            Spacer(modifier = Modifier.size(3.dp))
            Row(
                horizontalArrangement = Arrangement.Center
            )  {
                FavButton(1, stopId, co, index, stop, route, instance)
                Spacer(modifier = Modifier.size(5.dp))
                FavButton(2, stopId, co, index, stop, route, instance)
                Spacer(modifier = Modifier.size(5.dp))
                FavButton(3, stopId, co, index, stop, route, instance)
                Spacer(modifier = Modifier.size(5.dp))
                FavButton(4, stopId, co, index, stop, route, instance)
            }
        }
    }
}

@Composable
fun FavButton(favoriteIndex: Int, stopId: String, co: String, index: Int, stop: JSONObject, route: JSONObject, instance: EtaActivity) {
    val state = remember { mutableStateOf(Registry.getInstance(instance).isFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route)) }
    Button(
        onClick = {
            if (state.value) {
                Registry.getInstance(instance).clearFavouriteRouteStop(favoriteIndex, instance)
            } else {
                Registry.getInstance(instance).setFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route, instance)
            }
            state.value = Registry.getInstance(instance).isFavouriteRouteStop(favoriteIndex, stopId, co, index, stop, route)
        },
        modifier = Modifier
            .width(24.dp)
            .height(24.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = if (state.value) Color.Yellow else MaterialTheme.colors.secondaryVariant
        ),
        content = {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = if (state.value) Color.Yellow else Color(0xFF444444),
                text = favoriteIndex.toString()
            )
        }
    )
}

@Composable
fun Title(index: Int, stopName: JSONObject, routeNumber: String) {
    var name = stopName.optString(Shared.language)
    if (Shared.language == "en") {
        name = StringUtils.capitalize(name)
        AutoResizeText (
            modifier = Modifier
                .fillMaxWidth()
                .padding(37.dp, 0.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            text = "[".plus(routeNumber).plus("] ").plus(index).plus(". ").plus(name),
            maxLines = 2,
            fontWeight = FontWeight(900),
            fontSizeRange = FontSizeRange(
                min = 10.sp,
                max = 17.sp,
            )
        )
    } else {
        AutoResizeText (
            modifier = Modifier
                .fillMaxWidth()
                .padding(37.dp, 0.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            text = "[".plus(routeNumber).plus("] ").plus(index).plus(". ").plus(name),
            maxLines = 2,
            fontWeight = FontWeight(900),
            fontSizeRange = FontSizeRange(
                min = 10.sp,
                max = 17.sp,
            )
        )
    }
}

@Composable
fun SubTitle(destName: JSONObject) {
    var name = destName.optString(Shared.language)
    name = if (Shared.language == "en") "To " + StringUtils.capitalize(name) else "往$name"
    AutoResizeText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = name,
        maxLines = 1,
        fontSizeRange = FontSizeRange(
            min = 5.sp,
            max = 11.sp,
        )
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EtaText(lines: Map<Int, String>, seq: Int) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp)
            .basicMarquee(iterations = Int.MAX_VALUE),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        maxLines = 1,
        text = StringUtilsKt.toAnnotatedString(HtmlCompat.fromHtml(lines.getOrDefault(seq, "-"), HtmlCompat.FROM_HTML_MODE_COMPACT))
    )
}