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

package com.loohp.hkbuseta.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.google.android.horologist.compose.ambient.AmbientState
import com.google.android.horologist.compose.ambient.AmbientStateUpdate
import kotlin.random.Random


@Composable
fun rememberBurnInTranslation(
    ambientStateUpdate: AmbientStateUpdate,
    burnInOffsetPx: Int = 10
): Float {
    return remember(ambientStateUpdate) {
        when (val state = ambientStateUpdate.ambientState) {
            AmbientState.Interactive -> 0F
            is AmbientState.Ambient -> if (state.ambientDetails?.burnInProtectionRequired == true) {
                Random.nextInt(-burnInOffsetPx, burnInOffsetPx + 1).toFloat()
            } else {
                0F
            }
        }
    }
}


@Composable
fun rememberIsInAmbientMode(
    ambientStateUpdate: AmbientStateUpdate
): Boolean {
    val ambientMode by remember(ambientStateUpdate) { derivedStateOf { ambientStateUpdate.ambientState is AmbientState.Ambient } }
    return ambientMode
}


fun Modifier.ambientMode(
    ambientStateUpdate: AmbientStateUpdate
): Modifier = composed {
    val translationX = rememberBurnInTranslation(ambientStateUpdate)
    val translationY = rememberBurnInTranslation(ambientStateUpdate)

    this.graphicsLayer {
        this.translationX = translationX
        this.translationY = translationY
    }
}