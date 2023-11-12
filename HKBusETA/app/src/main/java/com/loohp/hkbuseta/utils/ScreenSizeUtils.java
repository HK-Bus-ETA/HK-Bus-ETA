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

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;

public class ScreenSizeUtils {

    private static boolean init;
    private static int width;
    private static int height;
    private static int min;
    private static float scale;

    private static void init(Context context) {
        if (init) {
            return;
        }
        synchronized (ScreenSizeUtils.class) {
            if (init) {
                return;
            }
            if (context instanceof Activity) {
                Rect bound = ((Activity) context).getWindowManager().getCurrentWindowMetrics().getBounds();
                width = Math.abs(bound.width());
                height = Math.abs(bound.height());
            } else {
                DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                width = Math.abs(displayMetrics.widthPixels);
                height = Math.abs(displayMetrics.heightPixels);
            }
            min = Math.min(width, height);
            scale = min / 454F;
            init = true;
        }
    }

    public static int getMinScreenSize(Context context) {
        init(context);
        return min;
    }

    public static float getScreenScale(Context context) {
        init(context);
        return scale;
    }

    public static int getScreenWidth(Context context) {
        init(context);
        return width;
    }

    public static int getScreenHeight(Context context) {
        init(context);
        return height;
    }

}
