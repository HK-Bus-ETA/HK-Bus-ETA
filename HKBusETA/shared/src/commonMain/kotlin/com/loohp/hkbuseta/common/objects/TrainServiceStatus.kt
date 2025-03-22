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

package com.loohp.hkbuseta.common.objects

data class TrainServiceStatus(
    val type: TrainServiceStatusType,
    val messages: TrainServiceStatusMessage?
)

data class TrainServiceStatusMessage(
    val title: BilingualText,
    val status: TrainServiceStatusMessageStatus,
    val url: BilingualText,
    val urlMobile: BilingualText
) {
    fun url(mobile: Boolean): BilingualText {
        return if (mobile) urlMobile else url
    }
}

enum class TrainServiceStatusType {
    NORMAL, NON_SERVICE_HOUR, TYPHOON, DISRUPTION
}

enum class TrainServiceStatusMessageStatus {
    NORMAL, RED
}