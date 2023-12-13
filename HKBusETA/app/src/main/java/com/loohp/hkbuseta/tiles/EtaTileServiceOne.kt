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

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.EventBuilders.TileEnterEvent
import androidx.wear.tiles.EventBuilders.TileLeaveEvent
import androidx.wear.tiles.EventBuilders.TileRemoveEvent
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.shared.Shared

private const val ETA_TILE_INDEX = 1 or Int.MIN_VALUE

class EtaTileServiceOne : TileService() {

    override fun onCreate() {
        Shared.setDefaultExceptionHandler(this)
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return EtaTileServiceCommon.buildTileRequest(ETA_TILE_INDEX, packageName, appContext)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return EtaTileServiceCommon.buildTileResourcesRequest()
    }

    override fun onTileEnterEvent(requestParams: TileEnterEvent) {
        EtaTileServiceCommon.handleTileEnterEvent(ETA_TILE_INDEX, appContext)
    }

    override fun onTileLeaveEvent(requestParams: TileLeaveEvent) {
        EtaTileServiceCommon.handleTileLeaveEvent(ETA_TILE_INDEX)
    }

    override fun onTileRemoveEvent(requestParams: TileRemoveEvent) {
        EtaTileServiceCommon.handleTileLeaveEvent(ETA_TILE_INDEX)
    }

}