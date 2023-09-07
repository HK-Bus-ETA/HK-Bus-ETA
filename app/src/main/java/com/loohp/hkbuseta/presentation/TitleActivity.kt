package com.loohp.hkbuseta.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.presentation.shared.Registry
import com.loohp.hkbuseta.presentation.shared.Shared
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.LocationUtils
import com.loohp.hkbuseta.presentation.utils.RemoteActivityUtils
import com.loohp.hkbuseta.presentation.utils.StringUtils
import java.util.function.Consumer


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
                .fillMaxSize()
                .padding(20.dp, 0.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(25, instance).dp))
            SearchButton(instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            NearbyButton(instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            LanguageButton(instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            CreditVersionText(instance)
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
            .width(StringUtils.scaledSize(StringUtils.scaledSize(220, instance), instance).dp)
            .height(StringUtils.scaledSize(StringUtils.scaledSize(45, instance), instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
                text = if (Shared.language == "en") "Input Route" else "輸入巴士路線"
            )
        }
    )
}

fun handleNearbyClick(permission: Boolean, instance: TitleActivity) {
    instance.runOnUiThread {
        if (permission) {
            instance.startActivity(Intent(instance, NearbyActivity::class.java))
        } else {
            Toast.makeText(instance, if (Shared.language == "en") "Location Access Permission Denied" else "位置存取權限被拒絕", Toast.LENGTH_SHORT).show()
        }
    }
}

fun handleNearbyClick(instance: TitleActivity) {
    if (LocationUtils.checkLocationPermission(instance) { r -> handleNearbyClick(r, instance) }) {
        handleNearbyClick(true, instance)
    }
}

@Composable
fun NearbyButton(instance: TitleActivity) {
    Button(
        onClick = {
            handleNearbyClick(instance)
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(StringUtils.scaledSize(220, instance), instance).dp)
            .height(StringUtils.scaledSize(StringUtils.scaledSize(45, instance), instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
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
            .width(StringUtils.scaledSize(110, instance).dp)
            .height(StringUtils.scaledSize(35, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(3F, instance), TextUnitType.Em),
                text = if (Shared.language == "en") "中文" else "English"
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
        fontSize = TextUnit(StringUtils.scaledSize(1.5F, instance), TextUnitType.Em),
        text = instance.resources.getString(R.string.app_name).plus(" v").plus(packageInfo.versionName).plus(" (").plus(packageInfo.longVersionCode).plus(")\n@LoohpJames")
    )
}