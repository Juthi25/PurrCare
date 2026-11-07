package com.purrcare.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.purrcare.app.R;
import com.purrcare.app.utils.PrefsManager;

/** Splash screen: shows the logo for ~2 seconds, then decides where to go (Home or Login). */
public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(this::routeUser, 2000);
    }

    private void routeUser() {
        if (isFinishing()) return;
        FirebaseUser user = null;
        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
        } catch (Exception e) {
            // Firebase is not configured (google-services.json missing) - go to Login, it shows the error.
        }

        Class<?> target;
        if (user != null && PrefsManager.isRememberMe(this)) {
            target = HomeActivity.class;        // Remember Me is ON -> stay logged in
        } else {
            if (user != null) FirebaseAuth.getInstance().signOut();   // Remember Me is OFF -> log out
            target = LoginActivity.class;
        }
        startActivity(new Intent(this, target));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
