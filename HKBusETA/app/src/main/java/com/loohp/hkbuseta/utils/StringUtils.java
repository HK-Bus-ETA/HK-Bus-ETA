package com.loohp.hkbuseta.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static int editDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)), dp[i - 1][j] + 1, dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    public static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    public static int min(int... numbers) {
        return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }

    public static String getCircledNumber(int number) {
        if (number < 0 || number > 20) {
            return String.valueOf(number);
        }
        if (number == 0) {
            return "⓿";
        }
        if (number > 10) {
            return String.valueOf((char) (9451 + (number - 11)));
        }
        return String.valueOf((char) (10102 + (number - 1)));
    }

    public static String getHollowCircledNumber(int number) {
        if (number < 0 || number > 10) {
            return String.valueOf(number);
        }
        if (number == 0) {
            return "⓪";
        }
        return String.valueOf((char) (9312 + (number - 1)));
    }

    public static String capitalize(String str) {
        return capitalize(str, true);
    }

    public static String capitalize(String str, boolean lower) {
        if (lower) {
            str = str.toLowerCase();
        }
        StringBuffer sb = new StringBuffer();
        Matcher matcher = Pattern.compile("(?:^|\\s|[\\\"'(\\[{/\\-])+\\S").matcher(str);
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

    public static float findTextLengthDp(Context context, String text, float sp) {
        TextView textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        return UnitUtils.pixelsToDp(context, textView.getPaint().measureText(text));
    }

}
