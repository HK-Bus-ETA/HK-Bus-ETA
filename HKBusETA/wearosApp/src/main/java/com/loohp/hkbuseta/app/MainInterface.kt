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

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.objects.GMBRegion
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun MainLoading(instance: AppActiveContext, stopId: String?, co: Operator?, index: Int?, stop: ImmutableState<Any?>, route: ImmutableState<Any?>, listStopRoute: ImmutableState<ByteArray?>, listStopScrollToStop: String?, listStopShowEta: Boolean?, queryKey: String?, queryRouteNumber: String?, queryBound: String?, queryCo: Operator?, queryDest: String?, queryGMBRegion: GMBRegion?, queryStop: String?, queryStopIndex: Int, queryStopDirectLaunch: Boolean, alightReminder: Boolean) {
    val state by remember { Registry.getInstance(instance).also {
        if (!it.state.value.isProcessing && System.currentTimeMillis() - it.lastUpdateCheck > 30000) it.checkUpdate(instance, false)
    }.state }.collectAsStateWithLifecycle()

    LaunchedEffect (Unit) {
        if (!alightReminder) {
            RemoteActivityUtils.dataToPhone(instance.context, Shared.REQUEST_ALIGHT_REMINDER_ID, null)
        }
    }
    LaunchedEffect (state) {
        when (state) {
            Registry.State.READY -> {
                if (alightReminder) {
                    instance.startActivity(AppIntent(instance, AppScreen.TITLE))
                    instance.startActivity(AppIntent(instance, AppScreen.ALIGHT_REMINDER_SERVICE))
                    instance.finishAffinity()
                } else {
                    val appScreen = instance.context.intent.getStringExtra("relaunch")?.let { AppScreen.valueOfNullable(it) }
                    val noAnimation = (instance.context.intent.flags and Intent.FLAG_ACTIVITY_NO_ANIMATION) == Intent.FLAG_ACTIVITY_NO_ANIMATION
                    if (appScreen != null) {
                        delay(250)
                    }
                    Shared.handleLaunchOptions(instance, stopId, co, index, stop.value, route.value, listStopRoute.value, listStopScrollToStop, listStopShowEta, queryKey, queryRouteNumber, queryBound, queryCo, queryDest, queryGMBRegion, queryStop, queryStopIndex, queryStopDirectLaunch, appScreen, noAnimation, false) {
                        WearOSShared.restoreCurrentScreenOrRun(instance, true) {
                            instance.startActivity(AppIntent(instance, AppScreen.TITLE))
                            instance.finishAffinity()
                        }
                    }
                }
            }
            Registry.State.ERROR -> {
                CoroutineScope(dispatcherIO).launch {
                    val intent = AppIntent(instance, AppScreen.FATAL_ERROR)
                    intent.putExtra("zh", "發生錯誤\n請檢查您的網絡連接")
                    intent.putExtra("en", "Fatal Error\nPlease check your internet connection")
                    instance.startActivity(intent)
                    instance.finish()
                }
            }
            else -> {}
        }
    }

    Loading(instance, instance.context.intent.getBooleanExtra("skipSplash", false))
}

@Composable
fun Loading(instance: AppActiveContext, skipSplash: Boolean) {
    HKBusETATheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top
        ) {
            WearOSShared.MainTime()
        }
        if (!skipSplash) {
            LoadingUpdatingElements(instance)
        }
    }
}

@Composable
fun LoadingUpdatingElements(instance: AppActiveContext) {
    val state by remember { Registry.getInstance(instance).state }.collectAsStateWithLifecycle()
    var wasUpdating by remember { mutableStateOf(state == Registry.State.UPDATING) }
    val updating by remember { derivedStateOf { wasUpdating || state == Registry.State.UPDATING } }

    LaunchedEffect (updating, state) {
        if (updating) {
            wasUpdating = true
        }
    }

    if (updating) {
        UpdatingElements(instance)
    } else {
        LoadingElements(instance)
    }
}

@Composable
fun UpdatingElements(instance: AppActiveContext) {
    val currentProgress by remember { Registry.getInstanceNoUpdateCheck(instance).updatePercentageState }.collectAsStateWithLifecycle()
    val progressAnimation by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "LoadingProgressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp, 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = 17F.scaledSize(instance).sp,
            text = "更新數據中..."
        )
        Spacer(modifier = Modifier.size(5.scaledSize(instance).dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = 17F.scaledSize(instance).sp,
            text = "Updating..."
        )
        Spacer(modifier = Modifier.size(30.scaledSize(instance).dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp, 0.dp),
            color = Color(0xFFF9DE09),
            trackColor = Color(0xFF797979),
            progress = { progressAnimation }
        )
    }
}

@Composable
fun LoadingElements(instance: AppActiveContext) {
    val currentState by remember { Registry.getInstanceNoUpdateCheck(instance).state }.collectAsStateWithLifecycle()
    val checkingUpdate by remember { derivedStateOf { currentState == Registry.State.UPDATE_CHECKING } }

    Box (
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp, 0.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    modifier = Modifier
                        .size(50.scaledSize(instance).dp)
                        .align(Alignment.Center),
                    painter = painterResource(R.mipmap.icon_full_smaller),
                    contentDescription = instance.getResourceString(R.string.app_name)
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 17F.scaledSize(instance).sp,
                text = "載入中..."
            )
            Spacer(modifier = Modifier.size(2.scaledSize(instance).dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 17F.scaledSize(instance).sp,
                text = "Loading..."
            )
        }
        if (checkingUpdate) {
            Box (
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(0.dp, 10.dp)
            ) {
                SkipChecksumButton(instance)
            }
        }
    }
}

@Composable
fun SkipChecksumButton(instance: AppActiveContext) {
    var enableSkip by remember { mutableStateOf(false) }

    val alpha by remember { derivedStateOf { if (enableSkip) 1F else 0F } }
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = TweenSpec(durationMillis = 400, easing = LinearEasing),
        label = ""
    )

    LaunchedEffect (Unit) {
        delay(3000)
        enableSkip = true
    }

    Button(
        onClick = {
            Registry.getInstanceNoUpdateCheck(instance).cancelCurrentChecksumTask()
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(55.scaledSize(instance).dp)
            .height(35.scaledSize(instance).dp)
            .alpha(animatedAlpha),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color(0xFFFFFFFF)
        ),
        enabled = enableSkip,
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(0.9F),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                text = if (Shared.language == "en") "Skip" else "略過"
            )
        }
    )
}