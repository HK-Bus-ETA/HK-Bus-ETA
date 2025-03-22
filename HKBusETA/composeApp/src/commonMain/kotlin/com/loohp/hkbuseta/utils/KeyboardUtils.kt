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

import androidx.compose.ui.input.key.Key

private val keyCharMap: Map<Key, Char> = mapOf(
    Key.Zero to '0',
    Key.NumPad0 to '0',
    Key.One to '1',
    Key.NumPad1 to '1',
    Key.Two to '2',
    Key.NumPad2 to '2',
    Key.Three to '3',
    Key.NumPad3 to '3',
    Key.Four to '4',
    Key.NumPad4 to '4',
    Key.Five to '5',
    Key.NumPad5 to '5',
    Key.Six to '6',
    Key.NumPad6 to '6',
    Key.Seven to '7',
    Key.NumPad7 to '7',
    Key.Eight to '8',
    Key.NumPad8 to '8',
    Key.Nine to '9',
    Key.NumPad9 to '9',
    Key.A to 'A',
    Key.B to 'B',
    Key.C to 'C',
    Key.D to 'D',
    Key.E to 'E',
    Key.F to 'F',
    Key.G to 'G',
    Key.H to 'H',
    Key.I to 'I',
    Key.J to 'J',
    Key.K to 'K',
    Key.L to 'L',
    Key.M to 'M',
    Key.N to 'N',
    Key.O to 'O',
    Key.P to 'P',
    Key.Q to 'Q',
    Key.R to 'R',
    Key.S to 'S',
    Key.T to 'T',
    Key.U to 'U',
    Key.V to 'V',
    Key.W to 'W',
    Key.X to 'X',
    Key.Y to 'Y',
    Key.Z to 'Z'
)

val Key.keyChar: Char? get() = keyCharMap[this]