/*
 * This file is part of HKBusETA Phone Companion.
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.loohp.hkbuseta.common.external.extractShareLink
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SendToWatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            intent.extractUrl()?.extractShareLink()?.apply {
                RemoteActivityUtils.dataToWatch(this@SendToWatchActivity, Shared.START_ACTIVITY_ID, this, {
                    runOnUiThread {
                        Toast.makeText(this@SendToWatchActivity, R.string.send_no_watch, Toast.LENGTH_LONG).show()
                        finishAffinity()
                    }
                }, {
                    runOnUiThread {
                        Toast.makeText(this@SendToWatchActivity, R.string.send_failed, Toast.LENGTH_LONG).show()
                        finishAffinity()
                    }
                }, {
                    runOnUiThread {
                        Toast.makeText(this@SendToWatchActivity, R.string.send_success, Toast.LENGTH_LONG).show()
                        finishAffinity()
                    }
                })
            }?: runOnUiThread {
                Toast.makeText(this@SendToWatchActivity, R.string.send_success, Toast.LENGTH_LONG).show()
                finishAffinity()
            }
        }
    }
}