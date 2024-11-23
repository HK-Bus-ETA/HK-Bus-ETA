package com.loohp.hkbuseta.common.utils

import android.util.AtomicFile
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


@OptIn(ExperimentalContracts::class)
inline fun AtomicFile.useWrite(block: (FileOutputStream) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val writer = startWrite()
    var exception: Throwable? = null
    try {
        return block.invoke(writer)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        if (exception == null) {
            finishWrite(writer)
        } else {
            failWrite(writer)
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AtomicFile.useWriteBuffered(block: (BufferedOutputStream) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return useWrite { block.invoke(it.buffered()) }
}