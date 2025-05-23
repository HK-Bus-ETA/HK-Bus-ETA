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
import androidx.compose.runtime.Stable
import com.loohp.hkbuseta.app.NearbyPage
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.objects.Operator
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.shared.WearOSShared
import com.loohp.hkbuseta.common.utils.LocationResult
import com.loohp.hkbuseta.common.utils.buildImmutableMap
import com.loohp.hkbuseta.common.utils.ifFalse
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet


@Stable
class NearbyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(appContext).ifFalse { return }
        WearOSShared.setDefaultExceptionHandler(this)

        var location: LocationResult? = null
        var exclude: ImmutableMap<Operator, MutableSet<String>> = persistentMapOf()
        var interchangeSearch = false
        if (intent.extras != null) {
            if (intent.extras!!.containsKey("lat") && intent.extras!!.containsKey("lng")) {
                val lat = intent.extras!!.getDouble("lat")
                val lng = intent.extras!!.getDouble("lng")
                location = LocationResult.of(lat, lng)
            }
            if (intent.extras!!.containsKey("exclude")) {
                val excludeList = intent.extras!!.getStringArrayList("exclude")!!
                exclude = buildImmutableMap {
                    for (entry in excludeList) {
                        val (co, routeNumber) = entry.split(",")
                        getOrPut(Operator.valueOf(co)) { mutableSetOf() }.add(routeNumber)
                    }
                }
            }
            interchangeSearch = intent.extras!!.getBoolean("interchangeSearch", false)
        }

        setContent {
            NearbyPage(location, exclude, interchangeSearch, appContext)
        }
    }

    override fun onStart() {
        super.onStart()
        WearOSShared.setSelfAsCurrentActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            WearOSShared.removeSelfFromCurrentActivity(this)
        }
    }

}