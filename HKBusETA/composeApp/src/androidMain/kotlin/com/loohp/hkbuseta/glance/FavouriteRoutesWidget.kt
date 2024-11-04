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

package com.loohp.hkbuseta.glance

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDefaults
import co.touchlab.stately.collections.ConcurrentMutableMap
import com.loohp.hkbuseta.MainActivity
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.appcontext.nonActiveAppContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.ETADisplayMode
import com.loohp.hkbuseta.common.objects.KMBSubsidiary
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.SpecialRouteAlerts
import com.loohp.hkbuseta.common.objects.StopIndexedRouteSearchResultEntry
import com.loohp.hkbuseta.common.objects.bilingualOnlyToPrefix
import com.loohp.hkbuseta.common.objects.bilingualToPrefix
import com.loohp.hkbuseta.common.objects.calculateServiceTimeCategory
import com.loohp.hkbuseta.common.objects.endOfLineText
import com.loohp.hkbuseta.common.objects.getDeepLink
import com.loohp.hkbuseta.common.objects.getDisplayFormattedName
import com.loohp.hkbuseta.common.objects.getKMBSubsidiary
import com.loohp.hkbuseta.common.objects.getListDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.getSpecialRouteAlerts
import com.loohp.hkbuseta.common.objects.idBound
import com.loohp.hkbuseta.common.objects.isBus
import com.loohp.hkbuseta.common.objects.isPetBus
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.resolvedDest
import com.loohp.hkbuseta.common.objects.resolvedDestWithBranch
import com.loohp.hkbuseta.common.objects.shouldPrependTo
import com.loohp.hkbuseta.common.objects.toRouteSearchResult
import com.loohp.hkbuseta.common.objects.toStopIndexed
import com.loohp.hkbuseta.common.objects.uniqueKey
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Shared.getResolvedText
import com.loohp.hkbuseta.common.utils.ColorContentStyle
import com.loohp.hkbuseta.common.utils.ImmutableState
import com.loohp.hkbuseta.common.utils.ServiceTimeCategory
import com.loohp.hkbuseta.common.utils.asImmutableState
import com.loohp.hkbuseta.common.utils.createTimetable
import com.loohp.hkbuseta.common.utils.currentLocalDateTime
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.firstIsInstanceOrNull
import com.loohp.hkbuseta.common.utils.getServiceTimeCategory
import com.loohp.hkbuseta.common.utils.indexOf
import com.loohp.hkbuseta.common.utils.isNotNullAndNotEmpty
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform
import com.loohp.hkbuseta.utils.Small
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.spToDp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit


val groupNameZhKey = stringPreferencesKey("groupName")

private val etaUpdateScope: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(8)

class FavouriteRoutesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavouriteRoutesWidget
}

object FavouriteRoutesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val instance = context.nonActiveAppContext
        if (Registry.isNewInstall(instance)) {
            provideContent {
                GlanceWidgetTheme(context) {
                    FirstInstallWidgetContent()
                }
            }
        } else {
            val registry = Registry.getInstanceNoUpdateCheck(instance)
            while (registry.state.value.isProcessing) {
                delay(100)
            }
            provideContent {
                GlanceWidgetTheme(context) {
                    FavouriteRoutesWidgetContent(instance)
                }
            }
        }
    }

}

@Composable
private fun etaColor(context: Context): Color {
    return if (GlanceTheme.colors.isDark(context)) Color(0xFFAAC3D5) else Color(0xFF2582C4)
}

@Composable
private fun etaSecondColor(context: Context): Color {
    return if (GlanceTheme.colors.isDark(context)) Color(0xFFCCCCCC) else Color(0xFF444444)
}

@Composable
fun FirstInstallWidgetContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .clickable(actionStartActivity<MainActivity>())
            .appWidgetBackground()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Image(
                    modifier = GlanceModifier.size(64.dp),
                    contentDescription = "香港巴士到站預報 HK Bus ETA",
                    provider = ImageProvider(R.mipmap.icon_max)
                )
            }
            item {
                Text(
                    text = "香港巴士到站預報",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = 22F.sp
                    )
                )
            }
            item {
                Text(
                    text = "HK Bus ETA",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = 22F.sp
                    )
                )
            }
            item {
                Spacer(
                    modifier = GlanceModifier.size(5.dp)
                )
            }
            item {
                Text(
                    text = "請開啟應用程式",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = 22F.sp
                    )
                )
            }
            item {
                Text(
                    text = "Please open the app",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = 22F.sp
                    )
                )
            }
        }
    }
}

