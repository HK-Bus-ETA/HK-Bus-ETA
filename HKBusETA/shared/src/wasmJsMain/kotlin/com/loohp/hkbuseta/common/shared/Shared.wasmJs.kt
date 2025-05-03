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

package com.loohp.hkbuseta.common.shared

import com.loohp.hkbuseta.common.utils.count

private val webUrlRegex = "^((?:[a-zA-Z0-9]+://)?[^/]+(?:/#)*)(.*)".toRegex()

actual const val JOINT_OPERATED_COLOR_REFRESH_RATE: Long = 10
actual val BASE_URL: String get() = href().let {
    val url = webUrlRegex.find(it)?.groupValues?.get(1)?: it
    if (url.endsWith("/#")) url else "$url/#"
}

val URL_STACK_COUNT: Int get() = (BASE_URL.count("/#") - 1).coerceAtLeast(0)
val URL_ROUTE: String get() = href().let {
    webUrlRegex.find(it)?.groupValues?.get(2)?: ""
}

fun href(): String = js("window.location.href")