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
package com.loohp.hkbuseta.utils

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
fun ComponentActivity.startActivity(intent: Intent, callback: (ActivityResult) -> Unit) {
    var ref: ActivityResultLauncher<Intent>? = null
    activityResultRegistry.register(Uuid.random().toString(), ActivityResultContracts.StartActivityForResult()) {
        callback.invoke(it)
        ref?.unregister()
    }.apply { ref = this }.launch(intent)
}

fun Bundle.optString(key: String): String? {
    return get(key)?.toString()
}

fun Bundle.optInt(key: String): Int? {
    return get(key)?.toString()?.toIntOrNull()
}

fun Bundle.optBoolean(key: String): Boolean? {
    return get(key)?.toString()?.toBooleanStrictOrNull()
}