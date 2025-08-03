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

package com.loohp.hkbuseta.shared

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.loohp.hkbuseta.FatalErrorActivity
import com.loohp.hkbuseta.R
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.background.DailyUpdateWorker
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.common.appcontext.globalWritingFilesCounterState
import com.loohp.hkbuseta.common.objects.AppAlert
import com.loohp.hkbuseta.common.services.AlightReminderRemoteData
import com.loohp.hkbuseta.common.shared.Registry
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.MutableNullableStateFlow
import com.loohp.hkbuseta.common.utils.currentTimeMillis
import com.loohp.hkbuseta.common.utils.interpolateColor
import com.loohp.hkbuseta.common.utils.nextScheduledDataUpdateMillis
import com.loohp.hkbuseta.compose.collectAsStateWithLifecycle
import com.loohp.hkbuseta.compose.firstVisibleItemIndex
import com.loohp.hkbuseta.compose.firstVisibleItemScrollOffset
import com.loohp.hkbuseta.utils.HongKongTimeSource
import com.loohp.hkbuseta.utils.adjustAlpha
import com.loohp.hkbuseta.utils.dp
import com.loohp.hkbuseta.utils.isEqualTo
import com.loohp.hkbuseta.utils.sp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
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
object WearOSShared {

    val CACHED_DISPATCHER: CoroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()

    val RESOURCE_RATIO: Map<Int, Float> = mapOf(
        R.mipmap.lrv to 128F / 95F,
        R.mipmap.lrv_empty to 128F / 95F
    )

    private val appAlertsState: MutableStateFlow<AppAlert?> = MutableStateFlow(null)

    @Composable
    fun rememberAppAlert(context: AppActiveContext): State<AppAlert?> {
        LaunchedEffect (Unit) {
            while (true) {
                appAlertsState.value = Registry.getInstance(context).getAppAlerts().await()
                delay(30000)
            }
        }
        return appAlertsState.collectAsStateWithLifecycle()
    }

    @Composable
    fun MainTime(lazyListState: LazyListState, modifier: Modifier = Modifier) {
        val hidden by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0 } }
        MainTime(hidden, modifier)
    }

    @Composable
    fun MainTime(lazyColumnState: TransformingLazyColumnState, modifier: Modifier = Modifier) {
        val hidden by remember { derivedStateOf { lazyColumnState.firstVisibleItemIndex > 0 || lazyColumnState.firstVisibleItemScrollOffset > 0 } }
        MainTime(hidden, modifier)
    }

    @Composable
    fun MainTime(scrollState: ScrollState, modifier: Modifier = Modifier) {
        val hidden by remember { derivedStateOf { scrollState.value > 0 } }
        MainTime(hidden, modifier)
    }

    @Composable
    fun MainTime(hidden: Boolean, modifier: Modifier = Modifier) {
        val hiddenOffset by animateDpAsState(
            targetValue = if (hidden) (-100).dp else 0.dp,
            animationSpec = tween(300),
            label = "HiddenOffset"
        )
        MainTime(modifier.offset { IntOffset(0, hiddenOffset.roundToPx()) })
    }

    private val globalWritingFilesState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Composable
    fun MainTime(modifier: Modifier = Modifier) {
        val globalWritingFilesCounter by globalWritingFilesCounterState.collectAsStateWithLifecycle()
        var globalWritingFiles by globalWritingFilesState.collectAsStateWithLifecycle()
        val globalWritingFilesIndicatorAlpha by animateFloatAsState(
            targetValue = if (globalWritingFiles && globalWritingFilesCounter > 0) 1F else 0F,
            animationSpec = tween(300),
            label = "GlobalWritingFilesIndicatorAlpha"
        )
        LaunchedEffect (globalWritingFilesCounter) {
            if (globalWritingFilesCounter > 0) {
                delay(500)
                globalWritingFiles = true
            } else {
                globalWritingFiles = false
            }
        }
        val indicator: (@Composable () -> Unit)? = if (globalWritingFilesIndicatorAlpha > 0F) ({
            Spacer(
                modifier = Modifier
                    .size(10F.sp.dp)
                    .shadow(5.dp, CircleShape)
                    .background(Color.Green.adjustAlpha(globalWritingFilesIndicatorAlpha), CircleShape),
            )
        }) else null
        TimeText(
            modifier = modifier.fillMaxWidth(),
            timeSource = HongKongTimeSource(TimeTextDefaults.timeFormat()),
            timeTextStyle = TimeTextDefaults.timeTextStyle().merge(fontSize = 15.dp.sp),
            endCurvedContent = indicator?.let { { curvedComposable { it.invoke() } } },
            endLinearContent = indicator
        )
    }

    @SuppressLint("WearRecents")
    fun setDefaultExceptionHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                runBlocking { Shared.invalidateCache(context.appContext) }
                if (context is Activity) {
                    var stacktrace = throwable.stackTraceToString()
                    if (stacktrace.length > 459000) {
                        stacktrace = stacktrace.substring(0, 459000) + "..."
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
    fun rememberOperatorColor(primaryColor: Color, secondaryColor: Color? = null): State<Color> {
        return if (secondaryColor == null) {
            remember(primaryColor) { mutableStateOf(primaryColor) }
        } else {
            val fraction by Shared.jointOperatedColorFractionState.collectAsStateWithLifecycle()
            remember(primaryColor, secondaryColor) { derivedStateOf { Color(interpolateColor(primaryColor.toArgb().toLong(), secondaryColor.toArgb().toLong(), fraction)) } }
        }
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
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED, requiresBatteryNotLow = true))
            .setInitialDelay((time?: nextScheduledDataUpdateMillis()) + 3600000 - currentTimeMillis(), TimeUnit.MILLISECONDS)
            .build()
        val existingPolicy = time?.let { ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE }?: ExistingPeriodicWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(BACKGROUND_SERVICE_REQUEST_TAG, existingPolicy, updateRequest)
    }

    val remoteAlightReminderService: MutableNullableStateFlow<AlightReminderRemoteData> = MutableNullableStateFlow(null)

    fun registryNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alightReminderChannel = NotificationChannel(
            "alight_reminder_channel",
            context.resources.getString(R.string.alight_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(alightReminderChannel)

        val generalChannel = NotificationChannel(
            "general_channel",
            context.resources.getString(R.string.general_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(generalChannel)

        Firebase.messaging.subscribeToTopic("General")
        Firebase.messaging.subscribeToTopic("Refresh")
    }

}