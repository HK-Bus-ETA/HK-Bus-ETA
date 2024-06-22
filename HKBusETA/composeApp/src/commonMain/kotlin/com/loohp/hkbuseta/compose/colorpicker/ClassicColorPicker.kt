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

package com.loohp.hkbuseta.compose.colorpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


/**
 * Classic Color Picker Component that shows a HSV representation of a color, with a Hue Bar on the right,
 * Alpha Bar on the bottom and the rest of the area covered with an area with saturation value touch area.
 *
 * @param modifier modifiers to set to this color picker.
 * @param color the initial color to set on the picker.
 * @param showAlphaBar whether or not to show the bottom alpha bar on the color picker.
 * @param onColorChanged callback that is triggered when the color changes
 *
 */
@Composable
fun ClassicColorPicker(
    modifier: Modifier = Modifier,
    color: HsvColor = HsvColor.from(Color.Red),
    showAlphaBar: Boolean = true,
    onColorChanged: (HsvColor) -> Unit
) {
    val colorPickerValueState = rememberSaveable(stateSaver = HsvColor.Saver) {
        mutableStateOf(color)
    }
    LaunchedEffect (colorPickerValueState.value) {
        onColorChanged.invoke(colorPickerValueState.value)
    }
    ClassicColorPicker(
        modifier = modifier,
        colorPickerValueState = colorPickerValueState,
        showAlphaBar = showAlphaBar
    )
}

/**
 * Classic Color Picker Component that shows a HSV representation of a color, with a Hue Bar on the right,
 * Alpha Bar on the bottom and the rest of the area covered with an area with saturation value touch area.
 *
 * @param modifier modifiers to set to this color picker.
 * @param colorPickerValueState the state of the color to set on the picker.
 * @param showAlphaBar whether or not to show the bottom alpha bar on the color picker.
 *
 */
@Composable
fun ClassicColorPicker(
    modifier: Modifier = Modifier,
    colorPickerValueState: MutableState<HsvColor>,
    showAlphaBar: Boolean = true
) {
    Row(modifier = modifier) {
        val barThickness = 32.dp
        val paddingBetweenBars = 8.dp
        Column(modifier = Modifier.weight(0.8f)) {
            SaturationValueArea(
                modifier = Modifier.weight(0.8f),
                currentColor = colorPickerValueState.value,
                onSaturationValueChanged = { saturation, value ->
                    colorPickerValueState.value =
                        colorPickerValueState.value.copy(saturation = saturation, value = value)
                }
            )
            if (showAlphaBar) {
                Spacer(modifier = Modifier.height(paddingBetweenBars))
                AlphaBar(
                    modifier = Modifier.height(barThickness),
                    currentColor = colorPickerValueState.value,
                    onAlphaChanged = { alpha ->
                        colorPickerValueState.value = colorPickerValueState.value.copy(alpha = alpha)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.width(paddingBetweenBars))
        HueBar(
            modifier = Modifier.width(barThickness),
            currentColor = colorPickerValueState.value,
            onHueChanged = { newHue ->
                colorPickerValueState.value = colorPickerValueState.value.copy(hue = newHue)
            }
        )
    }
}
