/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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

package com.loohp.hkbuseta.utils

import androidx.compose.ui.input.key.Key


val Key.keyString: String? get() = when (this) {
    Key.Zero, Key.NumPad0 -> "0"
    Key.One, Key.NumPad1 -> "1"
    Key.Two, Key.NumPad2 -> "2"
    Key.Three, Key.NumPad3 -> "3"
    Key.Four, Key.NumPad4 -> "4"
    Key.Five, Key.NumPad5 -> "5"
    Key.Six, Key.NumPad6 -> "6"
    Key.Seven, Key.NumPad7 -> "7"
    Key.Eight, Key.NumPad8 -> "8"
    Key.Nine, Key.NumPad9 -> "9"
    Key.A -> "A"
    Key.B -> "B"
    Key.C -> "C"
    Key.D -> "D"
    Key.E -> "E"
    Key.F -> "F"
    Key.G -> "G"
    Key.H -> "H"
    Key.I -> "I"
    Key.J -> "J"
    Key.K -> "K"
    Key.L -> "L"
    Key.M -> "M"
    Key.N -> "N"
    Key.O -> "O"
    Key.P -> "P"
    Key.Q -> "Q"
    Key.R -> "R"
    Key.S -> "S"
    Key.T -> "T"
    Key.U -> "U"
    Key.V -> "V"
    Key.W -> "W"
    Key.X -> "X"
    Key.Y -> "Y"
    Key.Z -> "Z"
    else -> null
}