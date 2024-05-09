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

package com.loohp.hkbuseta.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember

@Composable
expect fun RestartEffect(onRestart: () -> Unit)

@Composable
expect fun PauseEffect(onPause: () -> Unit)

@Composable
inline fun ImmediateEffect(key1: Any?, crossinline effect: @DisallowComposableCalls () -> Unit) = remember(key1, effect)

@Composable
inline fun ImmediateEffect(key1: Any?, key2: Any?, crossinline effect: @DisallowComposableCalls () -> Unit) = remember(key1, key2, effect)

@Composable
inline fun ImmediateEffect(key1: Any?, key2: Any?, key3: Any?, crossinline effect: @DisallowComposableCalls () -> Unit) = remember(key1, key2, key3, effect)

@Composable
inline fun ImmediateEffect(vararg keys: Any?, crossinline effect: @DisallowComposableCalls () -> Unit) = remember(keys = keys, effect)