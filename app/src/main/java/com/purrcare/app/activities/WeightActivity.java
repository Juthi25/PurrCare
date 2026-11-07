package com.purrcare.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.purrcare.app.R;
import com.purrcare.app.adapters.RecordAdapter;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.models.Cat;
import com.purrcare.app.models.WeightRecord;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.UiUtils;
import com.purrcare.app.utils.WeightChartView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Weight tracker: weight history + simple chart. Records are kept separate for each cat (one cat is always selected). */
public class WeightActivity extends BaseActivity {

    private final List<WeightRecord> allRecords = new ArrayList<>();
    private RecordAdapter<WeightRecord> adapter;
    private WeightChartView chart;
    private TextView tvSummary, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_list);
        initListScreen("Weight Tracker \u2696\uFE0F", R.id.nav_more, true);
        initCatFilter(false);   // no "All cats": weights are always shown for ONE cat

        tvSummary = findViewById(R.id.tvSummary);
        findViewById(R.id.cardSummary).setVisibility(View.VISIBLE);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No weight records yet.\nTap + to add one \u2696\uFE0F");

        // chart goes into the "extra" area above the list
        chart = new WeightChartView(this);
        FrameLayout extra = findViewById(R.id.extraContainer);
        extra.addView(chart, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, UiUtils.dp(this, 190)));
        extra.setVisibility(View.VISIBLE);

        adapter = new RecordAdapter<>(this::bindWeight, new RecordAdapter.ActionListener<WeightRecord>() {
            @Override
            public void onEdit(WeightRecord record) {
                showDialog(record);
            }

            @Override
            public void onDelete(WeightRecord record) {
                confirmDelete("Delete this weight record?", () ->
                        FirebaseHelper.delete(WeightActivity.this, Constants.NODE_WEIGHT, record.getId()));
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
        addSub(FirebaseHelper.listen(this, Constants.NODE_WEIGHT, WeightRecord.class, list -> {
            allRecords.clear();
            allRecords.addAll(list);
            render();
        }));
    }

    @Override
    protected void onFilterChanged() {
        render();
    }

    private void render() {
        List<WeightRecord> shown = new ArrayList<>();
        for (WeightRecord r : allRecords) {
            if (filterCatId != null && filterCatId.equals(r.getCatId())) shown.add(r);
        }
        // oldest -> newest for the chart
        Collections.sort(shown, (a, b) -> UiUtils.nz(a.getDate()).compareTo(UiUtils.nz(b.getDate())));
        chart.setData(shown);

        if (shown.isEmpty()) {
            tvSummary.setText(allCats.isEmpty() ? "Add a cat first, then record its weight." : "No weight recorded for this cat yet.");
        } else {
            WeightRecord latest = shown.get(shown.size() - 1);
            StringBuilder sb = new StringBuilder(String.format(Locale.US, "\u2696\uFE0F Latest weight: %.2f kg (%s)",
                    latest.getWeightKg(), DateTimeUtils.pretty(latest.getDate())));
            if (shown.size() > 1) {
                double change = latest.getWeightKg() - shown.get(shown.size() - 2).getWeightKg();
                sb.append(String.format(Locale.US, "\nChange since previous: %+.2f kg", change));
            }
            tvSummary.setText(sb.toString());
        }

        // list: newest first
        List<WeightRecord> newestFirst = new ArrayList<>(shown);
        Collections.reverse(newestFirst);
        adapter.setItems(newestFirst);
        tvEmpty.setVisibility(newestFirst.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bindWeight(WeightRecord r, RecordAdapter.RecordViewHolder h) {
        h.tvEmoji.setText("\u2696\uFE0F");
        h.tvTitle.setText(String.format(Locale.US, "%.2f kg", r.getWeightKg()));
        h.tvSubtitle.setText(catName(r.getCatId(), r.getCatName()) + " \u2022 " + DateTimeUtils.pretty(r.getDate()));
        h.showOptional(h.tvDetail, r.getNotes());
    }

    private void showDialog(WeightRecord existing) {
        if (!requireCats()) return;
        View view = getLayoutInflater().inflate(R.layout.dialog_weight, null);
        Spinner spCat = view.findViewById(R.id.spCat);
        TextInputEditText etWeight = view.findViewById(R.id.etWeight);
        TextInputEditText etDate = view.findViewById(R.id.etDate);
        TextInputEditText etNotes = view.findViewById(R.id.etNotes);

        setupCatSpinner(spCat, existing != null ? existing.getCatId() : filterCatId);
        UiUtils.attachDatePicker(this, etDate, true);

        if (existing != null) {
            etWeight.setText(String.valueOf(existing.getWeightKg()));
            etDate.setText(existing.getDate());
            etNotes.setText(existing.getNotes());
        } else {
            etDate.setText(DateTimeUtils.todayString());
        }

        showFormDialog(existing == null ? "Add weight" : "Edit weight", view, () -> {
            Double kg = UiUtils.requirePositiveNumber(etWeight, "Enter the weight in kg");
            if (kg == null) return false;
            String date = UiUtils.requireText(etDate, "Choose the measurement date");
            if (date == null) return false;
            Cat cat = selectedCat(spCat);
            if (cat == null) return false;

            String id = existing != null ? existing.getId() : FirebaseHelper.newId(Constants.NODE_WEIGHT);
            WeightRecord record = new WeightRecord(id, cat.getId(), cat.getName(), kg, date, UiUtils.text(etNotes));
            FirebaseHelper.save(this, Constants.NODE_WEIGHT, id, record);

            // If this is the newest measurement, also update the cat's "current weight" in its profile
            boolean isNewest = true;
            for (WeightRecord other : allRecords) {
                if (cat.getId().equals(other.getCatId()) && !id.equals(other.getId())
                        && UiUtils.nz(other.getDate()).compareTo(date) > 0) {
                    isNewest = false;
                }
            }
            if (isNewest) FirebaseHelper.updateField(this, Constants.NODE_CATS, cat.getId(), "weight", kg);
            return true;
        });
    }
}
