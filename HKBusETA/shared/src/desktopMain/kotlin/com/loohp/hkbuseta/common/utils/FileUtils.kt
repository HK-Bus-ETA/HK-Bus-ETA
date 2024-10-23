package com.loohp.hkbuseta.common.utils

import java.io.File
import java.nio.file.Files


val DATA_FOLDER: File by lazy {
    val path = File("hkbuseta-data").toPath()
    if (!Files.exists(path)) {
        Files.createDirectories(path)
    }
    path.toFile()
}

val KCEF_FOLDER: File by lazy {
    val path = File("hkbuseta-kcef").toPath()
    if (!Files.exists(path)) {
        Files.createDirectories(path)
    }
    path.toFile()
}