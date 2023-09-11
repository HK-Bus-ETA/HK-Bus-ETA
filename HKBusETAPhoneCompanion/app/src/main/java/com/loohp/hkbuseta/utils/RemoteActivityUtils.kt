package com.loohp.hkbuseta.utils

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

class RemoteActivityUtils {

    companion object {

        fun dataToWatch(instance: Context, path: String, data: JSONObject, noWatch: () -> Unit = {}, failed: () -> Unit = {}, success: () -> Unit = {}) {
            val dataRaw = data.toString().toByteArray(StandardCharsets.UTF_8)
            Wearable.getNodeClient(instance).connectedNodes.addOnCompleteListener { nodes ->
                if (nodes.result.isEmpty()) {
                    noWatch.invoke()
                } else {
                    val futures = nodes.result.map { node ->
                        val future: CompletableFuture<Task<Int>> = CompletableFuture()
                        Wearable.getMessageClient(instance).sendMessage(node.id, path, dataRaw).addOnCompleteListener {
                            future.complete(it)
                        }
                        return@map future
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