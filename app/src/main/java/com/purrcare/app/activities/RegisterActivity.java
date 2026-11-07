package com.purrcare.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.purrcare.app.R;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.PrefsManager;
import com.purrcare.app.utils.UiUtils;

import java.util.HashMap;
import java.util.Map;

/** Registration screen: creates the Firebase account and saves the user profile in the database. */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private MaterialButton btnRegister;
    private ProgressBar progress;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);

        try {
            auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            auth = null;
        }

        btnRegister.setOnClickListener(v -> attemptRegister());
        ((TextView) findViewById(R.id.tvGoLogin)).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        // INPUT VALIDATION with setError()
        String name = UiUtils.requireText(etName, "Enter your name");
        if (name == null) return;
        String email = UiUtils.requireText(etEmail, "Enter your e-mail");
        if (email == null) return;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid e-mail address");
            etEmail.requestFocus();
            return;
        }
        String password = UiUtils.requireText(etPassword, "Enter a password");
        if (password == null) return;
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        String confirm = UiUtils.requireText(etConfirm, "Confirm your password");
        if (confirm == null) return;
        if (!confirm.equals(password)) {
            etConfirm.setError("Passwords do not match");
            etConfirm.requestFocus();
            return;
        }
        if (auth == null) {
            UiUtils.snack(btnRegister, "Firebase is not configured. Add google-services.json (see README).");
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                setLoading(false);
                UiUtils.snack(btnRegister, registerErrorMessage(task.getException()));
                return;
            }
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                // Save the display name in Firebase Auth ...
                user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());
                // ... and a profile record in the database: users/{uid}/profile
                Map<String, Object> profile = new HashMap<>();
                profile.put("name", name);
                profile.put("email", email);
                profile.put("createdAt", System.currentTimeMillis());
                FirebaseDatabase.getInstance().getReference("users").child(user.getUid())
                        .child(Constants.NODE_PROFILE).setValue(profile);
            }
            PrefsManager.setRememberMe(this, true);
            PrefsManager.saveLastEmail(this, email);
            UiUtils.toast(this, "Account created. Welcome to PurrCare!");
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String registerErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) return "This e-mail is already registered. Try logging in.";
        if (e instanceof FirebaseAuthWeakPasswordException) return "Password is too weak. Use at least 6 characters.";
        return LoginActivity.authErrorMessage(e);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
