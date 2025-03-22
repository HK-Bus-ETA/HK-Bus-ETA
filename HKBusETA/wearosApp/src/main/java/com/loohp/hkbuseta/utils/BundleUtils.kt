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

import android.os.Bundle


fun Bundle.isEqualTo(other: Bundle?): Boolean {
    if (other == null) {
        return false
    }

    if (this.size() != other.size()) {
        return false
    }

    if (!this.keySet().containsAll(other.keySet())) {
        return false
    }

    for (key in this.keySet()) {
        val valueOne = this.get(key)
        val valueTwo = other.get(key)
        if (valueOne is Bundle && valueTwo is Bundle) {
            if (!valueOne.isEqualTo(valueTwo)) {
                return false
            }
        } else if (valueOne != valueTwo) {
            return false
        }
    }

    return true
}