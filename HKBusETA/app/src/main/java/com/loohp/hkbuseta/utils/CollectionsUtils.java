package com.loohp.hkbuseta.utils;

import java.util.Collection;
import java.util.function.Predicate;

public class CollectionsUtils {

    public static <T> int indexOf(Collection<T> collection, Predicate<T> matcher) {
        int i = 0;
        for (T t : collection) {
            if (matcher.test(t)) {
                return i;
            }
            i++;
        }
        return -1;
    }

}
