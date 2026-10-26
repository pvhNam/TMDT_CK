package com.example.tmdt.ui.common;

import android.content.Context;
import android.content.res.Configuration;

/** One app-local size setting for XML layouts, programmatic views, text and dialogs. */
public final class UiScale {
    public static final float FACTOR=0.84f;
    public static final float NAVIGATION_WIDTH_FACTOR=0.90f;
    private UiScale(){}

    public static Context compact(Context base){
        Configuration config=new Configuration(base.getResources().getConfiguration());
        int original=config.densityDpi>0?config.densityDpi:base.getResources().getDisplayMetrics().densityDpi;
        config.densityDpi=Math.max(1,Math.round(original*FACTOR));
        float ratio=(float)config.densityDpi/original;
        config.screenWidthDp=scaledViewport(config.screenWidthDp,ratio);
        config.screenHeightDp=scaledViewport(config.screenHeightDp,ratio);
        config.smallestScreenWidthDp=scaledViewport(config.smallestScreenWidthDp,ratio);
        // Preserve fontScale, locale and accessibility preferences from the user's configuration.
        return base.createConfigurationContext(config);
    }
    private static int scaledViewport(int dp,float ratio){return dp>0?Math.round(dp/ratio):dp;}
}
