package com.loohp.hkbuseta.common.utils

import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path


val DATA_FOLDER: File by lazy {
    val path = Path(AppDirsFactory.getInstance().getUserDataDir("hkbuseta", ".", ".", true))
    if (!Files.exists(path)) {
        Files.createDirectories(path)
    }
    path.toFile().apply { println("Data Folder is at $absolutePath") }
}