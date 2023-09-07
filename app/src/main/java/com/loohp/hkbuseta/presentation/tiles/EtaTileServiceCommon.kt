package com.loohp.hkbuseta.presentation.tiles

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.text.HtmlCompat
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
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.presentation.MainActivity
import com.loohp.hkbuseta.presentation.shared.Registry
import com.loohp.hkbuseta.presentation.shared.Shared
import com.loohp.hkbuseta.presentation.utils.StringUtils
import com.loohp.hkbuseta.presentation.utils.StringUtilsKt
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

class EtaTileServiceCommon {

    companion object {

        const val RESOURCES_VERSION = "0"

        fun pleaseWait(favoriteIndex: Int, packageName: String, context: Context): LayoutElementBuilders.LayoutElement {
            return LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
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

        fun noFavouriteRouteStop(favoriteIndex: Int, packageName: String, context: Context): LayoutElementBuilders.LayoutElement {
            return LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
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

        fun title(index: Int, stopName: JSONObject, routeNumber: String, context: Context): LayoutElementBuilders.Text {
            var name = stopName.optString(Shared.language)
            if (Shared.language == "en") {
                name = StringUtils.capitalize(name)
            }
            return LayoutElementBuilders.Text.Builder()
                .setText("[".plus(routeNumber).plus("] ").plus(index).plus(". ").plus(name))
                .setMaxLines(Int.MAX_VALUE)
                .setFontStyle(
                    FontStyle.Builder()
                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .setSize(
                            DimensionBuilders.SpProp.Builder().setValue(StringUtils.scaledSize(if (Shared.language == "en") 13F else 17F, context)).build()
                        )
                        .setColor(
                            ColorProp.Builder(Color.White.toArgb()).build()
                        )
                        .build()
                ).build()
        }

        fun subTitle(destName: JSONObject, context: Context): LayoutElementBuilders.Text {
            var name = destName.optString(Shared.language)
            name = if (Shared.language == "en") "To " + StringUtils.capitalize(name) else "往$name"
            return LayoutElementBuilders.Text.Builder()
                .setText(name)
                .setMaxLines(Int.MAX_VALUE)
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(
                            DimensionBuilders.SpProp.Builder().setValue(StringUtils.scaledSize(12F, context)).build()
                        )
                        .setColor(
                            ColorProp.Builder(Color.White.toArgb()).build()
                        )
                        .build()
                ).build()
        }

        @OptIn(ProtoLayoutExperimental::class)
        fun etaText(lines: Map<Int, String>, seq: Int, singleLine: Boolean, context: Context): LayoutElementBuilders.Text {
            val text = StringUtilsKt.toAnnotatedString(
                HtmlCompat.fromHtml(
                    lines.getOrDefault(seq, "-"),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
            val color = Color.White.toArgb()

            return LayoutElementBuilders.Text.Builder()
                .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_MARQUEE)
                .setMarqueeIterations(-1)
                .setMaxLines(if (singleLine) 1 else Int.MAX_VALUE)
                .setText(text.text)
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(
                            DimensionBuilders.SpProp.Builder().setValue(StringUtils.scaledSize(if (seq == 1) 15F else (if (Shared.language == "en") 11F else 13F), context)).build()
                        )
                        .setColor(
                            ColorProp.Builder(color).build()
                        )
                        .build()
                ).build()
        }

        fun buildLayout(favoriteIndex: Int, favouriteStopRoute: JSONObject, packageName: String, context: Context): LayoutElementBuilders.LayoutElement {
            val stopName = favouriteStopRoute.optJSONObject("stop").optJSONObject("name")
            val destName = favouriteStopRoute.optJSONObject("route").optJSONObject("dest")

            val stopId = favouriteStopRoute.optString("stopId")
            val co = favouriteStopRoute.optString("co")
            val index = favouriteStopRoute.optInt("index")
            val stop = favouriteStopRoute.optJSONObject("stop")
            val route = favouriteStopRoute.optJSONObject("route")

            val eta = Registry.getEta(stopId, co, route, context)

            val color = when (co) {
                "kmb" -> 0xFFFF4747.toInt()
                "ctb" -> 0xFFFFE15E.toInt()
                "nlb" -> 0xFF9BFFC6.toInt()
                "mtr-bus" -> 0xFFAAD4FF.toInt()
                "gmb" -> 0xFFAAFFAF.toInt()
                else -> android.graphics.Color.DKGRAY
            }

            return LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
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
                                    ColorProp.Builder(color).build()
                                ).build()
                        ).build()
                )
                .addContent(
                    LayoutElementBuilders.Row.Builder()
                        .setWidth(DimensionBuilders.wrap())
                        .setHeight(DimensionBuilders.wrap())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
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
                                                        .addKeyToExtraMapping("stop", ActionBuilders.AndroidStringExtra.Builder().setValue(stop.toString()).build())
                                                        .addKeyToExtraMapping("route", ActionBuilders.AndroidStringExtra.Builder().setValue(route.toString()).build())
                                                        .setPackageName(packageName)
                                                        .build()
                                                ).build()
                                        ).build()
                                ).build()
                        )
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
                                    title(index, stopName, route.optString("route"), context)
                                )
                                .addContent(
                                    subTitle(destName, context)
                                )
                                .addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(12F).build()
                                        )
                                        .build()
                                ).addContent(
                                    etaText(eta, 1, false, context)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(7F).build()
                                        )
                                        .build()
                                ).addContent(
                                    etaText(eta, 2, true, context)
                                ).addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(
                                            DimensionBuilders.DpProp.Builder(7F).build()
                                        )
                                        .build()
                                ).addContent(
                                    etaText(eta, 3, true, context)
                                ).build()
                        ).build()
                ).build()
        }

        fun buildSuitableElement(etaIndex: Int, packageName: String, context: Context): LayoutElementBuilders.LayoutElement {
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

        fun buildTileRequest(etaIndex: Int, packageName: String, context: Context): ListenableFuture<TileBuilders.Tile> {
            return Futures.submit(Callable {
                TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCES_VERSION)
                    .setFreshnessIntervalMillis(0)
                    .setTileTimeline(
                        TimelineBuilders.Timeline.Builder().addTimelineEntry(
                            TimelineBuilders.TimelineEntry.Builder().setLayout(
                                LayoutElementBuilders.Layout.Builder().setRoot(
                                    buildSuitableElement(etaIndex, packageName, context)
                                ).build()
                            ).build()
                        ).build()
                    ).build()
            }, ForkJoinPool.commonPool())
        }

        fun buildTileResourcesRequest(): ListenableFuture<ResourceBuilders.Resources> {
            return Futures.immediateFuture(
                ResourceBuilders.Resources.Builder()
                    .setVersion(RESOURCES_VERSION)
                    .build()
            )
        }

        private val timerTasks: MutableMap<Int, TimerTask> = ConcurrentHashMap()
        private val lastUpdate: MutableMap<Int, Long> = ConcurrentHashMap()

        fun handleTileEnterEvent(favoriteIndex: Int, context: TileService) {
            timerTasks.compute(favoriteIndex) { _, currentValue ->
                currentValue?.cancel()
                val timerTask = object : TimerTask() {
                    override fun run() {
                        TileService.getUpdater(context).requestUpdate(context.javaClass)
                        lastUpdate[favoriteIndex] = System.currentTimeMillis()
                    }
                }
                val lastUpdated = 30000 - (System.currentTimeMillis() - lastUpdate.getOrDefault(favoriteIndex, 0))
                Timer().schedule(timerTask, lastUpdated.coerceAtLeast(0), 30000)
                return@compute timerTask
            }
        }

        fun handleTileLeaveEvent(favoriteIndex: Int) {
            timerTasks.remove(favoriteIndex)?.cancel()
        }

    }

}