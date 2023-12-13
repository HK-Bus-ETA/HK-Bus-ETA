/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.appcontext.AppActiveContext
import com.loohp.hkbuseta.appcontext.AppIntent
import com.loohp.hkbuseta.appcontext.AppIntentFlag
import com.loohp.hkbuseta.appcontext.AppScreen
import com.loohp.hkbuseta.compose.ScrollBarConfig
import com.loohp.hkbuseta.compose.verticalScrollWithScrollbar
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun Message(instance: AppActiveContext, zh: String?, en: String?, exception: String?) {
    HKBusETATheme {
        LaunchedEffect (Unit) {
            Registry.clearInstance()
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 0.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = (if (zh == null) 14F else 16F).scaledSize(instance).dp.sp,
                color = MaterialTheme.colors.primary,
                text = zh ?: "發生錯誤"
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 0.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = (if (en == null) 14F else 16F).scaledSize(instance).dp.sp,
                color = MaterialTheme.colors.primary,
                text = en ?: "Fatal Error Occurred"
            )
            if (zh == null && en == null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 0.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 10F.scaledSize(instance).dp.sp,
                    color = MaterialTheme.colors.primary,
                    text = "巴士路線資料可能不完整\n點選重新載入刷新"
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 0.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 10F.scaledSize(instance).dp.sp,
                    color = MaterialTheme.colors.primary,
                    text = "Bus route data might be corrupted\nClick reload to refresh"
                )
            }
            if (exception != null) {
                val focusRequester = rememberActiveFocusRequester()
                val scroll = rememberScrollState()
                val scope = rememberCoroutineScope()
                val haptic = LocalHapticFeedback.current

                var scrollCounter by remember { mutableIntStateOf(0) }
                val scrollInProgress by remember { derivedStateOf { scroll.isScrollInProgress } }
                val scrollReachedEnd by remember { derivedStateOf { scroll.canScrollBackward != scroll.canScrollForward } }
                var scrollMoved by remember { mutableStateOf(false) }
                LaunchedEffect (scrollInProgress) {
                    if (scrollInProgress) {
                        scrollCounter++
                    }
                }
                LaunchedEffect (scrollCounter, scrollReachedEnd) {
                    delay(50)
                    if (scrollReachedEnd && scrollMoved) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    scrollMoved = true
                }

                Spacer(modifier = Modifier.size(2.scaledSize(instance).dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth(0.95F)
                        .fillMaxHeight(0.25F)
                        .verticalScrollWithScrollbar(
                            state = scroll,
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            scrollbarConfig = ScrollBarConfig(
                                indicatorThickness = 2.dp
                            )
                        )
                        .onRotaryScrollEvent {
                            scope.launch {
                                scroll.animateScrollBy(
                                    it.verticalScrollPixels / 2,
                                    TweenSpec(durationMillis = 500, easing = FastOutSlowInEasing)
                                )
                            }
                            true
                        }
                        .focusRequester(
                            focusRequester = focusRequester
                        )
                        .focusable()
                        .padding(20.dp, 0.dp),
                    textAlign = TextAlign.Left,
                    fontSize = 8F.dp.sp,
                    color = Color(0xFF7C7C7C),
                    text = exception
                )
            }
            Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier
                        .width(50.scaledSize(instance).dp)
                        .height(50.scaledSize(instance).dp),
                    onClick = {
                        Shared.invalidateCache(instance)
                        val intent = AppIntent(instance, AppScreen.MAIN)
                        intent.addFlags(AppIntentFlag.NEW_TASK, AppIntentFlag.CLEAR_TASK)
                        instance.startActivity(intent)
                        instance.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    content = {
                        Icon(
                            modifier = Modifier.size(35F.scaledSize(instance).dp),
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = if (Shared.language == "en") "Relaunch App" else "重新載入",
                            tint = Color.Yellow,
                        )
                    }
                )
                Spacer(modifier = Modifier.size(25.scaledSize(instance).dp))
                Button(
                    modifier = Modifier
                        .width(50.scaledSize(instance).dp)
                        .height(50.scaledSize(instance).dp),
                    onClick = {
                        Shared.invalidateCache(instance)
                        instance.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    content = {
                        Icon(
                            modifier = Modifier.size(35F.scaledSize(instance).dp),
                            imageVector = Icons.Filled.Close,
                            contentDescription = if (Shared.language == "en") "Exit App" else "退出應用程式",
                            tint = Color.Red,
                        )
                    }
                )
            }
        }
    }
}