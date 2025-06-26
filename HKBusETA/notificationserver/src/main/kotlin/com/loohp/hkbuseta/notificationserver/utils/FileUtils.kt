package com.loohp.hkbuseta.notificationserver.utils

import java.io.BufferedReader
import java.io.InputStream

object ResourceAccessor

fun readResourceAsStream(name: String): InputStream? {
    return ResourceAccessor.javaClass.classLoader.getResourceAsStream(name)
}

fun readResourceAsText(name: String): BufferedReader? {
    return readResourceAsStream(name)?.bufferedReader()
}

fun readResourceAsBytes(name: String): ByteArray? {
    return readResourceAsStream(name)?.readBytes()
}