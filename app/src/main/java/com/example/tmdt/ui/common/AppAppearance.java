package com.example.tmdt.ui.common;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

/** The selected appearance is device-local and restored before the first activity opens. */
public final class AppAppearance {
    private AppAppearance(){}
    public static int mode(Context context){return context.getSharedPreferences("app_settings",Context.MODE_PRIVATE).getInt("theme_mode",AppCompatDelegate.MODE_NIGHT_NO);}
    public static void restore(Context context){AppCompatDelegate.setDefaultNightMode(mode(context));}
    public static void select(Context context,int mode){
        if(mode!=AppCompatDelegate.MODE_NIGHT_NO&&mode!=AppCompatDelegate.MODE_NIGHT_YES&&mode!=AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)return;
        context.getSharedPreferences("app_settings",Context.MODE_PRIVATE).edit().putInt("theme_mode",mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
    public static String label(Context context){int value=mode(context);return value==AppCompatDelegate.MODE_NIGHT_YES?"Tối":value==AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM?"Theo hệ thống":"Sáng";}
}
