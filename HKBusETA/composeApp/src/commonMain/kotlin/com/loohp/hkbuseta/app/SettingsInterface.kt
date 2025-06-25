/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.github.ajalt.colormath.model.RGB
import com.loohp.hkbuseta.appcontext.PlatformType
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
import com.loohp.hkbuseta.common.utils.decodeFromStringReadChannel
import com.loohp.hkbuseta.common.utils.pad
import com.loohp.hkbuseta.common.utils.toLocalDateTime
import com.loohp.hkbuseta.compose.Bolt
import com.loohp.hkbuseta.compose.Computer
import com.loohp.hkbuseta.compose.DarkMode
import com.loohp.hkbuseta.compose.DirectionsBus
import com.loohp.hkbuseta.compose.DismissRequestType
import com.loohp.hkbuseta.compose.Download
import com.loohp.hkbuseta.compose.Fingerprint
import com.loohp.hkbuseta.compose.FormatBold
import com.loohp.hkbuseta.compose.LightMode
import com.loohp.hkbuseta.compose.Map
import com.loohp.hkbuseta.compose.MobileFriendly
import com.loohp.hkbuseta.compose.Palette
import com.loohp.hkbuseta.compose.PhotoLibrary
import com.loohp.hkbuseta.compose.PlatformAlertDialog
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformOutlinedTextField
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.Schedule
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.Settings
import com.loohp.hkbuseta.compose.Share
import com.loohp.hkbuseta.compose.Smartphone
import com.loohp.hkbuseta.compose.Tablet
import com.loohp.hkbuseta.compose.TextRotationNone
import com.loohp.hkbuseta.compose.Timer
import com.loohp.hkbuseta.compose.Translate
import com.loohp.hkbuseta.compose.Update
import com.loohp.hkbuseta.compose.Upload
import com.loohp.hkbuseta.compose.Watch
import com.loohp.hkbuseta.compose.colorpicker.ClassicColorPicker
import com.loohp.hkbuseta.compose.colorpicker.HsvColor
import com.loohp.hkbuseta.compose.combinedClickable
import com.loohp.hkbuseta.compose.platformShowDownloadAppBottomSheet
import com.loohp.hkbuseta.compose.shouldBeTintedForIcons
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.asAnnotatedString
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource


fun relaunch(instance: AppActiveContext, skipSplash: Boolean = true) {
    val intent = AppIntent(instance, AppScreen.MAIN)
    intent.addFlags(AppIntentFlag.NEW_TASK, AppIntentFlag.CLEAR_TASK, AppIntentFlag.NO_ANIMATION)
    intent.putExtra("relaunch", AppScreen.SETTINGS.name)
    if (skipSplash) {
        intent.putExtra("skipSplash", true)
    }
    instance.startActivity(intent)
    instance.finishAffinity()
}

expect suspend fun invalidateWatchCache(context: AppContext)

@Composable
expect fun rememberWearableConnected(context: AppContext): State<WearableConnectionState>

