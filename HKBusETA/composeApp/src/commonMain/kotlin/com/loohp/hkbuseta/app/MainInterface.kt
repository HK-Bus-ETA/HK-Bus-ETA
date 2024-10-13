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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.appcontext.compose
import com.loohp.hkbuseta.appcontext.isTopOfStack
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppIntentFlag
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.appcontext.AppShortcutIcon
import com.loohp.hkbuseta.common.appcontext.primaryThemeColor
import com.loohp.hkbuseta.common.objects.GMBRegion
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.SplashEntry
import com.loohp.hkbuseta.common.shared.BASE_URL
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Splash
import com.loohp.hkbuseta.common.shared.readImage
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.SmallSize
import com.loohp.hkbuseta.common.utils.buildFormattedString
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.compose.PlatformButton
import com.loohp.hkbuseta.compose.PlatformLinearProgressIndicator
import com.loohp.hkbuseta.compose.PlatformText
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.utils.DrawableResource
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.toImageBitmap
import io.ktor.util.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource


@Composable
fun MainLoading(instance: AppActiveContext, stopId: String?, co: Operator?, index: Int?, stop: ImmutableState<Any?>, route: ImmutableState<Any?>, listStopRoute: ImmutableState<ByteArray?>, listStopScrollToStop: String?, listStopShowEta: Boolean?, listStopIsAlightReminder: Boolean?, queryKey: String?, queryRouteNumber: String?, queryBound: String?, queryCo: Operator?, queryDest: String?, queryGMBRegion: GMBRegion?, queryStop: String?, queryStopIndex: Int, queryStopDirectLaunch: Boolean) {
    val state by remember { Registry.getInstance(instance).also {
        if (!it.state.value.isProcessing && currentTimeMillis() - it.lastUpdateCheck > 30000) it.checkUpdate(instance, false)
    }.state }.collectAsStateMultiplatform()
    var splashScreenDone by remember { mutableStateOf(false) }

    val iconColor = MaterialTheme.colorScheme.primary.toArgb().toLong()
    primaryThemeColor = iconColor

    LaunchedEffect (Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            instance.setAppShortcut(
                id = "Search",
                shortLabel = if (Shared.language == "en") "Search Routes" else "搜尋路線",
                longLabel = if (Shared.language == "en") "Search Routes" else "搜尋路線",
                icon = AppShortcutIcon.SEARCH,
                tint = iconColor,
                rank = 1,
                url = "${BASE_URL}/search"
            )
            instance.setAppShortcut(
                id = "Favourite",
                shortLabel = if (Shared.language == "en") "Favourites" else "喜愛路線",
                longLabel = if (Shared.language == "en") "Favourites" else "喜愛路線",
                icon = AppShortcutIcon.STAR,
                tint = iconColor,
                rank = 2,
                url = "${BASE_URL}/fav"
            )
            instance.setAppShortcut(
                id = "Nearby",
                shortLabel = if (Shared.language == "en") "Nearby Routes" else "附近路線",
                longLabel = if (Shared.language == "en") "Nearby Routes" else "附近路線",
                icon = AppShortcutIcon.NEAR_ME,
                tint = iconColor,
                rank = 3,
                url = "${BASE_URL}/nearby"
            )
        }
    }

    LaunchedEffect (state, splashScreenDone) {
        when (state) {
            Registry.State.READY -> {
                if (splashScreenDone || instance.compose.data.containsKey("relaunch")) {
                    val appScreen = (instance.compose.data["relaunch"] as? String)?.let { AppScreen.valueOfNullable(it) }
                    val noAnimation = instance.compose.flags.contains(AppIntentFlag.NO_ANIMATION)
                    if (appScreen != null) {
                        delay(250)
                    }
                    Shared.handleLaunchOptions(instance, stopId, co, index, stop.value, route.value, listStopRoute.value, listStopScrollToStop, listStopShowEta, queryKey, queryRouteNumber, queryBound, queryCo, queryDest, queryGMBRegion, queryStop, queryStopIndex, queryStopDirectLaunch, appScreen, noAnimation, false) {
                        if (instance.isTopOfStack()) {
                            instance.startActivity(AppIntent(instance, AppScreen.TITLE))
                        }
                        instance.finishAffinity()
                    }
                }
            }
            Registry.State.ERROR -> {
                CoroutineScope(Dispatchers.IO).launch {
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

    Loading(instance, instance.compose.data["skipSplash"] == true) { splashScreenDone = true }
}

@Composable
fun Loading(instance: AppActiveContext, skipSplash: Boolean, onSplashScreenDone: () -> Unit) {
    Scaffold {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!skipSplash) {
                SplashScreen(instance, onSplashScreenDone)
                LoadingUpdatingElements(instance)
            }
        }
    }
}

@Composable
fun SplashScreen(instance: AppActiveContext, onSplashScreenDone: () -> Unit) {
    var splashData: Pair<SplashEntry, ImageBitmap>? by remember { mutableStateOf(null) }
    var isHeldDown by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect (Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            Splash.getRandomSplashEntry(instance)?.let {
                val data = it to it.readImage(instance).toByteArray().toImageBitmap()
                withContext(Dispatchers.Main) {
                    splashData = data
                }
            }
            delay(1000)
            withContext(Dispatchers.Main) {
                isDone = true
            }
        }
    }
    LaunchedEffect (isDone, isHeldDown) {
        if (isDone && !isHeldDown) {
            onSplashScreenDone.invoke()
        }
    }

    instance.compose.setStatusNavBarColor(
        status = Color.DarkGray,
        nav = Color.DarkGray
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isHeldDown = true
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    isHeldDown = false
                }
            },
        contentAlignment = Alignment.TopStart
    ) {
        AnimatedContent(
            targetState = splashData,
            transitionSpec = {
                fadeIn(animationSpec = tween(500))
                    .togetherWith(fadeOut(animationSpec = tween(500)))
            }
        ) {
            it?.also { (splashEntry, image) ->
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = image,
                    contentDescription = splashEntry.description.toString(),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop
                )
            }?: Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
            )
        }
        Column(
            modifier = Modifier
                .padding(30.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                modifier = Modifier.size(60.dp),
                painter = painterResource(DrawableResource("icon_max.png")),
                contentDescription = "HKBusETA"
            )
            AnimatedContent(
                targetState = splashData,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500))
                        .togetherWith(fadeOut(animationSpec = tween(500)))
                }
            ) {
                PlatformText(
                    text = (it?.first?.displayText?: buildFormattedString {
                        append("香港巴士到站預報")
                        appendLineBreak()
                        append("HK Bus ETA", SmallSize)
                    }).asContentAnnotatedString().annotatedString,
                    color = Color.White,
                    fontSize = 25.sp,
                    lineHeight = 1.1F.em
                )
            }
        }
    }
}

