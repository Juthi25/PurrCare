package com.purrcare.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.purrcare.app.R;
import com.purrcare.app.utils.PrefsManager;
import com.purrcare.app.utils.UiUtils;

/** Login screen: Firebase Authentication with e-mail + password, validation and Remember Me. */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private CheckBox cbRemember;
    private MaterialButton btnLogin;
    private ProgressBar progress;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRemember = findViewById(R.id.cbRemember);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);

        try {
            auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            auth = null;
        }

        etEmail.setText(PrefsManager.getLastEmail(this));
        cbRemember.setChecked(PrefsManager.isRememberMe(this));

        btnLogin.setOnClickListener(v -> attemptLogin());
        ((TextView) findViewById(R.id.tvGoRegister)).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        ((TextView) findViewById(R.id.tvForgot)).setOnClickListener(v -> sendPasswordReset());
    }

    /** Checks the e-mail field. Returns the e-mail or null (and shows setError). */
    private String validEmail() {
        String email = UiUtils.requireText(etEmail, "Enter your e-mail");
        if (email == null) return null;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid e-mail address");
            etEmail.requestFocus();
            return null;
        }
        return email;
    }

    private void attemptLogin() {
        String email = validEmail();
        if (email == null) return;
        String password = UiUtils.requireText(etPassword, "Enter your password");
        if (password == null) return;
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (auth == null) {
            UiUtils.snack(btnLogin, "Firebase is not configured. Add google-services.json (see README).");
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                // SHAREDPREFERENCES: remember the choice so Splash can keep the user logged in
                PrefsManager.setRememberMe(this, cbRemember.isChecked());
                PrefsManager.saveLastEmail(this, email);
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                UiUtils.snack(btnLogin, authErrorMessage(task.getException()));
            }
        });
    }

    private void sendPasswordReset() {
        String email = validEmail();
        if (email == null || auth == null) return;
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) UiUtils.snack(btnLogin, "Password reset e-mail sent to " + email);
            else UiUtils.snack(btnLogin, authErrorMessage(task.getException()));
        });
    }

    /** Turns Firebase exceptions into messages a normal user understands. */
    static String authErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) return "No account found with this e-mail.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "Incorrect e-mail or password.";
        if (e instanceof FirebaseNetworkException) return "No internet connection. Please try again.";
        return e == null ? "Something went wrong." : "Failed: " + e.getMessage();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
