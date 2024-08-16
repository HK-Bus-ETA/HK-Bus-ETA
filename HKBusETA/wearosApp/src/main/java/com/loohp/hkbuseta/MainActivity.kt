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

package com.loohp.hkbuseta

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.app.MainLoading
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.external.extractShareLink
import com.loohp.hkbuseta.common.external.shareLaunch
import com.loohp.hkbuseta.common.objects.gmbRegion
import com.loohp.hkbuseta.common.objects.operator
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Tiles
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.dispatcherIO
import com.loohp.hkbuseta.common.utils.remove
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.tiles.EtaTileServiceCommon
import com.loohp.hkbuseta.utils.RemoteActivityUtils.Companion.hasPhoneApp
import com.loohp.hkbuseta.utils.optBoolean
import com.loohp.hkbuseta.utils.optInt
import com.loohp.hkbuseta.utils.optString
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@Stable
open class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WearOSShared.setDefaultExceptionHandler(this)
        WearOSShared.scheduleBackgroundUpdateService(this)
        Shared.setIsWearOS()
        Shared.provideBackgroundUpdateScheduler { c, t -> WearOSShared.scheduleBackgroundUpdateService(c.context, t) }
        Tiles.providePlatformUpdate { EtaTileServiceCommon.requestTileUpdate() }
        WearOSShared.registryNotificationChannel(this)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val stopId = intent.extras?.getString("stopId")
        val co = intent.extras?.getString("co")?.operator
        val index = intent.extras?.getInt("index")
        val stop = intent.extras?.get("stop")
        val route = intent.extras?.get("route")

        val listStopRoute = intent.extras?.getByteArray("stopRoute")
        val listStopScrollToStop = intent.extras?.getString("scrollToStop")
        val listStopShowEta = intent.extras?.getBoolean("showEta")

        val queryKey = intent.extras?.optString("k")
        val queryRouteNumber = intent.extras?.optString("r")
        val queryBound = intent.extras?.optString("b")
        val queryCo = intent.extras?.optString("c")?.operator
        val queryDest = intent.extras?.optString("d")
        val queryGMBRegion = intent.extras?.optString("g")?.gmbRegion
        val queryStop = intent.extras?.optString("s")
        val queryStopIndex = intent.extras?.optInt("si")?: 0
        val queryStopDirectLaunch = intent.extras?.optBoolean("sd") == true

        val alightReminder = intent.extras?.optBoolean("alightReminder") == true

        setContent {
            var watchDataOverwriteWarning by remember { mutableStateOf(runBlocking(dispatcherIO) { Registry.isNewInstall(appContext) }) }
            if (watchDataOverwriteWarning) {
                var requestSent by remember { mutableStateOf(false) }

                LaunchedEffect (Unit) {
                    watchDataOverwriteWarning = watchDataOverwriteWarning && hasPhoneApp(this@MainActivity).await()
                    while (true) {
                        delay(1000)
                        watchDataOverwriteWarning = Registry.isNewInstall(appContext)
                    }
                }
                LaunchedEffect (requestSent) {
                    if (!requestSent) {
                        while (true) {
                            requestSent = appContext.requestPreferencesIfPossible().await()
                            delay(1000)
                        }
                    }
                }

                Column (
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontSize = 17F.scaledSize(appContext).sp,
                        text = "與手機數據同步中..."
                    )
                    Spacer(modifier = Modifier.size(2.scaledSize(appContext).dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontSize = 17F.scaledSize(appContext).sp,
                        text = "Syncing with Phone..."
                    )
                }
            } else {
                MainLoading(appContext, stopId, co, index, stop.asImmutableState(), route.asImmutableState(), listStopRoute.asImmutableState(), listStopScrollToStop, listStopShowEta, queryKey, queryRouteNumber, queryBound, queryCo, queryDest, queryGMBRegion, queryStop, queryStopIndex, queryStopDirectLaunch, alightReminder)
            }
        }
        CoroutineScope(dispatcherIO).launch {
            intent.extractUrl()?.extractShareLink()?.apply {
                delay(500)
                shareLaunch(appContext, noAnimation = true, skipTitle = true)
            }
        }
    }
}

internal fun Intent.extractUrl(): String? {
    return when (action) {
        Intent.ACTION_SEND -> (if (type == "text/plain") getStringExtra(Intent.EXTRA_TEXT)?.remove("\n") else null)
            .apply { removeExtra(Intent.EXTRA_TEXT) }
        Intent.ACTION_VIEW -> data.toString()
            .apply { data = null }
        else -> null
    }
}