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

package com.loohp.hkbuseta.background

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.utils.dispatcherIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture
import java.util.concurrent.TimeUnit

class DailyUpdateWorker(private val context: Context, workerParams: WorkerParameters) : ListenableWorker(context, workerParams) {

    override fun startWork(): ListenableFuture<Result> {
        return CoroutineScope(dispatcherIO).async {
            val registry = Registry.getInstance(context.appContext)
            while (registry.state.value.isProcessing) {
                TimeUnit.MILLISECONDS.sleep(100)
            }
            TimeUnit.MILLISECONDS.sleep(2000)
            Result.success()
        }.asListenableFuture()
    }

}