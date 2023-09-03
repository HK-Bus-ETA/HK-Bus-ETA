package com.loohp.hkbuseta.presentation.utils;

public class IntUtils {

    public static int parseOrZero(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