@Composable
fun SettingsInterface(instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    val scroll = rememberScrollState()
    val wearableConnection by rememberWearableConnected(instance)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlatformIcon(
                modifier = Modifier.size(26.dp),
                painter = PlatformIcons.Filled.Settings,
                contentDescription = if (Shared.language == "en") "Settings" else "設定"
            )
            PlatformText(
                fontSize = 22.sp,
                lineHeight = 1.1F.em,
                text = if (Shared.language == "en") "Settings" else "設定"
            )
        }
        HorizontalDivider()
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
                onClick = {
                    Registry.getInstance(instance).setLanguage(if (Shared.language == "en") "zh" else "en", instance)
                    relaunch(instance)
                },
                icon = PlatformIcons.Filled.Translate,
                text = "切換語言 Switch Language".asAnnotatedString(),
                subText = (if (Shared.language == "en") "English/中文" else "中文/English").asAnnotatedString()
            )
            SettingsRow(
                onClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        Registry.invalidateCache(instance)
                        Registry.clearInstance()
                        invalidateWatchCache(instance)
                        delay(500)
                        relaunch(instance, skipSplash = false)
                    }
                },
                icon = PlatformIcons.Filled.Update,
                text = (if (Shared.language == "en") "Update Route Database" else "更新路線資料庫").asAnnotatedString(),
                subText = ("${if (Shared.language == "en") "Last updated" else "最近更新時間"}: ${Registry.getInstance(instance).getLastUpdatedTime()?.let { instance.formatDateTime(it.toLocalDateTime(), true) }?: if (Shared.language == "en") "Never" else "從未"}").asAnnotatedString()
            )
            SettingsRow(
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
            val showingColorPickerState = remember { mutableStateOf(false) }
            var showingColorPicker by showingColorPickerState
            SettingsRow(
                onClick = { showingColorPicker = true },
                icon = PlatformIcons.Filled.Palette,
                text = (if (Shared.language == "en") "Color" else "顏色").asAnnotatedString(),
                subText = if (Shared.color == null) {
                    (if (Shared.language == "en") "Default Color" else "預設顏色").asAnnotatedString()
                } else {
                    (if (Shared.language == "en") "Custom Color" else "自定顏色").asAnnotatedString()
                }
            )
            if (showingColorPicker) {
                ThemeColorPicker(instance, showingColorPickerState)
            }
            var etaDisplayMode by remember { mutableStateOf(Shared.etaDisplayMode) }
            SettingsRow(
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
            var disableBoldDest by remember { mutableStateOf(Shared.disableBoldDest) }
            SettingsRow(
                onClick = {
                    Registry.getInstance(instance).setDisableBoldDest(!Shared.disableBoldDest, instance)
                    disableBoldDest = Shared.disableBoldDest
                },
                icon = PlatformIcons.Outlined.FormatBold,
                text = (if (Shared.language == "en") "Destination Text Format" else "目的地文字格式").asAnnotatedString(),
                subText = if (disableBoldDest) {
                    (if (Shared.language == "en") "Disable Bold" else "停用粗體").asAnnotatedString()
                } else {
                    (if (Shared.language == "en") "Enable Bold" else "使用粗體").asAnnotatedString()
                }
            )
            var showRouteMap by remember { mutableStateOf(Shared.showRouteMap) }
            SettingsRow(
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
            var disableNavBarQuickActions by remember { mutableStateOf(Shared.disableNavBarQuickActions) }
            SettingsRow(
                onClick = {
                    Registry.getInstance(instance).setDisableNavBarQuickActions(!Shared.disableNavBarQuickActions, instance)
                    disableNavBarQuickActions = Shared.disableNavBarQuickActions
                },
                icon = if (disableNavBarQuickActions) PlatformIcons.Outlined.Bolt else PlatformIcons.Filled.Bolt,
                text = (if (Shared.language == "en") "Bottom Nav Bar Quick Actions" else "底部導覽列快速操作").asAnnotatedString(),
                subText = if (disableNavBarQuickActions) {
                    (if (Shared.language == "en") "Disabled Navigation Bar Quick Actions" else "停用導覽列快速操作").asAnnotatedString()
                } else {
                    (if (Shared.language == "en") "Enabled Navigation Bar Quick Actions" else "使用導覽列快速操作").asAnnotatedString()
                }
            )
            var downloadSplash by remember { mutableStateOf(Shared.downloadSplash) }
            SettingsRow(
                onClick = {
                    Registry.getInstance(instance).setDownloadSplash(!Shared.downloadSplash, instance)
                    downloadSplash = Shared.downloadSplash
                },
                icon = PlatformIcons.Outlined.PhotoLibrary,
                text = (if (Shared.language == "en") "Splash Screen" else "啟動畫面").asAnnotatedString(),
                subText = if (downloadSplash) {
                    (if (Shared.language == "en") "Fancy Version (Shows various places in HK)" else "精美版 (展示香港不同地點)").asAnnotatedString()
                } else {
                    (if (Shared.language == "en") "Simple Version" else "簡單版").asAnnotatedString()
                }
            )
            SettingsRow(
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
                onClick = {
                    instance.compose.readFileFromFileChooser("application/json") {
                        try {
                            val preferences = Preferences.deserialize(Json.decodeFromStringReadChannel(it)).apply {
                                lastSaved = currentTimeMillis()
                            }
                            Registry.getInstance(instance).syncPreference(instance, preferences, true)
                            relaunch(instance)
                            delay(1000)
                            instance.showToastText(if (Shared.language == "en") "Import Preferences Success" else "成功匯人個人喜好設定", ToastDuration.LONG)
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
                    onClick = it,
                    icon = PlatformIcons.Outlined.MobileFriendly,
                    text = (if (Shared.language == "en") "Download the HK Bus ETA app" else "下載香港巴士到站預報應用程式").asAnnotatedString(),
                    subText = "Android / iOS / Apple Silicon Mac / WearOS / watchOS".asAnnotatedString()
                )
            }?: when (wearableConnection) {
                WearableConnectionState.CONNECTED -> SettingsRow(
                    icon = PlatformIcons.Outlined.Watch,
                    text = (if (Shared.language == "en") "Smart Watch Sync" else "智能手錶同步").asAnnotatedString(),
                    subText = (if (Shared.language == "en") "Connected ✓" else "已連接 ✓").asAnnotatedString()
                )
                WearableConnectionState.PAIRED -> SettingsRow(
                    icon = PlatformIcons.Outlined.Watch,
                    text = (if (Shared.language == "en") "Smart Watch Sync" else "智能手錶同步").asAnnotatedString(),
                    subText = (if (Shared.language == "en") "Paired (Watch App in the Background)" else "已配對 (智能手錶程式在後台)").asAnnotatedString()
                )
                else -> SettingsRow(
                    onClick = instance.handleWebpages(BASE_URL, false, haptic.common),
                    icon = PlatformIcons.Outlined.Watch,
                    text = (if (Shared.language == "en") "Smart Watch App" else "智能手錶應用程式").asAnnotatedString(),
                    subText = "WatchOS / WearOS".asAnnotatedString()
                )
            }
            SettingsRow(
                onClick = instance.handleWebpages(BASE_URL, false, haptic.common),
                icon = PlatformIcons.Outlined.Share,
                text = (if (Shared.language == "en") "Share App" else "分享應用程式").asAnnotatedString()
            )
            SettingsRow(
                onClick = instance.handleWebpages("https://hkbus.app", false, haptic.common),
                icon = PlatformIcons.Outlined.DirectionsBus,
                text = (if (Shared.language == "en") "Special Thanks to hkbus.app" else "特別感謝 hkbus.app").asAnnotatedString()
            )
            SettingsRow(
                onClick = instance.handleWebpages("https://data.hkbuseta.com/PRIVACY_POLICY.html", false, haptic.common),
                icon = PlatformIcons.Outlined.Fingerprint,
                text = (if (Shared.language == "en") "Privacy Policy" else "隱私權聲明").asAnnotatedString()
            )
            SettingsRow(
                icon = when (composePlatform.type) {
                    PlatformType.PHONE -> PlatformIcons.Outlined.Smartphone
                    PlatformType.TABLET -> PlatformIcons.Outlined.Tablet
                    PlatformType.COMPUTER -> PlatformIcons.Outlined.Computer
                },
                text = (if (Shared.language == "en") "App Platform" else "應用程式平台").asAnnotatedString(),
                subText = composePlatform.displayName.asAnnotatedString()
            )
            SettingsRow(
                onClick = platformShowDownloadAppBottomSheet?: instance.handleWebpages(if (composePlatform.appleEnvironment) "https://apps.apple.com/app/id6475241017" else "https://play.google.com/store/apps/details?id=com.loohp.hkbuseta", false, haptic.common),
                onLongClick = instance.handleWebpages("https://loohpjames.com", true, haptic.common),
                icon = DrawableResource("icon_circle.png"),
                text = "${if (Shared.language == "en") "HK Bus ETA" else "香港巴士到站預報"} v${instance.versionName} (${instance.versionCode})".asAnnotatedString(),
                subText = "@LoohpJames".asAnnotatedString()
            )
        }
    }
}

@Composable
fun SettingsRow(
    onClick: () -> Unit = { /* do nothing */ },
    onLongClick: () -> Unit = { /* do nothing */ },
    icon: DrawableResource,
    text: AnnotatedString,
    subText: AnnotatedString? = null
) {
    SettingsRow(
        onClick = onClick,
        onLongClick = onLongClick,
        icon = painterResource(icon),
        text = text,
        subText = subText
    )
}

@Composable
fun SettingsRow(
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

@Composable
fun ThemeColorPicker(instance: AppActiveContext, showingState: MutableState<Boolean>) {
    var showing by showingState
    val colorState = rememberSaveable(stateSaver = HsvColor.Saver) { mutableStateOf(HsvColor.from(Shared.color?.let { Color(it) }?: Color.Red)) }
    var color by colorState
    var hexInput by remember { mutableStateOf(TextFieldValue(text = color.toColor().toHexString())) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect (hexInput) {
        val hex = hexInput.text
        if (hex.matches("#[0-9A-F]{6}".toRegex())) {
            color = HsvColor.from(Color(RGB(hex = hexInput.text).toRGBInt().argb.toLong()))
            error = false
        } else {
            error = true
        }
    }
    LaunchedEffect (color) {
        hexInput = hexInput.copy(text = color.toColor().toHexString())
    }

    PlatformAlertDialog(
        icon = {
            PlatformIcon(
                painter = PlatformIcons.Filled.Palette,
                contentDescription = if (Shared.language == "en") "Pick Color" else "選擇顏色"
            )
        },
        title = {
            PlatformText(text = if (Shared.language == "en") "Pick Color" else "選擇顏色", textAlign = TextAlign.Center)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ClassicColorPicker(
                    modifier = Modifier
                        .weight(1F, fill = false)
                        .layout { measurable, constraints ->
                            val adjusted = constraints.copy(maxHeight = constraints.maxWidth.coerceAtMost(constraints.maxHeight))
                            val placeable = measurable.measure(adjusted)
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        },
                    colorPickerValueState = colorState,
                    showAlphaBar = false
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .requiredSize(50.dp)
                            .clip(CircleShape)
                            .background(color.toColor())
                            .border(0.5F.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    PlatformOutlinedTextField(
                        modifier = Modifier,
                        value = hexInput,
                        singleLine = true,
                        isError = error,
                        onValueChange = { hexInput = it.copy(text = it.text.uppercase()) }
                    )
                }
            }
        },
        dismissButton = {
            PlatformText(
                textAlign = TextAlign.Center,
                lineHeight = 1.1F.em,
                text = if (Shared.language == "en") "App Default" else "程式預設"
            )
        },
        onDismissRequest = { when (it) {
            DismissRequestType.BUTTON -> {
                Shared.color = null
                Registry.getInstance(instance).setColor(Shared.color, instance)
                showing = false
                relaunch(instance)
            }
            DismissRequestType.CLICK_OUTSIDE -> {
                showing = false
            }
        } },
        iosCloseButton = true,
        confirmButton = {
            PlatformText(
                text = if (Shared.language == "en") "Confirm" else "確認"
            )
        },
        onConfirm = {
            Shared.color = color.toColor().toArgb().toLong()
            Registry.getInstance(instance).setColor(Shared.color, instance)
            showing = false
            relaunch(instance)
        }
    )
}