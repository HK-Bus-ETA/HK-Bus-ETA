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
import com.loohp.hkbuseta.app.Message
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.shared.Shared
import kotlinx.coroutines.runBlocking


@Stable
class FatalErrorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val zh = intent.extras?.getString("zh")
        val en = intent.extras?.getString("en")
        val exception = intent.extras?.getString("exception")

        runBlocking { Shared.invalidateCache(appContext) }

        setContent {
            Message(appContext, zh, en, exception)
        }
    }
}