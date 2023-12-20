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

package com.loohp.hkbuseta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Stable
import com.loohp.hkbuseta.app.NearbyPage
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.shared.AndroidShared
import com.loohp.hkbuseta.utils.LocationResult
import com.loohp.hkbuseta.common.utils.ifFalse
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet


@Stable
class NearbyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidShared.ensureRegistryDataAvailable(this).ifFalse { return }
        AndroidShared.setDefaultExceptionHandler(this)

        var location: LocationResult? = null
        var exclude: ImmutableSet<String> = persistentSetOf()
        var interchangeSearch = false
        if (intent.extras != null) {
            if (intent.extras!!.containsKey("lat") && intent.extras!!.containsKey("lng")) {
                val lat = intent.extras!!.getDouble("lat")
                val lng = intent.extras!!.getDouble("lng")
                location = LocationResult.fromLatLng(lat, lng)
            }
            if (intent.extras!!.containsKey("exclude")) {
                exclude = intent.extras!!.getStringArrayList("exclude")!!.toImmutableSet()
            }
            interchangeSearch = intent.extras!!.getBoolean("interchangeSearch", false)
        }

        setContent {
            NearbyPage(location, exclude, interchangeSearch, appContext)
        }
    }

    override fun onStart() {
        super.onStart()
        AndroidShared.setSelfAsCurrentActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            AndroidShared.removeSelfFromCurrentActivity(this)
        }
    }

}