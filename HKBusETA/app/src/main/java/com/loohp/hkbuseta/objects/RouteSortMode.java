/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

package com.loohp.hkbuseta.objects;

public enum RouteSortMode {

    NORMAL, RECENT, PROXIMITY;

    private static final RouteSortMode[] VALUES = values();

    public RouteSortMode nextMode(boolean allowRecentSort, boolean allowProximitySort) {
        RouteSortMode next = VALUES[(ordinal() + 1) % VALUES.length];
        if (next.isLegalMode(allowRecentSort, allowProximitySort)) {
            return next;
        } else {
            return next.nextMode(allowRecentSort, allowProximitySort);
        }
    }

    public boolean isLegalMode(boolean allowRecentSort, boolean allowProximitySort) {
        return (allowProximitySort || this != PROXIMITY) && (allowRecentSort || this != RECENT);
    }

}
