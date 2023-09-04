package com.loohp.hkbuseta.presentation.tiles

import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture

private const val ETA_TILE_INDEX = 2

class EtaTileServiceTwo : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return EtaTileServiceCommon.buildTileRequest(ETA_TILE_INDEX, packageName, this)
    }

}