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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.presentation.shared.Registry
import com.loohp.hkbuseta.presentation.shared.Shared
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.StringUtils

class TitleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HKBusETAApp(this)
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(25, instance).dp))
            SearchButton(instance)
            Spacer(modifier = Modifier.size(10.dp))
            NearbyButton(instance)
            Spacer(modifier = Modifier.size(10.dp))
            LanguageButton(instance)
        }
    }
}

@Composable
fun SearchButton(instance: TitleActivity) {
    Button(
        onClick = {
            instance.startActivity(Intent(instance, SearchActivity::class.java))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(3F, TextUnitType.Em),
                text = if (Shared.language == "en") "Input Route" else "輸入巴士路線"
            )
        }
    )
}

@Composable
fun NearbyButton(instance: TitleActivity) {
    Button(
        onClick = {
            if (Registry.getInstance(instance).checkLocationPermission(instance, true)) {
                instance.startActivity(Intent(instance, NearbyActivity::class.java))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(3F, TextUnitType.Em),
                text = if (Shared.language == "en") "Search Nearby" else "附近巴士路線"
            )
        }
    )
}

@Composable
fun LanguageButton(instance: TitleActivity) {
    Button(
        onClick = {
            Registry.getInstance(instance).setLanguage(if (Shared.language == "en") "zh" else "en", instance)
            instance.startActivity(Intent(instance, MainActivity::class.java))
            instance.finish()
        },
        modifier = Modifier
            .width(130.dp)
            .height(35.dp)
            .padding(20.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = if (Shared.language == "en") "中文" else "English"
            )
        }
    )
}