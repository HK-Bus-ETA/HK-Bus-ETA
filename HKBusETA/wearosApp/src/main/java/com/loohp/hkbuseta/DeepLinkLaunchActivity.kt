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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Stable


@SuppressLint("CustomSplashScreen")
@Stable
class DeepLinkLaunchActivity : ComponentActivity() {

    @SuppressLint("WearRecents")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val newIntent = Intent(this, MainActivity::class.java)
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        newIntent.action = intent.action
        when (intent.action) {
            Intent.ACTION_SEND -> if (intent.type == "text/plain") {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    newIntent.putExtra(Intent.EXTRA_TEXT, it)
                    newIntent.type = intent.type
                }
            }
            Intent.ACTION_VIEW -> {
                newIntent.data = intent.data
            }
        }
        startActivity(newIntent)
        finishAffinity()
    }

}