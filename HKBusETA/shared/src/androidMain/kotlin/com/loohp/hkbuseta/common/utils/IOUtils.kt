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