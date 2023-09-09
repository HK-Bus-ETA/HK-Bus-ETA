package com.loohp.hkbuseta.presentation.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

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

    public static float findOptimalSp(Context context, String text, int targetWidth, int maxLines, float minSp, float maxSp) {
        TextPaint paint = new TextPaint();
        paint.density = context.getResources().getDisplayMetrics().density;
        for (float sp = maxSp; sp >= minSp; sp--) {
            paint.setTextSize(UnitUtils.spToPixels(context, sp));
            paint.setTypeface(Typeface.DEFAULT);

            StaticLayout staticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), paint, targetWidth)
                    .setMaxLines(Integer.MAX_VALUE)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build();

            if (staticLayout.getLineCount() <= maxLines) {
                return sp;
            }
        }
        return minSp;
    }

}
