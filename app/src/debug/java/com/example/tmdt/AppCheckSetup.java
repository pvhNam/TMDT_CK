package com.example.tmdt;

import android.os.Build;
import android.util.Log;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

import java.util.Locale;

/** Normal debug APKs attest physical devices; Android emulators use registered debug tokens. */
public final class AppCheckSetup {
    private AppCheckSetup() {}

    public static void install() {
        FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
        if (isAndroidEmulator()) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
            Log.i("AppCheckSetup", "Debug provider: Android emulator");
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());
            Log.i("AppCheckSetup", "Play Integrity: physical device");
        }
    }

    private static boolean isAndroidEmulator() {
        String hardware = Build.HARDWARE.toLowerCase(Locale.ROOT);
        return hardware.contains("ranchu") || hardware.contains("goldfish") || hardware.contains("cutf_cvm");
    }
}
