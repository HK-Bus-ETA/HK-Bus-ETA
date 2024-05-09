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
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.appcontext.isDarkMode
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.ETADisplayMode
import com.loohp.hkbuseta.common.objects.Preferences
import com.loohp.hkbuseta.common.objects.Theme
import com.loohp.hkbuseta.common.objects.WearableConnectionState
import com.loohp.hkbuseta.common.shared.BASE_URL
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.common.utils.pad
import com.loohp.hkbuseta.common.utils.toLocalDateTime
import com.loohp.hkbuseta.compose.DarkMode
import com.loohp.hkbuseta.compose.Download
import com.loohp.hkbuseta.compose.Fingerprint
import com.loohp.hkbuseta.compose.LightMode
import com.loohp.hkbuseta.compose.Map
import com.loohp.hkbuseta.compose.MobileFriendly
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.Schedule
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.Share
import com.loohp.hkbuseta.compose.Smartphone
import com.loohp.hkbuseta.compose.TextRotationNone
import com.loohp.hkbuseta.compose.Timer
import com.loohp.hkbuseta.compose.Translate
import com.loohp.hkbuseta.compose.Update
import com.loohp.hkbuseta.compose.Upload
import com.loohp.hkbuseta.compose.Watch
import com.loohp.hkbuseta.compose.combinedClickable
import com.loohp.hkbuseta.compose.platformHorizontalDividerShadow
import com.loohp.hkbuseta.compose.platformShowDownloadAppBottomSheet
import com.loohp.hkbuseta.compose.platformTopBarColor
import com.loohp.hkbuseta.compose.shouldBeTintedForIcons
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource


fun relaunch(instance: AppActiveContext) {
    val intent = AppIntent(instance, AppScreen.MAIN)
    intent.addFlags(AppIntentFlag.NEW_TASK, AppIntentFlag.CLEAR_TASK, AppIntentFlag.NO_ANIMATION)
    intent.putExtra("relaunch", AppScreen.SETTINGS.name)
    instance.startActivity(intent)
    instance.finishAffinity()
}

expect suspend fun invalidateWatchCache(context: AppContext)

