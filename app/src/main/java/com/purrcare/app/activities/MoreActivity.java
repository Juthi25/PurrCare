package com.purrcare.app.activities;

import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.purrcare.app.R;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.utils.PrefsManager;

/** "More" tab: Food, Weight, Expense, Diary, Emergency + settings (SharedPreferences) + share + logout. */
public class MoreActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_more);
        setupBottomNav(R.id.nav_more);

        String email = FirebaseHelper.currentUser().getEmail();
        ((TextView) findViewById(R.id.tvEmail)).setText(email == null ? "" : email);
        populateFeatureGrid((GridLayout) findViewById(R.id.featureGrid));

        // SHAREDPREFERENCES: settings stored on the phone
        SwitchMaterial switchRemember = findViewById(R.id.switchRemember);
        switchRemember.setChecked(PrefsManager.isRememberMe(this));
        switchRemember.setOnCheckedChangeListener((button, checked) -> PrefsManager.setRememberMe(this, checked));

        SwitchMaterial switchShake = findViewById(R.id.switchShake);
        switchShake.setChecked(PrefsManager.isShakeEnabled(this));
        switchShake.setOnCheckedChangeListener((button, checked) -> {
            PrefsManager.setShakeEnabled(this, checked);
            startShakeIfEnabled();   // start or stop the accelerometer right away
        });

        findViewById(R.id.btnShare).setOnClickListener(v -> shareApp());
        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());
    }
}
