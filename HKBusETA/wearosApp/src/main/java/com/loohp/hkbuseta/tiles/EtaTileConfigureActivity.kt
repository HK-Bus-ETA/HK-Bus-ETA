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

package com.loohp.hkbuseta.tiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.FavouriteRouteGroup
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.FavouriteStopMode
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.getByName
import com.loohp.hkbuseta.common.objects.getDisplayName
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getFavouriteRouteStop
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.withEn
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Tiles
import com.loohp.hkbuseta.common.utils.indexOf
import com.loohp.hkbuseta.compose.AdvanceButton
import com.loohp.hkbuseta.compose.DrawPhaseColorText
import com.loohp.hkbuseta.compose.fullPageVerticalLazyScrollbar
import com.loohp.hkbuseta.compose.rotaryScroll
import com.loohp.hkbuseta.compose.userMarquee
import com.loohp.hkbuseta.compose.userMarqueeMaxLines
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.dpToPixels
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.getOperatorColor
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.util.concurrent.TimeUnit


enum class SelectMode(val selected: Boolean) {

    PRIMARY(true), SECONDARY(true), NONE(false);

}

@Stable
class EtaTileConfigureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WearOSShared.setDefaultExceptionHandler(this)
        WearOSShared.scheduleBackgroundUpdateService(this)
        Shared.setIsWearOS()
        Shared.provideBackgroundUpdateScheduler { c, t -> WearOSShared.scheduleBackgroundUpdateService(c.context, t) }
        Tiles.providePlatformUpdate { EtaTileServiceCommon.requestTileUpdate() }
        WearOSShared.registryNotificationChannel(this)

        val tileId = intent.extras!!.getInt("com.google.android.clockwork.EXTRA_PROVIDER_CONFIG_TILE_ID")
        val registry = Registry.getInstanceNoUpdateCheck(appContext)
        while (registry.state.value.isProcessing) {
            TimeUnit.MILLISECONDS.sleep(100)
        }

        setContent {
            SelectElements(tileId, appContext)
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

}

@OptIn(ExperimentalFoundationApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun SelectElements(tileId: Int, instance: AppActiveContext) {
    HKBusETATheme {
        val focusRequester = rememberActiveFocusRequester()
        val state = rememberLazyListState()

        val favouriteRouteStops by Shared.favoriteRouteStops.collectAsStateWithLifecycle()
        val selectedGroupState = remember { mutableStateOf(favouriteRouteStops.first().name) }
        val routeStops by remember(favouriteRouteStops, selectedGroupState) { derivedStateOf { favouriteRouteStops.getByName(selectedGroupState.value)!!.favouriteRouteStops } }

        val selectStates = remember { mutableStateListOf<Int>().also { it.addAll(Tiles.getEtaTileConfiguration(tileId)) } }

        LaunchedEffect (favouriteRouteStops) {
            selectStates.retainAll { favouriteRouteStops.any { (_, r) -> r.any { s -> s.favouriteId == it } } }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .fullPageVerticalLazyScrollbar(
                    state = state,
                    context = instance
                )
                .rotaryScroll(state, focusRequester),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = state
        ) {
            item {
                Spacer(modifier = Modifier.size(20.scaledSize(instance).dp))
            }
            item {
                SelectTitle(instance)
                Spacer(modifier = Modifier.size(5.scaledSize(instance).dp))
                SelectDescription(instance)
            }
            stickyHeader {
                Column (
                    modifier = Modifier.background(Brush.verticalGradient(
                        0F to Color(0xFF000000),
                        1F to Color(0x00000000),
                        startY = 45.scaledSize(instance).dpToPixels(instance)
                    ))
                ) {
                    Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                    ConfirmButton(tileId, selectStates, instance)
                    Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                }
            }
            item {
                SwitchGroupButton(favouriteRouteStops.toImmutableList(), selectedGroupState, instance)
                Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
            }
            if (routeStops.isNotEmpty()) {
                itemsIndexed(routeStops, key = { _, r -> r.favouriteId }) { index, routeStop ->
                    Column (
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        SelectButton(index + 1, routeStop, selectStates, instance)
                        Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                    }
                }
            }
            if (favouriteRouteStops.all { it.favouriteRouteStops.isEmpty() }) {
                item {
                    NoFavText(instance)
                    Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
                }
            }
            item {
                Spacer(modifier = Modifier.size(35.scaledSize(instance).dp))
            }
        }
    }
}

@Composable
fun ConfirmButton(tileId: Int, selectStates: SnapshotStateList<Int>, instance: AppActiveContext) {
    val enabled = selectStates.isNotEmpty()
    Button(
        onClick = {
            Registry.getInstanceNoUpdateCheck(instance).setEtaTileConfiguration(tileId, selectStates, instance)
            instance.finish()
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(0.8F)
            .height(35.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = if (enabled) Color(0xFF62FF00) else Color(0xFF444444)
        ),
        enabled = enabled,
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(0.9F),
                textAlign = TextAlign.Center,
                color = if (enabled) Color(0xFF62FF00) else Color(0xFF444444),
                fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                text = if (Shared.language == "en") "Confirm Selection" else "確認選擇"
            )
        }
    )
}