@Composable
expect fun rememberWearableConnected(context: AppContext): State<WearableConnectionState>

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SettingsInterface(instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    val scroll = rememberScrollState()
    val wearableConnection by rememberWearableConnected(instance)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier
                .height(30.dp)
                .fillMaxWidth()
                .platformHorizontalDividerShadow(5.dp)
                .background(platformTopBarColor)
                .padding(horizontal = 5.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithScrollbar(
                    state = scroll,
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                    scrollbarConfig = ScrollBarConfig(
                        indicatorThickness = 6.dp
                    )
                )
        ) {
            SettingsRow(
                instance = instance,
                onClick = {
                    Registry.getInstance(instance).setLanguage(if (Shared.language == "en") "zh" else "en", instance)
                    relaunch(instance)
                },
                icon = PlatformIcons.Filled.Translate,
                text = "切換語言 Switch Language".asAnnotatedString(),
                subText = (if (Shared.language == "en") "English/中文" else "中文/English").asAnnotatedString()
            )
            SettingsRow(
                instance = instance,
                onClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        Registry.invalidateCache(instance)
                        Registry.clearInstance()
                        invalidateWatchCache(instance)
                        delay(500)
                        relaunch(instance)
                    }
                },
                icon = PlatformIcons.Filled.Update,
                text = (if (Shared.language == "en") "Update Route Database" else "更新路線資料庫").asAnnotatedString(),
                subText = ("${if (Shared.language == "en") "Last updated" else "最近更新時間"}: ${Registry.getInstance(instance).getLastUpdatedTime()?.let { instance.formatDateTime(it.toLocalDateTime(), true) }?: if (Shared.language == "en") "Never" else "從未"}").asAnnotatedString()
            )
            SettingsRow(
                instance = instance,
                onClick = {
                    Registry.getInstance(instance).setTheme(Shared.theme.next, instance)
                    relaunch(instance)
                },
                icon = if (Shared.theme.isDarkMode) PlatformIcons.Filled.DarkMode else PlatformIcons.Outlined.LightMode,
                text = (if (Shared.language == "en") "Theme" else "主題").asAnnotatedString(),
                subText = when (Shared.theme) {
                    Theme.LIGHT -> (if (Shared.language == "en") "Light Mode" else "淺色模式").asAnnotatedString()
                    Theme.DARK -> (if (Shared.language == "en") "Dark Mode" else "深色模式").asAnnotatedString()
                    Theme.SYSTEM -> (if (Shared.language == "en") "System Default" else "系統預設").asAnnotatedString()
                }
            )
            var etaDisplayMode by remember { mutableStateOf(Shared.etaDisplayMode) }
            SettingsRow(
                instance = instance,
                onClick = {
                    Registry.getInstance(instance).setEtaDisplayMode(Shared.etaDisplayMode.next, instance)
                    etaDisplayMode = Shared.etaDisplayMode
                },
                icon = when (etaDisplayMode) {
                    ETADisplayMode.COUNTDOWN -> PlatformIcons.Outlined.Timer
                    ETADisplayMode.CLOCK_TIME -> PlatformIcons.Outlined.Schedule
                    ETADisplayMode.CLOCK_TIME_WITH_COUNTDOWN -> PlatformIcons.Outlined.Update
                },
                text = (if (Shared.language == "en") "Clock Time Display Mode" else "時間顯示模式").asAnnotatedString(),
                subText = when (etaDisplayMode) {
                    ETADisplayMode.COUNTDOWN -> (if (Shared.language == "en") "Countdown" else "倒數時間").asAnnotatedString()
                    ETADisplayMode.CLOCK_TIME -> (if (Shared.language == "en") "Clock Time" else "時鐘時間").asAnnotatedString()
                    ETADisplayMode.CLOCK_TIME_WITH_COUNTDOWN -> (if (Shared.language == "en") "Clock Time + Countdown" else "時鐘+倒數時間").asAnnotatedString()
                }
            )
            var disableMarquee by remember { mutableStateOf(Shared.disableMarquee) }
            SettingsRow(
                instance = instance,
                onClick = {
                    Registry.getInstance(instance).setDisableMarquee(!Shared.disableMarquee, instance)
                    disableMarquee = Shared.disableMarquee
                },
                icon = PlatformIcons.Outlined.TextRotationNone,
                text = (if (Shared.language == "en") "Text Marquee Mode" else "文字顯示模式").asAnnotatedString(),
                subText = if (disableMarquee) {
                    (if (Shared.language == "en") "Disable Text Marquee" else "靜止模式").asAnnotatedString()
                } else {
                    (if (Shared.language == "en") "Enable Text Marquee" else "走馬燈模式").asAnnotatedString()
                }
            )
            var showRouteMap by remember { mutableStateOf(Shared.showRouteMap) }
            SettingsRow(
                instance = instance,
                onClick = {
                    Registry.getInstance(instance).setShowRouteMap(!Shared.showRouteMap, instance)
                    showRouteMap = Shared.showRouteMap
                },
                icon = PlatformIcons.Outlined.Map,
                text = (if (Shared.language == "en") "Show/Hidden Route Map" else "顯示/隱藏路線地圖").asAnnotatedString(),
                subText = if (showRouteMap) {
                    (if (Shared.language == "en") "Showing Route Map" else "顯示路線地圖").asAnnotatedString()
                } else {
                    (if (Shared.language == "en") "Hidden Route Map" else "隱藏路線地圖").asAnnotatedString()
                }
            )
            SettingsRow(
                instance = instance,
                onClick = {
                    val preferences = Registry.getInstance(instance).exportPreference().toString()
                    val time = currentLocalDateTime().run {
                        "${year.pad(4)}${monthNumber.pad(2)}${dayOfMonth.pad(2)}_${hour.pad(2)}${minute.pad(2)}${second.pad(2)}"
                    }
                    val fileName = if (Shared.language == "en") {
                        "HKBusETA_Preferences_${time}.json"
                    } else {
                        "香港巴士到站預報_個人喜好設定_${time}.json"
                    }
                    instance.compose.writeFileToFileChooser("application/json", fileName, preferences) {
                        instance.showToastText(if (Shared.language == "en") "Export Preferences Success" else "成功匯出個人喜好設定", ToastDuration.LONG)
                    }
                },
                icon = PlatformIcons.Outlined.Upload,
                text = (if (Shared.language == "en") "Export Preferences" else "匯出個人喜好設定").asAnnotatedString()
            )
            SettingsRow(
                instance = instance,
                onClick = {
                    instance.compose.readFileFromFileChooser("application/json") {
                        try {
                            val preferences = Preferences.deserialize(Json.decodeFromString(it)).apply {
                                lastSaved = currentTimeMillis()
                            }
                            CoroutineScope(dispatcherIO).launch {
                                Registry.getInstance(instance).syncPreference(instance, preferences, true)
                                relaunch(instance)
                                delay(1000)
                                instance.showToastText(if (Shared.language == "en") "Import Preferences Success" else "成功匯人個人喜好設定", ToastDuration.LONG)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            instance.showToastText(if (Shared.language == "en") "Import Preferences Failed" else "匯人個人喜好設定失敗", ToastDuration.LONG)
                        }
                    }
                },
                icon = PlatformIcons.Outlined.Download,
                text = (if (Shared.language == "en") "Import Preferences" else "匯入個人喜好設定").asAnnotatedString(),
            )
            platformShowDownloadAppBottomSheet?.also {
                SettingsRow(
                    instance = instance,
                    onClick = it,
                    icon = PlatformIcons.Outlined.MobileFriendly,
                    text = (if (Shared.language == "en") "Download the HK Bus ETA app" else "下載香港巴士到站預報應用程式").asAnnotatedString(),
                    subText = "Android / iOS / Apple Silicon Mac / WearOS / watchOS".asAnnotatedString()
                )
            }?: when (wearableConnection) {
                WearableConnectionState.CONNECTED -> SettingsRow(
                    instance = instance,
                    icon = PlatformIcons.Outlined.Watch,
                    text = (if (Shared.language == "en") "Smart Watch Sync" else "智能手錶同步").asAnnotatedString(),
                    subText = (if (Shared.language == "en") "Connected ✓" else "已連接 ✓").asAnnotatedString()
                )
                WearableConnectionState.PAIRED -> SettingsRow(
                    instance = instance,
                    icon = PlatformIcons.Outlined.Watch,
                    text = (if (Shared.language == "en") "Smart Watch Sync" else "智能手錶同步").asAnnotatedString(),
                    subText = (if (Shared.language == "en") "Paired (Watch App in the Background)" else "已配對 (智能手錶程式在後台)").asAnnotatedString()
                )
                else -> SettingsRow(
                    instance = instance,
                    onClick = instance.handleWebpages(BASE_URL, false, haptic.common),
                    icon = PlatformIcons.Outlined.Watch,
                    text = (if (Shared.language == "en") "Smart Watch App" else "智能手錶應用程式").asAnnotatedString(),
                    subText = "WatchOS / WearOS".asAnnotatedString()
                )
            }
            SettingsRow(
                instance = instance,
                onClick = instance.handleWebpages(BASE_URL, false, haptic.common),
                icon = PlatformIcons.Outlined.Share,
                text = (if (Shared.language == "en") "Share App" else "分享應用程式").asAnnotatedString()
            )
            SettingsRow(
                instance = instance,
                onClick = instance.handleWebpages("https://hkbus.app", false, haptic.common),
                icon = PlatformIcons.Outlined.Smartphone,
                text = (if (Shared.language == "en") "Special Thanks to hkbus.app" else "特別感謝 hkbus.app").asAnnotatedString()
            )
            SettingsRow(
                instance = instance,
                onClick = instance.handleWebpages("https://data.hkbuseta.com/PRIVACY_POLICY.html", false, haptic.common),
                icon = PlatformIcons.Outlined.Fingerprint,
                text = (if (Shared.language == "en") "Privacy Policy" else "隱私權聲明").asAnnotatedString()
            )
            SettingsRow(
                instance = instance,
                onClick = platformShowDownloadAppBottomSheet?: instance.handleWebpages(if (composePlatform.appleEnvironment) "https://apps.apple.com/app/id6475241017" else "https://play.google.com/store/apps/details?id=com.loohp.hkbuseta", false, haptic.common),
                onLongClick = instance.handleWebpages("https://loohpjames.com", true, haptic.common),
                icon = DrawableResource("icon_circle.png"),
                text = "${if (Shared.language == "en") "HK Bus ETA" else "香港巴士到站預報"} v${instance.versionName} (${instance.versionCode})".asAnnotatedString(),
                subText = "@LoohpJames".asAnnotatedString()
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SettingsRow(
    instance: AppActiveContext,
    onClick: () -> Unit = { /* do nothing */ },
    onLongClick: () -> Unit = { /* do nothing */ },
    icon: DrawableResource,
    text: AnnotatedString,
    subText: AnnotatedString? = null
) {
    SettingsRow(
        instance = instance,
        onClick = onClick,
        onLongClick = onLongClick,
        icon = painterResource(icon),
        text = text,
        subText = subText
    )
}

@Composable
fun SettingsRow(
    instance: AppActiveContext,
    onClick: () -> Unit = { /* do nothing */ },
    onLongClick: () -> Unit = { /* do nothing */ },
    icon: Painter,
    text: AnnotatedString,
    subText: AnnotatedString? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon.shouldBeTintedForIcons) {
            PlatformIcon(
                modifier = Modifier
                    .padding(horizontal = 15.dp, vertical = 10.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.adjustAlpha(0.5F))
                    .padding(10.dp),
                painter = icon,
                tint = MaterialTheme.colorScheme.outline,
                contentDescription = text.text
            )
        } else {
            Image(
                modifier = Modifier
                    .padding(horizontal = 15.dp, vertical = 10.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.adjustAlpha(0.5F))
                    .padding(10.dp),
                painter = icon,
                contentDescription = text.text
            )
        }
        Column(
            modifier = Modifier.weight(1F),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            PlatformText(
                modifier = Modifier
                    .heightIn(min = 22.5F.sp.dp)
                    .fillMaxWidth(),
                fontSize = 19F.sp,
                lineHeight = 22.5F.sp,
                textAlign = TextAlign.Start,
                text = text
            )
            if (subText != null) {
                PlatformText(
                    modifier = Modifier
                        .heightIn(min = 17.5F.sp.dp)
                        .fillMaxWidth(),
                    fontSize = 14F.sp,
                    lineHeight = 17.5F.sp,
                    textAlign = TextAlign.Start,
                    text = subText
                )
            }
        }
    }
    HorizontalDivider()
}