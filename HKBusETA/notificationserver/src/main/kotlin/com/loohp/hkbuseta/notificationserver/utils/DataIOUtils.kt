package com.loohp.hkbuseta.notificationserver.utils

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.writeByte
import io.ktor.utils.io.writeByteArray
import java.io.IOException
import java.nio.charset.Charset


suspend fun ByteReadChannel.readVarInt(): Int {
    var i = 0
    var j = 0
    var b: Byte
    do {
        b = readByte()
        i = i or ((b.toInt() and 127) shl j++ * 7)
        if (j > 5) {
            throw RuntimeException("VarInt too big")
        }
    } while ((b.toInt() and 128) == 128)
    return i
}

suspend fun ByteWriteChannel.writeVarInt(value: Int) {
    var i = value
    while ((i and -128) != 0) {
        writeByte((i and 127 or 128).toByte())
        i = i ushr 7
    }
    writeByte(i.toByte())
}

suspend fun ByteReadChannel.readString(charset: Charset): String {
    val length = readVarInt()
    if (length == -1) {
        throw IOException("Premature end of stream.")
    }
    val b = readByteArray(length)
    return String(b, charset)
}

suspend fun ByteWriteChannel.writeString(value: String, charset: Charset) {
    val bytes = value.toByteArray(charset)
    writeVarInt(bytes.size)
    writeByteArray(bytes)
}

suspend fun ByteReadChannel.readBoolean(): Boolean {
    return readByte() > 0
}

suspend fun ByteWriteChannel.writeBoolean(value: Boolean) {
    writeByte(if (value) 1 else 0)
}