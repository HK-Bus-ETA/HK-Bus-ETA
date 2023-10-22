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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonUtils {

    public static JSONObject clone(JSONObject jsonObject) {
        try {
            return new JSONObject(jsonObject.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONArray clone(JSONArray jsonArray) {
        try {
            return new JSONArray(jsonArray.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static int indexOf(JSONArray array, Object obj) {
        for (int i = 0; i < array.length(); i++) {
            if (Objects.equals(array.opt(i), obj)) {
                return i;
            }
        }
        return -1;
    }

    public static <T> List<T> mapToList(JSONArray array, Function<Object, T> mapping) {
        List<T> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            list.add(mapping.apply(array.opt(i)));
        }
        return list;
    }

    public static boolean contains(JSONArray array, Object obj) {
        return indexOf(array, obj) >= 0;
    }

    public static boolean containsKey(JSONObject object, Object obj) {
        return StreamSupport.stream(((Iterable<String>) object::keys).spliterator(), false).anyMatch(e -> Objects.equals(e, obj));
    }

    /** @noinspection unchecked*/
    public static <T> List<T> toList(JSONArray array, Class<T> type) {
        List<T> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            list.add((T) array.opt(i));
        }
        return list;
    }

    /** @noinspection unchecked*/
    public static <T> Set<T> toSet(JSONArray array, Class<T> type) {
        Set<T> set = new LinkedHashSet<>();
        for (int i = 0; i < array.length(); i++) {
            set.add((T) array.opt(i));
        }
        return set;
    }

    public static <K, V> Map<K, V> toMap(JSONObject json, JSONFunction<String, K> keyDeserializer, JSONFunction<Object, V> valueDeserializer) throws JSONException {
        Map<K, V> map = new LinkedHashMap<>();
        for (Iterator<String> itr = json.keys(); itr.hasNext(); ) {
            String key = itr.next();
            map.put(keyDeserializer.apply(key), valueDeserializer.apply(json.opt(key)));
        }
        return map;
    }

    public static <V> Map<String, V> toMap(JSONObject json, JSONFunction<Object, V> valueDeserializer) throws JSONException {
        return toMap(json, k -> k, valueDeserializer);
    }

    public static <T> JSONArray fromCollection(Collection<T> list) {
        JSONArray array = new JSONArray();
        for (T t : list) {
            array.put(t);
        }
        return array;
    }

    public static <T> JSONArray fromStream(Stream<T> stream) {
        JSONArray array = new JSONArray();
        stream.forEach(array::put);
        return array;
    }

    public static <V> JSONObject fromMap(Map<?, V> map, JSONFunction<V, Object> valueSerializer) throws JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<?, V> entry : map.entrySet()) {
            json.put(String.valueOf(entry.getKey()), valueSerializer.apply(entry.getValue()));
        }
        return json;
    }

}
