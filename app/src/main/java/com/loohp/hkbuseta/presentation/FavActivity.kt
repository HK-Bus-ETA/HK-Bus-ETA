package com.loohp.hkbuseta.presentation

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.presentation.shared.Shared
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.StringUtils
import kotlinx.coroutines.delay


class FavActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FavElements(this)
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
        fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
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
        fontSize = TextUnit(StringUtils.scaledSize(2F, instance), TextUnitType.Em),
        text = if (Shared.language == "en") "These routes will display in their corresponding indexed Tile" else "這些路線將顯示在其相應數字的資訊方塊中"
    )
}

@Composable
fun FavButton(favoriteIndex: Int, instance: FavActivity) {
    var hasFavouriteStopRoute by remember { mutableStateOf(Shared.favoriteRouteStops[favoriteIndex] != null) }

    LaunchedEffect (Unit) {
        while (true) {
            delay(1000)
            val newState = Shared.favoriteRouteStops[favoriteIndex] != null
            if (newState != hasFavouriteStopRoute) {
                hasFavouriteStopRoute = newState
            }
        }
    }

    FavButtonInternal(favoriteIndex, hasFavouriteStopRoute, instance)
}

@Composable
fun FavButtonInternal(favoriteIndex: Int, hasFavouriteStopRoute: Boolean, instance: FavActivity) {
    Button(
        onClick = {
            val favouriteStopRoute = Shared.favoriteRouteStops[favoriteIndex]
            if (favouriteStopRoute != null) {
                val stopId = favouriteStopRoute.optString("stopId")
                val co = favouriteStopRoute.optString("co")
                val index = favouriteStopRoute.optInt("index")
                val stop = favouriteStopRoute.optJSONObject("stop")
                val route = favouriteStopRoute.optJSONObject("route")

                val intent = Intent(instance, EtaActivity::class.java)
                intent.putExtra("stopId", stopId)
                intent.putExtra("co", co)
                intent.putExtra("index", index)
                intent.putExtra("stop", stop.toString())
                intent.putExtra("route", route.toString())
                instance.startActivity(intent)
            }
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(35, instance).dp)
            .height(StringUtils.scaledSize(35, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = if (hasFavouriteStopRoute) Color(0xFFFFFF00) else Color(0xFF444444),
        ),
        enabled = hasFavouriteStopRoute,
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
                color = if (hasFavouriteStopRoute) Color(0xFFFFFF00) else Color(0xFF444444),
                text = favoriteIndex.toString()
            )
        }
    )
}