@Composable
fun FavouriteRoutesWidgetContent(instance: AppContext) {
    val groupNameZh = currentState(groupNameZhKey)
    val favouriteRouteStops by Shared.favoriteRouteStops.collectAsState()
    val group by remember(favouriteRouteStops, groupNameZh) { derivedStateOf { favouriteRouteStops.firstOrNull { it.name.zh == groupNameZh }?: favouriteRouteStops.first() } }
    var location: Coordinates? by remember { mutableStateOf(null) }
    val routeStops by remember(group) { derivedStateOf { group.favouriteRouteStops.toRouteSearchResult(instance, location).toStopIndexed(instance) } }
    val etaResults: MutableMap<String, Registry.ETAQueryResult> = remember { ConcurrentMutableMap() }
    val etaUpdateTimes: MutableMap<String, Long> = remember { ConcurrentMutableMap() }
    val lastUpdated by remember(etaUpdateTimes, etaResults) { derivedStateOf { instance.formatDateTime(currentLocalDateTime(), true) } }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect (Unit) {
        while (routeStops.size > etaResults.size) {
            delay(200)
        }
        loading = false
    }
    LaunchedEffect (Unit) {
        while (true) {
            location = getGPSLocation(instance).await()?.location
            delay(300000)
        }
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .appWidgetBackground()
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentDescription = if (Shared.language == "en") "HK Bus ETA" else "香港巴士到站預報",
                provider = ImageProvider(R.mipmap.icon_max)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                modifier = GlanceModifier
                    .padding(end = 3.dp)
                    .clickable(actionRunCallback<UpdateFavouriteRoutesCallback>()),
                style = TextDefaults.defaultTextStyle.copy(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 15.sp
                ),
                text = (if (Shared.language == "en") "Last Updated: " else "更新時間: ") + lastUpdated
            )
            if (loading) {
                CircularProgressIndicator(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(actionRunCallback<UpdateFavouriteRoutesCallback>()),
                    color = Color(0xFFF9DE09).forGlance()
                )
            } else {
                Image(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(actionRunCallback<UpdateFavouriteRoutesCallback>()),
                    contentDescription = if (Shared.language == "en") "Refresh" else "更新",
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                    provider = ImageProvider(R.drawable.baseline_refresh_24)
                )
            }
        }
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(10.dp),
                text = group.name[Shared.language],
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = GlanceTheme.colors.primaryContainer,
                    contentColor = GlanceTheme.colors.onPrimaryContainer
                ),
                onClick = actionRunCallback<SwitchGroupActionCallback>()
            )
        }
        LazyColumn(
            modifier = GlanceModifier
                .fillMaxSize()
                .defaultWeight(),
            horizontalAlignment = Alignment.Start
        ) {
            items(routeStops, itemId = { it.uniqueKey.hashCode().toLong() }) { route ->
                val co = route.co
                val kmbCtbJoint = route.route!!.isKmbCtbJoint
                val routeNumber = route.route!!.routeNumber
                val gmbRegion = route.route!!.gmbRegion
                val displayRouteNumber = co.getListDisplayRouteNumber(routeNumber, true)
                val dest = if (co.isTrain) {
                    Registry.getInstance(instance).getStopSpecialDestinations(route.stopInfo!!.stopId, co, route.route!!, true)
                } else {
                    route.resolvedDest(false, instance)
                }[Shared.language]
                val isNightRoute = co.isBus && calculateServiceTimeCategory(routeNumber, co) {
                    Registry.getInstance(instance).getAllBranchRoutes(routeNumber, route.route!!.idBound(co), co, gmbRegion).createTimetable(instance).getServiceTimeCategory()
                } == ServiceTimeCategory.NIGHT
                val secondLine = if (route.stopInfo != null) route.stopInfo!!.data!!.name[Shared.language] else null
                val coSpecialRemark = when {
                    co == Operator.NLB -> if (Shared.language == "en") "From ${route.route!!.orig.en}" else "從${route.route!!.orig.zh}開出"
                    co == Operator.KMB && routeNumber.getKMBSubsidiary() == KMBSubsidiary.SUNB -> if (Shared.language == "en") "Sun Bus (NR$routeNumber)" else "陽光巴士 (NR$routeNumber)"
                    co == Operator.KMB && routeNumber.isPetBus() -> if (Shared.language == "en") "Pet Bus \uD83D\uDC3E" else "寵物巴士 \uD83D\uDC3E"
                    else -> null
                }
                val color = co.getColor(routeNumber, Color.White).adjustBrightness(if (GlanceTheme.colors.isDark(instance.context)) 1F else 0.7F)
                val specialRouteAlerts = route.route!!.getSpecialRouteAlerts(instance)
                val otherDests = specialRouteAlerts.firstIsInstanceOrNull<SpecialRouteAlerts.SpecialDest>()?.routes?.mapNotNull {
                    if (it == route.route) {
                        null
                    } else {
                        it to it.resolvedDestWithBranch(false, it, route.stopInfoIndex, route.stopInfo!!.stopId, instance)[Shared.language]
                    }
                }

                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                ) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(route.getDeepLink())))),
                    ) {
                        Column(
                            modifier = GlanceModifier
                                .width(90.dp)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = GlanceModifier
                                    .cornerRadius(4.dp)
                                    .background(if (isNightRoute && !GlanceTheme.colors.isDark(instance.context)) Color.Black else Color.Transparent),
                                text = displayRouteNumber,
                                style = TextDefaults.defaultTextStyle.copy(
                                    color = if (isNightRoute) (if (GlanceTheme.colors.isDark(instance.context)) Color.Yellow else Color.White).forGlance() else GlanceTheme.colors.onBackground,
                                    fontSize = if (co == Operator.MTR && Shared.language != "en") {
                                        21F.sp
                                    } else {
                                        25F.sp
                                    }
                                )
                            )
                            Row(
                                modifier = GlanceModifier.wrapContentSize(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalAlignment = Alignment.Start
                            ) {
                                co.getDisplayFormattedName(routeNumber, kmbCtbJoint, gmbRegion, Shared.language).content.forEach {
                                    val styleColor = it.style.asSequence()
                                        .filterIsInstance<ColorContentStyle>()
                                        .firstOrNull()
                                        ?.color
                                        ?.run { Color(this).adjustBrightness(if (GlanceTheme.colors.isDark(instance.context)) 1F else 0.7F).forGlance() }
                                        ?: GlanceTheme.colors.onBackground
                                    Text(
                                        text = it.string,
                                        style = TextDefaults.defaultTextStyle.copy(
                                            fontSize = 12F.sp,
                                            color = styleColor
                                        )
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = GlanceModifier
                                .wrapContentHeight()
                                .defaultWeight(),
                            horizontalAlignment = Alignment.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = GlanceModifier.wrapContentSize(),
                            ) {
                                if (route.route!!.shouldPrependTo()) {
                                    Text(
                                        modifier = GlanceModifier.wrapContentWidth(),
                                        text = bilingualToPrefix[Shared.language],
                                        style = TextDefaults.defaultTextStyle.copy(
                                            color = GlanceTheme.colors.onBackground,
                                            fontSize = 22F.sp * TextUnit.Small.value
                                        )
                                    )
                                }
                                Text(
                                    modifier = GlanceModifier.defaultWeight(),
                                    text = dest,
                                    style = TextDefaults.defaultTextStyle.copy(
                                        color = GlanceTheme.colors.onBackground,
                                        fontSize = 22F.sp,
                                        fontWeight = if (Shared.disableBoldDest) TextDefaults.defaultTextStyle.fontWeight else FontWeight.Bold
                                    )
                                )
                            }
                            if (otherDests.isNotNullAndNotEmpty()) {
                                otherDests.let { otherDests ->
                                    for ((otherRoute, otherDest) in otherDests) {
                                        Row(
                                            modifier = GlanceModifier.wrapContentSize(),
                                        ) {
                                            if (otherRoute.shouldPrependTo()) {
                                                Text(
                                                    modifier = GlanceModifier.wrapContentWidth(),
                                                    text = bilingualOnlyToPrefix[Shared.language],
                                                    style = TextDefaults.defaultTextStyle.copy(
                                                        color = GlanceTheme.colors.onBackground,
                                                        fontSize = 17F.sp * TextUnit.Small.value
                                                    )
                                                )
                                            }
                                            Text(
                                                modifier = GlanceModifier.defaultWeight(),
                                                text = otherDest,
                                                style = TextDefaults.defaultTextStyle.copy(
                                                    color = GlanceTheme.colors.onBackground,
                                                    fontSize = 17F.sp,
                                                    fontWeight = if (Shared.disableBoldDest) TextDefaults.defaultTextStyle.fontWeight else FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            if (coSpecialRemark != null) {
                                Text(
                                    modifier = GlanceModifier.wrapContentWidth(),
                                    text = coSpecialRemark,
                                    style = TextDefaults.defaultTextStyle.copy(
                                        color = color.forGlance(),
                                        fontSize = 14F.sp
                                    )
                                )
                            }
                            if (secondLine != null) {
                                Text(
                                    text = secondLine,
                                    style = TextDefaults.defaultTextStyle.copy(
                                        color = GlanceTheme.colors.onBackground,
                                        fontSize = 14F.sp,
                                    )
                                )
                            }
                        }
                        RouteStopETAElement(route.uniqueKey, route, etaResults.asImmutableState(), etaUpdateTimes.asImmutableState(), instance)
                    }
                    Spacer(modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GlanceTheme.colors.outline)
                    )
                }
            }
        }
    }

}

@Composable
fun RouteStopETAElement(key: String, route: StopIndexedRouteSearchResultEntry, etaResults: ImmutableState<out MutableMap<String, Registry.ETAQueryResult>>, etaUpdateTimes: ImmutableState<out MutableMap<String, Long>>, instance: AppContext) {
    var etaState by remember { mutableStateOf(etaResults.value[key]) }

    LaunchedEffect (Unit) {
        etaUpdateTimes.value[key]?.apply {
            delay(etaUpdateTimes.value[key]?.let { (Shared.ETA_UPDATE_INTERVAL - (currentTimeMillis() - it)).coerceAtLeast(0) }?: 0)
        }
        while (true) {
            val result = CoroutineScope(etaUpdateScope).async {
                Registry.getInstance(instance).getEta(route.stopInfo!!.stopId, route.stopInfoIndex, route.co, route.route!!, instance).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
            }.await()
            etaState = result
            etaResults.value[key] = result
            if (!result.isConnectionError) {
                etaUpdateTimes.value[key] = currentTimeMillis()
            }
            delay(Shared.ETA_UPDATE_INTERVAL.toLong())
        }
    }

    Column(
        modifier = GlanceModifier.fillMaxHeight(),
        horizontalAlignment = Alignment.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val eta = etaState
        if (eta != null && !eta.isConnectionError) {
            if (eta.nextScheduledBus !in 0..59) {
                if (eta.isMtrEndOfLine) {
                    Image(
                        modifier = GlanceModifier.size(25.dp),
                        contentDescription = route.route!!.endOfLineText[Shared.language],
                        provider = ImageProvider(R.drawable.baseline_line_end_circle_24),
                        colorFilter = ColorFilter.tint(etaColor(instance.context).forGlance())
                    )
                } else if (eta.isTyphoonSchedule) {
                    val typhoonInfo by remember { Registry.getInstance(instance).typhoonInfo }.collectAsStateMultiplatform()
                    Image(
                        modifier = GlanceModifier.size(25.dp),
                        contentDescription = typhoonInfo.typhoonWarningTitle,
                        provider = ImageProvider(R.mipmap.cyclone)
                    )
                } else {
                    Image(
                        modifier = GlanceModifier.size(25.dp),
                        contentDescription = if (Shared.language == "en") "No scheduled departures at this moment" else "暫時沒有預定班次",
                        provider = ImageProvider(R.drawable.baseline_schedule_24),
                        colorFilter = ColorFilter.tint(etaColor(instance.context).forGlance())
                    )
                }
            } else {
                when (Shared.etaDisplayMode) {
                    ETADisplayMode.COUNTDOWN -> {
                        val (text1, text2) = eta.firstLine.shortText
                        Text(
                            modifier = GlanceModifier.height(24.2F.spToDp(instance).dp),
                            text = text1,
                            style = TextDefaults.defaultTextStyle.copy(
                                fontSize = 22F.sp,
                                color = etaColor(instance.context).forGlance(),
                                textAlign = TextAlign.End
                            )
                        )
                        Text(
                            modifier = GlanceModifier.height(11F.spToDp(instance).dp),
                            text = text2,
                            style = TextDefaults.defaultTextStyle.copy(
                                fontSize = 8F.sp,
                                color = etaColor(instance.context).forGlance(),
                                textAlign = TextAlign.End
                            )
                        )
                        (2..3).mapNotNull {
                            val (eText1, eText2) = eta[it].shortText
                            if (eText1.isBlank() || eText2 != text2) {
                                null
                            } else {
                                eText1
                            }
                        }.takeIf { it.isNotEmpty() }?.joinToString(", ", postfix = text2)?.apply {
                            Text(
                                modifier = GlanceModifier.height(14F.spToDp(instance).dp),
                                text = this,
                                style = TextDefaults.defaultTextStyle.copy(
                                    fontSize = 10F.sp,
                                    color = etaSecondColor(instance.context).forGlance(),
                                    textAlign = TextAlign.End
                                )
                            )
                        }
                    }
                    ETADisplayMode.CLOCK_TIME -> {
                        val text1 = eta.getResolvedText(1, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                        Text(
                            modifier = GlanceModifier.height(19F.spToDp(instance).dp),
                            text = text1,
                            style = TextDefaults.defaultTextStyle.copy(
                                fontSize = 17F.sp,
                                fontWeight = FontWeight.Bold,
                                color = etaColor(instance.context).forGlance(),
                                textAlign = TextAlign.End
                            )
                        )
                        (2..3).forEach {
                            val eText1 = eta.getResolvedText(it, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                            if (eText1.length > 1) {
                                Text(
                                    modifier = GlanceModifier.height(15F.spToDp(instance).dp),
                                    text = eText1,
                                    style = TextDefaults.defaultTextStyle.copy(
                                        fontSize = 13F.sp,
                                        color = etaSecondColor(instance.context).forGlance(),
                                        textAlign = TextAlign.End
                                    )
                                )
                            }
                        }
                    }
                    ETADisplayMode.CLOCK_TIME_WITH_COUNTDOWN -> {
                        val text1 = eta.getResolvedText(1, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                        val (text2, text3) = eta.firstLine.shortText
                        Row(
                            modifier = GlanceModifier
                                .wrapContentSize()
                                .padding(0.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                modifier = GlanceModifier.height(19F.spToDp(instance).dp),
                                text = text1,
                                style = TextDefaults.defaultTextStyle.copy(
                                    fontSize = 17F.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = etaColor(instance.context).forGlance(),
                                    textAlign = TextAlign.End
                                )
                            )
                            Spacer(
                                modifier = GlanceModifier
                                    .height(1F.dp)
                                    .width(14F.spToDp(instance).dp)
                            )
                            Text(
                                modifier = GlanceModifier.height(19F.spToDp(instance).dp),
                                text = text2,
                                style = TextDefaults.defaultTextStyle.copy(
                                    fontSize = 17F.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = etaColor(instance.context).forGlance(),
                                    textAlign = TextAlign.End
                                )
                            )
                            Text(
                                modifier = GlanceModifier.height(12F.spToDp(instance).dp),
                                text = text3,
                                style = TextDefaults.defaultTextStyle.copy(
                                    fontSize = 10F.sp,
                                    color = etaColor(instance.context).forGlance(),
                                    textAlign = TextAlign.End
                                )
                            )
                        }
                        (2..3).forEach {
                            val eText1 = eta.getResolvedText(it, Shared.etaDisplayMode, instance).resolvedClockTime.string.trim()
                            if (eText1.length > 1) {
                                val (eText2, eText3) = eta[it].shortText
                                Row(
                                    modifier = GlanceModifier
                                        .wrapContentSize()
                                        .padding(0.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        modifier = GlanceModifier.height(15F.spToDp(instance).dp),
                                        text = eText1,
                                        style = TextDefaults.defaultTextStyle.copy(
                                            fontSize = 13F.sp,
                                            color = etaSecondColor(instance.context).forGlance(),
                                            textAlign = TextAlign.End
                                        )
                                    )
                                    Spacer(
                                        modifier = GlanceModifier
                                            .height(1F.dp)
                                            .width(10F.spToDp(instance).dp)
                                    )
                                    Text(
                                        modifier = GlanceModifier.height(15F.spToDp(instance).dp),
                                        text = eText2,
                                        style = TextDefaults.defaultTextStyle.copy(
                                            fontSize = 13F.sp,
                                            color = etaColor(instance.context).forGlance(),
                                            textAlign = TextAlign.End
                                        )
                                    )
                                    Text(
                                        modifier = GlanceModifier.height(10F.spToDp(instance).dp),
                                        text = eText3,
                                        style = TextDefaults.defaultTextStyle.copy(
                                            fontSize = 8F.sp,
                                            color = etaColor(instance.context).forGlance(),
                                            textAlign = TextAlign.End
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

object UpdateFavouriteRoutesCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        FavouriteRoutesWidget.update(context, glanceId)
    }
}

object SwitchGroupActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) {
            val registry = Registry.getInstanceNoUpdateCheck(context.nonActiveAppContext)
            while (registry.state.value.isProcessing) {
                delay(100)
            }
            val groups = Shared.favoriteRouteStops.value
            val current = it[groupNameZhKey]
            val index = groups.indexOf { e -> e.name.zh == current }
            it[groupNameZhKey] = (if (index + 1 >= groups.size) groups.first() else groups[index + 1]).name.zh
        }
        FavouriteRoutesWidget.update(context, glanceId)
    }
}