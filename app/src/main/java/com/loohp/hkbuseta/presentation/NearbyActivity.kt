package com.loohp.hkbuseta.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.presentation.shared.Registry
import com.loohp.hkbuseta.presentation.shared.Shared
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.JsonUtils


class NearbyActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NearbyPage(this)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            MainElement(instance)
        }
    }
}

@Composable
fun NoNearbyText() {
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = if (Shared.language == "en") "There are no nearby bus stops" else "附近沒有巴士站"
    )
}

@Suppress("FoldInitializerAndIfToElvis")
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MainElement(instance: NearbyActivity) {
    val gps = Registry.getInstance(instance).getGPSLocation(instance)
    if (gps == null) {
        return NoNearbyText()
    }
    val result = Registry.getInstance(instance).getNearbyRoutes(gps[0], gps[1])
    return if (result == null) {
        NoNearbyText()
    } else {
        val intent = Intent(instance, ListRouteActivity::class.java)
        intent.putExtra("result", JsonUtils.fromCollection(result).toString())
        instance.startActivity(intent)
        instance.finish()
        Text(text = "")
    }
}