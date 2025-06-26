package com.loohp.hkbuseta.notificationserver.utils

import java.io.OutputStream
import java.io.PrintStream
import java.util.Locale


fun PrintStream.asLogging(logs: PrintStream): PrintStream = LogPrintStream(this, logs)

class LogPrintStream(
    output: OutputStream,
    val logs: PrintStream
): PrintStream(output) {

    override fun flush() {
        super.flush()
        logs.flush()
    }

    override fun close() {
        super.close()
        logs.close()
    }

    override fun write(b: Int) {
        super.write(b)
        logs.write(b)
    }

    override fun write(buf: ByteArray?, off: Int, len: Int) {
        super.write(buf, off, len)
        logs.write(buf, off, len)
    }

    override fun write(buf: ByteArray?) {
        super.write(buf)
        logs.write(buf)
    }

    override fun writeBytes(buf: ByteArray?) {
        super.writeBytes(buf)
        logs.writeBytes(buf)
    }

    override fun print(b: Boolean) {
        super.print(b)
        logs.print(b)
    }

    override fun print(c: Char) {
        super.print(c)
        logs.print(c)
    }

    override fun print(i: Int) {
        super.print(i)
        logs.print(i)
    }

    override fun print(l: Long) {
        super.print(l)
        logs.print(l)
    }

    override fun print(f: Float) {
        super.print(f)
        logs.print(f)
    }

    override fun print(d: Double) {
        super.print(d)
        logs.print(d)
    }

    override fun print(s: CharArray?) {
        super.print(s)
        logs.print(s)
    }

    override fun print(s: String?) {
        super.print(s)
        logs.print(s)
    }

    override fun print(obj: Any?) {
        super.print(obj)
        logs.print(obj)
    }

    override fun println() {
        super.println()
        logs.println()
    }

    override fun println(x: Boolean) {
        super.println(x)
        logs.println(x)
    }

    override fun println(x: Char) {
        super.println(x)
        logs.println(x)
    }

    override fun println(x: Int) {
        super.println(x)
        logs.println(x)
    }

    override fun println(x: Long) {
        super.println(x)
        logs.println(x)
    }

    override fun println(x: Float) {
        super.println(x)
        logs.println(x)
    }

    override fun println(x: Double) {
        super.println(x)
        logs.println(x)
    }

    override fun println(x: CharArray?) {
        super.println(x)
        logs.println(x)
    }

    override fun println(x: String?) {
        super.println(x)
        logs.println(x)
    }

    override fun println(x: Any?) {
        super.println(x)
        logs.println(x)
    }

    override fun printf(format: String, vararg args: Any?): PrintStream? {
        return super.printf(format, *args).apply {
            logs.printf(format, *args)
        }
    }

    override fun printf(l: Locale?, format: String, vararg args: Any?): PrintStream? {
        return super.printf(l, format, *args).apply {
            logs.printf(l, format, *args)
        }
    }

    override fun format(format: String, vararg args: Any?): PrintStream? {
        return super.format(format, *args).apply {
            logs.format(format, *args)
        }
    }

    override fun format(l: Locale?, format: String, vararg args: Any?): PrintStream? {
        return super.format(l, format, *args).apply {
            logs.format(l, format, *args)
        }
    }

    override fun append(csq: CharSequence?): PrintStream? {
        return super.append(csq).apply {
            logs.append(csq)
        }
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): PrintStream? {
        return super.append(csq, start, end).apply {
            logs.append(csq, start, end)
        }
    }

    override fun append(c: Char): PrintStream? {
        return super.append(c).apply {
            logs.append(c)
        }
    }
}