@Composable
fun SwitchGroupButton(groups: ImmutableList<FavouriteRouteGroup>, selectedState: MutableState<BilingualText>, instance: AppActiveContext) {
    Button(
        onClick = {
            val current = selectedState.value
            val index = groups.indexOf { it.name == current }
            selectedState.value = (if (index + 1 >= groups.size) groups.first() else groups[index + 1]).name
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .fillMaxWidth(0.8F)
            .height(35.scaledSize(instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color.White
        ),
        content = {
            Text(
                modifier = Modifier
                    .fillMaxWidth(0.9F)
                    .userMarquee(),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                text = selectedState.value[Shared.language]
            )
        }
    )
}

@Composable
fun SelectTitle(instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 17F.scaledSize(instance).sp.clamp(max = 17.dp),
        text = if (Shared.language == "en") "Select Routes" else "請選擇路線"
    )
}

@Composable
fun SelectDescription(instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 11F.scaledSize(instance).sp.clamp(max = 11.dp),
        text = if (Shared.language == "en") "Selected Favourite Routes will display in the Tile" else "所選最喜愛路線將顯示在資訊方塊中"
    )
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 11F.scaledSize(instance).sp.clamp(max = 11.dp),
        text = if (Shared.language == "en") "Multiple routes may be selected if their respective stop is close by" else "可選多條巴士站相近的路線"
    )
}

@Composable
fun NoFavText(instance: AppActiveContext) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = 17F.scaledSize(instance).sp.clamp(max = 17.dp),
        text = if (Shared.language == "en") "No favourite routes" else "沒有最喜愛路線"
    )
}

