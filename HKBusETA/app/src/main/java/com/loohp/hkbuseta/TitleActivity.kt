package com.loohp.hkbuseta

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.LocationUtils
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.StringUtils


class TitleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)
        setContent {
            HKBusETAApp(this)
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
            Row (
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageButton(instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                FavButton(instance)
            }
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
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
                text = if (Shared.language == "en") "Input Route" else "輸入巴士路線"
            )
        }
    )
}

@Composable
fun NearbyButton(instance: TitleActivity) {
    Button(
        onClick = {
            LocationUtils.checkLocationPermission(instance) {
                instance.runOnUiThread {
                    if (it) {
                        instance.startActivity(Intent(instance, NearbyActivity::class.java))
                    } else {
                        Toast.makeText(instance, if (Shared.language == "en") "Location Access Permission Denied" else "位置存取權限被拒絕", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(220, instance).dp)
            .height(StringUtils.scaledSize(45, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
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
            .width(StringUtils.scaledSize(90, instance).dp)
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
                fontSize = TextUnit(StringUtils.scaledSize(17F, instance), TextUnitType.Sp),
                text = if (Shared.language == "en") "中文" else "English"
            )
        }
    )
}

@Composable
fun FavButton(instance: TitleActivity) {
    Button(
        onClick = {
            instance.startActivity(Intent(instance, FavActivity::class.java))
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(35, instance).dp)
            .height(StringUtils.scaledSize(35, instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color(0xFFFFFF00)
        ),
        content = {
            Icon(
                modifier = Modifier.size(StringUtils.scaledSize(21, instance).dp),
                imageVector = Icons.Filled.Star,
                tint = Color(0xFFFFFF00),
                contentDescription = if (Shared.language == "en") "Favourite Routes" else "最喜愛路線"
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