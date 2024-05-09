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
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.google.android.horologist.compose.ambient.AmbientAware
import com.loohp.hkbuseta.app.AlightReminderInterface
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ifFalse
import com.loohp.hkbuseta.compose.ambientMode
import com.loohp.hkbuseta.compose.rememberIsInAmbientMode
import com.loohp.hkbuseta.shared.WearOSShared


@Stable
class AlightReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(appContext).ifFalse { return }
        WearOSShared.setDefaultExceptionHandler(this)

        setContent {
            AmbientAware {
                val ambientMode = rememberIsInAmbientMode(it)
                Box (
                    modifier = Modifier.ambientMode(it)
                ) {
                    AlightReminderInterface(appContext, ambientMode)
                }
            }
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