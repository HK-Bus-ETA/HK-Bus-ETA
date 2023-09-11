package com.loohp.hkbuseta

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.loohp.hkbuseta.ui.theme.HKBusETATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HKBusETATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1A1A1A)
                ) {
                    PhoneElements(this)
                }
            }
        }
    }
}

fun openLoohpJames(instance: MainActivity) {
    val intent = Intent(Intent.ACTION_VIEW)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setData(Uri.parse("https://loohpjames.com"))
    instance.startActivity(intent)
}

fun openGooglePlay(instance: MainActivity) {
    val intent = Intent(Intent.ACTION_VIEW)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setData(Uri.parse("https://play.google.com/store/apps/details?id=com.loohp.hkbuseta"))
    instance.startActivity(intent)
}

@Composable
fun PhoneElements(instance: MainActivity) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.clickable {
                openGooglePlay(instance)
            }.size(100.dp),
            painter = painterResource(R.mipmap.icon),
            contentDescription = instance.resources.getString(R.string.app_name)
        )
        Spacer(modifier = Modifier.size(20.dp))
        Text(
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = TextUnit(30F, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            text = instance.resources.getString(R.string.app_name)
        )
        Text(
            modifier = Modifier
                .clickable {
                    openLoohpJames(instance)
                },
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = TextUnit(13F, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            text = "@LoohpJames"
        )
        Spacer(modifier = Modifier.size(20.dp))
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(30.dp, 0.dp),
                textAlign = TextAlign.Left,
                color = Color.White,
                fontSize = TextUnit(17F, TextUnitType.Sp),
                text = instance.resources.getString(R.string.description_1)
            )
            Spacer(modifier = Modifier.size(20.dp))
            Text(
                modifier = Modifier.padding(30.dp, 0.dp),
                textAlign = TextAlign.Left,
                color = Color.White,
                fontSize = TextUnit(17F, TextUnitType.Sp),
                text = instance.resources.getString(R.string.description_2)
            )
            Spacer(modifier = Modifier.size(20.dp))
            Text(
                modifier = Modifier.padding(30.dp, 0.dp),
                textAlign = TextAlign.Left,
                color = Color.White,
                fontSize = TextUnit(17F, TextUnitType.Sp),
                text = instance.resources.getString(R.string.description_3)
            )
            Spacer(modifier = Modifier.size(40.dp))
            Text(
                modifier = Modifier.padding(30.dp, 0.dp),
                textAlign = TextAlign.Left,
                color = Color.White,
                fontSize = TextUnit(17F, TextUnitType.Sp),
                text = instance.resources.getString(R.string.download_description)
            )
            Spacer(modifier = Modifier.size(20.dp))
            Button(
                onClick = {
                    openGooglePlay(instance)
                },
                modifier = Modifier.padding(30.dp, 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF636363),
                    contentColor = Color(0xFFFFFFFF)
                ),
                content = {
                    Text(
                        textAlign = TextAlign.Left,
                        color = Color.White,
                        fontSize = TextUnit(17F, TextUnitType.Sp),
                        text = instance.resources.getString(R.string.download)
                    )
                }
            )
            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}