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

import android.os.Build
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.AnimationParameterBuilders
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.AtomicLong
import co.touchlab.stately.concurrency.AtomicReference
import com.benasher44.uuid.Uuid
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.MainActivity
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.FavouriteResolvedStop
import com.loohp.hkbuseta.common.objects.FavouriteRouteStop
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.objects.getDisplayRouteNumber
import com.loohp.hkbuseta.common.objects.isTrain
import com.loohp.hkbuseta.common.objects.resolveStop
import com.loohp.hkbuseta.common.objects.resolveStops
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Shared.getResolvedText
import com.loohp.hkbuseta.common.shared.Tiles
import com.loohp.hkbuseta.common.utils.getAndNegate
import com.loohp.hkbuseta.common.utils.parallelMapNotNull
import com.loohp.hkbuseta.shared.AndroidShared
import com.loohp.hkbuseta.utils.addContentAnnotatedString
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.asContentAnnotatedString
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.dpToPixels
import com.loohp.hkbuseta.utils.findOptimalSp
import com.loohp.hkbuseta.utils.getColor
import com.loohp.hkbuseta.utils.getGPSLocation
import com.loohp.hkbuseta.utils.getOr
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.spToDp
import com.loohp.hkbuseta.utils.timeZone
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.datetime.DateTimeUnit
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.roundToInt

class TileState {

    private val updateTask: AtomicReference<ScheduledFuture<*>?> = AtomicReference(null)
    private val lastUpdated: AtomicLong = AtomicLong(0)
    private val cachedETAQueryResult: AtomicReference<Pair<Registry.MergedETAQueryResult<Pair<FavouriteResolvedStop, FavouriteRouteStop>>?, Long>> = AtomicReference(null to 0)
    private val tileLayoutState: AtomicBoolean = AtomicBoolean(false)
    private val lastTileArcColor: AtomicInt = AtomicInt(Int.MIN_VALUE)
    private val lastUpdateSuccessful: AtomicBoolean = AtomicBoolean(false)
    private val isCurrentlyUpdating: AtomicLong = AtomicLong(0)
    private val lastLocation: AtomicReference<Coordinates?> = AtomicReference(null)

    fun setUpdateTask(future: ScheduledFuture<*>) {
        updateTask.updateAndGet { it?.cancel(false); future }
    }

    fun cancelUpdateTask() {
        updateTask.updateAndGet { it?.cancel(false); null }
    }

    fun markLastUpdated() {
        lastUpdated.set(System.currentTimeMillis())
    }

    fun markShouldUpdate() {
        lastUpdated.set(0)
    }

    fun shouldUpdate(): Boolean {
        return !isCurrentlyUpdating() && (System.currentTimeMillis() - lastUpdated.get() > Shared.ETA_UPDATE_INTERVAL)
    }

    fun cacheETAQueryResult(eta: Registry.MergedETAQueryResult<Pair<FavouriteResolvedStop, FavouriteRouteStop>>?) {
        cachedETAQueryResult.set(eta to System.currentTimeMillis())
    }

    fun getETAQueryResult(orElse: (TileState) -> Registry.MergedETAQueryResult<Pair<FavouriteResolvedStop, FavouriteRouteStop>>?): Registry.MergedETAQueryResult<Pair<FavouriteResolvedStop, FavouriteRouteStop>>? {
        val (cache, time) = cachedETAQueryResult.getAndUpdate { null to 0 }
        return if (cache == null || System.currentTimeMillis() - time > 10000) orElse.invoke(this) else cache
    }

    fun getCurrentTileLayoutState(): Boolean {
        return tileLayoutState.getAndNegate()
    }

    fun getLastUpdateSuccessful(): Boolean {
        return lastUpdateSuccessful.value
    }

    fun setLastUpdateSuccessful(value: Boolean) {
        lastUpdateSuccessful.value = value
    }

    fun isCurrentlyUpdating(): Boolean {
        return System.currentTimeMillis() - isCurrentlyUpdating.get() < Shared.ETA_UPDATE_INTERVAL * 2
    }

