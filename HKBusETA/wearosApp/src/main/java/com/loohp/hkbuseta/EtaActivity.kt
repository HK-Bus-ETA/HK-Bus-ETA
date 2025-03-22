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

package com.loohp.hkbuseta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.google.android.horologist.compose.ambient.AmbientAware
import com.loohp.hkbuseta.app.EtaElement
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.objects.Route
import com.loohp.hkbuseta.common.objects.Stop
import com.loohp.hkbuseta.common.objects.operator
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ifFalse
import com.loohp.hkbuseta.compose.ambientMode
import com.loohp.hkbuseta.compose.rememberIsInAmbientMode
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.utils.MutableHolder
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


@Stable
class EtaActivity : ComponentActivity() {

    private val sync: ExecutorService = Executors.newSingleThreadExecutor()
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val etaUpdatesMap: MutableHolder<Pair<ScheduledFuture<*>?, () -> Unit>> = MutableHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(appContext).ifFalse { return }
        WearOSShared.setDefaultExceptionHandler(this)

        val stopId = intent.extras!!.getString("stopId")
        val co = intent.extras!!.getString("co")?.operator
        val index = intent.extras!!.getInt("index")
        val stop = intent.extras!!.getByteArray("stop")?.let { runBlocking(WearOSShared.CACHED_DISPATCHER) { Stop.deserialize(ByteReadChannel(it)) } }?:
            intent.extras!!.getString("stopStr")?.let { Stop.deserialize(Json.decodeFromString<JsonObject>(it)) }
        val route = intent.extras!!.getByteArray("route")?.let { runBlocking(WearOSShared.CACHED_DISPATCHER) { Route.deserialize(ByteReadChannel(it)) } }?:
            intent.extras!!.getString("routeStr")?.let { Route.deserialize(Json.decodeFromString<JsonObject>(it)) }
        if (stopId == null || co == null || stop == null || route == null) {
            throw RuntimeException()
        }
        val offsetStart = intent.extras!!.getInt("offset", 0)

        setContent {
            AmbientAware {
                val ambientMode = rememberIsInAmbientMode(it)
                Box (
                    modifier = Modifier.ambientMode(it)
                ) {
                    EtaElement(ambientMode, stopId, co, index, stop, route, offsetStart, appContext) { isAdd, task ->
                        sync.execute {
                            if (isAdd) {
                                etaUpdatesMap.computeIfAbsent { executor.scheduleWithFixedDelay(task, 0, Shared.ETA_UPDATE_INTERVAL.toLong(), TimeUnit.MILLISECONDS) to task!! }
                            } else {
                                etaUpdatesMap.remove()?.first?.cancel(true)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        WearOSShared.setSelfAsCurrentActivity(this)
    }

    override fun onResume() {
        super.onResume()
        sync.execute {
            etaUpdatesMap.replace { value ->
                value.first?.cancel(true)
                executor.scheduleWithFixedDelay(value.second, 0, Shared.ETA_UPDATE_INTERVAL.toLong(), TimeUnit.MILLISECONDS) to value.second
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sync.execute {
            etaUpdatesMap.ifPresent { it.first?.cancel(true) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            executor.shutdownNow()
            sync.shutdownNow()
            WearOSShared.removeSelfFromCurrentActivity(this)
        }
    }

}