package com.loohp.hkbuseta.presentation.tiles

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture

private const val ETA_TILE_INDEX = 2

class EtaTileServiceTwo : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return EtaTileServiceCommon.buildTileRequest(ETA_TILE_INDEX, packageName, this)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return EtaTileServiceCommon.buildTileResourcesRequest()
    }

    override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) {
        EtaTileServiceCommon.handleTileEnterEvent(ETA_TILE_INDEX, this)
    }

    override fun onTileLeaveEvent(requestParams: EventBuilders.TileLeaveEvent) {
        EtaTileServiceCommon.handleTileLeaveEvent(ETA_TILE_INDEX)
    }

    override fun onTileRemoveEvent(requestParams: EventBuilders.TileRemoveEvent) {
        EtaTileServiceCommon.handleTileLeaveEvent(ETA_TILE_INDEX)
    }

}