    fun setCurrentlyUpdating(value: Boolean) {
        return isCurrentlyUpdating.set(if (value) System.currentTimeMillis() else 0)
    }

    fun getAndSetLastTileArcColor(value: Color): Color? {
        val lastValue = lastTileArcColor.getAndSet(value.toArgb())
        return if (lastValue == Int.MIN_VALUE) null else Color(lastValue)
    }

    fun setLastLocationOrGetLast(location: Coordinates?): Coordinates? {
        return location?.let { lastLocation.set(it); it }?: lastLocation.get()
    }
}

class EtaTileServiceCommon {

    companion object {

        private val resourceVersion: AtomicReference<String> = AtomicReference(Uuid.randomUUID().toString())
        private val inlineImageResources: MutableMap<String, Int> = ConcurrentHashMap()

        private val executor = Executors.newScheduledThreadPool(16)
        private val internalTileStates: MutableMap<Int, TileState> = ConcurrentHashMap()

        private fun tileState(etaIndex: Int): TileState {
            return internalTileStates.computeIfAbsent(etaIndex) { TileState() }
        }

        private fun addInlineImageResource(resource: Int): String {
            val key = resource.toString()
            inlineImageResources.computeIfAbsent(key) {
                resourceVersion.set(Uuid.randomUUID().toString())
                resource
            }
            return key
        }

        private fun targetWidth(context: AppContext, padding: Int): Int {
            return context.screenWidth - (padding * 2F).dpToPixels(context).roundToInt()
        }

        private fun noFavouriteRouteStop(tileId: Int, packageName: String, context: AppContext): LayoutElementBuilders.LayoutElement {
            val tileState = tileState(tileId)
            tileState.setLastUpdateSuccessful(true)
            tileState.getAndSetLastTileArcColor(Color.DarkGray)
            return if (tileId in (1 or Int.MIN_VALUE)..(8 or Int.MIN_VALUE)) {
                LayoutElementBuilders.Box.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setClickable(
                                ModifiersBuilders.Clickable.Builder()
                                    .setId("open")
                                    .setOnClick(
                                        ActionBuilders.LaunchAction.Builder()
                                            .setAndroidActivity(
                                                ActionBuilders.AndroidActivity.Builder()
                                                    .setClassName(MainActivity::class.java.name)
                                                    .setPackageName(packageName)
                                                    .build()
                                            ).build()
                                    ).build()
                            ).build()
                    )
                    .addContent(
                        LayoutElementBuilders.Arc.Builder()
                            .setAnchorAngle(
                                DimensionBuilders.DegreesProp.Builder(0F).build()
                            )
                            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                            .addContent(
                                ArcLine.Builder()
                                    .setLength(
                                        DimensionBuilders.DegreesProp.Builder(360F).build()
                                    )
                                    .setThickness(
                                        DimensionBuilders.dp(7F)
                                    )
                                    .setColor(
                                        ColorProp.Builder(Color.DarkGray.toArgb()).build()
                                    ).build()
                            ).build()
                    )
                    .addContent(
                        LayoutElementBuilders.Row.Builder()
                            .setWidth(wrap())
                            .setHeight(wrap())
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .addContent(
                                LayoutElementBuilders.Column.Builder()
                                    .setWidth(wrap())
                                    .setHeight(wrap())
                                    .setModifiers(
                                        ModifiersBuilders.Modifiers.Builder()
                                            .setPadding(
                                                ModifiersBuilders.Padding.Builder()
                                                    .setStart(DimensionBuilders.dp(20F))
                                                    .setEnd(DimensionBuilders.dp(20F))
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                    .addContent(
                                        LayoutElementBuilders.Text.Builder()
                                            .setMaxLines(Int.MAX_VALUE)
                                            .setFontStyle(
                                                FontStyle.Builder()
                                                    .setSize(
                                                        DimensionBuilders.sp(25F.scaledSize(context))
                                                    )
                                                    .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                                    .setColor(
                                                        ColorProp.Builder(Color.Yellow.toArgb()).build()
                                                    ).build()
                                            )
                                            .setText((tileId and Int.MAX_VALUE).toString())
                                            .build()
                                    )
                                    .addContent(
                                        LayoutElementBuilders.Text.Builder()
                                            .setMaxLines(Int.MAX_VALUE)
                                            .setText(if (Shared.language == "en") {
                                                "\nNo selected route stop\nYou can set the route stop in the app."
                                            } else {
                                                "\n未有選擇路線巴士站\n你可在應用程式中選取"
                                            })
                                            .build()
                                    ).build()
                            ).build()
                    ).build()
            } else {
                LayoutElementBuilders.Box.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setClickable(
                                ModifiersBuilders.Clickable.Builder()
                                    .setId("open")
                                    .setOnClick(
                                        ActionBuilders.LaunchAction.Builder()
                                            .setAndroidActivity(
                                                ActionBuilders.AndroidActivity.Builder()
                                                    .setClassName(MainActivity::class.java.name)
                                                    .setPackageName(packageName)
                                                    .build()
                                            ).build()
                                    ).build()
                            ).build()
                    )
                    .addContent(
                        LayoutElementBuilders.Arc.Builder()
                            .setAnchorAngle(
                                DimensionBuilders.DegreesProp.Builder(0F).build()
                            )
                            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                            .addContent(
                                ArcLine.Builder()
                                    .setLength(
                                        DimensionBuilders.DegreesProp.Builder(360F).build()
                                    )
                                    .setThickness(
                                        DimensionBuilders.dp(7F)
                                    )
                                    .setColor(
                                        ColorProp.Builder(Color.DarkGray.toArgb()).build()
                                    ).build()
                            ).build()
                    )
                    .addContent(
                        LayoutElementBuilders.Row.Builder()
                            .setWidth(wrap())
                            .setHeight(wrap())
                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                            .addContent(
                                LayoutElementBuilders.Column.Builder()
                                    .setWidth(wrap())
                                    .setHeight(wrap())
                                    .setModifiers(
                                        ModifiersBuilders.Modifiers.Builder()
                                            .setPadding(
                                                ModifiersBuilders.Padding.Builder()
                                                    .setStart(DimensionBuilders.dp(20F))
                                                    .setEnd(DimensionBuilders.dp(20F))
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                    .addContent(
                                        LayoutElementBuilders.Text.Builder()
                                            .setMaxLines(Int.MAX_VALUE)
                                            .setFontStyle(
                                                FontStyle.Builder()
                                                    .setSize(
                                                        DimensionBuilders.sp(15F.scaledSize(context))
                                                    ).build()
                                            )
                                            .setText(if (Shared.language == "en") {
                                                "Display selected favourite routes here\n"
                                            } else {
                                                "選擇最喜愛路線在此顯示\n"
                                            })
                                            .build()
                                    )
                                    .addContent(
                                        LayoutElementBuilders.Box.Builder()
                                            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                                            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                            .setModifiers(
                                                ModifiersBuilders.Modifiers.Builder()
                                                    .setBackground(
                                                        ModifiersBuilders.Background.Builder()
                                                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.dp(17.5F.scaledSize(context))).build())
                                                            .setColor(ColorProp.Builder(Color(0xFF1A1A1A).toArgb()).build())
                                                            .build()
                                                    )
                                                    .setClickable(
                                                        ModifiersBuilders.Clickable.Builder()
                                                            .setOnClick(
                                                                ActionBuilders.LaunchAction.Builder()
                                                                    .setAndroidActivity(
                                                                        ActionBuilders.AndroidActivity.Builder()
                                                                            .setClassName(EtaTileConfigureActivity::class.java.name)
                                                                            .addKeyToExtraMapping("com.google.android.clockwork.EXTRA_PROVIDER_CONFIG_TILE_ID", ActionBuilders.intExtra(tileId))
                                                                            .setPackageName(packageName)
                                                                            .build()
                                                                    ).build()
                                                            ).build()
                                                    )
                                                    .build()
                                            )
                                            .setWidth(DimensionBuilders.dp(135F.scaledSize(context)))
                                            .setHeight(DimensionBuilders.dp(35F.scaledSize(context)))
                                            .addContent(
                                                LayoutElementBuilders.Text.Builder()
                                                    .setMaxLines(1)
                                                    .setText(if (Shared.language == "en") {
                                                        "Select Route"
                                                    } else {
                                                        "選取路線"
                                                    })
                                                    .setFontStyle(
                                                        FontStyle.Builder()
                                                            .setSize(
                                                                DimensionBuilders.sp(17F.scaledSize(context))
                                                            )
                                                            .setColor(
                                                                ColorProp.Builder(Color.Yellow.toArgb()).build()
                                                            ).build()
                                                    )
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            ).build()
                    ).build()
            }
        }

        private fun title(index: Int, stopName: BilingualText, routeNumber: String, co: Operator, context: AppContext): LayoutElementBuilders.Text {
            val name = stopName[Shared.language]
            val text = if (co.isTrain) name else index.toString().plus(". ").plus(name)
            return LayoutElementBuilders.Text.Builder()
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(DimensionBuilders.dp(35F))
                                .setEnd(DimensionBuilders.dp(35F))
                                .build()
                        )
                        .build()
                )
                .setText(text)
                .setMaxLines(2)
                .setFontStyle(
                    FontStyle.Builder()
                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .setSize(
                            DimensionBuilders.sp(text.findOptimalSp(context, targetWidth(context, 35), 2, 1F, 17F).clampSp(context, dpMax = 17F))
                        )
                        .setColor(
                            ColorProp.Builder(Color.White.toArgb()).build()
                        )
                        .build()
                ).build()
        }

        private fun subTitle(destName: BilingualText, gpsStop: Boolean, routeNumber: String, co: Operator, context: AppContext): LayoutElementBuilders.Text {
            val name = co.getDisplayRouteNumber(routeNumber).plus(" ").plus(destName[Shared.language])
                .plus(if (gpsStop) (if (Shared.language == "en") " - Closest" else " - 最近") else "")
            return LayoutElementBuilders.Text.Builder()
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(DimensionBuilders.dp(20F))
                                .setEnd(DimensionBuilders.dp(20F))
                                .build()
                        )
                        .build()
                )
                .setText(name)
                .setMaxLines(1)
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(
                            DimensionBuilders.sp(name.findOptimalSp(context, targetWidth(context, 35), 1, 1F, 11F).clampSp(context, dpMax = 11F))
                        )
                        .setColor(
                            ColorProp.Builder(Color.White.toArgb()).build()
                        )
                        .build()
                ).build()
        }

        @androidx.annotation.OptIn(androidx.wear.protolayout.expression.ProtoLayoutExperimental::class)
        private fun etaText(eta: Registry.MergedETAQueryResult<Pair<FavouriteResolvedStop, FavouriteRouteStop>>?, seq: Int, mainResolvedStop: Pair<FavouriteResolvedStop, FavouriteRouteStop>, packageName: String, context: AppContext): LayoutElementBuilders.Spannable {
            val line = eta.getResolvedText(seq, Shared.clockTimeMode, context)
            val text = line.second.asContentAnnotatedString()
            val measure = text.annotatedString.text
            val color = Color.White.adjustBrightness(if (eta == null) 0.7F else 1F).toArgb()
            val maxTextSize = if (seq == 1) 15F else if (Shared.language == "en") 11F else 13F
            val padding = if (seq == 1) 20 else 35
            val (textSize, maxLines) = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                measure.findOptimalSp(context, targetWidth(context, padding + 2), 1, 7F, maxTextSize).clampSp(context, dpMax = maxTextSize) to if (seq == 1) 2 else 1
            } else {
                measure.findOptimalSp(context, targetWidth(context, padding + 2), 1, maxTextSize - (if (Shared.language == "en") 4F else 2F), maxTextSize).clampSp(context, dpMax = maxTextSize) to 1
            }
            val imageHeight = textSize.spToDp(context)

            val (resolvedStop, favouriteStopRoute) = line.first?: mainResolvedStop

            val (index, stopId, stop, route) = resolvedStop
            val co = favouriteStopRoute.co

            return LayoutElementBuilders.Spannable.Builder()
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(DimensionBuilders.dp(padding.toFloat()))
                                .setEnd(DimensionBuilders.dp(padding.toFloat()))
                                .build()
                        )
                        .setClickable(
                            ModifiersBuilders.Clickable.Builder()
                                .setId("open")
                                .setOnClick(
                                    ActionBuilders.LaunchAction.Builder()
                                        .setAndroidActivity(
                                            ActionBuilders.AndroidActivity.Builder()
                                                .setClassName(MainActivity::class.java.name)
                                                .addKeyToExtraMapping("stopId", ActionBuilders.stringExtra(stopId))
                                                .addKeyToExtraMapping("co", ActionBuilders.stringExtra(co.name))
                                                .addKeyToExtraMapping("index", ActionBuilders.intExtra(index))
                                                .addKeyToExtraMapping("stop", ActionBuilders.stringExtra(stop.serialize().toString()))
                                                .addKeyToExtraMapping("route", ActionBuilders.stringExtra(route.serialize().toString()))
                                                .setPackageName(packageName)
                                                .build()
                                        ).build()
                                ).build()
                        )
                        .build()
                )
                .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_MARQUEE)
                .setMaxLines(maxLines)
                .addContentAnnotatedString(text, textSize, { it.setColor(ColorProp.Builder(color).build()) }, { AndroidShared.RESOURCE_RATIO[it]!! * imageHeight to imageHeight }, { r -> addInlineImageResource(r) })
                .build()
        }

        private fun lastUpdated(context: AppContext): LayoutElementBuilders.Text {
            return LayoutElementBuilders.Text.Builder()
                .setMaxLines(1)
                .setText((if (Shared.language == "en") "Updated: " else "更新時間: ")
                    .plus(DateFormat.getTimeFormat(context.context).timeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Hong_Kong"))).format(Date())))
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(
                            DimensionBuilders.sp(9F.scaledSize(context).clampSp(context, dpMax = 9F))
                        ).build()
                ).build()
        }

        private fun buildLayout(tileId: Int, favouriteStopRoutes: List<FavouriteRouteStop>, packageName: String, context: AppContext): LayoutElementBuilders.LayoutElement {
            val tileState = tileState(tileId)

            val eta = tileState.getETAQueryResult {
                val favouriteRouteStops = favouriteStopRoutes.resolveStops(context) { it.setLastLocationOrGetLast(getGPSLocation(context).asCompletableFuture().getOr(7, TimeUnit.SECONDS) { null }?.location) }
                val eta = Registry.MergedETAQueryResult.merge(
                    favouriteRouteStops.parallelMapNotNull(executor.asCoroutineDispatcher()) { (favStop, resolved) ->
                        val (index, stopId, _, route) = resolved?: return@parallelMapNotNull null
                        (resolved to favStop) to Registry.getInstanceNoUpdateCheck(context).getEta(stopId, index, favStop.co, route, context).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
                    }
                )
                it.markLastUpdated()
                eta
            }
            val mainResolvedStop = eta?.firstKey?: run {
                val favStop = favouriteStopRoutes[0]
                favStop.resolveStop(context) { getGPSLocation(context).asCompletableFuture().getOr(2, TimeUnit.SECONDS) { null }?.location } to favStop
            }
            val (favouriteResolvedStop, favouriteStopRoute) = mainResolvedStop

            val (index, stopId, stop, route) = favouriteResolvedStop
            val co = favouriteStopRoute.co
            val gpsStop = favouriteStopRoute.favouriteStopMode.isRequiresLocation

            val routeNumber = route.routeNumber
            val stopName = stop.name
            val destName = Registry.getInstanceNoUpdateCheck(context).getStopSpecialDestinations(stopId, co, route, true)

            val color = if (eta == null) {
                tileState.setLastUpdateSuccessful(false)
                tileState.markShouldUpdate()
                co.getColor(routeNumber, Color.LightGray).adjustBrightness(0.5F)
            } else if (eta.isConnectionError) {
                tileState.setLastUpdateSuccessful(false)
                Color.DarkGray
            } else {
                tileState.setLastUpdateSuccessful(true)
                eta.nextCo.getColor(routeNumber, Color.LightGray).adjustBrightness(if (eta.nextScheduledBus !in 0..59) 0.2F else 1F)
            }
            val previousColor = tileState.getAndSetLastTileArcColor(color)?: color
            val colorProp = ColorProp.Builder(color.toArgb())
            if (previousColor != color) {
                colorProp.setDynamicValue(
                    DynamicColor.animate(previousColor.toArgb(), color.toArgb(),
                        AnimationParameterBuilders.AnimationSpec.Builder()
                            .setAnimationParameters(
                                AnimationParameterBuilders.AnimationParameters.Builder()
                                    .setDurationMillis(1000)
                                    .build()
                            ).build()
                    )
                )
            }

            return LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(
                            ModifiersBuilders.Clickable.Builder()
                                .setId("open")
                                .setOnClick(
                                    ActionBuilders.LaunchAction.Builder()
                                        .setAndroidActivity(
                                            ActionBuilders.AndroidActivity.Builder()
                                                .setClassName(MainActivity::class.java.name)
                                                .addKeyToExtraMapping("stopId", ActionBuilders.stringExtra(stopId))
                                                .addKeyToExtraMapping("co", ActionBuilders.stringExtra(co.name))
                                                .addKeyToExtraMapping("index", ActionBuilders.intExtra(index))
                                                .addKeyToExtraMapping("stop", ActionBuilders.stringExtra(stop.serialize().toString()))
                                                .addKeyToExtraMapping("route", ActionBuilders.stringExtra(route.serialize().toString()))
                                                .setPackageName(packageName)
                                                .build()
                                        ).build()
                                ).build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(0F).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                        .addContent(
                            ArcLine.Builder()
                                .setLength(
                                    DimensionBuilders.DegreesProp.Builder(360F).build()
                                )
                                .setThickness(
                                    DimensionBuilders.dp(7F)
                                )
                                .setColor(
                                    colorProp.build()
                                ).build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Row.Builder()
                        .setWidth(wrap())
                        .setHeight(wrap())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Column.Builder()
                                .setWidth(wrap())
                                .setHeight(wrap())
                                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                .addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.dp(16F.scaledSize(context))
                                        ).build()
                                ).addContent(
                                    title(index, stopName, routeNumber, co, context)
                                )
                                .addContent(
                                    subTitle(destName, gpsStop, routeNumber, co, context)
                                )
                                .addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.dp(12F.scaledSize(context))
                                        ).build()
                                ).addContent(
                                    etaText(eta, 1, mainResolvedStop, packageName, context)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.dp(7F.scaledSize(context))
                                        ).build()
                                ).addContent(
                                    etaText(eta, 2, mainResolvedStop, packageName, context)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.dp(7F.scaledSize(context))
                                        ).build()
                                ).addContent(
                                    etaText(eta, 3, mainResolvedStop, packageName, context)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.dp(7F.scaledSize(context))
                                        ).build()
                                ).addContent(
                                    lastUpdated(context)
                                ).build()
                        ).build()
                ).build()
        }

        private fun buildSuitableElement(tileId: Int, packageName: String, context: AppContext): LayoutElementBuilders.LayoutElement {
            while (Registry.getInstanceNoUpdateCheck(context).state.value.isProcessing) {
                TimeUnit.MILLISECONDS.sleep(10)
            }
            val favouriteRoutes = Tiles.getEtaTileConfiguration(tileId)
            return if (favouriteRoutes.isEmpty() || favouriteRoutes.none { Shared.favoriteRouteStops[it] != null }) {
                noFavouriteRouteStop(tileId, packageName, context)
            } else {
                buildLayout(tileId, favouriteRoutes.mapNotNull { Shared.favoriteRouteStops[it] }, packageName, context)
            }
        }

        fun buildTileRequest(tileId: Int, packageName: String, context: AppContext): ListenableFuture<TileBuilders.Tile> {
            val tileState = tileState(tileId)
            tileState.setCurrentlyUpdating(true)
            return Futures.submit(Callable {
                try {
                    val element = buildSuitableElement(tileId, packageName, context)
                    val stateElement = if (tileState.getCurrentTileLayoutState()) {
                        element
                    } else {
                        LayoutElementBuilders.Box.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .addContent(element)
                            .build()
                    }
                    TileBuilders.Tile.Builder()
                        .setResourcesVersion(resourceVersion.get().toString())
                        .setFreshnessIntervalMillis(0)
                        .setTileTimeline(
                            TimelineBuilders.Timeline.Builder().addTimelineEntry(
                                TimelineBuilders.TimelineEntry.Builder().setLayout(
                                    LayoutElementBuilders.Layout.Builder().setRoot(
                                        stateElement
                                    ).build()
                                ).build()
                            ).build()
                        ).build()
                } finally {
                    tileState.setCurrentlyUpdating(false)
                }
            }, executor)
        }

        fun buildTileResourcesRequest(): ListenableFuture<ResourceBuilders.Resources> {
            return Futures.submit(Callable {
                val resourceBuilder = ResourceBuilders.Resources.Builder().setVersion(resourceVersion.get().toString())
                for ((key, resource) in inlineImageResources) {
                    resourceBuilder.addIdToImageMapping(key, ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(resource)
                                .build()
                        )
                        .build()
                    )
                }
                resourceBuilder.build()
            }, executor)
        }

        fun handleTileEnterEvent(tileId: Int, context: AppContext) {
            Tiles.providePlatformUpdate { requestTileUpdate(it) }
            Shared.provideBackgroundUpdateScheduler { c, t -> AndroidShared.scheduleBackgroundUpdateService(c.context, t) }
            tileState(tileId).let {
                if (!it.getLastUpdateSuccessful()) {
                    it.markShouldUpdate()
                }
                it.setUpdateTask(executor.scheduleWithFixedDelay({
                    while (Registry.getInstanceNoUpdateCheck(context).state.value.isProcessing) {
                        TimeUnit.MILLISECONDS.sleep(10)
                    }
                    if (it.shouldUpdate()) {
                        val favouriteRoutes = Tiles.getEtaTileConfiguration(tileId)
                        if (favouriteRoutes.isNotEmpty()) {
                            val favouriteRouteStops = favouriteRoutes
                                .mapNotNull { favouriteRoute -> Shared.favoriteRouteStops[favouriteRoute] }
                                .resolveStops(context) { it.setLastLocationOrGetLast(getGPSLocation(context).asCompletableFuture().getOr(7, TimeUnit.SECONDS) { null }?.location) }
                            it.cacheETAQueryResult(Registry.MergedETAQueryResult.merge(
                                favouriteRouteStops.parallelMapNotNull(executor.asCoroutineDispatcher()) { (favStop, resolved) ->
                                    val (index, stopId, _, route) = resolved?: return@parallelMapNotNull null
                                    (resolved to favStop) to Registry.getInstanceNoUpdateCheck(context).getEta(stopId, index, favStop.co, route, context).get(Shared.ETA_UPDATE_INTERVAL, DateTimeUnit.MILLISECOND)
                                }
                            ))
                            it.markLastUpdated()
                            TileService.getUpdater(context.context).requestUpdate((context.context as TileService).javaClass)
                        }
                    }
                }, 0, 1000, TimeUnit.MILLISECONDS))
            }
        }

        fun handleTileLeaveEvent(tileId: Int) {
            tileState(tileId).cancelUpdateTask()
        }

        fun handleTileRemoveEvent(tileId: Int, context: AppContext) {
            handleTileLeaveEvent(tileId)
            Registry.getInstanceNoUpdateCheck(context).clearEtaTileConfiguration(tileId, context)
        }

        fun requestTileUpdate(legacyTileId: Int = 0) {
            if (legacyTileId in 1..8) {
                tileState(legacyTileId or Int.MIN_VALUE).markShouldUpdate()
            } else {
                for ((tileId, tileState) in internalTileStates) {
                    if (tileId !in (1 or Int.MIN_VALUE)..(8 or Int.MIN_VALUE)) {
                        tileState.markShouldUpdate()
                    }
                }
            }
        }

    }

}