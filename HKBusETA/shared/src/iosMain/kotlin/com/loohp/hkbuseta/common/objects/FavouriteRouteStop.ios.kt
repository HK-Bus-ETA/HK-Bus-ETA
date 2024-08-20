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

package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.appcontext.applicationBaseAppContext
import com.loohp.hkbuseta.common.utils.ColorContentStyle
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


actual val FavouriteRouteStop.platformDisplayInfo: JsonObject? get() = buildJsonObject {
    put("routeNumber", co.getDisplayRouteNumber(route.routeNumber, false))
    put("co", co.name)
    put("coDisplay-zh", buildJsonArray {
        co.getDisplayFormattedName(route.routeNumber, route.isKmbCtbJoint, route.gmbRegion, "zh").content.forEach {
            add(buildJsonObject {
                put("string", it.string)
                it.style.asSequence().filterIsInstance<ColorContentStyle>().firstOrNull()?.let { style ->
                    put("color", style.color)
                }
            })
        }
    })
    put("coDisplay-en", buildJsonArray {
        co.getDisplayFormattedName(route.routeNumber, route.isKmbCtbJoint, route.gmbRegion, "en").content.forEach {
            add(buildJsonObject {
                put("string", it.string)
                it.style.asSequence().filterIsInstance<ColorContentStyle>().firstOrNull()?.let { style ->
                    put("color", style.color)
                }
            })
        }
    })
    if (route.shouldPrependTo()) {
        put("prependTo-zh", bilingualToPrefix.zh)
        put("prependTo-en", bilingualToPrefix.en)
    }
    route.resolvedDest(false).let {
        put("dest-zh", it.zh)
        put("dest-en", it.en)
    }
    if (co == Operator.NLB) {
        put("coSpecialRemark-zh", "從${route.orig.zh}開出")
        put("coSpecialRemark-en", "From ${route.orig.en}")
    } else if (co == Operator.KMB && route.routeNumber.getKMBSubsidiary() == KMBSubsidiary.SUNB) {
        put("coSpecialRemark-zh", "陽光巴士 (NR${route.routeNumber})")
        put("coSpecialRemark-en", "Sun Bus (NR${route.routeNumber})")
    }
    if (favouriteStopMode == FavouriteStopMode.FIXED) {
        put("secondLine-zh", if (co.isTrain) stop.name.zh else "${index}. ${stop.name.zh}")
        put("secondLine-en", if (co.isTrain) stop.name.en else "${index}. ${stop.name.en}")
    }
    put("deeplink", route.getDeepLink(applicationBaseAppContext!!, stopId, index))
}