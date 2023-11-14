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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Immutable
public final class RouteListType implements Comparable<RouteListType> {

    private static final Map<String, RouteListType> VALUES = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static final RouteListType NORMAL = createBuiltIn("normal");
    public static final RouteListType NEARBY = createBuiltIn("nearby");
    public static final RouteListType FAVOURITE = createBuiltIn("favourite");
    public static final RouteListType RECENT = createBuiltIn("recent");

    private static RouteListType createBuiltIn(String name) {
        return VALUES.computeIfAbsent(name.toLowerCase(), k -> new RouteListType(k, COUNTER.getAndIncrement(), true));
    }

    public static RouteListType valueOf(String name) {
        return VALUES.computeIfAbsent(name.toLowerCase(), k -> new RouteListType(k, COUNTER.getAndIncrement(), false));
    }

    public static RouteListType[] values() {
        return VALUES.values().stream().sorted().toArray(RouteListType[]::new);
    }

    private final String name;
    private final int ordinal;
    private final boolean builtIn;

    private RouteListType(String name, int ordinal, boolean builtIn) {
        this.name = name;
        this.ordinal = ordinal;
        this.builtIn = builtIn;
    }

    public String name() {
        return name;
    }

    public int ordinal() {
        return ordinal;
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
        return this == o;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public int compareTo(RouteListType other) {
        return this.ordinal - other.ordinal;
    }

}
