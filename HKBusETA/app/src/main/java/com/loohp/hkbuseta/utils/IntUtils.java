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
