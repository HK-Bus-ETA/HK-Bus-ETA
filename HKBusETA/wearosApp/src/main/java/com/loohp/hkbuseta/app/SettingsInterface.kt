/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.TextRotationNone
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.ButtonDefaults
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.appcontext.wear
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.ETADisplayMode
import com.loohp.hkbuseta.common.objects.WearableConnectionState
import com.loohp.hkbuseta.common.shared.BASE_URL
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.toLocalDateTime
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


fun relaunch(instance: AppActiveContext, skipSplash: Boolean = true) {
    val intent = AppIntent(instance, AppScreen.MAIN)
    intent.addFlags(AppIntentFlag.NEW_TASK, AppIntentFlag.CLEAR_TASK)
    intent.putExtra("relaunch", AppScreen.SETTINGS.name)
    if (skipSplash) {
        intent.putExtra("skipSplash", true)
    }
    instance.startActivity(intent)
    instance.finishAffinity()
}

suspend fun invalidatePhoneCache(context: AppContext) {
    if (RemoteActivityUtils.hasPhoneApp(context.context).await()) {
        RemoteActivityUtils.dataToPhone(context.context, Shared.INVALIDATE_CACHE_ID, null)
    }
}

@Composable
fun rememberPhoneConnected(context: AppContext): State<WearableConnectionState> {
    val state = remember { mutableStateOf(WearableConnectionState.NONE_DETECTED) }

    LaunchedEffect (Unit) {
        while (true) {
            state.value = if (RemoteActivityUtils.hasPhoneApp(context.context).await()) {
                WearableConnectionState.CONNECTED
            } else {
                WearableConnectionState.NONE_DETECTED
            }
            delay(5000)
        }
    }

    return state
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun SettingsInterface(instance: AppActiveContext) {
    val focusRequester = rememberActiveFocusRequester()
    val scroll = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val phoneConnection by rememberPhoneConnected(instance)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalLazyScrollbar(
                    state = scroll,
                    context = instance
                )
                .rotaryScroll(scroll, focusRequester),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = scroll
        ) {
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
            item {
                SettingsButton(
                    instance = instance,
                    onClick = {
                        Registry.getInstance(instance).setLanguage(if (Shared.language == "en") "zh" else "en", instance)
                        relaunch(instance)
                    },
                    icon = Icons.Filled.Translate,
                    text = "切換語言 Switch Language".asAnnotatedString(),
                    subText = (if (Shared.language == "en") "English/中文" else "中文/English").asAnnotatedString()
                )
            }
            item {
                SettingsButton(
                    instance = instance,
                    onClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            Registry.invalidateCache(instance)
                            Registry.clearInstance()
                            invalidatePhoneCache(instance)
                            delay(500)
                            relaunch(instance, skipSplash = false)
                        }
                    },
                    icon = Icons.Filled.Update,
                    text = (if (Shared.language == "en") "Update Route Database" else "更新路線資料庫").asAnnotatedString(),
                    subText = ("${if (Shared.language == "en") "Last updated" else "最近更新時間"}: ${Registry.getInstance(instance).getLastUpdatedTime()?.let { instance.formatDateTime(it.toLocalDateTime(), true) }?: if (Shared.language == "en") "Never" else "從未"}").asAnnotatedString()
                )
            }
            item {
                var historyEnabled by remember { mutableStateOf(Shared.historyEnabled) }
                SettingsButton(
                    instance = instance,
                    onClick = {
                        Registry.getInstance(instance).setHistoryEnabled(!Shared.historyEnabled, instance)
                        historyEnabled = Shared.historyEnabled
                    },
                    icon = if (historyEnabled) rememberVectorPainter(Icons.Outlined.History) else painterResource(R.drawable.baseline_delete_history_24),
                    text = (if (Shared.language == "en") "Recent History" else "歷史記錄").asAnnotatedString(),
                    subText = if (historyEnabled) {
                        if (Shared.language == "en") "Enabled" else "開啟"
                    } else {
                        if (Shared.language == "en") "Disabled" else "停用"
                    }.asAnnotatedString()
                )
            }
            item {
                var etaDisplayMode by remember { mutableStateOf(Shared.etaDisplayMode) }
                SettingsButton(
                    instance = instance,
                    onClick = {
                        Registry.getInstance(instance).setEtaDisplayMode(Shared.etaDisplayMode.next, instance)
                        etaDisplayMode = Shared.etaDisplayMode
                    },
                    icon = when (etaDisplayMode) {
                        ETADisplayMode.COUNTDOWN -> Icons.Outlined.Timer
                        ETADisplayMode.CLOCK_TIME -> Icons.Outlined.Schedule
                        ETADisplayMode.CLOCK_TIME_WITH_COUNTDOWN -> Icons.Outlined.Update
                    },
                    text = (if (Shared.language == "en") "Clock Time Display Mode" else "時間顯示模式").asAnnotatedString(),
                    subText = when (etaDisplayMode) {
                        ETADisplayMode.COUNTDOWN -> (if (Shared.language == "en") "Countdown" else "倒數時間").asAnnotatedString()
                        ETADisplayMode.CLOCK_TIME -> (if (Shared.language == "en") "Clock Time" else "時鐘時間").asAnnotatedString()
                        ETADisplayMode.CLOCK_TIME_WITH_COUNTDOWN -> (if (Shared.language == "en") "Clock Time + Countdown" else "時鐘+倒數時間").asAnnotatedString()
                    }
                )
            }
            item {
                var disableMarquee by remember { mutableStateOf(Shared.disableMarquee) }
                SettingsButton(
                    instance = instance,
                    onClick = {
                        Registry.getInstance(instance).setDisableMarquee(!Shared.disableMarquee, instance)
                        disableMarquee = Shared.disableMarquee
                    },
                    icon = Icons.Outlined.TextRotationNone,
                    text = (if (Shared.language == "en") "Text Marquee Mode" else "文字顯示模式").asAnnotatedString(),
                    subText = if (disableMarquee) {
                        (if (Shared.language == "en") "Disable Text Marquee" else "靜止模式").asAnnotatedString()
                    } else {
                        (if (Shared.language == "en") "Enable Text Marquee" else "走馬燈模式").asAnnotatedString()
                    }
                )
            }
            item {
                var disableBoldDest by remember { mutableStateOf(Shared.disableBoldDest) }
                SettingsButton(
                    instance = instance,
                    onClick = {
                        Registry.getInstance(instance).setDisableBoldDest(!Shared.disableBoldDest, instance)
                        disableBoldDest = Shared.disableBoldDest
                    },
                    icon = Icons.Outlined.FormatBold,
                    text = (if (Shared.language == "en") "Destination Text Format" else "目的地文字格式").asAnnotatedString(),
                    subText = if (disableBoldDest) {
                        (if (Shared.language == "en") "Disable Bold" else "停用粗體").asAnnotatedString()
                    } else {
                        (if (Shared.language == "en") "Enable Bold" else "使用粗體").asAnnotatedString()
                    }
                )
            }
            item {
                if (phoneConnection == WearableConnectionState.CONNECTED) {
                    SettingsButton(
                        instance = instance,
                        icon = Icons.Outlined.Smartphone,
                        text = (if (Shared.language == "en") "Mobile Sync" else "手機同步").asAnnotatedString(),
                        subText = (if (Shared.language == "en") "Connected ✓" else "已連接 ✓").asAnnotatedString()
                    )
                } else {
                    SettingsButton(
                        instance = instance,
                        onClick = instance.handleWebpages(BASE_URL, false, haptic.common),
                        onLongClick = instance.wear.handleWebpages(BASE_URL, watchFirst = true, true, haptic.common),
                        icon = Icons.Outlined.Smartphone,
                        text = (if (Shared.language == "en") "Mobile App" else "手機應用程式").asAnnotatedString(),
                    )
                }
            }
            item {
                SettingsButton(
                    instance = instance,
                    onClick = instance.handleWebpages(BASE_URL, false, haptic.common),
                    onLongClick = instance.wear.handleWebpages(BASE_URL, watchFirst = true, true, haptic.common),
                    icon = Icons.Outlined.Share,
                    text = (if (Shared.language == "en") "Share App" else "分享應用程式").asAnnotatedString()
                )
            }
            item {
                SettingsButton(
                    instance = instance,
                    onClick = instance.handleWebpages("https://data.hkbuseta.com/PRIVACY_POLICY.html", false, haptic.common),
                    onLongClick = instance.wear.handleWebpages("https://data.hkbuseta.com/PRIVACY_POLICY.html", watchFirst = true, true, haptic.common),
                    icon = Icons.Outlined.Fingerprint,
                    text = (if (Shared.language == "en") "Privacy Policy" else "隱私權聲明").asAnnotatedString()
                )
            }
            item {
                SettingsButton(
                    instance = instance,
                    icon = Icons.Outlined.Watch,
                    text = (if (Shared.language == "en") "App Platform" else "應用程式平台").asAnnotatedString(),
                    subText = "WearOS".asAnnotatedString()
                )
            }
            item {
                SettingsButton(
                    instance = instance,
                    onClick = instance.handleWebpages("https://play.google.com/store/apps/details?id=com.loohp.hkbuseta", false, haptic.common),
                    onLongClick = instance.wear.handleWebpages("https://loohpjames.com", true, haptic.common),
                    icon = R.mipmap.icon_circle,
                    text = "${if (Shared.language == "en") "HK Bus ETA" else "香港巴士到站預報"} v${instance.versionName} (${instance.versionCode})".asAnnotatedString(),
                    subText = "@LoohpJames".asAnnotatedString()
                )
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            verticalArrangement = Arrangement.Top
        ) {
            WearOSShared.MainTime(scroll)
        }
    }
}

