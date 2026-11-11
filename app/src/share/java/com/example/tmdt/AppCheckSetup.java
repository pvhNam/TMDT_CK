package com.example.tmdt;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/** APK shared with physical devices uses attestation, not per-install debug secrets. */
public final class AppCheckSetup {
    private AppCheckSetup() {}

    public static void install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
    }
}
