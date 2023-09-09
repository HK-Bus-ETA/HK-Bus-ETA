package com.loohp.hkbuseta.presentation.utils;

import android.content.Context;
import android.util.TypedValue;

public class UnitUtils {

    public static float spToPixels(Context context, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static float dpToPixels(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static float pixelsToDp(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static float dpToSp(Context context, float dp) {
        return dpToPixels(context, dp) / context.getResources().getDisplayMetrics().scaledDensity;
    }

    public static float spToDp(Context context, float sp) {
        return pixelsToDp(context, sp * context.getResources().getDisplayMetrics().scaledDensity);
    }

}
