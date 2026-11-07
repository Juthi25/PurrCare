package com.purrcare.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.purrcare.app.R;
import com.purrcare.app.adapters.RecordAdapter;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.models.Cat;
import com.purrcare.app.models.HealthRecord;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Health tracker: history of vaccinations, deworming, medicine and vet visits + upcoming due dates. */
public class HealthActivity extends BaseActivity {

    private final List<HealthRecord> allRecords = new ArrayList<>();
    private RecordAdapter<HealthRecord> adapter;
    private TextView tvSummary, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_list);
        initListScreen("Health Tracker \uD83D\uDC89", R.id.nav_health, false);
        initCatFilter(true);

        tvSummary = findViewById(R.id.tvSummary);
        findViewById(R.id.cardSummary).setVisibility(View.VISIBLE);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No health records yet.\nTap + to add one \uD83D\uDC89");

        adapter = new RecordAdapter<>(this::bindRecord, new RecordAdapter.ActionListener<HealthRecord>() {
            @Override
            public void onEdit(HealthRecord record) {
                showDialog(record);
            }

            @Override
            public void onDelete(HealthRecord record) {
                confirmDelete("Delete this health record?", () ->
                        FirebaseHelper.delete(HealthActivity.this, Constants.NODE_HEALTH, record.getId()));
            }
        });
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        findViewById(R.id.fabAdd).setOnClickListener(v -> showDialog(null));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!loggedIn) return;
        addSub(FirebaseHelper.listen(this, Constants.NODE_HEALTH, HealthRecord.class, list -> {
            allRecords.clear();
            allRecords.addAll(list);
            render();
        }));
    }

    @Override
    protected void onFilterChanged() {
        render();
    }

    /** Applies the cat filter, sorts the history (newest first) and builds the "upcoming" box. */
    private void render() {
        List<HealthRecord> shown = new ArrayList<>();
        for (HealthRecord r : allRecords) {
            if (filterCatId == null || filterCatId.equals(r.getCatId())) shown.add(r);
        }
        Collections.sort(shown, (a, b) -> UiUtils.nz(b.getDate()).compareTo(UiUtils.nz(a.getDate())));
        adapter.setItems(shown);
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);

        // Upcoming / overdue due dates (skip a due date if a newer record of the same type was added since)
        List<HealthRecord> due = new ArrayList<>();
        for (HealthRecord r : shown) {
            if (UiUtils.nz(r.getNextDueDate()).isEmpty()) continue;
            boolean handled = false;
            for (HealthRecord other : allRecords) {
                if (other != r && UiUtils.nz(other.getCatId()).equals(UiUtils.nz(r.getCatId()))
                        && UiUtils.nz(other.getType()).equals(UiUtils.nz(r.getType()))
                        && UiUtils.nz(other.getDate()).compareTo(r.getNextDueDate()) >= 0) {
                    handled = true;
                }
            }
            if (!handled) due.add(r);
        }
        Collections.sort(due, (a, b) -> a.getNextDueDate().compareTo(b.getNextDueDate()));
        if (due.isEmpty()) {
            tvSummary.setText("\uD83D\uDCC5 No upcoming due dates.\nAdd a \"Next due date\" to a vaccination or deworming record.");
        } else {
            StringBuilder sb = new StringBuilder("\uD83D\uDCC5 Upcoming due dates");
            for (int i = 0; i < Math.min(5, due.size()); i++) {
                HealthRecord r = due.get(i);
                sb.append("\n\u2022 ").append(catName(r.getCatId(), r.getCatName())).append(" - ").append(r.getType())
                        .append(": ").append(DateTimeUtils.pretty(r.getNextDueDate()))
                        .append(" (").append(DateTimeUtils.dueText(r.getNextDueDate())).append(")");
            }
            tvSummary.setText(sb.toString());
        }
    }

    private String emojiFor(String type) {
        if ("Vaccination".equals(type)) return "\uD83D\uDC89";
        if ("Deworming".equals(type)) return "\uD83D\uDC1B";
        if ("Medicine".equals(type)) return "\uD83D\uDC8A";
        if ("Vet Visit".equals(type)) return "\uD83C\uDFE5";
        return "\uD83D\uDCCB";
    }

    /** Fills one card of the list. */
    private void bindRecord(HealthRecord r, RecordAdapter.RecordViewHolder h) {
        h.tvEmoji.setText(emojiFor(r.getType()));
        h.tvTitle.setText(r.getTitle());
        h.tvSubtitle.setText(catName(r.getCatId(), r.getCatName()) + " \u2022 " + r.getType() + " \u2022 " + DateTimeUtils.pretty(r.getDate()));
        StringBuilder detail = new StringBuilder();
        if (!UiUtils.nz(r.getMedicineName()).isEmpty()) detail.append("\uD83D\uDC8A ").append(r.getMedicineName());
        if (!UiUtils.nz(r.getDescription()).isEmpty()) {
            if (detail.length() > 0) detail.append("\n");
            detail.append(r.getDescription());
        }
        h.showOptional(h.tvDetail, detail.toString());
        if (!UiUtils.nz(r.getNextDueDate()).isEmpty()) {
            h.showOptional(h.tvTag, "Next: " + DateTimeUtils.pretty(r.getNextDueDate()) + " \u2022 " + DateTimeUtils.dueText(r.getNextDueDate()));
        }
    }

    /** Add (existing == null) or Edit dialog. */
    private void showDialog(HealthRecord existing) {
        if (!requireCats()) return;
        View view = getLayoutInflater().inflate(R.layout.dialog_health, null);
        Spinner spCat = view.findViewById(R.id.spCat);
        Spinner spType = view.findViewById(R.id.spType);
        TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        TextInputEditText etDate = view.findViewById(R.id.etDate);
        TextInputEditText etNextDue = view.findViewById(R.id.etNextDue);
        TextInputEditText etMedicine = view.findViewById(R.id.etMedicine);
        TextInputEditText etDescription = view.findViewById(R.id.etDescription);

        setupCatSpinner(spCat, existing != null ? existing.getCatId() : filterCatId);
        UiUtils.setupSpinner(this, spType, Constants.HEALTH_TYPES);
        UiUtils.attachDatePicker(this, etDate, false);
        UiUtils.attachDatePicker(this, etNextDue, false);

        if (existing != null) {
            etTitle.setText(existing.getTitle());
            spType.setSelection(UiUtils.indexOf(Constants.HEALTH_TYPES, existing.getType()));
            etDate.setText(existing.getDate());
            etNextDue.setText(existing.getNextDueDate());
            etMedicine.setText(existing.getMedicineName());
            etDescription.setText(existing.getDescription());
        } else {
            etDate.setText(DateTimeUtils.todayString());
        }

        showFormDialog(existing == null ? "Add health record" : "Edit health record", view, () -> {
            String title = UiUtils.requireText(etTitle, "Enter a title");
            if (title == null) return false;
            String date = UiUtils.requireText(etDate, "Choose the date");
            if (date == null) return false;
            String nextDue = UiUtils.text(etNextDue);
            if (!nextDue.isEmpty() && nextDue.compareTo(date) < 0) {
                etNextDue.setError("Next due date must be after the record date");
                return false;
            }
            Cat cat = selectedCat(spCat);
            if (cat == null) return false;

            String id = existing != null ? existing.getId() : FirebaseHelper.newId(Constants.NODE_HEALTH);
            HealthRecord record = new HealthRecord(id, cat.getId(), cat.getName(), title,
                    (String) spType.getSelectedItem(), date, nextDue,
                    UiUtils.text(etDescription), UiUtils.text(etMedicine));
            FirebaseHelper.save(this, Constants.NODE_HEALTH, id, record);
            return true;
        });
    }
}
