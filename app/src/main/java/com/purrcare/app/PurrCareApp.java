package com.purrcare.app;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Runs once when the app process starts.
 * Offline persistence keeps your data on the phone and syncs it when the internet comes back.
 */
public class PurrCareApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Must be called before any other Firebase Database usage.
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception ignored) {
            // If Firebase is not configured yet (no google-services.json) the app still shows the login screen.
        }
    }
}
