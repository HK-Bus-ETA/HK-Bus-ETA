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

package com.loohp.hkbuseta.background

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.glance.FavouriteRoutesWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
): ListenableWorker(context, workerParams) {

    override fun startWork(): ListenableFuture<Result> {
        return CoroutineScope(Dispatchers.IO).async {
            GlanceAppWidgetManager(context)
                .getGlanceIds(FavouriteRoutesWidget.javaClass)
                .forEach { FavouriteRoutesWidget.update(context, it) }
            Result.success()
        }.asListenableFuture()
    }

}