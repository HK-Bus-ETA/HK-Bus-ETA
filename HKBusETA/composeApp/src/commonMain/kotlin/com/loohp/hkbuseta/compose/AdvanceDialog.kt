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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.loohp.hkbuseta.common.objects.BilingualText
import com.loohp.hkbuseta.common.shared.Shared


@Composable
fun DeleteDialog(
    icon: Painter,
    title: BilingualText,
    text: BilingualText?,
    onDismissRequest: (DismissRequestType) -> Unit,
    onConfirmation: () -> Unit
) {
    PlatformAlertDialog(
        icon = {
            PlatformIcon(
                painter = icon,
                contentDescription = title[Shared.language]
            )
        },
        title = {
            PlatformText(text = title[Shared.language], textAlign = TextAlign.Center)
        },
        text = text?.let { {
            PlatformText(text = text[Shared.language])
        } },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            PlatformText(
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                text = if (Shared.language == "en") "Delete" else "刪除"
            )
        },
        onConfirm = onConfirmation,
        dismissButton = {
            PlatformText(
                text = if (Shared.language == "en") "Cancel" else "取消"
            )
        }
    )
}