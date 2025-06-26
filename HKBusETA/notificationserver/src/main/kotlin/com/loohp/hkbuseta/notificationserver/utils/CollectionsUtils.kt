package com.loohp.hkbuseta.notificationserver.utils

import java.io.InputStream
import java.io.SequenceInputStream
import java.util.Collections
import java.util.Enumeration


fun <T> Collection<T>.asEnumeration(): Enumeration<T> {
    return Collections.enumeration(this)
}

fun <T: InputStream> Collection<T>.joinInputStreams(): InputStream {
    return SequenceInputStream(asEnumeration())
}

class MergedList<T>(
    val list1: List<T>,
    val list2: List<T>
): AbstractList<T>() {
    override val size: Int get() = list1.size + list2.size
    override fun get(index: Int): T {
        if (index < list1.size) {
            return list1[index];
        }
        return list2[index - list1.size]
    }
}

fun <T> List<T>.asMergedView(other: List<T>): List<T> = MergedList(this, other)

class MapEntry<K, V>(
    override val key: K,
    override val value : V
): Map.Entry<K, V>