package com.loohp.hkbuseta.presentation.utils;

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

}
