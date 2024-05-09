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
import com.loohp.hkbuseta.app.TextElement
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.objects.BilingualFormattedText
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.ifFalse
import com.loohp.hkbuseta.shared.WearOSShared
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking


@Stable
class DismissibleTextDisplayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(appContext).ifFalse { return }
        WearOSShared.setDefaultExceptionHandler(this)

        val text = intent.extras!!.getByteArray("text")?.let { runBlocking(WearOSShared.CACHED_DISPATCHER) { BilingualFormattedText.deserialize(ByteReadChannel(it)) } }?: BilingualFormattedText.EMPTY
        val dismissText = intent.extras!!.getByteArray("dismissText")?.let { runBlocking(WearOSShared.CACHED_DISPATCHER) { BilingualText.deserialize(ByteReadChannel(it)) } }

        setContent {
            TextElement(text, dismissText, appContext)
        }
    }

    override fun onStart() {
        super.onStart()
        WearOSShared.setSelfAsCurrentActivity(this)
    }

}