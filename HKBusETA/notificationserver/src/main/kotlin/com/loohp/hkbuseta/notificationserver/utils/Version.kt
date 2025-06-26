package com.loohp.hkbuseta.notificationserver.utils

import kotlin.math.max

data class Version(val version: String): CharSequence by version, Comparable<Version> {

    init {
        require(version.matches("[0-9]+(\\.[0-9]+)*".toRegex())) { "Invalid version format" }
    }

    override fun compareTo(other: Version): Int {
        val thisParts = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val thatParts = other.version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val length = max(thisParts.size.toDouble(), thatParts.size.toDouble()).toInt()
        for (i in 0..<length) {
            val thisPart = if (i < thisParts.size) thisParts[i].toInt() else 0
            val thatPart = if (i < thatParts.size) thatParts[i].toInt() else 0
            if (thisPart < thatPart) return -1
            if (thisPart > thatPart) return 1
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this.javaClass != other.javaClass) return false
        return this.compareTo(other as Version) == 0
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}