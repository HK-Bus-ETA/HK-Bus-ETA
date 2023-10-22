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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Immutable
public class Operator {

    private static final Map<String, Operator> VALUES = new LinkedHashMap<>();

    public static final Operator KMB = createBuiltIn("kmb");
    public static final Operator CTB = createBuiltIn("ctb");
    public static final Operator NLB = createBuiltIn("nlb");
    public static final Operator MTR_BUS = createBuiltIn("mtr-bus");
    public static final Operator GMB = createBuiltIn("gmb");
    public static final Operator LRT = createBuiltIn("lightRail");
    public static final Operator MTR = createBuiltIn("mtr");

    private static Operator createBuiltIn(String name) {
        return VALUES.computeIfAbsent(name.toLowerCase(), name1 -> new Operator(name1, true));
    }

    public synchronized static Operator valueOf(String name) {
        return VALUES.computeIfAbsent(name.toLowerCase(), name1 -> new Operator(name1, false));
    }

    public static Operator[] values() {
        return VALUES.values().toArray(new Operator[0]);
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
        return name();
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
