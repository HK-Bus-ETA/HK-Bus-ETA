package com.loohp.hkbuseta.presentation.tiles

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.EventBuilders.TileEnterEvent
import androidx.wear.tiles.EventBuilders.TileLeaveEvent
import androidx.wear.tiles.EventBuilders.TileRemoveEvent
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.presentation.shared.Shared

private const val ETA_TILE_INDEX = 1

class EtaTileServiceOne : TileService() {

    override fun onCreate() {
        Shared.setDefaultExceptionHandler(this)
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return EtaTileServiceCommon.buildTileRequest(ETA_TILE_INDEX, packageName, this)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return EtaTileServiceCommon.buildTileResourcesRequest()
    }

    override fun onTileEnterEvent(requestParams: TileEnterEvent) {
        EtaTileServiceCommon.handleTileEnterEvent(ETA_TILE_INDEX, this)
    }

    override fun onTileLeaveEvent(requestParams: TileLeaveEvent) {
        EtaTileServiceCommon.handleTileLeaveEvent(ETA_TILE_INDEX)
    }

    override fun onTileRemoveEvent(requestParams: TileRemoveEvent) {
        EtaTileServiceCommon.handleTileLeaveEvent(ETA_TILE_INDEX)
    }

}