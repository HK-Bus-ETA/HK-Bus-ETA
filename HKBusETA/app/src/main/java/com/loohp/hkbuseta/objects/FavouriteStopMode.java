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

public enum FavouriteStopMode {

    FIXED(false), CLOSEST(true);

    private static final FavouriteStopMode[] VALUES = values();

    private final boolean requiresLocation;

    FavouriteStopMode(boolean requiresLocation) {
        this.requiresLocation = requiresLocation;
    }

    public boolean isRequiresLocation() {
        return requiresLocation;
    }

    public static FavouriteStopMode valueOfOrDefault(String name) {
        for (FavouriteStopMode favouriteStopMode : VALUES) {
            if (favouriteStopMode.name().equalsIgnoreCase(name)) {
                return favouriteStopMode;
            }
        }
        return FIXED;
    }

}
