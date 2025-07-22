package com.loohp.hkbuseta.utils

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.loohp.hkbuseta.common.utils.AutoSortedList


inline fun <E: Comparable<E>, T: SnapshotStateList<E>> T.asAutoSortedList(comparator: Comparator<E> = naturalOrder()): AutoSortedList<E, T> {
    return AutoSortedList(this, comparator, true)
}