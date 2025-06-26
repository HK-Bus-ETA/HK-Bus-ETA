package com.loohp.hkbuseta.notificationserver.utils

import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty


operator fun <T> AtomicReference<T>.getValue(thisObj: Any?, property: KProperty<*>): T = get()
operator fun <T> AtomicReference<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T) = set(value)