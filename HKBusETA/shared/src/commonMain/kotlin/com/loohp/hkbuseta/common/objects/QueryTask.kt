package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.DateTimeUnit

@Immutable
open class QueryTask<T>(
    private val errorResult: T,
    private val task: suspend () -> T
) {

    suspend fun query(): T {
        return try {
            task.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
            errorResult
        }
    }

    suspend fun query(timeout: Int, unit: DateTimeUnit.TimeBased): T {
        return try {
            withTimeout(unit.duration.times(timeout).inWholeMilliseconds) { task.invoke() }
        } catch (e: Exception) {
            e.printStackTrace()
            errorResult
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun query(callback: (T) -> Unit) {
        val deferred = CoroutineScope(Dispatchers.IO).async { task.invoke() }
        deferred.invokeOnCompletion { it?.let {
            it.printStackTrace()
            callback.invoke(errorResult)
        }?: callback.invoke(deferred.getCompleted()) }
    }

    fun query(timeout: Int, unit: DateTimeUnit.TimeBased, callback: (T) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val deferred = async { task.invoke() }
            try {
                callback.invoke(withTimeout(unit.duration.times(timeout).inWholeMilliseconds) { deferred.await() })
            } catch (e: Exception) {
                e.printStackTrace()
                try { deferred.cancel() } catch (ignore: Throwable) { }
                callback.invoke(errorResult)
            }
        }
    }
}