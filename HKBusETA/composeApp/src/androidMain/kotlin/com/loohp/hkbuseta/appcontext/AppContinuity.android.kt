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

package com.loohp.hkbuseta.appcontext

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.ToastDuration
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import kotlinx.coroutines.Deferred
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

actual fun sendToWatch(context: AppContext, routeKey: String, stopId: String?, stopIndex: Int?) {
    val payload = buildJsonObject {
        put("k", routeKey)
        if (stopId != null) {
            put("s", stopId)
            put("sd", true)
            if (stopIndex != null) {
                put("si", stopIndex)
            }
        }
    }
    RemoteActivityUtils.dataToWatch(context.context, Shared.START_ACTIVITY_ID, payload,
        noWatch = {
            context.showToastText(if (Shared.language == "en") "Unable to find watch" else "無法連接手錶", ToastDuration.SHORT)
        },
        failed = {
            context.showToastText(if (Shared.language == "en") "Failed to connect to watch" else "連接手錶失敗", ToastDuration.SHORT)
        },
        success = {
            context.showToastText(if (Shared.language == "en") "Check your watch" else "請在手錶上繼續", ToastDuration.SHORT)
        }
    )
}

actual fun hasWatchAppInstalled(context: AppContext): Deferred<Boolean> {
    return RemoteActivityUtils.hasWatchApp(context.context)
}