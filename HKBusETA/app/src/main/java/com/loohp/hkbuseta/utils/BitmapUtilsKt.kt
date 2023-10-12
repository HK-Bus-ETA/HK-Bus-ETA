package com.loohp.hkbuseta.utils

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream


fun Bitmap.compressToByteArray(format: Bitmap.CompressFormat, quality: Int): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(format, quality, stream)
    return stream.toByteArray()
}