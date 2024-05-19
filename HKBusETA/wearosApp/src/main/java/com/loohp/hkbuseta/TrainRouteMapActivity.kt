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

package com.loohp.hkbuseta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.google.android.horologist.compose.ambient.AmbientAware
import com.loohp.hkbuseta.app.TrainRouteMapInterface
import com.loohp.hkbuseta.app.TrainRouteMapType
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.compose.ambientMode
import com.loohp.hkbuseta.compose.rememberIsInAmbientMode


@Stable
class TrainRouteMapActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type = TrainRouteMapType.of(intent.extras!!.getString("type")!!)?: run {
            finish()
            return
        }

        setContent {
            AmbientAware {
                val ambientMode = rememberIsInAmbientMode(it)
                Box (
                    modifier = Modifier.ambientMode(it)
                ) {
                    TrainRouteMapInterface(appContext, ambientMode, type)
                }
            }
        }
    }

}