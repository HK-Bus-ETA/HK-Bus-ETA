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

/**
 * A representation of Color in Hue, Saturation and Value form.
 */
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.Color
import com.github.ajalt.colormath.model.HSV
import com.github.ajalt.colormath.model.RGB

/**
 * A representation of Color in Hue, Saturation and Value form.
 */
data class HsvColor(

    // from = 0.0, to = 360.0
    val hue: Float,

    // from = 0.0, to = 1.0
    val saturation: Float,

    // from = 0.0, to = 1.0
    val value: Float,

    // from = 0.0, to = 1.0
    val alpha: Float
) {

    fun toColor(): Color {
        val hsv = HSV(hue, saturation, value, alpha)
        val rgb = hsv.toSRGB()
        return Color(rgb.redInt, rgb.greenInt, rgb.blueInt, rgb.alphaInt)
    }

    fun getComplementaryColor(): List<HsvColor> {
        return listOf(
            this.copy(saturation = (saturation + 0.1f).coerceAtMost(1f), value = (value + 0.3f).coerceIn(0.0f, 1f)),
            this.copy(saturation = (saturation - 0.1f).coerceAtMost(1f), value = (value - 0.3f).coerceIn(0.0f, 1f)),
            this.copy(hue = (hue + 180) % 360), // actual complementary
            this.copy(hue = (hue + 180) % 360, saturation = (saturation + 0.2f).coerceAtMost(1f), value = (value - 0.3f).coerceIn(0.0f, 1f))
        )
    }

    fun getSplitComplementaryColors(): List<HsvColor> {
        return listOf(
            this.copy(hue = (hue + 150) % 360, saturation = (saturation - 0.05f).coerceIn(0.0f, 1f), value = (value - 0.3f).coerceIn(0.0f, 1f)),
            this.copy(hue = (hue + 210) % 360, saturation = (saturation - 0.05f).coerceIn(0.0f, 1f), value = (value - 0.3f).coerceIn(0.0f, 1f)),
            this.copy(hue = (hue + 150) % 360), // actual
            this.copy(hue = (hue + 210) % 360) // actual
        )
    }

    fun getTriadicColors(): List<HsvColor> {
        return listOf(
            this.copy(hue = (hue + 120) % 360, saturation = (saturation - 0.05f).coerceIn(0.0f, 1f), value = (value - 0.3f).coerceIn(0.0f, 1f)),
            this.copy(hue = (hue + 120) % 360),
            this.copy(hue = (hue + 240) % 360, saturation = (saturation - 0.05f).coerceIn(0.0f, 1f), value = (value - 0.3f).coerceIn(0.0f, 1f)),
            this.copy(hue = (hue + 240) % 360)
        )
    }

    fun getTetradicColors(): List<HsvColor> {
        return listOf(
            this.copy(saturation = (saturation + 0.2f).coerceIn(0.0f, 1f)), // bonus one
            this.copy(hue = (hue + 90) % 360),
            this.copy(hue = (hue + 180) % 360),
            this.copy(hue = (hue + 270) % 360)
        )
    }

    fun getAnalagousColors(): List<HsvColor> {
        return listOf(
            this.copy(hue = (hue + 30) % 360),
            this.copy(hue = (hue + 60) % 360),
            this.copy(hue = (hue + 90) % 360),
            this.copy(hue = (hue + 120) % 360)
        )
    }

    fun getMonochromaticColors(): List<HsvColor> {
        return listOf(
            this.copy(saturation = (saturation + 0.2f).mod(1f)),
            this.copy(saturation = (saturation + 0.4f).mod(1f)),
            this.copy(saturation = (saturation + 0.6f).mod(1f)),
            this.copy(saturation = (saturation + 0.8f).mod(1f))
        )
    }

    fun getShadeColors(): List<HsvColor> {
        return listOf(
            this.copy(value = (value - 0.10f).mod(1.0f).coerceAtLeast(0.2f)),
            this.copy(value = (value + 0.55f).mod(1.0f).coerceAtLeast(0.55f)),
            this.copy(value = (value + 0.30f).mod(1.0f).coerceAtLeast(0.3f)),
            this.copy(value = (value + 0.05f).mod(1.0f).coerceAtLeast(0.2f))
        )
    }

    companion object {

        val DEFAULT = HsvColor(360f, 1.0f, 1.0f, 1.0f)

        /**
         *  the color math hsv to local hsv color
         */
        private fun HSV.toColor(): HsvColor {
            return HsvColor(
                hue = if (this.h.isNaN()) 0f else this.h,
                saturation = this.s,
                value = this.v,
                alpha = this.alpha
            )
        }

        fun from(color: Color): HsvColor {
            return RGB(
                color.red,
                color.green,
                color.blue,
                color.alpha
            ).toHSV().toColor()
        }

        val Saver: Saver<HsvColor, *> = listSaver(
            save = {
                listOf(
                    it.hue,
                    it.saturation,
                    it.value,
                    it.alpha
                )
            },
            restore = {
                HsvColor(
                    it[0],
                    it[1],
                    it[2],
                    it[3]
                )
            }
        )
    }
}