@Composable
fun SelectButton(numIndex: Int, favouriteRouteStop: FavouriteRouteStop, selectStates: SnapshotStateList<Int>, instance: AppActiveContext) {
    val haptic = LocalHapticFeedback.current
    val favoriteId by remember(favouriteRouteStop) { derivedStateOf { favouriteRouteStop.favouriteId } }

    val selectState by remember(favoriteId, selectStates) { derivedStateOf {
        if (selectStates.isNotEmpty() && selectStates[0] == favoriteId) {
            SelectMode.PRIMARY
        } else if (selectStates.contains(favoriteId)) {
            SelectMode.SECONDARY
        } else {
            SelectMode.NONE
        }
    } }
    val enabled by remember(favoriteId, favouriteRouteStop, selectStates) { derivedStateOf {
        (selectStates.isNotEmpty() && selectStates[0] == favoriteId) || (selectStates.isEmpty() || Shared.favoriteRouteStops.value.getFavouriteRouteStop(selectStates[0])?.let {
            (it.favouriteStopMode.let { mode -> mode.isRequiresLocation && mode == favouriteRouteStop.favouriteStopMode } || it.stop.location.distance(favouriteRouteStop.stop.location) <= 0.3) && !(it.route.routeNumber == favouriteRouteStop.route.routeNumber && it.co == favouriteRouteStop.co)
        } == true)
    } }

    val backgroundColor by remember(selectState) { derivedStateOf { when (selectState) {
        SelectMode.PRIMARY -> Color(0xFF46633A)
        SelectMode.SECONDARY -> Color(0xFF63543A)
        SelectMode.NONE -> Color(0xFF1A1A1A)
    } } }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = TweenSpec(durationMillis = 250, easing = LinearEasing),
        label = ""
    )

    AdvanceButton(
        onClick = {
            if (selectState.selected) {
                selectStates.removeIf { it == favoriteId }
            } else {
                selectStates.add(favoriteId)
            }
        },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (selectState == SelectMode.PRIMARY) {
                selectStates.removeIf { it == favoriteId }
            } else {
                selectStates.removeIf { it == favoriteId }
                selectStates.add(0, favoriteId)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                TweenSpec(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            .padding(20.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = animatedBackgroundColor,
            contentColor = Color(0xFFFFFF00).adjustBrightness(if (enabled) 1.0F else 0.5F),
        ),
        shape = RoundedCornerShape(15.dp),
        enabled = enabled,
        content = {
            Row(
                modifier = Modifier.padding(5.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box (
                    modifier = Modifier
                        .padding(5.dp, 5.dp)
                        .width(35.scaledSize(instance).sp.clamp(max = 35.dp).dp)
                        .height(35.scaledSize(instance).sp.clamp(max = 35.dp).dp)
                        .clip(CircleShape)
                        .background(if (enabled) Color(0xFF3D3D3D) else Color(0xFF131313))
                        .drawWithContent {
                            if (selectState.selected) {
                                drawArc(
                                    startAngle = -90F,
                                    sweepAngle = 360F,
                                    useCenter = false,
                                    color = when (selectState) {
                                        SelectMode.PRIMARY -> Color(0xFF4CFF00)
                                        SelectMode.SECONDARY -> Color(0xFFFF8400)
                                        else -> Color(0x00FFFFFF)
                                    },
                                    topLeft = Offset.Zero + Offset(1.sp.toPx(), 1.sp.toPx()),
                                    size = Size(
                                        size.width - 2.sp.toPx(),
                                        size.height - 2.sp.toPx()
                                    ),
                                    alpha = 1F,
                                    style = Stroke(width = 2.sp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            drawContent()
                        }
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectState) {
                        SelectMode.PRIMARY, SelectMode.SECONDARY -> {
                            Icon(
                                modifier = Modifier.size(21.scaledSize(instance).dp),
                                imageVector = Icons.Filled.Check,
                                tint = (if (selectState == SelectMode.PRIMARY) Color(0xFF4CFF00) else Color(0xFFFF8400)).adjustBrightness(if (enabled) 1.0F else 0.5F),
                                contentDescription = if (Shared.language == "en") "Selected" else "已選擇"
                            )
                        }
                        SelectMode.NONE -> {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontSize = 17F.scaledSize(instance).sp,
                                color = Color(0xFFFFFF00).adjustBrightness(if (enabled) 1.0F else 0.5F),
                                text = numIndex.toString()
                            )
                        }
                    }
                }
                val index = favouriteRouteStop.index
                val stopName = if (favouriteRouteStop.favouriteStopMode == FavouriteStopMode.FIXED) {
                    favouriteRouteStop.stop.name
                } else {
                    if (selectStates.isNotEmpty() && (selectStates.size > 1 || selectState != SelectMode.PRIMARY) && Shared.favoriteRouteStops.value.getFavouriteRouteStop(selectStates[0])?.favouriteStopMode?.isRequiresLocation == true) {
                        "共同最近的任何站" withEn "Any Common Closest"
                    } else {
                        "最近的任何站" withEn "Any Closest"
                    }
                }
                val route = favouriteRouteStop.route
                val kmbCtbJoint = route.isKmbCtbJoint
                val co = favouriteRouteStop.co
                val routeNumber = route.routeNumber
                val gmbRegion = route.gmbRegion
                val stopId = favouriteRouteStop.stopId
                val destName = Registry.getInstanceNoUpdateCheck(instance).getStopSpecialDestinations(stopId, co, route, true)
                val rawColor = co.getColor(routeNumber, Color.White)
                val color by WearOSShared.rememberOperatorColor(rawColor, Operator.CTB.getOperatorColor(Color.White).takeIf { kmbCtbJoint })

                val operator = co.getDisplayName(routeNumber, kmbCtbJoint, gmbRegion, Shared.language)
                val mainText = operator.plus(" ").plus(co.getDisplayRouteNumber(routeNumber))
                val routeText = destName[Shared.language]
                val subText = (if (co.isTrain || favouriteRouteStop.favouriteStopMode == FavouriteStopMode.CLOSEST) "" else index.toString().plus(". ")).plus(stopName[Shared.language])
                Spacer(modifier = Modifier.size(5.dp))
                Column (
                    modifier = Modifier
                        .heightIn(
                            min = 60.scaledSize(instance).sp.clamp(max = 60.dp).dp
                        ),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    DrawPhaseColorText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .userMarquee(),
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        color = { color.adjustBrightness(if (enabled) 1.0F else 0.5F) },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16F.scaledSize(instance).sp.clamp(max = 16.dp),
                        maxLines = userMarqueeMaxLines(),
                        text = mainText
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .userMarquee(),
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colors.primary.adjustBrightness(if (enabled) 1.0F else 0.5F),
                        fontWeight = FontWeight.Normal,
                        fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                        maxLines = userMarqueeMaxLines(),
                        text = routeText
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .userMarquee()
                            .padding(0.dp, 0.dp, 0.dp, 3.dp),
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colors.primary.adjustBrightness(if (enabled) 1.0F else 0.5F),
                        fontSize = 11F.scaledSize(instance).sp.clamp(max = 11.dp),
                        maxLines = userMarqueeMaxLines(),
                        text = subText
                    )
                }
            }
        }
    )
}