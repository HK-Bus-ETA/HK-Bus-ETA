package com.loohp.hkbuseta.common.utils

import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

@OptIn(NativeRuntimeApi::class)
actual fun performGC() {
    GC.collect()
}