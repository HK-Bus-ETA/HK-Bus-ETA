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

package com.loohp.hkbuseta.shared

import com.loohp.hkbuseta.appcontext.applicationAppContext
import com.loohp.hkbuseta.common.shared.Shared
import javafx.application.Platform
import kotlinx.coroutines.runBlocking
import javax.swing.UIManager

object DesktopShared {

    fun setDefaultExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                runBlocking { Shared.invalidateCache(applicationAppContext) }
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
                throw throwable
            }
        }
    }

    fun setSystemFormatProviders() {
        System.setProperty("java.locale.providers", "HOST")
    }

    fun startupJavaFX() {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        Platform.startup { /* do nothing */ }
    }

}