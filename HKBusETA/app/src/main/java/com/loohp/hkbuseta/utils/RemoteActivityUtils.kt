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

import android.content.Context
import android.content.Intent
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

class RemoteActivityUtils {

    companion object {

        fun intentToPhone(instance: Context, intent: Intent, noPhone: () -> Unit = {}, failed: () -> Unit = {}, success: () -> Unit = {}) {
            val remoteActivityHelper = RemoteActivityHelper(instance, ForkJoinPool.commonPool())
            Wearable.getNodeClient(instance).connectedNodes.addOnCompleteListener { nodes ->
                if (nodes.result.isEmpty()) {
                    noPhone.invoke()
                } else {
                    val futures = nodes.result.map { node -> remoteActivityHelper.startRemoteActivity(intent, node.id) }
                    ForkJoinPool.commonPool().execute {
                        var isSuccess = false
                        for (future in futures) {
                            try {
                                future.get()
                                isSuccess = true
                                break
                            } catch (_: Exception) {}
                        }
                        if (isSuccess) {
                            success.invoke()
                        } else {
                            failed.invoke()
                        }
                    }
                }
            }
        }

        fun dataToPhone(instance: Context, path: String, data: JSONObject, noPhone: () -> Unit = {}, failed: () -> Unit = {}, success: () -> Unit = {}) {
            val dataRaw = data.toString().toByteArray(StandardCharsets.UTF_8)
            Wearable.getNodeClient(instance).connectedNodes.addOnCompleteListener { nodes ->
                if (nodes.result.isEmpty()) {
                    noPhone.invoke()
                } else {
                    val futures = nodes.result.mapNotNull { node ->
                        return@mapNotNull try {
                            val future: CompletableFuture<Task<Int>> = CompletableFuture()
                            Wearable.getMessageClient(instance).sendMessage(node.id, path, dataRaw).addOnCompleteListener {
                                future.complete(it)
                            }
                            future
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            null
                        }
                    }
                    ForkJoinPool.commonPool().execute {
                        var isSuccess = false
                        for (future in futures) {
                            try {
                                future.get()
                                isSuccess = true
                                break
                            } catch (_: Exception) {}
                        }
                        if (isSuccess) {
                            success.invoke()
                        } else {
                            failed.invoke()
                        }
                    }
                }
            }
        }

    }
}