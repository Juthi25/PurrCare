package com.purrcare.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences = small key/value settings stored on the phone (not in Firebase).
 * We use it for "Remember Me", the last e-mail typed, and the shake-to-quick-action switch.
 */
public final class PrefsManager {
    private static final String FILE = "purrcare_prefs";
    private static final String KEY_REMEMBER = "remember_me";
    private static final String KEY_EMAIL = "last_email";
    private static final String KEY_SHAKE = "shake_enabled";

    private PrefsManager() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static void setRememberMe(Context c, boolean value) {
        prefs(c).edit().putBoolean(KEY_REMEMBER, value).apply();
    }

    public static boolean isRememberMe(Context c) {
        return prefs(c).getBoolean(KEY_REMEMBER, false);
    }

    public static void saveLastEmail(Context c, String email) {
        prefs(c).edit().putString(KEY_EMAIL, email).apply();
    }

    public static String getLastEmail(Context c) {
        return prefs(c).getString(KEY_EMAIL, "");
    }

    public static void setShakeEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean(KEY_SHAKE, value).apply();
    }

    public static boolean isShakeEnabled(Context c) {
        return prefs(c).getBoolean(KEY_SHAKE, true);
    }
}
