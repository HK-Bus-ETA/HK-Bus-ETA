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

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.Arrays;

public class StringUtils {

    public static int editDistance(String x, String y) {
        if (x.equals(y)) {
            return 0;
        }

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

    private static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private static int min(int... numbers) {
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

    public static float scaledSize(int size, Context context) {
        return size * ScreenSizeUtils.getScreenScale(context);
    }

    public static float scaledSize(float size, Context context) {
        return size * ScreenSizeUtils.getScreenScale(context);
    }

    public static float findOptimalSp(Context context, String text, int targetWidth, int maxLines, float minSp, float maxSp) {
        TextPaint paint = new TextPaint();
        paint.density = context.getResources().getDisplayMetrics().density;
        for (float sp = maxSp; sp >= minSp; sp -= 0.5F) {
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
