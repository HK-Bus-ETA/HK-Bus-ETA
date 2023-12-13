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
import com.loohp.hkbuseta.objects.BilingualText
import com.loohp.hkbuseta.objects.withEn
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.utils.ifFalse
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking


private val defaultDismissText = "確認" withEn "OK"

@Stable
class DismissibleTextDisplayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.ensureRegistryDataAvailable(this).ifFalse { return }
        Shared.setDefaultExceptionHandler(this)

        val text = intent.extras!!.getByteArray("text")?.let { runBlocking { BilingualText.deserialize(ByteReadChannel(it)) } }?: BilingualText.EMPTY
        val dismissText = intent.extras!!.getByteArray("dismissText")?.let { runBlocking { BilingualText.deserialize(ByteReadChannel(it)) } }?: defaultDismissText

        setContent {
            TextElement(text, dismissText, appContext)
        }
    }

    override fun onStart() {
        super.onStart()
        Shared.setSelfAsCurrentActivity(this)
    }

}