package com.loohp.hkbuseta.presentation.tiles

import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.presentation.Shared
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Callable

private const val ETA_TILE_INDEX = 4

class EtaTileServiceFour : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return Futures.submit(Callable {
            TileBuilders.Tile.Builder()
                .setResourcesVersion(EtaTileServiceCommon.RESOURCES_VERSION)
                .setFreshnessIntervalMillis(30000)
                .setTileTimeline(
                    TimelineBuilders.Timeline.Builder().addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder().setLayout(
                            LayoutElementBuilders.Layout.Builder().setRoot(
                                if (!Shared.favoriteRouteStops.containsKey(ETA_TILE_INDEX)) EtaTileServiceCommon.noFavouriteRouteStop(
                                    ETA_TILE_INDEX,
                                    packageName,
                                    this
                                ) else EtaTileServiceCommon.buildLayout(
                                    ETA_TILE_INDEX,
                                    Shared.favoriteRouteStops[ETA_TILE_INDEX]!!,
                                    packageName,
                                    this
                                )
                            ).build()
                        ).build()
                    ).build()
                ).build()
        }, ForkJoinPool.commonPool())
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(EtaTileServiceCommon.RESOURCES_VERSION)
                .build()
        )
    }

}