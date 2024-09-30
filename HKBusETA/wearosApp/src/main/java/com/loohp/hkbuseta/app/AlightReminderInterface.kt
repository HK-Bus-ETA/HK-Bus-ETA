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

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.AlightReminderSyncForegroundService
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.Big
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.append
import com.loohp.hkbuseta.utils.checkNotificationPermission
import com.loohp.hkbuseta.utils.scaledSize


@Composable
fun AlightReminderInterface(instance: AppActiveContext, isAmbient: Boolean) {
    val optService by WearOSShared.remoteAlightReminderService.collectAsStateWithLifecycle()
    val alightReminderService by remember { derivedStateOf { optService.value } }

    LaunchedEffect (alightReminderService) {
        if (alightReminderService == null) {
            instance.finish()
        } else {
            checkNotificationPermission(instance) {
                if (it) {
                    instance.context.startForegroundService(Intent(instance.context, AlightReminderSyncForegroundService::class.java))
                }
            }
        }
    }

    HKBusETATheme {
        alightReminderService?.let { service ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2F.scaledSize(instance).dp, Alignment.CenterVertically)
            ) {
                Spacer(modifier = Modifier.size(10F.scaledSize(instance).dp))
                AutoResizeText(
                    modifier = Modifier.fillMaxWidth(0.65F),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSizeRange = FontSizeRange(max = 14F.scaledSize(instance).sp),
                    maxLines = 2,
                    text = service.titleLeading
                )
                AutoResizeText(
                    modifier = Modifier.fillMaxWidth(0.85F),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(max = 11F.scaledSize(instance).sp),
                    maxLines = 2,
                    text = service.titleTrailing
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSize = 20F.scaledSize(instance).sp,
                    text = if (Shared.language == "en") buildAnnotatedString {
                        append(service.stopsRemaining.toString(), SpanStyle(fontSize = TextUnit.Big, fontWeight = FontWeight.Bold))
                        append(" stops left")
                    } else buildAnnotatedString {
                        append("剩餘 ")
                        append(service.stopsRemaining.toString(), SpanStyle(fontSize = TextUnit.Big, fontWeight = FontWeight.Bold))
                        append(" 個站")
                    }
                )
                AutoResizeText(
                    modifier = Modifier.fillMaxWidth(0.85F),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    fontSizeRange = FontSizeRange(max = 11F.scaledSize(instance).sp),
                    maxLines = 2,
                    text = service.content
                )
                if (!isAmbient) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(15.scaledSize(instance).dp, Alignment.CenterHorizontally)
                    ) {
                        val haptic = LocalHapticFeedback.current
                        Button(
                            modifier = Modifier
                                .width(50.scaledSize(instance).dp)
                                .height(50.scaledSize(instance).dp),
                            onClick = instance.handleWebpages(service.url, false, haptic.common),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = MaterialTheme.colors.primary
                            ),
                            content = {
                                Icon(
                                    modifier = Modifier.size(35F.scaledSize(instance).dp),
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = if (Shared.language == "en") "Open on Mobile" else "在手機上開啟",
                                    tint = Color.Green,
                                )
                            }
                        )
                        Button(
                            modifier = Modifier
                                .width(50.scaledSize(instance).dp)
                                .height(50.scaledSize(instance).dp),
                            onClick = {
                                RemoteActivityUtils.dataToPhone(instance.context, Shared.TERMINATE_ALIGHT_REMINDER_ID, null)
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = MaterialTheme.colors.primary
                            ),
                            content = {
                                Icon(
                                    modifier = Modifier.size(35F.scaledSize(instance).dp),
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = if (Shared.language == "en") "Terminate Alight Reminder" else "關閉落車提示",
                                    tint = Color.Red,
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}