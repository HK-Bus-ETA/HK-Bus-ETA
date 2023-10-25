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

import androidx.compose.runtime.Immutable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Immutable
public class Operator {

    private static final Map<String, Operator> VALUES = new ConcurrentHashMap<>();

    public static final Operator KMB = createBuiltIn("kmb");
    public static final Operator CTB = createBuiltIn("ctb");
    public static final Operator NLB = createBuiltIn("nlb");
    public static final Operator MTR_BUS = createBuiltIn("mtr-bus");
    public static final Operator GMB = createBuiltIn("gmb");
    public static final Operator LRT = createBuiltIn("lightRail");
    public static final Operator MTR = createBuiltIn("mtr");

    private static Operator createBuiltIn(String name) {
        return VALUES.computeIfAbsent(name.toLowerCase(), k -> new Operator(k, true));
    }

    public static Operator valueOf(String name) {
        return VALUES.computeIfAbsent(name.toLowerCase(), k -> new Operator(k, false));
    }

    private final String name;
    private final boolean builtIn;

    private Operator(String name, boolean builtIn) {
        this.name = name;
        this.builtIn = builtIn;
    }

    public String name() {
        return name;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operator operator = (Operator) o;
        return Objects.equals(name, operator.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