@Composable
fun SettingsButton(
    instance: AppActiveContext,
    onClick: () -> Unit = { /* do nothing */ },
    onLongClick: () -> Unit = { /* do nothing */ },
    icon: ImageVector,
    text: AnnotatedString,
    subText: AnnotatedString? = null
) {
    SettingsButton(
        instance = instance,
        onClick = onClick,
        onLongClick = onLongClick,
        icon = rememberVectorPainter(icon),
        text = text,
        subText = subText
    )
}

@Composable
fun SettingsButton(
    instance: AppActiveContext,
    onClick: () -> Unit = { /* do nothing */ },
    onLongClick: () -> Unit = { /* do nothing */ },
    icon: Int,
    text: AnnotatedString,
    subText: AnnotatedString? = null
) {
    SettingsButton(
        instance = instance,
        onClick = onClick,
        onLongClick = onLongClick,
        icon = painterResource(icon),
        text = text,
        subText = subText
    )
}

@Composable
fun SettingsButton(
    instance: AppActiveContext,
    onClick: () -> Unit = { /* do nothing */ },
    onLongClick: () -> Unit = { /* do nothing */ },
    icon: Painter,
    text: AnnotatedString,
    subText: AnnotatedString? = null
) {
    AdvanceButton(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp),
        onClick = onClick,
        onLongClick = onLongClick,
        shape = RoundedCornerShape(25.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color(0xFFFFFF00),
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(5.dp, 5.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box (
                modifier = Modifier
                    .padding(5.dp, 5.dp)
                    .width(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                    .height(30.scaledSize(instance).sp.clamp(max = 30.dp).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3D3D3D))
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                if (icon is VectorPainter) {
                    Icon(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(17F.scaledSize(instance).sp.dp),
                        painter = icon,
                        tint = Color.White,
                        contentDescription = text.text
                    )
                } else {
                    Image(
                        modifier = Modifier
                            .padding(3.dp, 3.dp)
                            .size(17F.scaledSize(instance).sp.dp),
                        painter = icon,
                        contentDescription = text.text
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1F)
                    .padding(end = 5.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 16F.scaledSize(instance).sp,
                    textAlign = TextAlign.Start,
                    color = Color.White,
                    text = text
                )
                if (subText != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 12F.scaledSize(instance).sp,
                        textAlign = TextAlign.Start,
                        color = Color.White,
                        text = subText
                    )
                }
            }
        }
    }
}