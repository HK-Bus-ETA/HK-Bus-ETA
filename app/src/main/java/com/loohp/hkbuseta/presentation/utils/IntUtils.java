package com.loohp.hkbuseta.presentation.utils;

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

}
