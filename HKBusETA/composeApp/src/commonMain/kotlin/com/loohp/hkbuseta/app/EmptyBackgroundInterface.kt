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

package com.loohp.hkbuseta.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loohp.hkbuseta.common.appcontext.AppActiveContext
import com.loohp.hkbuseta.compose.PlatformIcon
import com.loohp.hkbuseta.compose.PlatformLinearProgressIndicator
import com.loohp.hkbuseta.compose.PlatformText


@Composable
fun EmptyBackgroundInterface(
    icon: Painter,
    text: String,
    subText: String? = null,
    color: Color = MaterialTheme.colorScheme.outline,
    instance: AppActiveContext
) {
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlatformIcon(
            modifier = Modifier.size(100.dp),
            tint = color,
            painter = icon,
            contentDescription = text
        )
        PlatformText(
            modifier = Modifier.fillMaxWidth(0.9F),
            fontSize = 30.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center,
            color = color,
            text = text
        )
        if (subText != null) {
            PlatformText(
                modifier = Modifier.fillMaxWidth(0.9F),
                fontSize = 22.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                color = color,
                text = subText
            )
        }
    }
}

@Composable
fun EmptyBackgroundInterfaceProgress(
    icon: Painter,
    text: String,
    subText: String? = null,
    color: Color = MaterialTheme.colorScheme.outline,
    instance: AppActiveContext
) {
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlatformIcon(
            modifier = Modifier.size(100.dp),
            tint = color,
            painter = icon,
            contentDescription = text
        )
        PlatformText(
            modifier = Modifier.fillMaxWidth(0.9F),
            fontSize = 30.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center,
            color = color,
            text = text
        )
        if (subText != null) {
            PlatformText(
                modifier = Modifier.fillMaxWidth(0.9F),
                fontSize = 22.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                color = color,
                text = subText
            )
        }
        PlatformLinearProgressIndicator(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(25.dp, 0.dp),
            color = Color(0xFFF9DE09),
            trackColor = Color(0xFF797979)
        )
    }
}