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

package com.loohp.hkbuseta.common.shared

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.SplashEntry
import com.loohp.hkbuseta.common.utils.JsonIgnoreUnknownKeys
import com.loohp.hkbuseta.common.utils.decodeFromStringReadChannel
import com.loohp.hkbuseta.common.utils.forList
import com.loohp.hkbuseta.common.utils.getRawResponse
import com.loohp.hkbuseta.common.utils.isNotNullAndNotEmpty
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json

object Splash {

    private var splashEntries: List<SplashEntry> = emptyList()

    suspend fun load(context: AppContext) {
        if (splashEntries.isEmpty() && context.listFiles().contains("splash.json")) {
            this.splashEntries = JsonIgnoreUnknownKeys.decodeFromStringReadChannel<List<SplashEntry>>(context.readTextFile("splash.json"))
        }
    }

    suspend fun reloadEntries(splashEntries: List<SplashEntry>?, context: AppContext) {
        if (splashEntries.isNotNullAndNotEmpty()) {
            val removedEntries = this.splashEntries - splashEntries.toSet()
            removedEntries.forEach { context.deleteFile(it.imageName) }
            this.splashEntries = splashEntries
            context.writeTextFile("splash.json", Json, SplashEntry.serializer().forList()) { splashEntries }
        }
    }

    suspend fun downloadMissingImages(context: AppContext) {
        val files = context.listFiles()
        splashEntries.forEach {
            if (!files.contains(it.imageName)) {
                getRawResponse("${Shared.SPLASH_DOMAIN}/${it.imageName}")?.apply {
                    context.writeRawFile(it.imageName) { this }
                }
            }
        }
    }

    suspend fun clearDownloadedImages(context: AppContext) {
        val files = context.listFiles()
        splashEntries.forEach {
            if (files.contains(it.imageName)) {
                context.deleteFile(it.imageName)
            }
        }
    }

    suspend fun getRandomSplashEntry(context: AppContext): SplashEntry? {
        load(context)
        val files = context.listFiles()
        val width = context.screenWidth
        val height = context.screenHeight
        return splashEntries.asSequence().shuffled().firstOrNull {
            it.fitOrientation(width, height) && files.contains(it.imageName)
        }
    }

}

suspend fun SplashEntry.readImage(context: AppContext): ByteReadChannel {
    return context.readRawFile(imageName)
}