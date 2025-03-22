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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirst
import kotlin.math.max


@Composable
fun AdvanceTabRow(
    selectedTabIndex: Int,
    totalTabSize: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 52.dp,
    scrollable: Boolean,
    widestTabWidth: Dp,
    tabs: @Composable (Boolean) -> Unit
) {
    if (scrollable) {
        PlatformScrollableTabRow(selectedTabIndex, modifier, edgePadding, totalTabSize, widestTabWidth) { tabs.invoke(true) }
    } else {
        PlatformTabRow(selectedTabIndex, modifier) { tabs.invoke(false) }
    }
}

@Composable
fun TabBaselineLayout(
    text: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?
) {
    Layout({
        if (text != null) {
            Box(
                modifier = Modifier
                    .layoutId("text")
                    .padding(horizontal = 5.dp)
            ) {
                text.invoke()
            }
        }
        if (icon != null) {
            Box(
                modifier = Modifier.layoutId("icon")
            ) {
                icon.invoke()
            }
        }
    }) { measurables, constraints ->
        val textPlaceable = text?.let {
            measurables.fastFirst { it.layoutId == "text" }.measure(
                // Measure with loose constraints for height as we don't want the text to take up more
                // space than it needs
                constraints.copy(minHeight = 0)
            )
        }

        val iconPlaceable = icon?.let {
            measurables.fastFirst { it.layoutId == "icon" }.measure(constraints)
        }

        val tabWidth = max(textPlaceable?.width?: 0, iconPlaceable?.width?: 0)

        val tabHeight = max(48.dp.roundToPx(), (iconPlaceable?.height?: 0) + (textPlaceable?.height?: 0) + 10.sp.roundToPx())

        val firstBaseline = textPlaceable?.get(FirstBaseline)
        val lastBaseline = textPlaceable?.get(LastBaseline)

        layout(tabWidth, tabHeight) {
            when {
                textPlaceable != null && iconPlaceable != null -> placeTextAndIcon(
                    density = this@Layout,
                    textPlaceable = textPlaceable,
                    iconPlaceable = iconPlaceable,
                    tabWidth = tabWidth,
                    tabHeight = tabHeight,
                    firstBaseline = firstBaseline!!,
                    lastBaseline = lastBaseline!!
                )
                textPlaceable != null -> placeTextOrIcon(textPlaceable, tabHeight)
                iconPlaceable != null -> placeTextOrIcon(iconPlaceable, tabHeight)
                else -> { /* do nothing */ }
            }
        }
    }
}

private fun Placeable.PlacementScope.placeTextOrIcon(
    textOrIconPlaceable: Placeable,
    tabHeight: Int
) {
    val contentY = (tabHeight - textOrIconPlaceable.height) / 2
    textOrIconPlaceable.placeRelative(0, contentY)
}

private fun Placeable.PlacementScope.placeTextAndIcon(
    density: Density,
    textPlaceable: Placeable,
    iconPlaceable: Placeable,
    tabWidth: Int,
    tabHeight: Int,
    firstBaseline: Int,
    lastBaseline: Int
) {
    // Total offset between the last text baseline and the bottom of the Tab layout
    val textOffset = with(density) { 8.5F.dp.roundToPx() }

    // How much space there is between the top of the icon (essentially the top of this layout)
    // and the top of the text layout's bounding box (not baseline)
    val iconOffset = with(density) { iconPlaceable.height + 17.sp.roundToPx() - firstBaseline }

    val textPlaceableX = (tabWidth - textPlaceable.width) / 2
    val textPlaceableY = tabHeight - lastBaseline - textOffset
    textPlaceable.placeRelative(textPlaceableX, textPlaceableY)

    val iconPlaceableX = (tabWidth - iconPlaceable.width) / 2
    val iconPlaceableY = textPlaceableY - iconOffset
    iconPlaceable.placeRelative(iconPlaceableX, iconPlaceableY)
}