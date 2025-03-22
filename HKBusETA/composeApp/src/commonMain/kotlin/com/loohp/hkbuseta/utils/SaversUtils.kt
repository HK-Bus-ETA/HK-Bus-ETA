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

package com.loohp.hkbuseta.utils

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import com.loohp.hkbuseta.common.objects.Coordinates
import com.loohp.hkbuseta.common.objects.RouteSortMode
import com.loohp.hkbuseta.common.objects.RouteSortPreference
import com.loohp.hkbuseta.common.objects.toCoordinatesOrNull

val coordinatesNullableStateSaver: Saver<MutableState<Coordinates?>, DoubleArray> get() = Saver(
    save = { it.value?.toArray() },
    restore = { mutableStateOf(it.toCoordinatesOrNull()) }
)

val routeSortPreferenceStateSaver: Saver<MutableState<RouteSortPreference>, IntArray> get() = Saver(
    save = { it.value.let { (m, f) -> intArrayOf(m.ordinal, if (f) 1 else 0) } },
    restore = { mutableStateOf(RouteSortPreference(RouteSortMode.entries[it[0]], it[1] > 0)) }
)