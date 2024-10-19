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

package com.loohp.hkbuseta.compose

import androidx.compose.ui.Modifier


inline fun Modifier.applyIf(condition: Boolean, apply: Modifier.() -> Modifier): Modifier {
    return applyIf({ condition }, apply)
}

inline fun Modifier.applyIf(condition: Boolean, apply: Modifier.() -> Modifier, elseApply: Modifier.() -> Modifier): Modifier {
    return applyIf({ condition }, apply, elseApply)
}

inline fun Modifier.applyIf(predicate: () -> Boolean, apply: Modifier.() -> Modifier): Modifier {
    return if (predicate.invoke()) apply.invoke(this) else this
}

inline fun Modifier.applyIf(predicate: () -> Boolean, apply: Modifier.() -> Modifier, elseApply: Modifier.() -> Modifier): Modifier {
    return if (predicate.invoke()) apply.invoke(this) else elseApply.invoke(this)
}

inline fun <T> Modifier.applyIfNotNull(item: T?, apply: Modifier.(T) -> Modifier): Modifier {
    return item?.let { apply.invoke(this, it) }?: this
}