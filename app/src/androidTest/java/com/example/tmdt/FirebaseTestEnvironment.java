package com.example.tmdt;

import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

final class FirebaseTestEnvironment {
    private static boolean configured;
    static void configure() {
        if(configured)return;
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("firebaseEmulators")))return;
        FirebaseAuth.getInstance().useEmulator("10.0.2.2",9099);
        FirebaseAuth.getInstance().getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
        FirebaseFirestore.getInstance().setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2",8080);
        FirebaseAuth.getInstance().signOut();configured=true;
    }
}

