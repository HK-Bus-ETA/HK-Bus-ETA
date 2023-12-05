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
package com.loohp.hkbuseta.utils

import android.app.Activity
import android.content.Context
import kotlin.math.abs

object ScreenSizeUtils {

    private var init = false
    private var width = 0
    private var height = 0
    private var min = 0
    private var scale = 0f

    private fun init(context: Context) {
        if (init) {
            return
        }
        synchronized(ScreenSizeUtils::class.java) {
            if (init) {
                return
            }
            if (context is Activity) {
                val bound = context.windowManager.currentWindowMetrics.bounds
                width = abs(bound.width())
                height = abs(bound.height())
            } else {
                val displayMetrics = context.resources.displayMetrics
                width = abs(displayMetrics.widthPixels)
                height = abs(displayMetrics.heightPixels)
            }
            min = width.coerceAtMost(height)
            scale = min / 454f
            init = true
        }
    }

    fun getMinScreenSize(context: Context): Int {
        init(context)
        return min
    }

    fun getScreenScale(context: Context): Float {
        init(context)
        return scale
    }

    fun getScreenWidth(context: Context): Int {
        init(context)
        return width
    }

    fun getScreenHeight(context: Context): Int {
        init(context)
        return height
    }
}
