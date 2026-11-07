package com.purrcare.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseUser;
import com.purrcare.app.R;
import com.purrcare.app.adapters.CatAdapter;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.models.Expense;
import com.purrcare.app.models.Reminder;
import com.purrcare.app.models.WeightRecord;
import com.purrcare.app.notifications.ReminderScheduler;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Home dashboard: welcome, totals, upcoming reminders, latest weights, monthly expense, quick access, cat cards. */
public class HomeActivity extends BaseActivity {

    private TextView tvTotalCats, tvMonthExpense, tvUpcoming, tvLatestWeight, tvEmptyCats;
    private CatAdapter catAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_home);
        setupBottomNav(R.id.nav_home);

        tvTotalCats = findViewById(R.id.tvTotalCats);
        tvMonthExpense = findViewById(R.id.tvMonthExpense);
        tvUpcoming = findViewById(R.id.tvUpcoming);
        tvLatestWeight = findViewById(R.id.tvLatestWeight);
        tvEmptyCats = findViewById(R.id.tvEmptyCats);

        // Welcome message + user information
        FirebaseUser user = FirebaseHelper.currentUser();
        String name = user.getDisplayName();
        String email = user.getEmail() == null ? "" : user.getEmail();
        if (name == null || name.trim().isEmpty()) name = email.contains("@") ? email.substring(0, email.indexOf('@')) : "friend";
        ((TextView) findViewById(R.id.tvWelcome)).setText("Hello, " + name + "! \uD83D\uDC3E");
        ((TextView) findViewById(R.id.tvEmail)).setText(email);

        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());
        findViewById(R.id.btnAddCat).setOnClickListener(v -> startActivity(new Intent(this, AddEditCatActivity.class)));
        populateFeatureGrid((GridLayout) findViewById(R.id.featureGrid));

        // Horizontal list of small cat cards
        RecyclerView rvCats = findViewById(R.id.rvCats);
        rvCats.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        catAdapter = new CatAdapter(true, cat -> {
            Intent intent = new Intent(this, AddEditCatActivity.class);
            intent.putExtra(Constants.EXTRA_CAT_ID, cat.getId());
            startActivity(intent);
        }, null);
        rvCats.setAdapter(catAdapter);

        requestNotificationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!loggedIn) return;
        // Real-time listeners: the dashboard numbers change by themselves when data changes
        addSub(FirebaseHelper.listen(this, Constants.NODE_REMINDERS, Reminder.class, list -> {
            ReminderScheduler.rescheduleAll(this, list);   // make sure alarms exist for pending reminders
            showUpcoming(list);
        }));
        addSub(FirebaseHelper.listen(this, Constants.NODE_WEIGHT, WeightRecord.class, this::showLatestWeights));
        addSub(FirebaseHelper.listen(this, Constants.NODE_EXPENSES, Expense.class, this::showMonthExpense));
    }

    @Override
    protected void onCatsChanged() {
        tvTotalCats.setText(String.valueOf(allCats.size()));
        catAdapter.setCats(allCats);
        tvEmptyCats.setVisibility(allCats.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showUpcoming(List<Reminder> list) {
        long now = System.currentTimeMillis();
        List<Reminder> upcoming = new ArrayList<>();
        for (Reminder r : list) {
            if (!r.isCompleted() && r.getDateTimeMillis() >= now) upcoming.add(r);
        }
        Collections.sort(upcoming, (a, b) -> Long.compare(a.getDateTimeMillis(), b.getDateTimeMillis()));
        if (upcoming.isEmpty()) {
            tvUpcoming.setText("No upcoming reminders \uD83C\uDF89");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, upcoming.size()); i++) {
            Reminder r = upcoming.get(i);
            if (i > 0) sb.append("\n");
            sb.append("\u2022 ").append(r.getTitle()).append(" (").append(catName(r.getCatId(), r.getCatName())).append(")\n   ")
                    .append(DateTimeUtils.formatDateTime(r.getDateTimeMillis()));
        }
        if (upcoming.size() > 3) sb.append("\n+ ").append(upcoming.size() - 3).append(" more");
        tvUpcoming.setText(sb.toString());
    }

    private void showLatestWeights(List<WeightRecord> list) {
        Map<String, WeightRecord> latest = new LinkedHashMap<>();
        for (WeightRecord w : list) {
            WeightRecord current = latest.get(w.getCatId());
            if (current == null || UiUtils.nz(w.getDate()).compareTo(UiUtils.nz(current.getDate())) > 0) {
                latest.put(w.getCatId(), w);
            }
        }
        if (latest.isEmpty()) {
            tvLatestWeight.setText("No weight recorded yet");
            return;
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (WeightRecord w : latest.values()) {
            if (count == 3) break;
            if (count > 0) sb.append("\n");
            sb.append(catName(w.getCatId(), w.getCatName())).append(": ")
                    .append(String.format(Locale.US, "%.1f kg", w.getWeightKg()))
                    .append("  (").append(DateTimeUtils.pretty(w.getDate())).append(")");
            count++;
        }
        tvLatestWeight.setText(sb.toString());
    }

    private void showMonthExpense(List<Expense> list) {
        String month = DateTimeUtils.currentMonthPrefix();
        double total = 0;
        for (Expense e : list) {
            if (UiUtils.nz(e.getDate()).startsWith(month)) total += e.getAmount();
        }
        tvMonthExpense.setText(UiUtils.money(total));
    }
}
