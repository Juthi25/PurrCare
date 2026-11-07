package com.purrcare.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.purrcare.app.R;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.models.Cat;
import com.purrcare.app.sensors.ShakeDetector;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.PrefsManager;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parent of every screen that is shown AFTER login. It gives all screens:
 *  - a login check (unauthorized users are sent back to the Login screen)
 *  - the list of the user's cats (real-time, from Firebase)
 *  - bottom navigation, back button, cat filter spinner
 *  - the shake-to-quick-action sensor
 *  - helpers for form dialogs, delete confirmation, logout and sharing the app
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Used by dialogs: return true when the input is valid (dialog closes), false to keep it open. */
    protected interface FormSubmit {
        boolean submit();
    }

    protected boolean loggedIn = false;
    /** All cats of the logged-in user, sorted by name. Updated automatically by a Firebase listener. */
    protected final List<Cat> allCats = new ArrayList<>();
    /** Selected cat in the filter spinner (null = all cats). */
    protected String filterCatId = null;

    private final List<FirebaseHelper.Subscription> subscriptions = new ArrayList<>();
    private ShakeDetector shakeDetector;
    private AlertDialog shakeDialog;

    private Spinner spCatFilter;
    private boolean filterIncludeAll = true;
    private final List<String> filterIds = new ArrayList<>();

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    UiUtils.snack(findViewById(android.R.id.content),
                            "Notifications are off, so reminders will not pop up.");
                }
            });

    // ------------------------------------------------------------------ login guard

    /** Call right after super.onCreate(): "if (!ensureLoggedIn()) return;" */
    protected boolean ensureLoggedIn() {
        loggedIn = FirebaseHelper.currentUser() != null;
        if (!loggedIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        return loggedIn;
    }

    // ------------------------------------------------------------------ lifecycle + real-time listeners

    @Override
    protected void onStart() {
        super.onStart();
        if (!loggedIn) return;
        // REAL-TIME LISTENER for the cats: runs now and every time a cat changes in Firebase
        addSub(FirebaseHelper.listen(this, Constants.NODE_CATS, Cat.class, items -> {
            Collections.sort(items, (a, b) -> UiUtils.nz(a.getName()).compareToIgnoreCase(UiUtils.nz(b.getName())));
            allCats.clear();
            allCats.addAll(items);
            refreshFilterSpinner();
            onCatsChanged();
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (FirebaseHelper.Subscription s : subscriptions) s.remove();
        subscriptions.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startShakeIfEnabled();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopShake();
    }

    protected void addSub(FirebaseHelper.Subscription subscription) {
        subscriptions.add(subscription);
    }

    /** Screens override this to refresh when the cat list changes. */
    protected void onCatsChanged() { }

    /** Screens with a cat filter override this to redraw their list. */
    protected void onFilterChanged() { }

    // ------------------------------------------------------------------ list screen setup

    /** Sets title + bottom navigation (+ back button for sub screens of "More"). */
    protected void initListScreen(String title, int navItemId, boolean showBackButton) {
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(title);
        setupBottomNav(navItemId);
        if (showBackButton) {
            ImageButton back = findViewById(R.id.btnBack);
            back.setVisibility(View.VISIBLE);
            back.setOnClickListener(v -> finish());
        }
    }

    /** NAVIGATION: bottom bar with Home, My Cats, Health, Reminders, More - each opens an Activity with an Intent. */
    protected void setupBottomNav(int selectedItemId) {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(selectedItemId);   // set BEFORE the listener so it does not trigger a click
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return true;
            Class<?> target;
            if (id == R.id.nav_home) target = HomeActivity.class;
            else if (id == R.id.nav_cats) target = CatsActivity.class;
            else if (id == R.id.nav_health) target = HealthActivity.class;
            else if (id == R.id.nav_reminders) target = RemindersActivity.class;
            else target = MoreActivity.class;

            Intent intent = new Intent(this, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            if (id == R.id.nav_home) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            if (!(this instanceof HomeActivity)) finish();
            return true;
        });
    }

    // ------------------------------------------------------------------ cat helpers

    /** Shows the "All cats / cat names" spinner at the top of a list screen. */
    protected void initCatFilter(boolean includeAllOption) {
        filterIncludeAll = includeAllOption;
        spCatFilter = findViewById(R.id.spCatFilter);
        spCatFilter.setVisibility(View.VISIBLE);
        spCatFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= filterIds.size()) return;
                filterCatId = filterIds.get(position);
                onFilterChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void refreshFilterSpinner() {
        if (spCatFilter == null) return;
        filterIds.clear();
        List<String> names = new ArrayList<>();
        if (filterIncludeAll) {
            filterIds.add(null);
            names.add("\uD83D\uDC3E All cats");
        }
        for (Cat cat : allCats) {
            filterIds.add(cat.getId());
            names.add("\uD83D\uDC31 " + cat.getName());
        }
        if (!filterIds.contains(filterCatId)) {
            filterCatId = filterIds.isEmpty() ? null : filterIds.get(0);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCatFilter.setAdapter(adapter);
        spCatFilter.setSelection(Math.max(0, filterIds.indexOf(filterCatId)), false);
    }

    /** Fills a spinner inside a dialog with the user's cat names. */
    protected void setupCatSpinner(Spinner spinner, String selectedCatId) {
        List<String> names = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < allCats.size(); i++) {
            names.add(allCats.get(i).getName());
            if (allCats.get(i).getId() != null && allCats.get(i).getId().equals(selectedCatId)) selected = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
    }

    protected Cat selectedCat(Spinner spinner) {
        int position = spinner.getSelectedItemPosition();
        return (position >= 0 && position < allCats.size()) ? allCats.get(position) : null;
    }

    /** Records need a cat. If there is none yet, tell the user instead of crashing. */
    protected boolean requireCats() {
        if (allCats.isEmpty()) {
            UiUtils.snack(findViewById(android.R.id.content), "Please add a cat first (My Cats tab).");
            return false;
        }
        return true;
    }

    /** Always shows the CURRENT cat name (in case the cat was renamed after the record was saved). */
    protected String catName(String catId, String fallback) {
        for (Cat cat : allCats) {
            if (cat.getId() != null && cat.getId().equals(catId)) return cat.getName();
        }
        return UiUtils.nz(fallback);
    }

    // ------------------------------------------------------------------ dialogs

    /**
     * ALERTDIALOG for Add/Edit forms. The dialog stays open when the input is invalid
     * (the submit callback returns false after showing setError()).
     */
    protected AlertDialog showFormDialog(String title, View content, FormSubmit onSubmit) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(content)
                .setPositiveButton("Save", null)    // real click listener is set below
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (onSubmit.submit()) dialog.dismiss();
        });
        return dialog;
    }

    /** ALERTDIALOG for Delete: nothing is deleted until the user confirms. */
    protected void confirmDelete(String message, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete?")
                .setMessage(message)
                .setPositiveButton("Delete", (d, w) -> onConfirm.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------------ notifications permission

    /** Android 13+ needs the user's permission to show notifications. */
    protected void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    // ------------------------------------------------------------------ feature cards (Home + More)

    private static final String[] FEATURE_EMOJI = {"\uD83C\uDF7D\uFE0F", "\u2696\uFE0F", "\uD83D\uDCB0", "\uD83D\uDCF8", "\uD83D\uDEA8"};
    private static final String[] FEATURE_LABEL = {"Food Tracker", "Weight Tracker", "Expense Tracker", "Growth Diary", "Emergency Info"};

    /** Adds the 5 quick-access cards to a 2-column GridLayout. Each card opens its screen with an Intent. */
    protected void populateFeatureGrid(GridLayout grid) {
        Class<?>[] targets = {FoodActivity.class, WeightActivity.class, ExpenseActivity.class,
                DiaryActivity.class, EmergencyActivity.class};
        for (int i = 0; i < targets.length; i++) {
            View card = getLayoutInflater().inflate(R.layout.item_feature, grid, false);
            ((TextView) card.findViewById(R.id.tvEmoji)).setText(FEATURE_EMOJI[i]);
            ((TextView) card.findViewById(R.id.tvLabel)).setText(FEATURE_LABEL[i]);
            final Class<?> target = targets[i];
            card.setOnClickListener(v -> startActivity(new Intent(this, target)));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            int m = UiUtils.dp(this, 6);
            lp.setMargins(m, m, m, m);
            card.setLayoutParams(lp);
            grid.addView(card);
        }
    }

    // ------------------------------------------------------------------ logout + share (implicit intent)

    protected void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log out")
                .setMessage("Do you want to log out of PurrCare?")
                .setPositiveButton("Log out", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    PrefsManager.setRememberMe(this, false);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** IMPLICIT INTENT (ACTION_SEND): lets the user share app info through any app (WhatsApp, Messenger...). */
    protected void shareApp() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "PurrCare - Cat Care App");
        send.putExtra(Intent.EXTRA_TEXT, "I use PurrCare \uD83D\uDC3E to look after my cats: health records, "
                + "feeding schedules, reminders, weight and expenses in one app!");
        startActivity(Intent.createChooser(send, "Share PurrCare via"));
    }

    // ------------------------------------------------------------------ SHAKE (sensor) quick action

    protected void startShakeIfEnabled() {
        stopShake();
        if (!loggedIn || !PrefsManager.isShakeEnabled(this)) return;
        shakeDetector = new ShakeDetector(this, this::showShakeDialog);
        shakeDetector.start();
    }

    protected void stopShake() {
        if (shakeDetector != null) {
            shakeDetector.stop();
            shakeDetector = null;
        }
    }

    /** Shown when the phone is shaken: a small quick-action dialog for feeding. */
    private void showShakeDialog() {
        if (shakeDialog != null && shakeDialog.isShowing()) return;   // never stack dialogs
        shakeDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("\uD83D\uDC3E Quick action")
                .setItems(new String[]{"\u2795  Add a feeding record", "\uD83C\uDF7D\uFE0F  Open feeding section"}, (d, which) -> {
                    Intent intent = new Intent(this, FoodActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    if (which == 0) intent.putExtra(Constants.EXTRA_OPEN_ADD, true);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
