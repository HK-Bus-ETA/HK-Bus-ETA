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

package com.loohp.hkbuseta.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.common
import com.loohp.hkbuseta.appcontext.composePlatform
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.AppAlert
import com.loohp.hkbuseta.common.objects.isNotBlank
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.interpolateColor
import com.loohp.hkbuseta.compose.ImmediateEffect
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.applyIfNotNull
import com.loohp.hkbuseta.compose.clickable
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.compose.platformPrimaryContainerColor
import com.loohp.hkbuseta.compose.platformSurfaceContainerColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

object ComposeShared {

    private val appAlertsState: MutableStateFlow<AppAlert?> = MutableStateFlow(null)

    @Composable
    fun rememberAppAlert(context: AppActiveContext): State<AppAlert?> {
        LaunchedEffect (Unit) {
            while (true) {
                appAlertsState.value = Registry.getInstance(context).getAppAlerts().await()
                delay(30000)
            }
        }
        return appAlertsState.collectAsStateMultiplatform()
    }

    @Composable
    fun AnimatedVisibilityColumnAppAlert(context: AppActiveContext, appAlert: AppAlert?, visible: Boolean = true) {
        val haptics = LocalHapticFeedback.current
        var nonNullAppAlert by remember { mutableStateOf(AppAlert.EMPTY) }

        ImmediateEffect (appAlert) {
            if (appAlert != null) {
                nonNullAppAlert = appAlert
            }
        }

        AnimatedVisibility(
            visible = visible && appAlert?.content?.isNotBlank() == true,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(durationMillis = 300)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .applyIfNotNull(appAlert?.url?.takeIf { it.isNotBlank() }) { clickable(
                        onClick = context.handleWebpages(it, false, haptics.common),
                        role = Role.Button
                    ) }
                    .background(if (composePlatform.applePlatform) platformPrimaryContainerColor else platformSurfaceContainerColor)
                    .padding(10.dp)
            ) {
                PlatformText(
                    fontSize = 16.sp,
                    lineHeight = 1.1F.em,
                    text = nonNullAppAlert.content?.get(Shared.language)?: ""
                )
            }
        }
    }

    @Composable
    fun rememberOperatorColor(primaryColor: Color, secondaryColor: Color? = null): State<Color> {
        return if (secondaryColor == null) {
            remember(primaryColor) { mutableStateOf(primaryColor) }
        } else {
            val fraction by Shared.jointOperatedColorFractionState.collectAsStateMultiplatform()
            remember(primaryColor, secondaryColor) { derivedStateOf { Color(interpolateColor(primaryColor.toArgb().toLong(), secondaryColor.toArgb().toLong(), fraction)) } }
        }
    }

}