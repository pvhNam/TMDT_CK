package com.example.tmdt;

public final class TmdtApplication extends android.app.Application {
    @Override public void onCreate(){super.onCreate();com.example.tmdt.ui.common.AppAppearance.restore(this);}
}
