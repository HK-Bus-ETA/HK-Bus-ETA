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
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.shared.Shared

class EtaTileService : TileService() {

    override fun onCreate() {
        Shared.setDefaultExceptionHandler(this)
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return EtaTileServiceCommon.buildTileRequest(requestParams.tileId, packageName, appContext)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return EtaTileServiceCommon.buildTileResourcesRequest()
    }

    override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) {
        EtaTileServiceCommon.handleTileEnterEvent(requestParams.tileId, appContext)
    }

    override fun onTileLeaveEvent(requestParams: EventBuilders.TileLeaveEvent) {
        EtaTileServiceCommon.handleTileLeaveEvent(requestParams.tileId)
    }

    override fun onTileRemoveEvent(requestParams: EventBuilders.TileRemoveEvent) {
        EtaTileServiceCommon.handleTileRemoveEvent(requestParams.tileId, appContext)
    }

}