package com.loohp.hkbuseta.common.utils


inline fun <T> T.runIf(condition: Boolean, block: T.() -> T): T {
    return if (condition) block.invoke(this) else this
}

inline fun <T> T.runIf(condition: () -> Boolean, block: T.() -> T): T {
    return if (condition.invoke()) block.invoke(this) else this
}

inline fun <E, T> T.runIfNotNull(item: E?, block: T.(E) -> T): T {
    return if (item == null) this else block.invoke(this, item)
}