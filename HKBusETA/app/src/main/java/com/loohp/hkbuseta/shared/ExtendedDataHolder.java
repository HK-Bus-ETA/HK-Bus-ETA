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

package com.loohp.hkbuseta.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExtendedDataHolder {

    private static final Map<String, ExtendedDataHolder> REGISTRY = new ConcurrentHashMap<>();

    public static Builder createNew() {
        return new Builder();
    }

    public static ExtendedDataHolder get(String key) {
        return REGISTRY.get(key);
    }

    private final Map<String, Object> extras;

    private ExtendedDataHolder(Map<String, Object> extras) {
        this.extras = Collections.unmodifiableMap(extras);
    }

    public Object getExtra(String name) {
        return extras.get(name);
    }

    /** @noinspection unchecked*/
    public <T> T getExtra(String name, Class<T> type) {
        return (T) extras.get(name);
    }

    public boolean hasExtra(String name) {
        return extras.containsKey(name);
    }

    public void clear() {
        extras.clear();
    }

    public static class Builder {

        private final Map<String, Object> extras;

        private Builder() {
            this.extras = new HashMap<>();
        }

        public String buildAndRegisterData(String key) {
            ExtendedDataHolder holder = new ExtendedDataHolder(extras);
            REGISTRY.put(key, holder);
            return key;
        }

        public Builder extra(String name, Object object) {
            extras.put(name, object);
            return this;
        }

        public Builder extras(ExtendedDataHolder other) {
            extras.putAll(other.extras);
            return this;
        }

    }

}
