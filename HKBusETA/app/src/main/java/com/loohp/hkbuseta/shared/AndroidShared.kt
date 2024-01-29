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

package com.loohp.hkbuseta.shared

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.loohp.hkbuseta.FatalErrorActivity
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.background.DailyUpdateWorker
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.interpolateColor
import com.loohp.hkbuseta.common.utils.nextScheduledDataUpdateMillis
import com.loohp.hkbuseta.utils.HongKongTimeSource
import com.loohp.hkbuseta.utils.isEqualTo
import com.loohp.hkbuseta.utils.sp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

data class CurrentActivityData(val cls: Class<Activity>, val extras: Bundle?, val shouldRelaunch: Boolean = extras?.getBoolean("shouldRelaunch", true)?: true) {

    fun isEqualTo(other: Any?): Boolean {
        return if (other is CurrentActivityData) {
            this.cls == other.cls && this.shouldRelaunch == other.shouldRelaunch && ((this.extras == null && other.extras == null) || (this.extras != null && this.extras.isEqualTo(other.extras)))
        } else {
            false
        }
    }

}

@Immutable
object AndroidShared {

    val CACHED_DISPATCHER: CoroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()

    val RESOURCE_RATIO: Map<Int, Float> = mapOf(
        R.mipmap.lrv to 128F / 95F,
        R.mipmap.lrv_empty to 128F / 95F
    )

    @Composable
    fun MainTime() {
        TimeText(
            modifier = Modifier.fillMaxWidth(),
            timeSource = HongKongTimeSource(TimeTextDefaults.timeFormat()),
            timeTextStyle = TimeTextDefaults.timeTextStyle().merge(fontSize = 15.dp.sp)
        )
    }

    @SuppressLint("WearRecents")
    fun setDefaultExceptionHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Shared.invalidateCache(context.appContext)
                if (context is Activity) {
                    var stacktrace = throwable.stackTraceToString()
                    if (stacktrace.length > 459000) {
                        stacktrace = stacktrace.substring(0, 459000).plus("...")
                    }
                    val intent = Intent(context, FatalErrorActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("exception", stacktrace)
                    context.startActivity(intent)
                }
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
                throw throwable
            }
        }
    }

    @Composable
    fun rememberOperatorColor(primaryColor: Color, secondaryColor: Color? = null): Color {
        return secondaryColor?.let {
            val fraction by Shared.jointOperatedColorFractionState.collectAsStateWithLifecycle()
            val color by remember { derivedStateOf { Color(interpolateColor(primaryColor.toArgb().toLong(), it.toArgb().toLong(), fraction)) } }
            return color
        }?: primaryColor
    }

    private var currentActivity: AtomicReference<CurrentActivityData?> = AtomicReference(null)

    fun getCurrentActivity(): CurrentActivityData? {
        return currentActivity.get()
    }

    fun setSelfAsCurrentActivity(activity: Activity) {
        currentActivity.set(CurrentActivityData(activity.javaClass, activity.intent.extras))
    }

    fun removeSelfFromCurrentActivity(activity: Activity) {
        val data = CurrentActivityData(activity.javaClass, activity.intent.extras)
        currentActivity.updateAndGet { if (it != null && it.isEqualTo(data)) null else it }
    }

    fun restoreCurrentScreenOrRun(context: AppActiveContext, runBehindAnyway: Boolean, orElse: () -> Unit) {
        val currentActivity = getCurrentActivity()
        if (currentActivity == null || !currentActivity.shouldRelaunch) {
            orElse.invoke()
        } else {
            val intent2 = Intent(context.context, currentActivity.cls)
            if (currentActivity.extras != null) {
                intent2.putExtras(currentActivity.extras)
            }
            if (runBehindAnyway) {
                orElse.invoke()
            }
            context.context.startActivity(intent2)
            context.context.finishAffinity()
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