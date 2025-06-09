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

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.ComposePlatform
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.objects.RouteNotice
import com.loohp.hkbuseta.common.objects.RouteNoticeExternal
import com.loohp.hkbuseta.common.objects.RouteNoticeImportance
import com.loohp.hkbuseta.common.objects.RouteNoticeText
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.FormattedText
import com.loohp.hkbuseta.compose.CurrencyExchange
import com.loohp.hkbuseta.compose.OpenInNew
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformIcons
import com.loohp.hkbuseta.compose.PlatformModalBottomSheet
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.PriorityHigh
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.Sync
import com.loohp.hkbuseta.compose.applyIf
import com.loohp.hkbuseta.compose.clickable
import com.loohp.hkbuseta.compose.combinedClickable
import com.loohp.hkbuseta.compose.rememberPlatformModalBottomSheetState
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.copyToClipboard
import com.loohp.hkbuseta.utils.getOperatorColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeInterface(instance: AppActiveContext, notices: ImmutableList<RouteNotice>?, possibleBidirectionalSectionFare: Boolean) {
    if (notices == null) {
        EmptyBackgroundInterfaceProgress(
            instance = instance,
            icon = PlatformIcons.Filled.Sync,
            text = if (Shared.language == "en") "Loading" else "載入中"
        )
    } else {
        val importantNotices by remember(notices) { derivedStateOf { notices.asSequence().filter { it.importance != RouteNoticeImportance.NOT_IMPORTANT }.sortedBy { it.importance }.toList() } }
        val notImportantNotices by remember(notices) { derivedStateOf { notices.filter { it.importance == RouteNoticeImportance.NOT_IMPORTANT } } }
        var showNoticeText: FormattedText? by remember { mutableStateOf(null) }

        val scroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        val sheetState = rememberPlatformModalBottomSheetState(skipPartiallyExpanded = true)
        val haptic = LocalHapticFeedback.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .applyIf(composePlatform is ComposePlatform.AndroidPlatform) {
                    windowInsetsPadding(WindowInsets.navigationBars)
                }
                .verticalScrollWithScrollbar(
                    state = scroll,
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                    scrollbarConfig = ScrollBarConfig(
                        indicatorThickness = 4.dp
                    )
                ),
        ) {
            for (notice in importantNotices) {
                Row(
                    modifier = Modifier
                        .drawWithContent {
                            drawContent()
                            notice.co?.let {
                                val thickness = 5.dp.toPx()
                                drawLine(
                                    color = it.getOperatorColor(Color.Transparent),
                                    alpha = if (notice.importance == RouteNoticeImportance.IMPORTANT) 1F else 0.6F,
                                    start = Offset(thickness / 2F, 0F),
                                    end = Offset(thickness / 2F, size.height),
                                    strokeWidth = thickness
                                )
                            }
                        }
                        .combinedClickable(
                            onClick = when (notice) {
                                is RouteNoticeExternal -> if (notice.isPdf) ({
                                    instance.startActivity(AppIntent(instance, AppScreen.PDF).apply {
                                        putExtra("title", notice.title)
                                        putExtra("url", notice.url)
                                    })
                                }) else {
                                    instance.handleWebpages(notice.url, false, haptic.common)
                                }
                                is RouteNoticeText -> ({ showNoticeText = notice.display })
                            },
                            onLongClick = when (notice) {
                                is RouteNoticeExternal -> ({
                                    scope.launch {
                                        val result = copyToClipboard(notice.url)
                                        instance.showToastText(if (result) {
                                            if (Shared.language == "en") "Copied to clipboard" else "已複製到剪貼簿"
                                        } else {
                                            if (Shared.language == "en") "Failed to copy to clipboard" else "無法複製到剪貼簿"
                                        }, ToastDuration.SHORT)
                                    }
                                })
                                else -> ({ /* do nothing */})
                            }
                        )
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(60.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start)
                ) {
                    PlatformText(
                        modifier = Modifier.weight(1F),
                        fontSize = 17.sp,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        text = notice.title
                    )
                    if (possibleBidirectionalSectionFare && notice.possibleTwoWaySectionFareInfo) {
                        PlatformIcon(
                            modifier = Modifier.size(25.dp),
                            painter = PlatformIcons.Outlined.CurrencyExchange,
                            contentDescription = if (Shared.language == "en") "Contains Two-way Section Fare Info" else "雙向分段收費資訊"
                        )
                    } else if (notice.importance == RouteNoticeImportance.IMPORTANT) {
                        PlatformIcon(
                            modifier = Modifier.size(25.dp),
                            painter = PlatformIcons.Filled.PriorityHigh,
                            contentDescription = if (Shared.language == "en") "Important" else "重要"
                        )
                    }
                    if (notice is RouteNoticeExternal) {
                        PlatformIcon(
                            modifier = Modifier.size(25.dp),
                            painter = PlatformIcons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = if (Shared.language == "en") "External Link" else "外部連結"
                        )
                    }
                }
                HorizontalDivider()
            }
            if (importantNotices.isNotEmpty() && notImportantNotices.isNotEmpty()) {
                HorizontalDivider(thickness = 5.dp)
            }
            for (notice in notImportantNotices) {
                Box(
                    modifier = Modifier
                        .drawWithContent {
                            drawContent()
                            notice.co?.let {
                                val thickness = 5.dp.toPx()
                                drawLine(
                                    color = it.getOperatorColor(Color.Transparent),
                                    alpha = 0.6F,
                                    start = Offset(thickness / 2F, 0F),
                                    end = Offset(thickness / 2F, size.height),
                                    strokeWidth = thickness
                                )
                            }
                        }
                        .clickable(
                            onClick = when (notice) {
                                is RouteNoticeExternal -> instance.handleWebpages(notice.url, false, haptic.common)
                                is RouteNoticeText -> { { showNoticeText = notice.display } }
                            }
                        )
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(60.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    PlatformText(
                        fontSize = 17.sp,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        text = notice.title
                    )
                }
                HorizontalDivider()
            }
        }
        showNoticeText?.let {
            val sheetScroll = rememberScrollState()
            PlatformModalBottomSheet(
                onDismissRequest = { showNoticeText = null },
                sheetState = sheetState
            ) {
                Column (
                    modifier = Modifier
                        .fillMaxHeight(0.9F)
                        .verticalScrollWithScrollbar(
                            state = sheetScroll,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            scrollbarConfig = ScrollBarConfig(
                                indicatorThickness = 4.dp
                            )
                        )
                        .padding(30.dp)
                        .padding(bottom = 30.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    val text = it.asContentAnnotatedString(
                        onClickUrls = { url -> instance.handleWebpages(url, false, haptic.common).invoke() }
                    ).annotatedString
                    SelectionContainer {
                        PlatformText(
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 17.sp,
                            text = text
                        )
                    }
                }
            }
        }
    }
}