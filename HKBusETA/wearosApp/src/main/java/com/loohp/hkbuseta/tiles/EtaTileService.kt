/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.MainActivity
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.utils.scaledSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Callable

class EtaTileService : TileService() {

    override fun onCreate() {
        WearOSShared.setDefaultExceptionHandler(this)
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return if (runBlocking { Registry.isNewInstall(appContext) }) {
            firstInstallTile(packageName, appContext)
        } else {
            EtaTileServiceCommon.buildTileRequest(requestParams.tileId, packageName, appContext)
        }
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return if (requestParams.version == "-1") {
            firstInstallResources()
        } else {
            EtaTileServiceCommon.buildTileResourcesRequest()
        }
    }

    override fun onRecentInteractionEventsAsync(events: MutableList<EventBuilders.TileInteractionEvent>): ListenableFuture<Void> {
        return if (runBlocking { !Registry.isNewInstall(appContext) }) {
            EtaTileServiceCommon.handleRecentInteractionEventsAsync(events, appContext)
        } else {
            Futures.immediateFuture(null)
        }
    }

    override fun onTileRemoveEvent(requestParams: EventBuilders.TileRemoveEvent) {
        if (runBlocking { !Registry.isNewInstall(appContext) }) {
            EtaTileServiceCommon.handleTileRemoveEvent(requestParams.tileId, appContext)
        }
    }

}

private fun firstInstallTile(packageName: String, context: AppContext): ListenableFuture<TileBuilders.Tile> {
    return Futures.submit(Callable {
        TileBuilders.Tile.Builder()
            .setResourcesVersion("-1")
            .setFreshnessIntervalMillis(0)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder().addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder().setLayout(
                        LayoutElementBuilders.Layout.Builder().setRoot(
                            firstInstallElement(packageName, context)
                        ).build()
                    ).build()
                ).build()
            ).build()
    }, Dispatchers.Default.asExecutor())
}

private fun firstInstallResources(): ListenableFuture<ResourceBuilders.Resources> {
    return Futures.submit(Callable {
        ResourceBuilders.Resources.Builder()
            .setVersion("-1")
            .addIdToImageMapping("icon", ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.icon_full_smaller)
                        .build()
                )
                .build()
            )
            .build()
    }, Dispatchers.Default.asExecutor())
}

private fun firstInstallElement(packageName: String, context: AppContext): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
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
                    LayoutElementBuilders.ArcLine.Builder()
                        .setLength(
                            DimensionBuilders.DegreesProp.Builder(360F).build()
                        )
                        .setThickness(
                            DimensionBuilders.dp(7F)
                        )
                        .setColor(
                            ColorBuilders.ColorProp.Builder(Color.Red.toArgb()).build()
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
                                        .setStart(DimensionBuilders.dp(20F.scaledSize(context)))
                                        .setEnd(DimensionBuilders.dp(20F.scaledSize(context)))
                                        .build()
                                )
                                .build()
                        )
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Image.Builder()
                                .setWidth(DimensionBuilders.dp(48F.scaledSize(context)))
                                .setHeight(DimensionBuilders.dp(48F.scaledSize(context)))
                                .setResourceId("icon")
                                .setModifiers(
                                    ModifiersBuilders.Modifiers.Builder()
                                        .setPadding(
                                            ModifiersBuilders.Padding.Builder()
                                                .setBottom(DimensionBuilders.dp(5F.scaledSize(context)))
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setMaxLines(Int.MAX_VALUE)
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(
                                            DimensionBuilders.sp(15F.scaledSize(context))
                                        ).build()
                                )
                                .setText("香港巴士到站預報\nHK Bus ETA\n\n請開啟應用程式\nPlease open the app")
                                .build()
                        )
                        .build()
                ).build()
        ).build()
}