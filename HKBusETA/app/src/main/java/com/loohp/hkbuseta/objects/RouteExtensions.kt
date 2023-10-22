/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

package com.loohp.hkbuseta.objects

import androidx.compose.ui.graphics.Color
import com.loohp.hkbuseta.shared.KMBSubsidiary
import com.loohp.hkbuseta.shared.Shared


fun CharSequence.coColor(routeNumber: String, elseColor: Color): Color {
    return when (this) {
        "kmb" -> if (Shared.getKMBSubsidiary(routeNumber) == KMBSubsidiary.LWB) Color(0xFFF26C33) else Color(0xFFFF4747)
        "ctb" -> Color(0xFFFFE15E)
        "nlb" -> Color(0xFF9BFFC6)
        "mtr-bus" -> Color(0xFFAAD4FF)
        "gmb" -> Color(0xFF36FF42)
        "lightRail" -> Color(0xFFD3A809)
        "mtr" -> {
            when (routeNumber) {
                "AEL" -> Color(0xFF00888E)
                "TCL" -> Color(0xFFF3982D)
                "TML" -> Color(0xFF9C2E00)
                "TKL" -> Color(0xFF7E3C93)
                "EAL" -> Color(0xFF5EB7E8)
                "SIL" -> Color(0xFFCBD300)
                "TWL" -> Color(0xFFE60012)
                "ISL" -> Color(0xFF0075C2)
                "KTL" -> Color(0xFF00A040)
                "DRL" -> Color(0xFFEB6EA5)
                else -> Color.LightGray
            }
        }
        else -> elseColor
    }
}

fun CharSequence.displayRouteNumber(routeNumber: String): String {
    return if (this == "mtr") {
        Shared.getMtrLineName(routeNumber, "???")
    } else if (this == "kmb" && Shared.getKMBSubsidiary(routeNumber) == KMBSubsidiary.SUNB) {
        "NR".plus(routeNumber)
    } else {
        routeNumber
    }
}

fun CharSequence.coName(routeNumber: String, kmbCtbJoint: Boolean, language: String): String {
    return if (language == "en") {
        when (this) {
            "kmb" -> when (Shared.getKMBSubsidiary(routeNumber)) {
                KMBSubsidiary.SUNB -> "Sun-Bus"
                KMBSubsidiary.LWB -> if (kmbCtbJoint) "LWB/CTB" else "LWB"
                else -> if (kmbCtbJoint) "KMB/CTB" else "KMB"
            }
            "ctb" -> "CTB"
            "nlb" -> "NLB"
            "mtr-bus" -> "MTR-Bus"
            "gmb" -> "GMB"
            "lightRail" -> "LRT"
            "mtr" -> "MTR"
            else -> "???"
        }
    } else {
        when (this) {
            "kmb" -> when (Shared.getKMBSubsidiary(routeNumber)) {
                KMBSubsidiary.SUNB -> "陽光巴士"
                KMBSubsidiary.LWB -> if (kmbCtbJoint) "龍運/城巴" else "龍運"
                else -> if (kmbCtbJoint) "九巴/城巴" else "九巴"
            }
            "ctb" -> "城巴"
            "nlb" -> "嶼巴"
            "mtr-bus" -> "港鐵巴士"
            "gmb" -> "專線小巴"
            "lightRail" -> "輕鐵"
            "mtr" -> "港鐵"
            else -> "???"
        }
    }
}