@Composable
fun LoadingUpdatingElements(instance: AppActiveContext) {
    val state by remember { Registry.getInstance(instance).state }.collectAsStateMultiplatform()
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
    val currentProgress by remember { Registry.getInstanceNoUpdateCheck(instance).updatePercentageState }.collectAsStateMultiplatform()
    val progressAnimation by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "LoadingProgressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlatformText(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 22F.sp,
            lineHeight = 1.1F.em,
            text = "更新數據中... Updating..."
        )
        PlatformLinearProgressIndicator(
            progress = { progressAnimation },
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(),
            color = Color(0xFFF9DE09),
            trackColor = Color(0xFF797979),
        )
    }
}

@Composable
fun LoadingElements(instance: AppActiveContext) {
    val currentState by remember { Registry.getInstanceNoUpdateCheck(instance).state }.collectAsStateMultiplatform()
    val checkingUpdate by remember { derivedStateOf { currentState == Registry.State.UPDATE_CHECKING } }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (checkingUpdate) {
            SkipChecksumButton(instance)
        }
        if (currentState != Registry.State.READY && currentState != Registry.State.ERROR) {
            PlatformText(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 22F.sp,
                lineHeight = 1.1F.em,
                text = "載入中... Loading..."
            )
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

    PlatformButton(
        onClick = {
            Registry.getInstanceNoUpdateCheck(instance).cancelCurrentChecksumTask()
        },
        modifier = Modifier
            .padding(bottom = 10.dp)
            .width(100.sp.dp)
            .height(60.sp.dp)
            .alpha(animatedAlpha),
        enabled = enableSkip,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.LightGray,
            contentColor = Color.White,
        ),
        content = {
            PlatformText(
                modifier = Modifier.fillMaxWidth(0.9F),
                textAlign = TextAlign.Center,
                fontSize = 22F.sp,
                text = if (Shared.language == "en") "Skip" else "略過"
            )
        }
    )
}