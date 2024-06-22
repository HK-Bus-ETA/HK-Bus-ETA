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

package com.loohp.hkbuseta.utils

import com.loohp.hkbuseta.common.utils.dispatcherIO
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

expect fun <T> runBlockingIfPossible(context: CoroutineContext, block: suspend CoroutineScope.() -> T, orElse: () -> T): T

fun <T> runBlockingIfPossible(block: suspend CoroutineScope.() -> T, orElse: () -> T): T {
    return runBlockingIfPossible(dispatcherIO, block, orElse)
}

fun <T> runBlockingIfPossible(block: suspend CoroutineScope.() -> T): T? {
    return runBlockingIfPossible(dispatcherIO, block) { null }
}