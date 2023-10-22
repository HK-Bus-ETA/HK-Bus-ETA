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

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.aghajari.compose.text.asAnnotatedString
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.MainActivity
import com.loohp.hkbuseta.objects.BilingualText
import com.loohp.hkbuseta.objects.FavouriteRouteStop
import com.loohp.hkbuseta.objects.coColor
import com.loohp.hkbuseta.objects.displayRouteNumber
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Registry.ETAQueryResult
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.utils.ScreenSizeUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.TimerUtils
import com.loohp.hkbuseta.utils.UnitUtils
import com.loohp.hkbuseta.utils.addContentAnnotatedString
import com.loohp.hkbuseta.utils.adjustBrightness
import com.loohp.hkbuseta.utils.clampSp
import com.loohp.hkbuseta.utils.toSpanned
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

data class InlineImageResource(val data: ByteArray, val width: Int, val height: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InlineImageResource

        if (!data.contentEquals(other.data)) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

class EtaTileServiceCommon {

    companion object {

        private val RESOURCES_VERSION: AtomicReference<String> = AtomicReference(UUID.randomUUID().toString())

        private val inlineImageResources: MutableMap<String, InlineImageResource> = ConcurrentHashMap()

        private fun addInlineImageResource(resource: InlineImageResource): String {
            val md = MessageDigest.getInstance("MD5")
            val hash = BigInteger(1, md.digest(resource.data)).toString(16).padStart(32, '0')
                .plus("_").plus(resource.width).plus("_").plus(resource.height)
            inlineImageResources.computeIfAbsent(hash) {
                RESOURCES_VERSION.set(UUID.randomUUID().toString())
                resource
            }
            return hash
        }

        private fun targetWidth(context: Context, padding: Int): Int {
            return ScreenSizeUtils.getScreenWidth(context) - UnitUtils.dpToPixels(context, (padding * 2).toFloat()).roundToInt()
        }

        private fun pleaseWait(favoriteIndex: Int, packageName: String, context: Context): LayoutElementBuilders.LayoutElement {
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
                                    DimensionBuilders.DpProp.Builder(7F).build()
                                )
                                .setColor(
                                    ColorProp.Builder(android.graphics.Color.DKGRAY).build()
                                ).build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Row.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.wrap())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Column.Builder()
                                .setWidth(DimensionBuilders.wrap())
                                .setHeight(DimensionBuilders.wrap())
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setPadding(
                                            ModifiersBuilders.Padding.Builder()
                                                .setStart(DimensionBuilders.DpProp.Builder(35F).build())
                                                .setEnd(DimensionBuilders.DpProp.Builder(35F).build())
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
                                                    DimensionBuilders.SpProp.Builder().setValue(StringUtils.scaledSize(25F, context)).build()
                                                )
                                                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                                .setColor(
                                                    ColorProp.Builder(Color.Gray.toArgb()).build()
                                                ).build()
                                        )
                                        .setText(favoriteIndex.toString())
                                        .build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(10F, context)).build()
                                        ).build()
                                )
                                .addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setMaxLines(Int.MAX_VALUE)
                                        .setFontStyle(
                                            FontStyle.Builder()
                                                .setSize(
                                                    DimensionBuilders.SpProp.Builder().setValue(StringUtils.scaledSize(17F, context)).build()
                                                ).build()
                                        )
                                        .setText("應用程式啟動中...")
                                        .build()
                                ).addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setMaxLines(Int.MAX_VALUE)
                                        .setFontStyle(
                                            FontStyle.Builder()
                                                .setSize(
                                                    DimensionBuilders.SpProp.Builder().setValue(StringUtils.scaledSize(9F, context)).build()
                                                ).build()
                                        )
                                        .setText("你可允許背景活動以防應用程式被終止")
                                        .build()
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(5F, context)).build()
                                        ).build()
                                ).addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setMaxLines(Int.MAX_VALUE)
                                        .setFontStyle(
                                            FontStyle.Builder()
                                                .setSize(
                                                    DimensionBuilders.SpProp.Builder().setValue(StringUtils.scaledSize(17F, context)).build()
                                                ).build()
                                        )
                                        .setText("Starting App...")
                                        .build()
                                ).addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setMaxLines(Int.MAX_VALUE)
                                        .setFontStyle(
                                            FontStyle.Builder()
                                                .setSize(
                                                    DimensionBuilders.SpProp.Builder().setValue(StringUtils.scaledSize(9F, context)).build()
                                                ).build()
                                        )
                                        .setText("You can allow background activity to prevent the app from being terminated")
                                        .build()
                                ).build()
                        ).build()
                ).build()
        }

        private fun noFavouriteRouteStop(favoriteIndex: Int, packageName: String, context: Context): LayoutElementBuilders.LayoutElement {
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
                                    DimensionBuilders.DpProp.Builder(7F).build()
                                )
                                .setColor(
                                    ColorProp.Builder(android.graphics.Color.DKGRAY).build()
                                ).build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Row.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.wrap())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Column.Builder()
                                .setWidth(DimensionBuilders.wrap())
                                .setHeight(DimensionBuilders.wrap())
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setPadding(
                                            ModifiersBuilders.Padding.Builder()
                                                .setStart(DimensionBuilders.DpProp.Builder(20F).build())
                                                .setEnd(DimensionBuilders.DpProp.Builder(20F).build())
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
                                                    DimensionBuilders.SpProp.Builder().setValue(StringUtils.scaledSize(25F, context)).build()
                                                )
                                                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                                .setColor(
                                                    ColorProp.Builder(Color.Yellow.toArgb()).build()
                                                ).build()
                                        )
                                        .setText(favoriteIndex.toString())
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
        }

        private fun title(index: Int, stopName: BilingualText, routeNumber: String, co: String, context: Context): LayoutElementBuilders.Text {
            val name = stopName[Shared.language]
            val text = if (co == "mtr") name else index.toString().plus(". ").plus(name)
            return LayoutElementBuilders.Text.Builder()
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(DimensionBuilders.DpProp.Builder(35F).build())
                                .setEnd(DimensionBuilders.DpProp.Builder(35F).build())
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
                            DimensionBuilders.SpProp.Builder().setValue(clampSp(context, StringUtils.findOptimalSp(context, text, targetWidth(context, 35), 2, 1F, 17F), dpMax = 17F)).build()
                        )
                        .setColor(
                            ColorProp.Builder(Color.White.toArgb()).build()
                        )
                        .build()
                ).build()
        }

        private fun subTitle(destName: BilingualText, routeNumber: String, co: String, context: Context): LayoutElementBuilders.Text {
            val routeName = co.displayRouteNumber(routeNumber)
            val name = if (Shared.language == "en") {
                routeName.plus(" To ").plus(destName.en)
            } else {
                routeName.plus(" 往").plus(destName.zh)
            }
            return LayoutElementBuilders.Text.Builder()
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(DimensionBuilders.DpProp.Builder(20F).build())
                                .setEnd(DimensionBuilders.DpProp.Builder(20F).build())
                                .build()
                        )
                        .build()
                )
                .setText(name)
                .setMaxLines(1)
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(
                            DimensionBuilders.SpProp.Builder().setValue(clampSp(context, StringUtils.findOptimalSp(context, name, targetWidth(context, 35), 1, 1F, 11F), dpMax = 11F)).build()
                        )
                        .setColor(
                            ColorProp.Builder(Color.White.toArgb()).build()
                        )
                        .build()
                ).build()
        }

        @androidx.annotation.OptIn(androidx.wear.protolayout.expression.ProtoLayoutExperimental::class)
        private fun etaText(eta: ETAQueryResult, seq: Int, singleLine: Boolean, context: Context): LayoutElementBuilders.Spannable {
            val raw = eta.getLine(seq)
            val measure = raw.toSpanned(context, 17F).asAnnotatedString().annotatedString.text
            val color = Color.White.toArgb()
            val maxTextSize = if (seq == 1) 15F else if (Shared.language == "en") 11F else 13F
            val maxLines = if (singleLine) 1 else 2
            val textSize = clampSp(context, StringUtils.findOptimalSp(context, measure, targetWidth(context, 20) / 10 * 8, maxLines, 1F, maxTextSize), dpMax = maxTextSize)
            val text = raw.toSpanned(context, textSize).asAnnotatedString()

            return LayoutElementBuilders.Spannable.Builder()
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(DimensionBuilders.DpProp.Builder(if (seq == 1) 20F else 35F).build())
                                .setEnd(DimensionBuilders.DpProp.Builder(if (seq == 1) 20F else 35F).build())
                                .build()
                        )
                        .build()
                )
                .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_MARQUEE)
                .setMaxLines(maxLines)
                .addContentAnnotatedString(context, text,
                    FontStyle.Builder()
                        .setSize(
                            DimensionBuilders.SpProp.Builder().setValue(textSize).build()
                        )
                        .setColor(
                            ColorProp.Builder(color).build()
                        )
                        .build()
                ) { d, w, h -> addInlineImageResource(InlineImageResource(d, w, h)) }
                .build()
        }

        private fun lastUpdated(context: Context): LayoutElementBuilders.Text {
            return LayoutElementBuilders.Text.Builder()
                .setMaxLines(1)
                .setText((if (Shared.language == "en") "Updated: " else "更新時間: ").plus(DateFormat.getTimeFormat(context).format(Date())))
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(
                            DimensionBuilders.SpProp.Builder().setValue(clampSp(context, StringUtils.scaledSize(9F, context), dpMax = 9F)).build()
                        ).build()
                ).build()
        }

        private fun buildLayout(favoriteIndex: Int, favouriteStopRoute: FavouriteRouteStop, packageName: String, context: Context): LayoutElementBuilders.LayoutElement {
            val stopId = favouriteStopRoute.stopId
            val co = favouriteStopRoute.co
            val index = favouriteStopRoute.index
            val stop = favouriteStopRoute.stop
            val route = favouriteStopRoute.route

            val routeNumber = route.routeNumber
            val stopName = stop.name
            val destName = Registry.getInstance(context).getStopSpecialDestinations(stopId, co, route)

            val eta = Registry.getEta(stopId, co, route, context)

            val color = if (eta.isConnectionError) {
                Color.DarkGray
            } else {
                eta.nextCo.coColor(routeNumber, Color.LightGray).adjustBrightness(if (eta.nextScheduledBus < 0 || eta.nextScheduledBus > 60) 0.2F else 1F)
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
                                                .addKeyToExtraMapping("stopId", ActionBuilders.AndroidStringExtra.Builder().setValue(stopId).build())
                                                .addKeyToExtraMapping("co", ActionBuilders.AndroidStringExtra.Builder().setValue(co).build())
                                                .addKeyToExtraMapping("index", ActionBuilders.AndroidIntExtra.Builder().setValue(index).build())
                                                .addKeyToExtraMapping("stop", ActionBuilders.AndroidStringExtra.Builder().setValue(stop.serialize().toString()).build())
                                                .addKeyToExtraMapping("route", ActionBuilders.AndroidStringExtra.Builder().setValue(route.serialize().toString()).build())
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
                                    DimensionBuilders.DpProp.Builder(7F).build()
                                )
                                .setColor(
                                    ColorProp.Builder(color.toArgb()).build()
                                ).build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Row.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.wrap())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Column.Builder()
                                .setWidth(DimensionBuilders.wrap())
                                .setHeight(DimensionBuilders.wrap())
                                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                .addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(16F, context)).build()
                                        ).build()
                                ).addContent(
                                    title(index, stopName, routeNumber, co, context)
                                )
                                .addContent(
                                    subTitle(destName, routeNumber, co, context)
                                )
                                .addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(12F, context)).build()
                                        ).build()
                                ).addContent(
                                    etaText(eta, 1, co == "mtr" || co == "lightRail", context)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(7F, context)).build()
                                        ).build()
                                ).addContent(
                                    etaText(eta, 2, true, context)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(7F, context)).build()
                                        ).build()
                                ).addContent(
                                    etaText(eta, 3, true, context)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(StringUtils.scaledSize(7F, context)).build()
                                        ).build()
                                ).addContent(
                                    lastUpdated(context)
                                ).build()
                        ).build()
                ).build()
        }

        private fun buildSuitableElement(etaIndex: Int, packageName: String, context: Context): LayoutElementBuilders.LayoutElement {
            if (!Registry.getInstance(context).isPreferencesLoaded) {
                TimeUnit.SECONDS.sleep(1)
                if (!Registry.getInstance(context).isPreferencesLoaded) {
                    return pleaseWait(etaIndex, packageName, context)
                }
            }
            return if (!Shared.favoriteRouteStops.containsKey(etaIndex)) noFavouriteRouteStop(
                etaIndex,
                packageName,
                context
            ) else buildLayout(
                etaIndex,
                Shared.favoriteRouteStops[etaIndex]!!,
                packageName,
                context
            )
        }

        private val states: MutableMap<Int, Boolean> = ConcurrentHashMap()

        fun buildTileRequest(etaIndex: Int, packageName: String, context: TileService): ListenableFuture<TileBuilders.Tile> {
            val element = buildSuitableElement(etaIndex, packageName, context)
            val stateElement = if (states.compute(etaIndex) { _, v -> if (v == null) false else !v }!!) {
                element
            } else {
                LayoutElementBuilders.Box.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .addContent(element)
                    .build()
            }
            return Futures.submit(Callable {
                TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCES_VERSION.get().toString())
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
            }, ForkJoinPool.commonPool())
        }

        fun buildTileResourcesRequest(): ListenableFuture<ResourceBuilders.Resources> {
            return Futures.submit(Callable {
                val resourceBuilder = ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION.get().toString())
                for ((key, resource) in inlineImageResources) {
                    resourceBuilder.addIdToImageMapping(key, ResourceBuilders.ImageResource.Builder()
                        .setInlineResource(
                            ResourceBuilders.InlineImageResource.Builder()
                                .setData(resource.data)
                                .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
                                .setWidthPx(resource.width)
                                .setHeightPx(resource.height)
                                .build()
                        )
                        .build()
                    )
                }
                return@Callable resourceBuilder.build()
            }, ForkJoinPool.commonPool())
        }

        private val timerTasks: MutableMap<Int, TimerTask> = ConcurrentHashMap()
        private val lastUpdate: MutableMap<Int, Long> = ConcurrentHashMap()

        fun handleTileEnterEvent(favoriteIndex: Int, context: TileService) {
            timerTasks.compute(favoriteIndex) { _, currentValue ->
                currentValue?.cancel()
                val timerTask = TimerUtils.newTimerTask {
                    TileService.getUpdater(context).requestUpdate(context.javaClass)
                    lastUpdate[favoriteIndex] = System.currentTimeMillis()
                }
                val lastUpdated = Shared.ETA_UPDATE_INTERVAL - (System.currentTimeMillis() - lastUpdate.getOrDefault(favoriteIndex, 0))
                Timer().schedule(timerTask, lastUpdated.coerceAtLeast(0), Shared.ETA_UPDATE_INTERVAL)
                return@compute timerTask
            }
        }

        fun handleTileLeaveEvent(favoriteIndex: Int) {
            timerTasks.remove(favoriteIndex)?.cancel()
        }

    }

}