/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.common.appcontext

import com.loohp.hkbuseta.common.utils.IOSerializable

class AppIntent(val context: AppContext, val screen: AppScreen) {

    val extras: AppBundle = AppBundle()

    val intentFlags: MutableSet<AppIntentFlag> = mutableSetOf()

    fun addFlags(vararg flags: AppIntentFlag) {
        intentFlags.addAll(flags.toSet())
    }

    fun removeFlags(vararg flags: AppIntentFlag) {
        intentFlags.removeAll(flags.toSet())
    }

    fun hasFlags(vararg flags: AppIntentFlag): Boolean {
        return intentFlags.containsAll(flags.toSet())
    }

    fun putExtra(name: String, value: IOSerializable): AppIntent {
        extras.putObject(name, value)
        return this
    }

    fun putExtra(name: String, value: Boolean): AppIntent {
        extras.putBoolean(name, value)
        return this
    }

    fun putExtra(name: String, value: Byte): AppIntent {
        extras.putByte(name, value)
        return this
    }

    fun putExtra(name: String, value: Char): AppIntent {
        extras.putChar(name, value)
        return this
    }

    fun putExtra(name: String, value: Short): AppIntent {
        extras.putShort(name, value)
        return this
    }

    fun putExtra(name: String, value: Int): AppIntent {
        extras.putInt(name, value)
        return this
    }

    fun putExtra(name: String, value: Long): AppIntent {
        extras.putLong(name, value)
        return this
    }

    fun putExtra(name: String, value: Float): AppIntent {
        extras.putFloat(name, value)
        return this
    }

    fun putExtra(name: String, value: Double): AppIntent {
        extras.putDouble(name, value)
        return this
    }

    fun putExtra(name: String, value: String?): AppIntent {
        extras.putString(name, value)
        return this
    }

    fun putExtra(name: String, value: CharSequence?): AppIntent {
        extras.putCharSequence(name, value)
        return this
    }

    fun putExtra(name: String, value: BooleanArray?): AppIntent {
        extras.putBooleanArray(name, value)
        return this
    }

    fun putExtra(name: String, value: ByteArray?): AppIntent {
        extras.putByteArray(name, value)
        return this
    }

    fun putExtra(name: String, value: ShortArray?): AppIntent {
        extras.putShortArray(name, value)
        return this
    }

    fun putExtra(name: String, value: CharArray?): AppIntent {
        extras.putCharArray(name, value)
        return this
    }

    fun putExtra(name: String, value: IntArray?): AppIntent {
        extras.putIntArray(name, value)
        return this
    }

    fun putExtra(name: String, value: LongArray?): AppIntent {
        extras.putLongArray(name, value)
        return this
    }

    fun putExtra(name: String, value: FloatArray?): AppIntent {
        extras.putFloatArray(name, value)
        return this
    }

    fun putExtra(name: String, value: DoubleArray?): AppIntent {
        extras.putDoubleArray(name, value)
        return this
    }

    fun putExtra(name: String, value: ArrayList<String>?): AppIntent {
        extras.putStringArrayList(name, value)
        return this
    }

    fun putExtra(name: String, value: AppBundle?): AppIntent {
        extras.putAppBundle(name, value)
        return this
    }

    fun putExtras(appBundle: AppBundle): AppIntent {
        extras.putAll(appBundle)
        return this
    }

    fun replaceExtras(appBundle: AppBundle?): AppIntent {
        extras.replace(appBundle)
        return this
    }

    fun removeExtra(name: String) {
        extras.remove(name)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppIntent) return false

        if (context != other.context) return false
        if (screen != other.screen) return false
        if (extras != other.extras) return false
        return intentFlags == other.intentFlags
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + screen.hashCode()
        result = 31 * result + extras.hashCode()
        result = 31 * result + intentFlags.hashCode()
        return result
    }

}

data class AppIntentResult(val resultCode: Int) {

    companion object {

        val NORMAL = AppIntentResult(0)

    }

}

enum class AppIntentFlag {

    NEW_TASK,
    CLEAR_TASK,
    NO_ANIMATION

}

class AppBundle {
    
    val data: MutableMap<String, Any?> = mutableMapOf()

    fun putObject(name: String, value: IOSerializable): AppBundle {
        data[name] = value
        return this
    }

    fun putBoolean(name: String, value: Boolean): AppBundle {
        data[name] = value
        return this
    }

    fun putByte(name: String, value: Byte): AppBundle {
        data[name] = value
        return this
    }

    fun putChar(name: String, value: Char): AppBundle {
        data[name] = value
        return this
    }

    fun putShort(name: String, value: Short): AppBundle {
        data[name] = value
        return this
    }

    fun putInt(name: String, value: Int): AppBundle {
        data[name] = value
        return this
    }

    fun putLong(name: String, value: Long): AppBundle {
        data[name] = value
        return this
    }

    fun putFloat(name: String, value: Float): AppBundle {
        data[name] = value
        return this
    }

    fun putDouble(name: String, value: Double): AppBundle {
        data[name] = value
        return this
    }

    fun putString(name: String, value: String?): AppBundle {
        data[name] = value
        return this
    }

    fun putCharSequence(name: String, value: CharSequence?): AppBundle {
        data[name] = value
        return this
    }

    fun putBooleanArray(name: String, value: BooleanArray?): AppBundle {
        data[name] = value
        return this
    }

    fun putByteArray(name: String, value: ByteArray?): AppBundle {
        data[name] = value
        return this
    }

    fun putShortArray(name: String, value: ShortArray?): AppBundle {
        data[name] = value
        return this
    }
    
    fun putCharArray(name: String, value: CharArray?): AppBundle {
        data[name] = value
        return this
    }

    fun putIntArray(name: String, value: IntArray?): AppBundle {
        data[name] = value
        return this
    }
    
    fun putLongArray(name: String, value: LongArray?): AppBundle {
        data[name] = value
        return this
    }

    fun putFloatArray(name: String, value: FloatArray?): AppBundle {
        data[name] = value
        return this
    }

    fun putDoubleArray(name: String, value: DoubleArray?): AppBundle {
        data[name] = value
        return this
    }

    fun putStringArrayList(name: String, value: ArrayList<String>?): AppBundle {
        data[name] = value
        return this
    }
    
    fun putAppBundle(name: String, value: AppBundle?): AppBundle {
        data[name] = value
        return this
    }
    
    fun putAll(appBundle: AppBundle): AppBundle {
        data.putAll(appBundle.data)
        return this
    }
    
    fun replace(appBundle: AppBundle?): AppBundle {
        data.clear()
        appBundle?.let { data.putAll(it.data) }
        return this
    }

    fun remove(name: String) {
        data.remove(name)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppBundle) return false

        return data == other.data
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

}