package com.loohp.hkbuseta.utils

import android.content.Context
import android.content.Intent
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.Wearable
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

    }
}