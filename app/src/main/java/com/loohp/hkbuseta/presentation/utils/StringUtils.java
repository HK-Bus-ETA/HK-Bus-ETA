package com.loohp.hkbuseta.presentation.utils;

import android.content.Context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static String capitalize(String str) {
        return capitalize(str, true);
    }

    public static String capitalize(String str, boolean lower) {
        if (lower) {
            str = str.toLowerCase();
        }
        StringBuffer sb = new StringBuffer();
        Matcher matcher = Pattern.compile("(?:^|\\s|[\"'(\\[{])+\\S").matcher(str);
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group().toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String padStart(String inputString, int length, char c) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append(c);
        }
        sb.append(inputString);

        return sb.toString();
    }

    public static int scaledSize(int size, Context context) {
        return Math.round(scaledSize((float) size, context));
    }

    public static float scaledSize(float size, Context context) {
        int dimension = ScreenSizeUtils.getMinScreenSize(context);
        float scale = dimension / 454F;
        return size * scale;
    }

}
