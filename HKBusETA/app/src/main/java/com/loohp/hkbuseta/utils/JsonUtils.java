package com.loohp.hkbuseta.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
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

}
