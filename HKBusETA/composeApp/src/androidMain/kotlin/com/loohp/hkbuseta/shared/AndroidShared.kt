/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.shared

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.loohp.hkbuseta.appcontext.applicationAppContext
import com.loohp.hkbuseta.background.DailyUpdateWorker
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.nextScheduledDataUpdateMillis
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit


object AndroidShared {

    fun setDefaultExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                runBlocking { Shared.invalidateCache(applicationAppContext) }
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
                throw throwable
            }
        }
    }

    private const val BACKGROUND_SERVICE_REQUEST_TAG: String = "HK_BUS_ETA_BG_SERVICE"

    fun scheduleBackgroundUpdateService(context: Context, time: Long? = null) {
        val updateRequest = PeriodicWorkRequestBuilder<DailyUpdateWorker>(1, TimeUnit.DAYS, 60, TimeUnit.MINUTES)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setInitialDelay((time?: nextScheduledDataUpdateMillis()) + 3600000 - currentTimeMillis(), TimeUnit.MILLISECONDS)
            .build()
        val existingPolicy = time?.let { ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE }?: ExistingPeriodicWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(BACKGROUND_SERVICE_REQUEST_TAG, existingPolicy, updateRequest)
    }

}