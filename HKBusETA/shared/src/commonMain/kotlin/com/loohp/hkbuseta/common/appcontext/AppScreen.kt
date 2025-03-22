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

package com.loohp.hkbuseta.common.appcontext

enum class AppScreen {

    DISMISSIBLE_TEXT_DISPLAY,
    FATAL_ERROR,
    ETA,
    ETA_MENU,
    FAV,
    FAV_ROUTE_LIST_VIEW,
    LIST_ROUTES,
    LIST_STOPS,
    MAIN,
    NEARBY,
    SEARCH,
    SEARCH_TRAIN,
    TITLE,
    URL_IMAGE,
    ALIGHT_REMINDER_SERVICE,
    ETA_TILE_CONFIGURE,
    ETA_TILE_LIST,
    RECENT,
    SETTINGS,
    PDF,
    JOURNEY_PLANNER,
    DUMMY;

    companion object {
        fun valueOfNullable(value: String): AppScreen? {
            return entries.firstOrNull { it.name.equals(value, true) }
        }
    }

}