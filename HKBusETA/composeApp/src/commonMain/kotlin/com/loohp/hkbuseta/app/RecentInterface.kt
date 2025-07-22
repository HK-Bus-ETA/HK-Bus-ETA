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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.ComposePlatform
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.RecentSortMode
import com.loohp.hkbuseta.common.objects.RouteListType
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.toStopIndexed
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.asImmutableList
import com.loohp.hkbuseta.compose.ArrowBack
import com.loohp.hkbuseta.compose.DeleteDialog
import com.loohp.hkbuseta.compose.DeleteForever
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformSwitch
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.plainTooltip
import com.loohp.hkbuseta.compose.platformHorizontalDividerShadow
import com.loohp.hkbuseta.compose.platformLargeShape
import com.loohp.hkbuseta.compose.platformLocalContentColor
import com.loohp.hkbuseta.compose.platformTopBarColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow


private val listedRoutes: MutableStateFlow<ImmutableList<StopIndexedRouteSearchResultEntry>> = MutableStateFlow(persistentListOf())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentInterface(instance: AppActiveContext, visible: Boolean = true) {
    var routes by listedRoutes.collectAsStateMultiplatform()
    var realVisible by remember { mutableStateOf(visible) }
    val recentRouteKeys by Shared.lastLookupRoutes.collectAsStateMultiplatform()
    var clearingHistory by remember { mutableStateOf(false) }
    var historyEnabled by remember { mutableStateOf(Shared.historyEnabled) }

    LaunchedEffect(visible, recentRouteKeys) {
        if (visible) {
            routes = Registry.getInstance(instance).findRoutes("", false, Shared.RECENT_ROUTE_FILTER).toStopIndexed(instance).asImmutableList()
        }
        realVisible = visible
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .platformHorizontalDividerShadow(5.dp)
                    .background(platformTopBarColor)
                    .applyIf(composePlatform is ComposePlatform.AndroidPlatform) {
                        this.statusBarsPadding()
                            .consumeWindowInsets(WindowInsets.statusBars)
                            .consumeWindowInsets(WindowInsets.navigationBars)
                    }
                    .height(50.dp)
                    .padding(horizontal = 5.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PlatformButton(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        onClick = { instance.finish() }
                    ) {
                        PlatformIcon(
                            modifier = Modifier.size(30.dp),
                            painter = PlatformIcons.AutoMirrored.Filled.ArrowBack,
                            tint = platformLocalContentColor,
                            contentDescription = if (Shared.language == "en") "Back" else "返回"
                        )
                    }
                    PlatformText(
                        fontSize = 21.sp,
                        lineHeight = 1.1F.em,
                        text = if (Shared.language == "en") "Recent History" else "歷史記錄"
                    )
                    PlatformSwitch(
                        checked = historyEnabled,
                        onCheckedChange = {
                            Registry.getInstance(instance).setHistoryEnabled(it, instance)
                            historyEnabled = Shared.historyEnabled
                        }
                    )
                }
                if (routes.isNotEmpty()) {
                    PlatformButton(
                        modifier = Modifier
                            .padding(end = 3.dp)
                            .size(50F.dp)
                            .clip(CircleShape)
                            .align(Alignment.CenterEnd)
                            .plainTooltip(if (Shared.language == "en") "Clear Recent History" else "清除歷史記錄"),
                        onClick = { clearingHistory = true },
                        contentPadding = PaddingValues(0.dp),
                        shape = platformLargeShape,
                        colors = ButtonDefaults.textButtonColors(),
                        content = {
                            PlatformIcon(
                                modifier = Modifier.size(23.dp),
                                painter = PlatformIcons.Filled.DeleteForever,
                                tint = Color.Red,
                                contentDescription = if (Shared.language == "en") "Clear Recent History" else "清除歷史記錄"
                            )
                        }
                    )
                }
            }
        },
        content = { padding ->
            Box(
                modifier = Modifier.padding(padding)
            ) {
                ListRoutesInterface(
                    instance = instance,
                    routes = routes,
                    checkSpecialDest = false,
                    listType = RouteListType.RECENT,
                    showEta = false,
                    recentSort = RecentSortMode.FORCED,
                    proximitySortOrigin = null,
                    showEmptyText = realVisible,
                    visible = realVisible
                )
            }
        }
    )

    if (clearingHistory) {
        DeleteDialog(
            icon = PlatformIcons.Filled.DeleteForever,
            title = "確認清除歷史記錄" withEn "Confirm Clear Recent History",
            text = "一經確認將不能復原" withEn "This action cannot be undone.",
            onDismissRequest = { clearingHistory = false },
            onConfirmation = {
                clearingHistory = false
                Registry.getInstance(instance).clearLastLookupRoutes(instance)
            }
        )
    }
}