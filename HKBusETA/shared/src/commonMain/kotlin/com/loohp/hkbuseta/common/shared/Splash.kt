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

package com.loohp.hkbuseta.common.shared

import co.touchlab.stately.collections.ConcurrentMutableList
import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.objects.SplashEntry
import com.loohp.hkbuseta.common.utils.JsonIgnoreUnknownKeys
import com.loohp.hkbuseta.common.utils.decodeFromStringReadChannel
import com.loohp.hkbuseta.common.utils.getRawResponse
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object Splash {

    private val splashEntries: MutableList<SplashEntry> = ConcurrentMutableList()

    fun platformDomainSplashUrl(): String {
        return "https://splash.hkbuseta.com"
    }

    suspend fun load(context: AppContext) {
        if (splashEntries.isEmpty() && context.listFiles().contains("splash.json")) {
            val splashEntries = JsonIgnoreUnknownKeys.decodeFromStringReadChannel<List<SplashEntry>>(context.readTextFile("splash.json"))
            this.splashEntries.addAll(splashEntries)
        }
    }

    suspend fun reloadEntries(splashEntries: List<SplashEntry>?, context: AppContext) {
        if (!splashEntries.isNullOrEmpty()) {
            val removedEntries = this.splashEntries - splashEntries.toSet()
            removedEntries.forEach { context.deleteFile(it.imageName) }
            this.splashEntries.clear()
            this.splashEntries.addAll(splashEntries)
            context.writeTextFile("splash.json", Json) { ListSerializer(SplashEntry.serializer()) to splashEntries }
        }
    }

    suspend fun downloadMissingImages(context: AppContext) {
        val files = context.listFiles()
        splashEntries.forEach {
            if (!files.contains(it.imageName)) {
                getRawResponse("${platformDomainSplashUrl()}/${it.imageName}")?.apply {
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
        return splashEntries.shuffled().firstOrNull {
            it.fitOrientation(width, height) && files.contains(it.imageName)
        }
    }

}

suspend fun SplashEntry.readImage(context: AppContext): ByteReadChannel {
    return context.readRawFile(imageName)
}