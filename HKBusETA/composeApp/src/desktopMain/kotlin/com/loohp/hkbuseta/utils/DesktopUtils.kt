/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.utils

import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.Locale


object DesktopUtils {

    fun browse(uri: URI): Boolean {
        return browseDESKTOP(uri)
    }

    fun open(file: File): Boolean {
        if (openSystemSpecific(file.path)) return true
        if (openDesktop(file)) return true
        return false
    }

    fun openParentFolder(file: File): Boolean {
        if (os.isWindows && runCommand("explorer", "%s", "/select,\"${file.path}\"")) {
            return true
        }
        return open(file)
    }

    fun edit(file: File): Boolean {
        if (openSystemSpecific(file.path)) return true
        if (editDesktop(file)) return true
        return false
    }

    private fun openSystemSpecific(what: String): Boolean {
        when {
            os.isLinux -> {
                if (runCommand("kde-open", "%s", what)) return true
                if (runCommand("gnome-open", "%s", what)) return true
                if (runCommand("xdg-open", "%s", what)) return true
            }
            os.isMac -> {
                if (runCommand("open", "%s", what)) return true
            }
            os.isWindows -> {
                if (runCommand("explorer", "%s", what)) return true
            }
        }
        return false
    }

    private fun browseDESKTOP(uri: URI): Boolean {
        logOut("Trying to use Desktop.getDesktop().browse() with $uri")
        try {
            if (!Desktop.isDesktopSupported()) {
                logErr("Platform is not supported.")
                return false
            }
            if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                logErr("BROWSE is not supported.")
                return false
            }
            Desktop.getDesktop().browse(uri)
            return true
        } catch (t: Throwable) {
            logErr("Error using desktop browse.", t)
            return false
        }
    }


    private fun openDesktop(file: File): Boolean {
        logOut("Trying to use Desktop.getDesktop().open() with $file")
        try {
            if (!Desktop.isDesktopSupported()) {
                logErr("Platform is not supported.")
                return false
            }
            if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                logErr("OPEN is not supported.")
                return false
            }
            Desktop.getDesktop().open(file)
            return true
        } catch (t: Throwable) {
            logErr("Error using desktop open.", t)
            return false
        }
    }


    private fun editDesktop(file: File): Boolean {
        logOut("Trying to use Desktop.getDesktop().edit() with $file")
        try {
            if (!Desktop.isDesktopSupported()) {
                logErr("Platform is not supported.")
                return false
            }

            if (!Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                logErr("EDIT is not supported.")
                return false
            }
            Desktop.getDesktop().edit(file)
            return true
        } catch (t: Throwable) {
            logErr("Error using desktop edit.", t)
            return false
        }
    }


    private fun runCommand(command: String, args: String, file: String): Boolean {
        logOut("Trying to exec:\n   cmd = $command\n   args = $args\n   %s = $file")
        val parts = prepareCommand(command, args, file)
        try {
            val p = Runtime.getRuntime().exec(parts) ?: return false
            try {
                val retval = p.exitValue()
                if (retval == 0) {
                    logErr("Process ended immediately.")
                    return false
                } else {
                    logErr("Process crashed.")
                    return false
                }
            } catch (itse: IllegalThreadStateException) {
                logErr("Process is running.")
                return true
            }
        } catch (e: IOException) {
            logErr("Error running command.", e)
            return false
        }
    }


    private fun prepareCommand(command: String, args: String?, file: String): Array<String> {
        val parts: MutableList<String> = mutableListOf()
        parts.add(command)
        if (args != null) {
            for (s in args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val s0 = String.format(s, file) // put in the filename thing
                parts.add(s0.trim { it <= ' ' })
            }
        }
        return parts.toTypedArray()
    }

    private fun logErr(msg: String, t: Throwable) {
        System.err.println(msg)
        t.printStackTrace()
    }

    private fun logErr(msg: String) {
        System.err.println(msg)
    }

    private fun logOut(msg: String) {
        println(msg)
    }

    val os: EnumOS get() {
        val s = System.getProperty("os.name").lowercase(Locale.getDefault())

        if (s.contains("win")) {
            return EnumOS.WINDOWS
        }

        if (s.contains("mac")) {
            return EnumOS.MACOS
        }

        if (s.contains("solaris")) {
            return EnumOS.SOLARIS
        }

        if (s.contains("sunos")) {
            return EnumOS.SOLARIS
        }

        if (s.contains("linux")) {
            return EnumOS.LINUX
        }

        return if (s.contains("unix")) {
            EnumOS.LINUX
        } else {
            EnumOS.UNKNOWN
        }
    }

    enum class EnumOS {
        LINUX, MACOS, SOLARIS, UNKNOWN, WINDOWS;

        val isLinux: Boolean get() = this == LINUX || this == SOLARIS
        val isMac: Boolean get() = this == MACOS
        val isWindows: Boolean get() = this == WINDOWS
    }
}