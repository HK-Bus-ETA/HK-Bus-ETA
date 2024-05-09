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

package com.loohp.hkbuseta.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.utils.interpolateColor
import com.loohp.hkbuseta.compose.collectAsStateMultiplatform

object ComposeShared {

    @Composable
    fun rememberOperatorColor(primaryColor: Color, secondaryColor: Color? = null): State<Color> {
        return if (secondaryColor == null) {
            remember(primaryColor) { mutableStateOf(primaryColor) }
        } else {
            val fraction by Shared.jointOperatedColorFractionState.collectAsStateMultiplatform()
            remember(primaryColor, secondaryColor) { derivedStateOf { Color(interpolateColor(primaryColor.toArgb().toLong(), secondaryColor.toArgb().toLong(), fraction)) } }
        }
    }

}