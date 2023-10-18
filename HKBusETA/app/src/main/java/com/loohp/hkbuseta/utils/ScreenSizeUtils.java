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

    public static int getMinScreenSize(Context context) {
        if (context instanceof Activity) {
            Rect bound = ((Activity) context).getWindowManager().getCurrentWindowMetrics().getBounds();
            return Math.min(Math.abs(bound.width()), Math.abs(bound.height()));
        } else {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return Math.min(Math.abs(displayMetrics.widthPixels), Math.abs(displayMetrics.heightPixels));
        }
    }

    public static int getScreenWidth(Context context) {
        if (context instanceof Activity) {
            Rect bound = ((Activity) context).getWindowManager().getCurrentWindowMetrics().getBounds();
            return Math.abs(bound.width());
        } else {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return Math.abs(displayMetrics.widthPixels);
        }
    }

    public static int getScreenHeight(Context context) {
        if (context instanceof Activity) {
            Rect bound = ((Activity) context).getWindowManager().getCurrentWindowMetrics().getBounds();
            return Math.abs(bound.height());
        } else {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return Math.abs(displayMetrics.heightPixels);
        }
    }

}
