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

package com.loohp.hkbuseta.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IntUtils {

    public static int parseOrZero(String input) {
        return parseOr(input, 0);
    }

    public static int parseOr(String input, int otherwise) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return otherwise;
        }
    }

    public static List<Integer> toList(int[] array) {
        return Arrays.stream(array).boxed().collect(Collectors.toList());
    }

}
